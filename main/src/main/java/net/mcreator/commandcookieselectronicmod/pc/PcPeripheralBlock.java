package net.mcreator.commandcookieselectronicmod.pc;

import java.util.EnumMap;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public abstract class PcPeripheralBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    private static final EnumMap<Direction,VoxelShape> KEYBOARD=new EnumMap<>(Direction.class),MOUSE=new EnumMap<>(Direction.class);
    public final boolean mouse;
    static {
        cache(KEYBOARD,Shapes.or(Block.box(.5,.15,4.47,15.5,1.28,11.55),Block.box(1.25,1.28,5.15,14.65,1.68,10.6),
            Block.box(1.15,0,5.1,2.2,.15,5.75),Block.box(1.15,0,10.45,2.2,.15,11.1),Block.box(13.8,0,5.1,14.85,.15,5.75),Block.box(13.8,0,10.45,14.85,.15,11.1)).optimize());
        cache(MOUSE,Shapes.or(Block.box(5.56,0,5.65,10.34,1.15,11.35),Block.box(5.95,1.15,8,10.05,1.85,10.7),
            Block.box(6.35,1.85,8.3,9.65,2.2,10.35),Block.box(5.95,1.15,5.95,10.05,1.75,8),Block.box(7.68,1.75,6.3,8.32,2,7.45)).optimize());
    }
    private static void cache(EnumMap<Direction,VoxelShape> map,VoxelShape s){for(var dir:new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST}){map.put(dir,s);VoxelShape[] next={Shapes.empty()};s.forAllBoxes((x,y,z,X,Y,Z)->next[0]=Shapes.or(next[0],Shapes.box(1-Z,y,x,1-z,Y,X)));s=next[0].optimize();}}
    protected PcPeripheralBlock(boolean mouse){super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1,2).noOcclusion().isRedstoneConductor((s,l,p)->false));this.mouse=mouse;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new PcDeviceEntity(p,s);}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack){super.setPlacedBy(l,p,s,e,stack);if(!l.isClientSide&&l.getBlockEntity(p) instanceof PcDeviceEntity d)d.fresh();}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return (mouse?MOUSE:KEYBOARD).get(s.getValue(FACING));}
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return getShape(s,l,p,c);}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){return InteractionResult.PASS;}
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock()))net.mcreator.commandcookieselectronicmod.power.PowerNetwork.removed(l,p);if(!s.is(next.getBlock())&&!l.isClientSide&&l.getBlockEntity(p) instanceof PcDeviceEntity d)PcInputs.detachPeripheral(d);super.onRemove(s,l,p,next,moving);}
}
