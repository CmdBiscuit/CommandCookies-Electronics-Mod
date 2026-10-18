package net.mcreator.commandcookieselectronicmod.nes;

import java.util.UUID;
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
import net.neoforged.neoforge.network.PacketDistributor;
import net.mcreator.commandcookieselectronicmod.power.*;

/** Links and cartridge/session ownership are validated exclusively on the server. */
public final class NesSystem {
    public static final String SELECTION="CemAvLink";
    public static final int MAX_CABLE_DISTANCE=16;
    public static boolean controller(Player p){return p.getMainHandItem().is(CommandcookiesElectronicModModItems.NES_CONTROLLER.get())||p.getOffhandItem().is(CommandcookiesElectronicModModItems.NES_CONTROLLER.get());}
    public static boolean cable(ItemStack s){return s.is(CommandcookiesElectronicModModItems.AV_CABLE.get());}
    public static CompoundTag selection(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound(SELECTION);}
    private static void selection(ItemStack stack,CompoundTag value){var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();if(value==null)tag.remove(SELECTION);else tag.put(SELECTION,value);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
    private static void notice(Player p,String message){p.displayClientMessage(Component.literal(message),true);}
    public static NesDeviceBlockEntity peer(NesDeviceBlockEntity d){
        Level l=d.getLevel();if(d.n64Peer||d.snesPeer||l==null||d.peer==null||d.peerIdentity==null||d.getBlockPos().distSqr(d.peer)>MAX_CABLE_DISTANCE*MAX_CABLE_DISTANCE||!l.hasChunkAt(d.peer))return null;
        if(!(l.getBlockEntity(d.peer) instanceof NesDeviceBlockEntity other)||other.monitor()==d.monitor())return null;
        if(!other.identity.equals(d.peerIdentity)||!d.identity.equals(other.peerIdentity)||!d.getBlockPos().equals(other.peer))return null;
        return other;
    }
    private static void powered(NesDeviceBlockEntity d,boolean value){
        var s=d.getBlockState();if(d.getLevel()==null||!d.getLevel().getBlockState(d.getBlockPos()).is(s.getBlock()))return;
        var nes=d.monitor()?peer(d):d;boolean loaded=value&&nes!=null&&net.mcreator.commandcookieselectronicmod.power.PowerNetwork.powered(nes.getLevel(),nes.getBlockPos())&&!nes.cartridge.isEmpty();
        if(s.hasProperty(NesDeviceBlock.POWERED)&&(s.getValue(NesDeviceBlock.POWERED)!=value||s.getValue(NesDeviceBlock.GAME_LOADED)!=loaded))d.getLevel().setBlock(d.getBlockPos(),s.setValue(NesDeviceBlock.POWERED,value).setValue(NesDeviceBlock.GAME_LOADED,loaded),3);
    }
    private static boolean supply(NesDeviceBlockEntity d){return net.mcreator.commandcookieselectronicmod.power.PowerNetwork.powered(d.getLevel(),d.getBlockPos());}
    private static void refreshDisplay(NesDeviceBlockEntity d){PowerCables.ensureAv(d);var other=peer(d);powered(d,supply(d));if(other!=null)powered(other,supply(other));}
    public static void interact(Level level,BlockPos pos,Player p,InteractionHand hand){
        if(level.isClientSide||!(p instanceof ServerPlayer player)||p.isSpectator()||p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64)return;
        if(!(level.getBlockEntity(pos) instanceof NesDeviceBlockEntity d))return;
        if(net.mcreator.commandcookieselectronicmod.n64.N64System.interactMonitor(level,pos,p,hand))return;
        if(net.mcreator.commandcookieselectronicmod.snes.SnesSystem.interactMonitor(level,pos,p,hand))return;
        long now=level.getGameTime();if(d.interaction==now)return;d.interaction=now;
        ItemStack stack=p.getItemInHand(hand);
        if(!cable(stack)&&cable(p.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND))){hand=hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;stack=p.getItemInHand(hand);}
        if(cable(stack)){link(d,player,stack);return;}
        if(!d.monitor()){
            if(p.isShiftKeyDown()){
                if(d.cartridge.isEmpty()){notice(p,"No cartridge inserted.");return;}
                close(d,"Cartridge removed.");give(player,d.cartridge.copy());d.cartridge=ItemStack.EMPTY;d.game=new TetrisEngine(System.nanoTime());d.changed();refreshDisplay(d);notice(p,"Cartridge ejected.");return;
            }
            if(!stack.is(CommandcookiesElectronicModModItems.NES_TETRIS_CARTRIDGE.get())&&p.getOffhandItem().is(CommandcookiesElectronicModModItems.NES_TETRIS_CARTRIDGE.get()))stack=p.getOffhandItem();
            if(stack.is(CommandcookiesElectronicModModItems.NES_TETRIS_CARTRIDGE.get())){
                if(!d.cartridge.isEmpty()){notice(p,"A cartridge is already inserted. Crouch + right-click to eject it.");return;}
                d.cartridge=stack.copyWithCount(1);if(!p.getAbilities().instabuild)stack.shrink(1);d.game=new TetrisEngine(System.nanoTime());d.changed();refreshDisplay(d);notice(p,"Cartridge inserted. Hold the controller and right-click the linked monitor.");return;
            }
            notice(p,d.cartridge.isEmpty()?"Right-click with a cartridge to insert it.":"A cartridge is inserted. Use the linked monitor with the controller.");return;
        }
        var nes=peer(d);
        refreshDisplay(d);
        if(!supply(d)){notice(p,"This monitor needs both a powered wall outlet and coax outlet. Connect each with the wrench.");return;}
        if(nes==null){notice(p,"Connect this monitor to a NES, SNES or N64 using an AV cable.");return;}
        if(!supply(nes)){notice(p,"The NES needs a powered wall outlet. Connect it with the wrench.");return;}
        if(nes.cartridge.isEmpty()){notice(p,"Insert a cartridge into the linked NES first.");return;}
        if(!controller(p)){notice(p,"Hold the NES controller in either hand to play.");return;}
        if(nes.owner!=null&&!nes.owner.equals(p.getUUID())){notice(p,"This NES is being used by another player.");return;}
        nes.owner=p.getUUID();nes.session=UUID.randomUUID();nes.lastInput=0;nes.inputs.reset();nes.game.paused=true;nes.changed();send(nes,player,true,true,"");
    }
    private static void link(NesDeviceBlockEntity target,ServerPlayer player,ItemStack stack){
        Level level=target.getLevel();
        if(player.isShiftKeyDown()){
            selection(stack,null);PowerNetwork.clearHighlight(player,target.getBlockPos());if(target.peer==null){notice(player,"AV selection cleared.");return;}
            detach(target,true);notice(player,"AV connection removed.");return;
        }
        var tag=selection(stack);long now=level.getGameTime();
        boolean saved=tag.hasUUID("Identity")&&tag.hasUUID("Owner")&&player.getUUID().equals(tag.getUUID("Owner"))
            &&tag.getString("Dimension").equals(level.dimension().location().toString())&&tag.getLong("Until")>=now&&tag.getLong("Until")<=now+2400;
        if(!saved){
            tag=new CompoundTag();tag.putUUID("Identity",target.identity);tag.putUUID("Owner",player.getUUID());tag.putString("Dimension",level.dimension().location().toString());
            tag.putLong("Position",target.getBlockPos().asLong());tag.putLong("Until",now+2400);selection(stack,tag);
            notice(player,(target.monitor()?"Monitor":"NES")+" selected. Right-click the other device with this cable.");highlight(player,target);return;
        }
        BlockPos firstPos=BlockPos.of(tag.getLong("Position"));
        if(firstPos.equals(target.getBlockPos())){selection(stack,null);PowerNetwork.clearHighlight(player,target.getBlockPos());notice(player,"AV selection cleared.");return;}
        if(!level.hasChunkAt(firstPos)||!(level.getBlockEntity(firstPos) instanceof NesDeviceBlockEntity first)||!first.identity.equals(tag.getUUID("Identity"))){selection(stack,null);PowerNetwork.clearHighlight(player,target.getBlockPos());notice(player,"The selected device is no longer available. Select it again.");return;}
        if(first.monitor()==target.monitor()){notice(player,"Connect one NES, SNES or N64 to one retro monitor.");return;}
        if(firstPos.distSqr(target.getBlockPos())>MAX_CABLE_DISTANCE*MAX_CABLE_DISTANCE){notice(player,"The AV cable reaches up to 16 blocks.");return;}
        if(peer(first)==target){selection(stack,null);PowerNetwork.clearHighlight(player,target.getBlockPos());notice(player,"These devices are already connected.");return;}
        if(first.peer!=null||target.peer!=null){notice(player,"A device is already linked. Crouch-click it with the cable to disconnect.");return;}
        first.peer=target.getBlockPos().immutable();first.peerIdentity=target.identity;target.peer=firstPos.immutable();target.peerIdentity=first.identity;
        var nes=first.monitor()?target:first;nes.paidCable=!player.getAbilities().instabuild;
        selection(stack,null);if(nes.paidCable)stack.shrink(1);first.changed();target.changed();refreshDisplay(first);
        PowerNetwork.clearHighlight(player,target.getBlockPos());notice(player,"AV connected. The monitor needs wall power and coax; the NES needs wall power.");
    }
    public static void detach(NesDeviceBlockEntity d,boolean refund){
        if(d.n64Peer){net.mcreator.commandcookieselectronicmod.n64.N64System.monitorRemoved(d);return;}
        if(d.snesPeer){net.mcreator.commandcookieselectronicmod.snes.SnesSystem.monitorRemoved(d);return;}
        PowerCables.signalRemoved(d.getLevel(),d.getBlockPos(),PowerCables.AV);
        var other=peer(d);var nes=d.monitor()?other:d;
        if(nes!=null){close(nes,"AV connection removed.");if(refund&&nes.paidCable)Block.popResource(d.getLevel(),d.getBlockPos(),new ItemStack(CommandcookiesElectronicModModItems.AV_CABLE.get()));nes.paidCable=false;}
        d.peer=null;d.peerIdentity=null;d.paidCable=false;powered(d,false);d.changed();
        if(other!=null){other.peer=null;other.peerIdentity=null;other.paidCable=false;powered(other,false);other.changed();}
    }
    public static void destroy(NesDeviceBlockEntity d){
        net.mcreator.commandcookieselectronicmod.n64.N64System.monitorRemoved(d);
        net.mcreator.commandcookieselectronicmod.snes.SnesSystem.monitorRemoved(d);
        detach(d,true);if(!d.cartridge.isEmpty()){Block.popResource(d.getLevel(),d.getBlockPos(),d.cartridge.copy());d.cartridge=ItemStack.EMPTY;}
    }
    private static void highlight(ServerPlayer p,NesDeviceBlockEntity source){var l=p.serverLevel();var targets=new java.util.ArrayList<BlockPos>();int range=MAX_CABLE_DISTANCE;for(int x=-range;x<=range;x++)for(int y=-range;y<=range;y++)for(int z=-range;z<=range;z++){if(x*x+y*y+z*z>range*range)continue;var pos=source.getBlockPos().offset(x,y,z);if(!l.hasChunkAt(pos))continue;var be=l.getBlockEntity(pos);if(be instanceof NesDeviceBlockEntity d&&d.monitor()!=source.monitor()&&d.peer==null||source.monitor()&&be instanceof net.mcreator.commandcookieselectronicmod.snes.SnesDeviceBlockEntity snes&&snes.peer==null||source.monitor()&&be instanceof net.mcreator.commandcookieselectronicmod.n64.N64DeviceBlockEntity n64&&n64.peer==null)targets.add(pos.immutable());}targets.sort(java.util.Comparator.comparingDouble(source.getBlockPos()::distSqr));if(targets.size()>64)targets.subList(64,targets.size()).clear();PacketDistributor.sendToPlayer(p,new PowerPayloads.Highlight(l.dimension().location().toString(),source.getBlockPos(),java.util.List.copyOf(targets),300));}
    private static void give(ServerPlayer p,ItemStack stack){if(!p.getInventory().add(stack))p.drop(stack,false);}
    private static boolean allowed(NesDeviceBlockEntity nes,ServerPlayer player){
        var monitor=peer(nes);return monitor!=null&&monitor.monitor()&&supply(nes)&&supply(monitor)&&!nes.cartridge.isEmpty()&&controller(player)&&player.isAlive()&&!player.isSpectator()
            &&player.level()==nes.getLevel()&&player.distanceToSqr(monitor.getBlockPos().getX()+.5,monitor.getBlockPos().getY()+.5,monitor.getBlockPos().getZ()+.5)<=64;
    }
    public static void action(ServerPlayer player,BlockPos monitorPos,UUID token,int action,int value){
        var l=player.serverLevel();if(l.hasChunkAt(monitorPos)&&l.getBlockEntity(monitorPos) instanceof NesDeviceBlockEntity monitor){var nes=peer(monitor);if(nes!=null)action(player,monitorPos,token,action,value,nes.inputs.received()+1,nes.game.generation());}
    }
    public static void action(ServerPlayer player,BlockPos monitorPos,UUID token,int action,int value,int sequence){
        var l=player.serverLevel();if(l.hasChunkAt(monitorPos)&&l.getBlockEntity(monitorPos) instanceof NesDeviceBlockEntity monitor){var nes=peer(monitor);if(nes!=null)action(player,monitorPos,token,action,value,sequence,nes.game.generation());}
    }
    public static void action(ServerPlayer player,BlockPos monitorPos,UUID token,int action,int value,int sequence,int generation){
        var level=player.serverLevel();if(!level.hasChunkAt(monitorPos)||!(level.getBlockEntity(monitorPos) instanceof NesDeviceBlockEntity monitor)||!monitor.monitor())return;
        var nes=peer(monitor);if(nes==null||!player.getUUID().equals(nes.owner)||!token.equals(nes.session))return;
        if(!allowed(nes,player)){close(nes,"Keep the controller in hand and stay near the connected monitor.");return;}
        if(!nes.inputs.offer(sequence,action,value,generation))return;
        if(action==7){close(nes,"");return;}
    }
    private static void apply(NesDeviceBlockEntity nes,NesInputQueue.Input input){
        int action=input.action(),value=input.value();
        var game=nes.game;
        switch(action){case 0->game.move(-1);case 1->game.move(1);case 2->game.rotate(1);case 3->game.rotate(-1);case 4->game.softDrop();
            case 5->{if(game.started&&!game.over)game.paused=!game.paused;}
            case 6->{game.start(game.startLevel);}
            case 8->{int settings=value&255;int music=value>>>8;if(!game.started&&value>=0&&music<=3&&(settings<60||settings>=100&&settings<=159)&&(settings/10)%10<=5){game.music=music;game.start(settings%10,settings>=100,(settings/10)%10);}}
            case 9->{if(game.over||game.paused){var fresh=new TetrisEngine(System.nanoTime());fresh.startLevel=game.startLevel;fresh.height=game.height;fresh.bType=game.bType;fresh.music=game.music;nes.game=fresh;}}
        }
        nes.best=Math.max(nes.best,nes.game.score);nes.setChanged();
    }
    public static void close(NesDeviceBlockEntity nes,String why){
        if(nes.owner!=null&&nes.getLevel()!=null&&nes.getLevel().getServer()!=null){var p=nes.getLevel().getServer().getPlayerList().getPlayer(nes.owner);if(p!=null)send(nes,p,false,false,why);}
        nes.owner=null;nes.session=UUID.randomUUID();nes.inputs.reset();nes.game.paused=true;nes.setChanged();
    }
    public static void tick(NesDeviceBlockEntity d){
        Level level=d.getLevel();if(level==null||level.isClientSide)return;long now=level.getGameTime();
        if(d.monitor()&&d.n64Peer){if(now%20==0)net.mcreator.commandcookieselectronicmod.n64.N64System.tickMonitor(d);return;}
        if(d.monitor()&&d.snesPeer){if(now%20==0)net.mcreator.commandcookieselectronicmod.snes.SnesSystem.tickMonitor(d);return;}
        if(now%20==0){PowerCables.ensureAv(d);var other=peer(d);powered(d,supply(d));if(d.peer!=null&&level.hasChunkAt(d.peer)&&other==null)detach(d,true);}
        if(d.monitor()||d.owner==null)return;
        var p=level.getServer().getPlayerList().getPlayer(d.owner);
        if(p==null||!allowed(d,p)){close(d,"Keep the controller in hand and stay near the connected monitor.");return;}
        int before=d.lastInput,mask=0;NesInputQueue.Input input;
        while((input=d.inputs.poll(d.game.generation(),mask))!=null){
            d.lastInput=input.sequence();if(input.action()<=4&&input.generation()!=d.game.generation())continue;
            mask|=1<<input.action();apply(d,input);
        }
        d.game.tick();d.best=Math.max(d.best,d.game.score);
        if(now%20==0)d.setChanged();
        if(before!=d.lastInput||(d.game.started&&!d.game.paused&&!d.game.over)||now%20==0)send(d,p,false,true,"");
    }
    private static void send(NesDeviceBlockEntity nes,ServerPlayer p,boolean open,boolean valid,String message){
        var monitor=peer(nes);BlockPos pos=monitor!=null?monitor.getBlockPos():nes.peer!=null?nes.peer:nes.getBlockPos();
        PacketDistributor.sendToPlayer(p,NesPayloads.State.of(pos,nes.session,nes.game,nes.best,nes.lastInput,open,valid,message));
    }
}
