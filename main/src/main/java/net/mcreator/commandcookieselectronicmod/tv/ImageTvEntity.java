package net.mcreator.commandcookieselectronicmod.tv;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ImageTvEntity extends BlockEntity {
    public UUID identity=UUID.randomUUID(),owner;
    public String image="";
    public boolean fill,powered;
    public int yaw,pitch,roll;
    private Map<BlockPos,VoxelShape> cells;
    public ImageTvEntity(BlockPos pos,BlockState state){super(TvRegistration.TYPE,pos,state);}
    public boolean wall(){return ((ImageTvBlock)getBlockState().getBlock()).wall;}
    public Direction facing(){return getBlockState().getValue(ImageTvBlock.FACING);}
    public Map<BlockPos,VoxelShape> cells(){if(cells==null)cells=TvGeometry.cells(wall(),facing(),yaw,pitch,roll);return cells;}
    public void invalidateGeometry(){cells=null;}
    public void changed(){invalidateGeometry();setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public void refreshPower(){if(level==null||level.isClientSide)return;boolean on=net.mcreator.commandcookieselectronicmod.power.PowerNetwork.powered(level,worldPosition);if(powered!=on){powered=on;changed();}}
    private void write(CompoundTag tag){tag.putBoolean("Powered",powered);tag.putUUID("Identity",identity);if(owner!=null)tag.putUUID("Owner",owner);tag.putString("Image",image);tag.putBoolean("Fill",fill);tag.putInt("Yaw",yaw);tag.putInt("Pitch",pitch);tag.putInt("Roll",roll);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider){super.saveAdditional(tag,provider);write(tag);tag.remove("Powered");}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider provider){
        super.loadAdditional(tag,provider);identity=tag.hasUUID("Identity")?tag.getUUID("Identity"):UUID.randomUUID();owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;
        try{image=ImgurLink.normalize(tag.getString("Image"));}catch(IllegalArgumentException ignored){image="";}
        powered=tag.getBoolean("Powered");fill=tag.getBoolean("Fill");yaw=wall()?Mth.clamp(tag.getInt("Yaw"),-TvGeometry.MAX_ANGLE,TvGeometry.MAX_ANGLE):0;pitch=wall()?Mth.clamp(tag.getInt("Pitch"),-TvGeometry.MAX_ANGLE,TvGeometry.MAX_ANGLE):0;roll=wall()?Mth.clamp(tag.getInt("Roll"),-TvGeometry.MAX_ROLL,TvGeometry.MAX_ROLL):0;invalidateGeometry();
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider){var tag=new CompoundTag();write(tag);return tag;}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    public AABB renderBounds(){return TvGeometry.bounds(wall(),facing(),yaw,pitch,roll).move(worldPosition);}
}
