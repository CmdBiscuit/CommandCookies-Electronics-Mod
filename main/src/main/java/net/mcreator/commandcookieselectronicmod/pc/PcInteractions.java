package net.mcreator.commandcookieselectronicmod.pc;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
@EventBusSubscriber(modid="commandcookies_electronic_mod")
public final class PcInteractions {
    @SubscribeEvent public static void click(PlayerInteractEvent.RightClickBlock e){if(!(e.getLevel().getBlockState(e.getPos()).getBlock() instanceof PcDeviceBlock))return;if(MonitorAttachments.interact(e.getLevel(),e.getPos(),e.getEntity(),e.getHand())){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;}if(e.getEntity().isShiftKeyDown()&&e.getItemStack().getItem() instanceof BlockItem)return;e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);if(!e.getLevel().isClientSide)PcSystem.interact(e.getLevel(),e.getPos(),e.getEntity(),e.getHand());}
}
