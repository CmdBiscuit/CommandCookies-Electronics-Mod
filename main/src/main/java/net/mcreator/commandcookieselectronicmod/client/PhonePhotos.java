package net.mcreator.commandcookieselectronicmod.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;

/** Client PNG library. Only a photo explicitly attached to Email is shared with that recipient. */
public final class PhonePhotos {
    private static final int MAX_PIXELS = 33_177_600;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, h:mm a").withZone(ZoneId.systemDefault());
    public record Photo(String id, Path file, long created, int width, int height) {
        public String date() { return DATE.format(Instant.ofEpochMilli(created)); }
    }
    private PhonePhotos() {}
    public static Path folder() {
        return Path.of(System.getProperty("phone.photos.path", Minecraft.getInstance().gameDirectory.toPath()
            .resolve("screenshots/commandcookies_phones").toString())).toAbsolutePath().normalize();
    }
    private static Path photoFile(String id) {
        if (!id.matches("photo_[0-9]{13}_[a-f0-9]{12}")) throw new IllegalArgumentException("Invalid photo name");
        Path file = folder().resolve(id + ".png").normalize();
        if (!file.getParent().equals(folder())) throw new IllegalArgumentException("Photo outside library");
        return file;
    }
    private static Path thumbnail(String id) { photoFile(id); return folder().resolve("thumbnails").resolve(id + ".png"); }
    private static void checkFile(Path path) throws IOException {
        if (!Files.isRegularFile(path) || Files.isSymbolicLink(path) || Files.size(path) > 67_108_864) throw new IOException("Unreadable photo");
    }
    private static int[] dimensions(Path path) throws IOException {
        checkFile(path);
        try (var input = ImageIO.createImageInputStream(path.toFile())) {
            if (input == null) throw new IOException("Cannot read photo");
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Unknown image format");
            var reader = readers.next();
            try {
                reader.setInput(input); int w = reader.getWidth(0), h = reader.getHeight(0);
                if (w <= 0 || h <= 0 || (long)w * h > MAX_PIXELS) throw new IOException("Photo dimensions too large");
                return new int[]{w,h};
            } finally { reader.dispose(); }
        }
    }
    public static CompletableFuture<List<Photo>> scan() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Files.createDirectories(folder());
                try (var files = Files.list(folder())) {
                    return files.filter(path -> path.getFileName().toString().matches("photo_[0-9]{13}_[a-f0-9]{12}\\.png"))
                        .sorted(Comparator.comparing((Path path) -> path.getFileName().toString()).reversed()).limit(1000)
                        .map(path -> {
                            try {
                                int[] size = dimensions(path);
                                String name = path.getFileName().toString(); String id = name.substring(0,name.length()-4);
                                return new Photo(id,path,Long.parseLong(id.substring(6,19)),size[0],size[1]);
                            } catch (Exception ex) { return null; }
                        }).filter(java.util.Objects::nonNull).toList();
                }
            } catch (IOException ex) { throw new java.util.concurrent.CompletionException(ex); }
        }, Util.ioPool());
    }
    /** Owns and closes the screenshot after the worker finishes writing it. */
    public static CompletableFuture<Photo> save(NativeImage image) {
        return CompletableFuture.supplyAsync(() -> {
            long created = System.currentTimeMillis();
            String id = "photo_" + created + "_" + UUID.randomUUID().toString().replace("-", "").substring(0,12);
            Path file = photoFile(id), temp = file.resolveSibling(file.getFileName()+".tmp");
            try (image) {
                if ((long)image.getWidth()*image.getHeight() > MAX_PIXELS) throw new IOException("Photo resolution too large");
                Files.createDirectories(folder()); Files.createDirectories(thumbnail(id).getParent());
                image.writeToFile(temp);
                try { Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE); }
                catch (java.nio.file.AtomicMoveNotSupportedException ex) { Files.move(temp,file); }
                try (var small = resized(image,160,100)) { small.writeToFile(thumbnail(id)); }
                catch (IOException ex) { /* The full photo remains usable; Gallery can recreate its thumbnail. */ }
                return new Photo(id,file,created,image.getWidth(),image.getHeight());
            } catch (IOException ex) {
                try { Files.deleteIfExists(temp); } catch (IOException ignored) {}
                throw new java.util.concurrent.CompletionException(ex);
            }
        }, Util.ioPool());
    }
    private static NativeImage resized(NativeImage original, int maxW, int maxH) {
        double scale = Math.min(1, Math.min(maxW/(double)original.getWidth(),maxH/(double)original.getHeight()));
        NativeImage result = new NativeImage(Math.max(1,(int)(original.getWidth()*scale)),Math.max(1,(int)(original.getHeight()*scale)),false);
        original.resizeSubRectTo(0,0,original.getWidth(),original.getHeight(),result); return result;
    }
    public static CompletableFuture<NativeImage> load(Photo photo, boolean full) {
        return CompletableFuture.supplyAsync(() -> {
            Path original = photoFile(photo.id());
            try {
                Path source = !full && Files.isRegularFile(thumbnail(photo.id())) ? thumbnail(photo.id()) : original;
                dimensions(source);
                try (var input = Files.newInputStream(source); var image = NativeImage.read(input)) {
                    return resized(image,full ? 1920 : 160,full ? 1080 : 100);
                }
            } catch (IOException ex) { throw new java.util.concurrent.CompletionException(ex); }
        },Util.ioPool());
    }
    /** Encode only a validated Gallery file; resize/compress off the render thread. */
    public static CompletableFuture<byte[]> emailAttachment(Photo photo) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path path=photoFile(photo.id());dimensions(path);var original=ImageIO.read(path.toFile());
                if(original==null)throw new IOException("Cannot read photo");
                try {
                    int limit=net.mcreator.commandcookieselectronicmod.email.EmailPhoto.MAX_SIDE;
                    for(int attempt=0;attempt<5;attempt++,limit=Math.max(128,limit*3/4)){
                        double scale=Math.min(1,Math.min(limit/(double)original.getWidth(),limit/(double)original.getHeight()));
                        var small=new BufferedImage(Math.max(1,(int)(original.getWidth()*scale)),Math.max(1,(int)(original.getHeight()*scale)),BufferedImage.TYPE_INT_RGB);
                        var graphics=small.createGraphics();try{graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);graphics.drawImage(original,0,0,small.getWidth(),small.getHeight(),null);}finally{graphics.dispose();}
                        try {
                            for(float quality:new float[]{.85f,.65f,.45f}){
                                var bytes=new java.io.ByteArrayOutputStream();var writer=ImageIO.getImageWritersByFormatName("JPEG").next();
                                try(var output=new javax.imageio.stream.MemoryCacheImageOutputStream(bytes)){
                                    writer.setOutput(output);var params=writer.getDefaultWriteParam();params.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);params.setCompressionQuality(quality);writer.write(null,new javax.imageio.IIOImage(small,null,null),params);output.flush();
                                }finally{writer.dispose();}
                                if(bytes.size()<=net.mcreator.commandcookieselectronicmod.email.EmailPhoto.MAX_BYTES)return bytes.toByteArray();
                            }
                        }finally{small.flush();}
                    }
                    throw new IOException("Could not prepare photo");
                }finally{original.flush();}
            }catch(IOException ex){throw new java.util.concurrent.CompletionException(ex);}
        },Util.ioPool());
    }
    public static CompletableFuture<Void> delete(Photo photo) {
        return CompletableFuture.runAsync(() -> {
            Path file = photoFile(photo.id());
            try {
                checkFile(file);Path trash = folder().resolve("deleted");Files.createDirectories(trash);
                String name = System.currentTimeMillis()+"_"+file.getFileName();
                // A Gallery preview can briefly hold an image stream open on Windows.
                // Retry the move on the IO worker, retaining the original on failure.
                for(int attempt=0;;attempt++) {
                    try { Files.move(file,trash.resolve(name));break; }
                    catch(java.nio.file.FileSystemException ex) {
                        if(attempt>=5)throw ex;
                        try { Thread.sleep(40L*(attempt+1)); }
                        catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IOException("Photo deletion interrupted",interrupted);}
                    }
                }
                try { if (Files.isRegularFile(thumbnail(photo.id()))) Files.move(thumbnail(photo.id()),trash.resolve(name+".thumbnail.png")); }
                catch (IOException ignored) { /* The photo itself has already been removed from Gallery. */ }
            } catch (IOException ex) { throw new java.util.concurrent.CompletionException(ex); }
        },Util.ioPool());
    }
    public static CompletableFuture<Void> copy(Photo photo) {
        final com.sun.jna.Pointer owner;
        try { owner=PhoneImageClipboard.owner(); }
        catch(IOException ex){return CompletableFuture.failedFuture(ex);}
        return CompletableFuture.runAsync(() -> {
            try {
                Path path=photoFile(photo.id());dimensions(path);
                BufferedImage image=ImageIO.read(path.toFile());
                if (image==null) throw new IOException("Cannot read photo");
                PhoneImageClipboard.copy(owner,image,Files.readAllBytes(path));
            } catch (Exception ex) {
                throw new java.util.concurrent.CompletionException(ex);
            }
        },Util.ioPool());
    }
}
