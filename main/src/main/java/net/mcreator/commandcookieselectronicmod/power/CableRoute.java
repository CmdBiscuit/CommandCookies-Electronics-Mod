package net.mcreator.commandcookieselectronicmod.power;

import java.util.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.mcreator.commandcookieselectronicmod.pc.PcDeviceBlock;
import net.mcreator.commandcookieselectronicmod.nes.NesDeviceBlock;
import net.mcreator.commandcookieselectronicmod.block.CrtTvBlock;
import net.mcreator.commandcookieselectronicmod.block.SnesConsoleBlock;
import net.mcreator.commandcookieselectronicmod.block.N64ConsoleBlock;
import net.mcreator.commandcookieselectronicmod.block.SecurityCameraBlock;
import net.mcreator.commandcookieselectronicmod.block.GeneratorBlock;

/** Client-side visual routing. Electrical connections and entity collision remain unchanged. */
public final class CableRoute {
    private static final double CLEARANCE=.045, STEP=.5, MARGIN=3;
    private static final int MAX_NODES=4096;
    // Penalised zone in front of a device's rear plane: its own footprint, up to two blocks forward.
    private static final double FRONT_SIDE=.52,FRONT_DEPTH=2,FRONT_WEIGHT=3;
    // Cord stiffness: the sharpest bend between samples and around a plug's strain relief.
    private static final double BEND_COS=Math.cos(Math.toRadians(28)),STEM_COS=Math.cos(Math.toRadians(14));
    private static final double[] FALL={1,.4,.15,.05},SLIDE={1,.5,.25},SWING={.5,.25};
    // Extra length a link may take while the final stiffness pass rounds a taut wrap.
    private static final double STRETCH=1.3;
    public record Path(List<Vec3> points,boolean clear,Map<BlockPos,BlockState> watched,int searched,boolean limited,CablePorts.Port firstPort,CablePorts.Port secondPort) {
        public boolean unchanged(Level level){
            for(var e:watched.entrySet()){
                var now=level.hasChunkAt(e.getKey())?level.getBlockState(e.getKey()):null;
                if(now!=e.getValue())return false;
            }
            return true;
        }
    }
    private record Cell(int seed,int x,int y,int z){
        Cell offset(int axis,int step){return switch(axis){case 0->new Cell(seed,x+step,y,z);case 1->new Cell(seed,x,y+step,z);default->new Cell(seed,x,y,z+step);};}
        Vec3 point(List<Seed> seeds){return seeds.get(seed).point.add(x*STEP,y*STEP,z*STEP);}
    }
    private record Seed(Vec3 point,List<Vec3> prefix,double cost){}
    private record Frame(BlockPos root,Vec3 point,Vec3 normal,double shell){}
    private record Node(Cell cell,double cost,double rank,long order){}
    private static final class Query {
        final Level level;final BlockPos first,second;final net.mcreator.commandcookieselectronicmod.tv.ImageTvEntity firstTv,secondTv;CablePorts.Port firstPort,secondPort;
        final Map<BlockPos,BlockState> watched=new HashMap<>();
        final Map<BlockPos,List<AABB>> boxes=new HashMap<>();final List<AABB> keepouts=new ArrayList<>();final List<BlockPos> devices=new ArrayList<>();final List<Frame> frames=new ArrayList<>();boolean fitting;
        final Long2ObjectOpenHashMap<List<AABB>> cells=new Long2ObjectOpenHashMap<>();
        Query(Level level,CablePorts.Port a,CablePorts.Port b){this.level=level;firstPort=a;secondPort=b;first=BlockPos.containing(a.point().subtract(a.normal().scale(.001)));second=BlockPos.containing(b.point().subtract(b.normal().scale(.001)));firstTv=net.mcreator.commandcookieselectronicmod.tv.TvParts.master(level,first);secondTv=net.mcreator.commandcookieselectronicmod.tv.TvParts.master(level,second);keepBehind(first);if(!second.equals(first))keepBehind(second);for(var root:List.of(first,second))for(int x=-2;x<=2;x++)for(int y=-1;y<=1;y++)for(int z=-2;z<=2;z++){var pos=root.offset(x,y,z);if(!watched.containsKey(pos))keepBehind(pos);}}
        private boolean socketCell(BlockPos pos){
            if(pos.equals(first)||pos.equals(second))return true;
            if(firstTv==null&&secondTv==null)return false;
            var tv=net.mcreator.commandcookieselectronicmod.tv.TvParts.master(level,pos);
            return tv!=null&&(tv==firstTv||tv==secondTv);
        }
        List<AABB> boxes(BlockPos pos){
            if(fitting&&socketCell(pos))return List.of();
            var old=boxes.get(pos);if(old!=null)return old;
            BlockState state=level.hasChunkAt(pos)?level.getBlockState(pos):null;watched.put(pos,state);
            boolean endpoint=pos.equals(first)||pos.equals(second);
            boolean device=state!=null&&(state.getBlock() instanceof net.mcreator.commandcookieselectronicmod.tv.ImageTvBlock||state.getBlock() instanceof net.mcreator.commandcookieselectronicmod.tv.TvPartBlock||state.getBlock() instanceof PcDeviceBlock||(state.getBlock() instanceof NesDeviceBlock||state.getBlock() instanceof SnesConsoleBlock||state.getBlock() instanceof N64ConsoleBlock||state.getBlock() instanceof SecurityCameraBlock)||state.getBlock() instanceof CrtTvBlock||state.getBlock() instanceof GeneratorBlock);
            boolean socketShell=endpoint&&state!=null&&((state.getBlock() instanceof NesDeviceBlock||state.getBlock() instanceof SnesConsoleBlock||state.getBlock() instanceof N64ConsoleBlock||state.getBlock() instanceof SecurityCameraBlock)||state.getBlock() instanceof CrtTvBlock);
            if(device&&!devices.contains(pos))keepBehind(pos);
            var result=endpoint&&(!device||socketShell)?List.<AABB>of():state==null?List.of(new AABB(pos).inflate(CLEARANCE)):
                state.getCollisionShape(level,pos).toAabbs().stream().map(v->v.move(pos).inflate(CLEARANCE)).toList();
            boxes.put(pos,result);return result;
        }
        /** Allocation-free lookup used by the hot collision test; same results as {@link #boxes}. */
        List<AABB> cell(int x,int y,int z){
            if(fitting&&socketCell(new BlockPos(x,y,z)))return List.of();
            long key=BlockPos.asLong(x,y,z);var old=cells.get(key);if(old!=null)return old;
            var result=boxes(new BlockPos(x,y,z));cells.put(key,result);return result;
        }
        private void keepBehind(BlockPos pos){
            BlockState state=level.hasChunkAt(pos)?level.getBlockState(pos):null;watched.put(pos,state);
            if(state==null||!(state.getBlock() instanceof PcDeviceBlock||(state.getBlock() instanceof NesDeviceBlock||state.getBlock() instanceof SnesConsoleBlock||state.getBlock() instanceof N64ConsoleBlock||state.getBlock() instanceof SecurityCameraBlock)||state.getBlock() instanceof CrtTvBlock||state.getBlock() instanceof GeneratorBlock))return;
            var shape=state.getCollisionShape(level,pos);if(shape.isEmpty())return;devices.add(pos);
            var rear=CablePorts.at(pos,CablePorts.spec(state,0,false,false));
            frames.add(new Frame(pos,rear.point(),rear.normal(),(state.getBlock() instanceof NesDeviceBlock||state.getBlock() instanceof SnesConsoleBlock||state.getBlock() instanceof N64ConsoleBlock||state.getBlock() instanceof SecurityCameraBlock)||state.getBlock() instanceof CrtTvBlock?.12:.075));
            double ceiling=level.getMaxBuildHeight()+MARGIN;
            double x=pos.getX()-.04,X=pos.getX()+1.04,z=pos.getZ()-.04,Z=pos.getZ()+1.04;
            double plane=(Math.abs(rear.normal().x)>.5?rear.point().x:rear.point().z)-((state.getBlock() instanceof NesDeviceBlock||state.getBlock() instanceof SnesConsoleBlock||state.getBlock() instanceof N64ConsoleBlock||state.getBlock() instanceof SecurityCameraBlock)||state.getBlock() instanceof CrtTvBlock?.12:.075)*(rear.normal().x+rear.normal().z);
            if(rear.normal().x>.5)X=plane;else if(rear.normal().x<-.5)x=plane;else if(rear.normal().z>.5)Z=plane;else z=plane;
            var socket=pos.equals(first)?firstPort:secondPort;
            if(pos.equals(first)||pos.equals(second))if(socket.kind()==CablePorts.NES_AV){
                // The side jack has a short entry channel in the console shell.
                double sidePlane=(Math.abs(socket.normal().x)>.5?socket.point().x:socket.point().z)-.12*(socket.normal().x+socket.normal().z);
                if(socket.normal().x>.5)X=Math.min(X,sidePlane);else if(socket.normal().x<-.5)x=Math.max(x,sidePlane);else if(socket.normal().z>.5)Z=Math.min(Z,sidePlane);else z=Math.max(z,sidePlane);
            }
            // These columns are checked independently of block-cell traversal. A
            // blocked rear must not make the search climb over a device's front.
            if((pos.equals(first)||pos.equals(second))&&x<X&&z<Z)keepouts.add(new AABB(x,pos.getY(),z,X,ceiling,Z));
            var body=shape.bounds().move(pos).inflate(CLEARANCE);
            keepouts.add(new AABB(body.minX,body.maxY-CLEARANCE*2,body.minZ,body.maxX,ceiling,body.maxZ));
        }
        List<Vec3> rearNodes(Vec3 origin){
            var out=new ArrayList<Vec3>();
            var nearby=devices.stream().sorted(Comparator.comparingDouble(p->Vec3.atCenterOf(p).distanceToSqr(origin))).limit(8).toList();
            for(var pos:nearby){
                var state=watched.get(pos);var rear=CablePorts.at(pos,CablePorts.spec(state,0,false,false));
                var body=state.getCollisionShape(level,pos).bounds().move(pos);
                boolean alongX=Math.abs(rear.normal().x)>.5,positive=rear.normal().x+rear.normal().z>0;
                boolean shell=(pos.equals(first)||pos.equals(second))&&((state.getBlock() instanceof NesDeviceBlock||state.getBlock() instanceof SnesConsoleBlock||state.getBlock() instanceof N64ConsoleBlock||state.getBlock() instanceof SecurityCameraBlock)||state.getBlock() instanceof CrtTvBlock);
                double edge=shell?(alongX?rear.point().x:rear.point().z)-(positive?.12:-.12):alongX?(positive?body.maxX:body.minX):(positive?body.maxZ:body.minZ);
                edge+=(positive?1:-1)*(shell?.001:CLEARANCE+.001);
                for(double side:new double[]{-.065,1.065}){
                    var p=alongX?new Vec3(edge,origin.y,pos.getZ()+side):new Vec3(pos.getX()+side,origin.y,edge);
                    if(clear(p,p))out.add(p);
                }
            }
            return out;
        }
        double frontCost(Vec3 a,Vec3 b){
            double length=a.distanceTo(b);if(length<1e-9)return 0;
            int samples=Math.clamp((int)Math.ceil(length/.5),1,256);double exposure=0;
            for(int i=0;i<samples;i++){
                var p=a.lerp(b,(i+.5)/samples);
                for(var frame:frames){
                    var root=frame.root;
                    if(p.y<root.getY()-1.5||p.y>root.getY()+2)continue;
                    double side=Math.abs(frame.normal.x)>.5?Math.abs(p.z-root.getZ()-.5):Math.abs(p.x-root.getX()-.5);
                    double depth=(p.x-frame.point.x)*frame.normal.x+(p.z-frame.point.z)*frame.normal.z;
                    // Only the device's own footprint and the space just before its face
                    // count. An unbounded corridor sent cords on long detours around rows
                    // of equipment, and a wide one punished cords running along a side wall.
                    if(side<=FRONT_SIDE&&depth< -frame.shell&&depth> -FRONT_DEPTH)exposure+=FRONT_WEIGHT;
                }
            }
            return exposure*length/samples;
        }
        boolean behind(List<Vec3> points){
            for(int i=1;i<points.size();i++)if(frontCost(points.get(i-1),points.get(i))>1e-6)return false;
            return true;
        }
        boolean clear(Vec3 a,Vec3 b){
            if(!fitting)for(var box:keepouts)if(intersects(box,a,b))return false;
            double reach=CLEARANCE*2;
            int x0=(int)Math.floor(Math.min(a.x,b.x)-reach),x1=(int)Math.floor(Math.max(a.x,b.x)+reach);
            int y0=(int)Math.floor(Math.min(a.y,b.y)-reach),y1=(int)Math.floor(Math.max(a.y,b.y)+reach);
            int z0=(int)Math.floor(Math.min(a.z,b.z)-reach),z1=(int)Math.floor(Math.max(a.z,b.z)+reach);
            // Short sections (nearly every rope sample) test each cell that can reach them.
            if((long)(x1-x0+1)*(y1-y0+1)*(z1-z0+1)<=64){
                for(int x=x0;x<=x1;x++)for(int y=y0;y<=y1;y++)for(int z=z0;z<=z1;z++)for(var box:cell(x,y,z))if(intersects(box,a,b))return false;
                return true;
            }
            var delta=b.subtract(a);int x=(int)Math.floor(a.x),y=(int)Math.floor(a.y),z=(int)Math.floor(a.z);
            int ex=(int)Math.floor(b.x),ey=(int)Math.floor(b.y),ez=(int)Math.floor(b.z);
            int sx=Double.compare(delta.x,0),sy=Double.compare(delta.y,0),sz=Double.compare(delta.z,0);
            double tx=crossing(a.x,delta.x,x,sx),ty=crossing(a.y,delta.y,y,sy),tz=crossing(a.z,delta.z,z,sz);
            double dx=sx==0?Double.POSITIVE_INFINITY:Math.abs(1/delta.x),dy=sy==0?Double.POSITIVE_INFINITY:Math.abs(1/delta.y),dz=sz==0?Double.POSITIVE_INFINITY:Math.abs(1/delta.z);
            int steps=Math.abs(ex-x)+Math.abs(ey-y)+Math.abs(ez-z)+4;
            var segment=new AABB(a,b).inflate(CLEARANCE);
            for(int n=0;n<steps;n++){
                // Neighbour cells cover the thickness of the wire and exact corner crossings.
                for(int ox=-1;ox<=1;ox++)for(int oy=-1;oy<=1;oy++)for(int oz=-1;oz<=1;oz++){
                    int cx=x+ox,cy=y+oy,cz=z+oz;
                    if(cx>=segment.maxX+CLEARANCE||cx+1<=segment.minX-CLEARANCE||cy>=segment.maxY+CLEARANCE||cy+1<=segment.minY-CLEARANCE||cz>=segment.maxZ+CLEARANCE||cz+1<=segment.minZ-CLEARANCE)continue;
                    for(var box:cell(cx,cy,cz))if(intersects(box,a,b))return false;
                }
                if(x==ex&&y==ey&&z==ez)return true;
                double next=Math.min(tx,Math.min(ty,tz));
                if(tx<=next+1e-10){x+=sx;tx+=dx;}
                if(ty<=next+1e-10){y+=sy;ty+=dy;}
                if(tz<=next+1e-10){z+=sz;tz+=dz;}
            }
            return false;
        }
        private static double crossing(double p,double d,int cell,int step){return step==0?Double.POSITIVE_INFINITY:((step>0?cell+1:cell)-p)/d;}
        private static boolean intersects(AABB box,Vec3 a,Vec3 b){
            // Include contact at t=0. The ordinary ray clip rejects that hit when
            // a negative-direction segment starts exactly on the box's max face.
            double low=0,high=1;
            for(int axis=0;axis<3;axis++){
                double p=axis==0?a.x:axis==1?a.y:a.z;
                double d=(axis==0?b.x:axis==1?b.y:b.z)-p;
                double min=axis==0?box.minX:axis==1?box.minY:box.minZ;
                double max=axis==0?box.maxX:axis==1?box.maxY:box.maxZ;
                if(Math.abs(d)<1e-12){if(p<min||p>max)return false;continue;}
                double enter=(min-p)/d,leave=(max-p)/d;
                if(enter>leave){double swap=enter;enter=leave;leave=swap;}
                low=Math.max(low,enter);high=Math.min(high,leave);if(low>high)return false;
            }
            return true;
        }
        boolean clear(List<Vec3> points){for(int i=1;i<points.size();i++)if(!clear(points.get(i-1),points.get(i)))return false;return true;}
        Path finish(List<Vec3> points,boolean clear,int searched){return finish(points,clear,searched,false);}
        Path finish(List<Vec3> points,boolean clear,int searched,boolean limited){return new Path(List.copyOf(points),clear,Collections.unmodifiableMap(new HashMap<>(watched)),searched,limited,firstPort,secondPort);}
    }
    public static Path find(Level level,CablePorts.Port a,CablePorts.Port b){
        var query=new Query(level,a,b);
        query.fitting=true;var fittedA=fit(query,a);var fittedB=fit(query,b);query.fitting=false;
        if(fittedA==null||fittedB==null)return query.finish(List.of(),false,0);
        a=fittedA;b=fittedB;query.firstPort=a;query.secondPort=b;
        var natural=natural(a,b);
        // Every cord prefers to stay behind equipment; otherwise the search decides.
        if(a.kind()!=CablePorts.NES_AV&&b.kind()!=CablePorts.NES_AV&&query.clear(natural)&&query.behind(natural))return query.finish(relax(query,natural),true,0);
        var start=a.exit();var end=b.exit();
        // Straight leads keep the wire leaving each plug in its correct direction.
        var firstLead=lead(query,a,end);var secondLead=lead(query,b,start);
        if(firstLead.isEmpty()||secondLead.isEmpty())return query.finish(List.of(),false,0);
        var origin=firstLead.getLast();var goal=secondLead.getLast();
        if(query.clear(origin,goal)&&query.frontCost(origin,goal)<1e-6)return query.finish(relax(query,join(firstLead,List.of(origin,goal),secondLead)),true,0);
        boolean reverse=!query.clear(end,end.add(b.normal().scale(.5)))&&query.clear(start,start.add(a.normal().scale(.5)));
        if(reverse){var swap=origin;origin=goal;goal=swap;}
        var bounds=new AABB(origin,goal).inflate(MARGIN);
        long deadline=System.nanoTime()+25_000_000L;var seeds=seeds(query,origin,deadline);
        // Rear gateways can lie beside neighboring cases. Include enough room
        // to turn around the wall beyond those gateways, even for a side plug.
        for(var seed:seeds)bounds=bounds.minmax(new AABB(seed.point,seed.point).inflate(1.25));
        int totalSearched=0;boolean exhausted=false;
        for(int phase=0;phase<2;phase++){
        if(System.nanoTime()>deadline){exhausted=true;break;}
        long phaseDeadline=phase==0?deadline-5_000_000L:deadline;
        var open=new PriorityQueue<Node>(Comparator.comparingDouble(Node::rank).thenComparingLong(Node::order));
        var costs=new HashMap<Cell,Double>();var parents=new HashMap<Cell,Cell>();var closed=new HashSet<Cell>();long order=0;
        for(int i=0;i<seeds.size();i++){
            var cell=new Cell(i,0,0,0);var seed=seeds.get(i);costs.put(cell,seed.cost);
            open.add(new Node(cell,seed.cost,seed.cost+seed.point.distanceTo(goal)+Math.max(0,goal.y-seed.point.y)*6,order++));
        }
        int searched=0;boolean limited=false;Cell best=null;double bestCost=Double.POSITIVE_INFINITY;
        double ceiling=phase==0?Math.max(origin.y,goal.y)+.08:Double.POSITIVE_INFINITY;
        while(!open.isEmpty()&&searched+totalSearched<(phase==0?MAX_NODES/2:MAX_NODES)){
            if(open.peek().rank>=bestCost)break;
            if((searched&31)==0&&System.nanoTime()>phaseDeadline){limited=true;break;}
            var node=open.remove();if(!closed.add(node.cell))continue;searched++;
            var point=node.cell.point(seeds);
            if((point.distanceToSqr(goal)<4||searched%16==0)&&query.clear(point,goal)){
                double cost=node.cost+point.distanceTo(goal)+Math.max(0,goal.y-point.y)*6+query.frontCost(point,goal);
                if(cost<bestCost){best=node.cell;bestCost=cost;}
            }
            for(int axis=0;axis<3;axis++)for(int direction:new int[]{-1,1}){
                var next=node.cell.offset(axis,direction);if(closed.contains(next))continue;
                var target=next.point(seeds);if(!bounds.contains(target)||target.y>ceiling)continue;
                double rise=Math.max(0,target.y-point.y),height=Math.max(0,target.y-Math.min(origin.y,goal.y));
                double cost=node.cost+STEP+rise*6+height*.15*STEP+query.frontCost(point,target);
                if(cost>=bestCost||cost>=costs.getOrDefault(next,Double.POSITIVE_INFINITY))continue;
                if(!query.clear(point,target))continue;
                costs.put(next,cost);parents.put(next,node.cell);
                open.add(new Node(next,cost,cost+target.distanceTo(goal)+Math.max(0,goal.y-target.y)*6,order++));
            }
        }
        limited|=searched+totalSearched>=(phase==0?MAX_NODES/2:MAX_NODES);
        if(best!=null){
            var route=new ArrayList<Vec3>(seeds.get(best.seed).prefix);var nodes=new ArrayList<Vec3>();Cell at=best;
            while(at!=null){nodes.add(at.point(seeds));at=parents.get(at);}Collections.reverse(nodes);
            route.addAll(nodes.subList(1,nodes.size()));route.add(goal);if(reverse)Collections.reverse(route);
            return query.finish(relax(query,join(firstLead,simplify(query,route),secondLead)),true,searched+totalSearched,limited);
        }
        totalSearched+=searched;exhausted|=limited;
        }
        // Electrical links remain intact when no clear visual route is available.
        return query.finish(List.of(),false,totalSearched,exhausted);
    }
    private static List<Seed> seeds(Query query,Vec3 origin,long deadline){
        var points=new ArrayList<Vec3>();points.add(origin);points.addAll(query.rearNodes(origin));
        int n=points.size();double[] costs=new double[n];Arrays.fill(costs,Double.POSITIVE_INFINITY);costs[0]=0;
        int[] parents=new int[n];Arrays.fill(parents,-1);boolean[] visited=new boolean[n];
        for(int step=0;step<n;step++){
            if(step>0&&System.nanoTime()>deadline)break;
            int at=-1;for(int i=0;i<n;i++)if(!visited[i]&&(at<0||costs[i]<costs[at]))at=i;
            if(at<0||!Double.isFinite(costs[at]))break;visited[at]=true;
            for(int next=1;next<n;next++)if(!visited[next]&&query.clear(points.get(at),points.get(next))){
                double cost=costs[at]+points.get(at).distanceTo(points.get(next))+query.frontCost(points.get(at),points.get(next));
                if(cost<costs[next]){costs[next]=cost;parents[next]=at;}
            }
        }
        var out=new ArrayList<Seed>();
        for(int i=0;i<n;i++)if(Double.isFinite(costs[i])){
            var prefix=new ArrayList<Vec3>();int at=i;while(at>=0){prefix.add(points.get(at));at=parents[at];}Collections.reverse(prefix);
            out.add(new Seed(points.get(i),List.copyOf(prefix),costs[i]));
        }
        return out;
    }
    public static boolean clear(Level level,CablePorts.Port a,CablePorts.Port b,List<Vec3> points){return new Query(level,a,b).clear(points);}
    private static boolean isRetro(CablePorts.Port port){
        return switch(port.kind()){case CablePorts.STAND_TV,CablePorts.WALL_TV,CablePorts.CAMERA,CablePorts.N64,CablePorts.N64_AV,CablePorts.SNES,CablePorts.SNES_AV,CablePorts.NES,CablePorts.NES_AV,CablePorts.RETRO,CablePorts.RETRO_AV,CablePorts.RETRO_COAX,CablePorts.TV,CablePorts.TV_COAX->true;default->false;};
    }
    /** Short signal leads are bought to length; mains, coax and feed cords carry spare cord. */
    private static boolean signal(CablePorts.Port port){
        return switch(port.kind()){case CablePorts.PC_HDMI,CablePorts.MONITOR_HDMI,CablePorts.NES_AV,CablePorts.RETRO_AV,CablePorts.SNES_AV,CablePorts.N64_AV->true;default->false;};
    }
    private static CablePorts.Port fit(Query query,CablePorts.Port port){
        // A full-length connector can protrude into a newly placed rear block.
        // Keep the socket position whenever possible and shorten the visual plug.
        boolean retro=isRetro(port);
        for(double room:retro?new double[]{.08,.06,.04,.02,0}:new double[]{0})for(double inset:new double[]{0,.04,.08,.12})for(double compression:new double[]{1,.75,.5,.35,.25,.15}){
            var fitted=new CablePorts.Port(port.point().subtract(port.normal().scale(inset)),port.normal(),port.kind(),port.slot(),port.compression()*compression);
            if(!query.clear(fitted.point(),fitted.exit()))continue;
            if(retro){
                query.fitting=false;boolean turningRoom;
                try{turningRoom=query.clear(fitted.exit(),fitted.exit().add(port.normal().scale(room)));}finally{query.fitting=true;}
                if(!turningRoom)continue;
            }
            return fitted;
        }
        return null;
    }
    private static List<Vec3> join(List<Vec3> first,List<Vec3> middle,List<Vec3> second){
        var points=new ArrayList<>(first);points.addAll(middle);
        for(int i=second.size()-1;i>=0;i--)points.add(second.get(i));return points;
    }
    private static List<Vec3> lead(Query query,CablePorts.Port port,Vec3 other){
        if(port.kind()==CablePorts.NES_AV){
            // Guide the real side jack towards the back before crossing to the
            // display. Starting the search here also resolves narrow rear gaps.
            var point=port.exit();var rear=port.normal().cross(new Vec3(0,-1,0));
            var root=port==query.firstPort?query.first:query.second;
            var back=CablePorts.at(root,(port.kind()==CablePorts.NES_AV?CablePorts.NES:port.kind())|((rear.z>.5?net.minecraft.core.Direction.NORTH:rear.x<-.5?net.minecraft.core.Direction.EAST:rear.z<-.5?net.minecraft.core.Direction.SOUTH:net.minecraft.core.Direction.WEST).get3DDataValue()<<5));
            double depth=Math.max(.2,back.point().subtract(point).dot(rear)+.04);
            for(double stem:new double[]{.18,.10,.04,.01,0}){
                var anchor=point.add(port.normal().scale(stem)).add(rear.scale(depth));
                var escape=cubic(point,point.add(port.normal().scale(stem)),anchor.subtract(rear.scale(.18)),anchor,24);
                if(query.clear(escape))return escape;
            }
        }
        var bend=turn(port,other);
        if(bend.points.size()>1&&query.clear(bend.points))return bend.points;
        // Leave enough space for a rounded elbow rather than reversing on a short stem.
        var point=port.exit();var normal=port.normal();
        for(double length:new double[]{.30,.20,.10,.06,.03,.01,0}){
            var end=point.add(normal.scale(length));if(!query.clear(point,end))continue;
            if(length<.1){
                var side=side(normal,other.subtract(point));
                for(var direction:List.of(side,side.scale(-1),new Vec3(0,1,0),new Vec3(0,-1,0))){
                    if(Math.abs(direction.dot(normal))>.1)continue;
                    var anchor=end.add(direction.scale(.28));
                    var escape=cubic(point,end,anchor.subtract(direction.scale(.14)),anchor,16);
                    if(query.clear(escape))return escape;
                }
            }
            return List.of(point,end);
        }
        return List.of();
    }
    private static List<Vec3> simplify(Query query,List<Vec3> input){
        var out=new ArrayList<Vec3>();int i=0;out.add(input.getFirst());
        while(i<input.size()-1){int next=input.size()-1;while(next>i+1){
                double existing=0;for(int j=i+1;j<=next;j++)existing+=input.get(j-1).distanceTo(input.get(j))+query.frontCost(input.get(j-1),input.get(j));
                double shortcut=input.get(i).distanceTo(input.get(next))+query.frontCost(input.get(i),input.get(next));
                if(shortcut<=existing+1e-6&&query.clear(input.get(i),input.get(next)))break;next--;
            }out.add(input.get(next));i=next;}
        return out;
    }
    private static double length(List<Vec3> points){double length=0;for(int i=1;i<points.size();i++)length+=points.get(i-1).distanceTo(points.get(i));return length;}
    /**
     * Turns a clear guide (natural curve or searched route) into a hanging cord: a chain
     * of equal-mass samples with inextensible links of fixed total length, settled under
     * gravity with bending stiffness. Every individual move is collision-checked, so the
     * result keeps full clearance; endpoints and plug directions are fixed.
     */
    private static List<Vec3> relax(Query query,List<Vec3> input){
        var guide=new ArrayList<Vec3>();
        for(var p:input)if(guide.isEmpty()||guide.getLast().distanceToSqr(p)>1e-10)guide.add(p);
        if(guide.size()>1)guide.set(guide.size()-1,input.getLast());
        if(guide.size()<2||!query.clear(guide))return input;
        double length=length(guide);if(length<.06)return guide;
        double chord=guide.getFirst().distanceTo(guide.getLast());
        double slack=signal(query.firstPort)||signal(query.secondPort)?Math.clamp(chord*.08,.03,.5):Math.clamp(chord*.3,.2,1.5);
        double ceiling=guide.stream().mapToDouble(p->p.y).max().orElse(0)+1e-6;
        // Settle a coarse chain first so the overall sag converges quickly, then refine.
        int segments=(int)Math.ceil(length/Math.max(.075,length/160));
        int levels=segments>=24?2:segments>=10?1:0;
        var rope=new Rope(query,resample(query,guide,length/Math.max(2,Math.ceil(segments/(double)(1<<levels)))),length+slack,ceiling);
        for(int level=0;;level++){
            rope.settle(level==levels?(levels==0?120:30):level==0?110:40);
            if(level==levels)break;
            rope.refine();
        }
        rope.respace();rope.straighten(24);
        var points=rope.points();var smooth=upsample(query,points,rope.startNormal,rope.endNormal,ceiling);
        if(smooth!=points&&query.clear(smooth))return smooth;
        return query.clear(points)?points:guide;
    }
    /** Even samples along the guide, keeping the guide's own corners wherever a chord would cut one. */
    private static List<Vec3> resample(Query query,List<Vec3> guide,double spacing){
        int m=guide.size();double[] at=new double[m];
        for(int i=1;i<m;i++)at[i]=at[i-1]+guide.get(i-1).distanceTo(guide.get(i));
        double total=at[m-1];int count=Math.max(1,(int)Math.round(total/spacing));
        var out=new ArrayList<Vec3>();out.add(guide.getFirst());double placed=0;int segment=1;
        for(int k=1;k<=count;k++){
            double s=k==count?total:total*k/count;
            while(segment<m-1&&at[segment]<s)segment++;
            double span=at[segment]-at[segment-1];
            var point=k==count?guide.getLast():guide.get(segment-1).lerp(guide.get(segment),span<1e-12?0:(s-at[segment-1])/span);
            if(!query.clear(out.getLast(),point))for(int j=1;j<m-1;j++)if(at[j]>placed+1e-9&&at[j]<s-1e-9&&out.getLast().distanceToSqr(guide.get(j))>1e-10)out.add(guide.get(j));
            if(out.size()==1||out.getLast().distanceToSqr(point)>1e-10)out.add(point);else if(k==count)out.set(out.size()-1,point);
            placed=s;
        }
        return out;
    }
    /** Interpolating four-point subdivision for a rounder tube; any unclear midpoint stays on the chord. */
    private static List<Vec3> upsample(Query query,List<Vec3> points,Vec3 startNormal,Vec3 endNormal,double ceiling){
        int n=points.size();if(n<3||n>160)return points;
        var out=new ArrayList<Vec3>(n*2);out.add(points.getFirst());
        for(int i=0;i<n-1;i++){
            var a=points.get(i);var b=points.get(i+1);double span=a.distanceTo(b);
            // The plugs supply the outer tangents, so the cord leaves each one straight.
            var before=i>0?points.get(i-1):a.subtract(startNormal.scale(span));
            var after=i+2<n?points.get(i+2):b.subtract(endNormal.scale(span));
            var mid=a.add(b).scale(9/16d).subtract(before.add(after).scale(1/16d));
            if(mid.y>ceiling&&mid.y>Math.max(a.y,b.y)||!query.clear(a,mid)||!query.clear(mid,b))mid=a.add(b).scale(.5);
            out.add(mid);out.add(b);
        }
        return out;
    }
    /** Lowest point within reach {@code ra} of {@code a} and {@code rb} of {@code b}: where a free sample hangs. */
    private static Vec3 lowest(Vec3 a,double ra,Vec3 b,double rb,Vec3 at){
        var lowA=a.add(0,-ra,0);var lowB=b.add(0,-rb,0);
        boolean aInB=lowA.distanceTo(b)<=rb,bInA=lowB.distanceTo(a)<=ra;
        if(aInB&&bInA)return lowA.y<lowB.y?lowA:lowB;
        if(aInB)return lowA;if(bInA)return lowB;
        var axis=b.subtract(a);double d=axis.length();
        if(d<1e-9)return lowA;
        // Over-stretched (only possible while pressed against an obstacle): pull taut.
        if(d>=ra+rb)return a.add(axis.scale(ra/(ra+rb)));
        var u=axis.scale(1/d);double s=(d*d+ra*ra-rb*rb)/(2*d);var centre=a.add(u.scale(s));
        double radius=Math.sqrt(Math.max(0,ra*ra-s*s));
        var down=new Vec3(0,-1,0).add(u.scale(u.y));
        if(down.lengthSqr()<1e-10){
            // Vertical links: the circle is level, so stay on the current side.
            down=at.subtract(centre);down=down.subtract(u.scale(down.dot(u)));
            if(down.lengthSqr()<1e-10)return centre;
        }
        return centre.add(down.normalize().scale(radius));
    }
    /** A chain of equal-mass samples joined by inextensible links. Every move keeps the cord clear. */
    private static final class Rope {
        final Query query;final double ceiling;final Vec3 startNormal,endNormal;
        Vec3[] p;double[] rest;int[] stamp;int sweep,moves;double stretch=1;
        Rope(Query query,List<Vec3> points,double total,double ceiling){
            this.query=query;this.ceiling=ceiling;startNormal=query.firstPort.normal();endNormal=query.secondPort.normal();
            p=points.toArray(new Vec3[0]);rest=new double[p.length-1];stamp=new int[p.length];double sum=0;
            for(int i=0;i<rest.length;i++){rest[i]=p[i].distanceTo(p[i+1]);sum+=rest[i];}
            // Guide corners kept by resampling can sit just beside a sample. A link sized
            // from that tiny chord stayed tiny through every refinement and bunched samples
            // into needle-sharp kinks, so short links get a normal share of the cord.
            double floor=.6*sum/rest.length,padded=0;
            for(int i=0;i<rest.length;i++){rest[i]=Math.max(rest[i],floor);padded+=rest[i];}
            // The spare length is spread evenly; gravity and sliding decide where it ends up.
            double scale=padded>1e-9?Math.max(1,total/padded):1;
            for(int i=0;i<rest.length;i++)rest[i]*=scale;
        }
        List<Vec3> points(){return List.of(p);}
        void settle(int sweeps){
            for(int s=0;s<sweeps;s++){
                sweep++;boolean forward=(sweep&1)==0;int n=p.length;
                // A sample whose neighbourhood has not moved would reach the same result again.
                for(int k=1;k<n-1;k++){int i=forward?k:n-1-k;if(stamp[i-1]>=sweep-1||stamp[i]>=sweep-1||stamp[i+1]>=sweep-1)fall(i);}
                bend(forward);
            }
        }
        /**
         * Final stiffness passes: removes kinks gravity leaves at supports and plugs. A taut
         * cord wrapped over a block edge cannot round it without a little extra length, so
         * links may stretch slightly here, giving a broad clear bend instead of a fold.
         */
        void straighten(int passes){
            stretch=STRETCH;
            for(int s=0;s<passes;s++){int before=moves;sweep++;bend((sweep&1)==0);if(moves==before)break;}
            stretch=1;
        }
        /** Drops samples bunched closer than about half a link, where the shortcut stays clear. */
        void respace(){
            double mean=0;for(double r:rest)mean+=r;mean/=rest.length;double min=.45*mean;
            var points=new ArrayList<Vec3>();var lengths=new ArrayList<Double>();points.add(p[0]);double carried=0;
            for(int i=1;i<p.length;i++){
                carried+=rest[i-1];
                if(i<p.length-1){
                    var last=points.getLast();
                    boolean bunched=last.distanceTo(p[i])<min||i+1==p.length-1&&p[i].distanceTo(p[i+1])<min;
                    if(bunched&&query.clear(last,p[i+1]))continue;
                }
                points.add(p[i]);lengths.add(carried);carried=0;
            }
            if(points.size()==p.length)return;
            p=points.toArray(new Vec3[0]);rest=new double[lengths.size()];for(int i=0;i<rest.length;i++)rest[i]=lengths.get(i);
            stamp=new int[p.length];Arrays.fill(stamp,sweep);
        }
        void refine(){
            int n=p.length;var points=new Vec3[n*2-1];var lengths=new double[n*2-2];
            for(int i=0;i<n-1;i++){points[i*2]=p[i];points[i*2+1]=p[i].add(p[i+1]).scale(.5);lengths[i*2]=lengths[i*2+1]=rest[i]*.5;}
            points[n*2-2]=p[n-1];p=points;rest=lengths;stamp=new int[p.length];Arrays.fill(stamp,sweep);
        }
        private void fall(int i){
            var at=p[i];var delta=lowest(p[i-1],rest[i-1],p[i+1],rest[i],at).subtract(at);
            if(delta.lengthSqr()<1e-10)return;
            for(double f:FALL)if(move(i,at.add(delta.scale(f))))return;
            // Resting on a surface: sink straight down, or slide where the cord pulls.
            if(delta.y<0)for(double f:SLIDE)if(move(i,at.add(0,delta.y*f,0)))return;
            var flat=new Vec3(delta.x,0,delta.z);
            if(flat.lengthSqr()>1e-8)for(double f:SLIDE)if(move(i,at.add(flat.scale(f))))return;
        }
        private void bend(boolean forward){
            int n=p.length;if(n<3)return;
            stem(1,0,startNormal);stem(n-2,n-1,endNormal);
            for(int k=1;k<n-1;k++){
                int i=forward?k:n-1-k;var a=p[i-1];var at=p[i];var b=p[i+1];
                var u=at.subtract(a);var v=b.subtract(at);double lu=u.length(),lv=v.length();
                if(lu<1e-6||lv<1e-6||u.dot(v)>=BEND_COS*lu*lv)continue;
                // Ease the corner itself; where it is pressed on an edge, swing a neighbour
                // onto the bisector instead so the cord wraps rather than folds.
                var mid=a.add(b).scale(.5);
                if(move(i,at.lerp(mid,.5))||move(i,at.lerp(mid,.25)))continue;
                // Half a turn first, then a quarter, which needs far less spare length.
                var incoming=u.scale(1/lu);var outgoing=v.scale(1/lv);boolean eased=false;
                if(i+1<n-1)for(double w:SWING){var dir=outgoing.scale(1-w).add(incoming.scale(w));if(dir.lengthSqr()>1e-6&&move(i+1,at.add(dir.normalize().scale(lv)))){eased=true;break;}}
                if(!eased&&i-1>0)for(double w:SWING){var dir=incoming.scale(1-w).add(outgoing.scale(w));if(dir.lengthSqr()>1e-6&&move(i-1,at.subtract(dir.normalize().scale(lu))))break;}
            }
        }
        /** Rotates the sample beside a plug towards the plug's axis. */
        private void stem(int i,int anchor,Vec3 normal){
            if(i<1||i>p.length-2)return;
            var offset=p[i].subtract(p[anchor]);double len=offset.length();if(len<1e-6)return;
            var out=offset.scale(1/len);double cos=out.dot(normal);if(cos>=STEM_COS||cos<-.9)return;
            move(i,p[anchor].add(out.add(normal).normalize().scale(len)));
        }
        private boolean move(int i,Vec3 to){
            var at=p[i];if(to.y>ceiling&&to.y>at.y)return false;
            if(!fits(at,to,p[i-1],rest[i-1])||!fits(at,to,p[i+1],rest[i]))return false;
            if(!query.clear(p[i-1],to)||!query.clear(to,p[i+1]))return false;
            p[i]=to;stamp[i]=sweep;moves++;return true;
        }
        /** Links never stretch past their length (bar the final easing), nor bunch into near-zero samples. */
        private boolean fits(Vec3 at,Vec3 to,Vec3 other,double rest){
            double now=at.distanceTo(other),next=to.distanceTo(other);
            return next<=Math.max(rest*stretch,now)+1e-9&&next>=Math.min(rest*.5,now)-1e-9;
        }
    }
    private record Turn(List<Vec3> points,Vec3 anchor,Vec3 normal){}
    private static Vec3 side(Vec3 normal,Vec3 toward){
        if(Math.abs(normal.y)<.5)toward=new Vec3(toward.x,0,toward.z);
        var side=toward.subtract(normal.scale(toward.dot(normal)));
        if(side.lengthSqr()<.01)side=normal.cross(new Vec3(0,1,0));
        if(side.lengthSqr()<.01)side=normal.cross(new Vec3(1,0,0));return side.normalize();
    }
    private static List<Vec3> cubic(Vec3 start,Vec3 ca,Vec3 cb,Vec3 end,int segments){
        var out=new ArrayList<Vec3>(segments+1);
        for(int i=0;i<=segments;i++){double t=i/(double)segments,u=1-t;out.add(i==0?start:i==segments?end:start.scale(u*u*u).add(ca.scale(3*u*u*t)).add(cb.scale(3*u*t*t)).add(end.scale(t*t*t)));}return out;
    }
    private static Turn turn(CablePorts.Port port,Vec3 other){
        var start=port.exit();var toward=other.subtract(start);var normal=port.normal();
        if(toward.lengthSqr()<.01||normal.dot(toward.normalize())>-.6)return new Turn(List.of(start),start,normal);
        // A socket facing away from the other device needs a broad return bend.
        // A single cubic with collinear controls otherwise develops a cusp here.
        double width=Math.min(.4,Math.max(.28,toward.length()*.07));var end=start.add(side(normal,toward).scale(width));
        var points=cubic(start,start.add(normal.scale(width*2/3)),end.add(normal.scale(width*2/3)),end,24);
        return new Turn(points,end,normal.scale(-1));
    }
    public static List<Vec3> natural(CablePorts.Port a,CablePorts.Port b){
        var first=turn(a,b.exit());var second=turn(b,a.exit());
        var start=first.anchor;var end=second.anchor;double distance=start.distanceTo(end),lead=Math.min(.7,Math.max(.2,distance*.15));
        var ca=start.add(first.normal.scale(lead));var cb=end.add(second.normal.scale(lead));
        int segments=Math.clamp((int)Math.ceil(distance*12),32,192);var points=new ArrayList<Vec3>(segments+1);
        for(int i=0;i<=segments;i++){double t=(double)i/segments,u=1-t;
            var p=start.scale(u*u*u).add(ca.scale(3*u*u*t)).add(cb.scale(3*u*t*t)).add(end.scale(t*t*t));
            double sag=Math.min(.4,distance*.1)*16*t*t*u*u;
            double floor=Math.min(Math.floor(a.point().y),Math.floor(b.point().y))+.055;
            double height=p.y-sag,delta=height-floor,blend=.08*Math.sin(Math.PI*t);
            // Smooth contact with the floor avoids a kink where sag used to clamp.
            height=floor+.5*(delta+Math.sqrt(delta*delta+blend*blend));
            points.add(i==0?start:i==segments?end:new Vec3(p.x,height,p.z));
        }
        var result=new ArrayList<>(first.points);result.addAll(points.subList(1,points.size()));
        for(int i=second.points.size()-2;i>=0;i--)result.add(second.points.get(i));return result;
    }
}
