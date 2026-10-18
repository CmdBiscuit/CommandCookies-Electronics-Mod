package net.mcreator.commandcookieselectronicmod.tv;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Selectable occupied TV cells let every part of the large screen be clicked. */
public final class TvPartBlock extends Block {
    public static final IntegerProperty X=IntegerProperty.create("anchor_x",0,8),Y=IntegerProperty.create("anchor_y",0,4),Z=IntegerProperty.create("anchor_z",0,8);
    public TvPartBlock(){super(Properties.of().sound(SoundType.METAL).strength(1.5f,4).noOcclusion().dynamicShape().noLootTable().isRedstoneConductor((s,l,p)->false));registerDefaultState(stateDefinition.any().setValue(X,4).setValue(Y,2).setValue(Z,4));}
    public static BlockPos anchor(BlockPos pos,BlockState state){return pos.offset(state.getValue(X)-4,state.getValue(Y)-2,state.getValue(Z)-4);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(X,Y,Z);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){var tv=TvParts.master(level,pos);return tv==null?Shapes.empty():tv.cells().getOrDefault(pos.subtract(tv.getBlockPos()),Shapes.empty());}
    @Override protected BlockState updateShape(BlockState state,Direction dir,BlockState other,LevelAccessor level,BlockPos pos,BlockPos neighbor){TvParts.supportChanged(level,pos,state);return state;}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return ImageTvBlock.open(level,pos,player);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){if(player.isShiftKeyDown())return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;ImageTvBlock.open(level,pos,player);return ItemInteractionResult.sidedSuccess(level.isClientSide);}
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){TvParts.broken(level,pos,state,!player.isCreative());return super.playerWillDestroy(level,pos,state,player);}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock()))TvParts.broken(level,pos,state,true);super.onRemove(state,level,pos,next,moving);}
    @Override public ItemStack getCloneItemStack(LevelReader level,BlockPos pos,BlockState state){var tv=TvParts.master(level,pos);return tv==null?ItemStack.EMPTY:new ItemStack(tv.getBlockState().getBlock());}
}
