package net.mcreator.commandcookieselectronicmod.pc;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems;
import net.mcreator.commandcookieselectronicmod.power.*;
import net.neoforged.neoforge.network.PacketDistributor;
public final class PcSystem {
    public static final String SELECTION="CemHdmiLink";
    public static final int RANGE=16;
    public static CompoundTag selection(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound(SELECTION);}
    private static void selection(ItemStack stack,CompoundTag value){var t=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();if(value==null)t.remove(SELECTION);else t.put(SELECTION,value);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(t));}
    private static void notice(Player p,String text){p.displayClientMessage(Component.literal(text),true);}
    public static PcDeviceEntity peer(PcDeviceEntity d){var l=d.getLevel();if(l==null||d.peer==null||d.peerIdentity==null||!l.hasChunkAt(d.peer)||d.getBlockPos().distSqr(d.peer)>RANGE*RANGE)return null;if(!(l.getBlockEntity(d.peer) instanceof PcDeviceEntity other)||!(other.getBlockState().getBlock() instanceof PcDeviceBlock)||other.monitor()==d.monitor()||!other.identity.equals(d.peerIdentity)||!d.identity.equals(other.peerIdentity)||!d.getBlockPos().equals(other.peer))return null;return other;}
    public static boolean active(PcDeviceEntity d){var other=peer(d);return other!=null&&PowerNetwork.powered(d.getLevel(),d.getBlockPos())&&PowerNetwork.powered(other.getLevel(),other.getBlockPos());}
    private static void display(PcDeviceEntity d,boolean on){var l=d.getLevel();var state=d.getBlockState();if(l==null||!l.getBlockState(d.getBlockPos()).is(state.getBlock()))return;if(state.getValue(PcDeviceBlock.POWERED)!=on){if(!on&&!d.monitor()){d.desktopSession=UUID.randomUUID();d.changed();}l.setBlock(d.getBlockPos(),state.setValue(PcDeviceBlock.POWERED,on),3);}}
    public static void refresh(PcDeviceEntity d){var l=d.getLevel();if(l==null||l.isClientSide)return;PowerCables.ensureHdmi(d);var other=peer(d);if(d.peer!=null&&l.hasChunkAt(d.peer)&&other==null){detach(d,true);return;}boolean on=active(d);display(d,on);if(other!=null)display(other,on);}
    public static void interact(Level l,BlockPos pos,Player p,InteractionHand hand){if(l.isClientSide||!(p instanceof ServerPlayer player)||!p.isAlive()||p.isSpectator()||p.distanceToSqr(Vec3Center(pos))>64||!(l.getBlockEntity(pos) instanceof PcDeviceEntity d))return;long now=l.getGameTime();if(d.interaction==now)return;d.interaction=now;var stack=p.getItemInHand(hand);if(!stack.is(CommandcookiesElectronicModModItems.HDMI_CABLE.get())&&p.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND).is(CommandcookiesElectronicModModItems.HDMI_CABLE.get()))stack=p.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);if(stack.is(CommandcookiesElectronicModModItems.HDMI_CABLE.get())){link(d,player,stack);return;}refresh(d);if(!PowerNetwork.powered(l,pos)){notice(p,"No power. Use the wrench to connect this device to a powered wall outlet.");return;}var other=peer(d);if(other==null){notice(p,"Use an HDMI cable to connect one gaming PC and one gaming monitor.");return;}if(!PowerNetwork.powered(l,other.getBlockPos())){notice(p,"Both the PC and monitor need their own wrench connection to a powered wall outlet.");return;}if(!d.monitor()){notice(p,"The PC is ready. Right-click its monitor to open BlockOS.");return;}l.sendBlockUpdated(pos,d.getBlockState(),d.getBlockState(),3);l.sendBlockUpdated(other.getBlockPos(),other.getBlockState(),other.getBlockState(),3);PacketDistributor.sendToPlayer(player,new PcOpenResponse(pos,other.identity,other.desktopSession));}
    private static net.minecraft.world.phys.Vec3 Vec3Center(BlockPos pos){return net.minecraft.world.phys.Vec3.atCenterOf(pos);}
    private static void link(PcDeviceEntity d,ServerPlayer p,ItemStack stack){
        var l=d.getLevel();long now=l.getGameTime();
        if(p.isShiftKeyDown()){selection(stack,null);if(d.peer!=null){detach(d,true);notice(p,"HDMI connection removed.");}else notice(p,"HDMI selection cleared.");PowerNetwork.clearHighlight(p,d.getBlockPos());return;}
        var t=selection(stack);boolean saved=t.hasUUID("Identity")&&t.hasUUID("Owner")&&p.getUUID().equals(t.getUUID("Owner"))&&t.getString("Dimension").equals(l.dimension().location().toString())&&t.getLong("Until")>=now&&t.getLong("Until")<=now+2400;
        if(!saved){t=new CompoundTag();t.putUUID("Identity",d.identity);t.putUUID("Owner",p.getUUID());t.putLong("Position",d.getBlockPos().asLong());t.putString("Dimension",l.dimension().location().toString());t.putLong("Until",now+2400);selection(stack,t);notice(p,(d.monitor()?"Monitor":"Gaming PC")+" selected. Click the other device with this HDMI cable.");highlight(p,d);return;}
        var firstPos=BlockPos.of(t.getLong("Position"));if(firstPos.equals(d.getBlockPos())){selection(stack,null);PowerNetwork.clearHighlight(p,d.getBlockPos());notice(p,"HDMI selection cleared.");return;}
        if(!l.hasChunkAt(firstPos)||!(l.getBlockEntity(firstPos) instanceof PcDeviceEntity first)||!first.identity.equals(t.getUUID("Identity"))){selection(stack,null);PowerNetwork.clearHighlight(p,d.getBlockPos());notice(p,"The selected device was removed. Select it again.");return;}
        if(first.monitor()==d.monitor()){notice(p,"Connect one gaming PC to one gaming monitor.");return;}if(firstPos.distSqr(d.getBlockPos())>RANGE*RANGE){notice(p,"HDMI cables reach up to 16 blocks.");return;}
        if(peer(first)==d){selection(stack,null);PowerNetwork.clearHighlight(p,d.getBlockPos());notice(p,"These devices are already connected.");return;}
        if(first.peer!=null||d.peer!=null){notice(p,"A device is already linked. Crouch-click it with HDMI to disconnect.");return;}
        first.peer=d.getBlockPos().immutable();first.peerIdentity=d.identity;d.peer=firstPos.immutable();d.peerIdentity=first.identity;var pc=d.monitor()?first:d;pc.paidCable=!p.getAbilities().instabuild;
        selection(stack,null);if(pc.paidCable)stack.shrink(1);first.changed();d.changed();refresh(first);PowerNetwork.clearHighlight(p,d.getBlockPos());notice(p,active(first)?"HDMI connected. PC and monitor are on.":"HDMI connected. Power both devices from a wall outlet with the wrench.");
    }
    public static void detach(PcDeviceEntity d,boolean refund){PowerCables.signalRemoved(d.getLevel(),d.getBlockPos(),PowerCables.HDMI);var other=peer(d);var pc=d.monitor()?other:d;if(pc!=null){if(refund&&pc.paidCable)Block.popResource(d.getLevel(),d.getBlockPos(),new ItemStack(CommandcookiesElectronicModModItems.HDMI_CABLE.get()));pc.paidCable=false;}d.peer=null;d.peerIdentity=null;display(d,false);d.changed();if(other!=null){other.peer=null;other.peerIdentity=null;other.paidCable=false;display(other,false);other.changed();}}
    /** Selection scans once, capped at 64 targets. Rendering uses cached coarse outlines. */
    private static void highlight(ServerPlayer p,PcDeviceEntity source){var l=p.serverLevel();var targets=new ArrayList<BlockPos>();for(int x=-RANGE;x<=RANGE;x++)for(int y=-RANGE;y<=RANGE;y++)for(int z=-RANGE;z<=RANGE;z++){if(x*x+y*y+z*z>RANGE*RANGE)continue;var pos=source.getBlockPos().offset(x,y,z);if(l.hasChunkAt(pos)&&l.getBlockEntity(pos) instanceof PcDeviceEntity d&&d.monitor()!=source.monitor()&&d.peer==null)targets.add(pos.immutable());}targets.sort(Comparator.comparingDouble(source.getBlockPos()::distSqr));if(targets.size()>64)targets.subList(64,targets.size()).clear();PacketDistributor.sendToPlayer(p,new PowerPayloads.Highlight(l.dimension().location().toString(),source.getBlockPos(),List.copyOf(targets),300));}
}
