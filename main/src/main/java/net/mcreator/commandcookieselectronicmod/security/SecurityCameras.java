package net.mcreator.commandcookieselectronicmod.security;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.mcreator.commandcookieselectronicmod.block.SecurityCameraBlock;

/** The server remembers paired cameras even while their chunks are unloaded. */
public final class SecurityCameras extends SavedData {
 public record Entry(UUID id,UUID owner,String dimension,BlockPos position,Direction facing,String name,long expires,Set<UUID> phones){
  public Entry{position=position.immutable();phones=Set.copyOf(phones);}
  public boolean powered(){return expires>System.currentTimeMillis();}
  public boolean paired(ServerPlayer player,UUID device){return owner.equals(player.getUUID())&&phones.contains(device);}
 }
 public final Map<UUID,Entry> entries=new HashMap<>();
 public static SecurityCameras get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(SecurityCameras::new,SecurityCameras::load,null),"cem_security_cameras");}
 public static Set<UUID> readPhones(CompoundTag tag){var result=new HashSet<UUID>();for(var value:tag.getList("PairedPhones",Tag.TAG_STRING)){if(result.size()==16)break;try{result.add(UUID.fromString(value.getAsString()));}catch(IllegalArgumentException ignored){}}return result;}
 public static ListTag writePhones(Set<UUID> phones){var result=new ListTag();phones.stream().sorted().forEach(id->result.add(StringTag.valueOf(id.toString())));return result;}
 public static SecurityCameras load(CompoundTag tag,HolderLookup.Provider lookup){var data=new SecurityCameras();for(var value:tag.getList("Cameras",Tag.TAG_COMPOUND)){var n=(CompoundTag)value;if(!n.hasUUID("Identity")||!n.hasUUID("Owner")||ResourceLocation.tryParse(n.getString("Dimension"))==null)continue;var face=Direction.from3DDataValue(n.getInt("Facing"));if(!face.getAxis().isHorizontal())continue;var entry=new Entry(n.getUUID("Identity"),n.getUUID("Owner"),n.getString("Dimension"),BlockPos.of(n.getLong("Position")),face,SecurityCameraService.cleanName(n.getString("Name")),Math.max(0,n.getLong("BatteryExpiresAt")),readPhones(n));data.entries.put(entry.id(),entry);}return data;}
 @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider lookup){var list=new ListTag();entries.values().stream().sorted(Comparator.comparing(Entry::id)).forEach(e->{var n=new CompoundTag();n.putUUID("Identity",e.id());n.putUUID("Owner",e.owner());n.putString("Dimension",e.dimension());n.putLong("Position",e.position().asLong());n.putInt("Facing",e.facing().get3DDataValue());n.putString("Name",e.name());n.putLong("BatteryExpiresAt",e.expires());n.put("PairedPhones",writePhones(e.phones()));list.add(n);});tag.put("Cameras",list);return tag;}
 public static void remember(SecurityCameraEntity camera){if(!(camera.getLevel() instanceof ServerLevel level)||camera.owner==null)return;var data=get(level.getServer());var entry=new Entry(camera.identity,camera.owner,level.dimension().location().toString(),camera.getBlockPos(),camera.getBlockState().getValue(SecurityCameraBlock.FACING),camera.name,camera.batteryExpiresAt,camera.pairedPhones);if(!entry.equals(data.entries.put(entry.id(),entry)))data.setDirty();}
 public static void loaded(SecurityCameraEntity camera){if(!(camera.getLevel() instanceof ServerLevel level))return;var old=get(level.getServer()).entries.get(camera.identity);if(old!=null){if(!old.dimension().equals(level.dimension().location().toString())||!old.position().equals(camera.getBlockPos())){camera.identity=UUID.randomUUID();camera.pairedPhones.clear();camera.setChanged();}else{camera.name=old.name();camera.pairedPhones.addAll(old.phones());}}remember(camera);}
 public static void removed(ServerLevel level,UUID id){var data=get(level.getServer());if(data.entries.remove(id)!=null)data.setDirty();SecurityCameraStream.stopCamera(level.getServer(),id);}
 public static ServerLevel level(MinecraftServer server,Entry e){return server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,ResourceLocation.parse(e.dimension())));}
 public static List<Entry> paired(ServerPlayer player,UUID device){return get(player.getServer()).entries.values().stream().filter(e->e.paired(player,device)).sorted(Comparator.comparing(Entry::name,String.CASE_INSENSITIVE_ORDER).thenComparing(Entry::id)).limit(SecurityCameraPackets.MAX_CAMERAS).toList();}
 public static Entry allowed(ServerPlayer player,SecurityCameraPackets.Request request){var entry=get(player.getServer()).entries.get(request.identity());if(entry==null||!entry.owner().equals(player.getUUID()))return null;if(request.phone())return entry.phones().contains(request.device())?entry:null;return entry.dimension().equals(player.serverLevel().dimension().location().toString())&&entry.position().distSqr(request.source())<=SecurityCameraService.RANGE*SecurityCameraService.RANGE?entry:null;}
}
