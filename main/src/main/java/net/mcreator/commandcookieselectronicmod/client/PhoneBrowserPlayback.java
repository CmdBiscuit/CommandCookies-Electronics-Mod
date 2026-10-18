package net.mcreator.commandcookieselectronicmod.client;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.mcreator.commandcookieselectronicmod.phone.PhoneDeviceIdentity;
import net.mcreator.commandcookieselectronicmod.procedures.IphoneLogicProcedure;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** One local phone media session can keep playing without a visible browser. */
@EventBusSubscriber(modid="commandcookies_electronic_mod",value=Dist.CLIENT)
public final class PhoneBrowserPlayback {
    private static Retained retained;
    private static int ticks;
    private static final long START_GRACE=5_000_000_000L,IDLE_GRACE=30_000_000_000L;
    private static final class Retained {
        final UUID player,device;final ChromiumSession session;final PhonePreferences preferences;
        long lastPlaying=System.nanoTime();boolean heard;
        Retained(UUID player,UUID device,ChromiumSession session,PhonePreferences preferences){this.player=player;this.device=device;this.session=session;this.preferences=preferences;heard=session.mediaPlaying();}
    }
    private PhoneBrowserPlayback(){}
    private static boolean carrying(UUID player,UUID device){
        var p=Minecraft.getInstance().player;if(p==null||!p.isAlive()||!p.getUUID().equals(player)||device==null)return false;
        for(int i=0;i<p.getInventory().getContainerSize();i++){var stack=p.getInventory().getItem(i);if(IphoneLogicProcedure.color(stack)!=null&&device.equals(PhoneDeviceIdentity.id(stack)))return true;}
        return false;
    }
    static boolean retain(UUID player,UUID device,ChromiumSession session,PhonePreferences preferences){
        if(session==null||!session.media()||!session.ready()||session.failed()||!preferences.backgroundMedia||!session.belongsTo(player,device)||!carrying(player,device))return false;
        stopAll();retained=new Retained(player,device,session,preferences);session.background(true);return true;
    }
    static ChromiumSession resume(UUID player,UUID device,int width,int height,int fps){
        var saved=retained;if(saved==null||!saved.player.equals(player)||!saved.device.equals(device))return null;
        retained=null;if(!saved.session.ready()||saved.session.failed()||!carrying(player,device)){saved.session.close();return null;}
        saved.session.mediaFrameRate(fps);saved.session.resize(width,height);saved.session.background(false);return saved.session;
    }
    public static boolean running(UUID device){var p=Minecraft.getInstance().player;return p!=null&&retained!=null&&retained.player.equals(p.getUUID())&&retained.device.equals(device)&&retained.session.ready();}
    public static void stop(UUID device){if(running(device))stopAll();}
    public static void stopAll(){var old=retained;retained=null;if(old!=null)old.session.close();}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(++ticks%10!=0)return;var saved=retained;if(saved==null)return;
        if(!saved.preferences.backgroundMedia||!saved.session.ready()||saved.session.failed()||!carrying(saved.player,saved.device)){stopAll();return;}
        long now=System.nanoTime();if(saved.session.mediaPlaying()){saved.heard=true;saved.lastPlaying=now;}
        else if(now-saved.lastPlaying>(saved.heard?IDLE_GRACE:START_GRACE))stopAll();
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){stopAll();}
}
