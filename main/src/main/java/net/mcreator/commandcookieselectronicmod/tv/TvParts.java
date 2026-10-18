package net.mcreator.commandcookieselectronicmod.tv;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class TvParts {
    private static boolean updating;
    public static ImageTvEntity master(BlockGetter level,BlockPos pos){
        var state=level.getBlockState(pos);
        if(state.getBlock() instanceof TvPartBlock)pos=TvPartBlock.anchor(pos,state);
        return level.getBlockEntity(pos) instanceof ImageTvEntity tv?tv:null;
    }
    private static boolean own(BlockGetter level,BlockPos pos,BlockPos anchor){var state=level.getBlockState(pos);return state.getBlock() instanceof TvPartBlock&&TvPartBlock.anchor(pos,state).equals(anchor);}
    public static boolean room(LevelReader level,BlockPos pos,Map<BlockPos,VoxelShape> cells){
        for(var offset:cells.keySet())if(!offset.equals(BlockPos.ZERO)){
            if(Math.abs(offset.getX())>4||Math.abs(offset.getY())>2||Math.abs(offset.getZ())>4)return false;
            var p=pos.offset(offset);if(p.getY()<level.getMinBuildHeight()||p.getY()>=level.getMaxBuildHeight())return false;
            var state=level.getBlockState(p);if(!state.canBeReplaced()&&!own(level,p,pos))return false;
        }
        return true;
    }
    public static void rebuild(ImageTvEntity tv,Map<BlockPos,VoxelShape> old){
        if(tv.getLevel()==null||tv.getLevel().isClientSide)return;
        var level=tv.getLevel();var anchor=tv.getBlockPos();var cells=tv.cells();updating=true;
        try{
            for(var offset:old.keySet())if(!offset.equals(BlockPos.ZERO)&&!cells.containsKey(offset)){
                var pos=anchor.offset(offset);if(own(level,pos,anchor))level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
            }
            for(var offset:cells.keySet())if(!offset.equals(BlockPos.ZERO))level.setBlock(anchor.offset(offset),TvRegistration.PART.defaultBlockState().setValue(TvPartBlock.X,4-offset.getX()).setValue(TvPartBlock.Y,2-offset.getY()).setValue(TvPartBlock.Z,4-offset.getZ()),3);
        }finally{updating=false;}
    }
    public static void remove(ImageTvEntity tv){
        var level=tv.getLevel();if(level==null||level.isClientSide)return;updating=true;
        try{for(var offset:tv.cells().keySet())if(!offset.equals(BlockPos.ZERO)){var p=tv.getBlockPos().offset(offset);if(own(level,p,tv.getBlockPos()))level.setBlock(p,Blocks.AIR.defaultBlockState(),3);}}finally{updating=false;}
    }
    public static void broken(Level level,BlockPos pos,BlockState state,boolean drops){
        if(updating||level.isClientSide)return;
        var master=masterFromState(level,pos,state);if(master!=null)level.destroyBlock(master.getBlockPos(),drops);
    }
    private static ImageTvEntity masterFromState(Level level,BlockPos pos,BlockState state){var anchor=TvPartBlock.anchor(pos,state);return level.getBlockEntity(anchor) instanceof ImageTvEntity tv?tv:null;}
    public static void supportChanged(LevelAccessor level,BlockPos pos,BlockState state){
        if(updating||level.isClientSide())return;
        var anchor=TvPartBlock.anchor(pos,state);
        if(level.getBlockState(anchor).getBlock() instanceof ImageTvBlock block)level.scheduleTick(anchor,block,1);
    }
    private TvParts(){}
}
