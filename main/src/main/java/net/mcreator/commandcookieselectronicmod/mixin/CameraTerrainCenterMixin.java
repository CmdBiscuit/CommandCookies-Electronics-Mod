package net.mcreator.commandcookieselectronicmod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.mcreator.commandcookieselectronicmod.client.RemoteCameraScene;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** The remote terrain follows the lens while the real player's position stays intact. */
@Mixin(LevelRenderer.class)
public abstract class CameraTerrainCenterMixin {
 @ModifyVariable(method="setupRender",at=@At("STORE"),ordinal=0)
 private double cemCameraX(double value){return RemoteCameraScene.distanceOverride()>=0?Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().x:value;}
 @ModifyVariable(method="setupRender",at=@At("STORE"),ordinal=1)
 private double cemCameraY(double value){return RemoteCameraScene.distanceOverride()>=0?Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y:value;}
 @ModifyVariable(method="setupRender",at=@At("STORE"),ordinal=2)
 private double cemCameraZ(double value){return RemoteCameraScene.distanceOverride()>=0?Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().z:value;}
}
