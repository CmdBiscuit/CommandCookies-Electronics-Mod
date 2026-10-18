package net.mcreator.commandcookieselectronicmod.guide;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.mcreator.commandcookieselectronicmod.CommandcookiesElectronicModMod;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/**
 * The 3D viewport of the setup guide. The scene is drawn straight into the GUI: one pose,
 * built by {@link #camera(PoseStack)}, maps scene blocks to GUI pixels, and the same pose
 * projects cue targets for the screen's markers. Drag rotation orbits around the middle of the
 * arrangement; the zoom always fits the whole floor at the current tilt.
 */
final class GuideSceneView {
    /** Looking at the north side, slightly from the west, from above. */
    static final float DEFAULT_YAW = 155, DEFAULT_PITCH = 28;
    private static final float MIN_PITCH = 4, MAX_PITCH = 85, DEGREES_PER_PIXEL = .6f;
    /** The scene's height above and below the orbit focus, in blocks. */
    private static final double ABOVE = 2.3, BELOW = 1.8;
    /** GUI depth of the focus; the view clears depth inside its own rectangle before and after. */
    private static final float DEPTH = 200;
    private static final int MARGIN = 4;

    private final GuideScene scene;
    /** Orbit centre, in scene blocks, and the furthest floor corner from the orbit axis. */
    private final Vec3 focus;
    private final double reach;
    private GuideSceneRenderer renderer;
    private boolean failed;
    private int x, y, width, height;
    private float yaw = DEFAULT_YAW, pitch = DEFAULT_PITCH;

    GuideSceneView(GuideScene scene) {
        this.scene = scene;
        int size = scene.lesson().size;
        focus = new Vec3(size / 2d, 1.8, size / 2d);
        reach = Math.sqrt(2) * size / 2;
    }

    void bounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    /** Drag deltas in GUI pixels: sideways spins the scene, up/down tilts it. */
    void rotate(double dx, double dy) {
        yaw = Mth.wrapDegrees(yaw + (float) dx * DEGREES_PER_PIXEL);
        pitch = Mth.clamp(pitch + (float) dy * DEGREES_PER_PIXEL, MIN_PITCH, MAX_PITCH);
    }

    void resetView() {
        yaw = DEFAULT_YAW;
        pitch = DEFAULT_PITCH;
    }

    float yaw() {
        return yaw;
    }

    float pitch() {
        return pitch;
    }

    /** GUI pixels per block, so the floor fits sideways at any spin and vertically at this tilt. */
    private float scale() {
        double tilt = Math.toRadians(pitch), sin = Math.sin(tilt), cos = Math.cos(tilt);
        double up = ABOVE * cos + reach * sin, down = BELOW * cos + reach * sin;
        double across = (width - 2 * MARGIN) / (2 * reach);
        double tall = (height - 2 * MARGIN) / (up + down);
        return (float) Math.max(1, Math.min(across, tall));
    }

    /**
     * Appends scene-to-GUI transforms: the focus is moved to the origin, the scene is spun and
     * tilted, scaled to pixels with Y flipped (GUI Y grows downwards) and centred in the view.
     * The vertical centre is shifted so that the parts above and below the focus share the room.
     */
    private void camera(PoseStack poses) {
        float s = scale();
        double tilt = Math.toRadians(pitch);
        double up = ABOVE * Math.cos(tilt) + reach * Math.sin(tilt), down = BELOW * Math.cos(tilt) + reach * Math.sin(tilt);
        float centreY = y + height / 2f + (float) ((up - down) / 2 * s);
        poses.translate(x + width / 2f, centreY, DEPTH);
        poses.scale(s, -s, s);
        poses.mulPose(Axis.XP.rotationDegrees(pitch));
        poses.mulPose(Axis.YP.rotationDegrees(yaw));
        poses.translate(-focus.x, -focus.y, -focus.z);
    }

    /** Where a scene point appears in GUI coordinates (x, y; z is GUI depth). */
    Vector3f project(Vec3 point, float partialTick) {
        var poses = new PoseStack();
        camera(poses);
        return poses.last().pose().transformPosition((float) point.x, (float) point.y, (float) point.z, new Vector3f());
    }

    void render(GuiGraphics g, float partialTick) {
        if (failed || width < 8 || height < 8) return;
        g.flush();
        g.enableScissor(x, y, x + width, y + height);
        var poses = g.pose();
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        poses.pushPose();
        try {
            // Earlier GUI fills must not hide the scene; scissoring limits the clear to the view.
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            Lighting.setupFor3DItems();
            camera(poses);
            if (renderer == null) renderer = new GuideSceneRenderer(scene.lesson());
            renderer.render(scene, poses, buffers, partialTick);
        } catch (RuntimeException error) {
            failed = true;
            CommandcookiesElectronicModMod.LOGGER.warn("Setup guide scene could not be drawn; showing the steps only", error);
        } finally {
            try {
                buffers.endBatch();
            } finally {
                poses.popPose();
                // Later GUI layers (markers, tooltips) draw over the scene.
                RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
                g.disableScissor();
                Lighting.setupFor3DItems();
            }
        }
    }
}
