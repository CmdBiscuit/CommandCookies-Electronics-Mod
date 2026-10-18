package net.mcreator.commandcookieselectronicmod.power;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModBlocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
@EventBusSubscriber(modid="commandcookies_electronic_mod",bus=EventBusSubscriber.Bus.MOD)
public final class PowerRegistration {
    public static BlockEntityType<GeneratorEntity> GENERATOR;
    public static BlockEntityType<OutletEntity> OUTLET;
    @SubscribeEvent public static void register(RegisterEvent e){
        if(e.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)){
            GENERATOR=BlockEntityType.Builder.of(GeneratorEntity::new,CommandcookiesElectronicModModBlocks.GENERATOR.get()).build(null);
            OUTLET=BlockEntityType.Builder.of(OutletEntity::new,CommandcookiesElectronicModModBlocks.WALL_POWER_OUTLET.get(),CommandcookiesElectronicModModBlocks.WALL_COAX_OUTLET.get()).build(null);
            e.register(Registries.BLOCK_ENTITY_TYPE,h->{h.register(id("coal_generator"),GENERATOR);h.register(id("power_outlet"),OUTLET);});
        }
        if(e.getRegistryKey().equals(Registries.MENU))e.register(Registries.MENU,h->h.register(id("generator"),GeneratorMenu.TYPE));
    }
    static ResourceLocation id(String name){return ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod",name);}
}
