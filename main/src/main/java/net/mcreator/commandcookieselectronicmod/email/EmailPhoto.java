package net.mcreator.commandcookieselectronicmod.email;

import java.io.*;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;

/** Small JPEG attachments keep a complete send below Minecraft's 32 KiB client packet limit. */
public final class EmailPhoto {
    public static final int MAX_BYTES=24_576,MAX_SIDE=640,MAX_PHOTOS=2048,MAX_STORAGE=33_554_432;
    public record Size(int width,int height) {}
    public static Size inspect(byte[] bytes,boolean decode) throws IOException {
        if(bytes==null||bytes.length<4||bytes.length>MAX_BYTES||(bytes[0]&255)!=255||(bytes[1]&255)!=216)throw new IOException("Invalid photo attachment");
        try(var input=new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))){
            var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IOException("Unknown photo format");var reader=readers.next();
            try{
                if(!reader.getFormatName().equalsIgnoreCase("JPEG"))throw new IOException("Only JPEG attachments are accepted");
                reader.setInput(input,true,true);int w=reader.getWidth(0),h=reader.getHeight(0);
                if(w<1||h<1||w>MAX_SIDE||h>MAX_SIDE)throw new IOException("Photo dimensions exceed limit");
                if(decode){var image=reader.read(0);if(image==null||image.getWidth()!=w||image.getHeight()!=h)throw new IOException("Unreadable photo");image.flush();}
                return new Size(w,h);
            }finally{reader.dispose();}
        }catch(RuntimeException ex){throw new IOException("Unreadable photo",ex);}
    }
    private EmailPhoto() {}
}
