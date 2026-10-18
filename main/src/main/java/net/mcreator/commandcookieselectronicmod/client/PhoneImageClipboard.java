package net.mcreator.commandcookieselectronicmod.client;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFWNativeWin32;

/** Minecraft runs AWT in headless mode; use Windows' native image clipboard instead. */
final class PhoneImageClipboard {
    public interface User extends StdCallLibrary {
        boolean OpenClipboard(Pointer owner);
        boolean CloseClipboard();
        boolean EmptyClipboard();
        Pointer SetClipboardData(int format,Pointer data);
        Pointer GetClipboardData(int format);
        int EnumClipboardFormats(int format);
        int RegisterClipboardFormatW(WString name);
    }
    public interface Kernel extends StdCallLibrary {
        Pointer GlobalAlloc(int flags,BaseTSD.SIZE_T bytes);
        Pointer GlobalLock(Pointer memory);
        boolean GlobalUnlock(Pointer memory);
        Pointer GlobalFree(Pointer memory);
        BaseTSD.SIZE_T GlobalSize(Pointer memory);
    }
    static final class Windows {
        static final User USER=Native.load("user32",User.class,W32APIOptions.UNICODE_OPTIONS);
        static final Kernel KERNEL=Native.load("kernel32",Kernel.class,W32APIOptions.UNICODE_OPTIONS);
    }
    private PhoneImageClipboard() {}
    static Pointer owner() throws IOException {
        if(!com.sun.jna.Platform.isWindows())throw new IOException("Image clipboard currently requires Windows");
        return new Pointer(GLFWNativeWin32.glfwGetWin32Window(Minecraft.getInstance().getWindow().getWindow()));
    }
    static Pointer allocate(byte[] bytes)throws IOException {
        Pointer handle=Windows.KERNEL.GlobalAlloc(0x0002,new BaseTSD.SIZE_T(bytes.length));
        if(handle==null)throw new IOException("Clipboard allocation failed");
        Pointer memory=Windows.KERNEL.GlobalLock(handle);
        if(memory==null){Windows.KERNEL.GlobalFree(handle);throw new IOException("Clipboard allocation lock failed");}
        try{memory.write(0,bytes,0,bytes.length);}finally{Windows.KERNEL.GlobalUnlock(handle);}return handle;
    }
    static void open(Pointer owner)throws IOException {
        for(int attempt=0;attempt<5;attempt++) {
            if(Windows.USER.OpenClipboard(owner))return;
            try{Thread.sleep(60);}catch(InterruptedException ex){Thread.currentThread().interrupt();throw new IOException("Clipboard interrupted",ex);}
        }
        throw new IOException("Clipboard is busy");
    }
    /** Transfers ownership of accepted handles to Windows and frees every unused allocation. */
    static void publish(Pointer owner,Map<Integer,Pointer> handles)throws IOException {
        boolean opened=false;
        try {
            open(owner);opened=true;
            if(!Windows.USER.EmptyClipboard())throw new IOException("Cannot clear clipboard");
            for(var entry:handles.entrySet()) {
                if(Windows.USER.SetClipboardData(entry.getKey(),entry.getValue())==null)throw new IOException("Cannot write clipboard image");
                entry.setValue(null);
            }
        } finally {
            if(opened)Windows.USER.CloseClipboard();
            for(Pointer unused:handles.values())if(unused!=null)Windows.KERNEL.GlobalFree(unused);
        }
    }
    static void copy(Pointer owner,BufferedImage image,byte[] png)throws IOException {
        int width=image.getWidth(),height=image.getHeight();long bytes=40L+width*(long)height*4;
        var handles=new LinkedHashMap<Integer,Pointer>();
        Pointer bitmap=Windows.KERNEL.GlobalAlloc(0x0042,new BaseTSD.SIZE_T(bytes));
        if(bitmap==null)throw new IOException("Clipboard bitmap allocation failed");
        handles.put(8,bitmap);boolean handedToPublish=false;
        try {
            Pointer pixels=Windows.KERNEL.GlobalLock(bitmap);
            if(pixels==null)throw new IOException("Clipboard bitmap lock failed");
            try {
                // BITMAPINFOHEADER + opaque BGRA pixels, stored from bottom to top (CF_DIB).
                pixels.setInt(0,40);pixels.setInt(4,width);pixels.setInt(8,height);
                pixels.setShort(12,(short)1);pixels.setShort(14,(short)32);pixels.setInt(20,width*height*4);
                int[] row=new int[width];
                for(int y=0;y<height;y++) {
                    image.getRGB(0,y,width,1,row,0,width);
                    for(int x=0;x<width;x++)row[x]|=0xFF000000;
                    pixels.write(40L+(height-1L-y)*width*4,row,0,width);
                }
            } finally {Windows.KERNEL.GlobalUnlock(bitmap);}
            int pngFormat=Windows.USER.RegisterClipboardFormatW(new WString("PNG"));
            if(pngFormat!=0)handles.put(pngFormat,allocate(png));
            handedToPublish=true;publish(owner,handles);
        } finally {
            if(!handedToPublish)for(Pointer unused:handles.values())if(unused!=null)Windows.KERNEL.GlobalFree(unused);
        }
    }
}
