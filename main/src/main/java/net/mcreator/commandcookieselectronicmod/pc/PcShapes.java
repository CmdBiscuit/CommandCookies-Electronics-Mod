package net.mcreator.commandcookieselectronicmod.pc;

import java.util.EnumMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PcShapes {
    private static final EnumMap<Direction, VoxelShape> PC = new EnumMap<>(Direction.class);
    private static final EnumMap<Direction, VoxelShape> MONITOR = new EnumMap<>(Direction.class);
    private static final EnumMap<Direction, VoxelShape> MONITOR_REAR = new EnumMap<>(Direction.class);

    static {
        // The closed case is solid; its four rubber feet leave the underside open.
        cache(PC, union(
            Block.box(3, 1, 2, 13, 15, 14),
            Block.box(3.6, 0, 3, 4.6, 1, 4.5),
            Block.box(3.6, 0, 11.8, 4.6, 1, 13.3),
            Block.box(11.4, 0, 3, 12.4, 1, 4.5),
            Block.box(11.4, 0, 11.8, 12.4, 1, 13.3)
        ));

        // Follow the exterior parts, leaving the space behind the panel empty.
        VoxelShape monitor = union(
            Block.box(.5, 4.95, 7.49, 15.5, 13.3, 8.65), // frame and screen
            Block.box(.8, 13.3, 7.6, 15.2, 13.6, 8.6), // top bevel
            Block.box(.8, 4.5, 7.49, 15.2, 4.95, 8.6), // lower chin
            Block.box(3, 6.35, 8.65, 13, 11.85, 9.25), // rear housing
            Block.box(1.3, 5, 8.65, 14.7, 6.15, 8.78), // connector strip
            Block.box(6.3, 7.3, 9.25, 9.7, 9.6, 9.6), // mounting plate
            Block.box(7.2, .95, 9.05, 8.8, 8.45, 10.1), // stand
            Block.box(4.5, .12, 6.3, 11.5, .72, 11.5), // base
            Block.box(4.8, .72, 6.6, 11.2, .95, 11.2), // raised base surface
            Block.box(4.75, 0, 6.6, 5.75, .12, 11.2), // left rubber pad
            Block.box(10.25, 0, 6.6, 11.25, .12, 11.2) // right rubber pad
        );
        cache(MONITOR, monitor);
        cache(MONITOR_REAR, monitor.move(0, 0, 3.0 / 16));
    }

    private static VoxelShape union(VoxelShape... parts) {
        VoxelShape result = Shapes.empty();
        for (VoxelShape part : parts) result = Shapes.or(result, part);
        return result.optimize();
    }

    private static void cache(EnumMap<Direction, VoxelShape> map, VoxelShape shape) {
        for (Direction facing : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            map.put(facing, shape);
            VoxelShape[] next = {Shapes.empty()};
            shape.forAllBoxes((x, y, z, X, Y, Z) ->
                next[0] = Shapes.or(next[0], Shapes.box(1 - Z, y, x, 1 - z, Y, X)));
            shape = next[0].optimize();
        }
    }

    public static VoxelShape shape(boolean monitor, Direction facing) {
        return shape(monitor, facing, false);
    }

    public static VoxelShape shape(boolean monitor, Direction facing, boolean keyboard) {
        return (monitor ? (keyboard ? MONITOR_REAR : MONITOR) : PC).get(facing);
    }
}
