package net.mcreator.commandcookieselectronicmod.nes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public abstract class NesDeviceBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED=BlockStateProperties.POWERED;
    public static final BooleanProperty GAME_LOADED=BooleanProperty.create("game_loaded");
    public final boolean monitor;
    protected NesDeviceBlock(boolean monitor){
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.5f,4f).noOcclusion().isRedstoneConductor((s,w,p)->false));this.monitor=monitor;
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(POWERED,false).setValue(GAME_LOADED,false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,POWERED,GAME_LOADED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new NesDeviceBlockEntity(p,s);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter w,BlockPos p,CollisionContext c){return NesShapes.shape(monitor,s.getValue(FACING));}
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter w,BlockPos p,CollisionContext c){return NesShapes.shape(monitor,s.getValue(FACING));}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!level.isClientSide)NesSystem.interact(level,pos,player,hand);return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(!level.isClientSide)NesSystem.interact(level,pos,player,InteractionHand.MAIN_HAND);return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide?null:createTickerHelper(type,NesSystemRegistration.DEVICE_TYPE,NesDeviceBlockEntity::tick);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())){net.mcreator.commandcookieselectronicmod.power.PowerNetwork.forget(level,pos);if(!level.isClientSide&&level.getBlockEntity(pos) instanceof NesDeviceBlockEntity d)NesSystem.destroy(d);}
        super.onRemove(state,level,pos,next,moving);
    }
}
