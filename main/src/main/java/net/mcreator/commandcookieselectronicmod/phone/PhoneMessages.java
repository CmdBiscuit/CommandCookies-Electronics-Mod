package net.mcreator.commandcookieselectronicmod.phone;

import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.mcreator.commandcookieselectronicmod.network.PhoneMessagePackets;
import net.mcreator.commandcookieselectronicmod.procedures.IphoneLogicProcedure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Sender identity, phone access and delivery are always checked on the server. */
@EventBusSubscriber(modid="commandcookies_electronic_mod")
public final class PhoneMessages {
    private static final Map<MinecraftServer,Map<UUID,long[]>> cooldowns=new WeakHashMap<>();
    public static boolean carrying(ServerPlayer player){for(int i=0;i<player.getInventory().getContainerSize();i++)if(IphoneLogicProcedure.color(player.getInventory().getItem(i))!=null)return true;return false;}
    private static ItemStack held(ServerPlayer player,UUID id){for(var stack:List.of(player.getMainHandItem(),player.getOffhandItem()))if(IphoneLogicProcedure.color(stack)!=null&&id.equals(PhoneDeviceIdentity.id(stack)))return stack;return ItemStack.EMPTY;}
    public static UUID deliveryDevice(ServerPlayer player){
        PhoneDeviceIdentity.ensureInventory(player);
        for(var stack:List.of(player.getMainHandItem(),player.getOffhandItem()))if(IphoneLogicProcedure.color(stack)!=null)return PhoneDeviceIdentity.ensure(stack);
        for(int i=0;i<player.getInventory().getContainerSize();i++){var stack=player.getInventory().getItem(i);if(IphoneLogicProcedure.color(stack)!=null)return PhoneDeviceIdentity.ensure(stack);}return null;
    }
    private static List<PhoneMessagePackets.Contact> contacts(ServerPlayer player){return player.getServer().getPlayerList().getPlayers().stream().filter(p->p!=player&&carrying(p)&&p.isAlive()&&!p.isSpectator()).sorted(Comparator.comparing(p->p.getGameProfile().getName(),String.CASE_INSENSITIVE_ORDER)).limit(128).map(p->new PhoneMessagePackets.Contact(p.getUUID(),p.getGameProfile().getName())).toList();}
    private static void reply(ServerPlayer player,UUID device,long request,boolean ok,String status){PacketDistributor.sendToPlayer(player,new PhoneMessagePackets.Snapshot(device,request,ok,status,ok?contacts(player):List.of(),ok?PhoneMessageData.get(player.getServer()).history(player.getUUID(),device):List.of()));}
    public static void handle(ServerPlayer player,PhoneMessagePackets.Request request){
        if(!player.isAlive()||player.isSpectator()||request.action()<0||request.action()>2||held(player,request.device()).isEmpty())return;
        var server=player.getServer();long now=server.overworld().getGameTime();var clocks=cooldowns.computeIfAbsent(server,k->new HashMap<>()).computeIfAbsent(player.getUUID(),k->new long[]{Long.MIN_VALUE,Long.MIN_VALUE,Long.MIN_VALUE});int action=request.action();
        long gap=action==PhoneMessagePackets.SEND?20:action==PhoneMessagePackets.SYNC?10:2;
        if(clocks[action]!=Long.MIN_VALUE&&now>=clocks[action]&&now-clocks[action]<gap){if(action==PhoneMessagePackets.SEND)reply(player,request.device(),request.request(),false,"Please wait a moment before sending again.");return;}
        clocks[action]=now;
        if(action==PhoneMessagePackets.SYNC){reply(player,request.device(),request.request(),true,"");return;}
        var data=PhoneMessageData.get(server);
        if(action==PhoneMessagePackets.READ){data.read(player.getUUID(),request.device(),request.peer());reply(player,request.device(),request.request(),true,"");return;}
        String text=request.text().strip();
        if(!PhoneMessageData.validText(text)){reply(player,request.device(),request.request(),false,"Write a message of up to 256 characters.");return;}
        var recipient=server.getPlayerList().getPlayer(request.peer());
        if(recipient==null||recipient==player||!recipient.isAlive()||recipient.isSpectator()){reply(player,request.device(),request.request(),false,"That player is not available right now.");return;}
        UUID target=deliveryDevice(recipient);
        if(target==null){reply(player,request.device(),request.request(),false,"That player needs a phone in their inventory.");return;}
        var a=new PhoneMessageData.Key(player.getUUID(),request.device());var b=new PhoneMessageData.Key(recipient.getUUID(),target);
        if(!data.canStore(a,b)){reply(player,request.device(),request.request(),false,"The server's message storage is full.");return;}
        UUID id=UUID.randomUUID();long time=System.currentTimeMillis();
        var incoming=new PhoneMessageData.Entry(id,player.getUUID(),player.getGameProfile().getName(),text,time,false,false);
        data.append(a,new PhoneMessageData.Entry(id,recipient.getUUID(),recipient.getGameProfile().getName(),text,time,true,true));data.append(b,incoming);
        reply(player,request.device(),request.request(),true,"Delivered");
        PacketDistributor.sendToPlayer(recipient,new PhoneMessagePackets.Notice(target,incoming));reply(recipient,target,0,true,"");
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){var map=cooldowns.get(p.getServer());if(map!=null)map.remove(p.getUUID());}}
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){cooldowns.remove(e.getServer());}
}
