package net.mcreator.commandcookieselectronicmod.client;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public final class PhoneGalleryScreen extends Screen {
    private record Texture(ResourceLocation id,int width,int height) {}
    private final Screen back,home;
    private final String brand;
    private final PhonePreferences prefs;
    private final java.util.function.Consumer<PhonePhotos.Photo> picker;
    private List<PhonePhotos.Photo> photos=List.of();
    private final LinkedHashMap<String,Texture> textures=new LinkedHashMap<>(32,.75f,true);
    private final Set<String> loading=new HashSet<>(),failed=new HashSet<>();
    private int x,y,w,h,columns,cellW,imageH,rows,page,selected=-1,generation;
    private boolean active,scanning,loaded,busy,confirmDelete;
    private String status="Loading your photos...";
    public PhoneGalleryScreen(Screen back,Screen home,String brand) {
        this(back,home,brand,null);
    }
    public PhoneGalleryScreen(Screen back,Screen home,String brand,java.util.function.Consumer<PhonePhotos.Photo> picker) {
        super(Component.literal("Phone Gallery"));this.back=back;this.home=home;this.brand=brand;
        this.picker=picker;this.prefs=PhoneWallpapers.preferences(home);
    }
    public PhonePreferences preferences(){return prefs;}
    @Override protected void init() {
        active=true;w=Math.min(640,width-16);h=Math.min(420,height-16);x=(width-w)/2;y=(height-h)/2;
        columns=Math.min(prefs.galleryColumns,Math.max(2,w/100));cellW=(w-32)/columns;imageH=Math.max(30,(cellW-10)*9/16);
        rows=Math.max(1,(h-105)/(imageH+24));controls();if(!loaded&&!scanning)refresh();
    }
    private int perPage(){return columns*rows;}
    private int pages(){return Math.max(1,(photos.size()+perPage()-1)/perPage());}
    private void refresh() {
        scanning=true;int revision=generation;
        PhonePhotos.scan().whenComplete((items,error) -> minecraft.execute(() -> {
            if(!active||revision!=generation)return;scanning=false;loaded=true;
            if(error!=null){status="Couldn't open the photo folder. Try again.";controls();return;}
            var sorted=new java.util.ArrayList<>(items);if(prefs.galleryOldest)java.util.Collections.reverse(sorted);photos=List.copyOf(sorted);page=Math.min(page,pages()-1);selected=Math.min(selected,photos.size()-1);
            status=photos.isEmpty()?"No photos yet. Open Camera to take your first one.":photos.size()+" saved photo"+(photos.size()==1?"":"s");controls();
        }));
    }
    private void button(int bx,int by,int bw,int bh,String label,int shade,Runnable action) {
        addRenderableWidget(new PhonePhotoButton(bx,by,bw,bh,label,shade,action));
    }
    private void controls() {
        clearWidgets();
        button(x+10,y+12,49,22,"Back",0xFF344453,this::onClose);
        if(picker==null)button(x+w-85,y+12,75,22,"Camera",0xFF48756A,() -> minecraft.setScreen(back instanceof PhoneCameraScreen?back:new PhoneCameraScreen(home,brand)));
        if(confirmDelete) {
            button(x+w/2-105,y+h/2+18,100,24,"Cancel",0xFF415467,() -> {confirmDelete=false;controls();});
            button(x+w/2+5,y+h/2+18,100,24,"Delete photo",0xFF925758,this::deleteSelected);return;
        }
        if(selected>=0 && selected<photos.size()) {
            button(x+12,y+h-41,36,22,"<",0xFF344453,() -> select(Math.floorMod(selected-1,photos.size())));
            button(x+w-48,y+h-41,36,22,">",0xFF344453,() -> select((selected+1)%photos.size()));
            if(picker!=null) {
                button(x+w/2-70,y+h-41,140,22,"Use wallpaper",prefs.accentColor(),() -> {picker.accept(photos.get(selected));minecraft.setScreen(back);});return;
            }
            int copyW=Math.min(152,(w-115)*2/3),delW=Math.min(78,w-115-copyW);
            var copy=addRenderableWidget(new PhonePhotoButton(x+w/2-(copyW+delW+6)/2,y+h-41,copyW,22,"Copy to clipboard",0xFF49776B,this::copySelected));copy.active=!busy;
            var delete=addRenderableWidget(new PhonePhotoButton(copy.getX()+copyW+6,y+h-41,delW,22,"Delete",0xFF784B55,() -> {if(prefs.confirmDelete){confirmDelete=true;controls();}else deleteSelected();}));delete.active=!busy;
        } else {
            int first=page*perPage();
            for(int i=0;i<perPage() && first+i<photos.size();i++) {
                int index=first+i;addRenderableWidget(new Tile(x+16+(i%columns)*cellW,y+48+(i/columns)*(imageH+24),cellW-10,imageH+19,index));
            }
            button(x+12,y+h-41,42,22,"<",0xFF344453,() -> {page=Math.floorMod(page-1,pages());controls();});
            button(x+w-54,y+h-41,42,22,">",0xFF344453,() -> {page=(page+1)%pages();controls();});
            button(x+w/2-39,y+h-41,78,22,"Refresh",0xFF415C72,() -> {status="Refreshing...";refresh();});
        }
    }
    private void select(int index){selected=index;confirmDelete=false;status=photos.get(index).date()+"  |  "+photos.get(index).width()+" x "+photos.get(index).height();controls();}
    private void copySelected() {
        if(busy||selected<0)return;busy=true;status="Copying image...";controls();
        PhonePhotos.copy(photos.get(selected)).whenComplete((ignored,error) -> minecraft.execute(() -> {
            if(!active)return;busy=false;status=error==null?"Image copied! Paste it with Ctrl+V.":"Couldn't copy. Try again with the game window active.";controls();
        }));
    }
    private void deleteSelected() {
        if(busy||selected<0)return;busy=true;confirmDelete=false;status="Removing photo...";controls();
        PhonePhotos.delete(photos.get(selected)).whenComplete((ignored,error) -> minecraft.execute(() -> {
            if(!active)return;busy=false;
            if(error!=null){net.mcreator.commandcookieselectronicmod.CommandcookiesElectronicModMod.LOGGER.warn("Could not remove phone photo",error);status="Couldn't delete this photo. Try again.";controls();return;}
            selected=-1;releaseTextures();refresh();
        }));
    }
    private Texture texture(PhonePhotos.Photo photo,boolean full) {
        String key=photo.id()+(full?"/full":"/thumb");Texture known=textures.get(key);if(known!=null)return known;
        if(failed.contains(key)||loading.contains(key))return null;
        loading.add(key);int revision=generation;
        PhonePhotos.load(photo,full).whenComplete((image,error) -> minecraft.execute(() -> {
            if(!active||revision!=generation){if(image!=null)image.close();return;}
            loading.remove(key);
            if(error!=null){failed.add(key);return;}
            var id=minecraft.getTextureManager().register("phone_photo",new DynamicTexture(image));
            textures.put(key,new Texture(id,image.getWidth(),image.getHeight()));
            while(textures.size()>24) {
                String oldest=textures.keySet().iterator().next();Texture old=textures.remove(oldest);minecraft.getTextureManager().release(old.id());
            }
        }));return null;
    }
    private void drawPhoto(GuiGraphics g,PhonePhotos.Photo photo,int px,int py,int pw,int ph,boolean full) {
        g.fill(px,py,px+pw,py+ph,0xFF111923);Texture texture=texture(photo,full);
        if(texture==null) {
            g.drawCenteredString(font,failed.contains(photo.id()+(full?"/full":"/thumb"))?"Unavailable":"Loading...",px+pw/2,py+ph/2-4,0xFF8B9FAD);return;
        }
        double fit=Math.min(pw/(double)texture.width(),ph/(double)texture.height());
        int dw=Math.max(1,(int)(texture.width()*fit)),dh=Math.max(1,(int)(texture.height()*fit));
        g.blit(texture.id(),px+(pw-dw)/2,py+(ph-dh)/2,dw,dh,0f,0f,texture.width(),texture.height(),texture.width(),texture.height());
    }
    private void releaseTextures() {
        generation++;for(Texture texture:textures.values())minecraft.getTextureManager().release(texture.id());textures.clear();loading.clear();failed.clear();
    }
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float tick) {
        g.fill(0,0,width,height,0xCC080E17);g.fill(x+3,y+3,x+w+3,y+h+3,0x77000000);
        g.fill(x,y,x+w,y+h,0xFF8096A0);g.fill(x+2,y+2,x+w-2,y+h-2,prefs.panel());
        g.fill(x+3,y+3,x+w-3,y+41,prefs.card());g.fill(x+3,y+h-50,x+w-3,y+h-3,prefs.card());
        g.drawCenteredString(font,selected>=0?"Photo "+(selected+1)+" / "+photos.size():picker!=null?"Choose wallpaper":"Gallery",x+w/2,y+14,prefs.ink());
        if(selected>=0&&selected<photos.size())drawPhoto(g,photos.get(selected),x+12,y+47,w-24,h-104,true);
        else if(photos.isEmpty()) {
            PhoneAppIcons.draw(g,"Gallery",x+w/2-16,y+h/2-32,2);
            g.drawCenteredString(font,scanning?"Loading photos...":"Your Minecraft moments",x+w/2,y+h/2+9,prefs.ink());
        } else g.drawCenteredString(font,"Page "+(page+1)+" of "+pages(),x+w/2,y+h-61,prefs.muted());
        if(confirmDelete){
            g.fill(x+3,y+42,x+w-3,y+h-50,0xF0162230);
            g.drawCenteredString(font,"Delete this photo?",x+w/2,y+h/2-18,0xFFFFD1CF);
            g.drawCenteredString(font,"It will be removed from Gallery.",x+w/2,y+h/2-2,0xFFB9C8CD);
        }
        g.drawCenteredString(font,font.plainSubstrByWidth(status,w-22),x+w/2,y+h-13,prefs.muted());
    }
    @Override public boolean keyPressed(int key,int scan,int mods) {
        if(picker==null&&!confirmDelete&&selected>=0&&key==GLFW.GLFW_KEY_C&&(mods&GLFW.GLFW_MOD_CONTROL)!=0){copySelected();return true;}
        if(!confirmDelete&&selected>=0&&(key==GLFW.GLFW_KEY_LEFT||key==GLFW.GLFW_KEY_RIGHT)) {
            select(Math.floorMod(selected+(key==GLFW.GLFW_KEY_LEFT?-1:1),photos.size()));return true;
        }
        return super.keyPressed(key,scan,mods);
    }
    @Override public void onClose() {
        if(confirmDelete){confirmDelete=false;controls();}
        else if(selected>=0){selected=-1;status=photos.size()+" saved photos";controls();}
        else minecraft.setScreen(back);
    }
    @Override public void removed(){active=false;scanning=false;releaseTextures();}
    @Override public boolean isPauseScreen(){return false;}
    private final class Tile extends Button {
        private final int index;
        Tile(int x,int y,int w,int h,int index) {
            super(x,y,w,h,Component.literal("Open photo "+(index+1)+", "+photos.get(index).date()),button -> select(index),DEFAULT_NARRATION);this.index=index;
        }
        @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager){PhoneWallpapers.click(manager,prefs);}
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float tick) {
            drawPhoto(g,photos.get(index),getX(),getY(),getWidth(),imageH,false);
            g.fill(getX(),getY()+imageH,getX()+getWidth(),getY()+getHeight(),prefs.card());
            g.drawCenteredString(font,font.plainSubstrByWidth(photos.get(index).date(),getWidth()-6),getX()+getWidth()/2,getY()+imageH+5,prefs.ink());
            if(isHoveredOrFocused())g.renderOutline(getX(),getY(),getWidth(),getHeight(),0xFFABCAC2);
        }
    }
}
