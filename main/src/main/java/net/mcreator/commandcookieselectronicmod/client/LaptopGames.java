package net.mcreator.commandcookieselectronicmod.client;

import java.util.*;

/** Small, bounded game engines. Updates are driven by client ticks, not frame rate. */
public final class LaptopGames {
    public enum Cue {START,PAUSE,MOVE,ROTATE,EAT,MERGE,FLAG,REVEAL,BOUNCE,BRICK,LAND,LINE,FLIP,MATCH,MISS,MARK,READY,TOO_SOON,WIN,LOSE,DRAW,UNDO,LIFE}
    public abstract static class Game {
        private final ArrayDeque<Cue> sounds=new ArrayDeque<>();
        protected final void emit(Cue cue){if(sounds.size()>=8)sounds.removeFirst();sounds.addLast(cue);}
        public final void clearSounds(){sounds.clear();}
        public final List<Cue> drainSounds(){var result=new ArrayList<>(sounds);sounds.clear();return result;}
        public final void restart(){reset();emit(Cue.START);}
        public int score,ticks;public boolean started,paused,over;
        public abstract void reset();
        public void tick(){ticks++;}
        public void toggle(){if(!over){if(!started){started=true;emit(Cue.START);}else{paused=!paused;emit(paused?Cue.PAUSE:Cue.START);}}}
    }
    public static Game create(int id){return switch(id){case 8->new Snake();case 9->new Merge();case 10->new Mines();case 11->new Breakout();case 12->new Fall();case 13->new Memory();case 14->new Tic();case 15->new Reaction();default->null;};}
    public static final class Snake extends Game {
        public static final int W=18,H=14;public final Deque<Integer> body=new ArrayDeque<>();public int direction=1,pending=1,food;private final Random random=new Random();
        public Snake(){reset();}
        public void reset(){clearSounds();body.clear();body.add(7*W+8);body.add(7*W+7);body.add(7*W+6);direction=pending=1;score=ticks=0;over=paused=started=false;food();}
        private void food(){var free=new ArrayList<Integer>();for(int i=0;i<W*H;i++)if(!body.contains(i))free.add(i);if(free.isEmpty()){over=true;food=-1;emit(Cue.WIN);}else food=free.get(random.nextInt(free.size()));}
        public void steer(int next){if(over)return;if(next!=Math.floorMod(direction+2,4)){if(pending!=next)emit(Cue.MOVE);pending=next;}if(!started)emit(Cue.START);started=true;}
        public void step(){direction=pending;int head=body.getFirst(),x=head%W,y=head/W;int nx=x+(direction==1?1:direction==3?-1:0),ny=y+(direction==2?1:direction==0?-1:0),pos=ny*W+nx;boolean eat=pos==food;
            if(nx<0||nx>=W||ny<0||ny>=H||body.contains(pos)&&(eat||pos!=body.getLast())){over=true;emit(Cue.LOSE);return;}if(!eat)body.removeLast();body.addFirst(pos);if(eat){score++;emit(Cue.EAT);food();}}
        @Override public void tick(){if(started&&!paused&&!over&&++ticks>=Math.max(2,5-score/4)){ticks=0;step();}}
    }
    public static final class Merge extends Game {
        public int[] board=new int[16],undo;public int undoScore;public boolean won;private final Random random=new Random();
        public Merge(){reset();}
        public void reset(){clearSounds();Arrays.fill(board,0);score=0;won=over=false;undo=null;spawn();spawn();}
        private void spawn(){var free=new ArrayList<Integer>();for(int i=0;i<16;i++)if(board[i]==0)free.add(i);if(!free.isEmpty())board[free.get(random.nextInt(free.size()))]=random.nextInt(10)==0?4:2;}
        public boolean move(int dir){if(over)return false;int[] before=board.clone();int previous=score;boolean wasWon=won;
            for(int line=0;line<4;line++){int[] indices=new int[4];for(int k=0;k<4;k++)indices[k]=switch(dir){case 0->k*4+line;case 1->line*4+3-k;case 2->(3-k)*4+line;default->line*4+k;};var values=new ArrayList<Integer>();for(int idx:indices)if(board[idx]!=0)values.add(board[idx]);var merged=new ArrayList<Integer>();for(int k=0;k<values.size();k++){int n=values.get(k);if(k+1<values.size()&&n==values.get(k+1)&&n<65536){n*=2;k++;score+=n;}merged.add(n);}for(int k=0;k<4;k++)board[indices[k]]=k<merged.size()?merged.get(k):0;}
            if(Arrays.equals(before,board)){over=!canMove();if(over)emit(Cue.LOSE);return false;}undo=before;undoScore=previous;spawn();won=Arrays.stream(board).anyMatch(v->v>=2048);over=!canMove();emit(score>previous?Cue.MERGE:Cue.MOVE);if(won&&!wasWon)emit(Cue.WIN);else if(over)emit(Cue.LOSE);return true;
        }
        public boolean canMove(){for(int i=0;i<16;i++)if(board[i]==0||board[i]<65536&&(i%4<3&&board[i]==board[i+1]||i<12&&board[i]==board[i+4]))return true;return false;}
        public void undo(){if(undo!=null){board=undo;score=undoScore;undo=null;over=false;won=Arrays.stream(board).anyMatch(v->v>=2048);emit(Cue.UNDO);}}
    }
    public static final class Mines extends Game {
        public static final int W=12,H=9,N=16;public final boolean[] bombs=new boolean[W*H],seen=new boolean[W*H],flags=new boolean[W*H];public boolean planted,won;public int revealed,flagged;private final Random random=new Random();
        public Mines(){reset();}
        public void reset(){clearSounds();Arrays.fill(bombs,false);Arrays.fill(seen,false);Arrays.fill(flags,false);planted=won=over=started=false;revealed=flagged=score=ticks=0;}
        private List<Integer> neighbors(int i){var result=new ArrayList<Integer>();for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){int x=i%W+dx,y=i/W+dy;if((dx!=0||dy!=0)&&x>=0&&x<W&&y>=0&&y<H)result.add(y*W+x);}return result;}
        private void plant(int safe){var cells=new ArrayList<Integer>();var exclude=neighbors(safe);exclude.add(safe);for(int i=0;i<W*H;i++)if(!exclude.contains(i))cells.add(i);Collections.shuffle(cells,random);for(int i=0;i<N;i++)bombs[cells.get(i)]=true;planted=started=true;}
        public int nearby(int i){int n=0;for(int cell:neighbors(i))if(bombs[cell])n++;return n;}
        public void flag(int i){if(i<0||i>=W*H||seen[i]||over)return;flags[i]=!flags[i];flagged+=flags[i]?1:-1;emit(Cue.FLAG);}
        public void reveal(int i){if(i<0||i>=W*H||seen[i]||flags[i]||over)return;if(!planted)plant(i);if(bombs[i]){seen[i]=true;over=true;emit(Cue.LOSE);return;}
            var queue=new ArrayDeque<Integer>();queue.add(i);while(!queue.isEmpty()){int cell=queue.removeFirst();if(seen[cell]||flags[cell]||bombs[cell])continue;seen[cell]=true;revealed++;if(nearby(cell)==0)queue.addAll(neighbors(cell));}score=revealed;emit(Cue.REVEAL);if(revealed==W*H-N){won=over=true;emit(Cue.WIN);}}
        @Override public void tick(){if(started&&!over)ticks++;}
    }
    public static final class Breakout extends Game {
        public final boolean[] bricks=new boolean[30];public double x,y,vx,vy,paddle;public int lives,level,move;public boolean launched;
        public Breakout(){reset();}
        public void reset(){clearSounds();Arrays.fill(bricks,true);lives=3;level=1;score=ticks=move=0;over=paused=started=launched=false;paddle=100;serve();}
        private void serve(){x=paddle+20;y=126;vx=2.2;vy=-2.8;launched=false;}
        public void launch(){if(over)return;started=true;if(!launched){launched=true;paused=false;emit(Cue.START);}else{paused=!paused;emit(paused?Cue.PAUSE:Cue.START);}}
        @Override public void tick(){if(paused||over)return;paddle=Math.max(0,Math.min(200,paddle+move*6));if(!launched){x=paddle+20;return;}
            for(int step=0;step<2;step++){x+=vx/2;y+=vy/2;if(x<3){x=3;vx=Math.abs(vx);emit(Cue.BOUNCE);}if(x>237){x=237;vx=-Math.abs(vx);emit(Cue.BOUNCE);}if(y<3){y=3;vy=Math.abs(vy);emit(Cue.BOUNCE);}if(vy>0&&y>=129&&y<=138&&x>=paddle-3&&x<=paddle+43){y=129;vx=(x-(paddle+20))*.16;vy=-Math.sqrt(Math.max(4,13-vx*vx));emit(Cue.BOUNCE);}
                for(int i=0;i<30;i++){int bx=i%6*38+7,by=i/6*12+14;if(bricks[i]&&x>=bx-3&&x<=bx+33&&y>=by-3&&y<=by+10){bricks[i]=false;score+=10;vy=-vy;emit(Cue.BRICK);break;}}
                if(y>148){lives--;if(lives<=0){over=true;emit(Cue.LOSE);}else{serve();emit(Cue.LIFE);}break;}
            }if(!over&&Arrays.stream(toInts()).sum()==0){level++;Arrays.fill(bricks,true);score+=100;serve();emit(Cue.WIN);}
        }
        private int[] toInts(){int[] values=new int[30];for(int i=0;i<30;i++)values[i]=bricks[i]?1:0;return values;}
    }
    public static final class Fall extends Game {
        public static final int W=10,H=18;private static final int[][] SHAPES={{0,1,1,1,2,1,3,1},{1,0,2,0,1,1,2,1},{1,0,0,1,1,1,2,1},{1,0,2,0,0,1,1,1},{0,0,1,0,1,1,2,1},{0,0,0,1,1,1,2,1},{2,0,0,1,1,1,2,1}};
        // The I shape uses row 1, keeping all rotations within a 4 x 4 matrix.
        public int[] board=new int[W*H],piece;public int type,next,x,y,lines,fallTicks;private final List<Integer> bag=new ArrayList<>();private final Random random=new Random();
        public Fall(){reset();}
        private int choose(){if(bag.isEmpty()){for(int i=0;i<7;i++)bag.add(i);Collections.shuffle(bag,random);}return bag.removeLast();}
        public void reset(){clearSounds();Arrays.fill(board,0);bag.clear();score=lines=fallTicks=0;started=paused=over=false;next=choose();spawn();}
        private void spawn(){type=next;next=choose();piece=SHAPES[type].clone();if(type==0)for(int i=1;i<8;i+=2)piece[i]=1;x=3;y=0;fallTicks=0;if(!fits(x,y,piece))over=true;}
        public boolean fits(int px,int py,int[] shape){for(int i=0;i<8;i+=2){int xx=px+shape[i],yy=py+shape[i+1];if(xx<0||xx>=W||yy<0||yy>=H||board[yy*W+xx]!=0)return false;}return true;}
        public void shift(int dx){if(!over&&!paused){started=true;if(fits(x+dx,y,piece)){x+=dx;emit(Cue.MOVE);}}}
        public void rotate(){if(over||paused||type==1)return;started=true;int[] rotated=piece.clone();int span=type==0?3:2;for(int i=0;i<8;i+=2){rotated[i]=span-piece[i+1];rotated[i+1]=piece[i];}for(int kick:new int[]{0,-1,1,-2,2})if(fits(x+kick,y,rotated)){piece=rotated;x+=kick;emit(Cue.ROTATE);return;}}
        public void down(){if(over||paused)return;started=true;if(fits(x,y+1,piece))y++;else lock();}
        public void hard(){if(over||paused)return;started=true;while(fits(x,y+1,piece)){y++;score+=2;}lock();}
        private void lock(){emit(Cue.LAND);for(int i=0;i<8;i+=2)board[(y+piece[i+1])*W+x+piece[i]]=type+1;int removed=0;for(int row=H-1;row>=0;row--){boolean full=true;for(int col=0;col<W;col++)if(board[row*W+col]==0)full=false;if(full){for(int yy=row;yy>0;yy--)System.arraycopy(board,(yy-1)*W,board,yy*W,W);Arrays.fill(board,0,W,0);removed++;row++;}}lines+=removed;score+=new int[]{0,100,300,500,800}[removed];if(removed>0)emit(Cue.LINE);spawn();if(over)emit(Cue.LOSE);}
        @Override public void tick(){if(started&&!paused&&!over&&++fallTicks>=Math.max(2,16-lines/3)){fallTicks=0;down();}}
    }
    public static final class Memory extends Game {
        public int[] cards=new int[16];public boolean[] matched=new boolean[16];public int first=-1,second=-1,delay,turns,pairs;
        public Memory(){reset();}
        public void reset(){clearSounds();var values=new ArrayList<Integer>();for(int i=0;i<8;i++){values.add(i);values.add(i);}Collections.shuffle(values);for(int i=0;i<16;i++)cards[i]=values.get(i);Arrays.fill(matched,false);first=second=-1;delay=turns=pairs=score=0;over=false;}
        public void flip(int i){if(i<0||i>=16||matched[i]||i==first||second>=0||over)return;emit(Cue.FLIP);if(first<0)first=i;else{second=i;turns++;if(cards[first]==cards[second]){matched[first]=matched[second]=true;pairs++;emit(Cue.MATCH);first=second=-1;if(pairs==8){over=true;score=turns;emit(Cue.WIN);}}else{delay=14;emit(Cue.MISS);}}}
        @Override public void tick(){if(second>=0&&--delay<=0)first=second=-1;}
    }
    public static final class Tic extends Game {
        public int[] board=new int[9];public int winner;public boolean expert=true;
        public Tic(){reset();}public void reset(){clearSounds();Arrays.fill(board,0);winner=0;over=false;}
        public static int winner(int[] board){for(int[] line:new int[][]{{0,1,2},{3,4,5},{6,7,8},{0,3,6},{1,4,7},{2,5,8},{0,4,8},{2,4,6}})if(board[line[0]]!=0&&board[line[0]]==board[line[1]]&&board[line[1]]==board[line[2]])return board[line[0]];return Arrays.stream(board).noneMatch(v->v==0)?3:0;}
        public void play(int i){if(i<0||i>=9||board[i]!=0||over)return;board[i]=1;emit(Cue.MARK);winner=winner(board);if(winner==0){int choice=-1,best=-100;for(int k=0;k<9;k++)if(board[k]==0){board[k]=2;int value=expert?solve(1):new Random().nextInt(100);board[k]=0;if(value>best){best=value;choice=k;}}if(choice>=0)board[choice]=2;winner=winner(board);}over=winner!=0;if(winner==1)score++;if(over)emit(winner==1?Cue.WIN:winner==2?Cue.LOSE:Cue.DRAW);}
        private int solve(int turn){int end=winner(board);if(end!=0)return end==2?10:end==1?-10:0;int best=turn==2?-100:100;for(int i=0;i<9;i++)if(board[i]==0){board[i]=turn;int n=solve(3-turn);board[i]=0;best=turn==2?Math.max(best,n):Math.min(best,n);}return best;}
    }
    public static final class Reaction extends Game {
        public int phase,wait;public long ready,result;
        public Reaction(){reset();}public void reset(){clearSounds();phase=0;result=ready=0;}
        public void begin(){clearSounds();emit(Cue.START);phase=1;wait=30+new Random().nextInt(60);result=0;}
        public void click(){if(phase==1){phase=4;emit(Cue.TOO_SOON);}else if(phase==2){result=Math.max(1,(System.nanoTime()-ready)/1_000_000);phase=3;score=(int)Math.min(Integer.MAX_VALUE,result);emit(Cue.WIN);}}
        @Override public void tick(){if(phase==1&&--wait<=0){phase=2;ready=System.nanoTime();emit(Cue.READY);}}
    }
}
