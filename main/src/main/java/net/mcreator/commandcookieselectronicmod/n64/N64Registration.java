package net.mcreator.commandcookieselectronicmod.n64;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModBlocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid="commandcookies_electronic_mod",bus=EventBusSubscriber.Bus.MOD)
public final class N64Registration {
    public static BlockEntityType<N64DeviceBlockEntity> DEVICE_TYPE;
    @SubscribeEvent public static void register(RegisterEvent e){
        if(e.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)){
            DEVICE_TYPE=BlockEntityType.Builder.of(N64DeviceBlockEntity::new,CommandcookiesElectronicModModBlocks.N64_CONSOLE.get()).build(null);
            e.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod","n64_device"),DEVICE_TYPE));
        }
    }
}
