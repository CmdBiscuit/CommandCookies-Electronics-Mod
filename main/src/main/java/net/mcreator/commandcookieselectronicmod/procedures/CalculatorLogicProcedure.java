package net.mcreator.commandcookieselectronicmod.procedures;

import net.minecraft.world.InteractionResult;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Locked custom MCreator procedure: owns the calculator screen and decimal engine. */
@EventBusSubscriber(modid = "commandcookies_electronic_mod")
public final class CalculatorLogicProcedure {
    private static boolean calculator(PlayerInteractEvent event) {
        return event.getItemStack().is(CommandcookiesElectronicModModItems.CALCULATOR.get());
    }
    @SubscribeEvent public static void useAir(PlayerInteractEvent.RightClickItem event) {
        if (calculator(event)) { event.setCancellationResult(InteractionResult.SUCCESS); event.setCanceled(true); }
    }
    @SubscribeEvent public static void useBlock(PlayerInteractEvent.RightClickBlock event) {
        if (calculator(event)) { event.setCancellationResult(InteractionResult.SUCCESS); event.setCanceled(true); }
    }
    @SubscribeEvent public static void useEntity(PlayerInteractEvent.EntityInteract event) {
        if (calculator(event)) { event.setCancellationResult(InteractionResult.SUCCESS); event.setCanceled(true); }
    }
    @SubscribeEvent public static void useEntityPart(PlayerInteractEvent.EntityInteractSpecific event) {
        if (calculator(event)) { event.setCancellationResult(InteractionResult.SUCCESS); event.setCanceled(true); }
    }

    // This nested subscriber is loaded only on the physical client, never a dedicated server.
    @EventBusSubscriber(modid = "commandcookies_electronic_mod", value = Dist.CLIENT)
    public static final class Client {
        private static void open(PlayerInteractEvent event) {
            var game = net.minecraft.client.Minecraft.getInstance();
            if (calculator(event) && event.getLevel().isClientSide() && event.getEntity() == game.player && game.screen == null)
                game.setScreen(new net.mcreator.commandcookieselectronicmod.client.CalculatorScreen());
        }
        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
        public static void useAir(PlayerInteractEvent.RightClickItem event) { open(event); }
        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
        public static void useBlock(PlayerInteractEvent.RightClickBlock event) { open(event); }
        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
        public static void useEntity(PlayerInteractEvent.EntityInteract event) { open(event); }
        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
        public static void useEntityPart(PlayerInteractEvent.EntityInteractSpecific event) { open(event); }
    }
}
