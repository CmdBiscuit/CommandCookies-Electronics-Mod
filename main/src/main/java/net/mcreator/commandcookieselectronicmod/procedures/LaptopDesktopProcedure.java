package net.mcreator.commandcookieselectronicmod.procedures;

import net.mcreator.commandcookieselectronicmod.block.LaptopBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class LaptopDesktopProcedure {
    @EventBusSubscriber(modid="commandcookies_electronic_mod",value=Dist.CLIENT)
    public static final class Client {
        @SubscribeEvent public static void disconnected(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){
            net.mcreator.commandcookieselectronicmod.client.LaptopDesktopScreen.clearSessions();
        }
    }
}
