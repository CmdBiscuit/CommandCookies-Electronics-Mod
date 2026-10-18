package net.mcreator.commandcookieselectronicmod.block;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
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
import net.mcreator.commandcookieselectronicmod.power.*;
public class GeneratorBlock extends BaseEntityBlock {
    public static final MapCodec<GeneratorBlock> CODEC=simpleCodec(p->new GeneratorBlock());
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED=BlockStateProperties.POWERED;
    public GeneratorBlock(){super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1,10));registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(POWERED,false));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,POWERED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new GeneratorEntity(p,s);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,PowerRegistration.GENERATOR,GeneratorEntity::tick);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player user,BlockHitResult h){if(user instanceof ServerPlayer sp&&l.getBlockEntity(p) instanceof GeneratorEntity d){PowerNetwork.clearHighlight(sp,p);sp.openMenu(d,p);}return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected ItemInteractionResult useItemOn(ItemStack item,BlockState s,Level l,BlockPos p,Player user,InteractionHand hand,BlockHitResult hit){if(user.isShiftKeyDown()&&item.getItem() instanceof BlockItem)return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;useWithoutItem(s,l,p,user,hit);return ItemInteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState n,boolean moving){if(!s.is(n.getBlock()))PowerNetwork.removed(l,p);if(!s.is(n.getBlock())&&l.getBlockEntity(p) instanceof GeneratorEntity d){Containers.dropContents(l,p,d);l.updateNeighbourForOutputSignal(p,this);}super.onRemove(s,l,p,n,moving);}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
}
