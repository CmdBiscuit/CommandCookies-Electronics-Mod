package net.mcreator.commandcookiesretroexpansion.retro;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.mcreator.commandcookieselectronicmod.nes.*;

/** Uses the saved cable identities, so the main mod's routing remains authoritative. */
public final class RetroConsoles {
 public record Device(BlockEntity block,RetroSystem system){
  public BlockPos pos(){return block.getBlockPos();}
  public UUID identity(){var t=block.getUpdateTag(block.getLevel().registryAccess());return t.hasUUID("Identity")?t.getUUID("Identity"):null;}
  public ItemStack cartridge(){return ((RetroConsoleState)block).retro$cartridge();}
  public void cartridge(ItemStack stack){((RetroConsoleState)block).retro$cartridge(stack);changed();}
  public void changed(){block.setChanged();var l=block.getLevel();if(l!=null&&!l.isClientSide)l.sendBlockUpdated(pos(),block.getBlockState(),block.getBlockState(),3);}
 }
 public static Device at(Level level,BlockPos pos){
  if(!level.hasChunkAt(pos))return null;var block=level.getBlockEntity(pos);if(!(block instanceof RetroConsoleState))return null;
  var key=BuiltInRegistries.BLOCK.getKey(block.getBlockState().getBlock());if(!key.getNamespace().equals("commandcookies_electronic_mod"))return null;
  for(var system:RetroSystem.values())if(key.getPath().equals(system.id+"_console"))return new Device(block,system);return null;
 }
 public static NesDeviceBlockEntity monitor(Device console){
  var l=console.block.getLevel();if(l==null)return null;var tag=console.block.getUpdateTag(l.registryAccess());
  if(!tag.contains("Peer")||!tag.hasUUID("PeerIdentity"))return null;var pos=BlockPos.of(tag.getLong("Peer"));
  if(console.pos().distSqr(pos)>NesSystem.MAX_CABLE_DISTANCE*NesSystem.MAX_CABLE_DISTANCE||!l.hasChunkAt(pos))return null;
  if(!(l.getBlockEntity(pos) instanceof NesDeviceBlockEntity m)||!m.monitor()||!m.identity.equals(tag.getUUID("PeerIdentity"))||!console.pos().equals(m.peer)||!Objects.equals(console.identity(),m.peerIdentity))return null;
  return m;
 }
 public static Device forMonitor(NesDeviceBlockEntity monitor){
  if(!monitor.monitor()||monitor.peer==null||monitor.getLevel()==null)return null;var c=at(monitor.getLevel(),monitor.peer);return c!=null&&monitor(c)==monitor?c:null;
 }
 public static boolean baseGameBusy(Device console){return console.block instanceof NesDeviceBlockEntity n&&n.owner!=null;}
 private RetroConsoles(){}
}
