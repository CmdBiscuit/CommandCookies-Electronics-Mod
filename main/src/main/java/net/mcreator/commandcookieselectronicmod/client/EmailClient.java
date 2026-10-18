package net.mcreator.commandcookieselectronicmod.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.mcreator.commandcookieselectronicmod.email.EmailData;
import net.mcreator.commandcookieselectronicmod.network.EmailPackets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="commandcookies_electronic_mod",value=Dist.CLIENT)
public final class EmailClient {
    public static String address="";public static List<EmailData.Entry> messages=List.of();public static long version;public static int unread;
    private static long sequence;private static final SystemToast.SystemToastId TOAST=new SystemToast.SystemToastId(6000);
    public static long request(BlockPos pos,UUID device,UUID session,int action,String to,String subject,String body,UUID message,boolean outgoing){return request(pos,device,session,action,to,subject,body,message,outgoing,new byte[0]);}
    public static long request(BlockPos pos,UUID device,UUID session,int action,String to,String subject,String body,UUID message,boolean outgoing,byte[] photo){if(Minecraft.getInstance().getConnection()==null)return 0;long id=++sequence;PacketDistributor.sendToServer(new EmailPackets.Request(pos,device,session,id,action,to,subject,body,message==null?new UUID(0,0):message,outgoing,photo));return id;}
    public static void snapshot(EmailPackets.Snapshot s){if(!(Minecraft.getInstance().screen instanceof EmailScreen screen)||!screen.matches(s))return;if(s.success()){address=s.address();messages=s.messages();unread=(int)messages.stream().filter(e->!e.outgoing()&&!e.read()).count();version++;}screen.response(s);}
    public static void photo(EmailPackets.Photo p){if(Minecraft.getInstance().screen instanceof EmailScreen screen)screen.photo(p);}
    public static void notice(EmailPackets.Notice n){var g=Minecraft.getInstance();if(g.player==null)return;unread=n.unread();g.getToasts().addToast(SystemToast.multiline(g,TOAST,Component.literal(n.from().isEmpty()?"Email inbox":"Email from "+n.from()),Component.literal(n.subject()+"\nOpen Email on a computer to read.")));if(g.screen instanceof EmailScreen screen)screen.refreshSoon();}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){address="";messages=List.of();version++;unread=0;sequence=0;SystemToast.forceHide(Minecraft.getInstance().getToasts(),TOAST);}
}
