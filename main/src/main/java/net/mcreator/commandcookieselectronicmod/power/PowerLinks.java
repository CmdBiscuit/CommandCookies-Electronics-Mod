package net.mcreator.commandcookieselectronicmod.power;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
/** Independent power and coax links, tied to physical endpoint identities. */
public final class PowerLinks extends SavedData {
    public record Link(String block,UUID device,BlockPos outlet,UUID outletIdentity,int socket){public Link(String block,UUID device,BlockPos outlet,UUID outletIdentity){this(block,device,outlet,outletIdentity,-1);}}
    public final Map<BlockPos,Link> links=new HashMap<>(),coaxLinks=new HashMap<>();
    public Map<BlockPos,Link> channel(boolean coax){return coax?coaxLinks:links;}
    public static PowerLinks get(ServerLevel l){return l.getDataStorage().computeIfAbsent(new Factory<>(PowerLinks::new,PowerLinks::load,null),"cem_power_links");}
    private static void read(CompoundTag t,String key,Map<BlockPos,Link> map){for(var item:t.getList(key,Tag.TAG_COMPOUND)){var n=(CompoundTag)item;if(n.hasUUID("OutletIdentity"))map.put(BlockPos.of(n.getLong("Position")),new Link(n.getString("Block"),n.hasUUID("Device")?n.getUUID("Device"):null,BlockPos.of(n.getLong("Outlet")),n.getUUID("OutletIdentity"),n.contains("Socket")?n.getInt("Socket"):-1));}}
    public static PowerLinks load(CompoundTag t,HolderLookup.Provider r){var d=new PowerLinks();read(t,"Links",d.links);read(t,"CoaxLinks",d.coaxLinks);return d;}
    private static ListTag write(Map<BlockPos,Link> map){var a=new ListTag();map.forEach((p,v)->{var n=new CompoundTag();n.putLong("Position",p.asLong());n.putString("Block",v.block);if(v.device!=null)n.putUUID("Device",v.device);n.putLong("Outlet",v.outlet.asLong());n.putUUID("OutletIdentity",v.outletIdentity);n.putInt("Socket",v.socket);a.add(n);});return a;}
    @Override public CompoundTag save(CompoundTag t,HolderLookup.Provider r){t.put("Links",write(links));t.put("CoaxLinks",write(coaxLinks));return t;}
}
