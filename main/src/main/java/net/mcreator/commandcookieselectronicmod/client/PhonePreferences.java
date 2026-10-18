package net.mcreator.commandcookieselectronicmod.client;

/** Saved app preferences for one phone color. Missing fields retain these defaults. */
public final class PhonePreferences {
    public int wallpaper, wallpaperColor, lockWallpaper, lockWallpaperColor, accent, textSize=1;
    public String wallpaperPhoto="", lockWallpaperPhoto="";
    public int brightness=100, uiVolume=75, musicVolume=100, cameraZoom=1, cameraTimer, photoQuality, aimSpeed=1, galleryColumns=3, startPage, chromiumFps=25;
    public boolean sameLockWallpaper=true, photoFill=true, dimHome=true, darkTheme=true, iconLabels=true, boldLabels;
    public boolean clock24, clockSeconds, lockDate=true, worldClock=true, lockOnOpen=true, rememberPage=true, notesAutosave=true;
    public boolean buttonSounds=true, silent, stopMusicOnClose, autoNext, shutterSound=true, saveChime;
    public boolean backgroundMedia=true;
    public boolean cameraGrid=true, cameraFlash=true, invertAim, galleryOldest, confirmDelete=true;
    public static final String[] COLOR_NAMES={"Match phone","Burgundy","Ocean","Sage","Sunset","Lavender","Teal","Gold","Rose","Ice","Slate","Lime","Mono"};
    public static final int[] COLORS={0xFF5E8392,0xFF964967,0xFF437EA6,0xFF639679,0xFFE18E54,0xFF9C7FBF,0xFF459F9C,0xFFBEA052,0xFFCC789C,0xFF94BFD6,0xFF778794,0xFF89AB55,0xFF9CA6AD};
    public void sanitize() {
        chromiumFps=chromiumFps==60?60:25;
        wallpaper=bound(wallpaper,0,12);lockWallpaper=bound(lockWallpaper,0,12);
        wallpaperColor=bound(wallpaperColor,0,12);lockWallpaperColor=bound(lockWallpaperColor,0,12);accent=bound(accent,0,12);
        textSize=bound(textSize,0,2);brightness=bound(brightness,30,100);uiVolume=bound(uiVolume,0,100);musicVolume=bound(musicVolume,0,100);
        if(cameraZoom!=1&&cameraZoom!=2&&cameraZoom!=4)cameraZoom=1;
        if(cameraTimer!=0&&cameraTimer!=3&&cameraTimer!=5)cameraTimer=0;
        photoQuality=bound(photoQuality,0,2);aimSpeed=bound(aimSpeed,0,2);galleryColumns=bound(galleryColumns,2,4);startPage=bound(startPage,0,1);
        wallpaperPhoto=validPhoto(wallpaperPhoto);lockWallpaperPhoto=validPhoto(lockWallpaperPhoto);
    }
    private static String validPhoto(String id){return id!=null&&(id.isEmpty()||id.matches("photo_[0-9]{13}_[a-f0-9]{12}"))?id:"";}
    private static int bound(int n,int min,int max){return Math.max(min,Math.min(max,n));}
    public int accentColor(){return COLORS[accent];}
    public int panel(){return darkTheme?0xFF1A2735:0xFFE8EFF2;}
    public int card(){return darkTheme?0xFF263A4A:0xFFD4E1E7;}
    public int ink(){return darkTheme?0xFFEAF1EF:0xFF233542;}
    public int muted(){return darkTheme?0xFFA9C1C9:0xFF536F7C;}
    public float textScale(){return textSize==0?.85f:textSize==2?1.15f:1f;}
    public float aimScale(){return aimSpeed==0?.55f:aimSpeed==2?1.6f:1f;}
    public float clickVolume(){return silent||!buttonSounds?0:uiVolume/100f;}
    public void copyFrom(PhonePreferences p) {
        wallpaper=p.wallpaper;wallpaperColor=p.wallpaperColor;lockWallpaper=p.lockWallpaper;lockWallpaperColor=p.lockWallpaperColor;accent=p.accent;textSize=p.textSize;
        wallpaperPhoto=p.wallpaperPhoto;lockWallpaperPhoto=p.lockWallpaperPhoto;brightness=p.brightness;uiVolume=p.uiVolume;musicVolume=p.musicVolume;
        cameraZoom=p.cameraZoom;cameraTimer=p.cameraTimer;photoQuality=p.photoQuality;aimSpeed=p.aimSpeed;galleryColumns=p.galleryColumns;startPage=p.startPage;
        chromiumFps=p.chromiumFps;backgroundMedia=p.backgroundMedia;
        sameLockWallpaper=p.sameLockWallpaper;photoFill=p.photoFill;dimHome=p.dimHome;darkTheme=p.darkTheme;iconLabels=p.iconLabels;boldLabels=p.boldLabels;
        clock24=p.clock24;clockSeconds=p.clockSeconds;lockDate=p.lockDate;worldClock=p.worldClock;lockOnOpen=p.lockOnOpen;rememberPage=p.rememberPage;notesAutosave=p.notesAutosave;
        buttonSounds=p.buttonSounds;silent=p.silent;stopMusicOnClose=p.stopMusicOnClose;autoNext=p.autoNext;shutterSound=p.shutterSound;saveChime=p.saveChime;
        cameraGrid=p.cameraGrid;cameraFlash=p.cameraFlash;invertAim=p.invertAim;galleryOldest=p.galleryOldest;confirmDelete=p.confirmDelete;sanitize();
    }
}
