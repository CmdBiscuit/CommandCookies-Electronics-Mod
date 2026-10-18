package net.mcreator.commandcookieselectronicmod.security;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModBlocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
@EventBusSubscriber(modid="commandcookies_electronic_mod",bus=EventBusSubscriber.Bus.MOD)
public final class SecurityCameraRegistration {
 public static BlockEntityType<SecurityCameraEntity> CAMERA_TYPE;
 @SubscribeEvent public static void register(RegisterEvent event){if(event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)){CAMERA_TYPE=BlockEntityType.Builder.of(SecurityCameraEntity::new,CommandcookiesElectronicModModBlocks.SECURITY_CAMERA.get()).build(null);event.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod","security_camera"),CAMERA_TYPE));}}
}
