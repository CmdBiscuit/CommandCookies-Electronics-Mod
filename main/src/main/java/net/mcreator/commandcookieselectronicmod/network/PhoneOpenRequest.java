package net.mcreator.commandcookieselectronicmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.mcreator.commandcookieselectronicmod.phone.PhoneDeviceIdentity;
import net.mcreator.commandcookieselectronicmod.procedures.IphoneLogicProcedure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** The server validates the held item and returns its identity before a GUI opens. */
public record PhoneOpenRequest(boolean offhand) implements CustomPacketPayload {
    public static final Type<PhoneOpenRequest> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod","phone_open_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PhoneOpenRequest> CODEC=StreamCodec.of((b,v)->b.writeBoolean(v.offhand()),b->new PhoneOpenRequest(b.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    @EventBusSubscriber(modid="commandcookies_electronic_mod",bus=EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){
            event.registrar("1").playToServer(TYPE,CODEC,(message,context)->context.enqueueWork(()->{
                if(!(context.player() instanceof ServerPlayer player)||!player.isAlive()||player.isSpectator())return;
                var hand=message.offhand()?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
                var stack=player.getItemInHand(hand);String color=IphoneLogicProcedure.color(stack);if(color==null)return;
                PhoneDeviceIdentity.ensureInventory(player);var id=PhoneDeviceIdentity.ensure(stack);
                player.containerMenu.broadcastChanges();
                PacketDistributor.sendToPlayer(player,new PhoneOpenResponse(message.offhand(),color,id));
            }));
        }
    }
}
