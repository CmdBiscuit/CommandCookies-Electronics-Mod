package net.mcreator.commandcookieselectronicmod.mixin;
import net.minecraft.client.Options;
import net.mcreator.commandcookieselectronicmod.client.RemoteCameraScene;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Options.class)
public abstract class CameraRenderDistanceMixin {
 @Inject(method="getEffectiveRenderDistance",at=@At("HEAD"),cancellable=true) private void cemCameraDistance(CallbackInfoReturnable<Integer> result){int distance=RemoteCameraScene.distanceOverride();if(distance>=0)result.setReturnValue(distance);}
}
