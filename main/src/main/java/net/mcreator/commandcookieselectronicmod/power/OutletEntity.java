package net.mcreator.commandcookieselectronicmod.power;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.commandcookieselectronicmod.block.PowerOutletBlock;
public final class OutletEntity extends BlockEntity {
    public UUID identity=UUID.randomUUID(),generatorIdentity;
    public BlockPos generator;
    public int socketMask;
    public OutletEntity(BlockPos p,BlockState s){super(PowerRegistration.OUTLET,p,s);}
    public boolean coax(){return ((PowerOutletBlock)getBlockState().getBlock()).coax;}
    public boolean powered(){return level!=null&&generator!=null&&generatorIdentity!=null&&worldPosition.distSqr(generator)<=PowerNetwork.GENERATOR_RANGE*PowerNetwork.GENERATOR_RANGE&&level.hasChunkAt(generator)&&level.getBlockEntity(generator) instanceof GeneratorEntity g&&g.identity.equals(generatorIdentity)&&g.powered();}
    public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.putUUID("PowerIdentity",identity);t.putInt("SocketMask",socketMask);if(generator!=null&&generatorIdentity!=null){t.putLong("Generator",generator.asLong());t.putUUID("GeneratorIdentity",generatorIdentity);}}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);identity=t.hasUUID("PowerIdentity")?t.getUUID("PowerIdentity"):UUID.randomUUID();socketMask=t.getInt("SocketMask")&3;generator=t.contains("Generator")?BlockPos.of(t.getLong("Generator")):null;generatorIdentity=t.hasUUID("GeneratorIdentity")?t.getUUID("GeneratorIdentity"):null;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    public static void tick(Level l,BlockPos p,BlockState s,OutletEntity d){if(l.getGameTime()%20!=0)return;
        if(d.generator!=null&&l.hasChunkAt(d.generator)&&(!(l.getBlockEntity(d.generator) instanceof GeneratorEntity g)||!g.identity.equals(d.generatorIdentity))){d.generator=null;d.generatorIdentity=null;d.changed();}
        if(l instanceof net.minecraft.server.level.ServerLevel server){PowerCables.normalize(server,d);PowerCables.ensureFeed(server,d);}
        if(s.getValue(PowerOutletBlock.POWERED)!=d.powered())l.setBlock(p,s.setValue(PowerOutletBlock.POWERED,d.powered()),3);
    }
}
