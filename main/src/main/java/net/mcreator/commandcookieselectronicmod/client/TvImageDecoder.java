package net.mcreator.commandcookieselectronicmod.client;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.w3c.dom.Node;

/** Image decoding stays on the image workers, independently of rendering. */
public final class TvImageDecoder {
    public static final int MAX_BYTES=8*1024*1024,MAX_FRAMES=600,FRAME_PIXELS=8*1024*1024;
    private static final int MAX_PIXELS=16*1024*1024;
    public record Image(int width,int height,int[][] pixels,int[] ends,int plays) {
        public int frameAt(long elapsedMillis){
            if(pixels.length==1)return 0;
            long duration=ends[ends.length-1],elapsed=Math.max(0,elapsedMillis);
            if(plays>0&&elapsed>=duration*plays)return pixels.length-1;
            int time=(int)(elapsed%duration),low=0,high=ends.length-1;
            while(low<high){int mid=(low+high)>>>1;if(time<ends[mid])high=mid;else low=mid+1;}return low;
        }
    }
    private record Control(int x,int y,int width,int height,String disposal,int delay,boolean transparent,int transparentIndex){}
    public static Image decode(byte[] bytes)throws IOException {
        if(bytes.length>MAX_BYTES)throw new IOException("Choose an image smaller than 8 MB.");
        try(var input=new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))){
            var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IOException("Use a PNG, JPG or GIF image.");var reader=readers.next();
            try{
                String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                if(!Set.of("png","jpg","jpeg","gif").contains(format))throw new IOException("Use a PNG, JPG or GIF image.");
                reader.setInput(input,false,false);
                return format.equals("gif")?gif(reader):still(reader);
            }catch(IndexOutOfBoundsException|IllegalArgumentException error){throw new IOException("This image could not be decoded.",error);}finally{reader.dispose();}
        }
    }
    private static void dimensions(int width,int height)throws IOException{
        if(width<1||height<1||width>8192||height>8192||(long)width*height>MAX_PIXELS)throw new IOException("Choose an image up to 16 megapixels and 8192 pixels per side.");
    }
    private static Image still(ImageReader reader)throws IOException {
        int width=reader.getWidth(0),height=reader.getHeight(0);dimensions(width,height);
        int sample=Math.max(1,(int)Math.ceil(Math.max(width,height)/2048d));var param=reader.getDefaultReadParam();param.setSourceSubsampling(sample,sample,0,0);
        var image=reader.read(0,param);if(image==null)throw new IOException("This image could not be decoded.");
        try{return new Image(image.getWidth(),image.getHeight(),new int[][]{pixels(image)},new int[]{100},1);}finally{image.flush();}
    }
    private static Image gif(ImageReader reader)throws IOException {
        long deadline=System.nanoTime()+15_000_000_000L;
        Node stream=reader.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0"),logical=child(stream,"LogicalScreenDescriptor");
        int width=number(logical,"logicalScreenWidth",reader.getWidth(0)),height=number(logical,"logicalScreenHeight",reader.getHeight(0));dimensions(width,height);
        int count=0;
        while(count<=MAX_FRAMES){try{reader.getWidth(count);count++;}catch(IndexOutOfBoundsException end){break;}if(System.nanoTime()>deadline)throw new IOException("This GIF took too long to decode. Try a smaller animation.");}
        if(count>MAX_FRAMES)throw new IOException("Use a shorter GIF (up to 600 frames).");
        if(count==0)throw new IOException("This GIF has no frames.");
        double scale=Math.min(1,Math.min((count==1?2048d:1024d)/Math.max(width,height),Math.sqrt(FRAME_PIXELS/((double)width*height*count))));
        int outputWidth=Math.max(1,(int)Math.floor(width*scale)),outputHeight=Math.max(1,(int)Math.floor(height*scale));
        Node palette=child(stream,"GlobalColorTable");int backgroundIndex=number(palette,"backgroundColorIndex",-1),background=0;
        if(palette!=null)for(Node e=palette.getFirstChild();e!=null;e=e.getNextSibling())if(e.getNodeName().equals("ColorTableEntry")&&number(e,"index",-2)==backgroundIndex){background=0xFF000000|number(e,"red",0)<<16|number(e,"green",0)<<8|number(e,"blue",0);break;}
        var canvas=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);var graphics=canvas.createGraphics();BufferedImage previous=null;
        int[][] frames=new int[count][];int[] ends=new int[count];int plays=1,total=0;Control last=null;
        try{
            for(int index=0;index<count;index++){
                if(System.nanoTime()>deadline)throw new IOException("This GIF took too long to decode. Try a smaller animation.");
                Node metadata=reader.getImageMetadata(index).getAsTree("javax_imageio_gif_image_1.0");Control control=control(metadata,reader,index);
                if(control.x<0||control.y<0||control.width<1||control.height<1||(long)control.x+control.width>width||(long)control.y+control.height>height)throw new IOException("This GIF has invalid frame dimensions.");
                if(index==0){fill(graphics,0,0,width,height,control.transparent?0:background);plays=plays(metadata);}
                if(last!=null){
                    if(last.disposal.equals("restoreToBackgroundColor"))fill(graphics,last.x,last.y,last.width,last.height,last.transparent?0:background);
                    else if(last.disposal.equals("restoreToPrevious")&&previous!=null){graphics.setComposite(AlphaComposite.Src);graphics.drawImage(previous,0,0,null);}
                }
                if(previous!=null){previous.flush();previous=null;}
                if(control.disposal.equals("restoreToPrevious")){previous=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);var copy=previous.createGraphics();try{copy.setComposite(AlphaComposite.Src);copy.drawImage(canvas,0,0,null);}finally{copy.dispose();}}
                var frame=reader.read(index);if(frame==null)throw new IOException("A GIF frame could not be decoded.");
                try{graphics.setComposite(AlphaComposite.SrcOver);graphics.drawImage(frame,control.x,control.y,null);}finally{frame.flush();}
                if(outputWidth==width&&outputHeight==height)frames[index]=pixels(canvas);
                else{var scaled=new BufferedImage(outputWidth,outputHeight,BufferedImage.TYPE_INT_ARGB);var g=scaled.createGraphics();try{g.setComposite(AlphaComposite.Src);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(canvas,0,0,outputWidth,outputHeight,null);frames[index]=pixels(scaled);}finally{g.dispose();scaled.flush();}}
                total+=control.delay;ends[index]=total;last=control;
            }
            return new Image(outputWidth,outputHeight,frames,ends,plays);
        }finally{graphics.dispose();canvas.flush();if(previous!=null)previous.flush();}
    }
    private static Control control(Node metadata,ImageReader reader,int index)throws IOException {
        Node descriptor=child(metadata,"ImageDescriptor"),gce=child(metadata,"GraphicControlExtension");int delay=number(gce,"delayTime",0)*10;if(delay<20)delay=100;
        return new Control(number(descriptor,"imageLeftPosition",0),number(descriptor,"imageTopPosition",0),number(descriptor,"imageWidth",reader.getWidth(index)),number(descriptor,"imageHeight",reader.getHeight(index)),attribute(gce,"disposalMethod","none"),delay,attribute(gce,"transparentColorFlag","FALSE").equalsIgnoreCase("TRUE"),number(gce,"transparentColorIndex",-1));
    }
    private static void fill(Graphics2D graphics,int x,int y,int width,int height,int argb){graphics.setComposite(AlphaComposite.Src);graphics.setColor(new Color(argb,true));graphics.fillRect(x,y,width,height);}
    private static int plays(Node metadata){
        Node applications=child(metadata,"ApplicationExtensions");if(applications==null)return 1;
        for(Node node=applications.getFirstChild();node!=null;node=node.getNextSibling()){
            String id=attribute(node,"applicationID","");
            if((id.equals("NETSCAPE")||id.equals("ANIMEXTS"))&&node instanceof IIOMetadataNode element&&element.getUserObject() instanceof byte[] data&&data.length>=3&&data[0]==1){int repeats=(data[1]&255)|(data[2]&255)<<8;return repeats==0?0:repeats+1;}
        }
        return 1;
    }
    private static int[] pixels(BufferedImage image){
        int[] data=image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth());
        for(int i=0;i<data.length;i++){int c=data[i],a=c>>>24,r=(c>>>16&255)*a/255,g=(c>>>8&255)*a/255,b=(c&255)*a/255;data[i]=0xFF000000|b<<16|g<<8|r;}return data;
    }
    private static Node child(Node root,String name){if(root==null)return null;for(Node node=root.getFirstChild();node!=null;node=node.getNextSibling())if(node.getNodeName().equals(name))return node;return null;}
    private static String attribute(Node node,String name,String fallback){if(node==null||node.getAttributes()==null)return fallback;var value=node.getAttributes().getNamedItem(name);return value==null?fallback:value.getNodeValue();}
    private static int number(Node node,String name,int fallback){try{return Integer.parseInt(attribute(node,name,""));}catch(NumberFormatException e){return fallback;}}
    private TvImageDecoder(){}
}
