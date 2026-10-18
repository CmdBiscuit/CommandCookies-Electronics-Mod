package net.mcreator.commandcookieselectronicmod.client;

import net.minecraft.client.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.Marker;
import net.mcreator.commandcookieselectronicmod.block.SecurityCameraBlock;
import net.mcreator.commandcookieselectronicmod.security.SecurityCameraPackets.Feed;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** A local viewing entity uses the normal world renderer. The real player never moves. */
@EventBusSubscriber(modid="commandcookies_electronic_mod",value=Dist.CLIENT)
public final class SecurityCameraView {
 private Entity previous;
 private Marker lens;
 private CameraType cameraType;
 private boolean hud,active;
 private static SecurityCameraView current;
 public boolean active(){return active;}
 public boolean show(Feed feed){
  var mc=Minecraft.getInstance();
  var scene=RemoteCameraScene.level(feed.identity());if(mc.level==null||scene==null||!feed.powered()){stop();return false;}
  if(!active){previous=mc.getCameraEntity();cameraType=mc.options.getCameraType();hud=mc.options.hideGui;lens=new Marker(EntityType.MARKER,scene);active=true;current=this;}
  if(lens.level()!=scene)lens=new Marker(EntityType.MARKER,scene);var eye=SecurityCameraBlock.eye(feed.position(),feed.facing());lens.moveTo(eye.x,eye.y,eye.z,feed.facing().toYRot(),22.5f);lens.setOldPosAndRot();
  mc.options.hideGui=true;mc.options.setCameraType(CameraType.FIRST_PERSON);if(mc.getCameraEntity()!=lens)mc.setCameraEntity(lens);return true;
 }
 public void stop(){
  if(!active)return;var mc=Minecraft.getInstance();
  if(mc.getCameraEntity()==lens)mc.setCameraEntity(previous!=null&&previous.level()==mc.level?previous:mc.player);
  mc.options.hideGui=hud;mc.options.setCameraType(cameraType);active=false;previous=null;lens=null;if(current==this)current=null;
 }
 @SubscribeEvent public static void fov(ViewportEvent.ComputeFov event){
  if(event.usedConfiguredFov()&&Minecraft.getInstance().screen instanceof SecurityCameraScreen screen&&screen.viewActive())event.setFOV(screen.feedFov());
 }
 @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){if(current!=null)current.stop();RemoteCameraScene.clear();}
}
