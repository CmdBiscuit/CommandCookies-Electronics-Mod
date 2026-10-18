package net.mcreator.commandcookieselectronicmod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.mcreator.commandcookieselectronicmod.power.PowerNetwork;
import net.mcreator.commandcookieselectronicmod.n64.*;

/** A decorative console with persistent physical cable connections. */
public final class N64ConsoleBlock extends BaseEntityBlock {
    public static final MapCodec<N64ConsoleBlock> CODEC=simpleCodec(p->new N64ConsoleBlock());
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED=BlockStateProperties.POWERED;
    public N64ConsoleBlock(){
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.5f,4f).noOcclusion().isRedstoneConductor((s,w,p)->false));
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(POWERED,false));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,POWERED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new N64DeviceBlockEntity(p,s);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter w,BlockPos p,CollisionContext c){return N64Shapes.shape(s.getValue(FACING));}
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter w,BlockPos p,CollisionContext c){return N64Shapes.shape(s.getValue(FACING));}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
        if(!l.isClientSide)N64System.interactConsole(l,pos,p,hand);return ItemInteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){
        if(!l.isClientSide)N64System.interactConsole(l,pos,p,InteractionHand.MAIN_HAND);return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){
        return l.isClientSide?null:createTickerHelper(type,N64Registration.DEVICE_TYPE,N64DeviceBlockEntity::tick);
    }
    @Override protected void onRemove(BlockState state,Level l,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())){
            PowerNetwork.forget(l,pos);
            if(!l.isClientSide&&l.getBlockEntity(pos) instanceof N64DeviceBlockEntity d)N64System.detach(d,true);
        }
        super.onRemove(state,l,pos,next,moving);
    }
}
