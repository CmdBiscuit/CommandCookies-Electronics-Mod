package net.mcreator.commandcookieselectronicmod.nes;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class NesPayloads {
    private static ResourceLocation id(String s){return ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod",s);}
    public record Action(BlockPos monitor,UUID session,int action,int value,int sequence,int generation) implements CustomPacketPayload {
        public static final Type<Action> TYPE=new Type<>(id("nes_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.of((b,v)->{b.writeBlockPos(v.monitor);b.writeUUID(v.session);b.writeVarInt(v.action);b.writeVarInt(v.value);b.writeVarInt(v.sequence);b.writeVarInt(v.generation);},b->new Action(b.readBlockPos(),b.readUUID(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record State(BlockPos monitor,UUID session,byte[] board,int[] counts,int piece,int next,int x,int y,int rotation,int score,int best,int lines,int level,int clearMask,int flags,int startLevel,int height,int ack,boolean open,boolean valid,String message) implements CustomPacketPayload {
        public static final Type<State> TYPE=new Type<>(id("nes_state"));
        public static State of(BlockPos p,UUID id,TetrisEngine e,int best,int ack,boolean open,boolean valid,String why){return new State(p,id,e.board.clone(),e.statistics.clone(),e.piece,e.next,e.x,e.y,e.rotation,e.score,best,e.lines,e.level,e.clearMask,(e.started?1:0)|(e.paused?2:0)|(e.over?4:0)|(e.won?8:0)|(e.bType?16:0)|(e.music<<5),e.startLevel,e.height,ack,open,valid,why);}
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of((b,v)->{
            b.writeBlockPos(v.monitor);b.writeUUID(v.session);b.writeByteArray(v.board);b.writeVarIntArray(v.counts);
            for(int n:new int[]{v.piece,v.next,v.x,v.y,v.rotation,v.score,v.best,v.lines,v.level,v.clearMask,v.flags,v.startLevel,v.height,v.ack})b.writeVarInt(n);
            b.writeBoolean(v.open);b.writeBoolean(v.valid);b.writeUtf(v.message,128);
        },b->new State(b.readBlockPos(),b.readUUID(),b.readByteArray(200),b.readVarIntArray(7),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readBoolean(),b.readUtf(128)));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
        public int music(){return (flags>>>5)&3;}
        public boolean won(){return (flags&8)!=0;}public boolean bType(){return (flags&16)!=0;}
        public boolean started(){return (flags&1)!=0;}public boolean paused(){return (flags&2)!=0;}public boolean over(){return (flags&4)!=0;}
    }
    @EventBusSubscriber(modid="commandcookies_electronic_mod",bus=EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e){
            var r=e.registrar("nes3");
            r.playToServer(Action.TYPE,Action.CODEC,(m,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer p)NesSystem.action(p,m.monitor,m.session,m.action,m.value,m.sequence,m.generation);}));
            r.playToClient(State.TYPE,State.CODEC,(m,c)->c.enqueueWork(()->Client.receive(m)));
        }
    }
    private static final class Client {
        static void receive(State m){
            var game=net.minecraft.client.Minecraft.getInstance();if(game.player==null||game.level==null||m.board.length!=200||m.counts.length!=7)return;
            if(!m.valid){if(game.screen instanceof net.mcreator.commandcookieselectronicmod.client.NesTetrisScreen s&&s.session.equals(m.session)){game.setScreen(null);if(!m.message.isEmpty())game.player.displayClientMessage(net.minecraft.network.chat.Component.literal(m.message),true);}return;}
            if(m.open){if(NesSystem.controller(game.player))game.setScreen(new net.mcreator.commandcookieselectronicmod.client.NesTetrisScreen(m));}
            else if(game.screen instanceof net.mcreator.commandcookieselectronicmod.client.NesTetrisScreen s&&s.session.equals(m.session))s.accept(m);
        }
    }
}
