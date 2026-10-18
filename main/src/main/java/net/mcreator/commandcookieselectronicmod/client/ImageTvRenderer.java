package net.mcreator.commandcookieselectronicmod.client;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.commandcookieselectronicmod.tv.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid="commandcookies_electronic_mod",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class ImageTvRenderer implements BlockEntityRenderer<ImageTvEntity> {
    private static final Map<String,List<Cube>> MODELS=new HashMap<>();
    private static final int[][] FACES={{1,0,3,2},{4,5,6,7},{0,4,7,3},{5,1,2,6},{7,6,2,3},{0,1,5,4}};
    private static final String[] NAMES={"north","south","west","east","up","down"};
    private record Quad(float[][] points,float[] uv,float nx,float ny,float nz){}
    private record Cube(String name,List<Quad> faces){}
    public ImageTvRenderer(BlockEntityRendererProvider.Context context){}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(TvRegistration.TYPE,ImageTvRenderer::new);}
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event){event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->Minecraft.getInstance().execute(()->{MODELS.clear();ImageTvImages.clear();}));}
    private static List<Cube> model(String name){return MODELS.computeIfAbsent(name,key->{
        try(var in=Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod","models/block/"+key+".json")).orElseThrow().open();var reader=new InputStreamReader(in,StandardCharsets.UTF_8)){
            var json=JsonParser.parseReader(reader).getAsJsonObject();var out=new ArrayList<Cube>();
            for(var item:json.getAsJsonArray("elements")){
                var e=item.getAsJsonObject();float[] a=vector(e.getAsJsonArray("from")),b=vector(e.getAsJsonArray("to"));float[][] points=vertices(a,b);
                if(e.has("rotation")){var r=e.getAsJsonObject("rotation");var origin=vector(r.getAsJsonArray("origin"));double angle=Math.toRadians(r.get("angle").getAsDouble()),c=Math.cos(angle),s=Math.sin(angle);String axis=r.get("axis").getAsString();
                    for(var p:points){double x=p[0]-origin[0],y=p[1]-origin[1],z=p[2]-origin[2];if(axis.equals("x")){p[1]=(float)(y*c-z*s+origin[1]);p[2]=(float)(y*s+z*c+origin[2]);}else if(axis.equals("y")){p[0]=(float)(x*c+z*s+origin[0]);p[2]=(float)(-x*s+z*c+origin[2]);}else{p[0]=(float)(x*c-y*s+origin[0]);p[1]=(float)(x*s+y*c+origin[1]);}}
                }
                var quads=new ArrayList<Quad>();var faces=e.getAsJsonObject("faces");
                for(int i=0;i<6;i++)if(faces.has(NAMES[i])){
                    var face=faces.getAsJsonObject(NAMES[i]);float[] uv=new float[4];for(int n=0;n<4;n++)uv[n]=face.getAsJsonArray("uv").get(n).getAsFloat()/16;
                    var pts=new float[4][3];for(int n=0;n<4;n++)for(int axis=0;axis<3;axis++)pts[n][axis]=points[FACES[i][n]][axis]/16;
                    float ax=pts[1][0]-pts[0][0],ay=pts[1][1]-pts[0][1],az=pts[1][2]-pts[0][2],bx=pts[2][0]-pts[0][0],by=pts[2][1]-pts[0][1],bz=pts[2][2]-pts[0][2];
                    float nx=ay*bz-az*by,ny=az*bx-ax*bz,nz=ax*by-ay*bx,len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
                    quads.add(new Quad(pts,uv,nx/len,ny/len,nz/len));
                }
                out.add(new Cube(e.get("name").getAsString(),quads));
            }
            return out;
        }catch(Exception error){net.mcreator.commandcookieselectronicmod.CommandcookiesElectronicModMod.LOGGER.warn("Could not load TV model {}",key,error);return List.of();}
    });}
    private static float[] vector(JsonArray json){return new float[]{json.get(0).getAsFloat(),json.get(1).getAsFloat(),json.get(2).getAsFloat()};}
    private static float[][] vertices(float[] a,float[] b){return new float[][]{{a[0],a[1],a[2]},{b[0],a[1],a[2]},{b[0],b[1],a[2]},{a[0],b[1],a[2]},{a[0],a[1],b[2]},{b[0],a[1],b[2]},{b[0],b[1],b[2]},{a[0],b[1],b[2]}};}
    private static boolean fixed(Cube cube){return cube.name.equals("Wall mounting plate")||cube.name.equals("Plate folded upper edge")||cube.name.startsWith("Wall fixing ");}
    private static void draw(Cube cube,PoseStack poses,VertexConsumer consumer,int light,int overlay){for(var q:cube.faces)quad(poses,consumer,q.points,q.uv,q.nx,q.ny,q.nz,light,overlay);}
    private static void quad(PoseStack poses,VertexConsumer consumer,float[][] points,float[] uv,float nx,float ny,float nz,int light,int overlay){
        for(int i=0;i<4;i++)consumer.addVertex(poses.last().pose(),points[i][0],points[i][1],points[i][2]).setColor(-1).setUv(i==0||i==3?uv[0]:uv[2],i<2?uv[3]:uv[1]).setOverlay(overlay).setLight(light).setNormal(poses.last(),nx,ny,nz);
    }
    private static void arm(PoseStack poses,VertexConsumer consumer,double extension,float[] uv,int light,int overlay){
        var p=vertices(new float[]{7.1f/16,8.6f/16,(float)((14-extension)/16)},new float[]{8.9f/16,10.4f/16,15.65f/16});
        float[][] normals={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
        for(int i=0;i<6;i++)quad(poses,consumer,new float[][]{p[FACES[i][0]],p[FACES[i][1]],p[FACES[i][2]],p[FACES[i][3]]},uv,normals[i][0],normals[i][1],normals[i][2],light,overlay);
    }
    @Override public void render(ImageTvEntity tv,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
        String name=tv.wall()?"tv_wall_mounted":"tv_with_stand";var cubes=model(name);if(cubes.isEmpty())return;
        var consumer=buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod","textures/block/"+name+".png")));
        poses.pushPose();poses.translate(.5,0,.5);poses.mulPose(Axis.YP.rotationDegrees(-90*TvGeometry.turns(tv.facing())));poses.translate(0,0,-.5);
        if(tv.wall()){
            for(var cube:cubes)if(fixed(cube))draw(cube,poses,consumer,light,overlay);
            var metal=cubes.stream().filter(c->c.name.equals("Mount crossbar")).findFirst().orElse(cubes.get(0));arm(poses,consumer,TvGeometry.extension(tv.yaw,tv.pitch,tv.roll),metal.faces.get(0).uv,light,overlay);
            poses.translate(0,0,-TvGeometry.extension(tv.yaw,tv.pitch,tv.roll)/16);poses.translate(.5,9.5/16,14.5/16);poses.mulPose(Axis.YP.rotationDegrees(tv.yaw));poses.mulPose(Axis.XP.rotationDegrees(tv.pitch));poses.mulPose(Axis.ZP.rotationDegrees(tv.roll));poses.translate(-.5,-9.5/16,-14.5/16);
            for(var cube:cubes)if(!fixed(cube))draw(cube,poses,consumer,light,overlay);
        }else for(var cube:cubes)draw(cube,poses,consumer,light,overlay);
        if(tv.powered&&!tv.image.isEmpty()){
            var image=ImageTvImages.get(tv.image);
            if(image!=null&&image.texture!=null){
                var fit=TvGeometry.fit(image.width,image.height,tv.fill,TvGeometry.portrait(tv.wall(),tv.roll));
                var corners=new double[][]{{fit.x0(),fit.y1()},{fit.x1(),fit.y1()},{fit.x1(),fit.y0()},{fit.x0(),fit.y0()}};var points=new float[4][3];
                for(int i=0;i<4;i++){var p=TvGeometry.imagePoint(corners[i][0],corners[i][1],tv.wall(),tv.roll);points[i]=new float[]{(float)p.x,(float)p.y,(float)p.z};}
                quad(poses,buffers.getBuffer(RenderType.entityCutoutNoCull(image.texture)),points,new float[]{fit.u0(),fit.v0(),fit.u1(),fit.v1()},0,0,-1,LightTexture.FULL_BRIGHT,overlay);
            }
        }
        poses.popPose();
    }
    @Override public int getViewDistance(){return 64;}
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(ImageTvEntity tv){return tv.renderBounds();}
    @EventBusSubscriber(modid="commandcookies_electronic_mod",value=Dist.CLIENT)
    public static final class Cleanup{@SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){ImageTvImages.clear();}}
}
