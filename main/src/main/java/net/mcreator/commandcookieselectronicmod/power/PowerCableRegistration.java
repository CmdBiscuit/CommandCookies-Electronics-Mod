package net.mcreator.commandcookieselectronicmod.power;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
@EventBusSubscriber(modid="commandcookies_electronic_mod",bus=EventBusSubscriber.Bus.MOD)
public final class PowerCableRegistration {
 public static EntityType<PowerCableEntity> TYPE;
 @SubscribeEvent public static void register(RegisterEvent e){if(e.getRegistryKey().equals(Registries.ENTITY_TYPE)){TYPE=EntityType.Builder.<PowerCableEntity>of(PowerCableEntity::new,MobCategory.MISC).sized(.05f,.05f).clientTrackingRange(12).updateInterval(20).fireImmune().build("commandcookies_electronic_mod:power_cable");e.register(Registries.ENTITY_TYPE,h->h.register(PowerRegistration.id("power_cable"),TYPE));}}
}
