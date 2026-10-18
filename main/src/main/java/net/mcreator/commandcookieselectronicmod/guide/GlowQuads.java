package net.mcreator.commandcookieselectronicmod.guide;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Untextured, unlit, double-sided colour quads for scene-local glow effects. No textures are created. */
final class GlowQuads {
    private final VertexConsumer out;
    private final PoseStack.Pose pose;
    private final float alpha;

    GlowQuads(MultiBufferSource buffers, PoseStack poses, float alpha) {
        out = buffers.getBuffer(RenderType.debugQuads());
        pose = poses.last();
        this.alpha = Math.clamp(alpha, 0f, 1f);
    }

    /** Corners in order; repeat the last corner for a triangle. Colours are 0xRRGGBB or 0xAARRGGBB. */
    void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int ca, int cb, int cc, int cd) {
        vertex(a, ca);
        vertex(b, cb);
        vertex(c, cc);
        vertex(d, cd);
    }

    void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
        quad(a, b, c, d, color, color, color, color);
    }

    /** A small axis-aligned cube, used for the travelling power pulses. */
    void cube(Vec3 centre, double half, int color) {
        box(centre.x - half, centre.y - half, centre.z - half, centre.x + half, centre.y + half, centre.z + half, color);
    }

    void box(double x, double y, double z, double X, double Y, double Z, int color) {
        Vec3[] v = {new Vec3(x, y, z), new Vec3(X, y, z), new Vec3(X, Y, z), new Vec3(x, Y, z),
            new Vec3(x, y, Z), new Vec3(X, y, Z), new Vec3(X, Y, Z), new Vec3(x, Y, Z)};
        int[][] faces = {{0, 1, 2, 3}, {5, 4, 7, 6}, {4, 0, 3, 7}, {1, 5, 6, 2}, {3, 2, 6, 7}, {4, 5, 1, 0}};
        for (var f : faces) quad(v[f[0]], v[f[1]], v[f[2]], v[f[3]], color);
    }

    /** The twelve edges of a box as thin bars of the given thickness. */
    void edges(AABB b, double thickness, int color) {
        double h = thickness / 2;
        double[] xs = {b.minX, b.maxX}, ys = {b.minY, b.maxY}, zs = {b.minZ, b.maxZ};
        for (double y : ys) for (double z : zs) box(b.minX - h, y - h, z - h, b.maxX + h, y + h, z + h, color);
        for (double x : xs) for (double z : zs) box(x - h, b.minY + h, z - h, x + h, b.maxY - h, z + h, color);
        for (double x : xs) for (double y : ys) box(x - h, y - h, b.minZ + h, x + h, y + h, b.maxZ - h, color);
    }

    private void vertex(Vec3 p, int color) {
        int a = (color >>> 24) == 0 ? 255 : color >>> 24;
        int argb = Math.round(a * alpha) << 24 | color & 0xFFFFFF;
        out.addVertex(pose.pose(), (float) p.x, (float) p.y, (float) p.z).setColor(argb);
    }
}
