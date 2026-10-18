package net.mcreator.commandcookieselectronicmod.client;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.commandcookieselectronicmod.nes.*;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Native-pixel artwork; immediate input prediction reconciles against server acknowledgements. */
public final class NesTetrisScreen extends Screen {
    private static ResourceLocation texture(String n){return ResourceLocation.fromNamespaceAndPath("cem_nes","textures/gui/"+n+".png");}
    private static final ResourceLocation FONT=texture("pixel_font"),MAZE=texture("maze"),TITLE=texture("title"),TYPE=texture("type_music"),LEVEL=texture("level"),TILES=texture("tiles");
    private static final int[][] PALETTES={{0x1010b4,0x39bbef},{0x168738,0x8eda52},{0x8643ad,0xe575ee},{0x3055b2,0x65d366},{0x9345b8,0x52cddd},{0x32913c,0xdfc335},{0xb24737,0x9291ed},{0x5930b4,0xb780f0},{0x426ec3,0xe3743d},{0xb94026,0xeb963b}};
    public enum Page {TITLE,TYPE,LEVEL,PLAY,LOSS,SUCCESS}
    public Page page;public final UUID session;public NesPayloads.State state;
    private final NesTetrisAudio audio=new NesTetrisAudio();private final Set<Integer> held=new HashSet<>();
    private record Input(int sequence,int action,int generation){}private final ArrayDeque<Input> pending=new ArrayDeque<>();
    private int sequence,ticks,lastUpdate,repeat,lastHorizontal=GLFW.GLFW_KEY_LEFT,selectedLevel,selectedHeight,music=1,focus,lossStarted;
    private boolean bType,ending;private int sentTick=-1,sentMask;public int visualX,visualY,visualRotation;
    private float scale,scaleY,left,top;
    private record Hit(String name,int x,int y,int w,int h,Runnable action){}private final List<Hit> hits=new ArrayList<>();
    public NesTetrisScreen(NesPayloads.State v){super(Component.literal("NES Tetris"));state=v;session=v.session();sequence=v.ack();selectedLevel=v.startLevel();selectedHeight=v.height();bType=v.bType();music=v.music();page=v.started()?(v.over()?(v.won()?Page.SUCCESS:Page.LOSS):Page.PLAY):Page.TITLE;reconcile();syncAudio();}
    public void accept(NesPayloads.State next){
        if(next.ack()<state.ack())return;
        if(next.lines()>state.lines())NesTetrisAudio.effect(next.lines()-state.lines()==4?"win":"line");
        else if(next.started()&&state.started()&&Arrays.stream(next.counts()).sum()>Arrays.stream(state.counts()).sum())NesTetrisAudio.effect("land");
        if(next.over()&&!state.over()){page=next.won()?Page.SUCCESS:Page.LOSS;lossStarted=ticks;held.clear();ending=false;}
        else if(next.started()&&!next.over()&&(page==Page.TITLE||page==Page.TYPE||page==Page.LEVEL)){page=Page.PLAY;held.clear();music=next.music();}
        state=next;lastUpdate=ticks;pending.removeIf(i->i.sequence<=next.ack()||i.generation!=generation());reconcile();syncAudio();
    }
    private void reconcile(){visualX=state.x();visualY=state.y();visualRotation=state.rotation();for(var i:pending)predict(i.action);}
    private int generation(){return Arrays.stream(state.counts()).sum();}
    private boolean fits(int x,int y,int r){int[] c=TetrisEngine.cells(state.piece(),r);for(int i=0;i<8;i+=2){int xx=x+c[i],yy=y+c[i+1];if(xx<0||xx>=10||yy< -2||yy>=20||yy>=0&&state.board()[yy*10+xx]!=0)return false;}return true;}
    private void predict(int n){
        if(!state.started()||state.paused()||state.over()||state.clearMask()!=0)return;
        if(n==0&&fits(visualX-1,visualY,visualRotation))visualX--;if(n==1&&fits(visualX+1,visualY,visualRotation))visualX++;
        if(n==2||n==3){int count=state.piece()==0||state.piece()==5||state.piece()==6?2:4,r=Math.floorMod(visualRotation+(n==2?1:-1),count);if(fits(visualX,visualY,r))visualRotation=r;}
        if(n==4&&fits(visualX,visualY+1,visualRotation))visualY++;
    }
    private void send(int n,int value){if(minecraft==null||minecraft.getConnection()==null)return;
        if(sentTick!=ticks){sentTick=ticks;sentMask=0;}if(n<=4&&((sentMask&(1<<n))!=0||Integer.bitCount(sentMask)>=3))return;sentMask|=1<<n;
        int id=++sequence;if(n<=4&&pending.size()<64){pending.add(new Input(id,n,generation()));predict(n);}PacketDistributor.sendToServer(new NesPayloads.Action(state.monitor(),session,n,value,id,generation()));}
    private void action(int n){send(n,0);}
    private void continuePage(){switch(page){case TITLE->{page=Page.TYPE;focus=0;syncAudio();}case TYPE->{page=Page.LEVEL;syncAudio();}case LEVEL->{if(!state.started())send(8,selectedLevel+selectedHeight*10+(bType?100:0)+music*256);}case PLAY->action(5);case LOSS,SUCCESS->setupAgain();}}
    private void setupAgain(){selectedLevel=state.startLevel();selectedHeight=state.height();bType=state.bType();music=state.music();page=Page.LEVEL;pending.clear();held.clear();ending=false;action(9);syncAudio();}
    private void syncAudio(){
        if(music==0){audio.stop();ending=false;return;}if(page==Page.TITLE||page==Page.PLAY&&state.paused()){audio.stop();ending=false;return;}
        if(page==Page.LOSS||page==Page.SUCCESS){if(!ending){ending=true;if(page==Page.SUCCESS)audio.success();else audio.play("high_score",false);}return;}ending=false;audio.play("music_"+music,true);
    }
    @Override public void tick(){
        ticks++;audio.tick();if(minecraft.player==null||minecraft.level==null||!NesSystem.controller(minecraft.player)||ticks-lastUpdate>120){onClose();return;}
        if(page!=Page.PLAY||state.paused()||state.over()){repeat=0;return;}
        if(held.contains(GLFW.GLFW_KEY_DOWN))action(4);
        if(held.contains(GLFW.GLFW_KEY_LEFT)||held.contains(GLFW.GLFW_KEY_RIGHT)){int key=held.contains(lastHorizontal)?lastHorizontal:held.contains(GLFW.GLFW_KEY_LEFT)?GLFW.GLFW_KEY_LEFT:GLFW.GLFW_KEY_RIGHT;if(++repeat>=5&&(repeat-5)%2==0)action(key==GLFW.GLFW_KEY_LEFT?0:1);}else repeat=0;
    }
    @Override public boolean keyPressed(int key,int scan,int mods){
        if(key==GLFW.GLFW_KEY_ESCAPE){if(page==Page.LEVEL){page=Page.TYPE;syncAudio();}else if(page==Page.TYPE){page=Page.TITLE;syncAudio();}else onClose();return true;}
        if(!held.add(key))return true;if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER||key==GLFW.GLFW_KEY_SPACE){continuePage();return true;}
        if(page==Page.TYPE){if(key==GLFW.GLFW_KEY_UP||key==GLFW.GLFW_KEY_DOWN)focus^=1;if(key==GLFW.GLFW_KEY_LEFT||key==GLFW.GLFW_KEY_RIGHT){if(focus==0)bType=!bType;else music=(music+(key==GLFW.GLFW_KEY_RIGHT?1:3))%4;syncAudio();}return true;}
        if(page==Page.LEVEL){if(key==GLFW.GLFW_KEY_LEFT)selectedLevel=(selectedLevel+9)%10;if(key==GLFW.GLFW_KEY_RIGHT)selectedLevel=(selectedLevel+1)%10;if(key==GLFW.GLFW_KEY_UP||key==GLFW.GLFW_KEY_DOWN)selectedLevel=(selectedLevel+5)%10;if(key==GLFW.GLFW_KEY_H&&bType)selectedHeight=(selectedHeight+1)%6;return true;}
        if(page!=Page.PLAY)return true;if(key==GLFW.GLFW_KEY_R&&state.paused()){setupAgain();return true;}if(state.paused()||state.over())return true;
        switch(key){case GLFW.GLFW_KEY_LEFT,GLFW.GLFW_KEY_RIGHT->{lastHorizontal=key;repeat=0;action(key==GLFW.GLFW_KEY_LEFT?0:1);}case GLFW.GLFW_KEY_DOWN->action(4);case GLFW.GLFW_KEY_X,GLFW.GLFW_KEY_UP->{action(2);NesTetrisAudio.effect("rotate");}case GLFW.GLFW_KEY_Z->{action(3);NesTetrisAudio.effect("rotate");}}return true;
    }
    @Override public boolean keyReleased(int key,int scan,int mods){held.remove(key);if(key==lastHorizontal)repeat=0;return true;}
    @Override public boolean isPauseScreen(){return false;}@Override public void onClose(){minecraft.setScreen(null);}
    @Override public void removed(){audio.stop();held.clear();pending.clear();if(minecraft.getConnection()!=null)action(7);super.removed();}
    @Override public boolean mouseClicked(double x,double y,int button){if(button!=0)return false;double xx=(x-left)/scale,yy=(y-top)/scaleY;for(var h:hits)if(xx>=h.x&&xx<h.x+h.w&&yy>=h.y&&yy<h.y+h.h){h.action.run();return true;}return false;}
    private void hit(String n,int x,int y,int w,int h,Runnable r){hits.add(new Hit(n,x,y,w,h,r));}
    private void fill(GuiGraphics g,int x,int y,int w,int h,int c){g.fill(x,y,x+w,y+h,0xff000000|c);}
    private void tint(GuiGraphics g,int c){g.setColor(((c>>16)&255)/255f,((c>>8)&255)/255f,(c&255)/255f,1);}
    private static final int[][] DIGITS={{14,17,19,21,25,17,14},{4,12,4,4,4,4,14},{14,17,1,2,4,8,31},{30,1,1,14,1,1,30},{2,6,10,18,31,2,2},{31,16,16,30,1,1,30},{14,16,16,30,17,17,14},{31,1,2,4,8,8,8},{14,17,17,14,17,17,14},{14,17,17,15,1,1,14}};
    private void digit(GuiGraphics g,int n,int x,int y,int c){for(int row=0;row<7;row++)for(int col=0;col<5;col++)if((DIGITS[n][row]&(1<<(4-col)))!=0)fill(g,x+col,y+row,1,1,c);}
    private void text(GuiGraphics g,String s,int x,int y,int c){for(int i=0;i<s.length();i++){char ch=Character.toUpperCase(s.charAt(i));if(ch>='0'&&ch<='9'){g.setColor(1,1,1,1);digit(g,ch-'0',x+i*8+1,y,c);}else{int n=ch-32;tint(g,c);if(n>=0&&n<96)g.blit(FONT,x+i*8,y,n%16*8,n/16*8,8,8,128,48);}}g.setColor(1,1,1,1);}
    private void counter(GuiGraphics g,String s,int x,int y){for(int i=0;i<s.length();i++)digit(g,s.charAt(i)-'0',x+i*7,y,0xa65f50);}
    private void panel(GuiGraphics g,int x,int y,int w,int h){fill(g,x-4,y-4,w+8,h+8,0x081411);fill(g,x-3,y-3,w+6,h+6,0x7e8c7c);fill(g,x-2,y-2,w+4,h+4,0xc4fcf8);fill(g,x-1,y-1,w+2,h+2,0x1d3835);fill(g,x,y,w,h,0);}
    private void cell(GuiGraphics g,int x,int y,int p,int size){int[] palette=PALETTES[Math.floorMod(state.level(),10)];int k=p==0||p==1||p==2?0:p==3||p==5?1:2;fill(g,x,y,size,size,0);tint(g,k==2?palette[1]:palette[0]);g.blit(TILES,x,y,size,size,k*8,0,8,8,48,8);g.setColor(1,1,1,1);g.blit(TILES,x,y,size,size,24+k*8,0,8,8,48,8);}
    private void piece(GuiGraphics g,int p,int r,int x,int y,int size){int[] c=TetrisEngine.cells(p,r);for(int i=0;i<8;i+=2)cell(g,x+c[i]*size,y+c[i+1]*size,p,size);}
    private void arrow(GuiGraphics g,int x,int y,boolean left){for(int row=0;row<7;row++){int w=4-Math.abs(row-3);fill(g,left?x:x+4-w,y+row,w,1,0xffffff);}}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        g.fill(0,0,width,height,0xdf0a1017);int rawW=minecraft.getWindow().getWidth(),rawH=minecraft.getWindow().getHeight();int pixel=Math.max(1,Math.min(rawW/256,rawH/240));float unit=Math.min(pixel,Math.min(rawW/256f,rawH/240f));
        scale=unit*width/rawW;scaleY=unit*height/rawH;left=(float)Math.floor((rawW-256*unit)/2)*width/rawW;top=(float)Math.floor((rawH-240*unit)/2)*height/rawH;
        g.pose().pushPose();g.pose().translate(left,top,0);g.pose().scale(scale,scaleY,1);hits.clear();switch(page){case TITLE->title(g);case TYPE->type(g);case LEVEL->level(g);default->game(g);}
        fill(g,0,224,256,16,0x071717);String guide=switch(page){case TITLE->"ENTER START / ESC EXIT";case TYPE->"ARROWS SELECT / ENTER NEXT";case LEVEL->"ENTER PLAY / ESC BACK";case LOSS,SUCCESS->"ENTER TO CONTINUE";case PLAY->state.paused()?"ENTER RESUME / R NEW GAME":"ARROWS / Z X / ENTER PAUSE";};
        text(g,guide,4,228,0xbadbd6);text(g,"X",244,228,0xbadbd6);hit("Continue",0,224,232,16,this::continuePage);hit("Exit",232,224,24,16,this::onClose);g.pose().popPose();
    }
    private void title(GuiGraphics g){g.blit(TITLE,0,0,0,0,256,224,256,224);if(ticks%30>=15)fill(g,54,151,86,10,0);hit("Push Start",16,32,224,160,this::continuePage);}
    private void type(GuiGraphics g){g.blit(TYPE,0,0,0,0,256,224,256,224);arrow(g,bType?160:64,56,true);arrow(g,bType?224:128,56,false);int row=music==0?3:music-1;arrow(g,104,136+row*16,true);arrow(g,184,136+row*16,false);
        hit("A-TYPE",56,48,80,24,()->{bType=false;focus=0;});hit("B-TYPE",152,48,80,24,()->{bType=true;focus=0;});for(int i=0;i<4;i++){final int m=i==3?0:i+1;hit("Music "+m,100,132+i*16,92,16,()->{music=m;focus=1;syncAudio();});}if(focus==1)fill(g,97,128+row*16,1,15,0xffffff);else fill(g,bType?151:55,47,1,25,0xffffff);
    }
    private void grid(GuiGraphics g,int x,int y,int cols,int count,int selected){for(int i=0;i<count;i++){int xx=x+i%cols*16,yy=y+i/cols*16;fill(g,xx+1,yy+1,15,15,i==selected?0xf7b638:0);text(g,Integer.toString(i),xx+4,yy+4,i==selected?0xd04a04:0xc33105);for(int v=0;v<=16;v+=2){fill(g,xx+v,yy,1,2,0x58aa50);fill(g,xx+v,yy+16,1,1,0x58aa50);fill(g,xx,yy+v,2,1,0x58aa50);fill(g,xx+16,yy+v,1,1,0x58aa50);}}}
    private void level(GuiGraphics g){g.blit(LEVEL,0,0,0,0,256,224,256,224);fill(g,104,16,48,8,0);text(g,bType?"B-TYPE":"A-TYPE",104,16,0xffffff);fill(g,50,74,88,38,0);grid(g,52,76,5,10,selectedLevel);for(int i=0;i<10;i++){final int n=i;hit("Level "+i,52+i%5*16,76+i/5*16,16,16,()->selectedLevel=n);}if(bType){panel(g,148,54,56,12);text(g,"HEIGHT",152,56,0xffffff);grid(g,152,76,3,6,selectedHeight);for(int i=0;i<6;i++){final int n=i;hit("Height "+i,152+i%3*16,76+i/3*16,16,16,()->selectedHeight=n);}}if(ticks%40<30)text(g,"PUSH START",88,176,0xffffff);}
    private void game(GuiGraphics g){
        g.blit(MAZE,0,0,0,0,256,224,256,224);
        panel(g,16,16,60,12);text(g,state.bType()?"B-TYPE":"A-TYPE",22,18,0xffffff);panel(g,92,8,84,12);text(g,String.format(Locale.ROOT,"LINES-%03d",state.bType()?Math.max(0,25-state.lines()):state.lines()%1000),98,10,0xffffff);
        panel(g,184,12,64,64);text(g,"TOP",188,18,0xffffff);text(g,String.format(Locale.ROOT,"%06d",Math.min(999999,Math.max(10000,state.best()))),188,26,0xffffff);text(g,"SCORE",188,44,0xffffff);text(g,String.format(Locale.ROOT,"%06d",Math.min(999999,state.score())),188,52,0xffffff);
        panel(g,8,64,80,140);text(g,"STATISTICS",8,68,0xffffff);int[] order={2,3,6,1,5,4,0};for(int i=0;i<7;i++){int p=order[i];piece(g,p,0,24,86+i*16,4);counter(g,String.format(Locale.ROOT,"%03d",state.counts()[p]%1000),50,88+i*16);}
        panel(g,184,96,40,40);text(g,"NEXT",188,98,0xffffff);if(!state.over())piece(g,state.next(),0,189,114,6);panel(g,184,160,48,16);text(g,"LEVEL",188,160,0xffffff);text(g,String.format(Locale.ROOT,"%02d",state.level()%100),204,168,0xffffff);panel(g,96,40,80,160);
        for(int y=0;y<20;y++)for(int x=0;x<10;x++)if(state.board()[y*10+x]>0){if((state.clearMask()&(1<<y))!=0&&ticks%4<2)fill(g,96+x*8,40+y*8,8,8,0xffffff);else cell(g,96+x*8,40+y*8,state.board()[y*10+x]-1,8);}
        if(!state.over()&&state.clearMask()==0){int[] c=TetrisEngine.cells(state.piece(),visualRotation);for(int i=0;i<8;i+=2){int x=visualX+c[i],y=visualY+c[i+1];if(y>=0&&y<20&&x>=0&&x<10)cell(g,96+x*8,40+y*8,state.piece(),8);}}
        if(page==Page.LOSS){int elapsed=Math.min(59,ticks-lossStarted),rows=Math.min(20,elapsed+1),cream=(elapsed/10)%2==0?0xf7dda0:0xd29b54;for(int r=0;r<rows;r++){fill(g,96,40+r*8,80,8,0x90300d);fill(g,96,41+r*8,80,5,cream);fill(g,96,46+r*8,80,1,0xf17d30);}}
        else if(page==Page.SUCCESS||state.paused()){fill(g,97,103,78,26,0);text(g,page==Page.SUCCESS?"SUCCESS":"PAUSED",page==Page.SUCCESS?108:112,106,0xffffff);text(g,"ENTER",116,119,0xc4ffff);}
    }
}
