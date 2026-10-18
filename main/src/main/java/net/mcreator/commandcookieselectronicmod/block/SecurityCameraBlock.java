package net.mcreator.commandcookieselectronicmod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.mcreator.commandcookieselectronicmod.power.PowerNetwork;
import net.mcreator.commandcookieselectronicmod.security.*;

public final class SecurityCameraBlock extends BaseEntityBlock {
 public static final MapCodec<SecurityCameraBlock> CODEC=simpleCodec(p->new SecurityCameraBlock());
 public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
 public static final BooleanProperty POWERED=BlockStateProperties.POWERED;
 public SecurityCameraBlock(){super(Properties.of().sound(SoundType.METAL).strength(1.5f,4f).noOcclusion().isRedstoneConductor((s,l,p)->false));registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(POWERED,false));}
 @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,POWERED);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c){var face=c.getClickedFace();if(!face.getAxis().isHorizontal())return null;var state=defaultBlockState().setValue(FACING,face);return canSurvive(state,c.getLevel(),c.getClickedPos())?state:null;}
 @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){var facing=s.getValue(FACING);var wall=p.relative(facing.getOpposite());return l.getBlockState(wall).isFaceSturdy(l,wall,facing);}
 @Override protected BlockState updateShape(BlockState s,Direction direction,BlockState other,LevelAccessor l,BlockPos p,BlockPos neighbor){return direction==s.getValue(FACING).getOpposite()&&!canSurvive(s,l,p)?Blocks.AIR.defaultBlockState():super.updateShape(s,direction,other,l,p,neighbor);}
 @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
 @Override public void appendHoverText(ItemStack stack,net.minecraft.world.item.Item.TooltipContext context,java.util.List<Component> lines,net.minecraft.world.item.TooltipFlag flag){super.appendHoverText(stack,context,lines,flag);lines.add(Component.literal("Mount on a wall; right-click with 2 AA batteries.").withStyle(net.minecraft.ChatFormatting.GRAY));lines.add(Component.literal("Right-click with your phone to pair; view it anywhere in Security.").withStyle(net.minecraft.ChatFormatting.GRAY));}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new SecurityCameraEntity(p,s);}
 @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity placer,ItemStack stack){super.setPlacedBy(l,p,s,placer,stack);if(!l.isClientSide&&l.getBlockEntity(p) instanceof SecurityCameraEntity camera){if(placer instanceof Player player)camera.owner=player.getUUID();if(stack.has(DataComponents.CUSTOM_NAME))camera.name=SecurityCameraService.cleanName(stack.getHoverName().getString());camera.changed();}}
 public static Vec3 rotate(Vec3 point,Direction facing){int turns=switch(facing){case EAST->1;case SOUTH->2;case WEST->3;default->0;};for(int i=0;i<turns;i++)point=new Vec3(-point.z,point.y,point.x);return point;}
 public static Vec3 eye(BlockPos p,Direction facing){return rotate(new Vec3(0,.459,.127-.5),facing).add(p.getX()+.5,p.getY(),p.getZ()+.5);}
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){var result=Shapes.empty();double[][] boxes={{4.3,6.15,1.0,11.7,14.1,11.8},{6.25,9.2,10.7,9.75,11.9,15.5},{5.9,8.2,15.3,10.1,13.5,16}};for(var b:boxes){var a=rotate(new Vec3(b[0]/16-.5,b[1]/16,b[2]/16-.5),s.getValue(FACING));var z=rotate(new Vec3(b[3]/16-.5,b[4]/16,b[5]/16-.5),s.getValue(FACING));result=Shapes.or(result,Shapes.box(Math.min(a.x,z.x)+.5,a.y,Math.min(a.z,z.z)+.5,Math.max(a.x,z.x)+.5,z.y,Math.max(a.z,z.z)+.5));}return result;}
 @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
 @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
 private void inspect(Level l,BlockPos pos,Player p){if(!l.isClientSide&&l.getBlockEntity(pos) instanceof SecurityCameraEntity camera){if(camera.owner==null){camera.owner=p.getUUID();camera.changed();}p.displayClientMessage(Component.literal(camera.name+" - "+camera.batteryStatus()+". Open Security on your phone or computer."),true);}}
 @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
  if(net.mcreator.commandcookieselectronicmod.procedures.IphoneLogicProcedure.color(stack)!=null){
   if(p instanceof net.minecraft.server.level.ServerPlayer player&&l.getBlockEntity(pos) instanceof SecurityCameraEntity camera){var result=camera.pairPhone(player,stack);String message=switch(result){case PAIRED->"Camera paired to this phone. Open Security to view it anywhere.";case ALREADY_PAIRED->"This phone is already paired to the camera.";case OWNER_ONLY->"Only the camera owner can pair a phone.";case LIMIT_REACHED->"This camera already has 16 paired phones.";};p.displayClientMessage(Component.literal(message),true);}
   return ItemInteractionResult.sidedSuccess(l.isClientSide);
  }
  if(stack.is(net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems.AA_BATTERY.get())){
   if(!l.isClientSide&&l.getBlockEntity(pos) instanceof SecurityCameraEntity camera){var result=camera.insertBatteries(p,stack);String message=switch(result){case CHARGED->"2 AA batteries inserted. Camera powered for 15 real days.";case ALREADY_CHARGED->camera.batteryStatus()+". Batteries were not used.";case NOT_ENOUGH->"You need 2 AA batteries to power this camera.";default->"Cannot charge this camera right now.";};p.displayClientMessage(Component.literal(message),true);}
   return ItemInteractionResult.sidedSuccess(l.isClientSide);
  }
  if(hand==InteractionHand.MAIN_HAND&&(p.getOffhandItem().is(net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems.AA_BATTERY.get())||net.mcreator.commandcookieselectronicmod.procedures.IphoneLogicProcedure.color(p.getOffhandItem())!=null))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
  inspect(l,pos,p);return ItemInteractionResult.sidedSuccess(l.isClientSide);
 }
 @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){if(p.getOffhandItem().is(net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems.AA_BATTERY.get())||net.mcreator.commandcookieselectronicmod.procedures.IphoneLogicProcedure.color(p.getOffhandItem())!=null)return InteractionResult.PASS;inspect(l,pos,p);return InteractionResult.sidedSuccess(l.isClientSide);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide?null:createTickerHelper(type,SecurityCameraRegistration.CAMERA_TYPE,SecurityCameraEntity::tick);}
 @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock())){if(l instanceof net.minecraft.server.level.ServerLevel server&&l.getBlockEntity(p) instanceof SecurityCameraEntity camera)SecurityCameras.removed(server,camera.identity);PowerNetwork.forget(l,p);}super.onRemove(s,l,p,next,moving);}
}
