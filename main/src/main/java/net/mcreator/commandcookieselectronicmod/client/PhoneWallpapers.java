package net.mcreator.commandcookieselectronicmod.client;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid="commandcookies_electronic_mod",value=Dist.CLIENT)
public final class PhoneWallpapers {
    public static final String[] NAMES={"Original","Gradient","Sunset","Mountains","Forest","Stars","Ocean","Circuit","Checker","Clouds","Neon","Stripes","Solid"};
    private static final PhonePreferences DEFAULT=new PhonePreferences();
    private record PhotoTexture(ResourceLocation id,int width,int height) {}
    private static final LinkedHashMap<String,PhotoTexture> PHOTOS=new LinkedHashMap<>(16,.75f,true);
    private static final HashSet<String> LOADING=new HashSet<>(),FAILED=new HashSet<>();
    private static int generation;
    private PhoneWallpapers() {}
    public static void click(net.minecraft.client.sounds.SoundManager manager,PhonePreferences prefs){click(manager,prefs,false);}
    public static void click(net.minecraft.client.sounds.SoundManager manager,PhonePreferences prefs,boolean shutter) {
        float volume=shutter?(prefs.silent?0:prefs.uiVolume/100f):prefs.clickVolume();
        if(volume>0)manager.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),shutter?1.6f:1f,volume));
    }
    public static void chime(PhonePreferences prefs) {
        if(!prefs.silent&&prefs.uiVolume>0)Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,.9f,prefs.uiVolume/100f));
    }
    public static PhonePreferences preferences(Screen screen) {
        if(screen instanceof IphoneScreen phone)return phone.preferences();
        if(screen instanceof ChromiumScreen browser)return browser.preferences();
        if(screen instanceof SecurityCameraScreen security)return security.preferences();
        if(screen instanceof PhoneMessagesScreen messages)return messages.preferences();
        if(screen instanceof PhoneSettingsScreen settings)return settings.preferences();
        if(screen instanceof PhoneCameraScreen camera)return camera.preferences();
        if(screen instanceof PhoneGalleryScreen gallery)return gallery.preferences();
        return DEFAULT;
    }
    private static int mix(int a,int b,double t) {
        t=Math.max(0,Math.min(1,t));int color=0xFF000000;
        for(int shift:new int[]{16,8,0})color|=(int)(((a>>shift)&255)*(1-t)+((b>>shift)&255)*t)<<shift;
        return color;
    }
    /** Original artworks on a 14x28 pixel canvas, including deterministic stars and circuit traces. */
    public static int[][] pixels(int style,int palette,int[][] original) {
        if(style==0&&original!=null)return original;
        int base=palette==0&&original!=null?original[10][6]:PhonePreferences.COLORS[palette],dark=mix(base,0xFF081628,.67),light=mix(base,0xFFF4DAA4,.55);
        int[][] out=new int[28][14];
        for(int y=0;y<28;y++)for(int x=0;x<14;x++) {
            int color=mix(light,dark,y/27.0);
            switch(style) {
                case 0 -> color=mix(base,dark,(y/6)*.15+(x>7?.15:0));
                case 1 -> color=mix(light,dark,(x+y)/40.0);
                case 2 -> {
                    color=mix(0xFFFFC27B,base,y/27.0);
                    if((x-9)*(x-9)+(y-9)*(y-9)<=9)color=0xFFFFE8AC;
                    if(y>=20+Math.abs(x-4)/3)color=dark;
                }
                case 3 -> {
                    if(y>=12+Math.abs(x-4))color=mix(base,0xFFBCC5C5,.3);
                    if(y>=17+Math.abs(x-10)/2)color=base;
                    if(y>=22+Math.abs(x-5)/3)color=dark;
                    if(y>=12+Math.abs(x-4)&&y<14+Math.abs(x-4))color=0xFFE8E8DD;
                }
                case 4 -> {
                    color=mix(light,0xFF294C40,.65);
                    for(int tree=0;tree<4;tree++) {
                        int tx=tree*4-1,top=8+(tree%2)*6;
                        if(y>=top&&y<=24&&Math.abs(x-tx)<=Math.min(4,(y-top)/3))color=tree%2==0?dark:base;
                        if(y>23&&x==tx)color=0xFF665947;
                    }
                }
                case 5 -> {
                    color=mix(dark,0xFF101326,.5);
                    if(Math.floorMod(x*53+y*37+x*y*7,67)==0)color=light;
                    if((x==9||x==10)&&y>=4&&y<=6)color=0xFFEADBA8;
                }
                case 6 -> {
                    color=mix(light,dark,y/27.0);
                    for(int wave=0;wave<4;wave++)if(y>=9+wave*5+(int)(2*Math.sin(x*.55+wave)))color=mix(base,dark,wave*.21);
                    if(y==9+(int)(2*Math.sin(x*.55)))color=0xFFBBE0DA;
                }
                case 7 -> {
                    color=dark;
                    if(x==3&&y>5&&y<21||y==7&&x>2&&x<11||x==10&&y>=7&&y<16||y==21&&x>=3&&x<8||x==7&&y>17)color=base;
                    if((x==3&&y==5)||(x==10&&y==16)||(x==7&&y==17))color=light;
                    if(x>=5&&x<=8&&y>=10&&y<=14)color=0xFF1B2835;
                    if(x==4&&y>=10&&y<=14&&y%2==0||x==9&&y>=10&&y<=14&&y%2==0)color=0xFFBCAC68;
                }
                case 8 -> color=((x/3+y/3)&1)==0?base:dark;
                case 9 -> {
                    color=mix(light,base,y/27.0);
                    if(y>=6&&y<=8&&x>=2&&x<=7||y==5&&x>=3&&x<=5||y>=16&&y<=18&&x>=8||y==15&&x>=10)color=0xFFEBEDE4;
                }
                case 10 -> {
                    color=0xFF0B1424;int line=Math.floorMod(x+y/2,9);
                    if(line==0||line==1)color=base;if(line==2)color=light;
                    if(y>20&&y%3==0)color=mix(color,base,.35);
                }
                case 11 -> color=mix(base,dark,Math.floorMod(x+y/2,6)/5.0);
                case 12 -> color=base;
            }
            out[y][x]=color;
        }return out;
    }
    public static void drawPixels(GuiGraphics g,int x,int y,int w,int h,int[][] image) {
        for(int row=0;row<28;row++)for(int col=0;col<14;col++)g.fill(x+col*w/14,y+row*h/28,x+(col+1)*w/14,y+(row+1)*h/28,image[row][col]);
    }
    private static PhotoTexture photo(String name) {
        PhotoTexture known=PHOTOS.get(name);if(known!=null)return known;
        if(LOADING.contains(name)||FAILED.contains(name))return null;LOADING.add(name);int revision=generation;
        Path file=PhonePhotos.folder().resolve(name+".png");
        PhonePhotos.load(new PhonePhotos.Photo(name,file,0,0,0),true).whenComplete((image,error) -> Minecraft.getInstance().execute(() -> {
            if(revision!=generation){if(image!=null)image.close();return;}
            LOADING.remove(name);if(error!=null){FAILED.add(name);return;}
            var manager=Minecraft.getInstance().getTextureManager();var id=manager.register("phone_wallpaper",new DynamicTexture(image));
            PHOTOS.put(name,new PhotoTexture(id,image.getWidth(),image.getHeight()));
            while(PHOTOS.size()>8){String oldest=PHOTOS.keySet().iterator().next();manager.release(PHOTOS.remove(oldest).id());}
        }));return null;
    }
    public static void draw(GuiGraphics g,int x,int y,int w,int h,PhonePreferences prefs,boolean lock,int[][] original) {
        boolean different=lock&&!prefs.sameLockWallpaper;
        int style=different?prefs.lockWallpaper:prefs.wallpaper,palette=different?prefs.lockWallpaperColor:prefs.wallpaperColor;
        String id=different?prefs.lockWallpaperPhoto:prefs.wallpaperPhoto;
        drawPixels(g,x,y,w,h,pixels(style,palette,original));
        if(!id.isEmpty()) {
            PhotoTexture texture=photo(id);
            if(texture!=null) {
                g.fill(x,y,x+w,y+h,0xFF111B24);
                double fit=prefs.photoFill?Math.max(w/(double)texture.width(),h/(double)texture.height()):Math.min(w/(double)texture.width(),h/(double)texture.height());
                int dw=Math.max(1,(int)Math.ceil(texture.width()*fit)),dh=Math.max(1,(int)Math.ceil(texture.height()*fit));
                g.enableScissor(x,y,x+w,y+h);
                g.blit(texture.id(),x+(w-dw)/2,y+(h-dh)/2,dw,dh,0f,0f,texture.width(),texture.height(),texture.width(),texture.height());g.disableScissor();
            }
        }
        int shade=(int)(255*(1-prefs.brightness/100f));if(!lock&&prefs.dimHome)shade=Math.min(230,shade+40);
        if(shade>0)g.fill(x,y,x+w,y+h,shade<<24);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        generation++;var manager=Minecraft.getInstance().getTextureManager();for(var texture:PHOTOS.values())manager.release(texture.id());PHOTOS.clear();LOADING.clear();FAILED.clear();
    }
}
