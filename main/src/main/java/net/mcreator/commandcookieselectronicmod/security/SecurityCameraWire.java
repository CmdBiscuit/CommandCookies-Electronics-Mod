package net.mcreator.commandcookieselectronicmod.security;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import io.netty.buffer.Unpooled;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Private, bounded scene packets use Minecraft's own chunk and entity codecs. */
public final class SecurityCameraWire {
 public static final int RADIUS=3,MAX_PACKED=1_048_576,MAX_RAW=3_145_728;
 private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String name){return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod",name));}
 public record Start(UUID device,UUID session,long request,UUID camera,UUID token,String dimension,String dimensionType,BlockPos position,Direction facing,long time,boolean flat) implements CustomPacketPayload {
  public static final Type<Start> TYPE=SecurityCameraWire.type("camera_scene_start");
  public static final StreamCodec<RegistryFriendlyByteBuf,Start> CODEC=StreamCodec.of((b,v)->{b.writeUUID(v.device);b.writeUUID(v.session);b.writeLong(v.request);b.writeUUID(v.camera);b.writeUUID(v.token);b.writeUtf(v.dimension,256);b.writeUtf(v.dimensionType,256);b.writeBlockPos(v.position);b.writeEnum(v.facing);b.writeLong(v.time);b.writeBoolean(v.flat);},b->new Start(b.readUUID(),b.readUUID(),b.readLong(),b.readUUID(),b.readUUID(),b.readUtf(256),b.readUtf(256),b.readBlockPos(),b.readEnum(Direction.class),b.readLong(),b.readBoolean()));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record Chunk(UUID token,int x,int z,byte[] packed) implements CustomPacketPayload {
  public static final Type<Chunk> TYPE=SecurityCameraWire.type("camera_scene_chunk");
  public static final StreamCodec<RegistryFriendlyByteBuf,Chunk> CODEC=StreamCodec.of((b,v)->{b.writeUUID(v.token);b.writeInt(v.x);b.writeInt(v.z);b.writeByteArray(v.packed);},b->new Chunk(b.readUUID(),b.readInt(),b.readInt(),b.readByteArray(MAX_PACKED)));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record Actors(UUID token,long time,byte[] packed) implements CustomPacketPayload {
  public static final Type<Actors> TYPE=SecurityCameraWire.type("camera_scene_actors");
  public static final StreamCodec<RegistryFriendlyByteBuf,Actors> CODEC=StreamCodec.of((b,v)->{b.writeUUID(v.token);b.writeLong(v.time);b.writeByteArray(v.packed);},b->new Actors(b.readUUID(),b.readLong(),b.readByteArray(MAX_PACKED)));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record Stop(UUID token,String reason) implements CustomPacketPayload {
  public static final Type<Stop> TYPE=SecurityCameraWire.type("camera_scene_stop");
  public static final StreamCodec<RegistryFriendlyByteBuf,Stop> CODEC=StreamCodec.of((b,v)->{b.writeUUID(v.token);b.writeUtf(v.reason,128);},b->new Stop(b.readUUID(),b.readUtf(128)));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public static byte[] bytes(RegistryFriendlyByteBuf buffer){if(buffer.readableBytes()>MAX_RAW)throw new IllegalArgumentException("Camera scene too large");var bytes=new byte[buffer.readableBytes()];buffer.getBytes(buffer.readerIndex(),bytes);return bytes;}
 public static byte[] pack(byte[] raw){if(raw.length>MAX_RAW)throw new IllegalArgumentException("Camera scene too large");var deflater=new Deflater(Deflater.BEST_SPEED);try(var output=new ByteArrayOutputStream();var stream=new DeflaterOutputStream(output,deflater)){stream.write(raw);stream.finish();var result=output.toByteArray();if(result.length>MAX_PACKED)throw new IllegalArgumentException("Camera packet too large");return result;}catch(IOException e){throw new IllegalStateException(e);}finally{deflater.end();}}
 public static byte[] unpack(byte[] packed){if(packed.length>MAX_PACKED)throw new IllegalArgumentException("Camera packet too large");try(var input=new InflaterInputStream(new ByteArrayInputStream(packed));var output=new ByteArrayOutputStream()){var block=new byte[8192];int size;while((size=input.read(block))!=-1){if(output.size()+size>MAX_RAW)throw new IllegalArgumentException("Camera scene too large");output.write(block,0,size);}return output.toByteArray();}catch(IOException e){throw new IllegalArgumentException("Invalid camera scene",e);}}
 @EventBusSubscriber(modid="commandcookies_electronic_mod",bus=EventBusSubscriber.Bus.MOD)
 public static final class Registration {
  @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){var r=event.registrar("security2");r.playToClient(Start.TYPE,Start.CODEC,(p,c)->c.enqueueWork(()->Client.start(p)));r.playToClient(Chunk.TYPE,Chunk.CODEC,(p,c)->c.enqueueWork(()->Client.chunk(p)));r.playToClient(Actors.TYPE,Actors.CODEC,(p,c)->c.enqueueWork(()->Client.actors(p)));r.playToClient(Stop.TYPE,Stop.CODEC,(p,c)->c.enqueueWork(()->Client.stop(p)));}
 }
 private static final class Client {
  static void start(Start p){net.mcreator.commandcookieselectronicmod.client.RemoteCameraScene.start(p);}
  static void chunk(Chunk p){net.mcreator.commandcookieselectronicmod.client.RemoteCameraScene.chunk(p);}
  static void actors(Actors p){net.mcreator.commandcookieselectronicmod.client.RemoteCameraScene.actors(p);}
  static void stop(Stop p){net.mcreator.commandcookieselectronicmod.client.RemoteCameraScene.stop(p);}
 }
}
