package net.mcreator.commandcookieselectronicmod.client;

import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.neoforged.fml.loading.FMLPaths;

/** Private local preferences for one player's physical laptop. */
public final class LaptopPreferences {
    public static final String[] WALLPAPERS={"Ocean","Dusk","Forest","Ember","Glacier","Midnight"};
    public static final String[] ACCENTS={"Mint","Sky","Lavender","Amber","Rose","Ice"};
    public static final int[] ACCENT_COLORS={0xFF7EDDB2,0xFF82CFF0,0xFFBFABE9,0xFFE8BF7D,0xFFE7A8BA,0xFFB1DCE4};
    private transient Path path;
    public int wallpaper,accent,theme,musicVolume=100,gameVolume=60,chromiumFps=25;
    public String wallpaperPhoto="";
    public boolean iconLabels=true,clock24,clockSeconds,showDate=true,lockOnOpen=true,rememberWindows=true;
    public boolean galleryOldest,confirmDelete=true,photoFill=true,dimPhoto=true,autoNext,stopMusicOnExit;
    private Path file(){return path;}
    static LaptopPreferences load(Path path){
        try {if(Files.exists(path)){var p=new Gson().fromJson(Files.readString(path),LaptopPreferences.class);if(p!=null){p.path=path;p.sanitize();return p;}}}
        catch(Exception error){
            try {Files.copy(path,path.resolveSibling(path.getFileName()+".backup-"+System.currentTimeMillis()));}catch(Exception ignored){}
            net.mcreator.commandcookieselectronicmod.CommandcookiesElectronicModMod.LOGGER.warn("Could not read laptop settings; using defaults",error);
        }
        var p=new LaptopPreferences();p.path=path;return p;
    }
    public void sanitize(){chromiumFps=chromiumFps==60?60:25;wallpaper=Math.max(0,Math.min(5,wallpaper));accent=Math.max(0,Math.min(5,accent));theme=Math.max(0,Math.min(2,theme));musicVolume=Math.max(0,Math.min(100,musicVolume));gameVolume=Math.max(0,Math.min(100,gameVolume));if(wallpaperPhoto==null||!wallpaperPhoto.matches("(?:|photo_[0-9]{13}_[a-f0-9]{12})"))wallpaperPhoto="";}
    public boolean save(){
        sanitize();try {Files.createDirectories(file().toAbsolutePath().getParent());Path tmp=file().resolveSibling(file().getFileName()+".tmp");Files.writeString(tmp,new Gson().toJson(this));
            try {Files.move(tmp,file(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(java.nio.file.AtomicMoveNotSupportedException e){Files.move(tmp,file(),StandardCopyOption.REPLACE_EXISTING);}return true;
        }catch(Exception error){net.mcreator.commandcookieselectronicmod.CommandcookiesElectronicModMod.LOGGER.warn("Could not save laptop settings",error);return false;}
    }
    public int accentColor(){return ACCENT_COLORS[accent];}
    public int panel(){return new int[]{0xFF172632,0xFF1C2037,0xFF192D29}[theme];}
    public void reset(){wallpaper=accent=theme=0;musicVolume=100;gameVolume=60;chromiumFps=25;wallpaperPhoto="";iconLabels=showDate=lockOnOpen=rememberWindows=confirmDelete=photoFill=dimPhoto=true;clock24=clockSeconds=galleryOldest=autoNext=stopMusicOnExit=false;save();}
}
