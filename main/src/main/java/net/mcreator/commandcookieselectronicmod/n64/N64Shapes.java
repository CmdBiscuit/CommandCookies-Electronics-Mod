package net.mcreator.commandcookieselectronicmod.n64;
import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.*;

/** Three coarse case volumes, precomputed for each horizontal rotation. */
public final class N64Shapes {
    private static final Map<Direction,VoxelShape> SHAPES=new EnumMap<>(Direction.class);
    static {
        double[][] boxes={{1,0,2.25,15,3,13.75},{1.5,3,2.5,14.5,4.1,13.4},{4,4.1,4,12,4.7,11.5}};
        for(var facing:Direction.Plane.HORIZONTAL){
            int turns=switch(facing){case NORTH->0;case EAST->1;case SOUTH->2;default->3;};
            var shape=Shapes.empty();
            for(var values:boxes){
                var b=values.clone();
                for(int i=0;i<turns;i++)b=new double[]{16-b[5],b[1],b[0],16-b[2],b[4],b[3]};
                shape=Shapes.or(shape,Shapes.create(new AABB(b[0]/16,b[1]/16,b[2]/16,b[3]/16,b[4]/16,b[5]/16)));
            }
            SHAPES.put(facing,shape.optimize());
        }
    }
    public static VoxelShape shape(Direction facing){return SHAPES.get(facing);}
    private N64Shapes(){}
}
