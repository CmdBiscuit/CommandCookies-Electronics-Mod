package net.mcreator.commandcookieselectronicmod.nes;

import java.io.*;
import java.util.Arrays;

/** Original classic-style rules: ten by twenty, no hold, ghost or wall kicks. */
public final class TetrisEngine {
    public static final int[][] BASE={{0,1,1,1,2,1,3,1},{1,0,2,0,1,1,2,1},{0,0,1,0,2,0,1,1},
        {0,0,1,0,2,0,2,1},{0,0,1,0,2,0,0,1},{1,0,2,0,0,1,1,1},{0,0,1,0,1,1,2,1}};
    public final byte[] board=new byte[200];
    public final int[] statistics=new int[7];
    public int piece,next,x=3,y=-1,rotation,score,lines,level,startLevel,clearMask,clearDelay,frames,lastClear;
    public boolean started,paused=true,over,bType,won;
    public int height,music=1;
    private long random;
    private int previous=-1;
    private boolean droppedThisTick,dropLockedThisTick;
    public TetrisEngine(long seed){random=seed==0?7391:seed;next=randomPiece();}
    private int random(int bound){random^=random<<13;random^=random>>>7;random^=random<<17;return (int)Long.remainderUnsigned(random,bound);}
    private int randomPiece(){int p=random(8);if(p==7||p==previous)p=random(7);previous=p;return p;}
    public static int[] cells(int piece,int rotation){
        int[] out=BASE[piece].clone();if(piece==1)return out;
        int size=piece==0?3:2;
        for(int r=0;r<(rotation%(piece==0||piece==5||piece==6?2:4));r++)for(int i=0;i<8;i+=2){int xx=out[i];out[i]=size-out[i+1];out[i+1]=xx;}
        return out;
    }
    public void start(int selectedLevel){start(selectedLevel,bType,height);}
    public void start(int selectedLevel,boolean mode,int selectedHeight){
        Arrays.fill(board,(byte)0);Arrays.fill(statistics,0);score=lines=clearMask=clearDelay=frames=lastClear=0;droppedThisTick=dropLockedThisTick=false;
        level=startLevel=Math.max(0,Math.min(9,selectedLevel));started=true;paused=over=won=false;bType=mode;height=Math.max(0,Math.min(5,selectedHeight));
        if(bType)for(int row=20-height*2;row<20;row++){int hole=random(10);for(int col=0;col<10;col++)board[row*10+col]=(byte)(col==hole||random(5)==0?0:1+random(7));}
        next=randomPiece();spawn();
    }
    private void spawn(){piece=next;next=randomPiece();x=3;y=-1;rotation=0;frames=0;statistics[piece]++;if(!fits(x,y,rotation))over=true;}
    public boolean fits(int xx,int yy,int rr){
        int[] cells=cells(piece,rr);
        for(int i=0;i<8;i+=2){int px=xx+cells[i],py=yy+cells[i+1];if(px<0||px>=10||py>=20||py< -2)return false;if(py>=0&&board[py*10+px]!=0)return false;}
        return true;
    }
    public boolean move(int dx){if(!started||paused||over||clearDelay>0)return false;if(!fits(x+dx,y,rotation))return false;x+=dx;return true;}
    public boolean rotate(int direction){if(!started||paused||over||clearDelay>0)return false;int r=Math.floorMod(rotation+direction,piece==0||piece==5||piece==6?2:4);if(!fits(x,y,r))return false;rotation=r;return true;}
    public int generation(){return Arrays.stream(statistics).sum();}
    public boolean softDrop(){if(!started||paused||over||clearDelay>0)return false;droppedThisTick=true;if(fits(x,y+1,rotation)){y++;score=Math.min(999999999,score+1);return true;}dropLockedThisTick=true;lock();return false;}
    public int gravityFrames(){
        int[] speeds={48,43,38,33,28,23,18,13,8,6};
        if(level<10)return speeds[level];if(level<13)return 5;if(level<16)return 4;if(level<19)return 3;if(level<29)return 2;return 1;
    }
    public void tick(){
        boolean dropped=droppedThisTick,locked=dropLockedThisTick;droppedThisTick=dropLockedThisTick=false;
        if(!started||paused||over)return;
        if(clearDelay>0){if(--clearDelay==0)clear();return;}
        if(locked)return;
        frames+=3;int gravity=gravityFrames();
        if(dropped&&frames>=gravity)frames-=gravity;
        while(frames>=gravity&&!over&&clearDelay==0){frames-=gravity;if(fits(x,y+1,rotation))y++;else{lock();break;}}
    }
    private void lock(){
        int[] cells=cells(piece,rotation);
        for(int i=0;i<8;i+=2)if(y+cells[i+1]<0){over=true;return;}
        for(int i=0;i<8;i+=2)board[(y+cells[i+1])*10+x+cells[i]]=(byte)(piece+1);
        clearMask=0;
        for(int row=0;row<20;row++){boolean full=true;for(int col=0;col<10;col++)if(board[row*10+col]==0){full=false;break;}if(full)clearMask|=1<<row;}
        if(clearMask!=0)clearDelay=6;else spawn();
    }
    private void clear(){
        int count=Integer.bitCount(clearMask);int dest=19;
        for(int row=19;row>=0;row--)if((clearMask&(1<<row))==0){System.arraycopy(board,row*10,board,dest*10,10);dest--;}
        while(dest>=0){Arrays.fill(board,dest*10,dest*10+10,(byte)0);dest--;}
        int[] points={0,40,100,300,1200};score=Math.min(999999999,score+points[count]*(level+1));lines+=count;lastClear=count;
        int transition=Math.min(startLevel*10+10,Math.max(100,startLevel*10-50));
        level=bType?startLevel:startLevel+(lines>=transition?1+(lines-transition)/10:0);clearMask=0;
        if(bType&&lines>=25){won=over=true;return;}spawn();
    }
    public byte[] save(){
        try{var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeInt(3);out.write(board);for(int v:statistics)out.writeInt(v);
            for(int v:new int[]{piece,next,x,y,rotation,score,lines,level,startLevel,clearMask,clearDelay,frames,lastClear,previous})out.writeInt(v);
            out.writeBoolean(started);out.writeBoolean(paused);out.writeBoolean(over);out.writeLong(random);out.writeBoolean(bType);out.writeBoolean(won);out.writeInt(height);out.writeInt(music);return bytes.toByteArray();
        }catch(IOException impossible){throw new AssertionError(impossible);}
    }
    public static TetrisEngine load(byte[] bytes){
        try{if(bytes.length>1024)throw new IOException();var in=new DataInputStream(new ByteArrayInputStream(bytes));int version=in.readInt();if(version<1||version>3)throw new IOException();
            var e=new TetrisEngine(7391);in.readFully(e.board);for(byte v:e.board)if(v<0||v>7)throw new IOException();
            for(int i=0;i<7;i++){e.statistics[i]=in.readInt();if(e.statistics[i]<0)throw new IOException();}
            e.piece=in.readInt();e.next=in.readInt();e.x=in.readInt();e.y=in.readInt();e.rotation=in.readInt();e.score=in.readInt();e.lines=in.readInt();
            e.level=in.readInt();e.startLevel=in.readInt();e.clearMask=in.readInt();e.clearDelay=in.readInt();e.frames=in.readInt();e.lastClear=in.readInt();e.previous=in.readInt();
            e.started=in.readBoolean();e.paused=in.readBoolean();e.over=in.readBoolean();e.random=in.readLong();if(version>=2){e.bType=in.readBoolean();e.won=in.readBoolean();e.height=in.readInt();}if(version>=3)e.music=in.readInt();
            if(e.piece<0||e.piece>6||e.next<0||e.next>6||e.x< -3||e.x>10||e.y< -2||e.y>19||e.rotation<0||e.rotation>3||e.score<0||e.score>999999999||e.lines<0||e.lines>1000000||e.level<0||e.level>100009||e.music<0||e.music>3||e.height<0||e.height>5||Integer.bitCount(e.clearMask)>4||e.frames<0||e.frames>60||e.startLevel<0||e.startLevel>9||e.clearDelay<0||e.clearDelay>6||in.available()!=0)throw new IOException();
            if(e.piece==0||e.piece==5||e.piece==6)e.rotation%=2;
            e.paused=true;return e;
        }catch(IOException|RuntimeException invalid){return new TetrisEngine(7391);}
    }
}
