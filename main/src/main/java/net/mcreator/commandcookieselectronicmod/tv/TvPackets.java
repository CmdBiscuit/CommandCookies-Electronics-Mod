package net.mcreator.commandcookieselectronicmod.tv;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class TvPackets {
    private static final Map<ServerPlayer,Integer> LAST=new WeakHashMap<>();
    public record Edit(BlockPos pos,UUID identity,long request,String image,boolean fill,int yaw,int pitch,int roll) implements CustomPacketPayload {
        public Edit(BlockPos pos,UUID identity,long request,String image,boolean fill,int yaw,int pitch){this(pos,identity,request,image,fill,yaw,pitch,0);}
        public static final Type<Edit> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod","tv_edit"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Edit> CODEC=StreamCodec.of((b,v)->{b.writeBlockPos(v.pos);b.writeUUID(v.identity);b.writeLong(v.request);b.writeUtf(v.image,512);b.writeBoolean(v.fill);b.writeVarInt(v.yaw);b.writeVarInt(v.pitch);b.writeVarInt(v.roll);},b->new Edit(b.readBlockPos(),b.readUUID(),b.readLong(),b.readUtf(512),b.readBoolean(),b.readVarInt(),b.readVarInt(),b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Result(BlockPos pos,UUID identity,long request,boolean success,String message) implements CustomPacketPayload {
        public static final Type<Result> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod","tv_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Result> CODEC=StreamCodec.of((b,v)->{b.writeBlockPos(v.pos);b.writeUUID(v.identity);b.writeLong(v.request);b.writeBoolean(v.success);b.writeUtf(v.message,160);},b->new Result(b.readBlockPos(),b.readUUID(),b.readLong(),b.readBoolean(),b.readUtf(160)));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public static String apply(ServerPlayer player,Edit edit){
        var level=player.serverLevel();
        if(!player.isAlive()||player.isSpectator()||player.distanceToSqr(Vec3.atCenterOf(edit.pos))>64||!level.hasChunkAt(edit.pos))return "Move closer to the TV.";
        if(!(level.getBlockEntity(edit.pos) instanceof ImageTvEntity tv)||!tv.identity.equals(edit.identity))return "This TV is no longer available.";
        if(tv.owner!=null&&!tv.owner.equals(player.getUUID())&&!player.hasPermissions(2))return "Only the TV owner can change it.";
        String image;try{image=ImgurLink.normalize(edit.image);}catch(IllegalArgumentException error){return error.getMessage();}
        int yaw=tv.wall()?Mth.clamp(edit.yaw,-TvGeometry.MAX_ANGLE,TvGeometry.MAX_ANGLE):0,pitch=tv.wall()?Mth.clamp(edit.pitch,-TvGeometry.MAX_ANGLE,TvGeometry.MAX_ANGLE):0,roll=tv.wall()?Mth.clamp(edit.roll,-TvGeometry.MAX_ROLL,TvGeometry.MAX_ROLL):0;
        var next=TvGeometry.cells(tv.wall(),tv.facing(),yaw,pitch,roll);
        if(!TvParts.room(level,edit.pos,next))return "There is not enough room for that angle.";
        var old=tv.cells();tv.image=image;tv.fill=edit.fill;tv.yaw=yaw;tv.pitch=pitch;tv.roll=roll;if(tv.owner==null)tv.owner=player.getUUID();tv.changed();TvParts.rebuild(tv,old);net.mcreator.commandcookieselectronicmod.power.PowerCables.refreshPorts(level,edit.pos);return "";
    }
    private static void handle(ServerPlayer player,Edit edit){
        String error;
        if(player.tickCount-LAST.getOrDefault(player,Integer.MIN_VALUE/2)<4)error="Please wait a moment.";
        else{LAST.put(player,player.tickCount);error=apply(player,edit);}
        PacketDistributor.sendToPlayer(player,new Result(edit.pos,edit.identity,edit.request,error.isEmpty(),error));
    }
    @EventBusSubscriber(modid="commandcookies_electronic_mod",bus=EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){var r=event.registrar("image_tv2");r.playToServer(Edit.TYPE,Edit.CODEC,(m,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer player)handle(player,m);}));r.playToClient(Result.TYPE,Result.CODEC,(m,c)->c.enqueueWork(()->Client.result(m)));}
    }
    private static final class Client{static void result(Result result){var mc=net.minecraft.client.Minecraft.getInstance();if(mc.screen instanceof net.mcreator.commandcookieselectronicmod.client.ImageTvScreen screen)screen.response(result);}}
}
