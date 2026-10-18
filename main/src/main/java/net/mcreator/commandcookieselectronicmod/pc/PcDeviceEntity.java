package net.mcreator.commandcookieselectronicmod.pc;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class PcDeviceEntity extends BlockEntity {
    public UUID identity=UUID.randomUUID(),desktopSession=UUID.randomUUID(),peerIdentity,keyboardIdentity,mouseIdentity,hostIdentity;
    public BlockPos peer,keyboard,mouse,host;
    public boolean paidCable,paidKeyboardAttachment,paidMouseAttachment;
    public long interaction=-1;
    public PcDeviceEntity(BlockPos p,BlockState s){super(PcRegistration.DEVICE_TYPE,p,s);}
    public boolean monitor(){return getBlockState().getBlock() instanceof PcDeviceBlock b&&b.monitor;}
    public void fresh(){identity=UUID.randomUUID();desktopSession=UUID.randomUUID();peer=null;peerIdentity=null;keyboard=null;keyboardIdentity=null;mouse=null;mouseIdentity=null;host=null;hostIdentity=null;paidCable=false;paidKeyboardAttachment=false;paidMouseAttachment=false;changed();}
    public void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.putUUID("Identity",identity);t.putUUID("DesktopSession",desktopSession);t.putBoolean("PaidCable",paidCable);t.putBoolean("PaidKeyboardAttachment",paidKeyboardAttachment);t.putBoolean("PaidMouseAttachment",paidMouseAttachment);if(peer!=null&&peerIdentity!=null){t.putLong("Peer",peer.asLong());t.putUUID("PeerIdentity",peerIdentity);}saveLink(t,"Keyboard",keyboard,keyboardIdentity);saveLink(t,"Mouse",mouse,mouseIdentity);saveLink(t,"Host",host,hostIdentity);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);identity=t.hasUUID("Identity")?t.getUUID("Identity"):UUID.randomUUID();desktopSession=t.hasUUID("DesktopSession")?t.getUUID("DesktopSession"):UUID.randomUUID();peer=t.contains("Peer")?BlockPos.of(t.getLong("Peer")):null;peerIdentity=t.hasUUID("PeerIdentity")?t.getUUID("PeerIdentity"):null;if(peer==null||peerIdentity==null){peer=null;peerIdentity=null;}paidCable=t.getBoolean("PaidCable");paidKeyboardAttachment=t.getBoolean("PaidKeyboardAttachment");paidMouseAttachment=t.getBoolean("PaidMouseAttachment");keyboard=loadPosition(t,"Keyboard");keyboardIdentity=loadIdentity(t,"Keyboard");mouse=loadPosition(t,"Mouse");mouseIdentity=loadIdentity(t,"Mouse");host=loadPosition(t,"Host");hostIdentity=loadIdentity(t,"Host");}
    private static void saveLink(CompoundTag t,String key,BlockPos pos,UUID id){if(pos!=null&&id!=null){t.putLong(key,pos.asLong());t.putUUID(key+"Identity",id);}}
    private static BlockPos loadPosition(CompoundTag t,String key){return t.contains(key)&&t.hasUUID(key+"Identity")?BlockPos.of(t.getLong(key)):null;}
    private static UUID loadIdentity(CompoundTag t,String key){return t.contains(key)&&t.hasUUID(key+"Identity")?t.getUUID(key+"Identity"):null;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
