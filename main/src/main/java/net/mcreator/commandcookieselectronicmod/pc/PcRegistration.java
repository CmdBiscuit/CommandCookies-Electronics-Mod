package net.mcreator.commandcookieselectronicmod.pc;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModBlocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
@EventBusSubscriber(modid="commandcookies_electronic_mod",bus=EventBusSubscriber.Bus.MOD)
public final class PcRegistration {
    public static BlockEntityType<PcDeviceEntity> DEVICE_TYPE;
    @SubscribeEvent public static void register(RegisterEvent e){if(e.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)){DEVICE_TYPE=BlockEntityType.Builder.of(PcDeviceEntity::new,CommandcookiesElectronicModModBlocks.GAMING_PC.get(),CommandcookiesElectronicModModBlocks.GAMING_MONITOR.get(),CommandcookiesElectronicModModBlocks.KEYBOARD.get(),CommandcookiesElectronicModModBlocks.MOUSE.get(),CommandcookiesElectronicModModBlocks.GAMING_PC_WHITE.get(),CommandcookiesElectronicModModBlocks.GAMING_MONITOR_WHITE.get(),CommandcookiesElectronicModModBlocks.KEYBOARD_WHITE.get(),CommandcookiesElectronicModModBlocks.MOUSE_WHITE.get()).build(null);e.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(id("pc_device"),DEVICE_TYPE));}}
    public static ResourceLocation id(String name){return ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod",name);}
}
