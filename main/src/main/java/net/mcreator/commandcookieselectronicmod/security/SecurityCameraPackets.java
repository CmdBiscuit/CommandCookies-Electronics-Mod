package net.mcreator.commandcookieselectronicmod.security;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class SecurityCameraPackets {
 public static final int SYNC=0,RENAME=1,VIEW=2,STOP=3,MAX_CAMERAS=128;
 public record Feed(BlockPos position,UUID identity,String name,Direction facing,boolean powered,String dimension){public Feed(BlockPos p,UUID id,String name,Direction face,boolean on){this(p,id,name,face,on,"minecraft:overworld");}}
 private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String name){return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod",name));}
 public record Request(boolean phone,BlockPos source,UUID device,UUID session,long request,int action,BlockPos camera,UUID identity,String name) implements CustomPacketPayload {
  public static final Type<Request> TYPE=SecurityCameraPackets.type("security_request");
  public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,v)->{b.writeBoolean(v.phone);b.writeBlockPos(v.source);b.writeUUID(v.device);b.writeUUID(v.session);b.writeLong(v.request);b.writeVarInt(v.action);b.writeBlockPos(v.camera);b.writeUUID(v.identity);b.writeUtf(v.name,32);},b->new Request(b.readBoolean(),b.readBlockPos(),b.readUUID(),b.readUUID(),b.readLong(),b.readVarInt(),b.readBlockPos(),b.readUUID(),b.readUtf(32)));
  @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record Snapshot(boolean phone,BlockPos source,UUID device,UUID session,long request,boolean success,String status,List<Feed> cameras) implements CustomPacketPayload {
  public Snapshot{cameras=List.copyOf(cameras);if(cameras.size()>MAX_CAMERAS)throw new IllegalArgumentException("Too many cameras");}
  public static final Type<Snapshot> TYPE=SecurityCameraPackets.type("security_snapshot");
  public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.of((b,v)->{b.writeBoolean(v.phone);b.writeBlockPos(v.source);b.writeUUID(v.device);b.writeUUID(v.session);b.writeLong(v.request);b.writeBoolean(v.success);b.writeUtf(v.status,128);b.writeVarInt(v.cameras.size());for(var camera:v.cameras){b.writeBlockPos(camera.position);b.writeUUID(camera.identity);b.writeUtf(camera.name,32);b.writeEnum(camera.facing);b.writeBoolean(camera.powered);b.writeUtf(camera.dimension,256);}},b->{boolean phone=b.readBoolean();var source=b.readBlockPos();var device=b.readUUID();var session=b.readUUID();long request=b.readLong();boolean success=b.readBoolean();String status=b.readUtf(128);int count=b.readVarInt();if(count<0||count>MAX_CAMERAS)throw new IllegalArgumentException("Too many cameras");var cameras=new ArrayList<Feed>();for(int i=0;i<count;i++){var p=b.readBlockPos();var id=b.readUUID();var name=b.readUtf(32);var facing=b.readEnum(Direction.class);if(!facing.getAxis().isHorizontal())throw new IllegalArgumentException("Invalid camera facing");cameras.add(new Feed(p,id,name,facing,b.readBoolean(),b.readUtf(256)));}return new Snapshot(phone,source,device,session,request,success,status,cameras);});
  @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 @EventBusSubscriber(modid="commandcookies_electronic_mod",bus=EventBusSubscriber.Bus.MOD)
 public static final class Registration {
  @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){var r=event.registrar("security2");r.playToServer(Request.TYPE,Request.CODEC,(m,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer p)SecurityCameraService.handle(p,m);}));r.playToClient(Snapshot.TYPE,Snapshot.CODEC,(m,c)->c.enqueueWork(()->Client.snapshot(m)));}
 }
 private static final class Client {static void snapshot(Snapshot m){var mc=net.minecraft.client.Minecraft.getInstance();if(mc.screen instanceof net.mcreator.commandcookieselectronicmod.client.SecurityCameraScreen screen)screen.response(m);}}
}
