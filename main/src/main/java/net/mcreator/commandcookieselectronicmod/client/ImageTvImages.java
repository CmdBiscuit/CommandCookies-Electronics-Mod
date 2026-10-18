package net.mcreator.commandcookieselectronicmod.client;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.commandcookieselectronicmod.tv.ImgurLink;

/** Downloads and decoding run on workers. Visible, powered TVs upload only changed frames. */
public final class ImageTvImages {
    private static final int MAX_BYTES=TvImageDecoder.MAX_BYTES,MAX_CACHE_PIXELS=32*1024*1024;
    private static final ExecutorService WORKERS=new ThreadPoolExecutor(2,2,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(32),r->{var t=new Thread(r,"CEM TV images");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    private static final LinkedHashMap<String,Entry> CACHE=new LinkedHashMap<>(16,.75f,true);
    private static int generation;
    private static final class Frames implements AutoCloseable {
        final TvImageDecoder.Image timing;
        final NativeImage[] images;
        Frames(TvImageDecoder.Image data){
            timing=data;images=new NativeImage[data.pixels().length];
            try{for(int index=0;index<images.length;index++){
                var image=new NativeImage(data.width(),data.height(),false);images[index]=image;int[] pixels=data.pixels()[index];
                for(int y=0,offset=0;y<data.height();y++)for(int x=0;x<data.width();x++)image.setPixelRGBA(x,y,pixels[offset++]);
                data.pixels()[index]=null;
            }}catch(Throwable failure){close();throw failure;}
        }
        long pixels(){return (long)timing.width()*timing.height()*(images.length+1);}
        @Override public void close(){for(int i=0;i<images.length;i++)if(images[i]!=null){images[i].close();images[i]=null;}}
    }
    public static final class Entry {
        public ResourceLocation texture;
        public int width,height;
        public String error;
        private boolean loading;
        private long retryAt,started;
        private int frame;
        private Frames frames;
        private DynamicTexture dynamic;
        private final String url;
        Entry(String url){this.url=url;}
        public int frameCount(){return frames==null?0:frames.images.length;}
        private void advance(){
            if(dynamic==null||frames==null||frames.images.length==1)return;
            int next=frames.timing.frameAt((System.nanoTime()-started)/1_000_000);
            if(next!=frame){dynamic.getPixels().copyFrom(frames.images[next]);dynamic.upload();frame=next;}
        }
    }
    public static Entry get(String url){
        if(url==null||url.isEmpty())return null;
        var entry=CACHE.get(url);
        if(entry==null){entry=new Entry(url);CACHE.put(url,entry);start(entry,false);}
        else if(entry.texture==null&&!entry.loading&&System.nanoTime()>=entry.retryAt)start(entry,false);
        entry.advance();return entry;
    }
    public static void retry(String url){if(url.isEmpty())return;release(CACHE.remove(url));var entry=new Entry(url);CACHE.put(url,entry);start(entry,true);}
    private static void release(Entry entry){
        if(entry==null)return;
        if(entry.texture!=null){Minecraft.getInstance().getTextureManager().release(entry.texture);entry.texture=null;}
        entry.dynamic=null;if(entry.frames!=null){entry.frames.close();entry.frames=null;}
    }
    private static void start(Entry entry,boolean fresh){
        entry.loading=true;entry.error=null;int revision=generation;
        while(CACHE.size()>32){var key=CACHE.keySet().iterator().next();release(CACHE.remove(key));}
        final CompletableFuture<Frames> job;
        try{job=CompletableFuture.supplyAsync(()->{try{return load(entry.url,fresh);}catch(Exception failure){throw new CompletionException(failure);}},WORKERS);}catch(RejectedExecutionException busy){entry.loading=false;entry.error="Other TV images are loading. Try again shortly.";entry.retryAt=System.nanoTime()+10_000_000_000L;return;}
        job.whenComplete((frames,failure)->Minecraft.getInstance().execute(()->{
            if(revision!=generation||CACHE.get(entry.url)!=entry){if(frames!=null)frames.close();return;}
            entry.loading=false;
            if(failure!=null){entry.error=message(failure);entry.retryAt=System.nanoTime()+120_000_000_000L;return;}
            entry.width=frames.timing.width();entry.height=frames.timing.height();entry.frames=frames;
            var pixels=new NativeImage(entry.width,entry.height,false);
            try{
                pixels.copyFrom(frames.images[0]);entry.dynamic=new DynamicTexture(pixels);entry.dynamic.setFilter(true,false);
                entry.texture=Minecraft.getInstance().getTextureManager().register("tv_image",entry.dynamic);entry.started=System.nanoTime();entry.frame=0;
            }catch(RuntimeException uploadFailure){if(entry.dynamic!=null)entry.dynamic.close();else pixels.close();entry.dynamic=null;frames.close();entry.frames=null;entry.error="Could not display this image.";entry.retryAt=System.nanoTime()+120_000_000_000L;return;}
            long used=CACHE.values().stream().filter(e->e.frames!=null).mapToLong(e->e.frames.pixels()).sum();
            for(var it=CACHE.entrySet().iterator();used>MAX_CACHE_PIXELS&&it.hasNext();){var old=it.next().getValue();if(old==entry||old.frames==null)continue;used-=old.frames.pixels();release(old);it.remove();}
        }));
    }
    private static String message(Throwable failure){while(failure instanceof CompletionException&&failure.getCause()!=null)failure=failure.getCause();return failure instanceof IOException&&failure.getMessage()!=null?failure.getMessage():"Could not display this image. Check the Imgur link.";}
    private static byte[] download(String value)throws IOException {
        String url=ImgurLink.normalize(value);
        for(int redirects=0;redirects<4;redirects++){
            if(url.equals("https://i.imgur.com/removed.png"))throw new IOException("Imgur could not find that image.");
            var connection=(HttpURLConnection)URI.create(url).toURL().openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(8000);connection.setReadTimeout(8000);connection.setRequestProperty("User-Agent","CEM-Image-TV/1.0");connection.setRequestProperty("Accept","image/png,image/jpeg,image/gif");
            try{
                int code=connection.getResponseCode();
                if(code>=300&&code<400){String location=connection.getHeaderField("Location");if(location==null)throw new IOException("Imgur returned an incomplete redirect.");url=ImgurLink.normalize(URI.create(url).resolve(location).toString());continue;}
                if(code!=200)throw new IOException(code==429?"Imgur is busy. Try reloading the image later.":"Imgur could not load that image (HTTP "+code+").");
                if(connection.getContentLengthLong()>MAX_BYTES)throw new IOException("Choose an image smaller than 8 MB.");
                String type=connection.getContentType();if(type!=null&&!type.toLowerCase(Locale.ROOT).startsWith("image/"))throw new IOException("That Imgur link did not return an image.");
                try(var in=connection.getInputStream();var out=new ByteArrayOutputStream()){
                    byte[] buffer=new byte[16384];int length;long deadline=System.nanoTime()+20_000_000_000L;
                    while((length=in.read(buffer))!=-1){if(out.size()+length>MAX_BYTES)throw new IOException("Choose an image smaller than 8 MB.");if(System.nanoTime()>deadline)throw new IOException("The image download took too long. Try again.");out.write(buffer,0,length);}return out.toByteArray();
                }
            }finally{connection.disconnect();}
        }
        throw new IOException("Imgur redirected this image too many times.");
    }

    private static Frames load(String url,boolean fresh)throws Exception {
        url=ImgurLink.normalize(url);
        Path root=Minecraft.getInstance().gameDirectory.toPath().resolve("cem_tv_images");Files.createDirectories(root);
        if(!root.toRealPath().startsWith(Minecraft.getInstance().gameDirectory.toPath().toRealPath()))throw new IOException("The TV image cache is unavailable.");
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(url.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        // The original bytes preserve GIF frames and timing. Old PNG-only cache entries are replaced on first use.
        Path file=root.resolve(hash+".image");TvImageDecoder.Image data=null;
        if(!fresh&&Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)&&Files.size(file)<=MAX_BYTES){try{data=TvImageDecoder.decode(Files.readAllBytes(file));Files.setLastModifiedTime(file,java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()));}catch(IOException ignored){Files.deleteIfExists(file);}}
        if(data==null){
            byte[] bytes=download(url);data=TvImageDecoder.decode(bytes);Path temp=Files.createTempFile(root,"tv-",".tmp");
            try{Files.write(temp,bytes);Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);trim(root);}finally{Files.deleteIfExists(temp);}
            Files.deleteIfExists(root.resolve(hash+".png"));
        }
        return new Frames(data);
    }
    private static void trim(Path root)throws IOException {
        try(var stream=Files.list(root)){
            var files=stream.filter(p->p.getFileName().toString().matches("[a-f0-9]{64}\\.(png|image)")&&Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)).sorted(Comparator.comparingLong(p->{try{return Files.getLastModifiedTime(p).toMillis();}catch(IOException e){return 0;}})).toList();
            long bytes=0;for(var p:files)bytes+=Files.size(p);int count=files.size();
            for(var p:files){if(bytes<=128L*1024*1024&&count<=64)break;bytes-=Files.size(p);Files.deleteIfExists(p);count--;}
        }
    }
    public static void clear(){generation++;for(var entry:CACHE.values())release(entry);CACHE.clear();}
    private ImageTvImages(){}
}
