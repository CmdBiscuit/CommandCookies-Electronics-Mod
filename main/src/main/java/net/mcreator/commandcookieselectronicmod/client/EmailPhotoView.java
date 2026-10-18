package net.mcreator.commandcookieselectronicmod.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.commandcookieselectronicmod.email.EmailData;
import net.mcreator.commandcookieselectronicmod.email.EmailPhoto;
import net.mcreator.commandcookieselectronicmod.network.EmailPackets;

/** Screen-scoped, bounded cache; inbox snapshots never include photo bytes. */
final class EmailPhotoView {
    private record Picture(ResourceLocation texture,int width,int height,byte[] bytes) {}
    private final LinkedHashMap<UUID,Picture> pictures=new LinkedHashMap<>(16,.75f,true);
    private UUID waiting;private long request,started;private int generation;private boolean active=true;
    private String error="";
    boolean loaded(UUID id){return pictures.containsKey(id);}
    void request(BlockPos pos,UUID device,UUID session,EmailData.Entry entry){
        if(!active||loaded(entry.id()))return;
        if(entry.id().equals(waiting)&&System.nanoTime()-started<8_000_000_000L)return;
        waiting=entry.id();started=System.nanoTime();error="";
        request=EmailClient.request(pos,device,session,EmailPackets.PHOTO,"","","",entry.id(),entry.outgoing());
    }
    void accept(EmailPackets.Photo packet){
        if(!active||packet.request()!=request||!packet.message().equals(waiting))return;
        if(!packet.status().isEmpty()){error=packet.status();waiting=null;return;}
        byte[] bytes=packet.bytes();UUID id=packet.message();int revision=generation;
        CompletableFuture.supplyAsync(()->{
            try{return decode(bytes);}
            catch(Exception ex){throw new java.util.concurrent.CompletionException(ex);}
        },Util.ioPool()).whenComplete((image,ex)->Minecraft.getInstance().execute(()->{
            if(!active||revision!=generation){if(image!=null)image.close();return;}
            if(id.equals(waiting))waiting=null;
            if(ex!=null){error="Could not display this photo.";return;}
            var manager=Minecraft.getInstance().getTextureManager();var texture=manager.register("email_photo",new DynamicTexture(image));
            var old=pictures.put(id,new Picture(texture,image.getWidth(),image.getHeight(),bytes));if(old!=null)manager.release(old.texture());
            while(pictures.size()>16){UUID oldest=pictures.keySet().iterator().next();manager.release(pictures.remove(oldest).texture());}
        }));
    }
    void draw(GuiGraphics g,UUID id,int x,int y,int w,int h){
        g.fill(x,y,x+w,y+h,0xFF08131A);var p=pictures.get(id);
        if(p==null){g.drawCenteredString(Minecraft.getInstance().font,error.isEmpty()?System.nanoTime()-started>8_000_000_000L?"Photo unavailable. Use Retry.":"Loading photo...":error,x+w/2,y+h/2-4,0xFF9BAFB9);return;}
        double fit=Math.min(w/(double)p.width(),h/(double)p.height());int dw=Math.max(1,(int)(p.width()*fit)),dh=Math.max(1,(int)(p.height()*fit));
        g.blit(p.texture(),x+(w-dw)/2,y+(h-dh)/2,dw,dh,0f,0f,p.width(),p.height(),p.width(),p.height());
    }
    void retry(BlockPos pos,UUID device,UUID session,EmailData.Entry entry){waiting=null;request(pos,device,session,entry);}
    CompletableFuture<PhonePhotos.Photo> save(UUID id){var p=pictures.get(id);if(p==null)return CompletableFuture.failedFuture(new java.io.IOException("Photo not loaded"));byte[] bytes=p.bytes();return CompletableFuture.supplyAsync(()->{try{return decode(bytes);}catch(Exception ex){throw new java.util.concurrent.CompletionException(ex);}},Util.ioPool()).thenCompose(PhonePhotos::save);}
    private static NativeImage decode(byte[] bytes)throws java.io.IOException{
        var size=EmailPhoto.inspect(bytes,false);var source=javax.imageio.ImageIO.read(new ByteArrayInputStream(bytes));
        if(source==null)throw new java.io.IOException("Photo not readable");
        try{
            if(source.getWidth()!=size.width()||source.getHeight()!=size.height())throw new java.io.IOException("Photo dimensions changed");
            var image=new NativeImage(size.width(),size.height(),false);
            try{for(int y=0;y<size.height();y++)for(int x=0;x<size.width();x++){int c=source.getRGB(x,y);image.setPixelRGBA(x,y,0xFF000000|(c&255)<<16|(c&0xFF00)|(c>>16&255));}return image;}
            catch(Throwable ex){image.close();throw ex;}
        }finally{source.flush();}
    }
    void release(){active=false;generation++;for(var p:pictures.values())Minecraft.getInstance().getTextureManager().release(p.texture());pictures.clear();waiting=null;}
}
