package net.mcreator.commandcookieselectronicmod.security;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.mcreator.commandcookieselectronicmod.block.*;
import net.mcreator.commandcookieselectronicmod.block.entity.LaptopBlockEntity;
import net.mcreator.commandcookieselectronicmod.pc.*;
import net.mcreator.commandcookieselectronicmod.phone.PhoneDeviceIdentity;
import net.mcreator.commandcookieselectronicmod.power.PowerNetwork;
import net.mcreator.commandcookieselectronicmod.procedures.IphoneLogicProcedure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="commandcookies_electronic_mod")
public final class SecurityCameraService {
 public static final int RANGE=64;
 private static final Map<MinecraftServer,Map<UUID,long[]>> clocks=new WeakHashMap<>();
 public static String cleanName(String text){String value=text==null?"":text.replaceAll("[\\p{Cntrl}\\u00a7]","").strip();return value.isEmpty()?"Security Camera":value.substring(0,Math.min(32,value.length()));}
 public static boolean authorized(ServerPlayer p,SecurityCameraPackets.Request r){
  if(!p.isAlive()||p.isSpectator())return false;
  if(r.phone()){for(var stack:List.of(p.getMainHandItem(),p.getOffhandItem()))if(IphoneLogicProcedure.color(stack)!=null&&r.device().equals(PhoneDeviceIdentity.id(stack)))return true;return false;}
  if(p.distanceToSqr(r.source().getX()+.5,r.source().getY()+.5,r.source().getZ()+.5)>64||!p.serverLevel().hasChunkAt(r.source()))return false;
  var entity=p.serverLevel().getBlockEntity(r.source());
  if(entity instanceof LaptopBlockEntity laptop)return laptop.getBlockState().getBlock() instanceof LaptopBlock&&laptop.getBlockState().getValue(LaptopBlock.OPEN)&&r.device().equals(laptop.deviceId)&&r.session().equals(laptop.desktopSessionId);
  if(entity instanceof PcDeviceEntity monitor&&monitor.monitor()&&PcSystem.active(monitor)){var pc=PcSystem.peer(monitor);return pc!=null&&r.device().equals(pc.identity)&&r.session().equals(pc.desktopSession);}
  return false;
 }
 public static BlockPos origin(ServerPlayer p,SecurityCameraPackets.Request r){return r.phone()?p.blockPosition():r.source();}
 public static List<SecurityCameraPackets.Feed> cameras(ServerPlayer p,BlockPos origin){
  var level=p.serverLevel();var result=new ArrayList<SecurityCameraPackets.Feed>();
  for(int x=Math.floorDiv(origin.getX()-RANGE,16);x<=Math.floorDiv(origin.getX()+RANGE,16);x++)for(int z=Math.floorDiv(origin.getZ()-RANGE,16);z<=Math.floorDiv(origin.getZ()+RANGE,16);z++){
   var chunk=level.getChunkSource().getChunkNow(x,z);if(chunk==null)continue;
   for(var entity:chunk.getBlockEntities().values())if(entity instanceof SecurityCameraEntity camera&&p.getUUID().equals(camera.owner)&&origin.distSqr(camera.getBlockPos())<=RANGE*RANGE){var state=camera.getBlockState();result.add(new SecurityCameraPackets.Feed(camera.getBlockPos().immutable(),camera.identity,camera.name,state.getValue(SecurityCameraBlock.FACING),PowerNetwork.powered(level,camera.getBlockPos()),level.dimension().location().toString()));}
  }
  result.sort(Comparator.comparingDouble((SecurityCameraPackets.Feed f)->origin.distSqr(f.position())).thenComparing(SecurityCameraPackets.Feed::name,String.CASE_INSENSITIVE_ORDER));return List.copyOf(result.subList(0,Math.min(SecurityCameraPackets.MAX_CAMERAS,result.size())));
 }
 public static boolean rename(ServerPlayer p,BlockPos origin,BlockPos pos,UUID identity,String name){if(origin.distSqr(pos)>RANGE*RANGE||!p.serverLevel().hasChunkAt(pos)||!(p.serverLevel().getBlockEntity(pos) instanceof SecurityCameraEntity camera)||!identity.equals(camera.identity)||!p.getUUID().equals(camera.owner))return false;camera.name=cleanName(name);camera.changed();return true;}
 public static java.util.List<SecurityCameraPackets.Feed> cameras(ServerPlayer p,SecurityCameraPackets.Request r){return r.phone()?SecurityCameras.paired(p,r.device()).stream().map(e->new SecurityCameraPackets.Feed(e.position(),e.id(),e.name(),e.facing(),e.powered(),e.dimension())).toList():cameras(p,origin(p,r));}
 private static void reply(ServerPlayer p,SecurityCameraPackets.Request r,boolean success,String status){PacketDistributor.sendToPlayer(p,new SecurityCameraPackets.Snapshot(r.phone(),r.source(),r.device(),r.session(),r.request(),success,status,success?cameras(p,r):List.of()));}
 public static boolean rename(ServerPlayer p,SecurityCameraPackets.Request r){var entry=SecurityCameras.allowed(p,r);if(entry==null)return false;var level=SecurityCameras.level(p.getServer(),entry);if(level==null)return false;level.getChunk(entry.position());if(!(level.getBlockEntity(entry.position()) instanceof SecurityCameraEntity camera)||!camera.identity.equals(entry.id())||!p.getUUID().equals(camera.owner))return false;camera.name=cleanName(r.name());camera.changed();return true;}
 public static void handle(ServerPlayer p,SecurityCameraPackets.Request r){
  if(r.action()<SecurityCameraPackets.SYNC||r.action()>SecurityCameraPackets.STOP)return;
  if(r.action()==SecurityCameraPackets.STOP){SecurityCameraStream.stopRequest(p,r);return;}
  if(!authorized(p,r)){SecurityCameraStream.stop(p,"Camera unavailable.",true);reply(p,r,false,r.phone()?"Hold this phone to view cameras.":"This computer is no longer available.");return;}
  long now=p.getServer().getTickCount();var times=clocks.computeIfAbsent(p.getServer(),s->new HashMap<>()).computeIfAbsent(p.getUUID(),id->new long[]{Long.MIN_VALUE,Long.MIN_VALUE,Long.MIN_VALUE,Long.MIN_VALUE});int action=r.action();long gap=action==SecurityCameraPackets.SYNC?10:20;
  if(times[action]!=Long.MIN_VALUE&&now>=times[action]&&now-times[action]<gap){if(action!=SecurityCameraPackets.SYNC)reply(p,r,false,"Please wait a moment before trying again.");return;}times[action]=now;
  if(action==SecurityCameraPackets.RENAME&&!rename(p,r)){reply(p,r,false,"That camera is no longer available.");return;}
  if(action==SecurityCameraPackets.VIEW&&!SecurityCameraStream.begin(p,r)){reply(p,r,false,"Camera unavailable or needs batteries.");return;}
  if(action==SecurityCameraPackets.SYNC)SecurityCameraStream.keepAlive(p,r);
  reply(p,r,true,action==SecurityCameraPackets.RENAME?"Camera renamed.":"");
 }
 @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p){SecurityCameraStream.stop(p,"",false);var values=clocks.get(p.getServer());if(values!=null)values.remove(p.getUUID());}}
 @SubscribeEvent public static void stop(ServerStoppedEvent event){SecurityCameraStream.clear(event.getServer());clocks.remove(event.getServer());}
}
