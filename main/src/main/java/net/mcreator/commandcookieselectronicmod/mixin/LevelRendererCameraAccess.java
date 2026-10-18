package net.mcreator.commandcookieselectronicmod.mixin;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(LevelRenderer.class)
public interface LevelRendererCameraAccess {
 @Accessor("skyBuffer") VertexBuffer cemSky();
 @Accessor("darkBuffer") VertexBuffer cemDark();
 @Accessor("starBuffer") VertexBuffer cemStars();
 @Accessor("cloudBuffer") VertexBuffer cemClouds();
}
