package net.mcreator.commandcookieselectronicmod.tv;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModBlocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
@EventBusSubscriber(modid="commandcookies_electronic_mod",bus=EventBusSubscriber.Bus.MOD)
public final class TvRegistration {
    public static BlockEntityType<ImageTvEntity> TYPE;
    public static TvPartBlock PART;
    @SubscribeEvent public static void register(RegisterEvent event){
        if(event.getRegistryKey().equals(Registries.BLOCK)){PART=new TvPartBlock();event.register(Registries.BLOCK,h->h.register(ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod","tv_part"),PART));}
        if(event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)){TYPE=BlockEntityType.Builder.of(ImageTvEntity::new,CommandcookiesElectronicModModBlocks.TV_WALL_MOUNTED.get(),CommandcookiesElectronicModModBlocks.TV_WITH_STAND.get()).build(null);event.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod","image_tv"),TYPE));}
    }
}
