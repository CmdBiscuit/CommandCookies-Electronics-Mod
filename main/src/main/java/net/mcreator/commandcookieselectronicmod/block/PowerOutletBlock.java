package net.mcreator.commandcookieselectronicmod.block;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.mcreator.commandcookieselectronicmod.power.*;
public abstract class PowerOutletBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED=BlockStateProperties.POWERED;
    public final boolean coax;
    private final VoxelShape[] shapes=new VoxelShape[4];
    protected PowerOutletBlock(boolean c){super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(.6f,2).noOcclusion().isRedstoneConductor((s,w,p)->false));coax=c;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.SOUTH).setValue(POWERED,false));
        double depth=c?3:1.5;shapes[0]=box(5,3,0,11,13,depth);shapes[1]=box(16-depth,3,5,16,13,11);shapes[2]=box(5,3,16-depth,11,13,16);shapes[3]=box(0,3,5,depth,13,11);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,POWERED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){var f=c.getClickedFace();if(!f.getAxis().isHorizontal())return null;var s=defaultBlockState().setValue(FACING,f);return canSurvive(s,c.getLevel(),c.getClickedPos())?s:null;}
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){var f=s.getValue(FACING);return l.getBlockState(p.relative(f.getOpposite())).isFaceSturdy(l,p.relative(f.getOpposite()),f);}
    @Override protected BlockState updateShape(BlockState s,Direction dir,BlockState neighbor,LevelAccessor l,BlockPos p,BlockPos np){return dir==s.getValue(FACING).getOpposite()&&!canSurvive(s,l,p)?Blocks.AIR.defaultBlockState():super.updateShape(s,dir,neighbor,l,p,np);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new OutletEntity(p,s);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return shapes[switch(s.getValue(FACING)){case SOUTH->0;case WEST->1;case NORTH->2;default->3;}];}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,PowerRegistration.OUTLET,OutletEntity::tick);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player user,BlockHitResult hit){if(!l.isClientSide&&l.getBlockEntity(p) instanceof OutletEntity d)user.displayClientMessage(net.minecraft.network.chat.Component.literal((coax?"Coax":"Power")+" outlet: "+(d.powered()?"Powered":"No power")+". Use a wrench to connect it."),true);return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock()))PowerNetwork.removed(l,p);super.onRemove(s,l,p,next,moving);}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
}
