package net.mcreator.commandcookieselectronicmod.pc;
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
public abstract class PcDeviceBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED=BlockStateProperties.POWERED;
    public final boolean monitor;
    protected PcDeviceBlock(boolean monitor){super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.5f,4f).noOcclusion().isRedstoneConductor((s,w,p)->false));this.monitor=monitor;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(POWERED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,POWERED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new PcDeviceEntity(p,s);}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,net.minecraft.world.entity.LivingEntity e,ItemStack stack){super.setPlacedBy(l,p,s,e,stack);if(!l.isClientSide&&l.getBlockEntity(p) instanceof PcDeviceEntity device)device.fresh();}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return PcShapes.shape(monitor,s.getValue(FACING),monitor&&s.getValue(MonitorAttachments.KEYBOARD)>0);}
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return getShape(s,l,p,c);}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){if(monitor&&(MonitorAttachments.attach(stack,l,p,player)||(stack.isEmpty()&&hand==InteractionHand.MAIN_HAND&&MonitorAttachments.attach(player.getOffhandItem(),l,p,player))))return ItemInteractionResult.sidedSuccess(l.isClientSide);if(!l.isClientSide)PcSystem.interact(l,p,player,hand);return ItemInteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(monitor&&player.getMainHandItem().isEmpty()&&(MonitorAttachments.attach(player.getOffhandItem(),l,p,player)||(player.getOffhandItem().isEmpty()&&MonitorAttachments.remove(l,p,player))))return InteractionResult.sidedSuccess(l.isClientSide);if(!l.isClientSide)PcSystem.interact(l,p,player,InteractionHand.MAIN_HAND);return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide?null:createTickerHelper(type,PcRegistration.DEVICE_TYPE,(level,pos,state,e)->{if(level.getGameTime()%20==0)PcSystem.refresh(e);});}
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock())){if(monitor)MonitorAttachments.drop(s,l,p);net.mcreator.commandcookieselectronicmod.power.PowerNetwork.forget(l,p);if(!l.isClientSide&&l.getBlockEntity(p) instanceof PcDeviceEntity d){if(!d.monitor())PcInputs.detachTower(d);PcSystem.detach(d,true);}}super.onRemove(s,l,p,next,moving);}
}
