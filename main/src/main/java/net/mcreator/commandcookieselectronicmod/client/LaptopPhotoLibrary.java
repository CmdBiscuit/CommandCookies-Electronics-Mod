package net.mcreator.commandcookieselectronicmod.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** Window-scoped texture cache over the existing phone photo folder, with no duplicate library. */
final class LaptopPhotoLibrary {
    private final LaptopPreferences prefs;
    LaptopPhotoLibrary(LaptopPreferences prefs){this.prefs=prefs;}
    private record Texture(ResourceLocation id,int width,int height){}
    private final LinkedHashMap<String,Texture> textures=new LinkedHashMap<>(32,.75f,true);
    private final Set<String> loading=new HashSet<>(),failed=new HashSet<>();
    private List<PhonePhotos.Photo> photos=List.of();
    private boolean active=true,scanning,loaded,refreshQueued;
    private int generation;
    private long nextRefresh;
    private String status="Loading phone photos...";
    boolean active(){return active;}
    boolean loaded(){return loaded;}
    String status(){return status;}
    List<PhonePhotos.Photo> photos(){var list=new ArrayList<>(photos);if(prefs.galleryOldest)java.util.Collections.reverse(list);return List.copyOf(list);}
    PhonePhotos.Photo find(String id){return photos.stream().filter(p->p.id().equals(id)).findFirst().orElse(null);}
    void poll(){if(!scanning&&System.currentTimeMillis()>=nextRefresh)refresh();}
    void refresh(){
        if(!active)return;if(scanning){refreshQueued=true;return;}failed.clear();scanning=true;nextRefresh=System.currentTimeMillis()+5000;int revision=generation;
        PhonePhotos.scan().whenComplete((items,error)->Minecraft.getInstance().execute(()->{
            if(!active||revision!=generation)return;scanning=false;loaded=true;if(refreshQueued){refreshQueued=false;nextRefresh=0;}
            if(error!=null){status="Could not read photos. Try Refresh.";return;}
            photos=List.copyOf(items);status=photos.size()+" phone photo"+(photos.size()==1?"":"s")+" / synced";
            Set<String> ids=new HashSet<>();photos.forEach(p->ids.add(p.id()));
            var it=textures.entrySet().iterator();while(it.hasNext()){var entry=it.next();if(!ids.contains(entry.getKey().split("/")[0])){Minecraft.getInstance().getTextureManager().release(entry.getValue().id());it.remove();}}
        }));
    }
    private Texture texture(PhonePhotos.Photo photo,boolean full){
        String key=photo.id()+(full?"/full":"/thumb");Texture known=textures.get(key);if(known!=null)return known;
        if(!active||loading.contains(key)||failed.contains(key))return null;
        loading.add(key);int revision=generation;
        PhonePhotos.load(photo,full).whenComplete((image,error)->Minecraft.getInstance().execute(()->{
            if(!active||revision!=generation){if(image!=null)image.close();return;}loading.remove(key);
            if(error!=null){failed.add(key);return;}
            var manager=Minecraft.getInstance().getTextureManager();var id=manager.register("laptop_phone_photo",new DynamicTexture(image));
            textures.put(key,new Texture(id,image.getWidth(),image.getHeight()));
            while(textures.size()>24){String oldest=textures.keySet().iterator().next();manager.release(textures.remove(oldest).id());}
        }));return null;
    }
    void draw(GuiGraphics g,PhonePhotos.Photo photo,int x,int y,int w,int h,boolean full,boolean fill){
        g.fill(x,y,x+w,y+h,0xFF0D1821);var t=texture(photo,full);
        if(t==null){g.drawCenteredString(Minecraft.getInstance().font,failed.contains(photo.id()+(full?"/full":"/thumb"))?"Unavailable":"Loading...",x+w/2,y+h/2-4,0xFF94B5C4);return;}
        if(fill){
            double target=w/(double)h,ratio=t.width()/(double)t.height();int sw=t.width(),sh=t.height();
            if(ratio>target)sw=(int)(sh*target);else sh=(int)(sw/target);
            g.blit(t.id(),x,y,w,h,(t.width()-sw)/2f,(t.height()-sh)/2f,sw,sh,t.width(),t.height());
        }else{double fit=Math.min(w/(double)t.width(),h/(double)t.height());int dw=Math.max(1,(int)(t.width()*fit)),dh=Math.max(1,(int)(t.height()*fit));g.blit(t.id(),x+(w-dw)/2,y+(h-dh)/2,dw,dh,0f,0f,t.width(),t.height(),t.width(),t.height());}
    }
    void release(){active=false;generation++;for(var t:textures.values())Minecraft.getInstance().getTextureManager().release(t.id());textures.clear();loading.clear();failed.clear();}
}
