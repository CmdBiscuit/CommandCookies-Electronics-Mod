package net.mcreator.commandcookieselectronicmod.block;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

public class GamingChairLightBlueBlock extends Block {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // Same component boxes as the editable MCreator bounding-box settings.
    private static final double[][] CHAIR_BOXES = {
        {1.975, 6.8, 3, 13.975, 9.3, 12.4},
        {2.475, 8.2, 11.3, 13.475, 20.9, 13.3},
        {2.975, 20.5, 10.95, 12.975, 24.9, 13.65},
        {14.225, 7, 6.5, 15.075, 12.55, 8.2},
        {13.625, 12.35, 4.85, 15.625, 13.9, 9.8},
        {0.925, 7, 6.5, 1.775, 12.55, 8.2},
        {0.325, 12.35, 4.85, 2.325, 13.9, 9.8},
        {5.975, 5.4, 6.1, 9.975, 6.8, 9.8},
        {7.125, 2.85, 7.15, 8.825, 5.8, 8.85},
        {2.38885, 1.35, 11.74767, 4.22733, 2.55, 13.58614},
        {3.23738, 1.35, 10.89914, 5.07586, 2.55, 12.73762},
        {4.08591, 1.35, 10.05061, 5.92439, 2.55, 11.88909},
        {4.93444, 1.35, 9.20208, 6.77292, 2.55, 11.04056},
        {5.78297, 1.35, 8.35355, 7.62144, 2.55, 10.19203},
        {6.6315, 1.35, 7.50503, 8.46997, 2.55, 9.3435},
        {7.48002, 1.35, 6.6565, 9.3185, 2.55, 8.49497},
        {8.32855, 1.35, 5.80797, 10.16703, 2.55, 7.64645},
        {9.17708, 1.35, 4.95944, 11.01556, 2.55, 6.79792},
        {10.02561, 1.35, 4.11091, 11.86409, 2.55, 5.94939},
        {10.87414, 1.35, 3.26238, 12.71261, 2.55, 5.10086},
        {11.72266, 1.35, 2.41386, 13.56114, 2.55, 4.25233},
        {2.38885, 1.35, 2.41386, 4.22733, 2.55, 4.25233},
        {3.23738, 1.35, 3.26238, 5.07586, 2.55, 5.10086},
        {4.08591, 1.35, 4.11091, 5.92439, 2.55, 5.94939},
        {4.93444, 1.35, 4.95944, 6.77292, 2.55, 6.79792},
        {5.78297, 1.35, 5.80797, 7.62144, 2.55, 7.64645},
        {6.6315, 1.35, 6.6565, 8.46997, 2.55, 8.49497},
        {7.48002, 1.35, 7.50503, 9.3185, 2.55, 9.3435},
        {8.32855, 1.35, 8.35355, 10.16703, 2.55, 10.19203},
        {9.17708, 1.35, 9.20208, 11.01556, 2.55, 11.04056},
        {10.02561, 1.35, 10.05061, 11.86409, 2.55, 11.88909},
        {10.87414, 1.35, 10.89914, 12.71261, 2.55, 12.73762},
        {11.72266, 1.35, 11.74767, 13.56114, 2.55, 13.58614},
        {6.625, 2.55, 6.65, 9.325, 2.9, 9.35},
        {11.975, 0, 12, 13.575, 1.5, 13.6},
        {2.375, 0, 12, 3.975, 1.5, 13.6},
        {11.975, 0, 2.4, 13.575, 1.5, 4},
        {2.375, 0, 2.4, 3.975, 1.5, 4}
    };
    private static final VoxelShape[] CHAIR_SHAPES = makeChairShapes();

    private static VoxelShape[] makeChairShapes() {
        VoxelShape[] shapes = new VoxelShape[4];
        for (int turn = 0; turn < 4; turn++) {
            VoxelShape shape = Shapes.empty();
            for (double[] b : CHAIR_BOXES) {
                double x1 = b[0], z1 = b[2], x2 = b[3], z2 = b[5];
                for (int step = 0; step < turn; step++) {
                    double oldX1 = x1, oldX2 = x2;
                    x1 = 16 - z2; x2 = 16 - z1;
                    z1 = oldX1; z2 = oldX2;
                }
                shape = Shapes.or(shape, Block.box(x1, b[1], z1, x2, b[4], z2));
            }
            shapes[turn] = shape.optimize();
        }
        return shapes;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return CHAIR_SHAPES[switch (state.getValue(FACING)) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        }];
    }

	public GamingChairLightBlueBlock() {
		super(BlockBehaviour.Properties.of().strength(1f, 10f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
		return true;
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 0;
	}

	@Override
	public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		if (state == null)
			return null;
		return state.setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	public BlockState rotate(BlockState state, Rotation rot) {
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}

	public BlockState mirror(BlockState state, Mirror mirrorIn) {
		return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
	}
}