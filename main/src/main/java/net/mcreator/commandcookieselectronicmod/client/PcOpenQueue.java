package net.mcreator.commandcookieselectronicmod.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.mcreator.commandcookieselectronicmod.pc.PcOpenResponse;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
/** A server-approved open can arrive before that tick's block entity updates. */
@EventBusSubscriber(modid="commandcookies_electronic_mod",value=Dist.CLIENT)
public final class PcOpenQueue {
    private static PcOpenResponse pending;private static ClientLevel owner;private static long until;
    private static void clear(){pending=null;owner=null;until=0;}
    public static void accept(PcOpenResponse message){clear();var g=Minecraft.getInstance();if(g.level==null||g.player==null||g.screen!=null)return;pending=message;owner=g.level;until=owner.getGameTime()+40;tryOpen();}
    private static void tryOpen(){
        if(pending==null)return;var g=Minecraft.getInstance();
        if(g.level!=owner||g.player==null||g.screen!=null||g.player.isShiftKeyDown()||!g.player.isAlive()||g.player.isSpectator()||owner.getGameTime()>until||g.player.distanceToSqr(Vec3.atCenterOf(pending.monitor()))>64){clear();return;}
        var access=DesktopAccess.at(owner,pending.monitor());if(access==null)return;
        if(!access.device().equals(pending.device())||!access.session().equals(pending.session())){clear();return;}
        var position=pending.monitor();clear();g.setScreen(new LaptopDesktopScreen(position));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){tryOpen();}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){clear();}
}
