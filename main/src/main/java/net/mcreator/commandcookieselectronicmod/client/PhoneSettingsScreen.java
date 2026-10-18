package net.mcreator.commandcookieselectronicmod.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The shared phone settings app. All controls update saved, working preferences. */
public final class PhoneSettingsScreen extends Screen {
    private static final String[] CATEGORIES={"Wallpaper","Display","Clock","Sound","Camera","Gallery","General","Chromium"};
    private final IphoneScreen home;
    private final String brand;
    private final PhonePreferences prefs;
    private int x,y,w,h,side,cx,cw,category,optionPage,wallpaperPage;
    private boolean lockTarget;
    private long saveRetryAt;
    private String status="Saved automatically for this phone",confirmation="";
    private record Option(String label,String detail,Supplier<String> value,Runnable change) {}
    private final List<Option> options=new ArrayList<>();
    public PhoneSettingsScreen(IphoneScreen home,String brand) {
        super(Component.literal("Phone Settings"));this.home=home;this.brand=brand;this.prefs=home.preferences();
    }
    public PhonePreferences preferences(){return prefs;}
    @Override protected void init() {
        w=Math.min(660,width-16);h=Math.min(430,height-16);x=(width-w)/2;y=(height-h)/2;
        side=w>=500?114:82;cx=x+side+14;cw=w-side-26;controls();
    }
    private void button(int px,int py,int pw,int ph,String label,int color,Runnable action) {
        addRenderableWidget(new PhonePhotoButton(px,py,pw,ph,label,color,action));
    }
    private void changed() {
        prefs.sanitize();PhoneAppState.INSTANCE.changed(home.appState());
        boolean saved=PhoneAppState.INSTANCE.save();saveRetryAt=saved?0:System.currentTimeMillis()+1000;
        status=saved?"Saved automatically for this phone":"Saving changes...";
        PhoneMusic.applyPreferences(prefs);controls();
    }
    private void controls() {
        clearWidgets();
        button(x+10,y+12,side-12,22,"< Home",prefs.card(),this::onClose);
        for(int i=0;i<CATEGORIES.length;i++) {
            int index=i;button(x+9,y+53+i*Math.min(25,(h-61)/CATEGORIES.length),side-10,Math.min(22,(h-61)/CATEGORIES.length-2),CATEGORIES[i],i==category?prefs.accentColor():prefs.card(),() -> {
                category=index;optionPage=0;confirmation="";controls();
            });
        }
        if(!confirmation.isEmpty()) {
            button(cx,y+h/2+18,Math.min(96,cw/2-5),25,"Cancel",prefs.card(),() -> {confirmation="";controls();});
            button(cx+Math.min(106,cw/2+5),y+h/2+18,Math.min(150,cw/2-5),25,"Confirm",prefs.accentColor(),this::confirm);return;
        }
        if(category==0){wallpaperControls();return;}
        makeOptions();int count=rows(),pages=Math.max(1,(options.size()+count-1)/count);optionPage=Math.min(optionPage,pages-1);
        for(int i=0;i<count&&optionPage*count+i<options.size();i++) {
            Option option=options.get(optionPage*count+i);
            addRenderableWidget(new SettingRow(cx,y+55+i*40,cw,36,option));
        }
        if(pages>1) {
            button(cx,y+h-53,31,20,"<",prefs.card(),() -> {optionPage=Math.floorMod(optionPage-1,pages);controls();});
            button(cx+cw-31,y+h-53,31,20,">",prefs.card(),() -> {optionPage=(optionPage+1)%pages;controls();});
        }
    }
    private int rows(){return Math.max(1,(h-115)/40);}
    private String on(boolean b){return b?"On":"Off";}
    private void option(String name,String detail,Supplier<String> value,Runnable action){options.add(new Option(name,detail,value,action));}
    private int cycle(int value,int... values){for(int i=0;i<values.length;i++)if(value==values[i])return values[(i+1)%values.length];return values[0];}
    private void makeOptions() {
        options.clear();
        switch(category) {
            case 1 -> {
                option("Theme","Changes app panels and text",() -> prefs.darkTheme?"Dark":"Light",() -> prefs.darkTheme=!prefs.darkTheme);
                option("Accent color","Highlights selected controls",() -> PhonePreferences.COLOR_NAMES[prefs.accent],() -> prefs.accent=(prefs.accent+1)%13);
                option("Text size","Scales labels inside phone apps",() -> new String[]{"Small","Normal","Large"}[prefs.textSize],() -> prefs.textSize=(prefs.textSize+1)%3);
                option("App labels","Names below home screen icons",() -> on(prefs.iconLabels),() -> prefs.iconLabels=!prefs.iconLabels);
                option("Bold labels","Emphasizes home screen names",() -> on(prefs.boldLabels),() -> prefs.boldLabels=!prefs.boldLabels);
                option("Wallpaper brightness","Adjusts home and lock backgrounds",() -> prefs.brightness+"%",() -> prefs.brightness=cycle(prefs.brightness,100,90,80,70,60,50,40,30));
                option("Dim home wallpaper","Keeps app icons easy to see",() -> on(prefs.dimHome),() -> prefs.dimHome=!prefs.dimHome);
                option("Same lock wallpaper","Use home wallpaper on lock screen",() -> on(prefs.sameLockWallpaper),() -> prefs.sameLockWallpaper=!prefs.sameLockWallpaper);
                option("Photo wallpaper fit","Keeps original photo proportions",() -> prefs.photoFill?"Fill / crop":"Fit / borders",() -> prefs.photoFill=!prefs.photoFill);
            }
            case 2 -> {
                option("Time format","Status bar, lock screen and Clock",() -> prefs.clock24?"24 hour":"12 hour",() -> prefs.clock24=!prefs.clock24);
                option("Show seconds","Lock screen and Clock app",() -> on(prefs.clockSeconds),() -> prefs.clockSeconds=!prefs.clockSeconds);
                option("Lock screen date","Show the date above the time",() -> on(prefs.lockDate),() -> prefs.lockDate=!prefs.lockDate);
                option("World clock","Show Minecraft time in Clock",() -> on(prefs.worldClock),() -> prefs.worldClock=!prefs.worldClock);
            }
            case 3 -> {
                option("Silent mode","Mutes this phone's clicks and music",() -> on(prefs.silent),() -> prefs.silent=!prefs.silent);
                option("Button sounds","Sound when pressing app controls",() -> on(prefs.buttonSounds),() -> prefs.buttonSounds=!prefs.buttonSounds);
                option("Button volume","Volume for phone clicks and chimes",() -> prefs.uiVolume+"%",() -> prefs.uiVolume=cycle(prefs.uiVolume,75,100,0,25,50));
                option("Music volume","Minecraft discs played by this phone",() -> prefs.musicVolume+"%",() -> prefs.musicVolume=cycle(prefs.musicVolume,100,75,50,25,0));
                option("Music on exit","Choose if music keeps playing",() -> prefs.stopMusicOnClose?"Stop":"Keep playing",() -> prefs.stopMusicOnClose=!prefs.stopMusicOnClose);
                option("Auto next disc","Plays the next track when one ends",() -> on(prefs.autoNext),() -> prefs.autoNext=!prefs.autoNext);
                option("Camera shutter sound","Click when the photo is captured",() -> on(prefs.shutterSound),() -> prefs.shutterSound=!prefs.shutterSound);
                option("Photo saved chime","Sound after saving successfully",() -> on(prefs.saveChime),() -> prefs.saveChime=!prefs.saveChime);
            }
            case 4 -> {
                option("Composition grid","Rule of thirds lines in Camera",() -> on(prefs.cameraGrid),() -> prefs.cameraGrid=!prefs.cameraGrid);
                option("Capture flash","Brief flash after taking a photo",() -> on(prefs.cameraFlash),() -> prefs.cameraFlash=!prefs.cameraFlash);
                option("Default zoom","Zoom when opening Camera",() -> prefs.cameraZoom+"x",() -> prefs.cameraZoom=cycle(prefs.cameraZoom,1,2,4));
                option("Photo timer","Delay before taking your picture",() -> prefs.cameraTimer==0?"Off":prefs.cameraTimer+" seconds",() -> prefs.cameraTimer=cycle(prefs.cameraTimer,0,3,5));
                option("Photo resolution","Saved image size; preview stays live",() -> new String[]{"Full","Half","Quarter"}[prefs.photoQuality],() -> prefs.photoQuality=(prefs.photoQuality+1)%3);
                option("Aim sensitivity","Drag or arrow key camera movement",() -> new String[]{"Low","Normal","High"}[prefs.aimSpeed],() -> prefs.aimSpeed=(prefs.aimSpeed+1)%3);
                option("Invert vertical aim","Reverses vertical camera movement",() -> on(prefs.invertAim),() -> prefs.invertAim=!prefs.invertAim);
            }
            case 5 -> {
                option("Gallery columns","Number of photos across the grid",() -> Integer.toString(prefs.galleryColumns),() -> prefs.galleryColumns=cycle(prefs.galleryColumns,3,4,2));
                option("Photo order","Sort your saved camera pictures",() -> prefs.galleryOldest?"Oldest first":"Newest first",() -> prefs.galleryOldest=!prefs.galleryOldest);
                option("Confirm deletion","Ask before removing a photo",() -> on(prefs.confirmDelete),() -> prefs.confirmDelete=!prefs.confirmDelete);
            }
            case 7 -> {
                option("Chromium frame rate","25 FPS saves power; 60 FPS is smoother",() -> prefs.chromiumFps+" FPS / "+(prefs.chromiumFps==60?"Smooth":"Performance"),() -> prefs.chromiumFps=prefs.chromiumFps==60?25:60);
                option("Keep media playing","Continue audio when closing the phone in Media mode",() -> on(prefs.backgroundMedia),() -> {prefs.backgroundMedia=!prefs.backgroundMedia;if(!prefs.backgroundMedia)PhoneBrowserPlayback.stop(home.deviceId());});
                option("Stop background playback","Stops this phone's hidden browser; saved tabs are kept",() -> PhoneBrowserPlayback.running(home.deviceId())?"Stop":"Not running",() -> PhoneBrowserPlayback.stop(home.deviceId()));
                option("Open Chromium","Browser mode and tabs are saved for this phone",() -> "Open",() -> minecraft.setScreen(new ChromiumScreen(home)));
            }
            case 6 -> {
                option("Lock when opening","Freshly opening the phone starts locked",() -> on(prefs.lockOnOpen),() -> prefs.lockOnOpen=!prefs.lockOnOpen);
                option("Remember home page","Return to the last page of apps",() -> on(prefs.rememberPage),() -> prefs.rememberPage=!prefs.rememberPage);
                option("Default home page","Used when Remember home page is off",() -> "Page "+(prefs.startPage+1),() -> prefs.startPage=1-prefs.startPage);
                option("Notes autosave","Saves while typing; exit still saves",() -> on(prefs.notesAutosave),() -> prefs.notesAutosave=!prefs.notesAutosave);
                option("Apply to my saved phones","Copies settings only to your existing phone profiles",() -> "Apply...",() -> confirmation="all");
                option("Reset phone settings","Keeps your notes, photos and stopwatch",() -> "Reset...",() -> confirmation="reset");
            }
        }
    }
    private void confirm() {
        if(confirmation.equals("reset"))prefs.copyFrom(new PhonePreferences());
        else if(confirmation.equals("all")) {
            PhoneAppState.INSTANCE.applyToSavedPhones(home.profileKey(),prefs);
        }
        confirmation="";changed();
    }
    private int artworkWidth(){return cw-(cw>=410?112:0);}
    private int artColumns(){return Math.max(2,Math.min(4,artworkWidth()/73));}
    private int artStride(){return h<260?66:83;}
    private int artRows(){return Math.max(1,Math.min(3,(h-178)/artStride()));}
    private int artPages(){return (13+artColumns()*artRows()-1)/(artColumns()*artRows());}
    private int currentStyle(){return lockTarget?prefs.lockWallpaper:prefs.wallpaper;}
    private int currentColor(){return lockTarget?prefs.lockWallpaperColor:prefs.wallpaperColor;}
    private String currentPhoto(){return lockTarget?prefs.lockWallpaperPhoto:prefs.wallpaperPhoto;}
    private void wallpaperControls() {
        int aw=artworkWidth();
        button(cx,y+54,Math.min(72,aw/3-3),22,"Home",!lockTarget?prefs.accentColor():prefs.card(),() -> {lockTarget=false;controls();});
        button(cx+Math.min(78,aw/3+3),y+54,Math.min(72,aw/3-3),22,"Lock",lockTarget?prefs.accentColor():prefs.card(),() -> {lockTarget=true;controls();});
        button(cx+aw-Math.min(103,aw/3),y+54,Math.min(103,aw/3),22,"Use photo",prefs.card(),() -> minecraft.setScreen(new PhoneGalleryScreen(this,home,brand,photo -> {
            if(lockTarget){prefs.lockWallpaperPhoto=photo.id();prefs.sameLockWallpaper=false;}else prefs.wallpaperPhoto=photo.id();changed();
        })));
        int cols=artColumns(),rows=artRows(),cell=(aw-6)/cols,perPage=cols*rows;wallpaperPage=Math.min(wallpaperPage,artPages()-1);
        for(int i=0;i<perPage&&wallpaperPage*perPage+i<13;i++) {
            int style=wallpaperPage*perPage+i;
            addRenderableWidget(new WallpaperTile(cx+(i%cols)*cell,y+87+(i/cols)*artStride(),cell-6,artStride()-6,style));
        }
        int space=Math.max(11,Math.min(24,aw/13));
        for(int i=0;i<13;i++) {
            int color=i;var chip=addRenderableWidget(new PhonePhotoButton(cx+i*space,y+h-69,space-2,17,"",PhonePreferences.COLORS[i],() -> {
                if(lockTarget){prefs.lockWallpaperColor=color;prefs.sameLockWallpaper=false;}else prefs.wallpaperColor=color;changed();
            }));chip.setTooltip(Tooltip.create(Component.literal(PhonePreferences.COLOR_NAMES[i])));
        }
        button(cx,y+h-46,27,19,"<",prefs.card(),() -> {wallpaperPage=Math.floorMod(wallpaperPage-1,artPages());controls();});
        button(cx+aw-27,y+h-46,27,19,">",prefs.card(),() -> {wallpaperPage=(wallpaperPage+1)%artPages();controls();});
    }
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float tick) {
        g.fill(0,0,width,height,0xBB09111C);g.fill(x+4,y+4,x+w+4,y+h+4,0x88000000);
        g.fill(x,y,x+w,y+h,prefs.accentColor());g.fill(x+2,y+2,x+w-2,y+h-2,prefs.panel());
        g.fill(x+3,y+3,x+side+3,y+h-3,prefs.darkTheme?0xFF142230:0xFFD0DFE6);
        g.drawString(font,Component.literal("Settings").withStyle(net.minecraft.ChatFormatting.BOLD),cx,y+14,prefs.ink(),false);
        g.drawString(font,CATEGORIES[category]+"  /  "+brand,cx,y+33,prefs.muted(),false);
        if(!confirmation.isEmpty()) {
            String question=confirmation.equals("all")?"Apply settings to your saved phones?":"Reset this phone's settings?";
            g.drawWordWrap(font,Component.literal(question),cx,y+h/2-36,cw,prefs.ink());
            g.drawWordWrap(font,Component.literal("Notes, photos, calculations and stopwatch data are kept."),cx,y+h/2-8,cw,prefs.muted());
        }else if(category==0) {
            int aw=artworkWidth();
            String label=(currentPhoto().isEmpty()?PhoneWallpapers.NAMES[currentStyle()]:"Gallery photo")+" / "+PhonePreferences.COLOR_NAMES[currentColor()]+"  "+(wallpaperPage+1)+"/"+artPages();
            g.drawCenteredString(font,font.plainSubstrByWidth(label,aw-65),cx+aw/2,y+h-39,prefs.ink());
            int space=Math.max(11,Math.min(24,aw/13));g.renderOutline(cx+currentColor()*space-1,y+h-70,space,19,prefs.ink());
            if(cw>=410) {
                int px=cx+aw+16,py=y+83,pw=84,ph=171;
                g.fill(px-3,py-3,px+pw+3,py+ph+3,0xFF0D1118);
                PhoneWallpapers.draw(g,px,py,pw,ph,prefs,lockTarget,home.originalWallpaper());
                g.fill(px+pw/2-10,py+4,px+pw/2+10,py+10,0xFF0D1118);
                if(lockTarget) {
                    g.drawCenteredString(font,"12:34",px+pw/2,py+35,0xFFFFFFFF);
                }else {
                    String[] icons={"Calculator","Notes","Clock","Camera","Settings","Gallery"};
                    for(int i=0;i<6;i++)PhoneAppIcons.draw(g,icons[i],px+12+(i%2)*42,py+29+(i/2)*35,1);
                }
                g.drawCenteredString(font,lockTarget?"Lock preview":"Home preview",px+pw/2,py+ph+13,prefs.muted());
                g.drawWordWrap(font,Component.literal("Changes apply instantly. Pick a design, color or Gallery photo."),px-4,py+ph+33,96,prefs.muted());
            }
        }else if(!options.isEmpty()&&options.size()>rows()) {
            g.drawCenteredString(font,"Page "+(optionPage+1)+" / "+((options.size()+rows()-1)/rows()),cx+cw/2,y+h-47,prefs.muted());
        }
        g.drawString(font,font.plainSubstrByWidth(status,cw),cx,y+h-17,prefs.muted(),false);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        if(vertical!=0&&confirmation.isEmpty()) {
            if(category==0)wallpaperPage=Math.floorMod(wallpaperPage+(vertical<0?1:-1),artPages());
            else {int pages=Math.max(1,(options.size()+rows()-1)/rows());optionPage=Math.floorMod(optionPage+(vertical<0?1:-1),pages);}
            controls();return true;
        }return super.mouseScrolled(mx,my,horizontal,vertical);
    }
    @Override public void onClose(){if(!confirmation.isEmpty()){confirmation="";controls();}else minecraft.setScreen(home);}
    @Override public void removed(){PhoneAppState.INSTANCE.save();}
    @Override public void tick(){
        if(saveRetryAt!=0&&System.currentTimeMillis()>=saveRetryAt) {
            boolean saved=PhoneAppState.INSTANCE.save();saveRetryAt=saved?0:System.currentTimeMillis()+1000;
            status=saved?"Saved automatically for this phone":"Couldn't save yet. Retrying...";
        }
    }
    @Override public boolean isPauseScreen(){return false;}
    private final class SettingRow extends Button {
        private final Option option;
        SettingRow(int px,int py,int pw,int ph,Option option) {
            super(px,py,pw,ph,Component.literal(option.label()),button -> {option.change().run();changed();},DEFAULT_NARRATION);
            this.option=option;setTooltip(Tooltip.create(Component.literal(option.detail()+": "+option.value().get())));
        }
        @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager){PhoneWallpapers.click(manager,prefs);}
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float tick) {
            int px=getX(),py=getY(),pw=getWidth();g.fill(px,py,px+pw,py+getHeight(),prefs.card());
            if(isHoveredOrFocused())g.renderOutline(px,py,pw,getHeight(),prefs.accentColor());
            int valueW=Math.min(115,pw/3);String value=font.plainSubstrByWidth(option.value().get(),valueW);
            g.drawString(font,font.plainSubstrByWidth(option.label(),pw-valueW-20),px+8,py+6,prefs.ink(),false);
            g.drawString(font,font.plainSubstrByWidth(option.detail(),pw-16),px+8,py+22,prefs.muted(),false);
            g.drawString(font,value,px+pw-font.width(value)-8,py+6,prefs.ink(),false);
        }
    }
    private final class WallpaperTile extends Button {
        private final int style;
        WallpaperTile(int px,int py,int pw,int ph,int style) {
            super(px,py,pw,ph,Component.literal(PhoneWallpapers.NAMES[style]),button -> {
                if(lockTarget){prefs.lockWallpaper=style;prefs.lockWallpaperPhoto="";prefs.sameLockWallpaper=false;}
                else {prefs.wallpaper=style;prefs.wallpaperPhoto="";}changed();
            },DEFAULT_NARRATION);this.style=style;
        }
        @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager){PhoneWallpapers.click(manager,prefs);}
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float tick) {
            int px=getX(),py=getY(),pw=getWidth();g.fill(px,py,px+pw,py+getHeight(),prefs.card());
            int ph=getHeight()-21,iw=ph/2;
            PhoneWallpapers.drawPixels(g,px+(pw-iw)/2,py+5,iw,ph,PhoneWallpapers.pixels(style,currentColor(),home.originalWallpaper()));
            g.drawCenteredString(font,font.plainSubstrByWidth(PhoneWallpapers.NAMES[style],pw-5),px+pw/2,py+getHeight()-12,prefs.ink());
            if(isHoveredOrFocused()||style==currentStyle()&&currentPhoto().isEmpty())g.renderOutline(px,py,pw,getHeight(),prefs.accentColor());
        }
    }
}
