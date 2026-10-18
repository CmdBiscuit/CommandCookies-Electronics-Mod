package net.mcreator.commandcookieselectronicmod.security;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.commandcookieselectronicmod.block.SecurityCameraBlock;
import net.mcreator.commandcookieselectronicmod.power.PowerNetwork;

public final class SecurityCameraEntity extends BlockEntity {
 public static final long BATTERY_DURATION_MS=15L*24*60*60*1000;
 public UUID identity=UUID.randomUUID(),owner;public String name="Security Camera";
 public long batteryExpiresAt;
 public final java.util.Set<UUID> pairedPhones=new java.util.HashSet<>();
 public SecurityCameraEntity(BlockPos p,BlockState s){super(SecurityCameraRegistration.CAMERA_TYPE,p,s);}
 public void changed(){setChanged();SecurityCameras.remember(this);if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
 private CompoundTag tag(){var tag=new CompoundTag();tag.putUUID("Identity",identity);if(owner!=null)tag.putUUID("Owner",owner);tag.putString("Name",name);tag.putLong("BatteryExpiresAt",batteryExpiresAt);tag.put("PairedPhones",SecurityCameras.writePhones(pairedPhones));return tag;}
 @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);tag.merge(tag());}
 @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){super.loadAdditional(tag,r);identity=tag.hasUUID("Identity")?tag.getUUID("Identity"):UUID.randomUUID();owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;name=SecurityCameraService.cleanName(tag.getString("Name"));batteryExpiresAt=Math.max(0,tag.getLong("BatteryExpiresAt"));pairedPhones.clear();pairedPhones.addAll(SecurityCameras.readPhones(tag));}
 @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){var update=tag();update.remove("PairedPhones");return update;}
 @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
 @Override public void onLoad(){super.onLoad();SecurityCameras.loaded(this);}
 public enum PairResult { PAIRED, ALREADY_PAIRED, OWNER_ONLY, LIMIT_REACHED }
 public PairResult pairPhone(net.minecraft.server.level.ServerPlayer player,net.minecraft.world.item.ItemStack phone){
  if(level==null||level.isClientSide||player.level()!=level||!player.isAlive()||player.isSpectator()||net.mcreator.commandcookieselectronicmod.procedures.IphoneLogicProcedure.color(phone)==null)return PairResult.OWNER_ONLY;
  if(owner!=null&&!owner.equals(player.getUUID()))return PairResult.OWNER_ONLY;
  var id=net.mcreator.commandcookieselectronicmod.phone.PhoneDeviceIdentity.ensure(phone);
  if(pairedPhones.contains(id))return PairResult.ALREADY_PAIRED;
  if(pairedPhones.size()>=16)return PairResult.LIMIT_REACHED;
  owner=player.getUUID();pairedPhones.add(id);changed();player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();if(player.containerMenu!=player.inventoryMenu)player.containerMenu.broadcastChanges();return PairResult.PAIRED;
 }
 public boolean powered(){return batteryExpiresAt>System.currentTimeMillis();}
 public String batteryStatus(){long left=Math.max(0,batteryExpiresAt-System.currentTimeMillis());if(left==0)return "Insert 2 AA batteries (15 real days)";long hours=(left+3_599_999)/3_600_000;return "Battery: "+(hours/24)+"d "+(hours%24)+"h remaining";}
 public enum BatteryResult { CHARGED, ALREADY_CHARGED, NOT_ENOUGH, WRONG_ITEM, DENIED }
 public BatteryResult insertBatteries(net.minecraft.world.entity.player.Player player,net.minecraft.world.item.ItemStack held){
  if(level==null||level.isClientSide||player.level()!=level||!player.isAlive()||player.isSpectator())return BatteryResult.DENIED;
  var item=net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems.AA_BATTERY.get();
  if(!held.is(item))return BatteryResult.WRONG_ITEM;
  if(powered())return BatteryResult.ALREADY_CHARGED;
  if(!player.getAbilities().instabuild){
   int total=held.getCount();for(int i=0;i<player.getInventory().getContainerSize();i++){var stack=player.getInventory().getItem(i);if(stack!=held&&stack.is(item))total+=stack.getCount();}
   if(total<2)return BatteryResult.NOT_ENOUGH;
   int remaining=2;int take=Math.min(remaining,held.getCount());held.shrink(take);remaining-=take;
   for(int i=0;remaining>0&&i<player.getInventory().getContainerSize();i++){var stack=player.getInventory().getItem(i);if(stack!=held&&stack.is(item)){take=Math.min(remaining,stack.getCount());stack.shrink(take);remaining-=take;}}
   player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();
  }
  if(owner==null)owner=player.getUUID();
  batteryExpiresAt=System.currentTimeMillis()+BATTERY_DURATION_MS;
  changed();refreshPower();return BatteryResult.CHARGED;
 }
 public void refreshPower(){if(level!=null&&!level.isClientSide){var state=getBlockState();boolean on=powered();if(state.getValue(SecurityCameraBlock.POWERED)!=on)level.setBlock(worldPosition,state.setValue(SecurityCameraBlock.POWERED,on),3);}}
 public static void tick(Level l,BlockPos p,BlockState s,SecurityCameraEntity camera){if(l instanceof net.minecraft.server.level.ServerLevel server&&l.getGameTime()%20==0){var links=net.mcreator.commandcookieselectronicmod.power.PowerLinks.get(server);if(links.links.containsKey(p)||links.coaxLinks.containsKey(p))net.mcreator.commandcookieselectronicmod.power.PowerCables.disconnectDevice(server,p);camera.refreshPower();SecurityCameras.remember(camera);}}

}
