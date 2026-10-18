package net.mcreator.commandcookieselectronicmod.nes;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Also handles crouch-click, which vanilla normally passes through to held items. */
@EventBusSubscriber(modid="commandcookies_electronic_mod")
public final class NesInteractions {
    @SubscribeEvent public static void use(PlayerInteractEvent.RightClickBlock e){
        if(!(e.getLevel().getBlockState(e.getPos()).getBlock() instanceof NesDeviceBlock))return;
        if(e.getEntity().isShiftKeyDown()&&e.getItemStack().getItem() instanceof BlockItem)return;
        e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
        if(!e.getLevel().isClientSide)NesSystem.interact(e.getLevel(),e.getPos(),e.getEntity(),e.getHand());
    }
}
