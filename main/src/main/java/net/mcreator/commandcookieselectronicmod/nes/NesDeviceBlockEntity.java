package net.mcreator.commandcookieselectronicmod.nes;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public final class NesDeviceBlockEntity extends BlockEntity {
    public UUID identity=UUID.randomUUID(),peerIdentity;
    public BlockPos peer;
    public boolean paidCable;
    public boolean snesPeer;
    public boolean n64Peer;
    public ItemStack cartridge=ItemStack.EMPTY;
    public TetrisEngine game=new TetrisEngine(System.nanoTime());
    public int best;
    public UUID owner,session=UUID.randomUUID();
    public long interaction=-1;
    public int lastInput;
    public final NesInputQueue inputs=new NesInputQueue();
    public NesDeviceBlockEntity(BlockPos p,BlockState s){super(NesSystemRegistration.DEVICE_TYPE,p,s);}
    public boolean monitor(){return getBlockState().getBlock() instanceof NesDeviceBlock b&&b.monitor;}
    public void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);tag.putUUID("Identity",identity);
        if(peer!=null&&peerIdentity!=null){tag.putLong("Peer",peer.asLong());tag.putUUID("PeerIdentity",peerIdentity);}
        tag.putBoolean("N64Peer",n64Peer);tag.putBoolean("SnesPeer",snesPeer);tag.putBoolean("PaidCable",paidCable);tag.putInt("Best",best);
        if(!cartridge.isEmpty())tag.put("Cartridge",cartridge.save(registries));
        if(!monitor())tag.putByteArray("Tetris",game.save());
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);identity=tag.hasUUID("Identity")?tag.getUUID("Identity"):UUID.randomUUID();
        peer=tag.contains("Peer")?BlockPos.of(tag.getLong("Peer")):null;peerIdentity=tag.hasUUID("PeerIdentity")?tag.getUUID("PeerIdentity"):null;
        if(peer==null||peerIdentity==null){peer=null;peerIdentity=null;}
        n64Peer=monitor()&&tag.getBoolean("N64Peer");snesPeer=monitor()&&!n64Peer&&tag.getBoolean("SnesPeer");paidCable=tag.getBoolean("PaidCable");best=Math.max(0,tag.getInt("Best"));
        cartridge=tag.contains("Cartridge")?ItemStack.parseOptional(registries,tag.getCompound("Cartridge")):ItemStack.EMPTY;
        if(!monitor())game=TetrisEngine.load(tag.getByteArray("Tetris"));owner=null;session=UUID.randomUUID();inputs.reset();lastInput=0;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){
        var tag=new CompoundTag();tag.putBoolean("N64Peer",n64Peer);tag.putBoolean("SnesPeer",snesPeer);tag.putUUID("Identity",identity);if(peer!=null&&peerIdentity!=null){tag.putLong("Peer",peer.asLong());tag.putUUID("PeerIdentity",peerIdentity);}return tag;
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    public static void tick(Level level,BlockPos pos,BlockState state,NesDeviceBlockEntity device){NesSystem.tick(device);}
}
