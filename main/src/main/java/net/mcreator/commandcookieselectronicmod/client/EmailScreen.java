package net.mcreator.commandcookieselectronicmod.client;

import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.mcreator.commandcookieselectronicmod.email.EmailData;
import net.mcreator.commandcookieselectronicmod.network.EmailPackets;
import org.lwjgl.glfw.GLFW;

/** BlockOS mail UI. Its player account is shared across that player's computers in this world. */
public final class EmailScreen extends Screen {
    private static final int W=460,H=270,INK=0xFFE9F2F4,MUTED=0xFF9BAFB9,EDGE=0xFF526574;
    private final LaptopDesktopScreen home;private final BlockPos pos;private final UUID device,session;
    private float scale,left,top;private String view="inbox",status="",draftTo="",draftSubject="",draftBody="",signup="";
    private int page,scroll,ticks;private UUID selected;private boolean outgoing,deleteConfirm,refresh;
    private long version=-1,pending,pendingAt;private int pendingAction;private boolean ready;
    private final LaptopPhotoLibrary gallery;private final EmailPhotoView pictures=new EmailPhotoView();
    private PhonePhotos.Photo draftPhoto;private byte[] draftImage=new byte[0];private boolean preparing,saving;private int galleryCount=-1,revision;
    private EditBox to,subject,name;private MultiLineEditBox body;private Button send;
    public EmailScreen(LaptopDesktopScreen home,BlockPos pos,UUID device,UUID session){super(Component.literal("Email"));this.home=home;this.pos=pos.immutable();this.device=device;this.session=session;gallery=new LaptopPhotoLibrary(home.preferences());}
    public boolean matches(EmailPackets.Snapshot s){return s.position().equals(pos)&&s.device().equals(device)&&s.session().equals(session);}
    @Override public boolean isPauseScreen(){return false;}
    @Override protected void init(){scale=Math.min((width-12f)/W,(height-12f)/H);left=(width-W*scale)/2;top=(height-H*scale)/2;controls();sync();gallery.poll();}
    private int accent(){return home.preferences().accentColor();}
    private void text(GuiGraphics g,String s,int x,int y,int color){g.drawString(font,s,x,y,color,false);}
    private String fit(String s,int width){if(font.width(s)<=width)return s;return font.plainSubstrByWidth(s,Math.max(0,width-font.width("...")))+"...";}
    private void center(GuiGraphics g,String s,int x,int y,int color){text(g,s,x-font.width(s)/2,y,color);}
    private final class MailButton extends Button {
        MailButton(String s,int x,int y,int w,Runnable action){super(x,y,w,20,Component.literal(s),b->action.run(),Button.DEFAULT_NARRATION);}
        @Override public void renderWidget(GuiGraphics g,int mx,int my,float partial){g.fill(getX(),getY(),getX()+width,getY()+height,active&&isHoveredOrFocused()?0xFF3A5261:0xFF293D4B);g.renderOutline(getX(),getY(),width,height,active&&isHoveredOrFocused()?accent():0xFF49616E);center(g,getMessage().getString(),getX()+width/2,getY()+6,active?INK:MUTED);}
    }
    private Button button(String s,int x,int y,int w,Runnable action){return addRenderableWidget(new MailButton(s,x,y,w,action));}
    private EditBox edit(String label,int x,int y,int w,int max,String value){var b=new EditBox(font,x,y,w,20,Component.literal(label));b.setMaxLength(max);b.setValue(value);b.setHint(Component.literal(label));addRenderableWidget(b);return b;}
    private void controls(){
        clearWidgets();to=null;subject=null;body=null;name=null;send=null;
        button("Desktop",386,8,64,this::onClose);
        if(!ready)return;
        if(EmailClient.address.isEmpty()){
            name=edit("heythere123",114,131,232,42,signup);name.setResponder(s->signup=s);send=button("Create address",172,165,116,()->submit(EmailPackets.CREATE,signup,"","",null,false));send.active=pending==0;return;
        }
        if(!view.equals("photo")){button("Inbox"+(EmailClient.unread>0?" ("+EmailClient.unread+")":""),12,49,88,()->list(false));
        button("Sent",12,76,88,()->list(true));button("Compose",12,108,88,()->compose("","",""));}
        if(view.equals("compose")){
            to=edit("name@email.com",158,48,280,42,draftTo);to.setResponder(s->draftTo=s);
            subject=edit("Subject",158,75,280,80,draftSubject);subject.setResponder(s->draftSubject=s);
            body=addRenderableWidget(new MailBody(114,119,324,66));body.setCharacterLimit(2048);body.setValue(draftBody);body.setValueListener(s->draftBody=s);
            send=button("Send",114,224,78,()->submit(EmailPackets.SEND,draftTo,draftSubject,draftBody,null,false));send.active=pending==0&&!preparing;
            var attach=button(preparing?"Preparing...":"Attach photo",114,192,100,()->{if(pending==0&&!preparing){view="gallery";page=0;status="Choose one photo from your phone Gallery.";gallery.refresh();controls();}});attach.active=pending==0&&!preparing;
            if(draftPhoto!=null){var remove=button("Remove",376,192,62,()->{draftPhoto=null;draftImage=new byte[0];status="Photo removed.";controls();});remove.active=pending==0&&!preparing;}
            var cancel=button("Cancel",198,224,74,()->list(false));cancel.active=pending==0&&!preparing;
        }else if(view.equals("gallery")){
            var all=gallery.photos();int pages=Math.max(1,(all.size()+5)/6);page=Math.min(page,pages-1);
            for(int i=0;i<6&&page*6+i<all.size();i++){var photo=all.get(page*6+i);int col=i%3,row=i/3;button("Photo "+(page*6+i+1),114+col*108,114+row*74,100,()->attach(photo));}
            button("Back to draft",114,224,108,()->{view="compose";status="";controls();});button("Refresh",228,224,74,gallery::refresh);
            if(pages>1){button("<",338,224,30,()->{page=Math.floorMod(page-1,pages);controls();});button(">",408,224,30,()->{page=(page+1)%pages;controls();});}
        }else if(view.equals("photo")){
            var e=current();if(e==null){list(outgoing);return;}
            button("Back to email",114,224,112,()->{view="read";status="";controls();});
            var save=button(saving?"Saving...":"Save to Gallery",232,224,138,()->savePhoto(e));save.active=pictures.loaded(e.id())&&!saving;
            button("Retry",376,224,62,()->pictures.retry(pos,device,session,e));
        }else if(view.equals("read")){
            var e=current();if(e==null){view=outgoing?"sent":"inbox";controls();return;}
            if(e.hasPhoto())button("View photo",114,198,116,()->{view="photo";status="";pictures.request(pos,device,session,e);controls();});
            if(!e.outgoing())button("Reply",114,224,72,()->compose(e.from(),e.subject().regionMatches(true,0,"Re:",0,3)?e.subject():"Re: "+e.subject(),""));
            button("Back",193,224,72,()->list(outgoing));var del=button(deleteConfirm?"Confirm delete":"Delete",272,224,116,()->{if(deleteConfirm)submit(EmailPackets.DELETE,"","","",e.id(),e.outgoing());else{deleteConfirm=true;controls();}});del.active=pending==0;
        }else{
            var all=listEntries();int pages=Math.max(1,(all.size()+5)/6);page=Math.min(page,pages-1);
            for(int i=0;i<6&&page*6+i<all.size();i++){var e=all.get(page*6+i);final int yy=74+i*24;button((!e.read()&&!e.outgoing()?"* ":"")+fit(e.outgoing()?e.to():e.from(),100)+" | "+fit(e.subject(),e.hasPhoto()?156:180)+(e.hasPhoto()?" [photo]":""),114,yy,324,()->open(e));}
            if(pages>1){button("<",114,224,30,()->{page=Math.floorMod(page-1,pages);controls();});button(">",408,224,30,()->{page=(page+1)%pages;controls();});}
        }
    }
    private final class MailBody extends MultiLineEditBox {
        MailBody(int x,int y,int w,int h){super(EmailScreen.this.font,x,y,w,h,Component.literal("Write your email..."),Component.literal("Message"));}
        @Override public void renderWidget(GuiGraphics g,int mx,int my,float partial){
            if(!visible)return;g.fill(getX(),getY(),getX()+getWidth(),getY()+getHeight(),0xFF10232D);g.renderOutline(getX(),getY(),getWidth(),getHeight(),isFocused()?accent():EDGE);g.flush();
            g.enableScissor((int)Math.floor(left+(getX()+1)*scale),(int)Math.floor(top+(getY()+1)*scale),(int)Math.ceil(left+(getX()+getWidth()-1)*scale),(int)Math.ceil(top+(getY()+getHeight()-1)*scale));
            g.pose().pushPose();g.pose().translate(0,-scrollAmount(),0);renderContents(g,mx,my,partial);g.pose().popPose();g.flush();g.disableScissor();
            if(scrollbarVisible()){int thumb=Math.max(12,getHeight()*getHeight()/Math.max(1,getInnerHeight()+4));int offset=(int)((getHeight()-thumb)*scrollAmount()/Math.max(1,getMaxScrollAmount()));g.fill(getX()+getWidth()+2,getY()+offset,getX()+getWidth()+6,getY()+offset+thumb,MUTED);}
        }
    }
    private void list(boolean sent){if(pending!=0||preparing)return;view=sent?"sent":"inbox";outgoing=sent;selected=null;page=0;scroll=0;deleteConfirm=false;status="";controls();}
    private void compose(String recipient,String title,String message){if(pending!=0||preparing)return;view="compose";draftPhoto=null;draftImage=new byte[0];draftTo=recipient;draftSubject=title.substring(0,Math.min(80,title.length()));draftBody=message;status="";deleteConfirm=false;controls();setFocused(to);}
    private void attach(PhonePhotos.Photo photo){
        if(preparing||pending!=0)return;preparing=true;view="compose";status="Preparing photo...";controls();int generation=revision;
        PhonePhotos.emailAttachment(photo).whenComplete((bytes,ex)->minecraft.execute(()->{
            if(!gallery.active()||generation!=revision)return;preparing=false;
            if(ex==null){draftPhoto=photo;draftImage=bytes;status="Photo attached. Send shares it with your recipient.";}else status="Could not prepare this photo. Try another.";
            controls();
        }));
    }
    public void photo(EmailPackets.Photo p){if(p.position().equals(pos)&&p.device().equals(device)&&p.session().equals(session))pictures.accept(p);}
    private void savePhoto(EmailData.Entry e){if(saving||!pictures.loaded(e.id()))return;saving=true;controls();pictures.save(e.id()).whenComplete((photo,ex)->minecraft.execute(()->{if(!gallery.active())return;saving=false;status=ex==null?"Photo saved to your phone and laptop Gallery.":"Could not save the photo. Try again.";if(ex==null)gallery.refresh();controls();}));}
    @Override public void removed(){revision++;gallery.release();pictures.release();}
    private List<EmailData.Entry> listEntries(){return EmailClient.messages.stream().filter(e->e.outgoing()==view.equals("sent")).sorted(Comparator.comparingLong(EmailData.Entry::time).reversed()).toList();}
    private EmailData.Entry current(){return EmailClient.messages.stream().filter(e->e.id().equals(selected)&&e.outgoing()==outgoing).findFirst().orElse(null);}
    private void open(EmailData.Entry e){selected=e.id();outgoing=e.outgoing();view="read";scroll=0;deleteConfirm=false;status="";controls();if(!e.outgoing()&&!e.read())submit(EmailPackets.READ,"","","",e.id(),false);}
    private void sync(){EmailClient.request(pos,device,session,EmailPackets.SYNC,"","","",null,false);}
    public void refreshSoon(){refresh=true;}
    private void submit(int action,String address,String title,String text,UUID id,boolean outgoing){if(pending!=0||preparing)return;pending=EmailClient.request(pos,device,session,action,address,title,text,id,outgoing,action==EmailPackets.SEND?draftImage:new byte[0]);pendingAction=action;pendingAt=System.nanoTime();status=pending==0?"Not connected to the world.":action==EmailPackets.SEND?"Sending...":"Updating...";if(send!=null)send.active=false;}
    public void response(EmailPackets.Snapshot s){
        if(s.success())ready=true;
        if(s.request()==pending&&pending!=0){pending=0;status=s.status();if(s.success()){if(pendingAction==EmailPackets.SEND){draftTo="";draftSubject="";draftBody="";draftPhoto=null;draftImage=new byte[0];view="sent";selected=null;page=0;}else if(pendingAction==EmailPackets.DELETE){view=outgoing?"sent":"inbox";selected=null;deleteConfirm=false;}}controls();}
        else if(!ready&&!s.success())status=s.status();
    }
    @Override public void tick(){
        var a=DesktopAccess.at(minecraft.level,pos);if(minecraft.player==null||a==null||!device.equals(a.device())||!session.equals(a.session())||minecraft.player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64){minecraft.setScreen(null);return;}
        if(view.equals("gallery")){gallery.poll();int count=gallery.photos().size();if(galleryCount!=count){galleryCount=count;controls();}}
        if(++ticks%100==0||refresh&&ticks%15==0){refresh=false;sync();}
        if(version!=EmailClient.version){version=EmailClient.version;controls();}
        if(pending!=0&&System.nanoTime()-pendingAt>8_000_000_000L){pending=0;status="No confirmation. Check Sent before trying again.";controls();}
        if(send!=null)send.active=pending==0&&!preparing;
        if(view.equals("photo"))for(var child:children())if(child instanceof Button b&&b.getMessage().getString().equals("Save to Gallery"))b.active=selected!=null&&pictures.loaded(selected)&&!saving;
    }
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float partial){g.fill(0,0,width,height,0xB0081018);}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g,mx,my,partial);int lx=(int)((mx-left)/scale),ly=(int)((my-top)/scale);g.pose().pushPose();g.pose().translate(left,top,0);g.pose().scale(scale,scale,1);
        g.fill(-4,-4,W+4,H+4,0xFF0A111A);g.renderOutline(-4,-4,W+8,H+8,EDGE);g.fill(0,0,W,H,0xFF182C39);g.fill(0,0,W,37,0xFF324956);LaptopAppIcons.draw(g,22,12,9,20);text(g,"Email",39,15,INK);text(g,fit(EmailClient.address,267),106,15,INK);
        if(!ready){center(g,"Opening your mailbox...",W/2,118,INK);}
        else if(EmailClient.address.isEmpty()){
            center(g,"Create your in-game email address",W/2,70,INK);center(g,"One private mailbox for you, on every computer.",W/2,91,MUTED);center(g,"Choose a name (or enter name@email.com)",W/2,114,MUTED);center(g,"Mail stays in this Minecraft world.",W/2,202,MUTED);
        }else{
            g.fill(106,44,448,247,0xFF10232D);if(!view.equals("photo")){text(g,"WORLD MAIL",17,148,MUTED);text(g,"Private inbox",17,165,MUTED);text(g,"Online +",17,193,MUTED);text(g,"offline delivery",17,204,MUTED);}
            if(view.equals("compose")){text(g,"To",114,54,MUTED);text(g,"Subject",114,81,MUTED);text(g,"Message",114,106,MUTED);text(g,draftBody.length()+" / 2048",349,106,MUTED);if(draftPhoto!=null){gallery.draw(g,draftPhoto,220,190,36,24,false,false);text(g,"1 photo attached",262,198,MUTED);}}
            else if(view.equals("gallery")){
                text(g,"Attach a photo",114,51,INK);var all=gallery.photos();
                if(all.isEmpty()){center(g,gallery.loaded()?"No photos yet. Take one with your phone.":"Loading Gallery...",276,119,MUTED);}
                for(int i=0;i<6&&page*6+i<all.size();i++)gallery.draw(g,all.get(page*6+i),114+i%3*108,64+i/3*74,100,48,false,false);
                if(all.size()>6)center(g,(page+1)+" / "+Math.max(1,(all.size()+5)/6),389,231,MUTED);
            }
            else if(view.equals("photo")){var e=current();if(e!=null){g.fill(12,44,448,217,0xFF10232D);pictures.draw(g,e.id(),18,47,424,165);}}
            else if(view.equals("read")){
                var e=current();if(e!=null){text(g,fit(e.subject(),318),114,52,INK);text(g,"From: "+fit(e.from(),276),114,69,MUTED);text(g,"To: "+fit(e.to(),291),114,84,MUTED);text(g,DateTimeFormatter.ofPattern("MMM d, yyyy  HH:mm").format(Instant.ofEpochMilli(e.time()).atZone(ZoneId.systemDefault())),114,99,MUTED);
                    var lines=font.split(Component.literal(e.body()),312);int rows=e.hasPhoto()?7:9;scroll=Math.max(0,Math.min(scroll,Math.max(0,lines.size()-rows)));for(int i=0;i<rows&&scroll+i<lines.size();i++)g.drawString(font,lines.get(scroll+i),114,118+i*11,INK,false);if(e.hasPhoto())text(g,e.photoWidth()+" x "+e.photoHeight()+" photo",241,204,MUTED);if(lines.size()>rows)text(g,(scroll+1)+"-"+Math.min(lines.size(),scroll+rows)+" / "+lines.size(),350,212,MUTED);
                }
            }else{
                var entries=listEntries();text(g,view.equals("sent")?"Sent emails":"Inbox",114,53,INK);text(g,entries.size()+" emails",375,53,MUTED);if(entries.isEmpty()){center(g,view.equals("sent")?"No sent emails yet.":"Your inbox is empty.",276,123,MUTED);center(g,"Use Compose to send an email.",276,143,MUTED);}else if(entries.size()>6)center(g,(page+1)+" / "+Math.max(1,(entries.size()+5)/6),276,230,MUTED);
            }
        }
        text(g,fit(status,438),12,255,MUTED);for(var child:children())if(child instanceof Renderable r)r.render(g,lx,ly,partial);g.pose().popPose();
    }
    @Override public boolean mouseClicked(double x,double y,int b){return super.mouseClicked((x-left)/scale,(y-top)/scale,b);}
    @Override public boolean mouseReleased(double x,double y,int b){return super.mouseReleased((x-left)/scale,(y-top)/scale,b);}
    @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy){return super.mouseDragged((x-left)/scale,(y-top)/scale,b,dx/scale,dy/scale);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(view.equals("read")){scroll=Math.max(0,scroll-(int)(vertical*3));return true;}return super.mouseScrolled((x-left)/scale,(y-top)/scale,horizontal,vertical);}
    @Override public boolean keyPressed(int key,int scan,int mods){if(key==GLFW.GLFW_KEY_ESCAPE){if(view.equals("photo")){view="read";controls();return true;}if(view.equals("gallery")){view="compose";controls();return true;}if(view.equals("compose")||view.equals("read")){list(outgoing);return true;}onClose();return true;}return super.keyPressed(key,scan,mods);}
    @Override public void onClose(){if(pending!=0||preparing)return;minecraft.setScreen(home);}
}
