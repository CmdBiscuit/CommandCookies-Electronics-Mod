package net.mcreator.commandcookieselectronicmod.nes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModBlocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
@EventBusSubscriber(modid="commandcookies_electronic_mod",bus=EventBusSubscriber.Bus.MOD)
public final class NesSystemRegistration {
    public static BlockEntityType<NesDeviceBlockEntity> DEVICE_TYPE;
    @SubscribeEvent public static void register(RegisterEvent e){
        if(e.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)){
            DEVICE_TYPE=BlockEntityType.Builder.of(NesDeviceBlockEntity::new,CommandcookiesElectronicModModBlocks.RETRO_MONITOR.get(),CommandcookiesElectronicModModBlocks.NES_CONSOLE.get()).build(null);
            e.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod","nes_device"),DEVICE_TYPE));
        }
    }
}
