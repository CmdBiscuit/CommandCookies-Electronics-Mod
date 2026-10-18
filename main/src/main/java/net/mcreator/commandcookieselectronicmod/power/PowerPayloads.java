package net.mcreator.commandcookieselectronicmod.power;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class PowerPayloads {
    public record Highlight(String dimension,BlockPos source,List<BlockPos> targets,int ticks) implements CustomPacketPayload {
        public static final Type<Highlight> TYPE=new Type<>(PowerRegistration.id("power_highlights"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Highlight> CODEC=StreamCodec.of((b,v)->{b.writeUtf(v.dimension,128);b.writeBlockPos(v.source);b.writeVarInt(v.targets.size());for(var p:v.targets)b.writeBlockPos(p);b.writeVarInt(v.ticks);},b->{String dim=b.readUtf(128);var source=b.readBlockPos();int n=b.readVarInt();if(n<0||n>64)throw new IllegalArgumentException("Too many highlights");var positions=new ArrayList<BlockPos>();for(int i=0;i<n;i++)positions.add(b.readBlockPos());return new Highlight(dim,source,List.copyOf(positions),b.readVarInt());});
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Removed(BlockPos position) implements CustomPacketPayload {
        public static final Type<Removed> TYPE=new Type<>(PowerRegistration.id("highlight_block_removed"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Removed> CODEC=StreamCodec.of((b,v)->b.writeBlockPos(v.position),b->new Removed(b.readBlockPos()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Lost(BlockPos position,UUID device) implements CustomPacketPayload {
        public static final Type<Lost> TYPE=new Type<>(PowerRegistration.id("power_lost"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Lost> CODEC=StreamCodec.of((b,v)->{b.writeBlockPos(v.position);b.writeUUID(v.device);},b->new Lost(b.readBlockPos(),b.readUUID()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    @EventBusSubscriber(modid="commandcookies_electronic_mod",bus=EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e){var r=e.registrar("power1");r.playToClient(Highlight.TYPE,Highlight.CODEC,(m,c)->c.enqueueWork(()->Client.highlight(m)));r.playToClient(Removed.TYPE,Removed.CODEC,(m,c)->c.enqueueWork(()->Client.blockRemoved(m.position())));r.playToClient(Lost.TYPE,Lost.CODEC,(m,c)->c.enqueueWork(()->Client.lost(m)));}
    }
    public static void blockRemoved(BlockPos p){Client.blockRemoved(p);}
    private static final class Client {
        static void blockRemoved(BlockPos p){net.mcreator.commandcookieselectronicmod.client.PowerHighlight.removed(p);}
        static void highlight(Highlight m){net.mcreator.commandcookieselectronicmod.client.PowerHighlight.accept(m);}
        // Keep the legacy packet compatible; portable laptops ignore outlet power loss.
        static void lost(Lost m){}
    }
}
