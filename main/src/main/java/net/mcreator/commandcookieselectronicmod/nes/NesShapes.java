package net.mcreator.commandcookieselectronicmod.nes;
import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.*;

/** Built once: three monitor boxes and one console box, never rebuilt per query. */
public final class NesShapes {
    private static final double[][] MONITOR={{1,3.15,1.31,15,15.8,15.5},{5.3,1.3,6.3,10.7,4.4,10.65},{3.4,0,3.75,12.6,1.3,11.5}};
    private static final double[][] CONSOLE={{.5,0,1.5,15,6.5,14.5}};
    private static final Map<Direction,VoxelShape> M=new EnumMap<>(Direction.class),N=new EnumMap<>(Direction.class);
    private static final Map<Direction,List<AABB>> ML=new EnumMap<>(Direction.class),NL=new EnumMap<>(Direction.class);
    static{for(Direction d:Direction.Plane.HORIZONTAL){build(d,MONITOR,M,ML);build(d,CONSOLE,N,NL);}}
    private static void build(Direction facing,double[][] src,Map<Direction,VoxelShape> shapes,Map<Direction,List<AABB>> outlines){
        int turns=switch(facing){case NORTH->0;case EAST->1;case SOUTH->2;default->3;};var boxes=new ArrayList<AABB>();var shape=Shapes.empty();
        for(double[] values:src){double[] b=values.clone();for(int t=0;t<turns;t++)b=new double[]{16-b[5],b[1],b[0],16-b[2],b[4],b[3]};
            var box=new AABB(b[0]/16,b[1]/16,b[2]/16,b[3]/16,b[4]/16,b[5]/16);boxes.add(box);shape=Shapes.or(shape,Shapes.create(box));}
        shapes.put(facing,shape.optimize());outlines.put(facing,List.copyOf(boxes));
    }
    public static VoxelShape shape(boolean monitor,Direction facing){return (monitor?M:N).get(facing);}
    public static List<AABB> outline(boolean monitor,Direction facing){return (monitor?ML:NL).get(facing);}
}
