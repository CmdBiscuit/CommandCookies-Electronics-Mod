package net.mcreator.commandcookieselectronicmod.procedures;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid="commandcookies_electronic_mod")
public final class IphoneLogicProcedure {
    private static final Set<UUID> ENABLED = new HashSet<>(), OWNED_EFFECT = new HashSet<>();
    public static String color(ItemStack stack) {
        if (stack.is(CommandcookiesElectronicModModItems.IPHONE_18_PRO_MAX.get())) return "burgundy";
        if (stack.is(CommandcookiesElectronicModModItems.IPHONE_18_PRO_MAX_BLACK.get())) return "black";
        if (stack.is(CommandcookiesElectronicModModItems.IPHONE_18_PRO_MAX_SILVER.get())) return "silver";
        if (stack.is(CommandcookiesElectronicModModItems.IPHONE_18_PRO_MAX_GLACIER.get())) return "glacier";
        return S26PhoneGuiProcedure.color(stack);
    }
    private static boolean phone(PlayerInteractEvent event) { return color(event.getItemStack()) != null; }
    private static boolean holding(ServerPlayer player) { return color(player.getMainHandItem()) != null || color(player.getOffhandItem()) != null; }
    public static void setFlashlight(ServerPlayer player, boolean enabled) {
        if (enabled && holding(player)) ENABLED.add(player.getUUID());
        else { ENABLED.remove(player.getUUID()); removeOurEffect(player); }
    }
    private static void removeOurEffect(ServerPlayer player) {
        if (!OWNED_EFFECT.remove(player.getUUID())) return;
        MobEffectInstance effect = player.getEffect(MobEffects.NIGHT_VISION);
        if (effect != null && effect.getAmplifier() == 0 && effect.getDuration() <= 240 && !effect.isInfiniteDuration())
            player.removeEffect(MobEffects.NIGHT_VISION);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if(player.tickCount%20==0)net.mcreator.commandcookieselectronicmod.phone.PhoneDeviceIdentity.ensureInventory(player);
        if (!ENABLED.contains(player.getUUID()) || !holding(player) || !player.isAlive()) { removeOurEffect(player); return; }
        if (player.tickCount % 20 != 0) return;
        MobEffectInstance effect = player.getEffect(MobEffects.NIGHT_VISION);
        boolean ours = OWNED_EFFECT.contains(player.getUUID());
        if (effect == null || (ours && effect.getAmplifier() == 0 && effect.getDuration() <= 240 && !effect.isInfiniteDuration())) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false, false));
            OWNED_EFFECT.add(player.getUUID());
        } else OWNED_EFFECT.remove(player.getUUID());
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        ENABLED.remove(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer player) removeOurEffect(player);
    }
    @SubscribeEvent public static void air(PlayerInteractEvent.RightClickItem event) {
        if (phone(event)) { event.setCancellationResult(InteractionResult.SUCCESS); event.setCanceled(true); }
    }
    @SubscribeEvent public static void block(PlayerInteractEvent.RightClickBlock event) {
        if (phone(event)) { event.setCancellationResult(InteractionResult.SUCCESS); event.setCanceled(true); }
    }
    @SubscribeEvent public static void entity(PlayerInteractEvent.EntityInteract event) {
        if (phone(event)) { event.setCancellationResult(InteractionResult.SUCCESS); event.setCanceled(true); }
    }
    @SubscribeEvent public static void entityPart(PlayerInteractEvent.EntityInteractSpecific event) {
        if (phone(event)) { event.setCancellationResult(InteractionResult.SUCCESS); event.setCanceled(true); }
    }
    @EventBusSubscriber(modid="commandcookies_electronic_mod", value=Dist.CLIENT)
    public static final class Client {
        public static boolean flashlight;
        private static long lastRequest;
        private static void open(PlayerInteractEvent event) {
            var game=net.minecraft.client.Minecraft.getInstance();long now=System.nanoTime();
            if(phone(event)&&event.getLevel().isClientSide()&&event.getEntity()==game.player&&game.screen==null&&now-lastRequest>300_000_000L){
                lastRequest=now;
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new net.mcreator.commandcookieselectronicmod.network.PhoneOpenRequest(event.getHand()==net.minecraft.world.InteractionHand.OFF_HAND));
            }
        }
        @SubscribeEvent public static void disconnected(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) { flashlight = false; lastRequest=0; }
        @SubscribeEvent(priority=EventPriority.LOWEST, receiveCanceled=true) public static void air(PlayerInteractEvent.RightClickItem event) { open(event); }
        @SubscribeEvent(priority=EventPriority.LOWEST, receiveCanceled=true) public static void block(PlayerInteractEvent.RightClickBlock event) { open(event); }
        @SubscribeEvent(priority=EventPriority.LOWEST, receiveCanceled=true) public static void entity(PlayerInteractEvent.EntityInteract event) { open(event); }
        @SubscribeEvent(priority=EventPriority.LOWEST, receiveCanceled=true) public static void entityPart(PlayerInteractEvent.EntityInteractSpecific event) { open(event); }
    }
}
