package net.mcreator.commandcookieselectronicmod.network;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** The server confirms the physical laptop; private app files never leave the client. */
public record LaptopOpenResponse(BlockPos position,UUID device,UUID session) implements CustomPacketPayload {
    public static final Type<LaptopOpenResponse> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod","laptop_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf,LaptopOpenResponse> CODEC=StreamCodec.of(
        (b,v)->{b.writeBlockPos(v.position());b.writeUUID(v.device());b.writeUUID(v.session());},
        b->new LaptopOpenResponse(b.readBlockPos(),b.readUUID(),b.readUUID()));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    @EventBusSubscriber(modid="commandcookies_electronic_mod",bus=EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){
            event.registrar("1").playToClient(TYPE,CODEC,(message,context)->context.enqueueWork(()->Client.open(message)));
        }
    }
    private static final class Client {
        static void open(LaptopOpenResponse message){
            var g=net.minecraft.client.Minecraft.getInstance();
            if(g.player==null||g.level==null||g.screen!=null||g.player.isShiftKeyDown()
                ||g.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(message.position()))>64
                ||!(g.level.getBlockEntity(message.position()) instanceof net.mcreator.commandcookieselectronicmod.block.entity.LaptopBlockEntity laptop)
                ||!laptop.getBlockState().getValue(net.mcreator.commandcookieselectronicmod.block.LaptopBlock.OPEN)
                ||!message.device().equals(laptop.deviceId)||!message.session().equals(laptop.desktopSessionId))return;
            g.setScreen(new net.mcreator.commandcookieselectronicmod.client.LaptopDesktopScreen(message.position()));
        }
    }
}
