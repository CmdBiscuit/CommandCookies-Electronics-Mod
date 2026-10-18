package net.mcreator.commandcookieselectronicmod.client;
import java.util.*;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.mcreator.commandcookieselectronicmod.power.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
public final class PowerCableRenderer extends EntityRenderer<PowerCableEntity> {
 public static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod","textures/entity/power_plugs.png");
 private static final ResourceLocation MODEL=ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod","models/entity/power_plugs.json");
 private record Box(Vec3 min,Vec3 max,double u,double v){}
 private record Quad(Vec3 a,Vec3 b,Vec3 c,Vec3 d,Vec3 normal,double u,double v,double span){}
 private static final class Mesh {
  final CablePorts.Port first,second;final CableRoute.Path path;final List<Quad> quads;long checkAt;int retries;
  Mesh(CablePorts.Port a,CablePorts.Port b,CableRoute.Path path,List<Quad> quads,long checkAt){first=a;second=b;this.path=path;this.quads=quads;this.checkAt=checkAt;}
 }
 private Mesh previewMesh;
 private static PowerCableRenderer current;private final Map<String,List<Box>> plugs=new HashMap<>();private final Map<PowerCableEntity,Mesh> cache=new WeakHashMap<>();
 public PowerCableRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=0;try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(MODEL)){var groups=JsonParser.parseReader(reader).getAsJsonObject();for(var entry:groups.entrySet()){var boxes=new ArrayList<Box>();for(var e:entry.getValue().getAsJsonArray()){var box=e.getAsJsonObject();boxes.add(new Box(vector(box.getAsJsonArray("from")).scale(1.0/16),vector(box.getAsJsonArray("to")).scale(1.0/16),box.get("u").getAsDouble()/32,box.get("v").getAsDouble()/32));}plugs.put(entry.getKey(),boxes);}}catch(Exception ex){throw new IllegalStateException("Unable to load power plug model",ex);}}
 private static Vec3 vector(JsonArray a){return new Vec3(a.get(0).getAsDouble(),a.get(1).getAsDouble(),a.get(2).getAsDouble());}
 @Override public ResourceLocation getTextureLocation(PowerCableEntity e){return TEXTURE;}
 @Override public boolean shouldRender(PowerCableEntity e,Frustum f,double x,double y,double z){return e.shouldRenderAtSqrDistance(e.distanceToSqr(x,y,z))&&f.isVisible(e.wireBounds());}
 @Override public void render(PowerCableEntity e,float yaw,float partial,PoseStack poses,MultiBufferSource buffers,int light){
  var a=e.first();var b=e.second();var old=cache.get(e);long now=e.level().getGameTime();
  boolean rebuild=old==null||!old.first.equals(a)||!old.second.equals(b);int retries=0;
  if(!rebuild&&now>=old.checkAt){old.checkAt=now+20+e.getId()%7;boolean changed=!old.path.unchanged(e.level());rebuild=changed||old.path.limited()&&old.retries<2;retries=changed?0:old.retries+1;}
  if(rebuild){var path=CableRoute.find(e.level(),a,b);var quads=wire(path.points());plug(quads,path.firstPort());plug(quads,path.secondPort());old=new Mesh(a,b,path,List.copyOf(quads),now+20+e.getId()%7);old.retries=retries;cache.put(e,old);}
  draw(old.quads,poses,buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),e.position(),light,0xFFFFFFFF,false);
 }
 private static Vec3 basis(CablePorts.Port p){var u=new Vec3(0,1,0).cross(p.normal());return u.lengthSqr()<.01?new Vec3(1,0,0):u.normalize();}
 private void plug(List<Quad> out,CablePorts.Port p){String group=switch(p.kind()){case CablePorts.OUTLET->"wall";case CablePorts.COAX,CablePorts.SPLITTER,CablePorts.RETRO_COAX,CablePorts.TV_COAX->"coax";case CablePorts.NES_AV->"av_nes";case CablePorts.SNES_AV->"av_snes";case CablePorts.N64_AV->"av_n64";case CablePorts.RETRO_AV->"av_adapter";case CablePorts.PC_HDMI,CablePorts.MONITOR_HDMI->"hdmi";case CablePorts.CAMERA,CablePorts.NES,CablePorts.SNES,CablePorts.N64->"barrel";case CablePorts.FEED,CablePorts.COAX_FEED->"grommet";default->"iec";};for(var b:plugs.get(group))box(out,p,b);if(p.kind()==CablePorts.SPLITTER&&p.slot()==0){var base=new CablePorts.Port(p.point().add(basis(p).scale(1.25/16)).add(p.normal().scale(-1.75/16)),p.normal(),p.kind(),0);for(var b:plugs.get("splitter"))box(out,base,b);}}
 private static Vec3 transform(CablePorts.Port p,Vec3 a){var u=basis(p);var v=p.normal().cross(u);return p.point().add(u.scale(a.x)).add(v.scale(a.y)).add(p.normal().scale(a.z*p.depthScale()));}
 private static void box(List<Quad> q,CablePorts.Port p,Box b){double x=b.min.x,y=b.min.y,z=b.min.z,X=b.max.x,Y=b.max.y,Z=b.max.z;Vec3[] v={new Vec3(x,y,z),new Vec3(X,y,z),new Vec3(X,Y,z),new Vec3(x,Y,z),new Vec3(x,y,Z),new Vec3(X,y,Z),new Vec3(X,Y,Z),new Vec3(x,Y,Z)};int[][] faces={{4,5,6,7},{1,0,3,2},{5,1,2,6},{0,4,7,3},{7,6,2,3},{0,1,5,4}};for(var face:faces){var a=transform(p,v[face[0]]);var B=transform(p,v[face[1]]);var c=transform(p,v[face[2]]);var d=transform(p,v[face[3]]);q.add(new Quad(a,B,c,d,B.subtract(a).cross(c.subtract(a)).normalize(),b.u,b.v,.21875));}}

 private static final int SIDES=8;
 private static Vec3[] ring(Vec3 point,Vec3 u,Vec3 v){var ring=new Vec3[SIDES];for(int i=0;i<SIDES;i++){double angle=2*Math.PI*i/SIDES;ring[i]=point.add(u.scale(Math.cos(angle)*.028)).add(v.scale(Math.sin(angle)*.028));}return ring;}
 private static Vec3 perpendicular(Vec3 t){var r=t.cross(new Vec3(0,1,0));if(r.lengthSqr()<1e-6)r=t.cross(new Vec3(1,0,0));return r.normalize();}
 private static List<Quad> wire(List<Vec3> points){
  var out=new ArrayList<Quad>();int n=points.size();if(n<2)return out;
  // A repeated sample keeps the previous direction; snapping to an axis pinched the tube.
  var tangents=new Vec3[n];Vec3 previous=points.get(n-1).subtract(points.get(0));
  for(int i=0;i<n;i++){
   var t=points.get(Math.min(n-1,i+1)).subtract(points.get(Math.max(0,i-1)));
   if(t.lengthSqr()<1e-9)t=previous;
   if(t.lengthSqr()<1e-9)t=new Vec3(0,0,1);
   tangents[i]=previous=t.normalize();
  }
  // Double-reflection rotation-minimising frames: no flips at vertical runs, no twist along bends.
  Vec3[][] rings=new Vec3[n][];Vec3 radial=perpendicular(tangents[0]);
  rings[0]=ring(points.get(0),radial,tangents[0].cross(radial).normalize());
  for(int i=0;i+1<n;i++){
   var step=points.get(i+1).subtract(points.get(i));double c1=step.lengthSqr();
   if(c1>1e-12){
    var r=radial.subtract(step.scale(2*step.dot(radial)/c1));var t=tangents[i].subtract(step.scale(2*step.dot(tangents[i])/c1));
    var w=tangents[i+1].subtract(t);double c2=w.lengthSqr();radial=c2>1e-12?r.subtract(w.scale(2*w.dot(r)/c2)):r;
   }
   radial=radial.subtract(tangents[i+1].scale(radial.dot(tangents[i+1])));
   radial=radial.lengthSqr()<1e-8?perpendicular(tangents[i+1]):radial.normalize();
   rings[i+1]=ring(points.get(i+1),radial,tangents[i+1].cross(radial).normalize());
  }
  for(int i=0;i<rings.length-1;i++)for(int j=0;j<SIDES;j++){
   var a=rings[i][j];var b=rings[i][(j+1)%SIDES];var c=rings[i+1][(j+1)%SIDES];var d=rings[i+1][j];
   // Use a single rubber-coloured texel, avoiding repeated bands at every bend.
   out.add(new Quad(a,b,c,d,b.subtract(a).cross(c.subtract(a)).normalize(),2.25/32,2.25/32,.5/32));
  }
  return out;
 }
 // Runs every frame for every cable, so it allocates nothing. A flat preview uses one bright texel.
 private static void draw(List<Quad> quads,PoseStack poses,VertexConsumer out,Vec3 origin,int light,int color,boolean flat){
  var pose=poses.last();double x=origin.x,y=origin.y,z=origin.z;
  for(var q:quads){
   float u=flat?0:(float)q.u,v=flat?.75f:(float)q.v,s=flat?.21875f:(float)q.span;
   float nx=(float)q.normal.x,ny=(float)q.normal.y,nz=(float)q.normal.z;
   vertex(out,pose,q.a,x,y,z,u,v,color,light,nx,ny,nz);vertex(out,pose,q.b,x,y,z,u+s,v,color,light,nx,ny,nz);
   vertex(out,pose,q.c,x,y,z,u+s,v+s,color,light,nx,ny,nz);vertex(out,pose,q.d,x,y,z,u,v+s,color,light,nx,ny,nz);
  }
 }
 private static void vertex(VertexConsumer out,PoseStack.Pose pose,Vec3 p,double x,double y,double z,float u,float v,int color,int light,float nx,float ny,float nz){out.addVertex(pose.pose(),(float)(p.x-x),(float)(p.y-y),(float)(p.z-z)).setColor(color).setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,nx,ny,nz);}
 /** Scene-local cord (the setup guide): production tube and plug geometry, without an entity or cache entry. */
 public static final class Baked {private final List<Quad> wire,firstPlug,secondPlug;private Baked(List<Quad> w,List<Quad> a,List<Quad> b){wire=w;firstPlug=a;secondPlug=b;}}
 public static Baked bake(List<Vec3> points,CablePorts.Port first,CablePorts.Port second){var a=new ArrayList<Quad>();var b=new ArrayList<Quad>();if(current!=null){current.plug(a,first);current.plug(b,second);}return new Baked(List.copyOf(wire(points)),List.copyOf(a),List.copyOf(b));}
 /** Draws the cord from its first plug up to {@code progress}; the second plug appears once the cord is complete. */
 public static void render(Baked mesh,float progress,PoseStack poses,MultiBufferSource buffers,int light){
  var out=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));float p=Math.clamp(progress,0f,1f);int rings=mesh.wire.size()/SIDES;
  draw(mesh.firstPlug,poses,out,Vec3.ZERO,light,0xFFFFFFFF,false);draw(mesh.wire.subList(0,Math.round(p*rings)*SIDES),poses,out,Vec3.ZERO,light,0xFFFFFFFF,false);
  if(p>=1)draw(mesh.secondPlug,poses,out,Vec3.ZERO,light,0xFFFFFFFF,false);
 }
 public static void preview(CablePorts.Port a,CablePorts.Port b,PoseStack pose,MultiBufferSource buffers,Vec3 camera,boolean valid){
  var level=Minecraft.getInstance().level;if(level==null)return;List<Quad> q;
  if(valid&&current!=null){
   var old=current.previewMesh;long now=level.getGameTime();
   boolean rebuild=old==null||!old.first.equals(a)||!old.second.equals(b);int retries=0;
   if(!rebuild&&now>=old.checkAt){old.checkAt=now+20;boolean changed=!old.path.unchanged(level);rebuild=changed||old.path.limited()&&old.retries<2;retries=changed?0:old.retries+1;}
   if(rebuild){var path=CableRoute.find(level,a,b);var quads=wire(path.points());current.plug(quads,path.firstPort());current.plug(quads,path.secondPort());old=new Mesh(a,b,path,List.copyOf(quads),now+20);old.retries=retries;current.previewMesh=old;}
   q=old.quads;valid=old.path.clear();
  }else{q=wire(CableRoute.natural(a,b));if(current!=null){current.plug(q,a);current.plug(q,b);}}
  draw(q,pose,buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)),camera,15728880,valid?0xC040FF70:0xC0FF5555,true);
 }
 @EventBusSubscriber(modid="commandcookies_electronic_mod",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
 public static final class Registration {@SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(PowerCableRegistration.TYPE,c->{current=new PowerCableRenderer(c);return current;});}}
}
