package net.mcreator.commandcookieselectronicmod.snes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModBlocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid="commandcookies_electronic_mod",bus=EventBusSubscriber.Bus.MOD)
public final class SnesRegistration {
    public static BlockEntityType<SnesDeviceBlockEntity> DEVICE_TYPE;
    @SubscribeEvent public static void register(RegisterEvent e){
        if(e.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)){
            DEVICE_TYPE=BlockEntityType.Builder.of(SnesDeviceBlockEntity::new,CommandcookiesElectronicModModBlocks.SNES_CONSOLE.get()).build(null);
            e.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod","snes_device"),DEVICE_TYPE));
        }
    }
}
