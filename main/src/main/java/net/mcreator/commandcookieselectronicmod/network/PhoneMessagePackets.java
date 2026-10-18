package net.mcreator.commandcookieselectronicmod.network;

import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.mcreator.commandcookieselectronicmod.phone.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class PhoneMessagePackets {
    public static final int SYNC=0,SEND=1,READ=2;
    public record Contact(UUID player,String name){}
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String name){return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod",name));}
    private static void writeEntry(RegistryFriendlyByteBuf b,PhoneMessageData.Entry e){b.writeUUID(e.id());b.writeUUID(e.peer());b.writeUtf(e.name(),64);b.writeUtf(e.text(),256);b.writeLong(e.time());b.writeBoolean(e.outgoing());b.writeBoolean(e.read());}
    private static PhoneMessageData.Entry readEntry(RegistryFriendlyByteBuf b){return new PhoneMessageData.Entry(b.readUUID(),b.readUUID(),b.readUtf(64),b.readUtf(256),b.readLong(),b.readBoolean(),b.readBoolean());}
    private static int count(RegistryFriendlyByteBuf b){int n=b.readVarInt();if(n<0||n>128)throw new IllegalArgumentException("Phone list exceeds limit");return n;}
    public record Request(UUID device,long request,int action,UUID peer,String text) implements CustomPacketPayload {
        public static final Type<Request> TYPE=PhoneMessagePackets.type("phone_message_request");
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,v)->{b.writeUUID(v.device);b.writeLong(v.request);b.writeVarInt(v.action);b.writeUUID(v.peer);b.writeUtf(v.text,256);},b->new Request(b.readUUID(),b.readLong(),b.readVarInt(),b.readUUID(),b.readUtf(256)));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Snapshot(UUID device,long request,boolean success,String status,List<Contact> contacts,List<PhoneMessageData.Entry> messages) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE=PhoneMessagePackets.type("phone_message_snapshot");
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.of((b,v)->{b.writeUUID(v.device);b.writeLong(v.request);b.writeBoolean(v.success);b.writeUtf(v.status,128);b.writeVarInt(v.contacts.size());for(var c:v.contacts){b.writeUUID(c.player);b.writeUtf(c.name,64);}b.writeVarInt(v.messages.size());for(var e:v.messages)writeEntry(b,e);},b->{var device=b.readUUID();long request=b.readLong();boolean success=b.readBoolean();String status=b.readUtf(128);var contacts=new ArrayList<Contact>();for(int n=count(b);n>0;n--)contacts.add(new Contact(b.readUUID(),b.readUtf(64)));var messages=new ArrayList<PhoneMessageData.Entry>();for(int n=count(b);n>0;n--)messages.add(readEntry(b));return new Snapshot(device,request,success,status,List.copyOf(contacts),List.copyOf(messages));});
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Notice(UUID device,PhoneMessageData.Entry message) implements CustomPacketPayload {
        public static final Type<Notice> TYPE=PhoneMessagePackets.type("phone_message_notice");
        public static final StreamCodec<RegistryFriendlyByteBuf,Notice> CODEC=StreamCodec.of((b,v)->{b.writeUUID(v.device);writeEntry(b,v.message);},b->new Notice(b.readUUID(),readEntry(b)));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    @EventBusSubscriber(modid="commandcookies_electronic_mod",bus=EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){var r=event.registrar("phone_messages1");r.playToServer(Request.TYPE,Request.CODEC,(m,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer player)PhoneMessages.handle(player,m);}));r.playToClient(Snapshot.TYPE,Snapshot.CODEC,(m,c)->c.enqueueWork(()->Client.snapshot(m)));r.playToClient(Notice.TYPE,Notice.CODEC,(m,c)->c.enqueueWork(()->Client.notice(m)));}
    }
    private static final class Client {
        static void snapshot(Snapshot m){net.mcreator.commandcookieselectronicmod.client.PhoneMessagesClient.snapshot(m);}
        static void notice(Notice m){net.mcreator.commandcookieselectronicmod.client.PhoneMessagesClient.notice(m);}
    }
}
