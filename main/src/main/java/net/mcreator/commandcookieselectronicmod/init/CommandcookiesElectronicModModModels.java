/*
 *    Maintained source: register new content directly in this file.
 */
package net.mcreator.commandcookieselectronicmod.init;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.mcreator.commandcookieselectronicmod.client.model.ModelLaptop;

@EventBusSubscriber(Dist.CLIENT)
public class CommandcookiesElectronicModModModels {
	@SubscribeEvent
	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(ModelLaptop.LAYER_LOCATION, ModelLaptop::createBodyLayer);
	}
}