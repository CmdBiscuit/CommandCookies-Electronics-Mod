package net.mcreator.commandcookieselectronicmod.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.mcreator.commandcookieselectronicmod.network.PhoneMessagePackets;
import net.mcreator.commandcookieselectronicmod.phone.PhoneDeviceIdentity;
import net.mcreator.commandcookieselectronicmod.procedures.IphoneLogicProcedure;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Session-only server snapshots. Never mixes inboxes between phones or servers. */
@EventBusSubscriber(modid="commandcookies_electronic_mod",value=Dist.CLIENT)
public final class PhoneMessagesClient {
    public static final class Model {public List<PhoneMessagePackets.Contact> contacts=List.of();public List<net.mcreator.commandcookieselectronicmod.phone.PhoneMessageData.Entry> messages=List.of();public long version,lastSync;}
    private static final LinkedHashMap<UUID,Model> models=new LinkedHashMap<>(8,.75f,true);
    private static final SystemToast.SystemToastId TOAST=new SystemToast.SystemToastId(6000);
    private static long sequence;
    public static Model model(UUID device){var known=models.computeIfAbsent(device,k->new Model());while(models.size()>8){var it=models.entrySet().iterator();it.next();it.remove();}return known;}
    public static int unread(UUID device){return device==null?0:(int)model(device).messages.stream().filter(e->!e.outgoing()&&!e.read()).count();}
    public static boolean held(UUID device){var p=Minecraft.getInstance().player;return p!=null&&device!=null&&(device.equals(PhoneDeviceIdentity.id(p.getMainHandItem()))&&IphoneLogicProcedure.color(p.getMainHandItem())!=null||device.equals(PhoneDeviceIdentity.id(p.getOffhandItem()))&&IphoneLogicProcedure.color(p.getOffhandItem())!=null);}
    private static boolean carrying(UUID device){var p=Minecraft.getInstance().player;if(p==null)return false;for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(IphoneLogicProcedure.color(s)!=null&&device.equals(PhoneDeviceIdentity.id(s)))return true;}return false;}
    public static long request(UUID device,int action,UUID peer,String text){if(!held(device)||Minecraft.getInstance().getConnection()==null)return 0;long id=++sequence;PacketDistributor.sendToServer(new PhoneMessagePackets.Request(device,id,action,peer==null?new UUID(0,0):peer,text));return id;}
    public static void sync(UUID device){if(device==null||!held(device))return;var model=model(device);long now=System.nanoTime();if(now-model.lastSync<600_000_000L)return;model.lastSync=now;request(device,PhoneMessagePackets.SYNC,null,"");}
    public static void snapshot(PhoneMessagePackets.Snapshot snapshot){if(!carrying(snapshot.device()))return;if(snapshot.success()){var model=model(snapshot.device());model.contacts=snapshot.contacts();model.messages=snapshot.messages();model.version++;}if(Minecraft.getInstance().screen instanceof PhoneMessagesScreen screen)screen.response(snapshot);}
    public static void notice(PhoneMessagePackets.Notice notice){
        if(!carrying(notice.device()))return;var game=Minecraft.getInstance();var entry=notice.message();var model=model(notice.device());
        if(model.messages.stream().noneMatch(e->e.id().equals(entry.id()))){var list=new ArrayList<>(model.messages);list.add(entry);if(list.size()>128)list.removeFirst();model.messages=List.copyOf(list);model.version++;}
        boolean viewing=game.screen instanceof PhoneMessagesScreen screen&&screen.viewing(notice.device(),entry.peer());
        if(!viewing){String preview=entry.text().length()>64?entry.text().substring(0,64)+"...":entry.text();game.getToasts().addToast(SystemToast.multiline(game,TOAST,Component.literal("Message from "+entry.name()),Component.literal(preview+"\nOpen Messages to reply.")));var prefs=PhoneAppState.INSTANCE.profile(PhoneAppState.key(game.player.getUUID(),notice.device())).settings;PhoneWallpapers.chime(prefs);}
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){models.clear();sequence=0;SystemToast.forceHide(Minecraft.getInstance().getToasts(),TOAST);}
}
