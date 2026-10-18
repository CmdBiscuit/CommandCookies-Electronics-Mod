package net.mcreator.commandcookieselectronicmod.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.mcreator.commandcookieselectronicmod.client.RemoteCameraScene;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(GameRenderer.class)
public abstract class CameraRenderScopeMixin {
 @WrapMethod(method="renderLevel") private void cemCameraRender(DeltaTracker delta,Operation<Void> original){try(var ignored=RemoteCameraScene.frame()){original.call(delta);}}
}
