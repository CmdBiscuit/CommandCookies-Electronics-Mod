package net.mcreator.commandcookieselectronicmod.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.commandcookieselectronicmod.block.LaptopBlock;
import net.mcreator.commandcookieselectronicmod.block.entity.LaptopBlockEntity;
import net.mcreator.commandcookieselectronicmod.client.model.ModelLaptop;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid="commandcookies_electronic_mod", value=Dist.CLIENT, bus=EventBusSubscriber.Bus.MOD)
public class LaptopRenderer<T extends LaptopBlockEntity> implements BlockEntityRenderer<T> {
    private final ModelLaptop<?> model;
    public LaptopRenderer(BlockEntityRendererProvider.Context context) { model=new ModelLaptop<>(context.bakeLayer(ModelLaptop.LAYER_LOCATION)); }
    @Override public void render(T block, float partial, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        int ticks=(int)block.getLevel().getGameTime();
        boolean open=block.getBlockState().getValue(LaptopBlock.OPEN);
        if (!block.animationInitialized) {
            block.lastOpen=open;
            block.animationInitialized=true;
        }
        if (open != block.lastOpen) {
            block.animationState0.stop(); block.animationState1.stop();
            (open ? block.animationState0 : block.animationState1).start(ticks);
            block.animationStartTick=ticks;
            block.animationInProgress=true;
            block.lastOpen=open;
        }
        model.animateLid(block.animationState0,block.animationState1,ticks+partial);
        if (block.animationInProgress && ticks+partial-block.animationStartTick>=12) block.animationInProgress=false;
        if (!block.animationInProgress) model.lid.xRot=open ? (float)Math.PI/8 : -(float)Math.PI/2;
        poses.pushPose();
        poses.translate(.5,0,.5);
        float angle=switch(block.getBlockState().getValue(LaptopBlock.FACING)){case EAST->90;case SOUTH->0;case WEST->-90;default->180;};
        poses.mulPose(Axis.YP.rotationDegrees(angle));
        poses.translate(0,1.5,0);poses.scale(-1,-1,1);
        model.renderToBuffer(poses,buffers.getBuffer(RenderType.entityCutout(((LaptopBlock)block.getBlockState().getBlock()).texture())),light,overlay,-1);
        poses.popPose();
    }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CommandcookiesElectronicModModBlockEntities.LAPTOP.get(),LaptopRenderer::new);
        event.registerBlockEntityRenderer(CommandcookiesElectronicModModBlockEntities.LAPTOP_BLACK.get(),LaptopRenderer::new);
        event.registerBlockEntityRenderer(CommandcookiesElectronicModModBlockEntities.LAPTOP_WHITE.get(),LaptopRenderer::new);
        event.registerBlockEntityRenderer(CommandcookiesElectronicModModBlockEntities.LAPTOP_NAVY_BLUE.get(),LaptopRenderer::new);
        event.registerBlockEntityRenderer(CommandcookiesElectronicModModBlockEntities.LAPTOP_FOREST_GREEN.get(),LaptopRenderer::new);
        event.registerBlockEntityRenderer(CommandcookiesElectronicModModBlockEntities.LAPTOP_BURGUNDY.get(),LaptopRenderer::new);
    }
}
