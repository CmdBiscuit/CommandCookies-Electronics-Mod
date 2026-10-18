package net.mcreator.commandcookieselectronicmod.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.mcreator.commandcookieselectronicmod.phone.PhoneDeviceIdentity;
import net.mcreator.commandcookieselectronicmod.procedures.IphoneLogicProcedure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Contains just the phone ID and presentation; private notes/settings stay on the client. */
public record PhoneOpenResponse(boolean offhand,String color,UUID device) implements CustomPacketPayload {
    public static final Type<PhoneOpenResponse> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod","phone_open_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PhoneOpenResponse> CODEC=StreamCodec.of((b,v)->{b.writeBoolean(v.offhand());b.writeUtf(v.color(),40);b.writeUUID(v.device());},b->new PhoneOpenResponse(b.readBoolean(),b.readUtf(40),b.readUUID()));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    @EventBusSubscriber(modid="commandcookies_electronic_mod",bus=EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){event.registrar("1").playToClient(TYPE,CODEC,(message,context)->context.enqueueWork(()->Client.open(message)));}
    }
    private static final class Client {
        static void open(PhoneOpenResponse message){
            var g=net.minecraft.client.Minecraft.getInstance();if(g.player==null||g.level==null||g.screen!=null)return;
            var stack=g.player.getItemInHand(message.offhand()?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
            if(!message.color().equals(IphoneLogicProcedure.color(stack))||!message.device().equals(PhoneDeviceIdentity.id(stack)))return;
            g.setScreen(message.color().startsWith("samsung_")
                ?new net.mcreator.commandcookieselectronicmod.client.SamsungScreen(message.color().substring(8),message.device())
                :new net.mcreator.commandcookieselectronicmod.client.IphoneScreen(message.color(),message.device()));
        }
    }
}
