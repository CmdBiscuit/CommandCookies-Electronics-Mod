package net.mcreator.commandcookieselectronicmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.mcreator.commandcookieselectronicmod.CommandcookiesElectronicModMod;
import org.lwjgl.glfw.GLFW;

/** BlockOS Chromium app: live browser tabs owned by a single computer's desktop session. */
public final class ChromiumScreen extends Screen {
    private static final int INK=0xFFE8EAED,MUTED=0xFF9AA0A6,EDGE=0xFF3C4043,PANEL=0xFF292A2D,BAR=0xFF202124,PAGE=0xFFFFFFFF,TRACK=0xFF202124;
    private static final String HOME="https://www.google.com/";
    private enum Mode {LIVE,STRIP,DIALOG,DOWNLOAD,PREPARE,FAILED}
    private final IphoneScreen phoneHome;
    private int phoneX,phoneY,phoneW,phoneH;private boolean phoneExpanded=true;
    private final Screen parent;private final BlockPos source;private final UUID device,desktopSession;
    private boolean keepOnRemoval;
    private boolean media,preferenceLoaded;private ChromiumSession session;private String failure;private boolean closing,dirty;private Mode mode=Mode.FAILED;
    private int px,py,pw,ph,vx,vy,vw,vh,browserW,browserH,cx,cy,cw,ch,dx,dy,dw,dh;
    private boolean ready,loading,canBack,canForward;private String status="",url="",title="";private double progress=-1;
    private String lastUrl,dismissed,addressText="",promptText="";
    private boolean pageFocused;private int pageButtons,lastX,lastY;private boolean pointerSent;private final Set<Integer> pageKeys=new HashSet<>();private char highSurrogate;
    private List<ChromiumSession.Tab> tabs=List.of();private int activeTab,tabOffset;
    private ChromiumSession.Dialog dialog,answered;private List<String> dialogLines=List.of();
    private EditBox address,prompt;private Button back,forward,reload,home;

    public ChromiumScreen(Screen parent,BlockPos source,UUID device,UUID desktopSession){super(Component.literal("Chromium"));this.phoneHome=null;this.parent=parent;this.source=source.immutable();this.device=device;this.desktopSession=desktopSession;}
    public ChromiumScreen(IphoneScreen home){super(Component.literal("Chromium"));phoneHome=home;parent=home;source=BlockPos.ZERO;device=home.deviceId();desktopSession=null;}
    private boolean compactPhone(){return phoneHome!=null&&!phoneExpanded;}
    private void togglePhoneView(){releaseHeld();phoneExpanded=!phoneExpanded;init();}
    public PhonePreferences preferences(){return phoneHome==null?null:phoneHome.preferences();}
    @Override public boolean isPauseScreen(){return false;}

    private boolean valid(){
        var mc=minecraft;if(mc==null||mc.player==null||mc.level==null||!mc.player.isAlive()||mc.player.isDeadOrDying()||device==null)return false;
        if(phoneHome!=null)return PhoneMessagesClient.held(device);
        var a=DesktopAccess.at(mc.level,source);
        return a!=null&&device.equals(a.device())&&Objects.equals(desktopSession,a.session())&&mc.player.distanceToSqr(source.getX()+.5,source.getY()+.5,source.getZ()+.5)<=64;
    }
    private int accent(){if(phoneHome!=null)return phoneHome.preferences().accentColor();try {if(parent instanceof LaptopDesktopScreen desktop&&desktop.preferences()!=null)return desktop.preferences().accentColor();}catch(RuntimeException ignored){}return LaptopPreferences.ACCENT_COLORS[0];}

    @Override protected void init(){
        int oldW=browserW,oldH=browserH;layout();
        if(session==null&&failure==null)open();
        else if(session!=null&&(oldW!=browserW||oldH!=browserH)){int w=browserW,h=browserH;call(()->session.resize(w,h));}
        poll();mode=currentMode();dirty=false;build();
    }
    private void layout(){
        if(compactPhone()){
            phoneH=Math.min(340,height-12);phoneW=Math.min(width-12,Math.max(160,Math.round(phoneH*.54f)));
            phoneX=(width-phoneW)/2;phoneY=(height-phoneH)/2;
            px=phoneX+7;py=phoneY+26;pw=phoneW-14;ph=phoneH-48;
        }else{int m=width>=480&&height>=300?8:3;px=m;py=m;pw=width-2*m;ph=height-2*m;}
        int toolbar=compactPhone()?68:50;
        vx=px;vy=py+toolbar;vw=Math.max(40,pw);vh=Math.max(40,ph-toolbar);
        double scale=Math.min(minecraft.getWindow().getGuiScale(),Math.min(2560d/vw,1440d/vh));
        if(compactPhone())scale=Math.min(2d,scale);
        browserW=Math.max(1,(int)Math.round(vw*scale));browserH=Math.max(1,(int)Math.round(vh*scale));
    }
    private static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}

    private void open(){
        failure=null;dialog=answered=null;dismissed=null;lastUrl=null;ready=false;status="";progress=-1;
        if(minecraft.player==null){failure="Chromium needs you to be in a world.";return;}
        try {if(phoneHome!=null){session=PhoneBrowserPlayback.resume(minecraft.player.getUUID(),device,browserW,browserH,phoneHome.preferences().chromiumFps);if(session!=null){media=true;preferenceLoaded=true;return;}}if(!preferenceLoaded){media=ChromiumSession.preferredMedia(minecraft.player.getUUID(),device);preferenceLoaded=true;}session=phoneHome==null?ChromiumSession.open(minecraft.player.getUUID(),device,browserW,browserH,media):ChromiumSession.open(minecraft.player.getUUID(),device,browserW,browserH,media,phoneHome.preferences().chromiumFps);if(session==null)failure="Chromium couldn't start. Try again in a moment.";}
        catch(Exception|LinkageError error){session=null;failure=explain(error);CommandcookiesElectronicModMod.LOGGER.warn("Chromium could not open",error);}
    }
    private static String explain(Throwable error){
        Throwable t=error;for(int depth=0;t!=null&&depth<8;depth++,t=t.getCause()){
            if(t instanceof UnsupportedOperationException)return "Chromium isn't supported on this computer's operating system.";
            if(t instanceof java.io.IOException||t instanceof java.io.UncheckedIOException)return "Chromium couldn't get its browser files. Check your internet connection, then try again.";
            if(t instanceof LinkageError)return "Chromium's browser files couldn't be loaded. Try again, or restart Minecraft.";
        }
        return "Chromium couldn't start. Try again in a moment.";
    }
    private void release(){
        var s=session;session=null;ready=false;loading=false;dialog=answered=null;pageButtons=0;pageKeys.clear();pageFocused=false;pointerSent=false;highSurrogate=0;
        if(s!=null)try {s.close();}catch(Exception|LinkageError error){CommandcookiesElectronicModMod.LOGGER.warn("Chromium did not close cleanly",error);}
    }
    private void fail(Throwable error){CommandcookiesElectronicModMod.LOGGER.warn("Chromium stopped",error);release();failure="Chromium stopped unexpectedly. Choose Try again to restart it.";dirty=true;}
    private void call(Runnable action){if(session==null)return;try {action.run();}catch(Exception|LinkageError error){fail(error);}}

    private void poll(){
        var s=session;if(s==null){ready=false;return;}
        try {
            ready=s.ready();status=clean(s.status());progress=s.progress();if(s.failed()){failure=status;release();dirty=true;return;}if(status.isEmpty())dismissed=null;
            if(!ready){dialog=null;return;}
            url=s.url()==null?"":s.url();title=clean(s.title());canBack=s.canBack();canForward=s.canForward();loading=s.loading();
            if(!tabs.equals(s.tabs())||activeTab!=s.activeTab()){tabs=s.tabs();activeTab=clamp(s.activeTab(),0,Math.max(0,tabs.size()-1));int visible=visibleTabs();if(activeTab<tabOffset)tabOffset=activeTab;if(activeTab>=tabOffset+visible)tabOffset=activeTab-visible+1;dirty=true;}
            var d=s.dialog();if(d==null)answered=null;else if(d.equals(answered))d=null;
            if(!Objects.equals(d,dialog)){dialog=d;promptText=d==null||d.initial()==null?"":d.initial();dirty=true;}
        }catch(Exception|LinkageError error){fail(error);}
    }
    private Mode currentMode(){
        if(session==null)return Mode.FAILED;if(session.waitingForDownloadConsent())return Mode.DOWNLOAD;if(!ready)return Mode.PREPARE;if(dialog!=null)return Mode.DIALOG;
        return !status.isEmpty()&&!status.equals(dismissed)?Mode.STRIP:Mode.LIVE;
    }
    private static boolean live(Mode m){return m==Mode.LIVE||m==Mode.STRIP;}
    private boolean live(){return session!=null&&live(mode);}
    private void refresh(){
        Mode next=currentMode();if(next==mode&&!dirty)return;
        boolean wasLive=live(mode);if(wasLive&&!live(next))releaseHeld();
        mode=next;dirty=false;build();
    }
    /** Lifts anything still held on the page so a modal or overlay never leaves a stuck button or key. */
    private void releaseHeld(){
        int x=lastX,y=lastY;
        for(int b=0;b<3;b++)if((pageButtons&1<<b)!=0){int button=b;call(()->session.mouse(2,x,y,button,0));}
        for(int key:List.copyOf(pageKeys))call(()->session.key(key,true,0));
        pageButtons=0;pageKeys.clear();highSurrogate=0;
    }

    private Button add(int x,int y,int w,int h,String label,String tip,Runnable action){
        var b=new Button(x,y,w,h,Component.literal(label),button->action.run(),supplier->supplier.get()){
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
                int ink=active?INK:0xFF5F6368;
                if(label.equals("Download"))rounded(g,getX(),getY(),getWidth(),getHeight(),5,isHoveredOrFocused()?0xFF2D7BEF:0xFF1967D2);
                else if(active&&(isHoveredOrFocused()))rounded(g,getX(),getY(),getWidth(),getHeight(),5,0xFF3C4043);
                int ox=getX()+getWidth()/2,oy=getY()+getHeight()/2;
                if(label.equals("Back")||label.equals("Forward")){
                    int direction=label.equals("Back")?-1:1;
                    g.fill(ox-5,oy,ox+6,oy+1,ink);
                    for(int i=0;i<5;i++)g.fill(ox+direction*(5-i),oy-i,ox+direction*(5-i)+1,oy+i+1,ink);
                }else if(label.equals("Reload")){
                    for(int i=0;i<12;i++){double a=i*Math.PI/7.5;int rx=ox+(int)Math.round(Math.cos(a)*5),ry=oy+(int)Math.round(Math.sin(a)*5);g.fill(rx,ry,rx+2,ry+2,ink);}
                    g.fill(ox+4,oy-5,ox+7,oy-1,ink);g.fill(ox+2,oy-3,ox+6,oy-1,ink);
                }else if(label.equals("Home")){
                    for(int i=0;i<5;i++)g.fill(ox-i,oy-5+i,ox+i+1,oy-4+i,ink);
                    g.fill(ox-4,oy,ox+5,oy+6,ink);g.fill(ox-1,oy+2,ox+2,oy+6,PANEL);
                }else if(label.equals("Close")){
                    for(int i=-3;i<=3;i++){g.fill(ox+i,oy+i,ox+i+1,oy+i+1,ink);g.fill(ox+i,oy-i,ox+i+1,oy-i+1,ink);}
                }else g.drawCenteredString(font,label.equals("Expand")?"[]":getMessage().getString(),ox,oy-4,ink);
            }
        };if(tip!=null)b.setTooltip(Tooltip.create(Component.literal(tip)));return addRenderableWidget(b);
    }
    private void build(){
        boolean typing=address!=null&&address.isFocused();if(address!=null)addressText=address.getValue();
        setFocused(null);clearWidgets();address=null;prompt=null;
        if(compactPhone()){
            back=add(px,py,20,20,"Back","Back",()->call(()->session.back()));
            forward=add(px+21,py,20,20,"Forward","Forward",()->call(()->session.forward()));
            reload=add(px+42,py,20,20,"Reload","Reload",()->call(()->session.reload()));
            home=add(px,phoneY+phoneH-21,20,16,"Home","Google",()->navigate(HOME));
            add(px+63,py,20,20,"Expand","Expand browser",this::togglePhoneView);
            add(px+pw-48,py,48,20,media?"Media":"Standard",media?"Switch to standard browsing.":"Enable protected media playback.",this::changeBrowser);
            address=new EditBox(font,px+8,py+29,pw-16,12,Component.literal("Address"));
            address.setBordered(false);address.setTextColor(INK);address.setTextColorUneditable(MUTED);address.setMaxLength(8192);
            address.setHint(Component.literal("Search or URL"));address.setValue(addressText);address.moveCursorToStart(false);address.setResponder(v->addressText=v);addRenderableWidget(address);
            add(px+23,phoneY+phoneH-21,pw-23,16,"Phone","Return to the phone (Esc)",this::onClose);
            if(live(mode)){
                var previous=add(px,py+47,16,18,"<","Previous tab",()->selectTab(Math.floorMod(activeTab-1,tabs.size())));previous.active=tabs.size()>1;
                add(px+pw-57,py+47,18,18,"Close","Close tab",()->closeTab(activeTab));
                var plus=add(px+pw-38,py+47,20,18,"+","New tab",this::newTab);plus.active=tabs.size()<8;
                var next=add(px+pw-16,py+47,16,18,">","Next tab",()->selectTab((activeTab+1)%tabs.size()));next.active=tabs.size()>1;
            }
        }else{
        int x=px+4,y=py+25;
        back=add(x,y,22,22,"Back","Back (Alt+Left)",()->call(()->session.back()));
        forward=add(x+24,y,22,22,"Forward","Forward (Alt+Right)",()->call(()->session.forward()));
        reload=add(x+48,y,22,22,"Reload","Reload (Ctrl+R)",()->call(()->session.reload()));
        home=add(x+72,y,22,22,"Home","Google",()->navigate(HOME));
        address=new EditBox(font,x+118,y+7,Math.max(40,pw-132),12,Component.literal("Address"));
        address.setBordered(false);address.setTextColor(INK);address.setTextColorUneditable(MUTED);
        address.setMaxLength(8192);address.setHint(Component.literal("Search Google or type a URL"));address.setValue(addressText);address.moveCursorToStart(false);address.setResponder(v->addressText=v);
        addRenderableWidget(address);
        add(px+pw-64,py+2,60,20,phoneHome==null?"Desktop":"Phone",phoneHome==null?"Return to the desktop (Esc)":"Return to the phone (Esc)",this::onClose);
        if(phoneHome!=null)add(px+pw-174,py+2,56,20,"Phone view","Compact phone view",this::togglePhoneView);
        add(px+pw-114,py+2,46,20,media?"Media":"Standard",media?"Media playback enabled. Switch to standard browsing.":"Enable protected media playback.",this::changeBrowser);
        if(live(mode)){
            int count=visibleTabs(),tw=tabWidth(),start=tabStart();
            if(tabs.size()>count){add(px+2,py+3,16,18,"<","Earlier tabs",()->{tabOffset=Math.max(0,tabOffset-1);dirty=true;});add(px+pw-158-(phoneHome==null?0:60),py+3,16,18,">","Later tabs",()->{tabOffset=Math.min(Math.max(0,tabs.size()-count),tabOffset+1);dirty=true;});}
            for(int j=0;j<count&&tabOffset+j<tabs.size();j++){int index=tabOffset+j;add(start+j*tw,py+3,tw-19,18,"","Switch tab",()->selectTab(index));add(start+(j+1)*tw-19,py+3,18,18,"Close","Close tab",()->closeTab(index));}
            var plus=add(px+pw-138-(phoneHome==null?0:60),py+3,20,18,"+","New tab (Ctrl+T)",this::newTab);plus.active=tabs.size()<8;
        }
        }
        if(mode==Mode.STRIP)add(vx+vw-60,vy+vh-18,56,16,"Dismiss",null,()->{dismissed=status;refresh();});
        else if(mode==Mode.DOWNLOAD){card(media?146:124);int by=cy+ch-28,bw=(cw-30)/2;add(cx+10,by,bw,20,"Download",null,()->call(()->session.confirmDownload()));add(cx+cw-10-bw,by,bw,20,"Cancel",null,this::onClose);}
        else if(mode==Mode.PREPARE){card(136);int by=cy+ch-28,bw=(cw-30)/2;add(cx+10,by,bw,20,"Cancel",null,this::cancel);add(cx+cw-10-bw,by,bw,20,phoneHome==null?"Back to desktop":"Phone",null,this::onClose);}
        else if(mode==Mode.FAILED){card(112);int by=cy+ch-28,bw=(cw-30)/2;add(cx+10,by,bw,20,"Try again",null,this::retry);add(cx+cw-10-bw,by,bw,20,phoneHome==null?"Back to desktop":"Phone",null,this::onClose);}
        else if(mode==Mode.DIALOG&&dialog!=null)buildDialog();
        updateChrome();
        if(typing&&live(mode)){setFocused(address);address.moveCursorToEnd(false);}
    }
    private void buildDialog(){
        String kind=kind();boolean ask=kind.equals("prompt"),alert=kind.equals("alert"),unload=kind.contains("unload");
        dw=Math.max(120,Math.min(420,(phoneHome==null?vw:width)-16));int fixed=24+6+(ask?26:0)+28;
        dialogLines=wrap(dialog.message(),dw-20,clamp(((phoneHome==null?vh:height)-16-fixed)/10,1,14));
        dh=fixed+dialogLines.size()*10;dx=phoneHome==null?vx+(vw-dw)/2:(width-dw)/2;dy=phoneHome==null?vy+Math.max(8,(vh-dh)/2):(height-dh)/2;int by=dy+dh-28;
        if(ask){prompt=new EditBox(font,dx+10,by-26,dw-20,20,Component.literal("Answer"));prompt.setMaxLength(4096);prompt.setValue(promptText);prompt.setResponder(v->promptText=v);addRenderableWidget(prompt);setFocused(prompt);}
        add(dx+dw-70,by,60,20,unload?"Leave":"OK",null,()->answer(true));
        if(!alert)add(dx+dw-134,by,60,20,unload?"Stay":"Cancel",null,()->answer(false));
    }
    private String kind(){return dialog==null||dialog.kind()==null?"":dialog.kind().toLowerCase(Locale.ROOT);}
    private void card(int height){if(phoneHome!=null){cw=Math.max(140,Math.min(380,width-20));ch=height;cx=(width-cw)/2;cy=(this.height-ch)/2;}else{cw=Math.max(140,Math.min(380,vw-16));ch=height;cx=vx+(vw-cw)/2;cy=vy+Math.max(8,(vh-ch)/2);}}
    private void updateChrome(){
        if(back==null)return;boolean live=live();
        back.active=live&&canBack;forward.active=live&&canForward;reload.active=live;home.active=live;if(address!=null)address.setEditable(live);
    }

    private void answer(boolean accept){
        if(dialog==null)return;String text=prompt==null?"":prompt.getValue();answered=dialog;dialog=null;
        call(()->session.answerDialog(accept,text));refresh();
    }
    private void cancel(){release();failure="Preparing Chromium was canceled. Choose Try again to continue.";refresh();}
    private void changeBrowser(){releaseHeld();release();media=!media;tabs=List.of();activeTab=tabOffset=0;open();poll();dirty=true;refresh();}
    private void retry(){release();open();poll();dirty=true;refresh();}
    private void navigate(String text){
        String target=text==null?"":text.strip();if(target.isEmpty()||!live())return;
        setFocused(null);addressText=target;if(address!=null){address.setValue(target);address.moveCursorToStart(false);}
        focusPage(true);call(()->session.navigate(target));
    }
    private int visibleTabs(){if(compactPhone())return 1;return Math.max(1,Math.min(8,(pw-180-(phoneHome==null?0:60))/90));}
    private int tabStart(){return px+(tabs.size()>visibleTabs()?20:3);}
    private int tabWidth(){return Math.min(180,Math.max(45,(pw-(tabs.size()>visibleTabs()?180:150)-(phoneHome==null?0:60))/Math.max(1,Math.min(visibleTabs(),tabs.size()))));}
    private void selectTab(int index){releaseHeld();focusPage(false);setFocused(null);lastUrl=null;call(()->session.switchTab(index));}
    private void newTab(){releaseHeld();focusPage(false);setFocused(null);lastUrl=null;call(()->session.newTab());}
    private void closeTab(int index){releaseHeld();focusPage(false);setFocused(null);lastUrl=null;call(()->session.closeTab(index));}
    private void focusAddress(){if(address==null||!live())return;focusPage(false);setFocused(address);address.moveCursorToEnd(false);address.setHighlightPos(0);}
    private void focusPage(boolean value){boolean changed=pageFocused!=value;pageFocused=value;if(changed||value)call(()->session.focus(value));}

    @Override public void tick(){
        if(!valid()){closing=true;release();minecraft.setScreen(null);return;}
        poll();
        if(ready&&address!=null&&!address.isFocused()&&!url.equals(lastUrl)){lastUrl=url;address.setValue(url);address.moveCursorToStart(false);}
        refresh();updateChrome();
    }
    @Override public void onClose(){
        if(closing||!valid()){closing=true;release();minecraft.setScreen(null);}else{keepOnRemoval=phoneHome!=null&&media&&phoneHome.preferences().backgroundMedia;minecraft.setScreen(parent);}
    }
    @Override public void removed(){
        releaseHeld();
        if(keepOnRemoval&&phoneHome!=null&&minecraft.player!=null&&PhoneBrowserPlayback.retain(minecraft.player.getUUID(),device,session,phoneHome.preferences())){session=null;ready=false;keepOnRemoval=false;}
        else release();keepOnRemoval=false;
    }

    private boolean overPage(double mx,double my){
        if(!live())return false;int bottom=mode==Mode.STRIP?vy+vh-20:vy+vh;
        return mx>=vx&&mx<vx+vw&&my>=vy&&my<bottom;
    }
    private void pointer(double mx,double my){lastX=clamp((int)Math.floor((mx-vx)*browserW/(double)vw),0,browserW-1);lastY=clamp((int)Math.floor((my-vy)*browserH/(double)vh),0,browserH-1);}
    private int mods(){
        long w=minecraft.getWindow().getWindow();int m=0;
        if(InputConstants.isKeyDown(w,GLFW.GLFW_KEY_LEFT_SHIFT)||InputConstants.isKeyDown(w,GLFW.GLFW_KEY_RIGHT_SHIFT))m|=GLFW.GLFW_MOD_SHIFT;
        if(InputConstants.isKeyDown(w,GLFW.GLFW_KEY_LEFT_CONTROL)||InputConstants.isKeyDown(w,GLFW.GLFW_KEY_RIGHT_CONTROL))m|=GLFW.GLFW_MOD_CONTROL;
        if(InputConstants.isKeyDown(w,GLFW.GLFW_KEY_LEFT_ALT)||InputConstants.isKeyDown(w,GLFW.GLFW_KEY_RIGHT_ALT))m|=GLFW.GLFW_MOD_ALT;
        if(InputConstants.isKeyDown(w,GLFW.GLFW_KEY_LEFT_SUPER)||InputConstants.isKeyDown(w,GLFW.GLFW_KEY_RIGHT_SUPER))m|=GLFW.GLFW_MOD_SUPER;
        return m;
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(overPage(mx,my)){
            setFocused(null);focusPage(true);
            if(button==GLFW.GLFW_MOUSE_BUTTON_4){call(()->session.back());return true;}
            if(button==GLFW.GLFW_MOUSE_BUTTON_5){call(()->session.forward());return true;}
            if(button<0||button>2)return true;
            pointer(mx,my);pointerSent=true;int x=lastX,y=lastY,m=mods();pageButtons|=1<<button;call(()->session.mouse(1,x,y,button,m));return true;
        }
        focusPage(false);super.mouseClicked(mx,my,button);return true;
    }
    @Override public boolean mouseReleased(double mx,double my,int button){
        if(button>=0&&button<=2&&(pageButtons&1<<button)!=0){
            pageButtons&=~(1<<button);pointer(mx,my);int x=lastX,y=lastY,m=mods();call(()->session.mouse(2,x,y,button,m));return true;
        }
        return super.mouseReleased(mx,my,button);
    }
    @Override public void mouseMoved(double mx,double my){
        if(session!=null&&(pageButtons!=0||overPage(mx,my))){
            int ox=lastX,oy=lastY;pointer(mx,my);
            if(!pointerSent||ox!=lastX||oy!=lastY){pointerSent=true;int x=lastX,y=lastY,m=mods(),held=pageButtons==0?0:Integer.numberOfTrailingZeros(pageButtons);call(()->session.mouse(0,x,y,held,m));}
        }
        super.mouseMoved(mx,my);
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dragX,double dragY){
        if(pageButtons!=0)return true;
        return super.mouseDragged(mx,my,button,dragX,dragY);
    }
    @Override public boolean mouseScrolled(double mx,double my,double scrollX,double scrollY){
        if(overPage(mx,my)){pointer(mx,my);int x=lastX,y=lastY,m=mods();if(scrollY!=0){double amount=-scrollY;call(()->session.wheel(x,y,amount,m));}if(scrollX!=0){double amount=-scrollX;call(()->session.wheel(x,y,amount,m|GLFW.GLFW_MOD_SHIFT));}return true;}
        return super.mouseScrolled(mx,my,scrollX,scrollY);
    }
    @Override public boolean keyPressed(int key,int scan,int mods){
        if(key==GLFW.GLFW_KEY_ESCAPE){onClose();return true;}
        boolean enter=key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER;
        if(mode==Mode.DIALOG){if(enter)answer(true);else super.keyPressed(key,scan,mods);return true;}
        if(live()){
            boolean shortcut=(mods&GLFW.GLFW_MOD_CONTROL)!=0||Minecraft.ON_OSX&&(mods&GLFW.GLFW_MOD_SUPER)!=0;
            boolean typing=address!=null&&address.isFocused();
            if(shortcut&&key==GLFW.GLFW_KEY_T){newTab();return true;}
            if(shortcut&&key==GLFW.GLFW_KEY_W){closeTab(activeTab);return true;}
            if(shortcut&&key==GLFW.GLFW_KEY_TAB&&!tabs.isEmpty()){selectTab(Math.floorMod(activeTab+((mods&GLFW.GLFW_MOD_SHIFT)!=0?-1:1),tabs.size()));return true;}
            if(shortcut&&key==GLFW.GLFW_KEY_L){focusAddress();return true;}
            if(shortcut&&key==GLFW.GLFW_KEY_R||key==GLFW.GLFW_KEY_F5){call(()->session.reload());return true;}
            if(typing){if(enter)navigate(address.getValue());else super.keyPressed(key,scan,mods);return true;}
            if((mods&GLFW.GLFW_MOD_ALT)!=0&&key==GLFW.GLFW_KEY_LEFT){call(()->session.back());return true;}
            if((mods&GLFW.GLFW_MOD_ALT)!=0&&key==GLFW.GLFW_KEY_RIGHT){call(()->session.forward());return true;}
            if(pageFocused){pageKeys.add(key);call(()->session.key(key,false,mods));return true;}
        }
        return super.keyPressed(key,scan,mods);
    }
    @Override public boolean keyReleased(int key,int scan,int mods){
        if(pageKeys.remove(key)){call(()->session.key(key,true,mods));return true;}
        return super.keyReleased(key,scan,mods);
    }
    @Override public boolean charTyped(char c,int mods){
        if(live()&&pageFocused&&(address==null||!address.isFocused())){
            int codepoint;
            if(Character.isHighSurrogate(c)){highSurrogate=c;return true;}
            if(Character.isLowSurrogate(c)){if(highSurrogate==0)return true;codepoint=Character.toCodePoint(highSurrogate,c);}else codepoint=c;
            highSurrogate=0;call(()->session.character(codepoint,mods));return true;
        }
        highSurrogate=0;return super.charTyped(c,mods);
    }

    private static String clean(String s){
        if(s==null)return "";if(s.length()>512)s=s.substring(0,512);
        return s.replace('\r',' ').replace('\n',' ').replace('\t',' ').replace('§',' ').strip();
    }
    private String fit(String s,int max){return font.width(s)<=max?s:font.plainSubstrByWidth(s,Math.max(0,max-font.width("...")))+"...";}
    private List<String> wrap(String text,int width,int max){
        String s=text==null?"":text;boolean cut=s.length()>4000;if(cut)s=s.substring(0,4000);
        s=s.replace("\r\n","\n").replace('\r','\n').replace('\t',' ').replace('§',' ');
        var out=new ArrayList<String>();for(FormattedText line:font.getSplitter().splitLines(s,width,Style.EMPTY))out.add(line.getString());
        if(out.size()>max||cut&&!out.isEmpty()){while(out.size()>max)out.remove(out.size()-1);int last=out.size()-1;out.set(last,font.plainSubstrByWidth(out.get(last),Math.max(0,width-font.width("...")))+"...");}
        return out;
    }
    private void text(GuiGraphics g,String s,int x,int y,int color){g.drawString(font,s,x,y,color,false);}
    private void center(GuiGraphics g,String s,int x,int y,int color){text(g,s,x-font.width(s)/2,y,color);}
    private void frame(GuiGraphics g,int x,int y,int w,int h,int edge){g.fill(x-1,y-1,x+w+1,y+h+1,edge);g.fill(x,y,x+w,y+h,PANEL);}
    private static void rounded(GuiGraphics g,int x,int y,int w,int h,int radius,int color){
        int r=Math.max(0,Math.min(radius,Math.min(w,h)/2));g.fill(x+r,y,x+w-r,y+h,color);g.fill(x,y+r,x+w,y+h-r,color);
        for(int i=0;i<r;i++){int inset=r-(int)Math.floor(Math.sqrt(r*r-(r-i-1)*(r-i-1)));g.fill(x+inset,y+i,x+w-inset,y+i+1,color);g.fill(x+inset,y+h-i-1,x+w-inset,y+h-i,color);}
    }
    private String host(){try {String h=java.net.URI.create(url).getHost();return h==null||h.isBlank()?"":clean(h);}catch(RuntimeException e){return "";}}

    @Override public void renderBackground(GuiGraphics g,int mx,int my,float partial){
        if(compactPhone()){
            phoneHome.renderAppFrame(g,phoneX,phoneY,phoneW,phoneH);frame(g,px,py,pw,ph,EDGE);g.fill(px,py,px+pw,py+68,PANEL);
            rounded(g,px+2,py+23,pw-4,22,7,address!=null&&address.isFocused()?0xFF3C4043:BAR);
            rounded(g,px+17,py+47,pw-76,18,5,BAR);
            String selected=tabs.isEmpty()?"Chromium":(activeTab+1)+"/"+tabs.size()+" "+clean(tabs.get(activeTab).title());text(g,fit(selected,Math.max(10,pw-84)),px+21,py+52,INK);
        }else{
        g.fill(0,0,width,height,0xFF111214);
        frame(g,px,py,pw,ph,EDGE);g.fill(px,py,px+pw,py+23,BAR);
        g.fill(px,py+23,px+pw,py+50,PANEL);
        int tw=tabWidth(),start=tabStart(),count=visibleTabs();
        if(tabs.isEmpty()){rounded(g,px+3,py+3,Math.min(180,pw-140-(phoneHome==null?0:60)),23,6,PANEL);text(g,"Chromium",px+26,py+9,INK);}
        else for(int j=0;j<count&&tabOffset+j<tabs.size();j++){int index=tabOffset+j,x=start+j*tw;rounded(g,x,py+3,tw-2,23,6,index==activeTab?PANEL:BAR);rounded(g,x+8,py+8,9,9,4,0xFF8AB4F8);rounded(g,x+11,py+11,3,3,1,0xFF3367D6);text(g,fit(clean(tabs.get(index).title()),Math.max(10,tw-48)),x+23,py+9,index==activeTab?INK:MUTED);}
        rounded(g,px+104,py+25,pw-110,22,10,address!=null&&address.isFocused()?0xFF3C4043:BAR);
        g.fill(px+110,py+31,px+116,py+32,MUTED);g.fill(px+110,py+38,px+116,py+39,MUTED);g.fill(px+112,py+29,px+113,py+34,MUTED);g.fill(px+114,py+36,px+115,py+41,MUTED);
        }
        g.fill(vx,vy,vx+vw,vy+vh,live(mode)?PAGE:BAR);
        if(live()&&loading){int seg=Math.max(30,vw/5),t=(int)(System.currentTimeMillis()/6%(vw+seg))-seg;g.fill(Math.max(vx,vx+t),vy-3,Math.min(vx+vw,vx+t+seg),vy-1,accent());}
        switch(mode){
            case LIVE,STRIP,DIALOG->{
                page(g);
                if(mode==Mode.STRIP){int sy=vy+vh-20;g.fill(vx,sy,vx+vw,vy+vh,0xF0572A31);text(g,fit(status,vw-74),vx+6,sy+6,INK);}
                if(mode==Mode.DIALOG&&dialog!=null)dialog(g);
            }
            case PREPARE->preparing(g);
            case DOWNLOAD->downloadPrompt(g);
            case FAILED->failed(g);
        }
    }
    private void page(GuiGraphics g){
        boolean drawn=false;
        if(session!=null)try {drawn=session.draw(g,vx,vy,vw,vh);}catch(Exception|LinkageError error){fail(error);}
        if(!drawn)center(g,session==null?"":"Loading page...",vx+vw/2,vy+vh/2-4,MUTED);
    }
    private void dialog(GuiGraphics g){
        g.fill(vx,vy,vx+vw,vy+vh,0xB8060B10);frame(g,dx,dy,dw,dh,accent());g.fill(dx,dy,dx+dw,dy+18,BAR);
        String kind=kind(),site=host();
        String header=kind.contains("unload")?"Leave this page?":site.isEmpty()?"This page says":site+" says";
        text(g,fit(header,dw-16),dx+8,dy+5,INK);
        for(int i=0;i<dialogLines.size();i++)text(g,dialogLines.get(i),dx+10,dy+24+i*10,INK);
    }
    private void preparing(GuiGraphics g){
        card(136);frame(g,cx,cy,cw,ch,EDGE);
        center(g,"Preparing Chromium",cx+cw/2,cy+10,INK);
        var lines=wrap(status.isEmpty()?"Getting the browser ready...":status,cw-20,2);for(int i=0;i<lines.size();i++)center(g,lines.get(i),cx+cw/2,cy+26+i*11,MUTED);
        int bx=cx+10,by=cy+54,bw=cw-20;g.fill(bx,by,bx+bw,by+10,TRACK);g.renderOutline(bx,by,bw,10,EDGE);
        double p=progress;boolean known=Double.isFinite(p)&&p>=0;if(known&&p>1.0001)p=p/100;p=Math.max(0,Math.min(1,p));
        if(known)g.fill(bx+1,by+1,bx+1+(int)Math.round((bw-2)*p),by+9,accent());
        else {int seg=Math.max(24,bw/4),t=(int)(System.currentTimeMillis()/8%(bw+seg))-seg;g.fill(Math.max(bx+1,bx+t),by+1,Math.min(bx+bw-1,bx+t+seg),by+9,accent());}
        center(g,known?Math.round(p*100)+"%":"Working...",cx+cw/2,cy+68,INK);
        var note=wrap("Browser files are saved for future use. Minecraft keeps running while it finishes.",cw-20,2);
        for(int i=0;i<note.size();i++)center(g,note.get(i),cx+cw/2,cy+82+i*10,MUTED);
    }
    private void failed(GuiGraphics g){
        card(112);frame(g,cx,cy,cw,ch,EDGE);
        center(g,"Chromium isn't running",cx+cw/2,cy+10,INK);
        var lines=wrap(failure==null?"Chromium is closed.":failure,cw-20,4);for(int i=0;i<lines.size();i++)center(g,lines.get(i),cx+cw/2,cy+28+i*11,MUTED);
    }
    private void downloadPrompt(GuiGraphics g){
        card(media?146:124);frame(g,cx,cy,cw,ch,EDGE);
        center(g,media?"Enable media playback?":"Download Chromium?",cx+cw/2,cy+12,INK);
        long bytes=session==null?0:session.pendingDownloadBytes();
        String size=Math.round(bytes/1_000_000d)+" MB ("+Math.round(bytes/1_048_576d)+" MiB)";
        var lines=wrap(bytes<0?"Download Google's protected playback component? Its size is determined by Google.":"You're about to download about "+size+" of browser files."+(media?" A protected playback component will also download from Google.":""),cw-24,6);
        for(int i=0;i<lines.size();i++)center(g,lines.get(i),cx+cw/2,cy+34+i*11,INK);
        center(g,"The files are saved for future use.",cx+cw/2,cy+(media?98:76),MUTED);
    }
}
