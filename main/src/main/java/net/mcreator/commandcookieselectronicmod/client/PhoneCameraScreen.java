package net.mcreator.commandcookieselectronicmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid="commandcookies_electronic_mod",value=Dist.CLIENT)
public final class PhoneCameraScreen extends Screen {
    private final Screen back;
    private final String brand;
    private final PhonePreferences prefs;
    private boolean previousHud, viewActive, pending, busy, aiming;
    private CameraType previousCamera;
    private int zoom=1;
    private long flashUntil, shootAt;
    private String status="Drag to aim. Scroll to zoom. Space takes a photo.";
    private PhonePhotoButton shutter;
    public PhoneCameraScreen(Screen back,String brand) { super(Component.literal("Phone Camera"));this.back=back;this.brand=brand;this.prefs=PhoneWallpapers.preferences(back);zoom=prefs.cameraZoom; }
    public PhonePreferences preferences(){return prefs;}
    @Override protected void init() {
        if (!viewActive) {
            previousHud=minecraft.options.hideGui;previousCamera=minecraft.options.getCameraType();
            minecraft.options.hideGui=true;minecraft.options.setCameraType(CameraType.FIRST_PERSON);viewActive=true;
        }
        addRenderableWidget(new PhonePhotoButton(12,12,52,22,"Back",0xDD243140,this::onClose));
        addRenderableWidget(new PhonePhotoButton(width-84,12,72,22,"Gallery",0xDD32485A,() -> minecraft.setScreen(new PhoneGalleryScreen(this,back,brand))));
        int[] values={1,2,4};
        for(int i=0;i<values.length;i++) {
            int z=values[i];addRenderableWidget(new PhonePhotoButton(width/2-55+i*38,height-69,34,20,z+"x",zoom==z?0xFF68857B:0xDD273A47,() -> setZoom(z)));
        }
        shutter=addRenderableWidget(new PhonePhotoButton(width/2-58,height-43,116,25,"Take photo",0xFFF0EEE0,this::shoot));
        shutter.active=!busy&&!pending;
    }
    private void setZoom(int value) {zoom=value;clearWidgets();init();}
    public void shoot() {
        if (busy || pending || minecraft.player==null || minecraft.level==null) return;
        pending=true;shootAt=System.currentTimeMillis()+prefs.cameraTimer*1000L;shutter.active=false;status=prefs.cameraTimer==0?"Taking photo...":"Timer started...";
    }
    @SubscribeEvent public static void fov(ViewportEvent.ComputeFov event) {
        if (event.usedConfiguredFov() && Minecraft.getInstance().screen instanceof PhoneCameraScreen camera)
            event.setFOV(Math.toDegrees(2*Math.atan(Math.tan(Math.toRadians(event.getFOV())/2)/camera.zoom)));
    }
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float tick) {}
    @Override public void render(GuiGraphics g,int mx,int my,float tick) {
        // The world has rendered; HUD and held item are hidden. Capture before any camera controls.
        if (pending && System.currentTimeMillis() >= shootAt && minecraft.level!=null && minecraft.getOverlay()==null) {
            pending=false;busy=true;g.flush();
            try {
                var image=Screenshot.takeScreenshot(minecraft.getMainRenderTarget());
                if(prefs.photoQuality>0) {
                    int divisor=prefs.photoQuality==1?2:4;
                    var reduced=new com.mojang.blaze3d.platform.NativeImage(Math.max(1,image.getWidth()/divisor),Math.max(1,image.getHeight()/divisor),false);
                    image.resizeSubRectTo(0,0,image.getWidth(),image.getHeight(),reduced);image.close();image=reduced;
                }
                flashUntil=prefs.cameraFlash?System.currentTimeMillis()+120:0;status="Saving photo...";
                if(prefs.shutterSound)PhoneWallpapers.click(minecraft.getSoundManager(),prefs,true);
                PhonePhotos.save(image).whenComplete((photo,error) -> minecraft.execute(() -> {
                    busy=false;status=error==null?"Photo saved. Open Gallery to view or copy it.":"Could not save photo. Try again.";
                    if(error==null&&prefs.saveChime)PhoneWallpapers.chime(prefs);
                    if(shutter!=null)shutter.active=true;
                }));
            } catch(Exception ex){busy=false;shutter.active=true;status="Could not capture photo. Try again.";}
        }
        g.fill(0,0,width,45,0xB0151D29);g.fill(0,height-82,width,height,0xCB111B28);
        g.drawCenteredString(font,brand+" Camera",width/2,15,0xFFECF4EF);
        int vx=18,vy=54,vw=width-36,vh=height-145;
        if(vh>20) {
            if(prefs.cameraGrid) for(int i=1;i<3;i++) {
                g.fill(vx+vw*i/3,vy,vx+vw*i/3+1,vy+vh,0x448ED3D1);
                g.fill(vx,vy+vh*i/3,vx+vw,vy+vh*i/3+1,0x448ED3D1);
            }
            for(int dx:new int[]{vx,vx+vw-12})for(int dy:new int[]{vy,vy+vh-12}) {
                g.fill(dx,dy,dx+12,dy+2,0xFFE1EDE7);g.fill(dx,dy,dx+2,dy+12,0xFFE1EDE7);
            }
        }
        if(pending && prefs.cameraTimer>0) {
            String count=Long.toString(Math.max(1,(shootAt-System.currentTimeMillis()+999)/1000));
            g.pose().pushPose();g.pose().translate(width/2,height/2-12,0);g.pose().scale(3,3,1);
            g.drawString(font,count,-font.width(count)/2,0,0xFFFFFFFF,true);g.pose().popPose();
        }
        g.drawCenteredString(font,font.plainSubstrByWidth(status,width-20),width/2,height-11,0xFFBDCCC9);
        super.render(g,mx,my,tick);
        if(System.currentTimeMillis()<flashUntil)g.fill(0,0,width,height,0xA8FFFFFF);
    }
    @Override public boolean mouseClicked(double mx,double my,int button) {
        if(super.mouseClicked(mx,my,button))return true;
        if(button==0 && my>45 && my<height-82){aiming=true;return true;}return false;
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy) {
        if(aiming&&button==0&&minecraft.player!=null){minecraft.player.turn(dx*2.5*prefs.aimScale()/zoom,dy*2.5*prefs.aimScale()*(prefs.invertAim?-1:1)/zoom);return true;}
        return super.mouseDragged(mx,my,button,dx,dy);
    }
    @Override public boolean mouseReleased(double mx,double my,int button){aiming=false;return super.mouseReleased(mx,my,button);}
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        if(vertical!=0)setZoom(vertical>0?Math.min(4,zoom*2):Math.max(1,zoom/2));return true;
    }
    @Override public boolean keyPressed(int key,int scan,int mods) {
        if(key==GLFW.GLFW_KEY_SPACE||key==GLFW.GLFW_KEY_ENTER){shoot();return true;}
        if(minecraft.player!=null){
            if(key==GLFW.GLFW_KEY_LEFT||key==GLFW.GLFW_KEY_RIGHT){minecraft.player.turn((key==GLFW.GLFW_KEY_LEFT?-40:40)*prefs.aimScale()/zoom,0);return true;}
            if(key==GLFW.GLFW_KEY_UP||key==GLFW.GLFW_KEY_DOWN){minecraft.player.turn(0,(key==GLFW.GLFW_KEY_UP?-40:40)*prefs.aimScale()*(prefs.invertAim?-1:1)/zoom);return true;}
        }
        return super.keyPressed(key,scan,mods);
    }
    @Override public void removed() {
        if(viewActive){minecraft.options.hideGui=previousHud;minecraft.options.setCameraType(previousCamera);viewActive=false;}
        pending=false;aiming=false;
    }
    @Override public void onClose(){minecraft.setScreen(back);}
    @Override public boolean isPauseScreen(){return false;}
}
