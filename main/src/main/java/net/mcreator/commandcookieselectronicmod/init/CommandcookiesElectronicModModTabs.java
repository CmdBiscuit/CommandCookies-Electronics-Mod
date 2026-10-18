/*
 *    Maintained source: register new content directly in this file.
 */
package net.mcreator.commandcookieselectronicmod.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

import net.mcreator.commandcookieselectronicmod.CommandcookiesElectronicModMod;

public class CommandcookiesElectronicModModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CommandcookiesElectronicModMod.MODID);
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GAMING_CHAIRS = REGISTRY.register("gaming_chairs", () -> CreativeModeTab.builder().title(Component.translatable("item_group.commandcookies_electronic_mod.gaming_chairs"))
			.icon(() -> new ItemStack(CommandcookiesElectronicModModItems.S_26_ULTRA.get())).displayItems((parameters, tabData) -> {
				tabData.accept(CommandcookiesElectronicModModItems.IPHONE_18_PRO_MAX.get());
				tabData.accept(CommandcookiesElectronicModModItems.IPHONE_18_PRO_MAX_BLACK.get());
				tabData.accept(CommandcookiesElectronicModModItems.IPHONE_18_PRO_MAX_SILVER.get());
				tabData.accept(CommandcookiesElectronicModModItems.IPHONE_18_PRO_MAX_GLACIER.get());
				tabData.accept(CommandcookiesElectronicModModItems.S_26_ULTRA.get());
				tabData.accept(CommandcookiesElectronicModModItems.S_26_ULTRA_COBALT_VIOLET.get());
				tabData.accept(CommandcookiesElectronicModModItems.S_26_ULTRA_SKY_BLUE.get());
				tabData.accept(CommandcookiesElectronicModModItems.S_26_ULTRA_WHITE.get());
				tabData.accept(CommandcookiesElectronicModModItems.S_26_ULTRA_PINK_GOLD.get());
				tabData.accept(CommandcookiesElectronicModModItems.S_26_ULTRA_SILVER_SHADOW.get());
				tabData.accept(CommandcookiesElectronicModModBlocks.LAPTOP.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.LAPTOP_BLACK.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.LAPTOP_WHITE.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.LAPTOP_NAVY_BLUE.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.LAPTOP_FOREST_GREEN.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.LAPTOP_BURGUNDY.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.GAMING_PC.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.GAMING_PC_WHITE.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.GAMING_MONITOR.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.GAMING_MONITOR_WHITE.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.KEYBOARD.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.KEYBOARD_WHITE.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.MOUSE.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.MOUSE_WHITE.get().asItem());
				tabData.accept(CommandcookiesElectronicModModItems.CALCULATOR.get());
				tabData.accept(CommandcookiesElectronicModModBlocks.CRT_TV.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.TV_WALL_MOUNTED.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.TV_WITH_STAND.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.RETRO_MONITOR.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.NES_CONSOLE.get().asItem());
				tabData.accept(CommandcookiesElectronicModModItems.NES_CONTROLLER.get());
				tabData.accept(CommandcookiesElectronicModModItems.NES_TETRIS_CARTRIDGE.get());
				tabData.accept(CommandcookiesElectronicModModBlocks.SNES_CONSOLE.get().asItem());
				tabData.accept(CommandcookiesElectronicModModItems.SNES_CONTROLLER.get());
				tabData.accept(CommandcookiesElectronicModModBlocks.N64_CONSOLE.get().asItem());
				tabData.accept(CommandcookiesElectronicModModItems.N64_CONTROLLER.get());
				tabData.accept(CommandcookiesElectronicModModItems.AIRPODS_MAX_MIDNIGHT.get());
				tabData.accept(CommandcookiesElectronicModModItems.AIRPODS_MAX_STARLIGHT.get());
				tabData.accept(CommandcookiesElectronicModModItems.AIRPODS_MAX_BLUE.get());
				tabData.accept(CommandcookiesElectronicModModItems.AIRPODS_MAX_PURPLE.get());
				tabData.accept(CommandcookiesElectronicModModItems.AIRPODS_MAX_ORANGE.get());
				tabData.accept(CommandcookiesElectronicModModItems.AIRPODS_MAX_SILVER.get());
				tabData.accept(CommandcookiesElectronicModModItems.AIRPODS_MAX_SPACE_GRAY.get());
				tabData.accept(CommandcookiesElectronicModModItems.AIRPODS_MAX_SKY_BLUE.get());
				tabData.accept(CommandcookiesElectronicModModItems.AIRPODS_MAX_PINK.get());
				tabData.accept(CommandcookiesElectronicModModItems.AIRPODS_MAX_GREEN.get());
				tabData.accept(CommandcookiesElectronicModModBlocks.GAMING_CHAIR_WHITE.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.GAMING_CHAIR_ORANGE.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.GAMING_CHAIR_MAGENTA.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.GAMING_CHAIR_LIGHT_BLUE.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.GAMING_CHAIR_YELLOW.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.GAMING_CHAIR_LIME.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.GAMING_CHAIR_PINK.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.GAMING_CHAIR_GRAY.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.GAMING_CHAIR_LIGHT_GRAY.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.GAMING_CHAIR_CYAN.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.GAMING_CHAIR_PURPLE.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.GAMING_CHAIR_BLUE.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.GAMING_CHAIR_BROWN.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.GAMING_CHAIR_GREEN.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.GAMING_CHAIR_RED.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.GAMING_CHAIR_BLACK.get().asItem());
				tabData.accept(CommandcookiesElectronicModModItems.WRENCH.get());
				tabData.accept(CommandcookiesElectronicModModItems.WIRES.get());
				tabData.accept(CommandcookiesElectronicModModItems.CAMERA_LENS.get());
				tabData.accept(CommandcookiesElectronicModModBlocks.SECURITY_CAMERA.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.BASIC_CIRCUIT_BOARD.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.ADVANCED_CIRCUIT_BOARD.get().asItem());
				tabData.accept(CommandcookiesElectronicModModItems.AV_CABLE.get());
				tabData.accept(CommandcookiesElectronicModModItems.HDMI_CABLE.get());
				tabData.accept(CommandcookiesElectronicModModBlocks.ELECTRONICS_WORKBENCH.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.GENERATOR.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.WALL_POWER_OUTLET.get().asItem());
				tabData.accept(CommandcookiesElectronicModModBlocks.WALL_COAX_OUTLET.get().asItem());
				tabData.accept(CommandcookiesElectronicModModItems.AA_BATTERY.get());
			}).build());
}