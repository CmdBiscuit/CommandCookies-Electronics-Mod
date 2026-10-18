package net.mcreator.commandcookieselectronicmod.procedures;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.mcreator.commandcookieselectronicmod.inventory.WorkbenchMenu;

@EventBusSubscriber(modid="commandcookies_electronic_mod",bus=EventBusSubscriber.Bus.MOD)
public class WorkbenchGuiProcedure {
    @SubscribeEvent public static void registerMenu(RegisterEvent event) {
        event.register(Registries.MENU,ResourceLocation.parse("commandcookies_electronic_mod:electronics_workbench"),()->WorkbenchMenu.TYPE);
    }
}
