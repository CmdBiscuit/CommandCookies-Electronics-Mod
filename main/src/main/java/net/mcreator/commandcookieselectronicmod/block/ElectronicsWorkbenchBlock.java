package net.mcreator.commandcookieselectronicmod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.mcreator.commandcookieselectronicmod.block.entity.ElectronicsWorkbenchBlockEntity;

public class ElectronicsWorkbenchBlock extends BaseEntityBlock {
    public static final MapCodec<ElectronicsWorkbenchBlock> CODEC = simpleCodec(p -> new ElectronicsWorkbenchBlock());
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    // Six coarse, integer-aligned volumes keep outlines and collision inexpensive.
    // Small tools, handles and decorations use their supporting surface's hitbox.
    // Model coordinates face south, matching the blockstate model rotations.
    private static final double[][] MODEL_BOXES={
        {0,10,0,16,11,16},
        {1,0,1,3,10,3},
        {1,0,13,3,10,15},
        {9,0,1,15,10,15},
        {3,2,3,9,5,13},
        {1,11,1,15,16,5}
    };
    private static final net.minecraft.world.phys.shapes.VoxelShape SOUTH_SHAPE=makeShape(0),WEST_SHAPE=makeShape(1),NORTH_SHAPE=makeShape(2),EAST_SHAPE=makeShape(3);
    private static net.minecraft.world.phys.shapes.VoxelShape makeShape(int turns) {
        var result=net.minecraft.world.phys.shapes.Shapes.empty();
        for(var b:MODEL_BOXES) {
            double minX=b[0],minZ=b[2],maxX=b[3],maxZ=b[5];
            for(int i=0;i<turns;i++){double oldMinX=minX,oldMaxX=maxX,oldMinZ=minZ;minX=16-maxZ;maxX=16-oldMinZ;minZ=oldMinX;maxZ=oldMaxX;}
            result=net.minecraft.world.phys.shapes.Shapes.or(result,Block.box(minX,b[1],minZ,maxX,b[4],maxZ));
        }
        return result.optimize();
    }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context) {
        return switch(state.getValue(FACING)){case SOUTH->SOUTH_SHAPE;case WEST->WEST_SHAPE;case NORTH->NORTH_SHAPE;default->EAST_SHAPE;};
    }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context) {
        return getShape(state,level,pos,context);
    }
    public ElectronicsWorkbenchBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(2.5f,6f).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite()); }
    @Override public BlockState rotate(BlockState s,Rotation r) { return s.setValue(FACING,r.rotate(s.getValue(FACING))); }
    @Override public BlockState mirror(BlockState s,Mirror m) { return s.rotate(m.getRotation(s.getValue(FACING))); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s) { return new ElectronicsWorkbenchBlockEntity(p,s); }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof ElectronicsWorkbenchBlockEntity bench)
            server.openMenu(bench,pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if (player.isShiftKeyDown()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        useWithoutItem(s,level,pos,player,hit);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState old,Level level,BlockPos pos,BlockState next,boolean moving) {
        if (!old.is(next.getBlock()) && level.getBlockEntity(pos) instanceof ElectronicsWorkbenchBlockEntity bench) {
            Containers.dropContents(level,pos,bench);
            level.updateNeighbourForOutputSignal(pos,this);
        }
        super.onRemove(old,level,pos,next,moving);
    }
}
