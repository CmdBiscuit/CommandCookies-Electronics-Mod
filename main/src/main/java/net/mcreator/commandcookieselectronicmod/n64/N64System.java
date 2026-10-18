package net.mcreator.commandcookieselectronicmod.n64;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.mcreator.commandcookieselectronicmod.block.N64ConsoleBlock;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems;
import net.mcreator.commandcookieselectronicmod.nes.*;
import net.mcreator.commandcookieselectronicmod.power.*;
import net.neoforged.neoforge.network.PacketDistributor;

/** Physical N64 links only. The controller remains a handheld item. */
public final class N64System {
    private static boolean allowed(Player p,BlockPos pos){return p instanceof ServerPlayer&&p.isAlive()&&!p.isSpectator()&&p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64;}
    private static void notice(Player p,String text){p.displayClientMessage(Component.literal(text),true);}
    private static void select(ItemStack item,CompoundTag value){
        var tag=item.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(value==null)tag.remove(NesSystem.SELECTION);else tag.put(NesSystem.SELECTION,value);
        item.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
    private static boolean selected(CompoundTag tag,ServerPlayer player){
        long now=player.level().getGameTime();return tag.hasUUID("Identity")&&tag.hasUUID("Owner")&&player.getUUID().equals(tag.getUUID("Owner"))
            &&tag.getString("Dimension").equals(player.level().dimension().location().toString())&&tag.getLong("Until")>=now&&tag.getLong("Until")<=now+2400;
    }
    public static NesDeviceBlockEntity peer(N64DeviceBlockEntity d){
        var l=d.getLevel();if(l==null||d.peer==null||d.peerIdentity==null||!l.hasChunkAt(d.peer)||d.getBlockPos().distSqr(d.peer)>NesSystem.MAX_CABLE_DISTANCE*NesSystem.MAX_CABLE_DISTANCE)return null;
        if(!(l.getBlockEntity(d.peer) instanceof NesDeviceBlockEntity m)||!m.monitor()||!m.n64Peer)return null;
        return m.identity.equals(d.peerIdentity)&&d.identity.equals(m.peerIdentity)&&d.getBlockPos().equals(m.peer)?m:null;
    }
    public static N64DeviceBlockEntity consoleFor(NesDeviceBlockEntity m){
        var l=m.getLevel();if(!m.monitor()||!m.n64Peer||l==null||m.peer==null||!l.hasChunkAt(m.peer))return null;
        return l.getBlockEntity(m.peer) instanceof N64DeviceBlockEntity c&&peer(c)==m?c:null;
    }
    public static void interactConsole(Level l,BlockPos pos,Player p,InteractionHand hand){
        if(l.isClientSide||!allowed(p,pos)||!(l.getBlockEntity(pos) instanceof N64DeviceBlockEntity d))return;
        long now=l.getGameTime();if(d.interaction==now)return;d.interaction=now;
        var held=p.getItemInHand(hand);var other=p.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
        if(!NesSystem.cable(held)&&NesSystem.cable(other))held=other;
        if(NesSystem.cable(held)){link((ServerPlayer)p,pos,held);return;}
        notice(p,"N64 console: decorative for now. Use an AV cable for the retro monitor and a wrench for wall power.");
    }
    /** Called before the NES handler, so either endpoint can be selected first. */
    public static boolean interactMonitor(Level l,BlockPos pos,Player p,InteractionHand hand){
        if(l.isClientSide||!allowed(p,pos)||!(l.getBlockEntity(pos) instanceof NesDeviceBlockEntity m)||!m.monitor())return false;
        var held=p.getItemInHand(hand);var other=p.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
        if(!NesSystem.cable(held)&&NesSystem.cable(other))held=other;
        var tag=NesSystem.selection(held);boolean incoming=false;
        if(NesSystem.cable(held)&&selected(tag,(ServerPlayer)p)){
            var first=BlockPos.of(tag.getLong("Position"));
            incoming=l.hasChunkAt(first)&&l.getBlockEntity(first) instanceof N64DeviceBlockEntity;
        }
        if(!m.n64Peer&&!incoming)return false;
        long now=l.getGameTime();if(m.interaction==now)return true;m.interaction=now;
        if(NesSystem.cable(held))link((ServerPlayer)p,pos,held);
        else notice(p,"A N64 is connected. N64 gameplay will be added to the Retro Expansion later.");
        return true;
    }
    private static void link(ServerPlayer p,BlockPos target,ItemStack stack){
        var l=p.serverLevel();var clicked=l.getBlockEntity(target);
        if(!(clicked instanceof N64DeviceBlockEntity)&&!(clicked instanceof NesDeviceBlockEntity m&&m.monitor()))return;
        if(p.isShiftKeyDown()){
            select(stack,null);PowerNetwork.clearHighlight(p,target);
            if(clicked instanceof N64DeviceBlockEntity c)detach(c,true);
            else if(clicked instanceof NesDeviceBlockEntity m&&m.n64Peer){var c=consoleFor(m);if(c!=null)detach(c,true);else clearMonitor(m);}
            notice(p,"N64 AV connection removed; selection cleared.");return;
        }
        var tag=NesSystem.selection(stack);
        if(!selected(tag,p)){
            var id=PowerNetwork.identity(l,target);if(id==null)return;
            tag=new CompoundTag();tag.putUUID("Identity",id);tag.putUUID("Owner",p.getUUID());tag.putLong("Position",target.asLong());
            tag.putString("Dimension",l.dimension().location().toString());tag.putLong("Until",l.getGameTime()+2400);select(stack,tag);
            highlight(p,target,clicked instanceof N64DeviceBlockEntity);notice(p,(clicked instanceof N64DeviceBlockEntity?"N64":"Monitor")+" selected. Right-click the other device with this AV cable.");return;
        }
        var first=BlockPos.of(tag.getLong("Position"));
        if(first.equals(target)){select(stack,null);PowerNetwork.clearHighlight(p,target);notice(p,"AV selection cleared.");return;}
        if(!l.hasChunkAt(first)||!Objects.equals(PowerNetwork.identity(l,first),tag.getUUID("Identity"))){select(stack,null);PowerNetwork.clearHighlight(p,target);notice(p,"The selected device was removed. Select it again.");return;}
        var source=l.getBlockEntity(first);
        var console=clicked instanceof N64DeviceBlockEntity c?c:source instanceof N64DeviceBlockEntity c?c:null;
        var monitor=clicked instanceof NesDeviceBlockEntity m&&m.monitor()?m:source instanceof NesDeviceBlockEntity m&&m.monitor()?m:null;
        if(console==null||monitor==null){notice(p,"Connect one N64 to one retro monitor.");return;}
        if(first.distSqr(target)>NesSystem.MAX_CABLE_DISTANCE*NesSystem.MAX_CABLE_DISTANCE){notice(p,"The AV cable reaches up to 16 blocks.");return;}
        if(peer(console)==monitor){select(stack,null);PowerNetwork.clearHighlight(p,target);notice(p,"These devices are already connected.");return;}
        if(console.peer!=null||monitor.peer!=null){notice(p,"A device is already linked. Crouch-click it with the AV cable to disconnect.");return;}
        console.peer=monitor.getBlockPos().immutable();console.peerIdentity=monitor.identity;console.paidCable=!p.getAbilities().instabuild;
        monitor.peer=console.getBlockPos().immutable();monitor.peerIdentity=console.identity;monitor.n64Peer=true;monitor.snesPeer=false;monitor.paidCable=false;
        select(stack,null);if(console.paidCable)stack.shrink(1);console.changed();monitor.changed();refresh(console);refreshMonitor(monitor);
        PowerNetwork.clearHighlight(p,target);notice(p,"N64 AV connected. This setup is decorative for now.");
    }
    private static void highlight(ServerPlayer p,BlockPos source,boolean console){
        var l=p.serverLevel();var targets=new ArrayList<BlockPos>();int range=NesSystem.MAX_CABLE_DISTANCE;
        for(int x=-range;x<=range;x++)for(int y=-range;y<=range;y++)for(int z=-range;z<=range;z++){
            if(x*x+y*y+z*z>range*range)continue;var pos=source.offset(x,y,z);if(!l.hasChunkAt(pos))continue;
            var be=l.getBlockEntity(pos);
            if(console&&be instanceof NesDeviceBlockEntity m&&m.monitor()&&m.peer==null||!console&&be instanceof N64DeviceBlockEntity c&&c.peer==null)targets.add(pos.immutable());
        }
        targets.sort(Comparator.comparingDouble(source::distSqr));if(targets.size()>64)targets.subList(64,targets.size()).clear();
        PacketDistributor.sendToPlayer(p,new PowerPayloads.Highlight(l.dimension().location().toString(),source,List.copyOf(targets),300));
    }
    public static void detach(N64DeviceBlockEntity c,boolean refund){
        var l=c.getLevel();if(l==null||l.isClientSide)return;var m=peer(c);boolean paid=c.paidCable;
        c.peer=null;c.peerIdentity=null;c.paidCable=false;c.changed();PowerCables.signalRemoved(l,c.getBlockPos(),PowerCables.AV);
        if(m!=null)clearMonitor(m);
        if(refund&&paid)Block.popResource(l,c.getBlockPos(),new ItemStack(CommandcookiesElectronicModModItems.AV_CABLE.get()));
    }
    private static void clearMonitor(NesDeviceBlockEntity m){m.peer=null;m.peerIdentity=null;m.n64Peer=false;m.paidCable=false;m.changed();refreshMonitor(m);}
    public static void monitorRemoved(NesDeviceBlockEntity m){if(m.n64Peer){var c=consoleFor(m);if(c!=null)detach(c,true);else clearMonitor(m);}}
    public static void tick(N64DeviceBlockEntity c){
        var l=c.getLevel();if(l==null||l.isClientSide)return;
        if(c.peer!=null&&l.hasChunkAt(c.peer)&&peer(c)==null)detach(c,true);refresh(c);
    }
    public static void tickMonitor(NesDeviceBlockEntity m){
        var l=m.getLevel();if(l==null||l.isClientSide)return;
        if(m.peer==null||l.hasChunkAt(m.peer)&&consoleFor(m)==null)clearMonitor(m);
        else {var c=consoleFor(m);if(c!=null)PowerCables.ensureN64Av(c);refreshMonitor(m);}
    }
    private static void refresh(N64DeviceBlockEntity c){
        var l=c.getLevel();if(l==null||l.isClientSide)return;var state=l.getBlockState(c.getBlockPos());
        if(state.getBlock() instanceof N64ConsoleBlock){boolean on=PowerNetwork.powered(l,c.getBlockPos());if(state.getValue(N64ConsoleBlock.POWERED)!=on)l.setBlock(c.getBlockPos(),state.setValue(N64ConsoleBlock.POWERED,on),3);}
        PowerCables.ensureN64Av(c);
    }
    private static void refreshMonitor(NesDeviceBlockEntity m){
        var l=m.getLevel();if(l==null||l.isClientSide)return;var state=l.getBlockState(m.getBlockPos());
        if(state.getBlock() instanceof NesDeviceBlock b&&b.monitor){boolean on=PowerNetwork.powered(l,m.getBlockPos());
            if(state.getValue(NesDeviceBlock.POWERED)!=on||state.getValue(NesDeviceBlock.GAME_LOADED))l.setBlock(m.getBlockPos(),state.setValue(NesDeviceBlock.POWERED,on).setValue(NesDeviceBlock.GAME_LOADED,false),3);
        }
    }
    private N64System(){}
}
