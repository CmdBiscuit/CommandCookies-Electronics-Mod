package net.mcreator.commandcookieselectronicmod.n64;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

/** Separate from the NES game entity: this console has no cartridge or emulator. */
public final class N64DeviceBlockEntity extends BlockEntity {
    public UUID identity=UUID.randomUUID(),peerIdentity;
    public BlockPos peer;
    public boolean paidCable;
    public long interaction=-1;
    public N64DeviceBlockEntity(BlockPos pos,BlockState state){super(N64Registration.DEVICE_TYPE,pos,state);}
    public void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    private CompoundTag connectionTag(){
        var tag=new CompoundTag();tag.putUUID("Identity",identity);
        if(peer!=null&&peerIdentity!=null){tag.putLong("Peer",peer.asLong());tag.putUUID("PeerIdentity",peerIdentity);}
        return tag;
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.merge(connectionTag());tag.putBoolean("PaidCable",paidCable);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);identity=tag.hasUUID("Identity")?tag.getUUID("Identity"):UUID.randomUUID();
        peer=tag.contains("Peer")?BlockPos.of(tag.getLong("Peer")):null;peerIdentity=tag.hasUUID("PeerIdentity")?tag.getUUID("PeerIdentity"):null;
        if(peer==null||peerIdentity==null){peer=null;peerIdentity=null;}
        paidCable=tag.getBoolean("PaidCable");
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return connectionTag();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    public static void tick(Level level,BlockPos pos,BlockState state,N64DeviceBlockEntity d){if(!level.isClientSide&&level.getGameTime()%20==0)N64System.tick(d);}
}
