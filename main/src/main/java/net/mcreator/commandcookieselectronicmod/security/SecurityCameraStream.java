package net.mcreator.commandcookieselectronicmod.security;

import java.util.*;
import java.security.*;
import io.netty.buffer.Unpooled;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.mcreator.commandcookieselectronicmod.block.SecurityCameraBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** A viewer holds temporary camera-region tickets; their player stays in place. */
@EventBusSubscriber(modid="commandcookies_electronic_mod")
public final class SecurityCameraStream {
 private static final TicketType<UUID> TICKET=TicketType.create("cem_camera_view",UUID::compareTo,100);
 private static final Map<MinecraftServer,Map<UUID,View>> viewers=new WeakHashMap<>();
 private static final class View {
  final ServerPlayer player;final SecurityCameraPackets.Request request;final SecurityCameras.Entry entry;final ServerLevel level;final ChunkPos center;final UUID token=UUID.randomUUID();final List<ChunkPos> chunks=new ArrayList<>();final Map<Long,byte[]> hashes=new HashMap<>();int cursor;long keepAlive;
  View(ServerPlayer p,SecurityCameraPackets.Request r,SecurityCameras.Entry e,ServerLevel l){player=p;request=r;entry=e;level=l;center=new ChunkPos(e.position());for(int x=-SecurityCameraWire.RADIUS;x<=SecurityCameraWire.RADIUS;x++)for(int z=-SecurityCameraWire.RADIUS;z<=SecurityCameraWire.RADIUS;z++)chunks.add(new ChunkPos(center.x+x,center.z+z));chunks.sort(Comparator.comparingInt(c->Math.abs(c.x-center.x)+Math.abs(c.z-center.z)));keepAlive=p.getServer().getTickCount();}
 }
 public static boolean begin(ServerPlayer player,SecurityCameraPackets.Request request){if(!SecurityCameraService.authorized(player,request))return false;var entry=SecurityCameras.allowed(player,request);if(entry==null||!entry.powered())return false;var level=SecurityCameras.level(player.getServer(),entry);if(level==null)return false;
  level.getChunk(entry.position());if(!(level.getBlockEntity(entry.position()) instanceof SecurityCameraEntity camera)||!camera.identity.equals(entry.id())||!camera.powered()){if(!(level.getBlockEntity(entry.position()) instanceof SecurityCameraEntity valid)||!valid.identity.equals(entry.id()))SecurityCameras.removed(level,entry.id());return false;}
  if(!player.getUUID().equals(camera.owner)||request.phone()&&!camera.pairedPhones.contains(request.device()))return false;
  SecurityCameras.remember(camera);stop(player,"",false);var view=new View(player,request,SecurityCameras.get(player.getServer()).entries.get(entry.id()),level);viewers.computeIfAbsent(player.getServer(),s->new HashMap<>()).put(player.getUUID(),view);level.getChunkSource().addRegionTicket(TICKET,view.center,SecurityCameraWire.RADIUS+1,view.token);
  PacketDistributor.sendToPlayer(player,new SecurityCameraWire.Start(request.device(),request.session(),request.request(),entry.id(),view.token,entry.dimension(),level.dimensionTypeRegistration().unwrapKey().orElseThrow().location().toString(),entry.position(),camera.getBlockState().getValue(SecurityCameraBlock.FACING),level.getDayTime(),level.isFlat()));return true;
 }
 public static void keepAlive(ServerPlayer player,SecurityCameraPackets.Request r){var map=viewers.get(player.getServer());var view=map==null?null:map.get(player.getUUID());if(view!=null&&view.request.session().equals(r.session())&&view.request.device().equals(r.device())&&view.entry.id().equals(r.identity()))view.keepAlive=player.getServer().getTickCount();}
 public static void stop(ServerPlayer player,String reason,boolean notify){var map=viewers.get(player.getServer());var view=map==null?null:map.remove(player.getUUID());if(view==null)return;view.level.getChunkSource().removeRegionTicket(TICKET,view.center,SecurityCameraWire.RADIUS+1,view.token);if(notify)PacketDistributor.sendToPlayer(player,new SecurityCameraWire.Stop(view.token,reason));}
 public static void stopRequest(ServerPlayer player,SecurityCameraPackets.Request r){var map=viewers.get(player.getServer());var view=map==null?null:map.get(player.getUUID());if(view!=null&&view.request.session().equals(r.session())&&view.request.device().equals(r.device()))stop(player,"",true);}
 public static void stopCamera(MinecraftServer server,UUID id){var map=viewers.get(server);if(map!=null)for(var view:List.copyOf(map.values()))if(view.entry.id().equals(id))stop(view.player,"Camera was removed.",true);}
 public static void clear(MinecraftServer server){var map=viewers.get(server);if(map!=null)for(var view:List.copyOf(map.values()))stop(view.player,"",false);viewers.remove(server);}
 public static int active(MinecraftServer server){var map=viewers.get(server);return map==null?0:map.size();}
 private static void chunk(View view,ChunkPos pos){var chunk=view.level.getChunk(pos.x,pos.z);var packet=new ClientboundLevelChunkWithLightPacket(chunk,view.level.getChunkSource().getLightEngine(),null,null);var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),view.level.registryAccess());try{ClientboundLevelChunkWithLightPacket.STREAM_CODEC.encode(buffer,packet);var raw=SecurityCameraWire.bytes(buffer);var digest=MessageDigest.getInstance("SHA-256").digest(raw);if(!Arrays.equals(view.hashes.put(pos.toLong(),digest),digest))PacketDistributor.sendToPlayer(view.player,new SecurityCameraWire.Chunk(view.token,pos.x,pos.z,SecurityCameraWire.pack(raw)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}finally{buffer.release();}}
 private static void actors(View view){var p=view.entry.position();var entities=view.level.getEntities((Entity)null,new AABB(p).inflate(SecurityCameraWire.RADIUS*16,32,SecurityCameraWire.RADIUS*16),e->!e.isRemoved()&&!(e instanceof Marker)).stream().limit(128).toList();var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),view.level.registryAccess());try{buffer.writeVarInt(entities.size());for(var e:entities){ClientboundAddEntityPacket.STREAM_CODEC.encode(buffer,new ClientboundAddEntityPacket(e,0,e.blockPosition()));var values=e.getEntityData().getNonDefaultValues();ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,new ClientboundSetEntityDataPacket(e.getId(),values==null?List.of():values));buffer.writeBoolean(e instanceof LivingEntity);if(e instanceof LivingEntity living){var equipment=new ArrayList<Pair<EquipmentSlot,net.minecraft.world.item.ItemStack>>();for(var slot:EquipmentSlot.values())equipment.add(Pair.of(slot,living.getItemBySlot(slot).copy()));ClientboundSetEquipmentPacket.STREAM_CODEC.encode(buffer,new ClientboundSetEquipmentPacket(e.getId(),equipment));}}PacketDistributor.sendToPlayer(view.player,new SecurityCameraWire.Actors(view.token,view.level.getDayTime(),SecurityCameraWire.pack(SecurityCameraWire.bytes(buffer))));}finally{buffer.release();}}
 @SubscribeEvent public static void tick(ServerTickEvent.Post event){var server=event.getServer();var map=viewers.get(server);if(map==null)return;for(var view:List.copyOf(map.values())){
  var current=SecurityCameras.allowed(view.player,view.request);if(server.getTickCount()-view.keepAlive>80||!SecurityCameraService.authorized(view.player,view.request)||current==null){stop(view.player,"Camera unavailable.",true);continue;}if(!current.powered()){stop(view.player,"Needs batteries",true);continue;}
  view.level.getChunkSource().addRegionTicket(TICKET,view.center,SecurityCameraWire.RADIUS+1,view.token);
  if(!(view.level.getBlockEntity(view.entry.position()) instanceof SecurityCameraEntity camera)||!camera.identity.equals(view.entry.id())){SecurityCameras.removed(view.level,view.entry.id());continue;}
  try{for(int n=0;n<2;n++){chunk(view,view.chunks.get(view.cursor));view.cursor=(view.cursor+1)%view.chunks.size();}if(server.getTickCount()%5==0)actors(view);}catch(RuntimeException failure){net.mcreator.commandcookieselectronicmod.CommandcookiesElectronicModMod.LOGGER.warn("Camera stream unavailable",failure);stop(view.player,"Camera stream unavailable.",true);}
 }}
}
