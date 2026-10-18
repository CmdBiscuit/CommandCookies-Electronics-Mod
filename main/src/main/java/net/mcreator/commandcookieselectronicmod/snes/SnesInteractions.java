package net.mcreator.commandcookieselectronicmod.snes;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid="commandcookies_electronic_mod")
public final class SnesInteractions {
    @SubscribeEvent public static void use(PlayerInteractEvent.RightClickBlock e){
        if(!(e.getLevel().getBlockEntity(e.getPos()) instanceof SnesDeviceBlockEntity))return;
        if(e.getEntity().isShiftKeyDown()&&e.getItemStack().getItem() instanceof BlockItem)return;
        e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
        if(!e.getLevel().isClientSide)SnesSystem.interactConsole(e.getLevel(),e.getPos(),e.getEntity(),e.getHand());
    }
}
