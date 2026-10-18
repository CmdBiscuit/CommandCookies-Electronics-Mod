package net.mcreator.commandcookieselectronicmod.power;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.mcreator.commandcookieselectronicmod.block.PowerOutletBlock;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
@EventBusSubscriber(modid="commandcookies_electronic_mod")
public final class PowerEvents {
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void click(PlayerInteractEvent.RightClickBlock e){
        var l=e.getLevel();var pos=PowerNetwork.target(l,e.getPos());var stack=e.getItemStack();
        if(!stack.is(CommandcookiesElectronicModModItems.WRENCH.get())||!(l.getBlockEntity(pos) instanceof GeneratorEntity||l.getBlockState(pos).getBlock() instanceof PowerOutletBlock||PowerNetwork.device(l,pos)))return;
        e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
        if(e.getEntity() instanceof ServerPlayer p)PowerNetwork.wrench(p,pos,stack);
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post e){if(e.getLevel() instanceof ServerLevel l)PowerNetwork.tick(l);}
    @SubscribeEvent public static void leave(PlayerEvent.PlayerLoggedOutEvent e){PowerNetwork.leave(e.getEntity().getUUID());}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){PowerNetwork.clear();}
}
