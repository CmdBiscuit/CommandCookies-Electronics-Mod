package net.mcreator.commandcookieselectronicmod.client;

import java.util.*;
import io.netty.buffer.Unpooled;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.DataLayer;
import net.mcreator.commandcookieselectronicmod.security.SecurityCameraWire;
import net.mcreator.commandcookieselectronicmod.mixin.MinecraftCameraAccess;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Separate scene storage leaves the player's chunks and network world untouched. */
@EventBusSubscriber(modid="commandcookies_electronic_mod",value=Dist.CLIENT)
public final class RemoteCameraScene {
 private static RemoteCameraScene current;
 private static int scopeDepth;
 private static final List<RenderBuffers> retired=new ArrayList<>();
 private final SecurityCameraWire.Start start;
 private final LevelRenderer renderer;
 private final RenderBuffers buffers;
 private final ClientLevel level;
 private final Set<Long> chunks=new HashSet<>();
 private Set<Integer> actors=new HashSet<>();
 private RemoteCameraScene(SecurityCameraWire.Start start){
  this.start=start;var mc=Minecraft.getInstance();buffers=new RenderBuffers(1);renderer=new LevelRenderer(mc,mc.getEntityRenderDispatcher(),mc.getBlockEntityRenderDispatcher(),buffers);
  var type=mc.getConnection().registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(ResourceKey.create(Registries.DIMENSION_TYPE,ResourceLocation.parse(start.dimensionType())));
  level=new ClientLevel(mc.getConnection(),new ClientLevel.ClientLevelData(Difficulty.NORMAL,false,start.flat()),ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(start.dimension())),type,SecurityCameraWire.RADIUS,SecurityCameraWire.RADIUS,mc::getProfiler,renderer,false,0);
  level.getChunkSource().updateViewCenter(start.position().getX()>>4,start.position().getZ()>>4);level.setDayTime(start.time());
  try(var ignored=new Scope(this)){renderer.setLevel(level);}
 }
 public static int distanceOverride(){return scopeDepth>0?SecurityCameraWire.RADIUS:-1;}
 public static ClientLevel level(UUID camera){return current!=null&&current.start.camera().equals(camera)&&current.chunks.contains(new ChunkPos(current.start.position()).toLong())?current.level:null;}
 public static UUID token(){return current==null?null:current.start.token();}
 public static int loadedChunks(){return current==null?0:current.chunks.size();}
 public static int actorCount(){return current==null?0:current.actors.size();}
 public static final class Scope implements AutoCloseable {
  private final Minecraft mc;private final ClientLevel previous;private final LevelRenderer previousRenderer;private final boolean entered;
  private Scope(RemoteCameraScene scene){mc=Minecraft.getInstance();entered=scene!=null;previous=mc.level;previousRenderer=mc.levelRenderer;if(entered){scopeDepth++;mc.level=scene.level;((MinecraftCameraAccess)mc).cemSetLevelRenderer(scene.renderer);mc.getEntityRenderDispatcher().setLevel(scene.level);mc.getBlockEntityRenderDispatcher().setLevel(scene.level);}}
  @Override public void close(){if(entered){mc.level=previous;((MinecraftCameraAccess)mc).cemSetLevelRenderer(previousRenderer);mc.getEntityRenderDispatcher().setLevel(previous);mc.getBlockEntityRenderDispatcher().setLevel(previous);scopeDepth--;}}
 }
 public static Scope frame(){var mc=Minecraft.getInstance();return new Scope(current!=null&&mc.screen instanceof SecurityCameraScreen screen&&screen.viewActive()&&mc.getCameraEntity()!=null&&mc.getCameraEntity().level()==current.level?current:null);}
 public static void start(SecurityCameraWire.Start value){var mc=Minecraft.getInstance();if(!(mc.screen instanceof SecurityCameraScreen screen)||!screen.acceptStream(value))return;clear();try{if(!value.facing().getAxis().isHorizontal())throw new IllegalArgumentException("Invalid camera direction");current=new RemoteCameraScene(value);}catch(RuntimeException failure){net.mcreator.commandcookieselectronicmod.CommandcookiesElectronicModMod.LOGGER.warn("Cannot open camera scene",failure);screen.streamStopped("Camera stream unavailable.");}}
 public static void clear(){var old=current;current=null;if(old!=null){old.renderer.setLevel(null);old.renderer.close();var resources=(net.mcreator.commandcookieselectronicmod.mixin.LevelRendererCameraAccess)old.renderer;for(var buffer:new com.mojang.blaze3d.vertex.VertexBuffer[]{resources.cemSky(),resources.cemDark(),resources.cemStars(),resources.cemClouds()})if(buffer!=null)buffer.close();old.buffers.fixedBufferPack().close();retired.add(old.buffers);var mc=Minecraft.getInstance();mc.getEntityRenderDispatcher().setLevel(mc.level);mc.getBlockEntityRenderDispatcher().setLevel(mc.level);}}
 public static void stop(SecurityCameraWire.Stop value){if(current!=null&&current.start.token().equals(value.token())){var mc=Minecraft.getInstance();if(mc.screen instanceof SecurityCameraScreen screen)screen.streamStopped(value.reason());else clear();}}
 private static boolean matches(UUID token){return current!=null&&current.start.token().equals(token);}
 public static void chunk(SecurityCameraWire.Chunk value){if(!matches(value.token()))return;var scene=current;var center=new ChunkPos(scene.start.position());if(Math.abs(value.x()-center.x)>SecurityCameraWire.RADIUS||Math.abs(value.z()-center.z)>SecurityCameraWire.RADIUS)return;
  var buffer=new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(SecurityCameraWire.unpack(value.packed())),scene.level.registryAccess());try(var ignored=new Scope(scene)){
   var packet=ClientboundLevelChunkWithLightPacket.STREAM_CODEC.decode(buffer);if(packet.getX()!=value.x()||packet.getZ()!=value.z()||buffer.isReadable())throw new IllegalArgumentException("Invalid scene chunk");
   var data=packet.getChunkData();var chunk=scene.level.getChunkSource().replaceWithPacketData(value.x(),value.z(),data.getReadBuffer(),data.getHeightmaps(),data.getBlockEntitiesTagsConsumer(value.x(),value.z()));if(chunk==null)return;
   var engine=scene.level.getChunkSource().getLightEngine();var lights=packet.getLightData();readLight(scene,value.x(),value.z(),LightLayer.SKY,lights.getSkyYMask(),lights.getEmptySkyYMask(),lights.getSkyUpdates().iterator());readLight(scene,value.x(),value.z(),LightLayer.BLOCK,lights.getBlockYMask(),lights.getEmptyBlockYMask(),lights.getBlockUpdates().iterator());engine.setLightEnabled(chunk.getPos(),true);
   for(int i=0;i<chunk.getSectionsCount();i++){int y=scene.level.getSectionYFromSectionIndex(i);engine.updateSectionStatus(SectionPos.of(chunk.getPos(),y),chunk.getSections()[i].hasOnlyAir());scene.level.setSectionDirtyWithNeighbors(value.x(),y,value.z());}
   engine.runLightUpdates();CameraRendererCompat.lightReady(scene.level,value.x(),value.z());scene.chunks.add(chunk.getPos().toLong());
  }catch(RuntimeException failure){net.mcreator.commandcookieselectronicmod.CommandcookiesElectronicModMod.LOGGER.warn("Cannot read camera chunk",failure);if(Minecraft.getInstance().screen instanceof SecurityCameraScreen screen)screen.streamStopped("Camera stream unavailable.");}finally{buffer.release();}
 }
 private static void readLight(RemoteCameraScene scene,int x,int z,LightLayer layer,BitSet full,BitSet empty,Iterator<byte[]> updates){var engine=scene.level.getChunkSource().getLightEngine();for(int i=0;i<engine.getLightSectionCount();i++)if(full.get(i)||empty.get(i)){int y=engine.getMinLightSection()+i;engine.queueSectionData(layer,SectionPos.of(x,y,z),full.get(i)?new DataLayer(updates.next().clone()):new DataLayer());scene.level.setSectionDirtyWithNeighbors(x,y,z);}}
 public static void actors(SecurityCameraWire.Actors value){if(!matches(value.token()))return;var scene=current;var buffer=new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(SecurityCameraWire.unpack(value.packed())),scene.level.registryAccess());try{int count=buffer.readVarInt();if(count<0||count>128)throw new IllegalArgumentException("Too many camera actors");var seen=new HashSet<Integer>();
  for(int i=0;i<count;i++){var packet=ClientboundAddEntityPacket.STREAM_CODEC.decode(buffer);var data=ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer);var equipment=buffer.readBoolean()?ClientboundSetEquipmentPacket.STREAM_CODEC.decode(buffer):null;
   var center=scene.start.position();if(Math.abs(packet.getX()-center.getX())>SecurityCameraWire.RADIUS*16+2||Math.abs(packet.getZ()-center.getZ())>SecurityCameraWire.RADIUS*16+2)continue;
   var entity=scene.level.getEntity(packet.getId());if(entity==null||!entity.getUUID().equals(packet.getUUID())){if(entity!=null)scene.level.removeEntity(entity.getId(),Entity.RemovalReason.DISCARDED);if(packet.getType()==EntityType.PLAYER){var info=Minecraft.getInstance().getConnection().getPlayerInfo(packet.getUUID());entity=info==null?null:new RemotePlayer(scene.level,info.getProfile());}else entity=packet.getType().create(scene.level);if(entity==null)continue;entity.recreateFromPacket(packet);scene.level.addEntity(entity);}else{entity.lerpTo(packet.getX(),packet.getY(),packet.getZ(),packet.getYRot(),packet.getXRot(),5);entity.setYHeadRot(packet.getYHeadRot());}
   entity.getEntityData().assignValues(data.packedItems());if(equipment!=null&&entity instanceof LivingEntity living)for(var pair:equipment.getSlots())living.setItemSlot(pair.getFirst(),pair.getSecond());seen.add(entity.getId());
  }
  if(buffer.isReadable())throw new IllegalArgumentException("Invalid actor frame");for(int id:scene.actors)if(!seen.contains(id))scene.level.removeEntity(id,Entity.RemovalReason.DISCARDED);scene.actors=seen;scene.level.setDayTime(value.time());
 }catch(RuntimeException failure){net.mcreator.commandcookieselectronicmod.CommandcookiesElectronicModMod.LOGGER.warn("Cannot read camera actors",failure);}finally{buffer.release();}}
 @SubscribeEvent public static void tick(ClientTickEvent.Post event){retired.removeIf(buffers->{var pool=buffers.sectionBufferPool();if(pool.getFreeBufferCount()!=1)return false;var pack=pool.acquire();if(pack!=null)pack.close();return true;});if(current==null)return;try(var ignored=new Scope(current)){current.level.getChunkSource().getLightEngine().runLightUpdates();current.level.tickEntities();current.renderer.tick();}}
}
