package net.mcreator.commandcookieselectronicmod.tv;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public abstract class ImageTvBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public final boolean wall;
    protected ImageTvBlock(boolean wall){super(Properties.of().sound(SoundType.METAL).strength(1.5f,4).noOcclusion().dynamicShape().isRedstoneConductor((s,l,p)->false));this.wall=wall;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        var face=wall?context.getClickedFace():context.getHorizontalDirection().getOpposite();
        if(!face.getAxis().isHorizontal())return null;
        var state=defaultBlockState().setValue(FACING,face);var cells=TvGeometry.cells(wall,face,0,0);
        return canSurvive(state,context.getLevel(),context.getClickedPos())&&TvParts.room(context.getLevel(),context.getClickedPos(),cells)?state:null;
    }
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){
        var facing=state.getValue(FACING);var width=TvGeometry.widthDirection(facing);
        for(var column:List.of(pos,pos.relative(width))){var support=column.relative(wall?facing.getOpposite():Direction.DOWN);if(!level.getBlockState(support).isFaceSturdy(level,support,wall?facing:Direction.UP))return false;}
        return true;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new ImageTvEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,TvRegistration.TYPE,(world,pos,s,tv)->{if(world.getGameTime()%20==0)tv.refreshPower();});}
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){
        super.setPlacedBy(level,pos,state,placer,stack);
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof ImageTvEntity tv){if(placer instanceof Player player)tv.owner=player.getUUID();tv.changed();TvParts.rebuild(tv,Map.of());}
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        return level.getBlockEntity(pos) instanceof ImageTvEntity tv?tv.cells().getOrDefault(BlockPos.ZERO,Shapes.empty()):TvGeometry.cells(wall,state.getValue(FACING),0,0).getOrDefault(BlockPos.ZERO,Shapes.empty());
    }
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos other){if(!level.isClientSide())level.scheduleTick(pos,this,1);return state;}
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){if(!canSurvive(state,level,pos))level.destroyBlock(pos,true);}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof ImageTvEntity tv){net.mcreator.commandcookieselectronicmod.power.PowerNetwork.forget(level,pos);TvParts.remove(tv);}super.onRemove(state,level,pos,next,moving);}
    public static InteractionResult open(Level level,BlockPos pos,Player player){var tv=TvParts.master(level,pos);if(level.isClientSide&&tv!=null)Client.open(tv.getBlockPos());return InteractionResult.sidedSuccess(level.isClientSide);}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return open(level,pos,player);}
    @Override protected ItemInteractionResult useItemOn(ItemStack item,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){if(player.isShiftKeyDown())return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;open(level,pos,player);return ItemInteractionResult.sidedSuccess(level.isClientSide);}
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,List<Component> text,TooltipFlag flag){super.appendHoverText(stack,context,text,flag);text.add(Component.literal("Requires a powered outlet; connect it with the wrench.").withStyle(net.minecraft.ChatFormatting.GRAY));text.add(Component.literal(wall?"Mount on a two-block-wide wall; right-click for images and angle sliders.":"Place on a two-block-wide surface; right-click to add an Imgur image.").withStyle(net.minecraft.ChatFormatting.GRAY));}
    private static final class Client{static void open(BlockPos pos){net.mcreator.commandcookieselectronicmod.client.ImageTvScreen.open(pos);}}
}
