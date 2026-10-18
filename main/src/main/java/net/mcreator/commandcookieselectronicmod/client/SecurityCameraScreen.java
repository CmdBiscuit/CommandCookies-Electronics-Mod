package net.mcreator.commandcookieselectronicmod.client;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.mcreator.commandcookieselectronicmod.security.SecurityCameraPackets;
import net.mcreator.commandcookieselectronicmod.security.SecurityCameraPackets.*;
import net.neoforged.neoforge.network.PacketDistributor;

/** Computer and portrait phone layouts share the same server-authorized camera list. */
public final class SecurityCameraScreen extends Screen {
 private enum Mode {LIST,VIEW,RENAME}
 private final Screen back;
 private final boolean phone;
 private final BlockPos source;
 private final UUID device,session;
 private final PhonePreferences prefs;
 private final SecurityCameraView view=new SecurityCameraView();
 private List<Feed> cameras=List.of();
 private Feed selected;
 private Mode mode=Mode.LIST;
 private int x,y,w,h,fx,fy,fw,fh,page,zoom=1,ticks,rows;
 private long pending,pendingAt;
 private int pendingAction;
 private static long sequence;
 private String status="Finding cameras...";
 private EditBox name;
 private String draft="";
 private boolean closing,sceneRequested;
 private long sceneRequest,retryAt;
 private static final UUID ZERO=new UUID(0,0);
 public SecurityCameraScreen(IphoneScreen home){super(Component.literal("Security"));back=home;phone=true;source=BlockPos.ZERO;device=home.deviceId()==null?ZERO:home.deviceId();session=UUID.randomUUID();prefs=home.preferences();}
 public SecurityCameraScreen(Screen back,BlockPos source,UUID device,UUID session){super(Component.literal("Security"));this.back=back;this.phone=false;this.source=source.immutable();this.device=device;this.session=session;prefs=new PhonePreferences();}
 public PhonePreferences preferences(){return prefs;}
 public boolean viewActive(){return view.active();}
 public boolean acceptStream(net.mcreator.commandcookieselectronicmod.security.SecurityCameraWire.Start value){return mode==Mode.VIEW&&sceneRequested&&value.request()==sceneRequest&&device.equals(value.device())&&session.equals(value.session())&&selected!=null&&selected.identity().equals(value.camera());}
 public void streamStopped(String reason){endWatch();retryAt=System.nanoTime()+2_000_000_000L;status=reason;if(selected!=null&&reason.equals("Needs batteries"))selected=new Feed(selected.position(),selected.identity(),selected.name(),selected.facing(),false,selected.dimension());}
 private void endWatch(){view.stop();RemoteCameraScene.clear();if(sceneRequested&&minecraft!=null&&minecraft.getConnection()!=null){PacketDistributor.sendToServer(new Request(phone,source,device,session,++sequence,SecurityCameraPackets.STOP,selected==null?BlockPos.ZERO:selected.position(),selected==null?ZERO:selected.identity(),""));}sceneRequested=false;sceneRequest=0;}

 /** Compensate for the centered phone crop without rendering a second world or reading the GPU. */
 public double feedFov(){double angle=2*Math.atan(Math.tan(Math.toRadians(60)/2)/zoom);return Math.toDegrees(phone?2*Math.atan(Math.tan(angle/2)*height/Math.max(1,fh)):angle);}
 private boolean valid(){
  if(minecraft.player==null||minecraft.level==null)return false;
  if(phone)return PhoneMessagesClient.held(device);
  var a=DesktopAccess.at(minecraft.level,source);return a!=null&&device.equals(a.device())&&session.equals(a.session())&&minecraft.player.distanceToSqr(source.getX()+.5,source.getY()+.5,source.getZ()+.5)<=64;
 }
 @Override protected void init(){
  if(phone){h=Math.min(340,height-12);w=Math.min(180,Math.round(h*.54f));x=(width-w)/2;y=(height-h)/2;fw=w-20;fh=Math.max(12,Math.min(fw*3/4,h-178));fx=(width-fw)/2;fy=(height-fh)/2;}
  else {w=Math.min(430,width-32);h=Math.min(320,height-32);x=(width-w)/2;y=(height-h)/2;fx=0;fy=0;fw=width;fh=height;}
  rows=Math.max(1,(h-(phone?140:125))/36);controls();if(pending==0)request(SecurityCameraPackets.SYNC,"");updateView();
 }
 private Button button(int px,int py,int pw,int ph,String label,int color,Runnable action){return addRenderableWidget(new PhonePhotoButton(px,py,pw,ph,label,color,action));}
 private void controls(){
  clearWidgets();name=null;
  int left=phone?x+10:x+16,available=w-(phone?20:32),foot=y+h-32;
  if(mode==Mode.LIST){
   int count=cameras.size(),pages=Math.max(1,(count+rows-1)/rows);page=Math.max(0,Math.min(page,pages-1));
   for(int i=page*rows;i<Math.min(count,(page+1)*rows);i++){
    Feed f=cameras.get(i);int yy=y+(phone?67:56)+(i-page*rows)*36;
    button(left,yy,available,31,(f.powered()?"● ":"○ ")+fit(f.name(),available-14),prefs.card(),()->select(f));
   }
   int nav=y+h-70;
   button(left,nav,30,20,"<",prefs.card(),()->{page--;controls();}).active=page>0;
   button(left+available-30,nav,30,20,">",prefs.card(),()->{page++;controls();}).active=page+1<pages;
   button(left,foot,available,22,phone?"Home":"Desktop",prefs.accentColor(),this::onClose);
  }else if(mode==Mode.VIEW){
   int zy=phone?fy+fh+7:height-34;
   int zx=phone?x+10:width/2-60;
   for(int i=0;i<3;i++){int value=1<<i;button(zx+i*(phone?(available+3)/3:42),zy,phone?(available-6)/3:38,19,value+"x",zoom==value?prefs.accentColor():prefs.card(),()->{zoom=value;controls();});}
   int by=phone?y+h-59:12;
   button(phone?left:12,by,phone?(available-5)/2:80,22,"Cameras",prefs.card(),()->{mode=Mode.LIST;endWatch();controls();});
   button(phone?left+(available+5)/2:width-92,by,phone?(available-5)/2:80,22,"Rename",prefs.card(),()->{draft=selected==null?"":selected.name();mode=Mode.RENAME;endWatch();controls();});
   button(phone?left:12,phone?foot:height-34,phone?available:80,22,phone?"Home":"Desktop",prefs.accentColor(),this::onClose);
  }else{
   name=addRenderableWidget(new EditBox(font,left,y+91,available,22,Component.literal("Camera name")));name.setMaxLength(32);name.setValue(draft);name.setResponder(v->draft=v);setInitialFocus(name);
   button(left,y+129,(available-5)/2,22,"Cancel",prefs.card(),()->{mode=Mode.VIEW;controls();updateView();});
   button(left+(available+5)/2,y+129,(available-5)/2,22,"Save",prefs.accentColor(),()->request(SecurityCameraPackets.RENAME,draft)).active=pending==0;
   button(left,foot,available,22,phone?"Home":"Desktop",prefs.card(),this::onClose);
  }
 }
 public void select(Feed feed){endWatch();retryAt=0;selected=feed;zoom=1;mode=Mode.VIEW;status="Connecting to camera...";controls();updateView();}
 private void updateView(){if(mode==Mode.VIEW&&selected!=null&&selected.powered()){if(!sceneRequested&&pending==0&&System.nanoTime()>=retryAt)request(SecurityCameraPackets.VIEW,"");view.show(selected);}else if(sceneRequested)endWatch();else view.stop();}
 private void request(int action,String text){
  if(pending!=0||!valid()||minecraft.getConnection()==null)return;
  pending=++sequence;pendingAt=System.nanoTime();pendingAction=action;if(action==SecurityCameraPackets.VIEW){sceneRequested=true;sceneRequest=pending;}
  PacketDistributor.sendToServer(new Request(phone,source,device,session,pending,action,selected==null?BlockPos.ZERO:selected.position(),selected==null?ZERO:selected.identity(),text));
  if(action==SecurityCameraPackets.RENAME)controls();
 }
 public void response(Snapshot value){
  if(value.phone()!=phone||!value.source().equals(source)||!value.device().equals(device)||!value.session().equals(session)||value.request()!=pending)return;
  int action=pendingAction;pending=0;status=value.status();
  if(value.success()){
   cameras=value.cameras();if(selected!=null){UUID id=selected.identity();selected=cameras.stream().filter(c->c.identity().equals(id)).findFirst().orElse(null);if(selected==null){mode=Mode.LIST;status="Camera was removed.";}}
   if(action==SecurityCameraPackets.RENAME)mode=Mode.VIEW;
  }else{cameras=List.of();selected=null;mode=Mode.LIST;}
  if(mode!=Mode.RENAME)controls();updateView();
 }
 @Override public void tick(){
  if(!valid()){closing=true;minecraft.setScreen(null);return;}
  if(pending!=0&&System.nanoTime()-pendingAt>5_000_000_000L){pending=0;status="Waiting for the server...";if(mode==Mode.RENAME)controls();}
  if(++ticks%20==0&&pending==0&&mode!=Mode.RENAME)request(SecurityCameraPackets.SYNC,"");
  updateView();
 }
 private String fit(String text,int max){return font.width(text)<=max?text:font.plainSubstrByWidth(text,Math.max(0,max-font.width("...")))+"...";}
 private void text(GuiGraphics g,String text,int px,int py,int color){g.drawString(font,fit(text,w-20),px,py,color,false);}
 @Override public void renderBackground(GuiGraphics g,int mx,int my,float delta){}
 @Override public void render(GuiGraphics g,int mx,int my,float delta){
  if(phone){
   if(mode==Mode.VIEW&&view.active()){
    g.fill(0,0,width,fy,0xFF0D1420);g.fill(0,fy+fh,width,height,0xFF0D1420);g.fill(0,fy,fx,fy+fh,0xFF0D1420);g.fill(fx+fw,fy,width,fy+fh,0xFF0D1420);
    g.fill(x,y,x+w,fy,prefs.panel());g.fill(x,fy+fh,x+w,y+h,prefs.panel());g.fill(x,fy,fx,fy+fh,prefs.panel());g.fill(fx+fw,fy,x+w,fy+fh,prefs.panel());
   }else{g.fill(0,0,width,height,0xFF0D1420);g.fill(x,y,x+w,y+h,prefs.panel());}
   g.renderOutline(x-1,y-1,w+2,h+2,0xFF4C5B68);
   text(g,LocalTime.now().format(DateTimeFormatter.ofPattern(prefs.clock24?"HH:mm":"h:mm")),x+10,y+10,prefs.muted());g.fill(x+w-24,y+11,x+w-10,y+17,prefs.muted());g.fill(x+w/2-15,y+7,x+w/2+15,y+13,0xFF0B1018);
   text(g,"Security",x+10,y+31,prefs.ink());g.fill(x+w/2-18,y+h-9,x+w/2+18,y+h-7,prefs.ink());
  }else if(mode==Mode.VIEW){g.fill(0,0,width,43,0xDF14222F);g.fill(0,height-45,width,height,0xDF14222F);}
  else{g.fill(0,0,width,height,0xE40D1420);g.fill(x,y,x+w,y+h,prefs.panel());g.renderOutline(x,y,w,h,0xFF567181);text(g,"Security cameras",x+16,y+16,prefs.ink());}
  if(mode==Mode.LIST){
   text(g,"Your cameras",x+(phone?10:16),y+(phone?49:34),prefs.muted());
   if(cameras.isEmpty()){text(g,"No cameras found.",x+10,y+83,prefs.ink());text(g,phone?"Pair a camera to this phone.":"Mount your camera nearby.",x+10,y+101,prefs.muted());text(g,"Insert 2 AA batteries.",x+10,y+116,prefs.muted());}
   String pageText=(page+1)+" / "+Math.max(1,(cameras.size()+rows-1)/rows);g.drawCenteredString(font,pageText,x+w/2,y+h-64,prefs.muted());
   if(!status.isEmpty())text(g,status,x+10,y+h-85,prefs.muted());
  }else if(mode==Mode.VIEW){
   String title=selected==null?"Camera unavailable":selected.name();
   if(phone)text(g,title,x+10,fy-17,prefs.ink());else g.drawCenteredString(font,title,width/2,17,prefs.ink());
   if(phone){g.renderOutline(fx-1,fy-1,fw+2,fh+2,0xFF617C8A);if(view.active()){g.fill(fx+6,fy+6,fx+10,fy+10,0xFF74D7A7);g.drawString(font,"LIVE",fx+14,fy+4,0xFFFFFFFF,false);}}
   else if(view.active())g.drawString(font,"● LIVE",width/2-24,height-62,0xFF8DE7B7,true);
   if(!view.active()){
    if(phone)g.fill(fx,fy,fx+fw,fy+fh,0xFF0C151F);else g.fill(0,43,width,height-45,0xFF0C151F);
    String message=selected!=null&&!selected.powered()?"Needs batteries":sceneRequested?"Connecting...":"No signal";g.drawCenteredString(font,message,width/2,height/2-5,0xFFBACBD1);
   }
   if(!status.isEmpty())text(g,status,phone?x+10:110,phone?y+51:height-30,prefs.muted());
  }else{text(g,"Name this camera",x+10,y+63,prefs.ink());if(!status.isEmpty())text(g,status,x+10,y+165,prefs.muted());}
  super.render(g,mx,my,delta);
 }
 @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){if(mode==Mode.VIEW&&vertical!=0){zoom=vertical>0?Math.min(4,zoom*2):Math.max(1,zoom/2);controls();return true;}return super.mouseScrolled(mx,my,horizontal,vertical);}
 @Override public void removed(){endWatch();pending=0;}
 @Override public void onClose(){minecraft.setScreen(closing?null:back);}
 @Override public boolean isPauseScreen(){return false;}
}
