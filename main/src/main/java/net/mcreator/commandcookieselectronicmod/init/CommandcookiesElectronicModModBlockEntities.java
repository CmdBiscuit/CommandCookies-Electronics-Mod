/*
 *    Maintained source: register new content directly in this file.
 */
package net.mcreator.commandcookieselectronicmod.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;

import net.mcreator.commandcookieselectronicmod.block.entity.*;
import net.mcreator.commandcookieselectronicmod.CommandcookiesElectronicModMod;

@EventBusSubscriber
public class CommandcookiesElectronicModModBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, CommandcookiesElectronicModMod.MODID);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LaptopBlockEntity>> LAPTOP = register("laptop", CommandcookiesElectronicModModBlocks.LAPTOP, LaptopBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectronicsWorkbenchBlockEntity>> ELECTRONICS_WORKBENCH = register("electronics_workbench", CommandcookiesElectronicModModBlocks.ELECTRONICS_WORKBENCH,
			ElectronicsWorkbenchBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LaptopBlackBlockEntity>> LAPTOP_BLACK = register("laptop_black", CommandcookiesElectronicModModBlocks.LAPTOP_BLACK, LaptopBlackBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LaptopWhiteBlockEntity>> LAPTOP_WHITE = register("laptop_white", CommandcookiesElectronicModModBlocks.LAPTOP_WHITE, LaptopWhiteBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LaptopNavyBlueBlockEntity>> LAPTOP_NAVY_BLUE = register("laptop_navy_blue", CommandcookiesElectronicModModBlocks.LAPTOP_NAVY_BLUE, LaptopNavyBlueBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LaptopForestGreenBlockEntity>> LAPTOP_FOREST_GREEN = register("laptop_forest_green", CommandcookiesElectronicModModBlocks.LAPTOP_FOREST_GREEN,
			LaptopForestGreenBlockEntity::new);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LaptopBurgundyBlockEntity>> LAPTOP_BURGUNDY = register("laptop_burgundy", CommandcookiesElectronicModModBlocks.LAPTOP_BURGUNDY, LaptopBurgundyBlockEntity::new);

	// Start of user code block custom block entities
	// End of user code block custom block entities
	private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(String registryname, DeferredHolder<Block, Block> block, BlockEntityType.BlockEntitySupplier<T> supplier) {
		return REGISTRY.register(registryname, () -> BlockEntityType.Builder.of(supplier, block.get()).build(null));
	}

	@SubscribeEvent
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LAPTOP.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ELECTRONICS_WORKBENCH.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LAPTOP_BLACK.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LAPTOP_WHITE.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LAPTOP_NAVY_BLUE.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LAPTOP_FOREST_GREEN.get(), SidedInvWrapper::new);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LAPTOP_BURGUNDY.get(), SidedInvWrapper::new);
	}
}