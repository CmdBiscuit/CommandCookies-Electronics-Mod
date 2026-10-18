package net.mcreator.commandcookiesretroexpansion.retro;
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

@EventBusSubscriber(modid=RetroItems.MOD,bus=EventBusSubscriber.Bus.MOD)
public final class RetroPackets {
 private static ResourceLocation id(String s){return ResourceLocation.fromNamespaceAndPath(RetroItems.MOD,s);}
 public record Configure(boolean offhand,String hash,String title) implements CustomPacketPayload {
  public static final Type<Configure> TYPE=new Type<>(id("configure"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Configure> CODEC=StreamCodec.of((b,v)->{b.writeBoolean(v.offhand);b.writeUtf(v.hash,64);b.writeUtf(v.title,48);},b->new Configure(b.readBoolean(),b.readUtf(64),b.readUtf(48)));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record Open(BlockPos monitor,UUID session,UUID world,UUID console,String hash,String title,RetroSystem system) implements CustomPacketPayload {
  public static final Type<Open> TYPE=new Type<>(id("open"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=StreamCodec.of((b,v)->{b.writeBlockPos(v.monitor);b.writeUUID(v.session);b.writeUUID(v.world);b.writeUUID(v.console);b.writeUtf(v.hash,64);b.writeUtf(v.title,48);b.writeUtf(v.system.id,8);},b->new Open(b.readBlockPos(),b.readUUID(),b.readUUID(),b.readUUID(),b.readUtf(64),b.readUtf(48),RetroSystem.fromId(b.readUtf(8))));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record Pulse(UUID session,boolean close) implements CustomPacketPayload {
  public static final Type<Pulse> TYPE=new Type<>(id("pulse"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Pulse> CODEC=StreamCodec.of((b,v)->{b.writeUUID(v.session);b.writeBoolean(v.close);},b->new Pulse(b.readUUID(),b.readBoolean()));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record Close(UUID session,String reason) implements CustomPacketPayload {
  public static final Type<Close> TYPE=new Type<>(id("close"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Close> CODEC=StreamCodec.of((b,v)->{b.writeUUID(v.session);b.writeUtf(v.reason,128);},b->new Close(b.readUUID(),b.readUtf(128)));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e){var r=e.registrar("retro2");
  r.playToServer(Configure.TYPE,Configure.CODEC,(m,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer p)RetroSessions.configure(p,m);}));
  r.playToServer(Pulse.TYPE,Pulse.CODEC,(m,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer p)RetroSessions.pulse(p,m);}));
  r.playToClient(Open.TYPE,Open.CODEC,(m,c)->c.enqueueWork(()->Client.open(m)));
  r.playToClient(Close.TYPE,Close.CODEC,(m,c)->c.enqueueWork(()->Client.close(m)));
 }
 private static final class Client {
  static void open(Open m){var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player!=null&&mc.level!=null)mc.setScreen(new net.mcreator.commandcookiesretroexpansion.retro.client.EmulatorScreen(m));}
  static void close(Close m){var mc=net.minecraft.client.Minecraft.getInstance();if(mc.screen instanceof net.mcreator.commandcookiesretroexpansion.retro.client.EmulatorScreen s&&s.session.session().equals(m.session)){mc.setScreen(null);if(mc.player!=null&&!m.reason.isEmpty())mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(m.reason),true);}}
 }
}
