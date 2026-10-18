package net.mcreator.commandcookieselectronicmod.client;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.mcreator.commandcookieselectronicmod.network.PhoneMessagePackets;
import org.lwjgl.glfw.GLFW;

/** Portrait Messages app shared by all iPhone and Samsung colors. */
public final class PhoneMessagesScreen extends Screen {
    private final IphoneScreen home;
    private final UUID device;
    private final PhonePreferences prefs;
    private UUID peer,draftPeer,pendingPeer;
    private String peerName="",draft="",status="",sentDraft="";
    private boolean players;
    private int x,y,w,h,cx,cw,page,scroll,ticks;
    private long version=-1,pending,pendingAt;
    private EditBox input;
    private Button sendButton;
    private record Person(UUID id,String name,int unread,String preview){}
    private List<Person> peopleCache=List.of();
    private long peopleVersion=-1,bubbleVersion=-1;
    private boolean cachedPlayers,cached24;
    private int bubbleWidth,totalHeight;
    private float cachedScale;
    private UUID bubblePeer;
    private record Bubble(List<FormattedCharSequence> lines,boolean outgoing,String time,int height){}
    private List<Bubble> bubbles=List.of();
    private final Map<UUID,PlayerInfo> playerSkins=new LinkedHashMap<>();

    public PhoneMessagesScreen(IphoneScreen home){super(Component.literal("Messages"));this.home=home;device=home.deviceId();prefs=home.preferences();}
    public PhonePreferences preferences(){return prefs;}
    public boolean viewing(UUID device,UUID peer){return this.peer!=null&&Objects.equals(this.device,device)&&Objects.equals(this.peer,peer);}
    @Override protected void init(){h=Math.min(340,height-12);w=Math.min(180,Math.round(h*.54f));x=(width-w)/2;y=(height-h)/2;cx=x+10;cw=w-20;controls();PhoneMessagesClient.sync(device);}
    private PhoneMessagesClient.Model model(){return PhoneMessagesClient.model(device);}
    private boolean online(UUID id){return model().contacts.stream().anyMatch(c->c.player().equals(id));}
    private List<Person> people(){
        if(peopleVersion==model().version&&cachedPlayers==players)return peopleCache;
        var map=new LinkedHashMap<UUID,Person>();
        if(!players){var messages=model().messages;for(int i=messages.size()-1;i>=0;i--){var e=messages.get(i);if(!map.containsKey(e.peer()))map.put(e.peer(),new Person(e.peer(),e.name(),(int)messages.stream().filter(m->m.peer().equals(e.peer())&&!m.outgoing()&&!m.read()).count(),(e.outgoing()?"You: ":"")+e.text()));}}
        for(var c:model().contacts)map.putIfAbsent(c.player(),new Person(c.player(),c.name(),0,"Online"));
        peopleVersion=model().version;cachedPlayers=players;peopleCache=List.copyOf(map.values());return peopleCache;
    }
    private Button button(int px,int py,int pw,int ph,String text,int color,Runnable action){return addRenderableWidget(new PhonePhotoButton(px,py,pw,ph,text,color,action));}
    private PlayerSkin skin(UUID id){
        var connection=minecraft.getConnection();var info=connection==null?null:connection.getPlayerInfo(id);
        if(info!=null&&playerSkins.get(id)!=info){playerSkins.put(id,info);if(playerSkins.size()>256)playerSkins.remove(playerSkins.keySet().iterator().next());}
        var cached=playerSkins.get(id);return cached==null?DefaultPlayerSkin.get(id):cached.getSkin();
    }
    private String fitName(String name,int maxWidth){
        if(font.width(name)<=maxWidth)return name;
        return font.plainSubstrByWidth(name,Math.max(0,maxWidth-font.width("...")))+"...";
    }
    private void text(GuiGraphics g,FormattedCharSequence text,int px,int py,int color,float scale){
        g.pose().pushPose();g.pose().translate(px,py,0);g.pose().scale(scale,scale,1);g.drawString(font,text,0,0,color,false);g.pose().popPose();
    }
    private void text(GuiGraphics g,String text,int px,int py,int color,float scale){text(g,Component.literal(text).getVisualOrderText(),px,py,color,scale);}
    private final class ContactButton extends Button {
        private final Person person;
        ContactButton(int px,int py,int pw,Person person){super(px,py,pw,34,Component.literal((person.unread()>0?"* ":"")+person.name()),b->openConversation(person),DEFAULT_NARRATION);this.person=person;}
        @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager){PhoneWallpapers.click(manager,prefs);}
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float tick){
            int px=getX(),py=getY(),pw=getWidth();
            g.fill(px+2,py,px+pw-2,py+34,prefs.card());g.fill(px,py+2,px+pw,py+32,prefs.card());
            if(isHoveredOrFocused())g.renderOutline(px,py,pw,34,prefs.accentColor());
            PlayerFaceRenderer.draw(g,skin(person.id()),px+5,py+7,20);
            float scale=prefs.textScale();
            text(g,fitName(getMessage().getString(),(int)((pw-37)/scale)),px+31,py+6,prefs.ink(),scale);
            text(g,fitName(person.preview(),(int)((pw-37)/(.8f*scale))),px+31,py+21,prefs.muted(),.8f*scale);
        }
    }
    private void openConversation(Person person){
        if(!Objects.equals(draftPeer,person.id())){draft="";draftPeer=person.id();}
        peer=person.id();peerName=person.name();scroll=0;status="";input=null;controls();PhoneMessagesClient.request(device,PhoneMessagePackets.READ,peer,"");
    }
    private void back(){peer=null;input=null;scroll=0;controls();}
    private void goHome(){minecraft.setScreen(home);}
    private void controls(){
        int cursor=input==null?draft.length():input.getCursorPosition();boolean focus=input==null||input.isFocused();
        if(input!=null)draft=input.getValue();setFocused(null);clearWidgets();input=null;sendButton=null;
        button(x+9,y+30,19,17,"<",prefs.card(),peer==null?this::goHome:this::back);
        button(x+w/2-28,y+h-20,56,14,"Home",prefs.card(),this::goHome);
        if(peer==null){
            int tab=(w-23)/2;
            button(x+10,y+53,tab,18,"Inbox",!players?prefs.accentColor():prefs.card(),()->{players=false;page=0;controls();});
            button(x+13+tab,y+53,tab,18,"Players",players?prefs.accentColor():prefs.card(),()->{players=true;page=0;controls();});
            var people=people();int rows=Math.max(1,(h-126)/38),pages=Math.max(1,(people.size()+rows-1)/rows);page=Math.min(page,pages-1);
            for(int i=0;i<rows&&page*rows+i<people.size();i++){var person=people.get(page*rows+i);var b=addRenderableWidget(new ContactButton(x+9,y+78+i*38,w-18,person));b.setTooltip(Tooltip.create(Component.literal(person.name()+"\n"+person.preview())));}
            if(pages>1){button(x+10,y+h-47,25,17,"<",prefs.card(),()->{page=Math.floorMod(page-1,pages);controls();});button(x+w-35,y+h-47,25,17,">",prefs.card(),()->{page=(page+1)%pages;controls();});}
        }else{
            input=addRenderableWidget(new EditBox(font,cx,y+h-60,cw-40,20,Component.literal("Message")));
            input.setMaxLength(256);input.setHint(Component.literal("Message..."));input.setValue(draft);input.setResponder(value->draft=value);input.setCursorPosition(Math.min(cursor,draft.length()));if(focus)setFocused(input);
            sendButton=button(cx+cw-36,y+h-60,36,20,"Send",prefs.accentColor(),this::send);
        }
    }
    private void send(){
        if(input==null||input.getValue().isBlank()||pending!=0)return;
        if(!PhoneMessagesClient.held(device)){status="Hold this phone to send.";return;}if(!online(peer)){status="That player is offline.";return;}
        sentDraft=input.getValue();pendingPeer=peer;pending=PhoneMessagesClient.request(device,PhoneMessagePackets.SEND,peer,sentDraft);pendingAt=System.nanoTime();if(pending!=0)status="Sending...";
    }
    public void response(PhoneMessagePackets.Snapshot snapshot){
        if(!Objects.equals(device,snapshot.device()))return;
        if(pending!=0&&pending==snapshot.request()){
            status=snapshot.status();pending=0;
            if(snapshot.success()&&Objects.equals(draftPeer,pendingPeer)&&draft.equals(sentDraft)){draft="";if(input!=null)input.setValue("");scroll=0;}
        }
    }
    @Override public void tick(){
        super.tick();if(++ticks%100==0)PhoneMessagesClient.sync(device);
        if(pending!=0&&System.nanoTime()-pendingAt>15_000_000_000L){pending=0;status="No response. Try again.";}
        if(model().version!=version){version=model().version;controls();if(peer!=null&&model().messages.stream().anyMatch(e->e.peer().equals(peer)&&!e.outgoing()&&!e.read()))PhoneMessagesClient.request(device,PhoneMessagePackets.READ,peer,"");}
        if(sendButton!=null)sendButton.active=pending==0&&peer!=null&&online(peer)&&PhoneMessagesClient.held(device);
    }
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float tick){
        home.renderAppFrame(g,x,y,w,h);float scale=prefs.textScale();
        String title=fitName("Messages",(int)((w-58)/scale));text(g,title,x+(w-Math.round(font.width(title)*scale))/2,y+34,prefs.ink(),scale);
        if(peer==null){
            if(people().isEmpty())g.drawWordWrap(font,Component.literal("No players with phones online. They need a phone in their inventory."),cx,y+95,cw,prefs.muted());
            text(g,fitName(status,(int)(cw/.8f)),cx,y+h-62,prefs.muted(),.8f);return;
        }
        PlayerFaceRenderer.draw(g,skin(peer),cx,y+56,20);
        text(g,fitName(peerName,(int)((cw-28)/scale)),cx+28,y+57,prefs.ink(),scale);
        text(g,online(peer)?"Online":"Offline",cx+28,y+71,online(peer)?0xFF73BE91:prefs.muted(),.8f);
        g.fill(cx,y+81,cx+cw,y+82,prefs.card());drawMessages(g);
        String line=PhoneMessagesClient.held(device)?status:"Hold this phone to message.";text(g,fitName(line,(int)(cw/.8f)),cx,y+h-35,prefs.muted(),.8f);
    }
    private void drawMessages(GuiGraphics g){
        float scale=prefs.textScale();
        if(bubbleVersion!=model().version||!Objects.equals(bubblePeer,peer)||bubbleWidth!=cw||cached24!=prefs.clock24||cachedScale!=scale){
            var list=new ArrayList<Bubble>();totalHeight=0;var format=DateTimeFormatter.ofPattern(prefs.clock24?"HH:mm":"h:mm a");
            for(var e:model().messages)if(e.peer().equals(peer)){var lines=font.split(Component.literal(e.text()),Math.max(24,(int)((cw-28)/scale)));int bh=Math.round(lines.size()*font.lineHeight*scale)+23;String time=Instant.ofEpochMilli(e.time()).atZone(ZoneId.systemDefault()).format(format);list.add(new Bubble(lines,e.outgoing(),time,bh));totalHeight+=bh+5;}
            bubbles=List.copyOf(list);bubbleVersion=model().version;bubblePeer=peer;bubbleWidth=cw;cached24=prefs.clock24;cachedScale=scale;
        }
        int top=y+87,bottom=y+h-69,available=Math.max(1,bottom-top);scroll=Math.max(0,Math.min(scroll,Math.max(0,totalHeight-available)));int yy=bottom-totalHeight+scroll;
        g.enableScissor(cx,top,cx+cw,bottom);
        for(var b:bubbles){int bx=cx+(b.outgoing()?10:0),bw=cw-10;if(yy+b.height()>=top&&yy<bottom){g.fill(bx,yy,bx+bw,yy+b.height(),b.outgoing()?(prefs.darkTheme?0xFF285849:0xFFB9DFC6):prefs.card());float ty=yy+5;for(var line:b.lines()){text(g,line,bx+6,Math.round(ty),prefs.ink(),scale);ty+=font.lineHeight*scale;}text(g,b.time(),bx+6,yy+b.height()-12,prefs.muted(),.8f);}yy+=b.height()+5;}
        g.disableScissor();if(bubbles.isEmpty())g.drawWordWrap(font,Component.literal("Say hello to "+peerName+"."),cx,top+8,cw,prefs.muted());
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){if(peer!=null&&mx>=cx&&mx<=cx+cw&&my>=y+87&&my<=y+h-69){scroll=Math.max(0,scroll+(int)(vertical*24));return true;}return super.mouseScrolled(mx,my,horizontal,vertical);}
    @Override public boolean keyPressed(int key,int scan,int mods){if((key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER)&&input!=null&&input.isFocused()){send();return true;}return super.keyPressed(key,scan,mods);}
    @Override public void onClose(){if(peer!=null)back();else goHome();}
    @Override public boolean isPauseScreen(){return false;}
}
