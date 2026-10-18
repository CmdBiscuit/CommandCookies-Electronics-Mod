package net.mcreator.commandcookieselectronicmod.power;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.mcreator.commandcookieselectronicmod.block.*;
import net.mcreator.commandcookieselectronicmod.nes.*;
import net.neoforged.neoforge.network.PacketDistributor;
public final class PowerNetwork {
    public static final int GENERATOR_RANGE=120,OUTLET_RANGE=15;
    public static final String SELECTION="CemPowerLink";
    private record HighlightStamp(BlockPos source,long tick){}
    private static final Map<UUID,HighlightStamp> highlights=new HashMap<>();
    private static final Map<ServerLevel,Map<BlockPos,UUID>> selectionsWithoutEntities=new WeakHashMap<>();
    /** Multi-block screens share one physical power endpoint. */
    public static BlockPos target(Level l,BlockPos p){var tv=net.mcreator.commandcookieselectronicmod.tv.TvParts.master(l,p);return tv==null?p:tv.getBlockPos();}
    private static void refreshDevice(Level l,BlockPos pos){var be=l.getBlockEntity(pos);if(be instanceof net.mcreator.commandcookieselectronicmod.pc.PcDeviceEntity pc)net.mcreator.commandcookieselectronicmod.pc.PcSystem.refresh(pc);else if(be instanceof net.mcreator.commandcookieselectronicmod.tv.ImageTvEntity tv)tv.refreshPower();}
    public static boolean device(Level l,BlockPos p){var b=l.getBlockState(p).getBlock();return b instanceof net.mcreator.commandcookieselectronicmod.tv.ImageTvBlock||b instanceof CrtTvBlock||b instanceof N64ConsoleBlock||b instanceof SnesConsoleBlock||b instanceof NesDeviceBlock||b instanceof net.mcreator.commandcookieselectronicmod.pc.PcDeviceBlock;}
    public static boolean requiresCoax(Level l,BlockPos p){var b=l.getBlockState(p).getBlock();return b instanceof CrtTvBlock||b instanceof NesDeviceBlock n&&n.monitor;}
    public static boolean compatible(OutletEntity source,Level l,BlockPos p){return device(l,p)&&(!source.coax()||requiresCoax(l,p));}
    /** Click order and current power do not change physical cable compatibility. */
    public static int connectionRange(Level l,BlockPos a,BlockPos b){
        if(!l.hasChunkAt(a)||!l.hasChunkAt(b))return 0;var first=l.getBlockEntity(a);var second=l.getBlockEntity(b);
        if(first instanceof GeneratorEntity&&second instanceof OutletEntity||first instanceof OutletEntity&&second instanceof GeneratorEntity)return GENERATOR_RANGE;
        if(first instanceof OutletEntity o&&compatible(o,l,b)||second instanceof OutletEntity outlet&&compatible(outlet,l,a))return OUTLET_RANGE;
        return 0;
    }
    public static UUID identity(Level l,BlockPos p){var d=l.getBlockEntity(p);return d instanceof net.mcreator.commandcookieselectronicmod.tv.ImageTvEntity tv?tv.identity:d instanceof net.mcreator.commandcookieselectronicmod.security.SecurityCameraEntity c?c.identity:d instanceof GeneratorEntity g?g.identity:d instanceof OutletEntity o?o.identity:d instanceof net.mcreator.commandcookieselectronicmod.n64.N64DeviceBlockEntity n64?n64.identity:d instanceof net.mcreator.commandcookieselectronicmod.snes.SnesDeviceBlockEntity snes?snes.identity:d instanceof NesDeviceBlockEntity n?n.identity:d instanceof net.mcreator.commandcookieselectronicmod.pc.PcDeviceEntity pc?pc.identity:null;}
    // CRT TVs have no block entity. Give only their pending selection a session identity;
    // saved power links keep their existing format and survive reloads.
    private static UUID selectionIdentity(ServerLevel l,BlockPos p){var id=identity(l,p);return id!=null?id:device(l,p)?selectionsWithoutEntities.computeIfAbsent(l,k->new HashMap<>()).computeIfAbsent(p.immutable(),k->UUID.randomUUID()):null;}
    private static String blockId(Level l,BlockPos p){return BuiltInRegistries.BLOCK.getKey(l.getBlockState(p).getBlock()).toString();}
    public static boolean powered(Level l,BlockPos p){
        if(!(l instanceof ServerLevel level)||!l.hasChunkAt(p))return false;
        if(l.getBlockEntity(p) instanceof net.mcreator.commandcookieselectronicmod.security.SecurityCameraEntity camera)return camera.powered();
        return connected(level,p,false)&&(!requiresCoax(l,p)||connected(level,p,true));
    }
    public static boolean connected(ServerLevel level,BlockPos p,boolean coax){
        Level l=level;if(!l.hasChunkAt(p))return false;var link=PowerLinks.get(level).channel(coax).get(p);if(link==null||!device(l,p)||!link.block().equals(blockId(l,p))||!Objects.equals(link.device(),identity(l,p))||p.distSqr(link.outlet())>OUTLET_RANGE*OUTLET_RANGE||!l.hasChunkAt(link.outlet()))return false;
        return l.getBlockEntity(link.outlet()) instanceof OutletEntity outlet&&outlet.identity.equals(link.outletIdentity())&&outlet.coax()==coax&&compatible(outlet,l,p)&&outlet.powered();
    }
    public static void removed(Level l,BlockPos p){PowerCables.endpointRemoved(l,p);if(l.isClientSide)PowerPayloads.blockRemoved(p);else if(l instanceof ServerLevel s){var ids=selectionsWithoutEntities.get(s);if(ids!=null)ids.remove(p);PacketDistributor.sendToPlayersTrackingChunk(s,new net.minecraft.world.level.ChunkPos(p),new PowerPayloads.Removed(p));}}
    public static void forget(Level l,BlockPos p){removed(l,p);if(l instanceof ServerLevel s){var data=PowerLinks.get(s);if(data.links.remove(p)!=null)data.setDirty();}}
    public static void clearHighlight(ServerPlayer p,BlockPos source){highlights.remove(p.getUUID());PacketDistributor.sendToPlayer(p,new PowerPayloads.Highlight(p.level().dimension().location().toString(),source,List.of(),0));}
    public static void clear(){highlights.clear();selectionsWithoutEntities.clear();PowerCables.clear();}
    public static void leave(UUID id){highlights.remove(id);}
    public static void tick(ServerLevel level){
        if(level.getGameTime()%20!=0)return;PowerCables.maintain(level);
        var data=PowerLinks.get(level);
        var devices=new HashSet<BlockPos>(data.links.keySet());devices.addAll(data.coaxLinks.keySet());
        for(var p:devices){if(!level.hasChunkAt(p))continue;
            var state=level.getBlockState(p);
            if(state.getBlock() instanceof CrtTvBlock){boolean on=powered(level,p);if(state.getValue(CrtTvBlock.POWERED)!=on)level.setBlock(p,state.setValue(CrtTvBlock.POWERED,on),3);}
        }
    }
    public static CompoundTag selection(ItemStack item){return item.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound(SELECTION);}
    private static void selection(ItemStack item,CompoundTag value){var tag=item.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();if(value==null)tag.remove(SELECTION);else{tag.put(SELECTION,value);tag.remove(net.mcreator.commandcookieselectronicmod.pc.PcInputs.SELECTION);}item.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
    private static void notice(Player p,String s){p.displayClientMessage(Component.literal(s),true);}
    private static boolean validSelection(CompoundTag tag,ServerPlayer p){long now=p.level().getGameTime();return tag.hasUUID("Identity")&&tag.hasUUID("Owner")&&p.getUUID().equals(tag.getUUID("Owner"))&&tag.getString("Dimension").equals(p.level().dimension().location().toString())&&tag.getLong("Until")>=now&&tag.getLong("Until")<=now+2400;}
    public static void wrench(ServerPlayer p,BlockPos target,ItemStack item){
        var l=p.serverLevel();target=target(l,target);if(!p.isAlive()||p.isSpectator()||p.distanceToSqr(target.getX()+.5,target.getY()+.5,target.getZ()+.5)>64)return;
        var be=l.getBlockEntity(target);
        if(p.isShiftKeyDown()){
            selection(item,null);
            if(be instanceof OutletEntity o){o.generator=null;o.generatorIdentity=null;o.changed();PowerCables.ensureFeed(l,o);notice(p,"Outlet disconnected.");}
            else if(device(l,target)){PowerCables.disconnectDevice(l,target);var state=l.getBlockState(target);if(state.getBlock() instanceof CrtTvBlock)l.setBlock(target,state.setValue(CrtTvBlock.POWERED,false),3);refreshDevice(l,target);notice(p,"Device disconnected.");}
            else notice(p,"Wrench selection cleared.");
            clearHighlight(p,target);return;
        }
        var tag=selection(item);long now=l.getGameTime();
        boolean valid=validSelection(tag,p);
        if(valid){
            var source=BlockPos.of(tag.getLong("Position"));
            if(source.equals(target)){selection(item,null);clearHighlight(p,target);notice(p,"Selection cleared.");return;}
            if(!l.hasChunkAt(source)||!Objects.equals(tag.getUUID("Identity"),selectionIdentity(l,source))){selection(item,null);clearHighlight(p,target);notice(p,"The selected block was removed. Select a source again.");return;}
            var first=l.getBlockEntity(source);
            int range=connectionRange(l,source,target);
            if(range>0&&source.distSqr(target)>range*range){notice(p,(range==GENERATOR_RANGE?"Generator":"Outlet")+" connections reach up to "+range+" blocks.");return;}
            if(range==GENERATOR_RANGE){
                var g=first instanceof GeneratorEntity generator?generator:(GeneratorEntity)be;
                var o=first instanceof OutletEntity outlet?outlet:(OutletEntity)be;
                o.generator=g.getBlockPos().immutable();o.generatorIdentity=g.identity;o.changed();PowerCables.ensureFeed(l,o);
                selection(item,null);notice(p,"Outlet connected to the generator.");clearHighlight(p,target);return;
            }
            if(range==OUTLET_RANGE){
                var outlet=first instanceof OutletEntity o?o:(OutletEntity)be;var pos=first instanceof OutletEntity?target:source;
                int socket=PowerCables.socketFor(l,outlet,pos);if(socket<0){notice(p,outlet.coax()?"This coax outlet already has a cable. Disconnect its display or use another coax outlet.":"This outlet already has two cables. Crouch-wrench a connected device to free a socket.");return;}
                PowerCables.connect(l,outlet,pos,socket);
                refreshDevice(l,pos);selection(item,null);notice(p,outlet.coax()?"Coax cable connected. The TV or retro monitor also needs wall power.":requiresCoax(l,pos)?"Power cord connected. The TV or retro monitor also needs coax.":"Power cord connected.");clearHighlight(p,target);return;
            }
            var outlet=first instanceof OutletEntity o?o:be instanceof OutletEntity o?o:null;
            notice(p,outlet!=null&&outlet.coax()&&(device(l,source)||device(l,target))?"Coax outlets connect only to the CRT TV and retro monitor.":"Connect a generator to an outlet, or an outlet to a compatible device.");return;
        }
        if(!(be instanceof GeneratorEntity)&&!(be instanceof OutletEntity)&&!device(l,target)){notice(p,"Select a generator, outlet, or compatible device with the wrench.");return;}
        var saved=new CompoundTag();saved.putUUID("Identity",selectionIdentity(l,target));saved.putUUID("Owner",p.getUUID());saved.putLong("Position",target.asLong());saved.putString("Dimension",l.dimension().location().toString());saved.putLong("Until",now+2400);selection(item,saved);
        notice(p,be instanceof GeneratorEntity?"Generator selected. Click a highlighted outlet.":be instanceof OutletEntity?"Outlet selected. Click a generator or compatible device.":"Device selected. Click a compatible outlet.");highlight(p,target);
    }
    /** Long feed previews inspect loaded block entities; device previews scan only 15 blocks. */
    public static void highlight(ServerPlayer p,BlockPos source){
        var l=p.serverLevel();long now=l.getGameTime();var previous=highlights.get(p.getUUID());if(previous!=null&&previous.source().equals(source)&&previous.tick()>now-10)return;highlights.put(p.getUUID(),new HighlightStamp(source.immutable(),now));
        var be=l.getBlockEntity(source);if(!(be instanceof GeneratorEntity)&&!(be instanceof OutletEntity)&&!device(l,source))return;
        var list=new ArrayList<BlockPos>();
        int range=GENERATOR_RANGE;
        if(be instanceof GeneratorEntity||be instanceof OutletEntity){
            int minX=Math.floorDiv(source.getX()-range,16),maxX=Math.floorDiv(source.getX()+range,16);
            int minZ=Math.floorDiv(source.getZ()-range,16),maxZ=Math.floorDiv(source.getZ()+range,16);
            for(int cx=minX;cx<=maxX;cx++)for(int cz=minZ;cz<=maxZ;cz++){
                var chunk=l.getChunkSource().getChunkNow(cx,cz);if(chunk==null)continue;
                for(var candidate:chunk.getBlockEntities().values()){
                    var pos=candidate.getBlockPos();if(source.distSqr(pos)<=range*range&&connectionRange(l,source,pos)==GENERATOR_RANGE)list.add(pos.immutable());
                }
            }
        }
        if(!(be instanceof GeneratorEntity)){
            range=OUTLET_RANGE;
            for(int x=-range;x<=range;x++)for(int y=-range;y<=range;y++)for(int z=-range;z<=range;z++){
                if(x*x+y*y+z*z>range*range)continue;var pos=source.offset(x,y,z);
                if(connectionRange(l,source,pos)==OUTLET_RANGE)list.add(pos.immutable());
            }
        }
        list.sort(Comparator.comparingDouble(source::distSqr));if(list.size()>64)list.subList(64,list.size()).clear();
        PacketDistributor.sendToPlayer(p,new PowerPayloads.Highlight(l.dimension().location().toString(),source,List.copyOf(list),300));
    }
}
