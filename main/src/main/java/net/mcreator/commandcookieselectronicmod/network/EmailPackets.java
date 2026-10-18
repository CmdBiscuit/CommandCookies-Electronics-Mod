package net.mcreator.commandcookieselectronicmod.network;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.mcreator.commandcookieselectronicmod.email.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class EmailPackets {
    public static final int SYNC=0,CREATE=1,SEND=2,READ=3,DELETE=4,PHOTO=5;
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String s){return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod",s));}
    private static void entry(RegistryFriendlyByteBuf b,EmailData.Entry e){b.writeUUID(e.id());b.writeUtf(e.from(),42);b.writeUtf(e.to(),42);b.writeUtf(e.subject(),80);b.writeUtf(e.body(),2048);b.writeLong(e.time());b.writeBoolean(e.outgoing());b.writeBoolean(e.read());b.writeVarInt(e.photoWidth());b.writeVarInt(e.photoHeight());}
    private static EmailData.Entry entry(RegistryFriendlyByteBuf b){return new EmailData.Entry(b.readUUID(),b.readUtf(42),b.readUtf(42),b.readUtf(80),b.readUtf(2048),b.readLong(),b.readBoolean(),b.readBoolean(),dimension(b),dimension(b));}
    private static int dimension(RegistryFriendlyByteBuf b){int n=b.readVarInt();if(n<0||n>EmailPhoto.MAX_SIDE)throw new IllegalArgumentException("Photo dimensions exceed limit");return n;}
    public record Request(BlockPos position,UUID device,UUID session,long request,int action,String address,String subject,String body,UUID message,boolean outgoing,byte[] photo) implements CustomPacketPayload {
        public Request{photo=photo.clone();if(photo.length>EmailPhoto.MAX_BYTES)throw new IllegalArgumentException("Photo exceeds limit");}
        public Request(BlockPos position,UUID device,UUID session,long request,int action,String address,String subject,String body,UUID message,boolean outgoing){this(position,device,session,request,action,address,subject,body,message,outgoing,new byte[0]);}
        @Override public byte[] photo(){return photo.clone();}
        @Override public boolean equals(Object o){return o instanceof Request r&&position.equals(r.position)&&device.equals(r.device)&&session.equals(r.session)&&request==r.request&&action==r.action&&address.equals(r.address)&&subject.equals(r.subject)&&body.equals(r.body)&&message.equals(r.message)&&outgoing==r.outgoing&&Arrays.equals(photo,r.photo);}
        @Override public int hashCode(){return 31*Objects.hash(position,device,session,request,action,address,subject,body,message,outgoing)+Arrays.hashCode(photo);}
        public static final Type<Request> TYPE=EmailPackets.type("email_request");
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,v)->{b.writeBlockPos(v.position);b.writeUUID(v.device);b.writeUUID(v.session);b.writeLong(v.request);b.writeVarInt(v.action);b.writeUtf(v.address,42);b.writeUtf(v.subject,80);b.writeUtf(v.body,2048);b.writeUUID(v.message);b.writeBoolean(v.outgoing);b.writeByteArray(v.photo);},b->new Request(b.readBlockPos(),b.readUUID(),b.readUUID(),b.readLong(),b.readVarInt(),b.readUtf(42),b.readUtf(80),b.readUtf(2048),b.readUUID(),b.readBoolean(),b.readByteArray(EmailPhoto.MAX_BYTES)));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Snapshot(BlockPos position,UUID device,UUID session,long request,boolean success,String status,String address,List<EmailData.Entry> messages) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE=EmailPackets.type("email_snapshot");
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.of((b,v)->{b.writeBlockPos(v.position);b.writeUUID(v.device);b.writeUUID(v.session);b.writeLong(v.request);b.writeBoolean(v.success);b.writeUtf(v.status,128);b.writeUtf(v.address,42);b.writeVarInt(v.messages.size());for(var e:v.messages)entry(b,e);},b->{var pos=b.readBlockPos();var device=b.readUUID();var session=b.readUUID();long request=b.readLong();boolean ok=b.readBoolean();String status=b.readUtf(128),address=b.readUtf(42);int n=b.readVarInt();if(n<0||n>EmailData.HISTORY)throw new IllegalArgumentException("Email history exceeds limit");var entries=new ArrayList<EmailData.Entry>();for(int i=0;i<n;i++)entries.add(entry(b));return new Snapshot(pos,device,session,request,ok,status,address,List.copyOf(entries));});
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Notice(String from,String subject,int unread) implements CustomPacketPayload {
        public static final Type<Notice> TYPE=EmailPackets.type("email_notice");
        public static final StreamCodec<RegistryFriendlyByteBuf,Notice> CODEC=StreamCodec.of((b,v)->{b.writeUtf(v.from,42);b.writeUtf(v.subject,80);b.writeVarInt(v.unread);},b->new Notice(b.readUtf(42),b.readUtf(80),Math.max(0,Math.min(EmailData.HISTORY,b.readVarInt()))));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Photo(BlockPos position,UUID device,UUID session,long request,UUID message,String status,byte[] bytes) implements CustomPacketPayload {
        public Photo{bytes=bytes.clone();if(bytes.length>EmailPhoto.MAX_BYTES)throw new IllegalArgumentException("Photo exceeds limit");}
        @Override public byte[] bytes(){return bytes.clone();}
        public static final Type<Photo> TYPE=EmailPackets.type("email_photo");
        public static final StreamCodec<RegistryFriendlyByteBuf,Photo> CODEC=StreamCodec.of((b,v)->{b.writeBlockPos(v.position);b.writeUUID(v.device);b.writeUUID(v.session);b.writeLong(v.request);b.writeUUID(v.message);b.writeUtf(v.status,128);b.writeByteArray(v.bytes);},b->new Photo(b.readBlockPos(),b.readUUID(),b.readUUID(),b.readLong(),b.readUUID(),b.readUtf(128),b.readByteArray(EmailPhoto.MAX_BYTES)));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    @EventBusSubscriber(modid="commandcookies_electronic_mod",bus=EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e){var r=e.registrar("email2");r.playToServer(Request.TYPE,Request.CODEC,(m,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer p)EmailService.handle(p,m);}));r.playToClient(Snapshot.TYPE,Snapshot.CODEC,(m,c)->c.enqueueWork(()->Client.snapshot(m)));r.playToClient(Photo.TYPE,Photo.CODEC,(m,c)->c.enqueueWork(()->Client.photo(m)));r.playToClient(Notice.TYPE,Notice.CODEC,(m,c)->c.enqueueWork(()->Client.notice(m)));}
    }
    private static final class Client {
        static void snapshot(Snapshot m){net.mcreator.commandcookieselectronicmod.client.EmailClient.snapshot(m);}
        static void photo(Photo m){net.mcreator.commandcookieselectronicmod.client.EmailClient.photo(m);}
        static void notice(Notice m){net.mcreator.commandcookieselectronicmod.client.EmailClient.notice(m);}
    }
}
