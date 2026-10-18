package net.mcreator.commandcookiesretroexpansion.retro.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.commandcookieselectronicmod.nes.*;
import net.mcreator.commandcookiesretroexpansion.retro.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Covers only the CRT glass, preserving the original cabinet and Tetris cartridge. */
@EventBusSubscriber(modid=RetroItems.MOD,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class RetroMonitorRenderer implements BlockEntityRenderer<NesDeviceBlockEntity> {
    public static final ResourceLocation LOGO=ResourceLocation.fromNamespaceAndPath(RetroItems.MOD,"textures/block/retro_rom_logo.png");
    public RetroMonitorRenderer(BlockEntityRendererProvider.Context context){}
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event){
        event.enqueueWork(()->{try{RomLibrary.ensureFolders();}catch(java.io.IOException error){org.slf4j.LoggerFactory.getLogger(RetroItems.MOD).error("Could not create local ROM library folders",error);}});
    }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){
        event.registerBlockEntityRenderer(NesSystemRegistration.DEVICE_TYPE,RetroMonitorRenderer::new);
    }
    public static boolean showsLogo(NesDeviceBlockEntity device){
        if(!device.monitor())return false;
        var state=device.getBlockState();
        if(!state.getValue(NesDeviceBlock.POWERED))return false;
        var console=RetroConsoles.forMonitor(device);
        return console!=null&&((RetroDisplayState)(Object)console.block()).retro$hasRom();
    }
    @Override public void render(NesDeviceBlockEntity device,float tick,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
        if(!showsLogo(device))return;
        poses.pushPose();
        poses.translate(.5,0,.5);
        poses.mulPose(Axis.YP.rotationDegrees(-device.getBlockState().getValue(NesDeviceBlock.FACING).toYRot()+180));
        poses.translate(-.5,0,-.5);
        var out=buffers.getBuffer(RenderType.entitySolid(LOGO));
        var pose=poses.last();
        // The model's flat glass is x=3..13, y=5.9..13.4, z=1.6 (sixteenths).
        // Offset inward-facing surface by 0.01 model units to prevent depth flicker.
        vertex(out,pose,3,13.4f,0,0);
        vertex(out,pose,13,13.4f,1,0);
        vertex(out,pose,13,5.9f,1,1);
        vertex(out,pose,3,5.9f,0,1);
        poses.popPose();
    }
    private static void vertex(VertexConsumer out,PoseStack.Pose pose,float x,float y,float u,float v){
        out.addVertex(pose.pose(),x/16,y/16,1.59f/16).setColor(0xffffffff).setUv(u,v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xf000f0).setNormal(pose,0,0,-1);
    }
}
