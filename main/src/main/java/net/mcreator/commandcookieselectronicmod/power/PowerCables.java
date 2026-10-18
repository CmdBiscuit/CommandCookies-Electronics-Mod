package net.mcreator.commandcookieselectronicmod.power;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.mcreator.commandcookieselectronicmod.nes.*;
import net.mcreator.commandcookieselectronicmod.pc.*;
/** Work scales with existing links; visual entities never load chunks or collide. */
public final class PowerCables {
 public static final int POWER=0,FEED=1,COAX=2,AV=3,HDMI=4;
 private record Key(BlockPos target,int mode){}
 private static final Map<ServerLevel,Map<Key,PowerCableEntity>> entities=new WeakHashMap<>();
 private static Map<Key,PowerCableEntity> cache(ServerLevel l){return entities.computeIfAbsent(l,k->new HashMap<>());}
 public static void clear(){entities.clear();}
 /** Move existing plugs with a changed endpoint without replacing the links or visual entities. */
 public static void refreshPorts(Level world,BlockPos pos){if(world instanceof ServerLevel l)for(var e:cache(l).values())if(!e.isRemoved()&&(e.start().equals(pos)||e.end().equals(pos)))e.syncPorts(e.startSpec()>>>8&1);}
 public static int capacity(OutletEntity o){return o.coax()?1:2;}
 private static boolean stale(ServerLevel l,BlockPos p,PowerLinks.Link v,boolean coax){
  if(p.distSqr(v.outlet())>PowerNetwork.OUTLET_RANGE*PowerNetwork.OUTLET_RANGE)return true;
  if(l.hasChunkAt(p)&&(!PowerNetwork.device(l,p)||!BuiltInRegistries.BLOCK.getKey(l.getBlockState(p).getBlock()).toString().equals(v.block())||!Objects.equals(PowerNetwork.identity(l,p),v.device())))return true;
  return l.hasChunkAt(v.outlet())&&(!(l.getBlockEntity(v.outlet()) instanceof OutletEntity o)||!o.identity.equals(v.outletIdentity())||o.coax()!=coax||l.hasChunkAt(p)&&!PowerNetwork.compatible(o,l,p));
 }
 /** Old saves stored coax in the power map. Move those links without losing sockets. */
 public static void migrate(ServerLevel l){var data=PowerLinks.get(l);boolean dirty=false;var it=data.links.entrySet().iterator();while(it.hasNext()){var e=it.next();if(l.hasChunkAt(e.getValue().outlet())&&l.getBlockEntity(e.getValue().outlet()) instanceof OutletEntity o&&o.coax()){data.coaxLinks.putIfAbsent(e.getKey(),e.getValue());it.remove();remove(l,new Key(e.getKey(),POWER));dirty=true;}}if(dirty)data.setDirty();}
 public static void normalize(ServerLevel l,OutletEntity outlet){
  migrate(l);var data=PowerLinks.get(l);var map=data.channel(outlet.coax());int mode=outlet.coax()?COAX:POWER;
  boolean dirty=map.entrySet().removeIf(e->{if(!e.getValue().outlet().equals(outlet.getBlockPos())||!stale(l,e.getKey(),e.getValue(),outlet.coax()))return false;remove(l,new Key(e.getKey(),mode));return true;});
  var group=map.entrySet().stream().filter(e->e.getValue().outlet().equals(outlet.getBlockPos())&&e.getValue().outletIdentity().equals(outlet.identity)).sorted(Comparator.<Map.Entry<BlockPos,PowerLinks.Link>>comparingInt(e->e.getValue().socket()<0?2:e.getValue().socket()).thenComparingLong(e->e.getKey().asLong())).toList();int mask=0;
  for(var entry:group){var link=entry.getValue();int slot=link.socket();if(slot<0||slot>=capacity(outlet)||(mask&1<<slot)!=0)slot=(mask&1)==0?0:capacity(outlet)==2&&(mask&2)==0?1:-1;
   if(slot<0){map.remove(entry.getKey());remove(l,new Key(entry.getKey(),mode));dirty=true;continue;}
   mask|=1<<slot;if(slot!=link.socket()){map.put(entry.getKey(),new PowerLinks.Link(link.block(),link.device(),link.outlet(),link.outletIdentity(),slot));dirty=true;}
  }
  if(dirty)data.setDirty();if(outlet.socketMask!=mask){outlet.socketMask=mask;outlet.changed();}
 }
 public static int socketFor(ServerLevel l,OutletEntity outlet,BlockPos target){normalize(l,outlet);var existing=PowerLinks.get(l).channel(outlet.coax()).get(target);if(existing!=null&&existing.outlet().equals(outlet.getBlockPos())&&existing.outletIdentity().equals(outlet.identity))return existing.socket();return (outlet.socketMask&1)==0?0:capacity(outlet)==2&&(outlet.socketMask&2)==0?1:-1;}
 public static void connect(ServerLevel l,OutletEntity outlet,BlockPos target,int socket){var data=PowerLinks.get(l);var link=new PowerLinks.Link(BuiltInRegistries.BLOCK.getKey(l.getBlockState(target).getBlock()).toString(),PowerNetwork.identity(l,target),outlet.getBlockPos().immutable(),outlet.identity,socket);var previous=data.channel(outlet.coax()).put(target.immutable(),link);data.setDirty();if(previous!=null&&!previous.outlet().equals(outlet.getBlockPos())&&l.hasChunkAt(previous.outlet())&&l.getBlockEntity(previous.outlet()) instanceof OutletEntity old)normalize(l,old);normalize(l,outlet);ensureConnection(l,target,link,outlet.coax());}
 public static void maintain(ServerLevel l){
  migrate(l);var data=PowerLinks.get(l);for(boolean coax:new boolean[]{false,true}){var map=data.channel(coax);int mode=coax?COAX:POWER;boolean changed=map.entrySet().removeIf(e->{if(!stale(l,e.getKey(),e.getValue(),coax))return false;remove(l,new Key(e.getKey(),mode));return true;});if(changed)data.setDirty();
   var outlets=new HashSet<BlockPos>();for(var v:map.values())outlets.add(v.outlet());for(var p:outlets)if(l.hasChunkAt(p)&&l.getBlockEntity(p) instanceof OutletEntity o)normalize(l,o);
   for(var entry:map.entrySet())ensureConnection(l,entry.getKey(),entry.getValue(),coax);
  }cache(l).entrySet().removeIf(e->e.getValue().isRemoved());
 }
 private static void ensureConnection(ServerLevel l,BlockPos target,PowerLinks.Link link,boolean coax){if(!l.hasChunkAt(target)||!l.hasChunkAt(link.outlet())||link.socket()<0||stale(l,target,link,coax))return;ensure(l,link.outlet(),target,link.outletIdentity(),link.device(),coax?COAX:POWER,link.socket());}
 public static void ensureFeed(ServerLevel l,OutletEntity o){if(o.generator==null||o.generatorIdentity==null){remove(l,new Key(o.getBlockPos(),FEED));return;}if(!l.hasChunkAt(o.generator))return;ensure(l,o.generator,o.getBlockPos(),o.generatorIdentity,o.identity,FEED,0);}
 public static void ensureAv(NesDeviceBlockEntity d){if(!(d.getLevel() instanceof ServerLevel l))return;var other=NesSystem.peer(d);if(other==null)return;var source=d.monitor()?other:d;var target=d.monitor()?d:other;ensure(l,source.getBlockPos(),target.getBlockPos(),source.identity,target.identity,AV,0);}
 public static void ensureSnesAv(net.mcreator.commandcookieselectronicmod.snes.SnesDeviceBlockEntity d){if(!(d.getLevel() instanceof ServerLevel l))return;var monitor=net.mcreator.commandcookieselectronicmod.snes.SnesSystem.peer(d);if(monitor!=null)ensure(l,d.getBlockPos(),monitor.getBlockPos(),d.identity,monitor.identity,AV,0);}
 public static void ensureN64Av(net.mcreator.commandcookieselectronicmod.n64.N64DeviceBlockEntity d){if(!(d.getLevel() instanceof ServerLevel l))return;var monitor=net.mcreator.commandcookieselectronicmod.n64.N64System.peer(d);if(monitor!=null)ensure(l,d.getBlockPos(),monitor.getBlockPos(),d.identity,monitor.identity,AV,0);}
 public static void ensureHdmi(PcDeviceEntity d){if(!(d.getLevel() instanceof ServerLevel l))return;var other=PcSystem.peer(d);if(other==null)return;var source=d.monitor()?other:d;var target=d.monitor()?d:other;ensure(l,source.getBlockPos(),target.getBlockPos(),source.identity,target.identity,HDMI,0);}
 private static void ensure(ServerLevel l,BlockPos source,BlockPos target,UUID aid,UUID bid,int mode,int slot){
  var key=new Key(target.immutable(),mode);var old=cache(l).get(key);if(old!=null&&!old.isRemoved()&&old.start().equals(source)&&Objects.equals(aid,old.sourceIdentity)&&Objects.equals(bid,old.targetIdentity)){old.syncPorts(slot);return;}
  remove(l,key);var cable=PowerCableRegistration.TYPE.create(l);if(cable==null)return;cable.configure(source,target,aid,bid,mode,slot);if(l.addFreshEntity(cable))cache(l).put(key,cable);
 }
 private static void remove(ServerLevel l,Key key){var e=cache(l).remove(key);if(e!=null)e.discard();}
 public static void signalRemoved(Level world,BlockPos pos,int mode){if(world instanceof ServerLevel l)cache(l).entrySet().removeIf(v->{var e=v.getValue();if(e.mode!=mode||!e.start().equals(pos)&&!e.end().equals(pos))return false;e.discard();return true;});}
 public static boolean valid(PowerCableEntity e){
  if(!(e.level() instanceof ServerLevel l))return true;if(!l.hasChunkAt(e.start())||!l.hasChunkAt(e.end()))return true;
  if(!Objects.equals(PowerNetwork.identity(l,e.start()),e.sourceIdentity)||!Objects.equals(PowerNetwork.identity(l,e.end()),e.targetIdentity))return false;
  if(e.mode==FEED)return l.getBlockEntity(e.end()) instanceof OutletEntity o&&e.start().equals(o.generator)&&Objects.equals(e.sourceIdentity,o.generatorIdentity)&&e.start().distSqr(e.end())<=PowerNetwork.GENERATOR_RANGE*PowerNetwork.GENERATOR_RANGE;
  if(e.mode==AV){var source=l.getBlockEntity(e.start());boolean paired=source instanceof net.mcreator.commandcookieselectronicmod.n64.N64DeviceBlockEntity n64?net.mcreator.commandcookieselectronicmod.n64.N64System.peer(n64)==l.getBlockEntity(e.end()):source instanceof net.mcreator.commandcookieselectronicmod.snes.SnesDeviceBlockEntity snes?net.mcreator.commandcookieselectronicmod.snes.SnesSystem.peer(snes)==l.getBlockEntity(e.end()):source instanceof NesDeviceBlockEntity d&&!d.monitor()&&NesSystem.peer(d)==l.getBlockEntity(e.end());return paired&&e.start().distSqr(e.end())<=NesSystem.MAX_CABLE_DISTANCE*NesSystem.MAX_CABLE_DISTANCE;}
  if(e.mode==HDMI)return l.getBlockEntity(e.start()) instanceof PcDeviceEntity d&&!d.monitor()&&PcSystem.peer(d)==l.getBlockEntity(e.end());
  boolean coax=e.mode==COAX;var link=PowerLinks.get(l).channel(coax).get(e.end());return link!=null&&link.outlet().equals(e.start())&&link.outletIdentity().equals(e.sourceIdentity)&&link.socket()==(e.startSpec()>>>8&1)&&!stale(l,e.end(),link,coax);
 }
 private static void removeLinks(ServerLevel l,BlockPos pos,boolean physical){var data=PowerLinks.get(l);var affected=new HashSet<BlockPos>();boolean dirty=false;for(boolean coax:new boolean[]{false,true}){var map=data.channel(coax);dirty|=map.entrySet().removeIf(v->{if(!v.getKey().equals(pos)&&(!physical||!v.getValue().outlet().equals(pos)))return false;affected.add(v.getValue().outlet());return true;});}if(dirty)data.setDirty();for(var p:affected)if(l.hasChunkAt(p)&&l.getBlockEntity(p) instanceof OutletEntity o&&(!physical||!p.equals(pos)))normalize(l,o);}
 public static void disconnectDevice(ServerLevel l,BlockPos pos){signalRemoved(l,pos,POWER);signalRemoved(l,pos,COAX);removeLinks(l,pos,false);}
 public static void endpointRemoved(Level world,BlockPos pos){if(!(world instanceof ServerLevel l))return;cache(l).entrySet().removeIf(v->{var e=v.getValue();if(!e.start().equals(pos)&&!e.end().equals(pos))return false;e.discard();return true;});removeLinks(l,pos,true);}
}
