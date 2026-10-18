package net.mcreator.commandcookieselectronicmod.snes;
import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.*;

/** Three coarse case volumes, precomputed for each horizontal rotation. */
public final class SnesShapes {
    private static final Map<Direction,VoxelShape> SHAPES=new EnumMap<>(Direction.class);
    static {
        double[][] boxes={{1,0,1.5,15,3.25,14.22},{1.5,3.25,2.25,14.5,4,13.5},{4,4,3.75,12,4.8,12.75}};
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
    private SnesShapes(){}
}
