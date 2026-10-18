package net.mcreator.commandcookieselectronicmod.client;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.mcreator.commandcookieselectronicmod.power.PowerPayloads;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
@EventBusSubscriber(modid="commandcookies_electronic_mod",value=Dist.CLIENT)
public final class PowerHighlight {
    private static PowerPayloads.Highlight selected;private static long until,acceptedAt;
    private static ClientLevel owner;private static Snapshot source;
    private static final Map<BlockPos,Snapshot> targets=new HashMap<>();
    // A new block entity at the same coordinate is a different physical device.
    // Compare the instance rather than its UUID, which may still be awaiting NBT.
    private record Snapshot(Block block,BlockEntity entity){
        static Snapshot at(ClientLevel l,BlockPos p){return !l.hasChunkAt(p)||l.getBlockState(p).isAir()?null:new Snapshot(l.getBlockState(p).getBlock(),l.getBlockEntity(p));}
        boolean matches(ClientLevel l,BlockPos p){return l.hasChunkAt(p)&&l.getBlockState(p).is(block)&&l.getBlockEntity(p)==entity;}
    }
    private static boolean tool(net.minecraft.world.item.ItemStack s){return s.is(net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems.AV_CABLE.get())||s.is(net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems.WRENCH.get())||s.is(net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems.HDMI_CABLE.get());}
    private static void clear(){selected=null;until=0;owner=null;source=null;targets.clear();}
    public static void accept(PowerPayloads.Highlight m){
        clear();var g=Minecraft.getInstance();
        if(g.level==null||m.ticks()<=0||!m.dimension().equals(g.level.dimension().location().toString()))return;
        var captured=Snapshot.at(g.level,m.source());if(captured==null)return;
        owner=g.level;source=captured;acceptedAt=owner.getGameTime();until=acceptedAt+Math.clamp(m.ticks(),0,300);
        var positions=new ArrayList<BlockPos>();for(var p:m.targets()){if(positions.size()==64)break;var s=Snapshot.at(owner,p);if(s!=null&&!targets.containsKey(p)){targets.put(p.immutable(),s);positions.add(p.immutable());}}
        selected=new PowerPayloads.Highlight(m.dimension(),m.source().immutable(),List.copyOf(positions),m.ticks());
    }
    public static BlockPos source(){return selected==null?null:selected.source();}
    public static void removed(BlockPos p){
        if(selected==null)return;if(p.equals(selected.source())){clear();return;}
        if(targets.remove(p)!=null)trimTargets();
    }
    private static void trimTargets(){selected=new PowerPayloads.Highlight(selected.dimension(),selected.source(),selected.targets().stream().filter(targets::containsKey).toList(),selected.ticks());}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){clear();}
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        if(selected==null)return;var g=Minecraft.getInstance();
        if(g.level!=owner||g.player==null||until<=owner.getGameTime()||!source.matches(owner,selected.source())){clear();return;}
        if(owner.getGameTime()>acceptedAt+2&&(g.screen!=null||!tool(g.player.getMainHandItem())&&!tool(g.player.getOffhandItem()))){clear();return;}
        // Check only the existing capped selection; no area scans or chunk loads.
        if(targets.entrySet().removeIf(v->!v.getValue().matches(owner,v.getKey())))trimTargets();
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)return;var g=Minecraft.getInstance();
        if(g.player==null||g.level!=owner||selected==null||until<=owner.getGameTime())return;
        draw(e,selected.source(),1,.7f,.15f);for(var pos:selected.targets())draw(e,pos,.2f,.95f,.85f);
    }
    private static void draw(RenderLevelStageEvent e,BlockPos p,float r,float green,float b){
        var g=Minecraft.getInstance();if(!g.level.hasChunkAt(p)||g.player.distanceToSqr(p.getX()+.5,p.getY()+.5,p.getZ()+.5)>(net.mcreator.commandcookieselectronicmod.power.PowerNetwork.GENERATOR_RANGE+8)*(net.mcreator.commandcookieselectronicmod.power.PowerNetwork.GENERATOR_RANGE+8))return;
        var shape=g.level.getBlockState(p).getShape(g.level,p);if(shape.isEmpty())return;
        // One coarse outline per target keeps detailed models inexpensive.
        var camera=e.getCamera().getPosition();var pose=e.getPoseStack();pose.pushPose();pose.translate(p.getX()-camera.x,p.getY()-camera.y,p.getZ()-camera.z);
        LevelRenderer.renderLineBox(pose,g.renderBuffers().bufferSource().getBuffer(RenderType.lines()),shape.bounds().inflate(.003),r,green,b,1);pose.popPose();
    }
}
