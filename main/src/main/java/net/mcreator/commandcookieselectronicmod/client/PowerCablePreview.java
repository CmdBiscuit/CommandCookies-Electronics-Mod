package net.mcreator.commandcookieselectronicmod.client;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems;
import net.mcreator.commandcookieselectronicmod.power.*;
import net.mcreator.commandcookieselectronicmod.nes.*;
import net.mcreator.commandcookieselectronicmod.pc.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
@EventBusSubscriber(modid="commandcookies_electronic_mod",value=Dist.CLIENT)
public final class PowerCablePreview {
 private static int tool(ItemStack s){return s.is(CommandcookiesElectronicModModItems.WRENCH.get())?0:s.is(CommandcookiesElectronicModModItems.AV_CABLE.get())?1:s.is(CommandcookiesElectronicModModItems.HDMI_CABLE.get())?2:-1;}
 @SubscribeEvent public static void render(RenderLevelStageEvent e){
  if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;var g=Minecraft.getInstance();if(g.level==null||g.player==null||g.screen!=null)return;
  int tool=tool(g.player.getMainHandItem());if(tool<0)tool=tool(g.player.getOffhandItem());if(tool<0)return;
  var source=PowerHighlight.source();if(source==null||!g.level.hasChunkAt(source))return;var first=g.level.getBlockEntity(source);
  if(tool==0){wrench(e,source);return;}
  int mode,range,slot=0;
  if(tool==1&&(first instanceof NesDeviceBlockEntity||first instanceof net.mcreator.commandcookieselectronicmod.snes.SnesDeviceBlockEntity||first instanceof net.mcreator.commandcookieselectronicmod.n64.N64DeviceBlockEntity)){mode=PowerCables.AV;range=NesSystem.MAX_CABLE_DISTANCE;}
  else if(tool==2&&first instanceof PcDeviceEntity){mode=PowerCables.HDMI;range=PcSystem.RANGE;}else return;
  var hit=g.hitResult;Vec3 aim=hit!=null?hit.getLocation():g.player.getEyePosition().add(g.player.getLookAngle().scale(5));
  boolean valid=false,compatible=false;CablePorts.Port b=null;
  if(hit instanceof BlockHitResult block){var target=block.getBlockPos();var be=g.level.getBlockEntity(target);
   if(mode==PowerCables.AV&&first instanceof NesDeviceBlockEntity a&&be instanceof NesDeviceBlockEntity other){compatible=a.monitor()!=other.monitor();valid=compatible&&(a.peer==null&&other.peer==null||target.equals(a.peer)&&source.equals(other.peer));}
   else if(mode==PowerCables.AV&&first instanceof net.mcreator.commandcookieselectronicmod.snes.SnesDeviceBlockEntity a&&be instanceof NesDeviceBlockEntity other&&other.monitor()){compatible=true;valid=a.peer==null&&other.peer==null||target.equals(a.peer)&&source.equals(other.peer);}
   else if(mode==PowerCables.AV&&first instanceof NesDeviceBlockEntity a&&a.monitor()&&be instanceof net.mcreator.commandcookieselectronicmod.snes.SnesDeviceBlockEntity other){compatible=true;valid=a.peer==null&&other.peer==null||target.equals(a.peer)&&source.equals(other.peer);}
   else if(mode==PowerCables.AV&&first instanceof net.mcreator.commandcookieselectronicmod.n64.N64DeviceBlockEntity a&&be instanceof NesDeviceBlockEntity other&&other.monitor()){compatible=true;valid=a.peer==null&&other.peer==null||target.equals(a.peer)&&source.equals(other.peer);}
   else if(mode==PowerCables.AV&&first instanceof NesDeviceBlockEntity a&&a.monitor()&&be instanceof net.mcreator.commandcookieselectronicmod.n64.N64DeviceBlockEntity other){compatible=true;valid=a.peer==null&&other.peer==null||target.equals(a.peer)&&source.equals(other.peer);}
   else if(mode==PowerCables.HDMI&&first instanceof PcDeviceEntity a&&be instanceof PcDeviceEntity other){compatible=a.monitor()!=other.monitor();valid=compatible&&(a.peer==null&&other.peer==null||target.equals(a.peer)&&source.equals(other.peer));}
   if(compatible){b=CablePorts.at(target,CablePorts.connectionSpec(g.level,target,0,mode,true,false));if(mode==PowerCables.FEED)valid=true;valid=valid&&!source.equals(target)&&source.distSqr(target)<=range*range;}
  }
  var a=CablePorts.at(source,CablePorts.connectionSpec(g.level,source,slot,mode,false,false));
  if(b==null)b=new CablePorts.Port(aim,a.point().subtract(aim).normalize(),a.kind(),0);
  PowerCableRenderer.preview(a,b,e.getPoseStack(),g.renderBuffers().bufferSource(),e.getCamera().getPosition(),valid);
 }
 private static void wrench(RenderLevelStageEvent e,BlockPos source){
  var g=Minecraft.getInstance();var l=g.level;var first=l.getBlockEntity(source);
  if(!(first instanceof GeneratorEntity)&&!(first instanceof OutletEntity)&&!PowerNetwork.device(l,source))return;
  var hit=g.hitResult;Vec3 aim=hit!=null?hit.getLocation():g.player.getEyePosition().add(g.player.getLookAngle().scale(5));
  var target=hit instanceof BlockHitResult block?PowerNetwork.target(l,block.getBlockPos()):null;
  if(target!=null&&l.getBlockEntity(target) instanceof PcDeviceEntity input&&PcInputs.peripheral(input))return;
  int range=target==null?0:PowerNetwork.connectionRange(l,source,target);
  int mode,slot=0;boolean valid=false;CablePorts.Port a,b;
  if(range==PowerNetwork.GENERATOR_RANGE){
   var generator=first instanceof GeneratorEntity?source:target;var outlet=first instanceof OutletEntity?source:target;
   mode=PowerCables.FEED;a=CablePorts.at(generator,CablePorts.connectionSpec(l,generator,0,mode,false,false));b=CablePorts.at(outlet,CablePorts.connectionSpec(l,outlet,0,mode,true,false));
   valid=source.distSqr(target)<=range*range;
  }else if(range==PowerNetwork.OUTLET_RANGE){
   var outletPos=first instanceof OutletEntity?source:target;var device=first instanceof OutletEntity?target:source;var outlet=(OutletEntity)l.getBlockEntity(outletPos);
   mode=outlet.coax()?PowerCables.COAX:PowerCables.POWER;slot=socket(outlet,device);
   valid=slot>=0&&source.distSqr(target)<=range*range;
   a=CablePorts.at(outletPos,CablePorts.connectionSpec(l,outletPos,Math.max(0,slot),mode,false,false));b=CablePorts.at(device,CablePorts.connectionSpec(l,device,0,mode,true,false));
  }else{
   mode=first instanceof GeneratorEntity?PowerCables.FEED:first instanceof OutletEntity outlet&&outlet.coax()?PowerCables.COAX:PowerCables.POWER;
   if(first instanceof OutletEntity outlet)slot=(outlet.socketMask&1)==0||outlet.coax()?0:1;
   a=CablePorts.at(source,CablePorts.connectionSpec(l,source,slot,mode,PowerNetwork.device(l,source),false));
   int kind=first instanceof GeneratorEntity?CablePorts.FEED:first instanceof OutletEntity?a.kind():CablePorts.OUTLET;
   b=new CablePorts.Port(aim,a.point().subtract(aim).normalize(),kind,0);
  }
  PowerCableRenderer.preview(a,b,e.getPoseStack(),g.renderBuffers().bufferSource(),e.getCamera().getPosition(),valid);
 }
 private static int socket(OutletEntity outlet,BlockPos device){
  for(var entity:Minecraft.getInstance().level.entitiesForRendering())if(entity instanceof PowerCableEntity c&&(c.startSpec()&31)==(outlet.coax()?CablePorts.COAX:CablePorts.OUTLET)&&c.start().equals(outlet.getBlockPos())&&c.end().equals(device))return c.startSpec()>>>8&1;
  return (outlet.socketMask&1)==0?0:PowerCables.capacity(outlet)==2&&(outlet.socketMask&2)==0?1:-1;
 }
}
