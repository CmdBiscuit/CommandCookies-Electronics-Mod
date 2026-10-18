package net.mcreator.commandcookieselectronicmod.pc;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public record PcOpenResponse(BlockPos monitor,UUID device,UUID session) implements CustomPacketPayload {
    public static final Type<PcOpenResponse> TYPE=new Type<>(PcRegistration.id("pc_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PcOpenResponse> CODEC=StreamCodec.of((b,v)->{b.writeBlockPos(v.monitor);b.writeUUID(v.device);b.writeUUID(v.session);},b->new PcOpenResponse(b.readBlockPos(),b.readUUID(),b.readUUID()));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    @EventBusSubscriber(modid="commandcookies_electronic_mod",bus=EventBusSubscriber.Bus.MOD)
    public static final class Registration {@SubscribeEvent public static void register(RegisterPayloadHandlersEvent e){e.registrar("pc1").playToClient(TYPE,CODEC,(m,c)->c.enqueueWork(()->Client.open(m)));}}
    private static final class Client {static void open(PcOpenResponse m){net.mcreator.commandcookieselectronicmod.client.PcOpenQueue.accept(m);}}
}
