package net.mcreator.commandcookieselectronicmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.mcreator.commandcookieselectronicmod.procedures.IphoneLogicProcedure;

public record PhoneFlashlightPayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<PhoneFlashlightPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod", "phone_flashlight"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PhoneFlashlightPayload> CODEC = StreamCodec.of((buffer, value) -> buffer.writeBoolean(value.enabled()), buffer -> new PhoneFlashlightPayload(buffer.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    @EventBusSubscriber(modid="commandcookies_electronic_mod", bus=EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToServer(TYPE, CODEC, (message, context) -> context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) IphoneLogicProcedure.setFlashlight(player, message.enabled());
            }));
        }
    }
}
