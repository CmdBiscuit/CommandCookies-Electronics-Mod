package net.mcreator.commandcookieselectronicmod.pc;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.mcreator.commandcookieselectronicmod.power.*;
import net.neoforged.neoforge.network.PacketDistributor;

/** Reciprocal physical keyboard/mouse connections; never scans during desktop ticks. */
public final class PcInputs {
    public static final String SELECTION="CemPcInputLink";
    public static boolean tower(PcDeviceEntity d){return d.getBlockState().getBlock() instanceof PcDeviceBlock b&&!b.monitor;}
    public static boolean peripheral(PcDeviceEntity d){return d.getBlockState().getBlock() instanceof PcPeripheralBlock;}
    private static boolean mouse(PcDeviceEntity d){return ((PcPeripheralBlock)d.getBlockState().getBlock()).mouse;}
    private static BlockPos slot(PcDeviceEntity pc,boolean mouse){return mouse?pc.mouse:pc.keyboard;}
    private static UUID slotId(PcDeviceEntity pc,boolean mouse){return mouse?pc.mouseIdentity:pc.keyboardIdentity;}
    private static void slot(PcDeviceEntity pc,boolean mouse,BlockPos pos,UUID id){if(mouse){pc.mouse=pos;pc.mouseIdentity=id;}else{pc.keyboard=pos;pc.keyboardIdentity=id;}}

    public static PcDeviceEntity connected(PcDeviceEntity pc,boolean mouse){
        var l=pc.getLevel();var pos=slot(pc,mouse);var id=slotId(pc,mouse);
        if(l==null||!tower(pc)||pos==null||id==null||pc.getBlockPos().distSqr(pos)>PcSystem.RANGE*PcSystem.RANGE||!l.hasChunkAt(pos))return null;
        if(!(l.getBlockEntity(pos) instanceof PcDeviceEntity input)||!peripheral(input)||mouse(input)!=mouse||!input.identity.equals(id)
            ||!pc.identity.equals(input.hostIdentity)||!pc.getBlockPos().equals(input.host))return null;
        return input;
    }
    public static boolean ready(PcDeviceEntity pc){return connected(pc,false)!=null&&connected(pc,true)!=null;}
    private static PcDeviceEntity host(PcDeviceEntity input){
        var l=input.getLevel();if(l==null||input.host==null||input.hostIdentity==null||!l.hasChunkAt(input.host))return null;
        if(l.getBlockEntity(input.host) instanceof PcDeviceEntity pc&&tower(pc)&&pc.identity.equals(input.hostIdentity)&&connected(pc,mouse(input))==input)return pc;
        return null;
    }
    public static void reconcile(PcDeviceEntity pc){
        if(!tower(pc)||pc.getLevel()==null||pc.getLevel().isClientSide)return;
        for(boolean mouse:new boolean[]{false,true}){
            var pos=slot(pc,mouse);if(pos!=null&&pc.getLevel().hasChunkAt(pos)&&connected(pc,mouse)==null){slot(pc,mouse,null,null);pc.changed();}
        }
    }
    public static void detachPeripheral(PcDeviceEntity input){
        var pc=host(input);if(pc!=null){slot(pc,mouse(input),null,null);pc.changed();}
        input.host=null;input.hostIdentity=null;input.changed();
    }
    public static void detachTower(PcDeviceEntity pc){
        for(boolean mouse:new boolean[]{false,true}){var input=connected(pc,mouse);if(input!=null){input.host=null;input.hostIdentity=null;input.changed();}slot(pc,mouse,null,null);}
        pc.changed();
    }
    private static void notice(ServerPlayer p,String s){p.displayClientMessage(Component.literal(s),true);}
    public static CompoundTag selection(ItemStack item){return item.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound(SELECTION);}
    private static void selection(ItemStack item,CompoundTag value){var tag=item.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();if(value==null)tag.remove(SELECTION);else{tag.put(SELECTION,value);tag.remove(PowerNetwork.SELECTION);}item.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
    private static boolean valid(CompoundTag t,ServerPlayer p){long now=p.level().getGameTime();return t.hasUUID("Identity")&&t.hasUUID("Owner")&&p.getUUID().equals(t.getUUID("Owner"))&&t.getString("Dimension").equals(p.level().dimension().location().toString())&&t.getLong("Until")>=now&&t.getLong("Until")<=now+2400;}

    /** Returns false to leave existing generator/outlet/monitor wrench actions intact. */
    public static boolean wrench(ServerPlayer p,BlockPos target,ItemStack item){
        var l=p.serverLevel();var entity=l.getBlockEntity(target);
        if(!(entity instanceof PcDeviceEntity d)||(!tower(d)&&!peripheral(d))){selection(item,null);return false;}
        if(p.isShiftKeyDown()){
            selection(item,null);
            if(!peripheral(d))return false;
            detachPeripheral(d);notice(p,"Disconnected from the PC.");PowerNetwork.clearHighlight(p,target);return true;
        }
        if(valid(PowerNetwork.selection(item),p)){
            if(peripheral(d)){notice(p,"Keyboard and mouse connect directly to the PC. Finish or clear the outlet selection first.");return true;}
            return false;
        }
        if(tower(d))reconcile(d);
        else if(d.host!=null&&l.hasChunkAt(d.host)&&host(d)==null)detachPeripheral(d);
        var t=selection(item);
        if(valid(t,p)){
            var source=BlockPos.of(t.getLong("Position"));
            if(source.equals(target)){selection(item,null);PowerNetwork.clearHighlight(p,target);notice(p,"Wrench selection cleared.");return true;}
            if(!l.hasChunkAt(source)||!(l.getBlockEntity(source) instanceof PcDeviceEntity first)||!first.identity.equals(t.getUUID("Identity"))||(!tower(first)&&!peripheral(first))){selection(item,null);PowerNetwork.clearHighlight(p,target);notice(p,"The selected block was removed. Select it again.");return true;}
            if(tower(first)==tower(d)){notice(p,"Connect one PC to a keyboard or mouse.");return true;}
            if(source.distSqr(target)>PcSystem.RANGE*PcSystem.RANGE){notice(p,"Keyboard and mouse connections reach up to 16 blocks.");return true;}
            var pc=tower(first)?first:d;var input=peripheral(first)?first:d;boolean mouse=mouse(input);reconcile(pc);
            if(input.host!=null&&l.hasChunkAt(input.host)&&host(input)==null)detachPeripheral(input);
            if(connected(pc,mouse)==input){selection(item,null);PowerNetwork.clearHighlight(p,target);notice(p,"Already connected to this PC.");return true;}
            if(slot(pc,mouse)!=null||input.host!=null){notice(p,"Already connected. Crouch-click the keyboard or mouse with the wrench to disconnect it first.");return true;}
            slot(pc,mouse,input.getBlockPos().immutable(),input.identity);input.host=pc.getBlockPos().immutable();input.hostIdentity=pc.identity;
            pc.changed();input.changed();selection(item,null);
            PowerNetwork.clearHighlight(p,target);
            notice(p,(mouse?"Mouse":"Keyboard")+" connected."+(ready(pc)?" Both are ready. Open the PC monitor.":" Connect the "+(mouse?"keyboard":"mouse")+" too."));return true;
        }
        t=new CompoundTag();t.putUUID("Identity",d.identity);t.putUUID("Owner",p.getUUID());t.putLong("Position",target.asLong());t.putString("Dimension",l.dimension().location().toString());t.putLong("Until",l.getGameTime()+2400);selection(item,t);
        notice(p,tower(d)?"PC selected. Wrench-click a keyboard, mouse, or wall outlet.":(mouse(d)?"Mouse":"Keyboard")+" selected. Wrench-click its PC.");highlight(p,d);return true;
    }
    private static void highlight(ServerPlayer p,PcDeviceEntity source){
        var l=p.serverLevel();var targets=new ArrayList<BlockPos>();int range=PcSystem.RANGE;
        for(int x=-range;x<=range;x++)for(int y=-range;y<=range;y++)for(int z=-range;z<=range;z++){
            if(x*x+y*y+z*z>range*range)continue;var pos=source.getBlockPos().offset(x,y,z);
            if(!l.hasChunkAt(pos))continue;
            if(tower(source)&&source.getBlockPos().distSqr(pos)<=PowerNetwork.OUTLET_RANGE*PowerNetwork.OUTLET_RANGE&&l.getBlockEntity(pos) instanceof OutletEntity o&&PowerNetwork.compatible(o,l,source.getBlockPos())){targets.add(pos.immutable());continue;}
            if(!(l.getBlockEntity(pos) instanceof PcDeviceEntity d))continue;
            boolean ok=tower(source)?peripheral(d)&&d.host==null&&slot(source,mouse(d))==null:tower(d)&&slot(d,mouse(source))==null;
            if(ok)targets.add(pos.immutable());
        }
        targets.sort(Comparator.comparingDouble(source.getBlockPos()::distSqr));if(targets.size()>64)targets.subList(64,targets.size()).clear();
        PacketDistributor.sendToPlayer(p,new PowerPayloads.Highlight(l.dimension().location().toString(),source.getBlockPos(),List.copyOf(targets),300));
    }
}
