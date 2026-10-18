package net.mcreator.commandcookieselectronicmod.email;

import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.mcreator.commandcookieselectronicmod.block.LaptopBlock;
import net.mcreator.commandcookieselectronicmod.block.entity.LaptopBlockEntity;
import net.mcreator.commandcookieselectronicmod.pc.*;
import net.mcreator.commandcookieselectronicmod.network.EmailPackets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** All mailbox operations use the packet sender's identity and a current computer session. */
@EventBusSubscriber(modid="commandcookies_electronic_mod")
public final class EmailService {
    private static final Map<MinecraftServer,Map<UUID,long[]>> cooldowns=new WeakHashMap<>();
    public static boolean authorized(ServerPlayer p,EmailPackets.Request r){
        if(!p.isAlive()||p.isSpectator()||p.distanceToSqr(r.position().getX()+.5,r.position().getY()+.5,r.position().getZ()+.5)>64||!p.serverLevel().hasChunkAt(r.position()))return false;
        var e=p.serverLevel().getBlockEntity(r.position());
        if(e instanceof LaptopBlockEntity l)return l.getBlockState().getBlock() instanceof LaptopBlock&&l.getBlockState().getValue(LaptopBlock.OPEN)&&r.device().equals(l.deviceId)&&r.session().equals(l.desktopSessionId);
        if(e instanceof PcDeviceEntity monitor&&monitor.monitor()&&PcSystem.active(monitor)){var pc=PcSystem.peer(monitor);return pc!=null&&r.device().equals(pc.identity)&&r.session().equals(pc.desktopSession);}
        return false;
    }
    private static void reply(ServerPlayer p,EmailPackets.Request r,boolean ok,String status){var data=EmailData.get(p.getServer());PacketDistributor.sendToPlayer(p,new EmailPackets.Snapshot(r.position(),r.device(),r.session(),r.request(),ok,status,ok?data.address(p.getUUID()):"",ok?data.history(p.getUUID()):List.of()));}
    private static void photoReply(ServerPlayer p,EmailPackets.Request r,String error){
        byte[] bytes=error.isEmpty()?EmailData.get(p.getServer()).attachment(p.getUUID(),r.message(),r.outgoing()):new byte[0];
        PacketDistributor.sendToPlayer(p,new EmailPackets.Photo(r.position(),r.device(),r.session(),r.request(),r.message(),error.isEmpty()&&bytes.length==0?"That photo is no longer available.":error,bytes));
    }
    public static void handle(ServerPlayer p,EmailPackets.Request r){
        if(r.action()<0||r.action()>EmailPackets.PHOTO)return;
        if(!authorized(p,r)){if(r.action()==EmailPackets.PHOTO)photoReply(p,r,"This computer is no longer available.");else reply(p,r,false,"This computer is no longer available.");return;}
        long now=p.serverLevel().getGameTime();var clocks=cooldowns.computeIfAbsent(p.getServer(),k->new HashMap<>()).computeIfAbsent(p.getUUID(),k->new long[]{Long.MIN_VALUE,Long.MIN_VALUE,Long.MIN_VALUE,Long.MIN_VALUE,Long.MIN_VALUE,Long.MIN_VALUE});
        int action=r.action();long gap=action==EmailPackets.SYNC?10:action==EmailPackets.READ?2:action==EmailPackets.PHOTO?4:20;
        if(clocks[action]!=Long.MIN_VALUE&&now>=clocks[action]&&now-clocks[action]<gap){if(action==EmailPackets.PHOTO)photoReply(p,r,"Please wait a moment and try again.");else if(action!=EmailPackets.SYNC)reply(p,r,false,"Please wait a moment and try again.");return;}clocks[action]=now;
        var data=EmailData.get(p.getServer());if(action==EmailPackets.PHOTO){photoReply(p,r,"");return;}String error="",status="";
        switch(action){
            case EmailPackets.CREATE->{error=data.create(p.getUUID(),r.address());status="Your email address is ready.";}
            case EmailPackets.SEND->{String to=EmailData.normalize(r.address());error=data.deliver(p.getUUID(),to,r.subject().strip(),r.body().strip(),r.photo());status="Email sent.";if(error.isEmpty()){var recipient=p.getServer().getPlayerList().getPlayer(data.owner(to));if(recipient!=null)PacketDistributor.sendToPlayer(recipient,new EmailPackets.Notice(data.address(p.getUUID()),r.subject().strip(),data.unread(recipient.getUUID())));}}
            case EmailPackets.READ->{if(!data.read(p.getUUID(),r.message()))error="That email is no longer in your inbox.";}
            case EmailPackets.DELETE->{if(!data.delete(p.getUUID(),r.message(),r.outgoing()))error="That email is no longer in your mailbox.";else status="Email deleted.";}
            default->{}
        }
        reply(p,r,error.isEmpty(),error.isEmpty()?status:error);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){int unread=EmailData.get(p.getServer()).unread(p.getUUID());if(unread>0)PacketDistributor.sendToPlayer(p,new EmailPackets.Notice("",unread+" unread emails in your inbox",unread));}}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){var map=cooldowns.get(p.getServer());if(map!=null)map.remove(p.getUUID());}}
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){cooldowns.remove(e.getServer());}
}
