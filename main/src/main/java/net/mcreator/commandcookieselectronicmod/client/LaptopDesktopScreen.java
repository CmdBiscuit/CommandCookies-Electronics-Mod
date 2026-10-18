package net.mcreator.commandcookieselectronicmod.client;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.mcreator.commandcookieselectronicmod.calculator.CalculatorEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.mcreator.commandcookieselectronicmod.block.LaptopBlock;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;

/** Client-only BlockOS desktop with local apps and independent laptop sessions. */
public final class LaptopDesktopScreen extends Screen {
    private static final int W=480,H=286,BAR=258;
    private static final int INK=0xFFE9F2F4,MUTED=0xFF9BAFB9,EDGE=0xFF526574;
    private static final String[] APPS={"Files","Browser","Calculator","Notes","Music","Gallery","Terminal","Settings","Snake","2048","Mines","Breakout","Blockfall","Memory","Tic Tac Toe","Reaction","Paint","Planner","Calendar","Converter","Focus Timer","System","Email","Security","Chromium"};
    private final LaptopPreferences prefs;
    private final LaptopNotes notes;
    private final LaptopPhotoLibrary photos;
    private final LaptopPrograms programs;
    private int desktopPage,startPage,taskbarPage;
    private DeskWindow painting;
    private int accent(){return prefs.accentColor();}
    private record SessionKey(net.minecraft.resources.ResourceKey<Level> dimension,BlockPos position,java.util.UUID player,java.util.UUID device) {}
    private record Session(java.util.UUID id,List<DeskWindow> windows,boolean locked,boolean showDesktop) {}
    private static final java.util.Map<SessionKey,Session> SESSIONS=new java.util.LinkedHashMap<>();
    private final SessionKey sessionKey;
    private final java.util.UUID sessionId;
    private final BlockPos position;
    private final Level level;
    private final List<DeskWindow> windows=new ArrayList<>();
    private final List<Hit> hits=new ArrayList<>();
    private float scale,left,top;
    private int mx,my,focus=-1,dragX,dragY;
    private boolean start,quick,locked,searchFocus,showDesktop;
    private String search="",notice="";
    private int noticeTicks;
    private DeskWindow dragging,resizing;
    private long lastTitleClick;
    private DeskWindow lastTitle;
    // Minecraft 1.21.1 scissors use screen coordinates, independently of the pose transform.
    private final class NotesEditBox extends MultiLineEditBox {
        NotesEditBox(int x,int y,int w,int h){super(LaptopDesktopScreen.this.font,x,y,w,h,Component.literal("Write your note here..."),Component.literal("Note text"));}
        @Override public void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partial){
            if(!visible)return;
            g.fill(getX(),getY(),getX()+getWidth(),getY()+getHeight(),0xFF10232D);
            g.renderOutline(getX(),getY(),getWidth(),getHeight(),isFocused()?0xFFE9D994:EDGE);
            g.flush();
            g.enableScissor((int)Math.floor(left+(getX()+1)*scale),(int)Math.floor(top+(getY()+1)*scale),(int)Math.ceil(left+(getX()+getWidth()-1)*scale),(int)Math.ceil(top+(getY()+getHeight()-1)*scale));
            g.pose().pushPose();g.pose().translate(0,-scrollAmount(),0);renderContents(g,mouseX,mouseY,partial);g.pose().popPose();g.flush();g.disableScissor();
            if(scrollbarVisible()){
                int thumb=Math.max(12,getHeight()*getHeight()/Math.max(1,getInnerHeight()+4));
                int offset=(int)((getHeight()-thumb)*scrollAmount()/Math.max(1,getMaxScrollAmount()));
                g.fill(getX()+getWidth()+1,getY(),getX()+getWidth()+7,getY()+getHeight(),0xFF213A46);
                g.fill(getX()+getWidth()+2,getY()+offset,getX()+getWidth()+6,getY()+offset+thumb,0xFFB3C8C5);
            }
        }
    }
    private record Hit(String name,int x,int y,int w,int h,Runnable action) {
        boolean contains(double px,double py){return px>=x&&py>=y&&px<x+w&&py<y+h;}
    }
    private static final class DeskWindow {
        final int app;
        int x,y,w=306,h=174,rx,ry,rw,rh,musicScroll,musicSelection;
        boolean minimized,maximized,hiddenByDesktop;
        final CalculatorEngine calculator=new CalculatorEngine();
        String noteId="",editorId="";
        int notesScroll;
        boolean confirmDelete;
        int settingsTab,photoPage,filesTab,filesScroll,terminalScroll,commandHistory;
        String photoId="",filesQuery="",terminalDraft="";
        boolean photoBusy;
        final List<String> output=new ArrayList<>(List.of("BlockOS Terminal", "Type help to see commands.")),commands=new ArrayList<>();
        EditBox filesSearch,commandInput,browserAddress,programInput;
        final LaptopBrowser browser=new LaptopBrowser();
        final LaptopGames.Game game;
        int uninstallApp=-1,programScroll,paintColor=1,paintLast=-1,calendarOffset,converterCategory,converterFrom,converterTo=1;
        int[] paintUndo;
        String programDraft="";
        boolean paintBusy,clearPaint;
        EditBox noteTitle;
        MultiLineEditBox noteBody;
        DeskWindow(int app,int offset){this.app=app;game=LaptopGames.create(app);x=112+offset;y=35+offset;if(app==4){x=88;y=26;w=364;h=218;}if(app==2){x=132;y=18;w=312;h=230;}if(app==3){x=72;y=18;w=392;h=230;}if(app==0||app>=5){x=52;y=18;w=414;h=230;}if(app==1){x=28;y=12;w=440;h=240;}if(app>=8){x=42;y=12;w=428;h=240;}}
    }
    public LaptopDesktopScreen(BlockPos position){
        super(Component.literal("BlockOS desktop"));this.position=position.immutable();this.level=net.minecraft.client.Minecraft.getInstance().level;
        var access=DesktopAccess.at(level,position);
        if(access==null)throw new IllegalStateException("Computer identity not synchronized");
        sessionKey=new SessionKey(level.dimension(),this.position,Minecraft.getInstance().player.getUUID(),access.device());
        var profile=new LaptopProfile(sessionKey.player(),sessionKey.device());
        programs=profile.programs;prefs=profile.preferences;notes=profile.notes;photos=new LaptopPhotoLibrary(prefs);
        sessionId=access.session();
        Session saved=SESSIONS.get(sessionKey);
        if(prefs.rememberWindows&&saved!=null&&saved.id.equals(sessionId)){windows.addAll(saved.windows.stream().filter(w->programs.installed(w.app)).toList());locked=saved.locked;showDesktop=saved.showDesktop;for(var w:windows){w.noteTitle=null;w.noteBody=null;w.filesSearch=null;w.commandInput=null;w.browserAddress=null;w.programInput=null;}}
        else {SESSIONS.remove(sessionKey);locked=prefs.lockOnOpen;}
    }
    public static void clearSessions(){SESSIONS.clear();}
    LaptopPreferences preferences(){return prefs;}
    public boolean isLaptop(net.minecraft.core.BlockPos pos){return position.equals(pos);}
    @Override public void removed(){
        LaptopGameSounds.stop();for(var window:windows)if(window.game!=null)window.game.clearSounds();
        for(var w:windows){recordGame(w);if(w.game instanceof LaptopGames.Reaction reaction&&(reaction.phase==1||reaction.phase==2))reaction.reset();}programs.save();notes.save();prefs.save();photos.release();if(prefs.stopMusicOnExit)LaptopMusic.stop(position);
        var access=DesktopAccess.at(level,position);
        if(prefs.rememberWindows&&minecraft!=null&&minecraft.level==level&&access!=null
            &&access.session().equals(sessionId)&&sessionKey.device().equals(access.device()))
            SESSIONS.put(sessionKey,new Session(sessionId,new ArrayList<>(windows),locked,showDesktop));
        else SESSIONS.remove(sessionKey);
        while(SESSIONS.size()>256)SESSIONS.remove(SESSIONS.keySet().iterator().next());
        dragging=null;resizing=null;
        super.removed();
    }
    @Override protected void init(){scale=Math.min((width-12f)/W,(height-12f)/H);left=(width-W*scale)/2;top=(height-H*scale)/2;focus=-1;photos.poll();}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void tick(){
        notes.autosave();
        if(!prefs.wallpaperPhoto.isEmpty()||windows.stream().anyMatch(w->!w.minimized&&(w.app==0||w.app==5||w.app==7)))photos.poll();
        if(noticeTicks>0)noticeTicks--;
        for(int id:programs.tick())notify(APPS[id]+" installed - find it on your desktop");
        var running=activeWindow();if(!locked&&!start&&!quick&&running!=null&&running.game!=null){running.game.tick();LaptopGameSounds.play(running.game.drainSounds());if(!(running.game instanceof LaptopGames.Merge))recordGame(running);}
        for(var w:windows)if(w!=running||locked||start||quick){if(w.game!=null)w.game.clearSounds();if(w.game instanceof LaptopGames.Breakout ball)ball.move=0;if(w.game instanceof LaptopGames.Reaction reaction&&(reaction.phase==1||reaction.phase==2))reaction.reset();}
        if(programs.data().timerRunning&&programs.remaining()==0){programs.data().timerRunning=false;programs.data().timerRemaining=0;programs.data().timerNotified=true;programs.changed();programs.save();notify("Focus timer complete - time for a break!");}
        var access=DesktopAccess.at(level,position);
        if(minecraft.player==null||minecraft.level!=level||access==null||!sessionKey.device().equals(access.device())||!sessionId.equals(access.session())
            ||minecraft.player.distanceToSqr(position.getX()+.5,position.getY()+.5,position.getZ()+.5)>64)onClose();
    }
    private void text(GuiGraphics g,String s,int x,int y,int color){g.drawString(font,s,x,y,color,false);}
    private void center(GuiGraphics g,String s,int x,int y,int color){text(g,s,x-font.width(s)/2,y,color);}
    private void rect(GuiGraphics g,int x,int y,int w,int h,int color){g.fill(x,y,x+w,y+h,color);}
    private void frame(GuiGraphics g,int x,int y,int w,int h,int color){rect(g,x+3,y+3,w,h,0x77000000);rect(g,x,y,w,h,color);g.renderOutline(x,y,w,h,EDGE);}
    private void hit(String name,int x,int y,int w,int h,Runnable action){hits.add(new Hit(name,x,y,w,h,action));}
    private boolean hover(int x,int y,int w,int h){return mx>=x&&my>=y&&mx<x+w&&my<y+h;}
    private void button(GuiGraphics g,String name,String label,int x,int y,int w,int h,Runnable action){
        rect(g,x,y,w,h,hover(x,y,w,h)?0xFF3A5261:0xFF293D4B);g.renderOutline(x,y,w,h,0xFF49616E);
        center(g,label,x+w/2,y+(h-8)/2,INK);hit(name,x,y,w,h,action);
    }
    private void notify(String message){notice=message;noticeTicks=65;}
    private void launch(int app){
        if(app==23){start=false;quick=false;searchFocus=false;var a=DesktopAccess.at(level,position);if(a!=null)minecraft.setScreen(new SecurityCameraScreen(this,position,a.device(),a.session()));return;}
        if(app==22){start=false;quick=false;searchFocus=false;var a=DesktopAccess.at(level,position);if(a!=null)minecraft.setScreen(new EmailScreen(this,position,a.device(),a.session()));return;}
        if(!programs.installed(app)){openStore("apps/"+LaptopCatalog.find(app).key());return;}
        if(app==24){start=false;quick=false;searchFocus=false;var access=DesktopAccess.at(level,position);if(access!=null)minecraft.setScreen(new ChromiumScreen(this,position,access.device(),access.session()));return;}
        start=false;quick=false;searchFocus=false;showDesktop=false;
        DeskWindow found=windows.stream().filter(w->w.app==app).findFirst().orElse(null);
        if(found==null){found=new DeskWindow(app,(windows.size()%4)*9);windows.add(found);if(found.game instanceof LaptopGames.Merge merge&&java.util.Arrays.stream(programs.data().merge).anyMatch(n->n>0)){merge.board=programs.data().merge.clone();merge.score=programs.data().mergeScore;merge.over=!merge.canMove();merge.won=java.util.Arrays.stream(merge.board).anyMatch(n->n>=2048);}}else{found.minimized=false;found.hiddenByDesktop=false;raise(found);}defocusNotesExcept(found);
        focus=-1;
    }
    private void defocusNotesExcept(DeskWindow keep){for(var w:windows)if(w!=keep){if(w.noteTitle!=null)w.noteTitle.setFocused(false);if(w.noteBody!=null)w.noteBody.setFocused(false);if(w.filesSearch!=null)w.filesSearch.setFocused(false);if(w.commandInput!=null)w.commandInput.setFocused(false);if(w.browserAddress!=null)w.browserAddress.setFocused(false);if(w.programInput!=null)w.programInput.setFocused(false);}}
    private DeskWindow activeWindow(){return windows.stream().filter(w->!w.minimized).reduce((a,b)->b).orElse(null);}
    private void raise(DeskWindow w){windows.remove(w);windows.add(w);defocusNotesExcept(w);}
    private void closeWindow(DeskWindow w){recordGame(w);programs.save();if(w.app==4)LaptopMusic.stop(position);if(w.app==3)notes.save();windows.remove(w);focus=-1;}
    private void maximize(DeskWindow w){
        if(w.maximized){w.x=w.rx;w.y=w.ry;w.w=w.rw;w.h=w.rh;}else{w.rx=w.x;w.ry=w.y;w.rw=w.w;w.rh=w.h;w.x=3;w.y=3;w.w=W-6;w.h=BAR-6;}
        w.maximized=!w.maximized;dragging=null;resizing=null;
    }
    private void desktop(){
        showDesktop=!showDesktop;for(var w:windows){if(showDesktop){w.hiddenByDesktop=!w.minimized;w.minimized=true;}else if(w.hiddenByDesktop){w.minimized=false;w.hiddenByDesktop=false;}}
        start=false;quick=false;
    }
    private void icon(GuiGraphics g,int app,int x,int y,int size){
        LaptopAppIcons.draw(g,app,x,y,size);
        if(app==22&&EmailClient.unread>0&&size>=20){String badge=EmailClient.unread>99?"99+":Integer.toString(EmailClient.unread);int bw=font.width(badge)+4;rect(g,x+size-bw+3,y-2,bw,10,0xFFCB4D5B);text(g,badge,x+size-bw+5,y-1,0xFFFFFFFF);}
    }
    private void logo(GuiGraphics g,int x,int y,int size){int s=(size-2)/2;for(int a=0;a<2;a++)for(int b=0;b<2;b++)rect(g,x+a*(s+2),y+b*(s+2),s,s,accent());}
    private void wallpaper(GuiGraphics g){
        var photo=photos.find(prefs.wallpaperPhoto);
        if(photo!=null){photos.draw(g,photo,0,0,W,BAR,true,prefs.photoFill);if(prefs.dimPhoto)rect(g,0,0,W,BAR,0x6008111C);return;}
        int[][] colors={{0xFF10283B,0xFF265563,0xFF32756C,0xFF183B45},{0xFF30284F,0xFF594774,0xFF806277,0xFF3D354C},{0xFF173A38,0xFF315D4E,0xFF669572,0xFF203C39},{0xFF442A28,0xFF765041,0xFFB17850,0xFF4E3033},{0xFF1E374C,0xFF457187,0xFF83B8BF,0xFF2A4A63},{0xFF10152C,0xFF282D51,0xFF434C75,0xFF171F39}};
        var c=colors[prefs.wallpaper];g.fillGradient(0,0,W,BAR,c[0],c[1]);
        for(int x=0;x<W;x+=16){int hill=(int)(22*Math.sin(x*.027)+13*Math.sin(x*.061));rect(g,x,183+hill,16,BAR-183-hill,c[2]);rect(g,x,215+hill/2,16,BAR-215-hill/2,c[3]);}
        for(int i=0;i<26;i++){int x=(i*79+137)%W,y=(i*37+19)%143;rect(g,x,y,1+(i%3==0?1:0),1,0x447EDDB2);}
        rect(g,329,47,50,50,0x102EDDA9);rect(g,337,55,34,34,0x2035DAAC);logo(g,341,59,26);
        text(g,"BLOCK OS",322,105,0xFFC0E9DC);text(g,"A little world of your own.",286,118,0xFF8FB6B8);
    }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){g.fill(0,0,width,height,0xB0081018);}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        mx=(int)((mouseX-left)/scale);my=(int)((mouseY-top)/scale);hits.clear();renderBackground(g,mouseX,mouseY,partial);
        g.pose().pushPose();g.pose().translate(left,top,0);g.pose().scale(scale,scale,1);
        rect(g,-4,-4,W+8,H+8,0xFF0A111A);g.renderOutline(-4,-4,W+8,H+8,0xFF556672);wallpaper(g);
        if(locked){drawLock(g);g.pose().popPose();return;}
        var available=matchingApps("");int pages=Math.max(1,(available.size()+7)/8);desktopPage=Math.min(desktopPage,pages-1);
        for(int i=0;i<8&&desktopPage*8+i<available.size();i++){
            int app=available.get(desktopPage*8+i),x=10+(i/4)*64,y=12+(i%4)*56;
            if(hover(x,y,58,49))rect(g,x,y,58,49,0x553A8594);
            icon(g,app,x+18,y+3,24);if(prefs.iconLabels)center(g,font.plainSubstrByWidth(APPS[app],59),x+29,y+32,INK);hit("Open "+APPS[app],x,y,58,49,()->launch(app));
        }
        if(pages>1){button(g,"Desktop previous","<",10,237,22,17,()->desktopPage=Math.floorMod(desktopPage-1,pages));center(g,(desktopPage+1)+" / "+pages,74,241,INK);button(g,"Desktop next",">",115,237,22,17,()->desktopPage=(desktopPage+1)%pages);}
        int layer=20;for(var window:List.copyOf(windows))if(!window.minimized){g.pose().pushPose();g.pose().translate(0,0,layer);drawWindow(g,window);g.flush();g.pose().popPose();layer+=20;}
        g.pose().pushPose();g.pose().translate(0,0,600);
        drawTaskbar(g);
        if(start)drawStart(g);
        if(quick)drawQuick(g);
        if(noticeTicks>0){frame(g,147,BAR-31,320,22,0xFF203A43);text(g,notice,155,BAR-24,INK);}
        if(focus>=0&&focus<hits.size()){Hit h=hits.get(focus);g.renderOutline(h.x,h.y,h.w,h.h,accent());}
        g.flush();g.pose().popPose();g.pose().popPose();
    }
    private void drawWindow(GuiGraphics g,DeskWindow w){
        boolean active=windows.stream().filter(v->!v.minimized).reduce((a,b)->b).orElse(null)==w;
        frame(g,w.x,w.y,w.w,w.h,prefs.panel());if(active)g.renderOutline(w.x,w.y,w.w,w.h,0xFF84BCAF);
        hit("Window "+APPS[w.app],w.x,w.y,w.w,w.h,()->raise(w));
        rect(g,w.x+1,w.y+1,w.w-2,23,active?0xFF324956:0xFF293A47);icon(g,w.app,w.x+7,w.y+5,14);text(g,APPS[w.app],w.x+27,w.y+8,INK);
        hit("Move "+APPS[w.app],w.x+1,w.y+1,w.w-77,23,()->{
            raise(w);long now=System.currentTimeMillis();if(lastTitle==w&&now-lastTitleClick<300){maximize(w);lastTitle=null;}else{if(!w.maximized){dragging=w;dragX=mx-w.x;dragY=my-w.y;}lastTitle=w;lastTitleClick=now;}
        });
        button(g,"Minimize "+APPS[w.app],"_",w.x+w.w-73,w.y+3,22,18,()->{w.minimized=true;focus=-1;});
        button(g,"Maximize "+APPS[w.app],w.maximized?"<>":"[]",w.x+w.w-49,w.y+3,22,18,()->maximize(w));
        button(g,"Close "+APPS[w.app],"x",w.x+w.w-25,w.y+3,22,18,()->closeWindow(w));
        if(w.app==2||w.app==3){if(w.app==2)drawCalculator(g,w);else drawNotes(g,w);if(!w.maximized)hit("Resize "+APPS[w.app],w.x+w.w-12,w.y+w.h-12,12,12,()->{raise(w);resizing=w;});return;}
        if(w.app==4){drawMusic(g,w);if(!w.maximized)hit("Resize Music",w.x+w.w-12,w.y+w.h-12,12,12,()->{raise(w);resizing=w;});return;}
        if(w.app==0||w.app==1||w.app>=5){switch(w.app){case 0->drawFiles(g,w);case 1->drawBrowser(g,w);case 5->drawGallery(g,w);case 6->drawTerminal(g,w);case 7->drawSettings(g,w);default->drawProgram(g,w);}if(!w.maximized)hit("Resize "+APPS[w.app],w.x+w.w-12,w.y+w.h-12,12,12,()->{raise(w);resizing=w;});return;}
        rect(g,w.x+1,w.y+25,70,w.h-26,0xFF13212B);text(g,"BLOCK OS",w.x+9,w.y+36,MUTED);
        icon(g,w.app,w.x+24,w.y+57,23);center(g,APPS[w.app],w.x+35,w.y+90,INK);
        int cx=w.x+71+(w.w-71)/2;
        icon(g,w.app,cx-14,w.y+46,28);center(g,APPS[w.app],cx,w.y+86,INK);center(g,"Releasing Soon",cx,w.y+103,accent());
        center(g,"This app is coming in a future update.",cx,w.y+121,MUTED);
        rect(g,w.x+72,w.y+w.h-20,w.w-73,19,0xFF12212B);text(g,"Desktop preview",w.x+80,w.y+w.h-14,MUTED);
        for(int i=0;i<3;i++)rect(g,w.x+w.w-5-i*3,w.y+w.h-3,2,1,EDGE);
        if(!w.maximized)hit("Resize "+APPS[w.app],w.x+w.w-12,w.y+w.h-12,12,12,()->{raise(w);resizing=w;});
    }
    private void browse(DeskWindow w,String url){w.uninstallApp=-1;w.browser.tab().navigate(url);programs.visit(w.browser.tab().url);w.browserAddress=null;raise(w);focus=-1;}
    private void openStore(String url){launch(1);browse(activeWindow(),url);}
    private void browserMove(DeskWindow w,int direction){w.browser.tab().move(direction);w.browserAddress=null;raise(w);}
    private void installApp(int app){if(programs.download(app))notify("Installing "+APPS[app]+"...");}
    private void uninstallApp(int app){for(var w:List.copyOf(windows))if(w.app==app)closeWindow(w);for(var entry:SESSIONS.entrySet())if(entry.getKey().equals(sessionKey))entry.getValue().windows().removeIf(w->w.app==app);programs.uninstall(app);notify(APPS[app]+" uninstalled; saved data kept");}
    private void wrap(GuiGraphics g,String message,int x,int y,int width,int lines,int color){
        var rest=message;for(int i=0;i<lines&&!rest.isEmpty();i++){String part=font.plainSubstrByWidth(rest,width);if(part.length()<rest.length()){int space=part.lastIndexOf(' ');if(space>0)part=part.substring(0,space);}if(part.isEmpty())break;text(g,part,x,y+i*11,color);rest=rest.substring(part.length()).stripLeading();}
    }
    private void drawBrowser(GuiGraphics g,DeskWindow w){
        int x=w.x,y=w.y;var tab=w.browser.tab();
        rect(g,x+1,y+25,w.w-2,22,0xFF12222F);
        for(int i=0;i<w.browser.tabs.size();i++){int index=i,tx=x+7+i*70;var t=w.browser.tabs.get(i);rect(g,tx,y+28,67,16,index==w.browser.selected?0xFF385463:0xFF213442);text(g,font.plainSubstrByWidth(LaptopBrowser.title(t.url),60),tx+4,y+32,index==w.browser.selected?INK:MUTED);hit("Browser tab "+i,tx,y+28,67,16,()->{w.browser.selected=index;w.browserAddress=null;raise(w);});}
        button(g,"Browser new tab","+",x+7+w.browser.tabs.size()*70,y+28,18,16,()->{if(!w.browser.add())notify("Up to five browser tabs");w.browserAddress=null;});
        button(g,"Browser close tab","x",x+w.w-25,y+28,18,16,()->{w.browser.close();w.browserAddress=null;});
        button(g,"Browser back","<",x+7,y+51,22,19,()->browserMove(w,-1));button(g,"Browser forward",">",x+31,y+51,22,19,()->browserMove(w,1));button(g,"Browser home","H",x+55,y+51,22,19,()->browse(w,"home"));
        int aw=w.w-153;
        if(w.browserAddress==null||w.browserAddress.getWidth()!=aw){w.browserAddress=new EditBox(font,x+82,y+51,aw,19,Component.literal("BlockNet address or search"));w.browserAddress.setMaxLength(160);w.browserAddress.setValue(tab.draft);w.browserAddress.setResponder(v->w.browser.tab().draft=v);}
        w.browserAddress.setPosition(x+82,y+51);w.browserAddress.render(g,mx,my,0);hit("Browser address",x+82,y+51,aw,19,()->{raise(w);w.browserAddress.setFocused(true);w.browserAddress.mouseClicked(mx,my,0);});
        button(g,"Browser go","Go",x+w.w-65,y+51,29,19,()->browse(w,tab.draft));button(g,"Browser bookmark",programs.data().bookmarks.contains(tab.url)?"*":"+*",x+w.w-33,y+51,26,19,()->{programs.bookmark(tab.url);notify(programs.data().bookmarks.contains(tab.url)?"Page bookmarked":"Bookmark removed");});
        String[] names={"Home","Store","Games","Tools","My Apps","More"},routes={"home","apps","games","tools","library","portal"};int navW=(w.w-16)/6;
        for(int i=0;i<6;i++){int index=i;button(g,"Browser "+names[i],names[i],x+7+i*navW,y+76,navW-3,17,()->browse(w,routes[index]));if(tab.url.equals("blocknet://"+routes[i]))rect(g,x+10+i*navW,y+92,navW-9,1,accent());}
        int bx=x+8,by=y+100,bw=w.w-16,bh=w.h-122;rect(g,bx,by,bw,bh,0xFF10202D);
        String route=tab.url.substring(11).split("[/?]",2)[0];
        switch(route){
            case "home"->browserHome(g,w,bx,by,bw,bh);
            case "apps","games","tools","library","search"->browserApps(g,w,bx,by,bw,bh,route);
            case "guides"->browserArticles(g,w,bx,by,bw,bh,false);
            case "news"->browserArticles(g,w,bx,by,bw,bh,true);
            case "wallpapers"->browserWallpapers(g,w,bx,by,bw,bh);
            case "scores"->browserScores(g,w,bx,by,bw,bh);
            case "portal"->browserPortal(g,w,bx,by,bw,bh);
            case "bookmarks","history"->browserLinks(g,w,bx,by,bw,bh,route.equals("history"));
            case "external"->{center(g,"BlockNet is an in-game browser",bx+bw/2,by+19,INK);center(g,"Real websites aren't loaded in this version.",bx+bw/2,by+38,MUTED);wrap(g,"Try Store, Games or Tools, or search a program's name.",bx+20,by+58,bw-40,2,MUTED);button(g,"Return to BlockNet","Back to BlockNet",bx+bw/2-74,by+bh-28,148,22,()->browse(w,"home"));}
            default->{center(g,"Page not found",bx+bw/2,by+35,INK);button(g,"Browser page home","Go home",bx+bw/2-48,by+65,96,22,()->browse(w,"home"));}
        }
        text(g,"BLOCKNET / In-game sites",x+10,y+w.h-14,MUTED);int n=programs.data().downloads.size();text(g,n>0?n+" install"+(n==1?"":"s")+" in progress":"15 free programs",x+w.w-139,y+w.h-14,n>0?accent():MUTED);
    }
    private void browserPortal(GuiGraphics g,DeskWindow w,int x,int y,int width,int height){
        text(g,"Discover BlockNet",x+8,y+6,accent());String[] titles={"Bookmarks","History","High Scores","BlockOS News","Help & Guides","Wallpapers"},routes={"bookmarks","history","scores","news","guides","wallpapers"};int[] icons={1,6,8,3,17,5};int cw=(width-12)/2,ch=(height-24)/3;
        for(int i=0;i<6;i++){int id=i,xx=x+5+i%2*(cw+3),yy=y+23+i/2*ch;rect(g,xx,yy,cw-1,ch-3,hover(xx,yy,cw-1,ch-3)?0xFF345464:0xFF233C4B);icon(g,icons[i],xx+6,yy+5,18);text(g,titles[i],xx+32,yy+(ch-8)/2,INK);hit("BlockNet link "+titles[i],xx,yy,cw-1,ch-3,()->browse(w,routes[id]));}
    }
    private void browserHome(GuiGraphics g,DeskWindow w,int x,int y,int width,int height){
        g.fillGradient(x,y,x+width,y+43,0xFF284756,0xFF163447);logo(g,x+12,y+11,22);text(g,"Welcome to BlockNet",x+44,y+9,INK);text(g,"Games, creativity, and tools for your blocky desktop.",x+44,y+24,MUTED);
        String[] titles={"App Store","Pixel Arcade","Build Better","Wallpapers"},descs={"15 free downloads","Eight playable games","Guides and shortcuts","Six desktop landscapes"},urls={"apps","games","guides","wallpapers"};int[] icons={1,8,17,5};int cw=(width-9)/2,ch=Math.max(32,(height-48)/2);
        for(int i=0;i<4;i++){int id=i,xx=x+3+(i%2)*(cw+3),yy=y+47+(i/2)*ch;rect(g,xx,yy,cw-1,ch-3,hover(xx,yy,cw-1,ch-3)?0xFF345464:0xFF233C4B);icon(g,icons[i],xx+7,yy+7,19);text(g,titles[i],xx+34,yy+6,INK);text(g,descs[i],xx+34,yy+19,MUTED);hit("BlockNet tile "+titles[i],xx,yy,cw-1,ch-3,()->browse(w,urls[id]));}
    }
    private void browserApps(GuiGraphics g,DeskWindow w,int x,int y,int width,int height,String route){
        var tab=w.browser.tab();String[] path=tab.url.substring(11).split("[/?]",3);
        if(path.length>1&&route.equals("apps")){var app=LaptopCatalog.find(path[1]);if(app!=null){browserDetail(g,w,app,x,y,width,height);return;}}
        String q=route.equals("search")?LaptopBrowser.query(tab.url).toLowerCase(java.util.Locale.ROOT):"";
        var all=LaptopCatalog.ALL.stream().filter(a->(!route.equals("games")||a.category().equals("Games"))&&(!route.equals("tools")||a.category().equals("Tools"))&&(!route.equals("library")||programs.installed(a.id())||programs.data().downloads.containsKey(a.id()))&&(q.isEmpty()||(a.name()+" "+a.category()+" "+a.tagline()).toLowerCase(java.util.Locale.ROOT).contains(q))).toList();
        text(g,route.equals("search")?"Search: "+font.plainSubstrByWidth(q,width-99):route.equals("library")?"Your installed apps":route.equals("games")?"Pixel Arcade":route.equals("tools")?"The Toolbox":"BlockNet App Store",x+7,y+5,accent());
        int rows=Math.max(1,(height-39)/39),per=rows*2,pages=Math.max(1,(all.size()+per-1)/per),page=Math.max(0,Math.min(tab.scroll,pages-1));tab.scroll=page;int cw=(width-9)/2;
        for(int i=0;i<per&&page*per+i<all.size();i++){var app=all.get(page*per+i);int xx=x+4+(i%2)*(cw+3),yy=y+20+(i/2)*39;rect(g,xx,yy,cw-1,35,0xFF243B49);icon(g,app.id(),xx+6,yy+6,23);text(g,font.plainSubstrByWidth(app.name(),cw-95),xx+35,yy+5,INK);text(g,app.category()+" / Free",xx+35,yy+18,MUTED);hit("Details "+app.key(),xx,yy,cw-1,35,()->browse(w,"apps/"+app.key()));
            var progress=programs.data().downloads.get(app.id());String label=progress!=null?(progress*100/40)+"%":programs.installed(app.id())?"Open":"Get";
            button(g,"Store "+app.key(),label,xx+cw-55,yy+9,47,21,()->{if(programs.installed(app.id()))launch(app.id());else installApp(app.id());});
        }
        if(all.isEmpty())center(g,route.equals("library")?"Your library is empty. Get apps from the Store.":"No matching programs.",x+width/2,y+52,MUTED);
        button(g,"Browser page previous","<",x+6,y+height-19,22,17,()->tab.scroll=Math.floorMod(tab.scroll-1,pages));button(g,"Browser page next",">",x+width-29,y+height-19,22,17,()->tab.scroll=(tab.scroll+1)%pages);center(g,(page+1)+" / "+pages+"  -  "+all.size()+" programs",x+width/2,y+height-15,MUTED);
    }
    private void browserDetail(GuiGraphics g,DeskWindow w,LaptopCatalog.App app,int x,int y,int width,int height){
        icon(g,app.id(),x+11,y+11,32);text(g,app.name(),x+53,y+10,INK);text(g,app.category()+" / Free / "+(app.id()==24?"Chromium":"BlockNet Originals"),x+53,y+24,accent());wrap(g,app.tagline(),x+11,y+50,width-22,1,INK);wrap(g,app.instructions(),x+11,y+66,width-22,3,MUTED);
        int yy=y+height-26;var progress=programs.data().downloads.get(app.id());
        if(programs.installed(app.id())){button(g,"Open installed "+app.key(),"Open app",x+11,yy,91,21,()->launch(app.id()));button(g,"Uninstall "+app.key(),w.uninstallApp==app.id()?"Confirm remove":"Uninstall",x+109,yy,101,21,()->{if(w.uninstallApp==app.id()){uninstallApp(app.id());w.uninstallApp=-1;}else{w.uninstallApp=app.id();notify("Click Confirm remove; your app data is kept");}});}
        else if(progress!=null){rect(g,x+11,yy,185,21,0xFF263E4C);rect(g,x+12,yy+1,progress*183/40,19,0xFF3C786C);center(g,"Installing... "+progress*100/40+"%",x+104,yy+7,INK);button(g,"Cancel install "+app.key(),"Cancel",x+204,yy,70,21,()->programs.cancel(app.id()));}
        else button(g,"Download "+app.key(),"Download",x+11,yy,101,21,()->installApp(app.id()));
        button(g,"Back to Store","All apps",x+width-82,yy,72,21,()->browse(w,"apps"));
    }
    private void browserLinks(GuiGraphics g,DeskWindow w,int x,int y,int width,int height,boolean history){
        text(g,history?"Recently visited":"Your bookmarks",x+9,y+6,accent());button(g,"Browser show history",history?"Bookmarks":"History",x+width-85,y+3,75,18,()->browse(w,history?"bookmarks":"history"));
        var list=history?programs.data().history:programs.data().bookmarks;int rows=Math.max(1,(height-34)/20),max=Math.max(0,list.size()-rows);w.browser.tab().scroll=Math.min(w.browser.tab().scroll,max);
        for(int i=0;i<rows&&w.browser.tab().scroll+i<list.size();i++){String url=list.get(w.browser.tab().scroll+i);int yy=y+28+i*20;button(g,"Visit "+url,font.plainSubstrByWidth(LaptopBrowser.title(url)+" - "+url,width-37),x+9,yy,width-18,18,()->browse(w,url));}
        if(list.isEmpty())text(g,"Use the star beside the address to add a bookmark.",x+10,y+35,MUTED);
    }
    private void browserWallpapers(GuiGraphics g,DeskWindow w,int x,int y,int width,int height){
        text(g,"Landscapes for your desktop",x+9,y+5,accent());int cw=(width-20)/3,ch=(height-27)/2;int[] colors={0xFF32756C,0xFF806277,0xFF669572,0xFFB17850,0xFF83B8BF,0xFF434C75};
        for(int i=0;i<6;i++){int id=i,xx=x+6+i%3*(cw+4),yy=y+22+i/3*ch;rect(g,xx,yy,cw,ch-5,0xFF23384B);for(int bx=0;bx<cw;bx+=8){int h=(int)(7*Math.sin(bx*.09));rect(g,xx+bx,yy+13+h,Math.min(8,cw-bx),Math.max(1,ch-27-h),colors[i]);}center(g,LaptopPreferences.WALLPAPERS[i],xx+cw/2,yy+ch-18,INK);hit("Browser wallpaper "+LaptopPreferences.WALLPAPERS[i],xx,yy,cw,ch-5,()->chooseWallpaper(id));if(prefs.wallpaperPhoto.isEmpty()&&prefs.wallpaper==i)g.renderOutline(xx,yy,cw,ch-5,accent());}
    }
    private void browserScores(GuiGraphics g,DeskWindow w,int x,int y,int width,int height){
        text(g,"Your personal bests",x+10,y+7,accent());var games=LaptopCatalog.ALL.stream().filter(a->a.id()<16).toList();int rows=Math.max(1,(height-30)/19),max=Math.max(0,games.size()-rows);w.browser.tab().scroll=Math.min(w.browser.tab().scroll,max);
        for(int i=0;i<rows&&i+w.browser.tab().scroll<games.size();i++){var app=games.get(i+w.browser.tab().scroll);int yy=y+27+i*19;icon(g,app.id(),x+10,yy-2,14);text(g,app.name(),x+32,yy,INK);long score=programs.best(app.key());String value=score==0?"Play to set a best":app.id()==15?score+" ms":app.id()==13?score+" turns":app.id()==10?score+" seconds":score+" points";text(g,value,x+width-143,yy,score==0?MUTED:accent());hit("Score app "+app.key(),x+8,yy-3,width-16,18,()->{if(programs.installed(app.id()))launch(app.id());else browse(w,"apps/"+app.key());});}
    }
    private void browserArticles(GuiGraphics g,DeskWindow w,int x,int y,int width,int height,boolean news){
        String[] titles=news?new String[]{"Welcome to BlockNet 1.0","Eight games, one laptop","Create something blocky","Your own private library"}:new String[]{"Getting started","Window shortcuts","Install and manage apps","Game controls","Paint and the shared Gallery","Your saves"};
        String[] bodies=news?new String[]{"The laptop now has tabs, bookmarks, search and an App Store. Everything here is built into the mod; internet is not required.","Snake, 2048, Mines, Breakout, Blockfall, Memory, Tic Tac Toe and Reaction are free in the Arcade. Every game has its own controls and saved personal bests.","Make pixel art in Paint, export it to Gallery, and use it as a phone or laptop wallpaper. Planner, Calendar, Converter and Focus Timer help with your next build.","Installed programs, game records, tasks and your drawing belong to your private profile for this laptop. Uninstalling keeps your app's saved data."}:new String[]{"Crouch-right-click opens or closes the lid. Normal right-click opens BlockOS. Escape leaves the laptop; your windows resume until you physically close the lid.","Drag a window's title to move it. Double-click the title to maximize. Minimize restores from the taskbar; Alt+F4 closes the active app. Start searches installed apps.","Open Browser, then Store. Get downloads an included app; Open launches it. My Apps shows your library. An app's detail page offers uninstall and keeps saved progress.","Snake: arrows/WASD. 2048: arrows. Breakout: A/D or arrows, Space launches. Blockfall: arrows, Space drops, P pauses. Mines: click reveals, right-click flags.","Paint has a 32 x 32 saved canvas, sixteen colors, erase and undo. Export creates a PNG in the same local Gallery your phones and laptop already use.","Programs save separately for each laptop and player on this computer. Your installed apps, scores, bookmarks, drawing, tasks, 2048 board and focus timer survive restarting Minecraft."};
        int index=Math.floorMod(w.browser.tab().scroll,titles.length);text(g,news?"BLOCKOS JOURNAL":"BLOCKNET HELP",x+10,y+5,accent());text(g,titles[index],x+10,y+24,INK);wrap(g,bodies[index],x+10,y+43,width-20,Math.max(1,(height-65)/11),MUTED);
        button(g,"Browser article previous","<",x+10,y+height-22,24,18,()->w.browser.tab().scroll=Math.floorMod(index-1,titles.length));button(g,"Browser article next",">",x+width-34,y+height-22,24,18,()->w.browser.tab().scroll=(index+1)%titles.length);center(g,"Article "+(index+1)+" / "+titles.length,x+width/2,y+height-16,MUTED);
    }

    private static final int[] PALETTE={0xFFF2EEE5,0xFF152330,0xFF526879,0xFFAFBEC2,0xFFED6C71,0xFFECAD68,0xFFECD882,0xFF91CC8A,0xFF4B9772,0xFF87D6D1,0xFF5AA2D8,0xFF365899,0xFFB69ADA,0xFF754C92,0xFFE4A6C9,0xFF8E6153};

    private void drawProgram(GuiGraphics g,DeskWindow w){
        switch(w.app){case 8,9,10,11,12,13,14,15->drawGame(g,w);case 16->drawPaint(g,w);case 17->drawPlanner(g,w);case 18->drawCalendar(g,w);case 19->drawConverter(g,w);case 20->drawFocus(g,w);case 21->drawSystem(g,w);}
    }
    private void recordGame(DeskWindow w){
        var a=LaptopCatalog.find(w.app);var game=w.game;if(game==null)return;
        if(game instanceof LaptopGames.Merge merge){programs.data().merge=merge.board.clone();programs.data().mergeScore=merge.score;programs.changed();}
        if(game instanceof LaptopGames.Mines mines){if(mines.won)programs.score(a.key(),Math.max(1,mines.ticks/20),true);}
        else if(game instanceof LaptopGames.Memory memory){if(memory.over)programs.score(a.key(),memory.turns,true);}
        else if(game instanceof LaptopGames.Reaction reaction){if(reaction.phase==3)programs.score(a.key(),reaction.result,true);}
        else programs.score(a.key(),game.score,false);
    }
    private void resetGame(DeskWindow w){recordGame(w);w.game.restart();if(w.game instanceof LaptopGames.Merge)recordGame(w);}
    private void drawGame(GuiGraphics g,DeskWindow w){
        int x=w.x,y=w.y,side=x+w.w-128;var game=w.game;var app=LaptopCatalog.find(w.app);
        text(g,app.tagline(),x+11,y+34,accent());button(g,"New "+app.key(),"New game",side,y+29,116,21,()->resetGame(w));
        int boardW=w.w-146,boardH=w.h-79,bx=x+12,by=y+60;rect(g,bx,by,boardW,boardH,0xFF0B1924);
        text(g,"PERSONAL BEST",side,y+64,MUTED);long best=programs.best(app.key());text(g,best==0?"--":best+(w.app==15?" ms":w.app==13?" turns":w.app==10?" sec":""),side,y+79,accent());
        text(g,"Score: "+game.score,side,y+99,INK);
        wrap(g,switch(w.app){case 8->"Arrows / WASD steer. Space pauses.";case 9->"Arrows slide. Equal tiles merge.";case 10->"Click reveals. Right-click flags.";case 11->"A/D move. Space starts or pauses.";case 12->"Arrows move / rotate. Space drops. P pauses.";case 13->"Click two cards to find a pair.";case 14->"You play X. The laptop plays O.";default->"Wait for green, then click!";},side,y+120,116,4,MUTED);
        if(w.app==8){var snake=(LaptopGames.Snake)game;int cell=Math.min(boardW/18,boardH/14),xx=bx+(boardW-cell*18)/2,yy=by+(boardH-cell*14)/2;for(int row=0;row<14;row++)for(int col=0;col<18;col++)rect(g,xx+col*cell,yy+row*cell,cell,cell,(row+col)%2==0?0xFF142C30:0xFF183237);for(int p:snake.body)rect(g,xx+p%18*cell+1,yy+p/18*cell+1,cell-2,cell-2,p==snake.body.getFirst()?0xFFCEEBC1:0xFF7BD398);if(snake.food>=0)rect(g,xx+snake.food%18*cell+2,yy+snake.food/18*cell+2,cell-4,cell-4,0xFFED8687);button(g,"Snake start",snake.started?(snake.paused?"Resume":"Pause"):"Start",side,y+175,116,22,snake::toggle);}
        if(w.app==9){var merge=(LaptopGames.Merge)game;int cell=Math.min(boardW/4,boardH/4),xx=bx+(boardW-cell*4)/2,yy=by+(boardH-cell*4)/2;for(int i=0;i<16;i++){int value=merge.board[i],tx=xx+i%4*cell,ty=yy+i/4*cell;int color=value==0?0xFF293D4A:PALETTE[Math.min(15,2+Integer.numberOfTrailingZeros(value))];rect(g,tx+2,ty+2,cell-4,cell-4,color);if(value>0)center(g,""+value,tx+cell/2,ty+cell/2-4,0xFF142634);}button(g,"2048 undo","Undo",side,y+175,116,22,()->{merge.undo();recordGame(w);});if(merge.won)text(g,"2048 reached!",side,y+155,accent());}
        if(w.app==10){var mines=(LaptopGames.Mines)game;int cell=Math.min(boardW/12,boardH/9),xx=bx+(boardW-cell*12)/2,yy=by+(boardH-cell*9)/2;for(int i=0;i<108;i++){int tx=xx+i%12*cell,ty=yy+i/12*cell,id=i;rect(g,tx+1,ty+1,cell-2,cell-2,mines.seen[i]?0xFF213745:0xFF526A76);if(mines.flags[i]){rect(g,tx+cell/2-2,ty+3,1,cell-6,INK);rect(g,tx+cell/2-1,ty+3,5,4,0xFFF0909A);}else if(mines.bombs[i]&&mines.over){rect(g,tx+3,ty+3,cell-6,cell-6,0xFFE58A84);}else if(mines.seen[i]&&mines.nearby(i)>0)center(g,""+mines.nearby(i),tx+cell/2,ty+(cell-8)/2,PALETTE[7+Math.min(7,mines.nearby(i))]);hit("Mines cell "+i,tx,ty,cell,cell,()->{mines.reveal(id);recordGame(w);});}text(g,"Flags: "+mines.flagged+" / 16",side,y+169,MUTED);text(g,"Time: "+mines.ticks/20+"s",side,y+185,INK);}
        if(w.app==11){var ball=(LaptopGames.Breakout)game;float z=Math.min(boardW/240f,boardH/144f);g.pose().pushPose();g.pose().translate(bx+(boardW-240*z)/2,by+(boardH-144*z)/2,0);g.pose().scale(z,z,1);for(int i=0;i<30;i++)if(ball.bricks[i])rect(g,i%6*38+7,i/6*12+14,32,9,new int[]{0xFFE794A5,0xFFE7B375,0xFFE4DA85,0xFF92D3AD,0xFF93BCE8}[i/6]);rect(g,(int)ball.paddle,134,40,4,0xFFBAE9E0);rect(g,(int)ball.x-3,(int)ball.y-3,6,6,INK);g.pose().popPose();text(g,"Lives: "+ball.lives,side,y+163,INK);button(g,"Breakout launch",ball.launched?(ball.paused?"Resume":"Pause"):"Launch",side,y+180,116,22,ball::launch);}
        if(w.app==12){var fall=(LaptopGames.Fall)game;int cell=Math.min(boardW/10,boardH/18),xx=bx+(boardW-cell*10)/2,yy=by+(boardH-cell*18)/2;for(int row=0;row<18;row++)for(int col=0;col<10;col++){int value=fall.board[row*10+col];rect(g,xx+col*cell,yy+row*cell,cell-1,cell-1,value==0?0xFF20313E:PALETTE[4+value]);}int ghost=fall.y;while(fall.fits(fall.x,ghost+1,fall.piece))ghost++;for(int i=0;i<8;i+=2){g.renderOutline(xx+(fall.x+fall.piece[i])*cell,yy+(ghost+fall.piece[i+1])*cell,cell-1,cell-1,0xFF627B85);rect(g,xx+(fall.x+fall.piece[i])*cell,yy+(fall.y+fall.piece[i+1])*cell,cell-1,cell-1,PALETTE[5+fall.type]);}text(g,"Lines: "+fall.lines,side,y+166,INK);text(g,"Next",bx+6,by+4,MUTED);int[] shape=fallPreview(fall.next);for(int i=0;i<8;i+=2)rect(g,bx+5+shape[i]*6,by+17+shape[i+1]*6,5,5,PALETTE[5+fall.next]);button(g,"Blockfall pause",fall.started?(fall.paused?"Resume":"Pause"):"Start",side,y+183,116,22,fall::toggle);}
        if(w.app==13){var memory=(LaptopGames.Memory)game;int cell=Math.min(boardW/4,boardH/4),xx=bx+(boardW-cell*4)/2,yy=by+(boardH-cell*4)/2;for(int i=0;i<16;i++){int id=i,tx=xx+i%4*cell,ty=yy+i/4*cell;boolean open=memory.matched[i]||memory.first==i||memory.second==i;rect(g,tx+2,ty+2,cell-4,cell-4,open?0xFF365E65:0xFF43506A);if(open)icon(g,8+memory.cards[i],tx+cell/2-10,ty+cell/2-10,20);else center(g,"?",tx+cell/2,ty+cell/2-4,0xFFB4C5DB);hit("Memory card "+i,tx,ty,cell,cell,()->{memory.flip(id);recordGame(w);});}text(g,"Pairs: "+memory.pairs+" / 8",side,y+167,INK);text(g,"Turns: "+memory.turns,side,y+184,INK);}
        if(w.app==14){var tic=(LaptopGames.Tic)game;int cell=Math.min(boardW/3,boardH/3),xx=bx+(boardW-cell*3)/2,yy=by+(boardH-cell*3)/2;for(int i=0;i<9;i++){int id=i,tx=xx+i%3*cell,ty=yy+i/3*cell;rect(g,tx+2,ty+2,cell-4,cell-4,0xFF314A58);if(tic.board[i]!=0){g.pose().pushPose();g.pose().translate(tx+cell/2,ty+cell/2-10,0);g.pose().scale(2.5f,2.5f,1);center(g,tic.board[i]==1?"X":"O",0,0,tic.board[i]==1?0xFF94DFC1:0xFFEAB38A);g.pose().popPose();}hit("Tic cell "+i,tx,ty,cell,cell,()->{tic.play(id);recordGame(w);});}button(g,"Tic difficulty",tic.expert?"Expert":"Casual",side,y+176,116,22,()->{tic.expert=!tic.expert;resetGame(w);});}
        if(w.app==15){var reaction=(LaptopGames.Reaction)game;int bg=reaction.phase==1?0xFF78544C:reaction.phase==2?0xFF3F806A:0xFF314D63;rect(g,bx+6,by+6,boardW-12,boardH-12,bg);String label=switch(reaction.phase){case 1->"Wait for green...";case 2->"CLICK NOW!";case 3->reaction.result+" ms";case 4->"Too soon! Try again.";default->"Ready when you are.";};center(g,label,bx+boardW/2,by+boardH/2-4,INK);hit("Reaction target",bx+6,by+6,boardW-12,boardH-12,()->{reaction.click();recordGame(w);});button(g,"Reaction start","Start test",side,y+177,116,22,reaction::begin);}
        if(game.over||game.paused){String status=game.paused?"PAUSED":w.app==10&&((LaptopGames.Mines)game).won||w.app==13?"YOU WIN!":w.app==14?switch(((LaptopGames.Tic)game).winner){case 1->"YOU WIN!";case 2->"LAPTOP WINS";default->"DRAW";}:"GAME OVER";rect(g,bx,by+boardH/2-12,boardW,24,0xDD162939);center(g,status,bx+boardW/2,by+boardH/2-4,INK);}
        text(g,"Games pause while another window is active",x+11,y+w.h-12,MUTED);
    }
    private int[] fallPreview(int type){return switch(type){case 0->new int[]{0,0,1,0,2,0,3,0};case 1->new int[]{0,0,1,0,0,1,1,1};case 2->new int[]{1,0,0,1,1,1,2,1};case 3->new int[]{1,0,2,0,0,1,1,1};case 4->new int[]{0,0,1,0,1,1,2,1};case 5->new int[]{0,0,0,1,1,1,2,1};default->new int[]{2,0,0,1,1,1,2,1};};}
    private boolean gameKey(DeskWindow w,int key){
        int direction=switch(key){case GLFW.GLFW_KEY_UP,GLFW.GLFW_KEY_W->0;case GLFW.GLFW_KEY_RIGHT,GLFW.GLFW_KEY_D->1;case GLFW.GLFW_KEY_DOWN,GLFW.GLFW_KEY_S->2;case GLFW.GLFW_KEY_LEFT,GLFW.GLFW_KEY_A->3;default->-1;};
        if(w.game instanceof LaptopGames.Snake snake){if(direction>=0)snake.steer(direction);else if(key==GLFW.GLFW_KEY_SPACE)snake.toggle();}
        if(w.game instanceof LaptopGames.Merge merge&&direction>=0){merge.move(direction);recordGame(w);}
        if(w.game instanceof LaptopGames.Breakout ball){if(direction==1||direction==3)ball.move=direction==1?1:-1;else if(key==GLFW.GLFW_KEY_SPACE)ball.launch();}
        if(w.game instanceof LaptopGames.Fall fall){if(direction==0)fall.rotate();if(direction==1||direction==3)fall.shift(direction==1?1:-1);if(direction==2)fall.down();if(key==GLFW.GLFW_KEY_SPACE)fall.hard();if(key==GLFW.GLFW_KEY_P)fall.toggle();recordGame(w);}
        return direction>=0||key==GLFW.GLFW_KEY_SPACE||key==GLFW.GLFW_KEY_P;
    }
    private int paintCell(DeskWindow w,double px,double py){int size=Math.min((w.w-148)/32,(w.h-76)/32),x=w.x+13,y=w.y+53;if(size<1||px<x||py<y||px>=x+size*32||py>=y+size*32)return -1;return (int)((py-y)/size)*32+(int)((px-x)/size);}
    private void paintAt(DeskWindow w,int cell){if(cell<0||cell>=1024){w.paintLast=-1;return;}int from=w.paintLast<0?cell:w.paintLast,x=from%32,y=from/32,tx=cell%32,ty=cell/32,dx=Math.abs(tx-x),dy=-Math.abs(ty-y),sx=x<tx?1:-1,sy=y<ty?1:-1,error=dx+dy;while(true){programs.data().paint[y*32+x]=w.paintColor;if(x==tx&&y==ty)break;int e=2*error;if(e>=dy){error+=dy;x+=sx;}if(e<=dx){error+=dx;y+=sy;}}w.paintLast=cell;programs.changed();}
    private void drawPaint(GuiGraphics g,DeskWindow w){
        int x=w.x,y=w.y,side=x+w.w-126;int size=Math.min((w.w-148)/32,(w.h-76)/32);text(g,"PIXEL STUDIO / 32 x 32",x+13,y+34,accent());for(int i=0;i<1024;i++)rect(g,x+13+i%32*size,y+53+i/32*size,size,size,PALETTE[programs.data().paint[i]]);g.renderOutline(x+12,y+52,size*32+2,size*32+2,EDGE);
        hit("Paint canvas",x+13,y+53,size*32,size*32,()->{w.paintUndo=programs.data().paint.clone();w.paintLast=-1;painting=w;paintAt(w,paintCell(w,mx,my));});
        text(g,"PALETTE",side,y+36,MUTED);for(int i=0;i<16;i++){int id=i,xx=side+i%4*27,yy=y+52+i/4*22;rect(g,xx,yy,23,18,PALETTE[i]);if(w.paintColor==i)g.renderOutline(xx-1,yy-1,25,20,accent());hit("Paint color "+i,xx,yy,23,18,()->w.paintColor=id);}
        button(g,"Paint undo","Undo",side,y+146,52,19,()->{if(w.paintUndo!=null){int[] old=programs.data().paint;programs.data().paint=w.paintUndo;w.paintUndo=old;programs.changed();}});
        button(g,"Paint erase","Eraser",side+58,y+146,52,19,()->w.paintColor=0);
        button(g,"Paint clear",w.clearPaint?"Confirm clear":"Clear canvas",side,y+171,110,21,()->{if(w.clearPaint){w.paintUndo=programs.data().paint.clone();java.util.Arrays.fill(programs.data().paint,0);programs.changed();w.clearPaint=false;}else w.clearPaint=true;});
        button(g,"Paint export",w.paintBusy?"Saving...":"Export to Gallery",side,y+198,110,21,()->exportPaint(w));text(g,"Drawings save automatically",x+13,y+w.h-12,MUTED);
    }
    private void exportPaint(DeskWindow w){if(w.paintBusy)return;w.paintBusy=true;var image=new com.mojang.blaze3d.platform.NativeImage(256,256,false);int[] canvas=programs.data().paint.clone();for(int y=0;y<256;y++)for(int x=0;x<256;x++){int c=PALETTE[canvas[y/8*32+x/8]];image.setPixelRGBA(x,y,0xFF000000|(c&255)<<16|(c&0xFF00)|(c>>16&255));}PhonePhotos.save(image).whenComplete((p,error)->minecraft.execute(()->{w.paintBusy=false;if(photos.active()){photos.refresh();notify(error==null?"Drawing exported to phone and laptop Gallery":"Could not export drawing. Try again.");}}));programs.save();}
    private EditBox programInput(DeskWindow w,int x,int y,int width,String hint){if(w.programInput==null||w.programInput.getWidth()!=width){w.programInput=new EditBox(font,x,y,width,20,Component.literal(hint));w.programInput.setMaxLength(w.app==17?100:40);w.programInput.setValue(w.programDraft);w.programInput.setResponder(v->w.programDraft=v);}w.programInput.setPosition(x,y);return w.programInput;}
    private void addTask(DeskWindow w){if(programs.addTask(w.programDraft)){w.programDraft="";if(w.programInput!=null)w.programInput.setValue("");notify("Task added");}else notify("Enter a task (up to 64 tasks)");}
    private void drawPlanner(GuiGraphics g,DeskWindow w){int x=w.x,y=w.y;var data=programs.data();text(g,"BUILD PLANNER",x+11,y+34,accent());text(g,data.tasks.stream().filter(t->t.done).count()+" / "+data.tasks.size()+" complete",x+w.w-118,y+34,MUTED);
        var input=programInput(w,x+11,y+52,w.w-89,"Add a task");input.render(g,mx,my,0);hit("Planner input",x+11,y+52,w.w-89,20,()->{raise(w);input.setFocused(true);input.mouseClicked(mx,my,0);});button(g,"Planner add","Add",x+w.w-69,y+52,58,20,()->addTask(w));
        int rows=Math.max(1,(w.h-111)/25),max=Math.max(0,data.tasks.size()-rows);w.programScroll=Math.min(w.programScroll,max);for(int i=0;i<rows&&i+w.programScroll<data.tasks.size();i++){int id=i+w.programScroll,yy=y+82+i*25;var task=data.tasks.get(id);rect(g,x+11,yy,w.w-22,22,0xFF263D49);button(g,"Planner toggle "+id,task.done?"x":"",x+14,yy+2,18,18,()->{task.done=!task.done;programs.changed();programs.save();});text(g,font.plainSubstrByWidth(task.text,w.w-102),x+41,yy+7,task.done?MUTED:INK);button(g,"Planner delete "+id,"x",x+w.w-35,yy+2,20,18,()->{data.tasks.remove(task);programs.changed();programs.save();});}
        if(data.tasks.isEmpty())center(g,"Plan your next build. Add your first task above.",x+w.w/2,y+116,MUTED);button(g,"Planner previous","<",x+11,y+w.h-25,24,18,()->w.programScroll=Math.max(0,w.programScroll-rows));button(g,"Planner next",">",x+40,y+w.h-25,24,18,()->w.programScroll=Math.min(max,w.programScroll+rows));text(g,"Tasks save automatically",x+77,y+w.h-19,MUTED);
    }
    private void drawCalendar(GuiGraphics g,DeskWindow w){int x=w.x,y=w.y;var month=java.time.YearMonth.now().plusMonths(w.calendarOffset);var today=java.time.LocalDate.now();center(g,month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),x+w.w/2,y+35,accent());button(g,"Calendar previous","<",x+11,y+30,26,21,()->w.calendarOffset=Math.max(-1200,w.calendarOffset-1));button(g,"Calendar next",">",x+w.w-37,y+30,26,21,()->w.calendarOffset=Math.min(1200,w.calendarOffset+1));int cw=(w.w-30)/7,ch=(w.h-102)/6;String[] days={"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};for(int i=0;i<7;i++)center(g,days[i],x+15+i*cw+cw/2,y+62,MUTED);int offset=month.atDay(1).getDayOfWeek().getValue()-1;for(int day=1;day<=month.lengthOfMonth();day++){int p=offset+day-1,xx=x+15+p%7*cw,yy=y+78+p/7*ch;boolean current=today.equals(month.atDay(day));rect(g,xx+1,yy,cw-3,ch-2,current?0xFF3E776B:0xFF253B49);center(g,""+day,xx+cw/2,yy+(ch-8)/2,current?INK:p%7>=5?0xFFCFB5D7:MUTED);}button(g,"Calendar today","Today",x+12,y+w.h-25,66,18,()->w.calendarOffset=0);text(g,today.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),x+88,y+w.h-19,MUTED);}
    private void drawConverter(GuiGraphics g,DeskWindow w){int x=w.x,y=w.y;int cat=w.converterCategory;String[] units=LaptopUnits.UNITS[cat];text(g,"UNIT CONVERTER",x+12,y+35,accent());button(g,"Converter category",LaptopUnits.CATEGORIES[cat]+" >",x+w.w-141,y+29,129,23,()->{w.converterCategory=(w.converterCategory+1)%5;w.converterFrom=0;w.converterTo=1;});text(g,"Amount",x+12,y+67,MUTED);var input=programInput(w,x+12,y+84,w.w-24,"Number to convert");input.render(g,mx,my,0);hit("Converter input",x+12,y+84,w.w-24,20,()->{raise(w);input.setFocused(true);input.mouseClicked(mx,my,0);});int bw=(w.w-64)/2;button(g,"Converter from",units[w.converterFrom]+" >",x+12,y+114,bw,24,()->w.converterFrom=(w.converterFrom+1)%units.length);button(g,"Converter swap","<>",x+18+bw,y+114,28,24,()->{int tmp=w.converterFrom;w.converterFrom=w.converterTo;w.converterTo=tmp;});button(g,"Converter to",units[w.converterTo]+" >",x+52+bw,y+114,bw,24,()->w.converterTo=(w.converterTo+1)%units.length);String result=LaptopUnits.convert(w.programDraft,cat,w.converterFrom,w.converterTo);rect(g,x+12,y+150,w.w-24,40,0xFF102431);center(g,font.plainSubstrByWidth(result,w.w-42),x+w.w/2,y+166,INK);button(g,"Converter copy","Copy result",x+12,y+201,106,22,()->{minecraft.keyboardHandler.setClipboard(result);notify("Conversion copied");});text(g,"Changes calculate instantly",x+128,y+209,MUTED);}
    private void drawFocus(GuiGraphics g,DeskWindow w){int x=w.x,y=w.y;long remain=programs.remaining();text(g,"FOCUS TIMER",x+13,y+36,accent());center(g,programs.data().timerRunning?(remain>0?"Keep building. You have this.":"Session complete!"):"A little space to concentrate.",x+w.w/2,y+65,MUTED);String clock=String.format(java.util.Locale.ROOT,"%02d:%02d",remain/60000,remain/1000%60);g.pose().pushPose();g.pose().translate(x+w.w/2,y+99,0);g.pose().scale(3,3,1);center(g,clock,0,0,INK);g.pose().popPose();button(g,"Focus start",programs.data().timerRunning?"Pause":"Start",x+w.w/2-109,y+151,103,25,programs::timerToggle);button(g,"Focus reset","Reset",x+w.w/2+6,y+151,103,25,()->programs.timerReset(25));int[] minutes={5,15,25,45};for(int i=0;i<4;i++){int m=minutes[i];button(g,"Focus preset "+m,m+" min",x+12+i*(w.w-20)/4,y+189,(w.w-28)/4,23,()->programs.timerReset(m));}center(g,"The timer keeps time when you leave the laptop.",x+w.w/2,y+w.h-12,MUTED);}
    private void drawSystem(GuiGraphics g,DeskWindow w){int x=w.x,y=w.y;var p=minecraft.player;var runtime=Runtime.getRuntime();String[] labels={"PLAYER","POSITION","DIRECTION","BIOME","WORLD TIME","FPS / MEMORY","INSTALLED APPS"};String biome=level.getBiome(p.blockPosition()).unwrapKey().map(k->k.location().toString()).orElse("Unknown");long time=Math.floorMod(level.getDayTime()+6000,24000);String[] values={p.getName().getString(),String.format(java.util.Locale.ROOT,"%.1f, %.1f, %.1f",p.getX(),p.getY(),p.getZ()),p.getDirection().getName().toUpperCase(java.util.Locale.ROOT),biome,String.format(java.util.Locale.ROOT,"%02d:%02d  /  Day %d",time/1000,time%1000*60/1000,level.getDayTime()/24000+1),Minecraft.getInstance().getFps()+" FPS  /  "+(runtime.totalMemory()-runtime.freeMemory())/1048576+" MiB",programs.data().installed.size()+" downloaded + 9 built-in"};for(int i=0;i<labels.length;i++){int yy=y+35+i*22;rect(g,x+10,yy,w.w-20,20,i%2==0?0xFF233946:0xFF1B303C);text(g,labels[i],x+17,yy+6,MUTED);text(g,font.plainSubstrByWidth(values[i],w.w-153),x+140,yy+6,INK);}button(g,"System copy coordinates","Copy coordinates",x+11,y+w.h-27,132,21,()->{minecraft.keyboardHandler.setClipboard(String.format(java.util.Locale.ROOT,"%d %d %d",p.getBlockX(),p.getBlockY(),p.getBlockZ()));notify("Coordinates copied");});text(g,"Live local information",x+154,y+w.h-20,MUTED);}

    private void calc(DeskWindow w,String command){raise(w);focus=-1;w.calculator.press(command);}
    private void drawCalculator(GuiGraphics g,DeskWindow w){
        int x=w.x,y=w.y;var c=w.calculator;
        rect(g,x+9,y+29,w.w-18,45,0xFF0C1F29);text(g,font.plainSubstrByWidth(c.history().isEmpty()?"STANDARD":c.history(),w.w-45),x+16,y+35,MUTED);
        if(c.hasMemory())text(g,"M",x+w.w-22,y+35,accent());
        String value=c.display();float size=Math.min(1.7f,(w.w-36f)/Math.max(1,font.width(value)));
        g.pose().pushPose();g.pose().translate(x+w.w-17,y+51,0);g.pose().scale(size,size,1);text(g,value,-font.width(value),0,c.isError()?0xFFF4A095:INK);g.pose().popPose();
        String[] memory={"MC","MR","M+","M-"};int memW=(w.w-76)/4;
        for(int i=0;i<memory.length;i++){String command=memory[i];button(g,"Calculator "+command,command,x+10+i*(memW+3),y+80,memW,17,()->calc(w,command));}
        button(g,"Copy result","Copy",x+w.w-56,y+80,46,17,()->{minecraft.keyboardHandler.setClipboard(c.display());notify("Result copied");});
        String[][] keys={{"C","SIGN","%","/"},{"7","8","9","*"},{"4","5","6","-"},{"1","2","3","+"},{"0",".","BACK","="}};
        int kw=(w.w-29)/4,stride=(w.h-117)/5;
        for(int row=0;row<5;row++)for(int col=0;col<4;col++){String command=keys[row][col];String label=switch(command){case "SIGN"->"+/-";case "BACK"->"DEL";case "*"->"x";default->command;};
            int bx=x+10+col*(kw+3),by=y+102+row*stride;button(g,"Calculator "+command,label,bx,by,kw,stride-2,()->calc(w,command));if(command.equals("="))g.renderOutline(bx,by,kw,stride-2,accent());
        }
        center(g,"Type numbers  |  Enter =  |  Delete clears",x+w.w/2,y+w.h-11,MUTED);
    }
    private int noteRows(DeskWindow w){return Math.max(1,(w.h-109)/24);}
    private void selectNote(DeskWindow w,String id){w.noteId=id;w.noteTitle=null;w.noteBody=null;w.confirmDelete=false;raise(w);}
    private void newNote(DeskWindow w){var n=notes.create();if(n==null){notify("Notebook is full (64 notes)");return;}w.notesScroll=0;selectNote(w,n.id);}
    private void notesPage(DeskWindow w,int delta){w.notesScroll=Math.max(0,Math.min(Math.max(0,notes.all().size()-noteRows(w)),w.notesScroll+delta));}
    private void drawNotes(GuiGraphics g,DeskWindow w){
        var book=notes;var all=book.all();int x=w.x,y=w.y,split=114,right=x+split+9,rw=w.w-split-19;
        if(book.find(w.noteId)==null&&!all.isEmpty()){w.noteId=all.getFirst().id;w.noteTitle=null;w.noteBody=null;w.confirmDelete=false;}
        var note=book.find(w.noteId);
        rect(g,x+1,y+25,split,w.h-26,0xFF182B31);text(g,"NOTEBOOK",x+11,y+35,0xFFE9D994);
        button(g,"New note","+ New note",x+9,y+49,97,20,()->newNote(w));
        w.notesScroll=Math.min(w.notesScroll,Math.max(0,all.size()-noteRows(w)));
        for(int i=0;i<noteRows(w)&&w.notesScroll+i<all.size();i++){var n=all.get(w.notesScroll+i);int yy=y+77+i*24;
            if(n.id.equals(w.noteId)||hover(x+6,yy,102,23))rect(g,x+6,yy,102,23,n.id.equals(w.noteId)?0xFF48534A:0xFF2B3D40);
            text(g,font.plainSubstrByWidth(n.title(),91),x+11,yy+3,INK);String preview=n.text().replace('\n',' ').strip();text(g,font.plainSubstrByWidth(preview.isEmpty()?"Empty note":preview,91),x+11,yy+13,MUTED);
            hit("Select note "+n.id,x+6,yy,102,23,()->selectNote(w,n.id));
        }
        button(g,"Notes scroll up","^",x+9,y+w.h-24,22,17,()->notesPage(w,-noteRows(w)));button(g,"Notes scroll down","v",x+35,y+w.h-24,22,17,()->notesPage(w,noteRows(w)));
        text(g,all.size()+" / 64",x+66,y+w.h-19,MUTED);
        if(note==null){center(g,"Your ideas live here",right+rw/2,y+86,INK);center(g,"Create a note to get started.",right+rw/2,y+106,MUTED);button(g,"Create first note","+ New note",right+rw/2-50,y+130,100,23,()->newNote(w));return;}
        button(g,"Save note","Save",right,y+32,48,20,()->notify(book.save()?"Notebook saved":"Save failed - try again"));
        button(g,"Delete note","Delete",right+54,y+32,53,20,()->{w.confirmDelete=true;w.noteTitle.setFocused(false);w.noteBody.setFocused(false);});
        text(g,"LOCAL NOTES",right+rw-66,y+39,MUTED);
        int bodyHeight=w.h-113;
        if(w.noteBody==null||!w.editorId.equals(note.id)||w.noteBody.getWidth()!=rw||w.noteBody.getHeight()!=bodyHeight){
            boolean titleFocused=w.noteTitle!=null&&w.noteTitle.isFocused();boolean bodyFocused=w.noteBody==null||w.noteBody.isFocused();
            w.editorId=note.id;
            w.noteTitle=new EditBox(font,right,y+61,rw,20,Component.literal("Note title"));w.noteTitle.setMaxLength(48);w.noteTitle.setValue(note.rawTitle());
            w.noteBody=new NotesEditBox(right,y+86,rw,bodyHeight);w.noteBody.setCharacterLimit(LaptopNotes.MAX_TEXT);w.noteBody.setValue(note.text());
            String id=note.id;w.noteTitle.setResponder(value->book.update(id,value,w.noteBody.getValue()));w.noteBody.setValueListener(value->book.update(id,w.noteTitle.getValue(),value));
            w.noteTitle.setFocused(titleFocused&&activeWindow()==w);w.noteBody.setFocused(bodyFocused&&activeWindow()==w);
        }
        w.noteTitle.setX(right);w.noteTitle.setY(y+61);w.noteBody.setX(right);w.noteBody.setY(y+86);
        w.noteTitle.render(g,mx,my,0);w.noteBody.render(g,mx,my,0);
        hit("Edit note title",right,y+61,rw,20,()->{raise(w);w.noteBody.setFocused(false);w.noteTitle.setFocused(true);w.noteTitle.mouseClicked(mx,my,0);});
        hit("Edit note text",right,y+86,rw+8,bodyHeight,()->{raise(w);w.noteTitle.setFocused(false);w.noteBody.setFocused(true);w.noteBody.mouseClicked(mx,my,0);});
        text(g,font.plainSubstrByWidth(book.status(),rw-65),right,y+w.h-14,MUTED);String count=note.text().length()+"/8000";text(g,count,right+rw-font.width(count),y+w.h-14,MUTED);
        if(w.confirmDelete){
            rect(g,x+1,y+25,w.w-2,w.h-26,0xD0111E28);hit("Delete confirmation",x+1,y+25,w.w-2,w.h-26,()->{});
            int cx=x+w.w/2;center(g,"Delete this note?",cx,y+84,INK);center(g,"This cannot be undone.",cx,y+102,MUTED);
            button(g,"Cancel delete","Keep note",cx-92,y+125,86,23,()->w.confirmDelete=false);
            button(g,"Confirm delete","Delete",cx+6,y+125,86,23,()->{book.delete(w.noteId);book.save();w.noteId="";w.noteTitle=null;w.noteBody=null;w.confirmDelete=false;});
        }
    }
    private int musicRows(DeskWindow w){return Math.max(1,(w.h-88)/18);}
    private void musicPage(DeskWindow w,int delta){w.musicScroll=Math.max(0,Math.min(Math.max(0,PhoneMusic.tracks().size()-musicRows(w)),w.musicScroll+delta));}
    private void playMusic(DeskWindow w,int index){var tracks=PhoneMusic.tracks();if(tracks.isEmpty())return;w.musicSelection=Math.floorMod(index,tracks.size());LaptopMusic.play(position,tracks.get(w.musicSelection));}
    private void drawMusic(GuiGraphics g,DeskWindow w){
        var tracks=PhoneMusic.tracks();int split=Math.min(162,w.w*43/100),x=w.x,y=w.y,rows=musicRows(w);
        w.musicScroll=Math.min(w.musicScroll,Math.max(0,tracks.size()-rows));w.musicSelection=Math.min(w.musicSelection,Math.max(0,tracks.size()-1));
        rect(g,x+1,y+25,split,w.h-26,0xFF12232D);text(g,"DISC LIBRARY",x+10,y+34,accent());text(g,tracks.size()+" tracks",x+split-61,y+34,MUTED);
        var current=LaptopMusic.selected(position);boolean playing=LaptopMusic.playing(position);
        for(int i=0;i<rows&&w.musicScroll+i<tracks.size();i++){
            int index=w.musicScroll+i,yy=y+49+i*18;var t=tracks.get(index);boolean chosen=current!=null?current.id().equals(t.id()):w.musicSelection==index;
            if(chosen||hover(x+5,yy,split-9,17))rect(g,x+5,yy,split-9,17,chosen?0xFF345263:0xFF233A47);
            rect(g,x+10,yy+5,7,7,0xFF0A1118);rect(g,x+12,yy+7,3,3,chosen?accent():0xFFB296C8);
            text(g,font.plainSubstrByWidth(t.title(),split-56),x+23,yy+5,chosen?INK:MUTED);
            String duration=LaptopMusic.time((int)t.song().lengthInSeconds());text(g,duration,x+split-font.width(duration)-9,yy+5,MUTED);
            hit("Play disc "+t.id().getPath(),x+5,yy,split-9,17,()->{raise(w);playMusic(w,index);});
        }
        int foot=y+w.h-25;
        button(g,"Music scroll up","^",x+9,foot,22,17,()->musicPage(w,-rows));button(g,"Music scroll down","v",x+35,foot,22,17,()->musicPage(w,rows));
        text(g,tracks.isEmpty()?"No discs":(w.musicScroll+1)+"-"+Math.min(tracks.size(),w.musicScroll+rows)+" / "+tracks.size(),x+65,foot+5,MUTED);
        if(tracks.size()>rows){int trackH=rows*18;rect(g,x+split-3,y+49,2,trackH,0xFF29424F);int thumb=Math.max(8,trackH*rows/tracks.size());rect(g,x+split-3,y+49+(trackH-thumb)*w.musicScroll/(tracks.size()-rows),2,thumb,accent());}
        int panelX=x+split+1,panelW=w.w-split-2,cx=panelX+panelW/2;
        text(g,playing?"NOW PLAYING":"MUSIC PLAYER",panelX+12,y+35,accent());
        int coverY=y+52;rect(g,cx-31,coverY,62,54,0xFF263B4B);rect(g,cx-29,coverY+2,58,50,0xFF443D60);
        rect(g,cx-18,coverY+7,36,40,0xFF0E1924);rect(g,cx-22,coverY+11,44,32,0xFF0E1924);rect(g,cx-14,coverY+13,28,28,0xFF26323F);rect(g,cx-17,coverY+16,34,22,0xFF26323F);
        rect(g,cx-7,coverY+20,14,14,0xFFB796D8);rect(g,cx-2,coverY+25,4,4,0xFF0D1823);
        var shown=current!=null?current:tracks.isEmpty()?null:tracks.get(w.musicSelection);
        center(g,font.plainSubstrByWidth(shown==null?"Choose a disc":shown.title(),panelW-18),cx,y+115,INK);
        String artist="Minecraft records";if(shown!=null){String desc=shown.song().description().getString();int dash=desc.indexOf(" - ");if(dash>=0)artist=desc.substring(0,dash);}
        center(g,font.plainSubstrByWidth(artist,panelW-18),cx,y+129,MUTED);
        int controls=y+w.h-39,progressY=controls-14;
        text(g,LaptopMusic.elapsed(position),panelX+12,progressY-12,MUTED);String duration=LaptopMusic.time(shown==null?0:(int)shown.song().lengthInSeconds());text(g,duration,x+w.w-12-font.width(duration),progressY-12,MUTED);
        rect(g,panelX+12,progressY,panelW-24,3,0xFF334957);rect(g,panelX+12,progressY,(int)((panelW-24)*LaptopMusic.progress(position)),3,accent());
        int bx=cx-76;
        button(g,"Music previous","<",bx,controls,25,21,()->playMusic(w,currentIndex(w)-1));
        button(g,"Music play",playing?"Replay":"Play",bx+29,controls,48,21,()->playMusic(w,currentIndex(w)));
        button(g,"Music stop","Stop",bx+81,controls,42,21,()->LaptopMusic.stop(position));
        button(g,"Music next",">",bx+127,controls,25,21,()->playMusic(w,currentIndex(w)+1));
        center(g,playing?"Playing on this computer":"Click a disc to play",cx,y+w.h-12,MUTED);
    }
    private void savePreferences(){if(!prefs.save())notify("Settings could not save - try again");}
    private void chooseWallpaper(int id){prefs.wallpaper=id;prefs.wallpaperPhoto="";savePreferences();notify("Wallpaper: "+LaptopPreferences.WALLPAPERS[id]);}
    private void photoWallpaper(String id){prefs.wallpaperPhoto=id;savePreferences();notify("Photo set as desktop wallpaper");}
    private void toggle(GuiGraphics g,String name,String label,int x,int y,int w,boolean value,Runnable change){button(g,name,label+": "+(value?"On":"Off"),x,y,w,20,()->{change.run();savePreferences();});}
    private String clockPattern(){return prefs.clock24?(prefs.clockSeconds?"HH:mm:ss":"HH:mm"):(prefs.clockSeconds?"h:mm:ss a":"h:mm a");}
    private void drawSettings(GuiGraphics g,DeskWindow w){
        int x=w.x,y=w.y,right=x+116,rw=w.w-126;
        rect(g,x+1,y+25,105,w.h-26,0xFF12232D);
        String[] tabs=programs.installed(24)?new String[]{"Appearance","Display","Sound","Library","About","Chromium"}:new String[]{"Appearance","Display","Sound","Library","About"};
        w.settingsTab=Math.max(0,Math.min(tabs.length-1,w.settingsTab));
        for(int i=0;i<tabs.length;i++){int tab=i;button(g,"Settings "+tabs[i],tabs[i],x+8,y+35+i*27,91,22,()->w.settingsTab=tab);if(w.settingsTab==i)g.renderOutline(x+8,y+35+i*27,91,22,accent());}
        text(g,tabs[w.settingsTab].toUpperCase(java.util.Locale.ROOT),right,y+35,accent());
        switch(w.settingsTab){
            case 0->{
                text(g,"Desktop wallpaper",right,y+51,MUTED);
                int bw=(rw-8)/3;
                for(int i=0;i<6;i++){int id=i,bx=right+(i%3)*(bw+4),by=y+66+(i/3)*25;button(g,"Set wallpaper "+LaptopPreferences.WALLPAPERS[i],LaptopPreferences.WALLPAPERS[i],bx,by,bw,21,()->chooseWallpaper(id));if(prefs.wallpaperPhoto.isEmpty()&&prefs.wallpaper==i)g.renderOutline(bx,by,bw,21,accent());}
                button(g,"Choose photo wallpaper","Use a phone photo",right,y+121,rw,21,()->{launch(5);notify("Select a photo, then choose Wallpaper");});
                text(g,"Accent color",right,y+151,MUTED);int cw=Math.max(15,(rw-15)/6);
                for(int i=0;i<6;i++){int id=i,bx=right+i*(cw+3);rect(g,bx,y+166,cw,18,LaptopPreferences.ACCENT_COLORS[i]);if(prefs.accent==i)g.renderOutline(bx,y+166,cw,18,INK);hit("Accent "+LaptopPreferences.ACCENTS[i],bx,y+166,cw,18,()->{prefs.accent=id;savePreferences();});}
                button(g,"Color theme","Theme: "+new String[]{"Slate","Midnight","Forest"}[prefs.theme],right,y+193,rw,20,()->{prefs.theme=(prefs.theme+1)%3;savePreferences();});
            }
            case 1->{
                int yy=y+55;
                toggle(g,"Toggle icon labels","Icon labels",right,yy,rw,prefs.iconLabels,()->prefs.iconLabels=!prefs.iconLabels);yy+=22;
                toggle(g,"Toggle 24 hour","24-hour clock",right,yy,rw,prefs.clock24,()->prefs.clock24=!prefs.clock24);yy+=22;
                toggle(g,"Toggle clock seconds","Clock seconds",right,yy,rw,prefs.clockSeconds,()->prefs.clockSeconds=!prefs.clockSeconds);yy+=22;
                toggle(g,"Toggle desktop date","Show date",right,yy,rw,prefs.showDate,()->prefs.showDate=!prefs.showDate);yy+=22;
                toggle(g,"Toggle fresh lock","Lock on fresh open",right,yy,rw,prefs.lockOnOpen,()->prefs.lockOnOpen=!prefs.lockOnOpen);yy+=22;
                toggle(g,"Toggle remember windows","Remember open windows",right,yy,rw,prefs.rememberWindows,()->prefs.rememberWindows=!prefs.rememberWindows);
            }
            case 2->{
                text(g,"Music volume",right,y+51,INK);
                button(g,"Volume down","-",right,y+64,30,19,()->{prefs.musicVolume=Math.max(0,prefs.musicVolume-10);savePreferences();});
                center(g,prefs.musicVolume+"%",right+rw/2,y+70,INK);
                button(g,"Volume up","+",right+rw-30,y+64,30,19,()->{prefs.musicVolume=Math.min(100,prefs.musicVolume+10);savePreferences();});
                text(g,"Game effects",right,y+94,INK);
                button(g,"Game volume down","-",right,y+107,30,19,()->{prefs.gameVolume=Math.max(0,prefs.gameVolume-10);if(prefs.gameVolume==0)LaptopGameSounds.stop();savePreferences();});
                center(g,prefs.gameVolume==0?"Muted":prefs.gameVolume+"%",right+rw/2,y+113,INK);
                button(g,"Game volume up","+",right+rw-30,y+107,30,19,()->{prefs.gameVolume=Math.min(100,prefs.gameVolume+10);savePreferences();});
                toggle(g,"Toggle music auto next","Play next disc",right,y+136,rw,prefs.autoNext,()->prefs.autoNext=!prefs.autoNext);
                toggle(g,"Toggle stop on exit","Stop when leaving desktop",right,y+161,rw,prefs.stopMusicOnExit,()->prefs.stopMusicOnExit=!prefs.stopMusicOnExit);
                button(g,"Test game sound","Test game sound",right,y+187,rw,20,()->LaptopGameSounds.play(LaptopGames.Cue.EAT));
            }
            case 3->{
                toggle(g,"Toggle gallery sort","Oldest photos first",right,y+55,rw,prefs.galleryOldest,()->prefs.galleryOldest=!prefs.galleryOldest);
                toggle(g,"Toggle photo confirmation","Confirm photo deletion",right,y+81,rw,prefs.confirmDelete,()->prefs.confirmDelete=!prefs.confirmDelete);
                toggle(g,"Toggle wallpaper fill","Fill photo wallpaper",right,y+107,rw,prefs.photoFill,()->prefs.photoFill=!prefs.photoFill);
                toggle(g,"Toggle dim photo","Dim photo wallpaper",right,y+133,rw,prefs.dimPhoto,()->prefs.dimPhoto=!prefs.dimPhoto);
                button(g,"Refresh phone library","Refresh phone library",right,y+165,rw,21,()->{photos.refresh();notify("Refreshing shared phone photos");});
                text(g,"Shared with every iPhone and Samsung.",right,y+196,MUTED);
            }
            case 5->{
                text(g,"Browser frame rate",right,y+55,INK);
                button(g,"Chromium 25 FPS","25 FPS / Performance",right,y+73,rw,25,()->{prefs.chromiumFps=25;savePreferences();});
                if(prefs.chromiumFps==25)g.renderOutline(right,y+73,rw,25,accent());
                text(g,"Lower load on your computer",right,y+104,MUTED);
                button(g,"Chromium 60 FPS","60 FPS / Smooth",right,y+127,rw,25,()->{prefs.chromiumFps=60;savePreferences();});
                if(prefs.chromiumFps==60)g.renderOutline(right,y+127,rw,25,accent());
                text(g,"Smoother pages and videos; uses more power",right,y+158,MUTED);
                text(g,"Applies when you open Chromium.",right,y+185,MUTED);
                text(g,"Works in Standard and Media mode.",right,y+202,MUTED);
            }
            default->{
                text(g,"BlockOS / Minecraft 1.21.1",right,y+57,INK);
                text(g,"Welcome, "+font.plainSubstrByWidth(minecraft.player.getName().getString(),rw-65),right,y+76,MUTED);
                text(g,"Notebook: "+notes.all().size()+" notes",right,y+95,MUTED);
                text(g,"Photo library: "+photos.photos().size()+" pictures",right,y+113,MUTED);
                text(g,"Music: "+PhoneMusic.tracks().size()+" Minecraft discs",right,y+131,MUTED);
                text(g,"This computer: "+position.toShortString(),right,y+150,MUTED);
                text(g,"Settings save for this computer and player.",right,y+174,MUTED);
            }
        }
        text(g,"Changes save automatically",right,y+w.h-11,MUTED);
    }

    private void selectPhoto(DeskWindow w,String id){w.photoId=id;w.confirmDelete=false;raise(w);}
    private void nextPhoto(DeskWindow w,int step){var all=photos.photos();for(int i=0;i<all.size();i++)if(all.get(i).id().equals(w.photoId)){selectPhoto(w,all.get(Math.floorMod(i+step,all.size())).id());return;}}
    private void copyPhoto(DeskWindow w){var p=photos.find(w.photoId);if(p==null||w.photoBusy)return;w.photoBusy=true;notify("Copying photo...");PhonePhotos.copy(p).whenComplete((v,error)->minecraft.execute(()->{w.photoBusy=false;if(photos.active())notify(error==null?"Image copied - paste with Ctrl+V":"Could not copy photo. Try again.");}));}
    private void deletePhoto(DeskWindow w){var p=photos.find(w.photoId);if(p==null||w.photoBusy)return;w.photoBusy=true;w.confirmDelete=false;PhonePhotos.delete(p).whenComplete((v,error)->minecraft.execute(()->{w.photoBusy=false;if(!photos.active())return;if(error==null){w.photoId="";photos.refresh();notify("Photo removed from the shared Gallery");}else notify("Could not delete photo. Try again.");}));}
    private void drawGallery(GuiGraphics g,DeskWindow w){
        int x=w.x,y=w.y;var all=photos.photos();var selected=photos.find(w.photoId);
        if(selected==null)w.photoId="";
        if(w.confirmDelete&&selected!=null){
            center(g,"Delete this shared photo?",x+w.w/2,y+80,INK);center(g,"It is removed from phone and laptop Gallery.",x+w.w/2,y+99,MUTED);
            center(g,"A recovery copy is kept in the photo folder.",x+w.w/2,y+116,MUTED);
            button(g,"Cancel photo deletion","Cancel",x+w.w/2-95,y+142,88,23,()->w.confirmDelete=false);
            button(g,"Confirm photo deletion","Delete",x+w.w/2+7,y+142,88,23,()->deletePhoto(w));return;
        }
        button(g,"Gallery grid","Grid",x+10,y+31,40,20,()->w.photoId="");
        button(g,"Refresh Gallery","Refresh",x+56,y+31,62,20,photos::refresh);
        text(g,font.plainSubstrByWidth(photos.status(),w.w-140),x+128,y+38,MUTED);
        if(selected!=null){
            photos.draw(g,selected,x+10,y+59,w.w-20,w.h-111,true,false);
            button(g,"Gallery previous","<",x+10,y+w.h-45,24,21,()->nextPhoto(w,-1));
            button(g,"Copy Gallery image",w.photoBusy?"Working...":"Copy image",x+40,y+w.h-45,87,21,()->copyPhoto(w));
            button(g,"Use Gallery wallpaper","Wallpaper",x+133,y+w.h-45,84,21,()->photoWallpaper(w.photoId));
            button(g,"Delete Gallery image","Delete",x+223,y+w.h-45,62,21,()->{if(!w.photoBusy){if(prefs.confirmDelete)w.confirmDelete=true;else deletePhoto(w);}});
            button(g,"Gallery next",">",x+w.w-34,y+w.h-45,24,21,()->nextPhoto(w,1));
            center(g,selected.date()+" / "+selected.width()+" x "+selected.height(),x+w.w/2,y+w.h-12,MUTED);return;
        }
        int columns=3,cell=(w.w-24)/columns,rows=Math.max(1,(w.h-89)/68),perPage=columns*rows,pages=Math.max(1,(all.size()+perPage-1)/perPage);
        w.photoPage=Math.max(0,Math.min(w.photoPage,pages-1));
        if(all.isEmpty()){icon(g,5,x+w.w/2-16,y+87,32);center(g,photos.loaded()?"Your Minecraft moments":"Loading phone photos...",x+w.w/2,y+130,INK);center(g,"Take a photo with any phone to see it here.",x+w.w/2,y+147,MUTED);}
        for(int i=0;i<perPage&&w.photoPage*perPage+i<all.size();i++){
            var p=all.get(w.photoPage*perPage+i);int px=x+10+(i%columns)*cell,py=y+60+(i/columns)*68;
            photos.draw(g,p,px,py,cell-7,46,false,false);rect(g,px,py+46,cell-7,15,0xFF243945);
            text(g,font.plainSubstrByWidth(p.date(),cell-13),px+3,py+50,MUTED);hit("Open Gallery photo "+p.id(),px,py,cell-7,61,()->selectPhoto(w,p.id()));
            if(hover(px,py,cell-7,61))g.renderOutline(px,py,cell-7,61,accent());
        }
        button(g,"Gallery page previous","<",x+10,y+w.h-25,28,18,()->w.photoPage=Math.floorMod(w.photoPage-1,pages));
        button(g,"Gallery page next",">",x+w.w-38,y+w.h-25,28,18,()->w.photoPage=(w.photoPage+1)%pages);
        center(g,"Page "+(w.photoPage+1)+" / "+pages+"  |  Same library as your phones",x+w.w/2,y+w.h-18,MUTED);
    }

    private record FileEntry(String id,String title,String detail,int app){}
    private List<FileEntry> files(DeskWindow w){
        var result=new ArrayList<FileEntry>();
        if(w.filesTab==0){result.add(new FileEntry("notes","Notes",notes.all().size()+" saved notes",3));result.add(new FileEntry("photos","Phone photos",photos.photos().size()+" shared pictures",5));result.add(new FileEntry("music","Music discs",PhoneMusic.tracks().size()+" tracks",4));result.add(new FileEntry("programs","Programs",programs.data().installed.size()+" installed programs",8));}
        if(w.filesTab==1)for(var n:notes.all())result.add(new FileEntry(n.id,n.title(),font.plainSubstrByWidth(n.text().replace('\n',' ').strip(),170),3));
        if(w.filesTab==2)for(var p:photos.photos())result.add(new FileEntry(p.id(),p.date(),p.width()+" x "+p.height(),5));
        if(w.filesTab==3)for(var t:PhoneMusic.tracks())result.add(new FileEntry(t.id().toString(),t.title(),LaptopMusic.time((int)t.song().lengthInSeconds()),4));
        if(w.filesTab==4)for(var app:LaptopCatalog.ALL)if(programs.installed(app.id()))result.add(new FileEntry(app.key(),app.name(),app.category()+" / BlockNet",app.id()));
        String find=w.filesQuery.toLowerCase(java.util.Locale.ROOT);return result.stream().filter(f->(f.title()+" "+f.detail()).toLowerCase(java.util.Locale.ROOT).contains(find)).toList();
    }
    private void openFile(DeskWindow w,FileEntry f){
        if(w.filesTab==0){w.filesTab=f.app()==3?1:f.app()==5?2:f.app()==4?3:4;w.filesScroll=0;w.filesQuery="";w.filesSearch=null;return;}
        launch(f.app());var target=activeWindow();
        if(f.app()==3)selectNote(target,f.id());
        if(f.app()==5)selectPhoto(target,f.id());
        if(f.app()==4){var tracks=PhoneMusic.tracks();for(int i=0;i<tracks.size();i++)if(tracks.get(i).id().toString().equals(f.id()))playMusic(target,i);}
    }
    private void drawFiles(GuiGraphics g,DeskWindow w){
        int x=w.x,y=w.y,right=x+103,rw=w.w-113;rect(g,x+1,y+25,93,w.h-26,0xFF12232D);
        String[] folders={"Quick access","Notes","Pictures","Music","Programs"};
        for(int i=0;i<5;i++){int tab=i;button(g,"Files "+folders[i],folders[i],x+6,y+36+i*27,82,22,()->{w.filesTab=tab;w.filesScroll=0;w.filesQuery="";w.filesSearch=null;});if(w.filesTab==i)g.renderOutline(x+6,y+36+i*27,82,22,accent());}
        button(g,"Files refresh","Refresh",x+6,y+w.h-32,82,20,photos::refresh);
        if(w.filesSearch==null||w.filesSearch.getWidth()!=rw){w.filesSearch=new EditBox(font,right,y+33,rw,18,Component.literal("Search files"));w.filesSearch.setMaxLength(80);w.filesSearch.setValue(w.filesQuery);w.filesSearch.setResponder(v->{w.filesQuery=v;w.filesScroll=0;});}
        w.filesSearch.setPosition(right,y+33);w.filesSearch.render(g,mx,my,0);hit("Search files",right,y+33,rw,18,()->{raise(w);w.filesSearch.setFocused(true);w.filesSearch.mouseClicked(mx,my,0);});
        text(g,folders[w.filesTab].toUpperCase(java.util.Locale.ROOT),right,y+60,accent());var list=files(w);int rows=Math.max(1,(w.h-105)/29);
        w.filesScroll=Math.min(w.filesScroll,Math.max(0,list.size()-rows));
        for(int i=0;i<rows&&w.filesScroll+i<list.size();i++){var f=list.get(w.filesScroll+i);int yy=y+77+i*29;rect(g,right,yy,rw,27,hover(right,yy,rw,27)?0xFF365260:0xFF223744);icon(g,f.app(),right+4,yy+5,17);text(g,font.plainSubstrByWidth(f.title(),rw-33),right+28,yy+3,INK);text(g,font.plainSubstrByWidth(f.detail(),rw-33),right+28,yy+15,MUTED);hit("Open file "+f.id(),right,yy,rw,27,()->openFile(w,f));}
        if(list.isEmpty())text(g,w.filesQuery.isEmpty()?"This folder is empty.":"No matching files.",right,y+91,MUTED);
        button(g,"Files page up","^",right,y+w.h-25,24,18,()->w.filesScroll=Math.max(0,w.filesScroll-rows));
        button(g,"Files page down","v",right+29,y+w.h-25,24,18,()->w.filesScroll=Math.min(Math.max(0,list.size()-rows),w.filesScroll+rows));
        text(g,list.size()+" item"+(list.size()==1?"":"s"),right+62,y+w.h-19,MUTED);
    }

    private void terminalLine(DeskWindow w,String line){w.output.add(line);while(w.output.size()>160)w.output.remove(0);w.terminalScroll=0;}
    private void runTerminal(DeskWindow w){
        String input=w.terminalDraft.strip();if(input.isEmpty())return;
        terminalLine(w,"> "+input);w.commands.add(input);if(w.commands.size()>60)w.commands.remove(0);w.commandHistory=w.commands.size();w.terminalDraft="";if(w.commandInput!=null)w.commandInput.setValue("");
        String[] parts=input.split("\\s+",2);String cmd=parts[0].toLowerCase(java.util.Locale.ROOT),arg=parts.length>1?parts[1]:"";
        switch(cmd){
            case "help"->{terminalLine(w,"help / clear / echo <text> / time / date / whoami");terminalLine(w,"sysinfo / coords / ls [notes|photos|music] / pwd");terminalLine(w,"open <app> / calc <expression> / theme <preset>");terminalLine(w,"apps / install <program> / browser <address>");}
            case "clear","cls"->w.output.clear();
            case "echo"->terminalLine(w,arg);
            case "time"->terminalLine(w,LocalDateTime.now().format(DateTimeFormatter.ofPattern(clockPattern())));
            case "date"->terminalLine(w,LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
            case "whoami"->terminalLine(w,minecraft.player.getName().getString());
            case "pwd"->terminalLine(w,"BlockOS:/");
            case "coords"->terminalLine(w,String.format(java.util.Locale.ROOT,"Player %.1f, %.1f, %.1f",minecraft.player.getX(),minecraft.player.getY(),minecraft.player.getZ()));
            case "sysinfo"->{terminalLine(w,"BlockOS / Minecraft 1.21.1");terminalLine(w,"World: "+level.dimension().location());terminalLine(w,"Laptop: "+position.toShortString());terminalLine(w,"Memory: "+((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1048576)+" MiB used");}
            case "ls","dir"->{
                switch(arg.toLowerCase(java.util.Locale.ROOT)){
                    case "notes"->{for(var n:notes.all())terminalLine(w,n.title());}
                    case "photos","pictures"->{photos.refresh();terminalLine(w,photos.photos().size()+" photos in the shared phone library");for(var p:photos.photos().stream().limit(15).toList())terminalLine(w,p.date()+"  "+p.width()+"x"+p.height());}
                    case "music"->{for(var t:PhoneMusic.tracks())terminalLine(w,t.title());}
                    default->terminalLine(w,"Notes/  Pictures/  Music/  Programs/");
                }
            }
            case "apps"->{for(var app:LaptopCatalog.ALL)terminalLine(w,app.name()+" - "+(programs.installed(app.id())?"installed":programs.data().downloads.containsKey(app.id())?"installing":"available"));}
            case "install"->{var app=LaptopCatalog.find(arg);if(app==null)terminalLine(w,"Unknown program. Type apps.");else{installApp(app.id());terminalLine(w,"Program: "+app.name());}}
            case "browser"->{openStore(arg);terminalLine(w,"Opened BlockNet");}
            case "open"->{boolean found=false;for(int i=0;i<APPS.length;i++)if(APPS[i].equalsIgnoreCase(arg)){launch(i);found=true;break;}terminalLine(w,found?"Opened "+arg:"App not found. Use Files, Gallery, Notes or Music.");}
            case "theme"->{boolean found=false;for(int i=0;i<6;i++)if(LaptopPreferences.WALLPAPERS[i].equalsIgnoreCase(arg)){chooseWallpaper(i);found=true;break;}terminalLine(w,found?"Wallpaper changed":"Use Ocean, Dusk, Forest, Ember, Glacier or Midnight.");}
            case "calc"->{try{terminalLine(w,LaptopExpression.evaluate(arg));}catch(IllegalArgumentException error){terminalLine(w,"Math error: "+error.getMessage());}}
            default->terminalLine(w,"Unknown command. Type help.");
        }
    }
    private void drawTerminal(GuiGraphics g,DeskWindow w){
        int x=w.x,y=w.y;rect(g,x+1,y+25,w.w-2,w.h-26,0xFF0B1721);
        button(g,"Terminal help","Help",x+9,y+31,45,18,()->{w.terminalDraft="help";runTerminal(w);});
        button(g,"Terminal clear","Clear",x+59,y+31,46,18,()->w.output.clear());
        button(g,"Copy terminal output","Copy",x+110,y+31,46,18,()->{minecraft.keyboardHandler.setClipboard(String.join("\n",w.output));notify("Terminal output copied");});
        var wrapped=new ArrayList<String>();for(String line:w.output){String rest=line;do{String part=font.plainSubstrByWidth(rest,w.w-24);if(part.isEmpty()&&!rest.isEmpty())part=rest.substring(0,1);wrapped.add(part);rest=rest.substring(part.length());}while(!rest.isEmpty());}
        int rows=Math.max(1,(w.h-105)/11);w.terminalScroll=Math.min(w.terminalScroll,Math.max(0,wrapped.size()-rows));int from=Math.max(0,wrapped.size()-rows-w.terminalScroll);
        for(int i=0;i<rows&&from+i<wrapped.size();i++)text(g,wrapped.get(from+i),x+12,y+60+i*11,wrapped.get(from+i).startsWith(">")?accent():0xFFD3E2E5);
        int inputW=w.w-73;
        if(w.commandInput==null||w.commandInput.getWidth()!=inputW){w.commandInput=new EditBox(font,x+11,y+w.h-35,inputW,20,Component.literal("Terminal command"));w.commandInput.setMaxLength(256);w.commandInput.setValue(w.terminalDraft);w.commandInput.setResponder(v->w.terminalDraft=v);w.commandInput.setFocused(true);}
        w.commandInput.setPosition(x+11,y+w.h-35);w.commandInput.render(g,mx,my,0);hit("Terminal input",x+11,y+w.h-35,inputW,20,()->{raise(w);w.commandInput.setFocused(true);w.commandInput.mouseClicked(mx,my,0);});
        button(g,"Run terminal command","Run",x+w.w-56,y+w.h-35,45,20,()->runTerminal(w));
        text(g,"Enter runs / Up and Down recall commands",x+12,y+w.h-11,MUTED);
    }

    private int currentIndex(DeskWindow w){var current=LaptopMusic.selected(position);var tracks=PhoneMusic.tracks();for(int i=0;i<tracks.size();i++)if(current!=null&&tracks.get(i).id().equals(current.id()))return i;return w.musicSelection;}
    private void drawTaskbar(GuiGraphics g){
        rect(g,0,BAR,W,H-BAR,0xFF101D29);rect(g,0,BAR,W,1,0xFF44606C);
        if(start||hover(4,BAR+3,31,22))rect(g,4,BAR+3,31,22,0xFF304D58);logo(g,12,BAR+7,15);
        hit("Start",4,BAR+3,31,22,()->{start=!start;quick=false;searchFocus=start;search="";focus=-1;});
        button(g,"Search","Find",39,BAR+4,37,20,()->{start=true;quick=false;searchFocus=true;search="";});
        int x=85;List<Integer> apps=new ArrayList<>(List.of(0,1,22,3,7));for(var w:windows.stream().sorted(java.util.Comparator.comparingInt(v->v.app)).toList())if(!apps.contains(w.app))apps.add(w.app);
        int pages=Math.max(1,(apps.size()+7)/8);taskbarPage=Math.min(taskbarPage,pages-1);for(int app:apps.subList(taskbarPage*8,Math.min(apps.size(),taskbarPage*8+8))){int id=app;DeskWindow w=windows.stream().filter(a->a.app==id).findFirst().orElse(null);
            if(hover(x,BAR+3,27,23))rect(g,x,BAR+3,27,23,0xFF304D58);icon(g,app,x+5,BAR+5,17);
            if(w!=null)rect(g,x+7,BAR+24,13,2,w.minimized?EDGE:accent());
            hit("Taskbar "+APPS[app],x,BAR+3,27,23,()->{if(w==null)launch(id);else if(w.minimized){w.minimized=false;raise(w);}else if(windows.getLast()==w)w.minimized=true;else raise(w);start=false;quick=false;});x+=29;
        }
        if(pages>1)button(g,"Taskbar next",">",326,BAR+4,22,20,()->taskbarPage=(taskbarPage+1)%pages);
        button(g,"Quick settings","...",354,BAR+4,26,20,()->{quick=!quick;start=false;focus=-1;});
        LocalDateTime now=LocalDateTime.now();String time=now.format(DateTimeFormatter.ofPattern(clockPattern())),date=now.format(DateTimeFormatter.ofPattern("MMM d"));
        center(g,time,426,BAR+4,INK);if(prefs.showDate)center(g,date,426,BAR+15,MUTED);hit("Clock",383,BAR+2,82,25,()->{quick=!quick;start=false;});
        rect(g,473,BAR+2,1,24,EDGE);hit("Show desktop",470,BAR+1,10,27,this::desktop);
    }
    private List<Integer> matchingApps(String query){List<Integer> result=new ArrayList<>();for(int i=0;i<APPS.length;i++)if(programs.installed(i)&&APPS[i].toLowerCase(java.util.Locale.ROOT).contains(query.toLowerCase(java.util.Locale.ROOT)))result.add(i);if(result.remove(Integer.valueOf(22)))result.add(Math.min(2,result.size()),22);return result;}
    private List<Integer> matching(){return matchingApps(search);}
    private void drawStart(GuiGraphics g){
        hits.clear();frame(g,7,34,262,219,0xFF182C39);logo(g,19,46,17);text(g,"BlockOS",45,46,INK);text(g,"Your desktop, one block away.",45,59,MUTED);
        rect(g,18,79,240,23,0xFF0D1D29);g.renderOutline(18,79,240,23,searchFocus?accent():EDGE);
        text(g,search.isEmpty()?"Search apps...":search,27,87,search.isEmpty()?MUTED:INK);if(searchFocus&&(System.currentTimeMillis()/500)%2==0)rect(g,27+font.width(search),86,1,10,accent());
        hit("Search apps",18,79,240,23,()->searchFocus=true);text(g,search.isEmpty()?"PINNED APPS":"SEARCH RESULTS",19,112,MUTED);
        List<Integer> results=matching();int pages=Math.max(1,(results.size()+7)/8);startPage=Math.min(startPage,pages-1);button(g,"Start page",(startPage+1)+"/"+pages+" >",205,107,50,17,()->startPage=(startPage+1)%pages);for(int i=0;i<8&&startPage*8+i<results.size();i++){int app=results.get(startPage*8+i),x=17+(i%4)*61,y=128+(i/4)*42;
            if(hover(x,y,60,40))rect(g,x,y,60,40,0xFF2B4654);icon(g,app,x+20,y+2,20);center(g,font.plainSubstrByWidth(APPS[app],59),x+30,y+27,INK);hit("Launch "+APPS[app],x,y,60,40,()->launch(app));}
        if(results.isEmpty())text(g,"No apps found.",25,139,MUTED);
        rect(g,8,217,260,35,0xFF11212E);text(g,font.plainSubstrByWidth(minecraft.player.getName().getString(),85),20,230,INK);
        button(g,"Lock","Lock",116,225,51,20,()->{locked=true;start=false;searchFocus=false;focus=-1;});
        button(g,"Leave laptop","Exit",177,225,78,20,this::onClose);
    }
    private void drawQuick(GuiGraphics g){
        hits.clear();frame(g,258,123,216,130,0xFF182C39);text(g,"DESKTOP",272,136,INK);button(g,"Open full settings","Settings",395,132,64,20,()->launch(7));text(g,"Wallpaper",272,158,MUTED);
        String[] names={"Ocean","Dusk","Forest"};for(int i=0;i<3;i++){int id=i;button(g,"Wallpaper "+names[i],names[i],272+i*63,174,58,21,()->{chooseWallpaper(id);});if(prefs.wallpaperPhoto.isEmpty()&&prefs.wallpaper==i)rect(g,276+i*63,196,50,2,accent());}
        button(g,"Lock desktop","Lock",272,211,85,23,()->{locked=true;quick=false;focus=-1;});button(g,"Exit desktop","Exit laptop",365,211,94,23,this::onClose);
    }
    private void drawLock(GuiGraphics g){
        rect(g,0,0,W,H,0x77101B28);var now=LocalDateTime.now();String time=now.format(DateTimeFormatter.ofPattern(clockPattern()));
        g.pose().pushPose();g.pose().translate(W/2,75,0);g.pose().scale(2.8f,2.8f,1);center(g,time,0,0,INK);g.pose().popPose();
        if(prefs.showDate)center(g,now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),W/2,111,INK);logo(g,231,148,18);
        button(g,"Unlock","Welcome back",W/2-70,186,140,25,()->{locked=false;focus=-1;});center(g,"Press Enter to return to your desktop",W/2,226,MUTED);
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        mx=(int)((x-left)/scale);my=(int)((y-top)/scale);
        if(button==1){var w=activeWindow();if(!locked&&!start&&!quick&&w!=null&&w.game instanceof LaptopGames.Mines mines){for(int i=hits.size()-1;i>=0;i--){var h=hits.get(i);if(h.contains(mx,my)){if(h.name.startsWith("Mines cell "))mines.flag(Integer.parseInt(h.name.substring(11)));break;}}}return true;}
        if(button!=0)return true;mx=(int)((x-left)/scale);my=(int)((y-top)/scale);
        for(int i=hits.size()-1;i>=0;i--){Hit h=hits.get(i);if(h.contains(mx,my)){if(!start&&!quick){for(int j=windows.size()-1;j>=0;j--){var win=windows.get(j);if(!win.minimized&&mx>=win.x&&mx<win.x+win.w&&my>=win.y&&my<win.y+win.h){raise(win);break;}}}focus=-1;h.action.run();return true;}}
        if(start||quick){start=false;quick=false;searchFocus=false;focus=-1;}return true;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        int px=(int)((x-left)/scale),py=(int)((y-top)/scale);
        if(painting!=null){paintAt(painting,paintCell(painting,px,py));return true;}
        if(dragging!=null){dragging.x=Math.max(2,Math.min(W-dragging.w-2,px-dragX));dragging.y=Math.max(2,Math.min(BAR-dragging.h-2,py-dragY));return true;}
        if(resizing!=null){int minW=resizing.app==0||resizing.app==1||resizing.app>=5?400:resizing.app==4?344:resizing.app==3?350:290,minH=resizing.app>=8?240:resizing.app==0||resizing.app==1||resizing.app>=5?230:resizing.app==4?212:resizing.app==3?212:resizing.app==2?220:164;resizing.w=Math.max(minW,Math.min(W-resizing.x-2,px-resizing.x));resizing.h=Math.max(minH,Math.min(BAR-resizing.y-2,py-resizing.y));return true;}
        var w=activeWindow();if(!locked&&!start&&!quick&&w!=null&&w.app==3&&!w.confirmDelete){if(w.noteBody!=null&&w.noteBody.isFocused())w.noteBody.mouseDragged(px,py,button,dx/scale,dy/scale);else if(w.noteTitle!=null&&w.noteTitle.isFocused())w.noteTitle.mouseDragged(px,py,button,dx/scale,dy/scale);}return true;
    }
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){
        if(start){int pages=Math.max(1,(matching().size()+7)/8);startPage=Math.floorMod(startPage+(dy>0?-1:1),pages);return true;}
        if(locked||quick)return true;double px=(x-left)/scale,py=(y-top)/scale;
        for(int i=windows.size()-1;i>=0;i--){var w=windows.get(i);if(!w.minimized&&px>=w.x&&px<w.x+w.w&&py>=w.y&&py<w.y+w.h){if(w.app==1)w.browser.tab().scroll=Math.max(0,w.browser.tab().scroll+(dy>0?-1:1));if(w.app==17)w.programScroll=Math.max(0,w.programScroll+(dy>0?-1:1));if(w.app==4)musicPage(w,dy>0?-1:dy<0?1:0);if(w.app==0)w.filesScroll=Math.max(0,w.filesScroll+(dy>0?-1:1));if(w.app==5&&!w.confirmDelete){if(w.photoId.isEmpty())w.photoPage=Math.max(0,w.photoPage+(dy>0?-1:1));else nextPhoto(w,dy>0?-1:1);}if(w.app==6)w.terminalScroll=Math.max(0,w.terminalScroll+(dy>0?3:-3));if(w.app==3&&!w.confirmDelete){if(px<w.x+114)notesPage(w,dy>0?-1:dy<0?1:0);else if(w.noteBody!=null)w.noteBody.mouseScrolled(px,py,dx,dy);}return true;}}return true;
    }
    @Override public boolean mouseReleased(double x,double y,int button){dragging=null;resizing=null;painting=null;var w=activeWindow();if(w!=null&&w.noteBody!=null)w.noteBody.mouseReleased((x-left)/scale,(y-top)/scale,button);return true;}
    @Override public boolean keyReleased(int key,int scan,int mods){for(var w:windows)if(w.game instanceof LaptopGames.Breakout ball&&(key==GLFW.GLFW_KEY_LEFT||key==GLFW.GLFW_KEY_RIGHT||key==GLFW.GLFW_KEY_A||key==GLFW.GLFW_KEY_D))ball.move=0;return true;}
    @Override public boolean charTyped(char c,int mods){
        if(locked||quick)return true;
        if(start){if(searchFocus&&!Character.isISOControl(c)&&search.length()<28){search+=c;startPage=0;focus=-1;}return true;}
        var w=activeWindow();if(w==null)return true;
        if(w.app==1&&w.browserAddress!=null&&w.browserAddress.isFocused()){w.browserAddress.charTyped(c,mods);return true;}
        if(w.app>=8&&w.programInput!=null&&w.programInput.isFocused()){w.programInput.charTyped(c,mods);return true;}
        if(w.app==0&&w.filesSearch!=null&&w.filesSearch.isFocused()){w.filesSearch.charTyped(c,mods);return true;}
        if(w.app==6&&w.commandInput!=null&&w.commandInput.isFocused()){w.commandInput.charTyped(c,mods);return true;}
        if(w.app==2){if(c>='0'&&c<='9'||".+-*/%=".indexOf(c)>=0)calc(w,String.valueOf(c));else if(c=='c'||c=='C')calc(w,"C");}
        if(w.app==3&&!w.confirmDelete){focus=-1;if(w.noteTitle!=null&&w.noteTitle.isFocused())w.noteTitle.charTyped(c,mods);else if(w.noteBody!=null&&w.noteBody.isFocused())w.noteBody.charTyped(c,mods);}return true;
    }
    @Override public boolean keyPressed(int key,int scan,int mods){
        if(key==GLFW.GLFW_KEY_ESCAPE){if(start||quick){start=false;quick=false;searchFocus=false;}else onClose();return true;}
        if(locked){if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_SPACE)locked=false;return true;}
        var active=activeWindow();
        if(!start&&!quick&&active!=null){
            if(key==GLFW.GLFW_KEY_F4&&hasAltDown()){closeWindow(active);return true;}
            if(active.app==1){
                if(hasControlDown()&&key==GLFW.GLFW_KEY_L){if(active.browserAddress!=null){active.browserAddress.setFocused(true);active.browserAddress.setHighlightPos(0);active.browserAddress.setCursorPosition(active.browserAddress.getValue().length());}return true;}
                if(hasControlDown()&&key==GLFW.GLFW_KEY_T){active.browser.add();active.browserAddress=null;return true;}
                if(hasControlDown()&&key==GLFW.GLFW_KEY_W){active.browser.close();active.browserAddress=null;return true;}
                if(hasAltDown()&&(key==GLFW.GLFW_KEY_LEFT||key==GLFW.GLFW_KEY_RIGHT)){browserMove(active,key==GLFW.GLFW_KEY_LEFT?-1:1);return true;}
                if(active.browserAddress!=null&&active.browserAddress.isFocused()){if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER){browse(active,active.browser.tab().draft);return true;}if(key!=GLFW.GLFW_KEY_TAB){active.browserAddress.keyPressed(key,scan,mods);return true;}active.browserAddress.setFocused(false);}
            }
            if(active.app>=8&&active.programInput!=null&&active.programInput.isFocused()){if(active.app==17&&(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER)){addTask(active);return true;}if(key!=GLFW.GLFW_KEY_TAB){active.programInput.keyPressed(key,scan,mods);return true;}active.programInput.setFocused(false);}
            if(active.game!=null&&gameKey(active,key))return true;
            if(active.app==6&&active.commandInput!=null&&active.commandInput.isFocused()){
                if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER){runTerminal(active);return true;}
                if(key==GLFW.GLFW_KEY_UP||key==GLFW.GLFW_KEY_DOWN){active.commandHistory=Math.max(0,Math.min(active.commands.size(),active.commandHistory+(key==GLFW.GLFW_KEY_UP?-1:1)));active.commandInput.setValue(active.commandHistory<active.commands.size()?active.commands.get(active.commandHistory):"");return true;}
                if(hasControlDown()&&key==GLFW.GLFW_KEY_L){active.output.clear();return true;}
                if(key!=GLFW.GLFW_KEY_TAB){active.commandInput.keyPressed(key,scan,mods);return true;}active.commandInput.setFocused(false);
            }
            if(active.app==0&&active.filesSearch!=null&&active.filesSearch.isFocused()){if(key!=GLFW.GLFW_KEY_TAB){active.filesSearch.keyPressed(key,scan,mods);return true;}active.filesSearch.setFocused(false);}
            if(active.app==5&&!active.confirmDelete&&!active.photoBusy&&!active.photoId.isEmpty()){if(key==GLFW.GLFW_KEY_LEFT||key==GLFW.GLFW_KEY_RIGHT){nextPhoto(active,key==GLFW.GLFW_KEY_LEFT?-1:1);return true;}if(hasControlDown()&&key==GLFW.GLFW_KEY_C){copyPhoto(active);return true;}}
            if(active.app==3){
                if(hasControlDown()&&key==GLFW.GLFW_KEY_S){notify(notes.save()?"Notebook saved":"Save failed - try again");return true;}
                if(!active.confirmDelete&&hasControlDown()&&key==GLFW.GLFW_KEY_N){newNote(active);return true;}
                if(!active.confirmDelete&&key!=GLFW.GLFW_KEY_TAB){
                    if(active.noteTitle!=null&&active.noteTitle.isFocused()){active.noteTitle.keyPressed(key,scan,mods);return true;}
                    if(active.noteBody!=null&&active.noteBody.isFocused()){active.noteBody.keyPressed(key,scan,mods);return true;}
                }
                if(key==GLFW.GLFW_KEY_TAB){if(active.noteTitle!=null)active.noteTitle.setFocused(false);if(active.noteBody!=null)active.noteBody.setFocused(false);}
            }
            if(active.app==2){
                if(hasControlDown()&&key==GLFW.GLFW_KEY_C){minecraft.keyboardHandler.setClipboard(active.calculator.display());return true;}
                if(key==GLFW.GLFW_KEY_BACKSPACE){calc(active,"BACK");return true;}
                if(key==GLFW.GLFW_KEY_DELETE){calc(active,"C");return true;}
                if((key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER)&&focus<0){calc(active,"=");return true;}
            }
        }
        if(key==GLFW.GLFW_KEY_TAB&&!hits.isEmpty()){focus=Math.floorMod(focus+(hasShiftDown()?-1:1),hits.size());return true;}
        if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_SPACE&&focus>=0){if(focus>=0&&focus<hits.size())hits.get(focus).action.run();else if(start&&!matching().isEmpty())launch(matching().getFirst());return true;}
        if(start&&searchFocus&&key==GLFW.GLFW_KEY_BACKSPACE){if(!search.isEmpty())search=search.substring(0,search.length()-1);startPage=0;return true;}
        return true;
    }
}
