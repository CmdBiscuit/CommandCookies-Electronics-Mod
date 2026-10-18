package net.mcreator.commandcookieselectronicmod.power;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
/** A visual entity only. Authoritative connections are saved by PowerLinks/OutletEntity. */
public final class PowerCableEntity extends Entity {
 private static final EntityDataAccessor<BlockPos> START=SynchedEntityData.defineId(PowerCableEntity.class,EntityDataSerializers.BLOCK_POS),END=SynchedEntityData.defineId(PowerCableEntity.class,EntityDataSerializers.BLOCK_POS);
 private static final EntityDataAccessor<Integer> A=SynchedEntityData.defineId(PowerCableEntity.class,EntityDataSerializers.INT),B=SynchedEntityData.defineId(PowerCableEntity.class,EntityDataSerializers.INT);
 public UUID sourceIdentity,targetIdentity;public boolean feed;public int mode;
 public PowerCableEntity(EntityType<? extends PowerCableEntity> type,Level level){super(type,level);noPhysics=true;setNoGravity(true);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(START,BlockPos.ZERO);b.define(END,BlockPos.ZERO);b.define(A,0);b.define(B,0);}
 public BlockPos start(){return entityData.get(START);}public BlockPos end(){return entityData.get(END);}public int startSpec(){return entityData.get(A);}public int endSpec(){return entityData.get(B);}
 public CablePorts.Port first(){return CablePorts.at(start(),startSpec());}public CablePorts.Port second(){return CablePorts.at(end(),endSpec());}
 public void configure(BlockPos a,BlockPos b,UUID aid,UUID bid,int mode,int slot){this.mode=mode;this.feed=mode==PowerCables.FEED;sourceIdentity=aid;targetIdentity=bid;entityData.set(START,a.immutable());entityData.set(END,b.immutable());syncPorts(slot);setPos(b.getX()+.5,b.getY()+.5,b.getZ()+.5);}
 public void syncPorts(int slot){if(level().hasChunkAt(start())&&level().hasChunkAt(end())){entityData.set(A,CablePorts.connectionSpec(level(),start(),slot,mode,false,false));entityData.set(B,CablePorts.connectionSpec(level(),end(),0,mode,true,false));}}
 public AABB wireBounds(){return new AABB(first().point(),second().point()).inflate(4);}
 @Override public boolean shouldBeSaved(){return false;}
 @Override public boolean isPickable(){return false;}@Override public boolean isPushable(){return false;}@Override public boolean canBeCollidedWith(){return false;}
 @Override public boolean shouldRenderAtSqrDistance(double distance){return distance<192*192;}
 @Override public void tick(){super.tick();if(!level().isClientSide&&tickCount%20==0){if(!PowerCables.valid(this)){discard();return;}if(level().hasChunkAt(start())&&level().hasChunkAt(end()))syncPorts(startSpec()>>>8&1);}}
 @Override protected void readAdditionalSaveData(CompoundTag t){}@Override protected void addAdditionalSaveData(CompoundTag t){}
}
