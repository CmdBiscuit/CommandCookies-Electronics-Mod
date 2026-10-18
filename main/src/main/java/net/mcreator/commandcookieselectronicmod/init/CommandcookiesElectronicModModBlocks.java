/*
 *    Maintained source: register new content directly in this file.
 */
package net.mcreator.commandcookieselectronicmod.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;

import net.minecraft.world.level.block.Block;

import net.mcreator.commandcookieselectronicmod.block.*;
import net.mcreator.commandcookieselectronicmod.CommandcookiesElectronicModMod;

public class CommandcookiesElectronicModModBlocks {
	public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(CommandcookiesElectronicModMod.MODID);
	public static final DeferredBlock<Block> GAMING_CHAIR_RED;
	public static final DeferredBlock<Block> GAMING_CHAIR_WHITE;
	public static final DeferredBlock<Block> GAMING_CHAIR_ORANGE;
	public static final DeferredBlock<Block> GAMING_CHAIR_MAGENTA;
	public static final DeferredBlock<Block> GAMING_CHAIR_LIGHT_BLUE;
	public static final DeferredBlock<Block> GAMING_CHAIR_YELLOW;
	public static final DeferredBlock<Block> GAMING_CHAIR_LIME;
	public static final DeferredBlock<Block> GAMING_CHAIR_PINK;
	public static final DeferredBlock<Block> GAMING_CHAIR_GRAY;
	public static final DeferredBlock<Block> GAMING_CHAIR_LIGHT_GRAY;
	public static final DeferredBlock<Block> GAMING_CHAIR_CYAN;
	public static final DeferredBlock<Block> GAMING_CHAIR_PURPLE;
	public static final DeferredBlock<Block> GAMING_CHAIR_BLUE;
	public static final DeferredBlock<Block> GAMING_CHAIR_BROWN;
	public static final DeferredBlock<Block> GAMING_CHAIR_GREEN;
	public static final DeferredBlock<Block> GAMING_CHAIR_BLACK;
	public static final DeferredBlock<Block> CRT_TV;
	public static final DeferredBlock<Block> LAPTOP;
	public static final DeferredBlock<Block> ADVANCED_CIRCUIT_BOARD;
	public static final DeferredBlock<Block> ELECTRONICS_WORKBENCH;
	public static final DeferredBlock<Block> RETRO_MONITOR;
	public static final DeferredBlock<Block> NES_CONSOLE;
	public static final DeferredBlock<Block> BASIC_CIRCUIT_BOARD;
	public static final DeferredBlock<Block> GENERATOR;
	public static final DeferredBlock<Block> WALL_POWER_OUTLET;
	public static final DeferredBlock<Block> WALL_COAX_OUTLET;
	public static final DeferredBlock<Block> LAPTOP_BLACK;
	public static final DeferredBlock<Block> LAPTOP_WHITE;
	public static final DeferredBlock<Block> LAPTOP_NAVY_BLUE;
	public static final DeferredBlock<Block> LAPTOP_FOREST_GREEN;
	public static final DeferredBlock<Block> LAPTOP_BURGUNDY;
	public static final DeferredBlock<Block> GAMING_PC;
	public static final DeferredBlock<Block> GAMING_MONITOR;
	public static final DeferredBlock<Block> KEYBOARD;
	public static final DeferredBlock<Block> MOUSE;
	public static final DeferredBlock<Block> GAMING_PC_WHITE;
	public static final DeferredBlock<Block> GAMING_MONITOR_WHITE;
	public static final DeferredBlock<Block> KEYBOARD_WHITE;
	public static final DeferredBlock<Block> MOUSE_WHITE;
	public static final DeferredBlock<Block> SNES_CONSOLE;
	public static final DeferredBlock<Block> N64_CONSOLE;
	public static final DeferredBlock<Block> SECURITY_CAMERA;
	public static final DeferredBlock<Block> TV_WALL_MOUNTED;
	public static final DeferredBlock<Block> TV_WITH_STAND;
	static {
		GAMING_CHAIR_RED = REGISTRY.register("gaming_chair_red", GamingChairRedBlock::new);
		GAMING_CHAIR_WHITE = REGISTRY.register("gaming_chair_white", GamingChairWhiteBlock::new);
		GAMING_CHAIR_ORANGE = REGISTRY.register("gaming_chair_orange", GamingChairOrangeBlock::new);
		GAMING_CHAIR_MAGENTA = REGISTRY.register("gaming_chair_magenta", GamingChairMagentaBlock::new);
		GAMING_CHAIR_LIGHT_BLUE = REGISTRY.register("gaming_chair_light_blue", GamingChairLightBlueBlock::new);
		GAMING_CHAIR_YELLOW = REGISTRY.register("gaming_chair_yellow", GamingChairYellowBlock::new);
		GAMING_CHAIR_LIME = REGISTRY.register("gaming_chair_lime", GamingChairLimeBlock::new);
		GAMING_CHAIR_PINK = REGISTRY.register("gaming_chair_pink", GamingChairPinkBlock::new);
		GAMING_CHAIR_GRAY = REGISTRY.register("gaming_chair_gray", GamingChairGrayBlock::new);
		GAMING_CHAIR_LIGHT_GRAY = REGISTRY.register("gaming_chair_light_gray", GamingChairLightGrayBlock::new);
		GAMING_CHAIR_CYAN = REGISTRY.register("gaming_chair_cyan", GamingChairCyanBlock::new);
		GAMING_CHAIR_PURPLE = REGISTRY.register("gaming_chair_purple", GamingChairPurpleBlock::new);
		GAMING_CHAIR_BLUE = REGISTRY.register("gaming_chair_blue", GamingChairBlueBlock::new);
		GAMING_CHAIR_BROWN = REGISTRY.register("gaming_chair_brown", GamingChairBrownBlock::new);
		GAMING_CHAIR_GREEN = REGISTRY.register("gaming_chair_green", GamingChairGreenBlock::new);
		GAMING_CHAIR_BLACK = REGISTRY.register("gaming_chair_black", GamingChairBlackBlock::new);
		CRT_TV = REGISTRY.register("crt_tv", CrtTvBlock::new);
		LAPTOP = REGISTRY.register("laptop", LaptopBlock::new);
		ADVANCED_CIRCUIT_BOARD = REGISTRY.register("advanced_circuit_board", AdvancedCircuitBoardBlock::new);
		ELECTRONICS_WORKBENCH = REGISTRY.register("electronics_workbench", ElectronicsWorkbenchBlock::new);
		RETRO_MONITOR = REGISTRY.register("retro_monitor", RetroMonitorBlock::new);
		NES_CONSOLE = REGISTRY.register("nes_console", NesConsoleBlock::new);
		BASIC_CIRCUIT_BOARD = REGISTRY.register("basic_circuit_board", BasicCircuitBoardBlock::new);
		GENERATOR = REGISTRY.register("generator", GeneratorBlock::new);
		WALL_POWER_OUTLET = REGISTRY.register("wall_power_outlet", WallPowerOutletBlock::new);
		WALL_COAX_OUTLET = REGISTRY.register("wall_coax_outlet", WallCoaxOutletBlock::new);
		LAPTOP_BLACK = REGISTRY.register("laptop_black", LaptopBlackBlock::new);
		LAPTOP_WHITE = REGISTRY.register("laptop_white", LaptopWhiteBlock::new);
		LAPTOP_NAVY_BLUE = REGISTRY.register("laptop_navy_blue", LaptopNavyBlueBlock::new);
		LAPTOP_FOREST_GREEN = REGISTRY.register("laptop_forest_green", LaptopForestGreenBlock::new);
		LAPTOP_BURGUNDY = REGISTRY.register("laptop_burgundy", LaptopBurgundyBlock::new);
		GAMING_PC = REGISTRY.register("gaming_pc", GamingPcBlock::new);
		GAMING_MONITOR = REGISTRY.register("gaming_monitor", GamingMonitorBlock::new);
		KEYBOARD = REGISTRY.register("keyboard", KeyboardBlock::new);
		MOUSE = REGISTRY.register("mouse", MouseBlock::new);
		GAMING_PC_WHITE = REGISTRY.register("gaming_pc_white", GamingPcWhiteBlock::new);
		GAMING_MONITOR_WHITE = REGISTRY.register("gaming_monitor_white", GamingMonitorWhiteBlock::new);
		KEYBOARD_WHITE = REGISTRY.register("keyboard_white", KeyboardWhiteBlock::new);
		MOUSE_WHITE = REGISTRY.register("mouse_white", MouseWhiteBlock::new);
		SNES_CONSOLE = REGISTRY.register("snes_console", SnesConsoleBlock::new);
		N64_CONSOLE = REGISTRY.register("n64_console", N64ConsoleBlock::new);
		SECURITY_CAMERA = REGISTRY.register("security_camera", SecurityCameraBlock::new);
		TV_WALL_MOUNTED = REGISTRY.register("tv_wall_mounted", TvWallMountedBlock::new);
		TV_WITH_STAND = REGISTRY.register("tv_with_stand", TvWithStandBlock::new);
	}
	// Start of user code block custom blocks
	// End of user code block custom blocks
}