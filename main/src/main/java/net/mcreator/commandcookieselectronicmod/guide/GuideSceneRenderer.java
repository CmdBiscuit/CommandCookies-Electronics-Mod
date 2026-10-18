package net.mcreator.commandcookieselectronicmod.guide;

import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.mcreator.commandcookieselectronicmod.block.LaptopBlock;
import net.mcreator.commandcookieselectronicmod.client.ImageTvRenderer;
import net.mcreator.commandcookieselectronicmod.client.PowerCableRenderer;
import net.mcreator.commandcookieselectronicmod.client.model.ModelLaptop;
import net.mcreator.commandcookieselectronicmod.guide.GuideLesson.CordTrack;
import net.mcreator.commandcookieselectronicmod.guide.GuideLesson.TvTrack;
import net.mcreator.commandcookieselectronicmod.power.CablePorts;
import net.mcreator.commandcookieselectronicmod.power.CableRoute;
import net.mcreator.commandcookieselectronicmod.tv.ImageTvEntity;
import net.mcreator.commandcookieselectronicmod.tv.TvGeometry;

/**
 * Draws a {@link GuideScene} in scene-local block coordinates into the current pose. Blocks use
 * the game's own block models for the lesson's registered block states, TV cases use CEM's
 * ImageTvRenderer against standalone entities that are never added to a level, laptops use
 * their own entity model, and cords use CEM's production tube and plug geometry. Everything
 * heavy is built once per guide opening, sized by the lesson, and is immutable afterwards.
 */
final class GuideSceneRenderer {
    private static final int LIGHT = LightTexture.FULL_BRIGHT;
    private static final int PULSES = 3, BEAM_DOTS = 4;
    private static final double PULSE_SPEED = .06, PULSE_SIZE = .045;
    /** Layer spacing in blocks, in front of the image plane used by ImageTvRenderer. */
    private static final double LAYER = .0012;

    private final GuideLesson lesson;
    private final BlockState floor = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
    private final BlockState wall = Blocks.WHITE_CONCRETE.defaultBlockState();
    /** Only read by ImageTvRenderer; never placed, ticked or registered. Their images stay blank. */
    private final List<ImageTvEntity> tvs;
    private final ImageTvRenderer tvRenderer = new ImageTvRenderer(null);
    private final List<CordMesh> cords;
    private final List<ItemStack> items;
    private final ModelLaptop<?> laptop;

    GuideSceneRenderer(GuideLesson lesson) {
        this.lesson = lesson;
        var screens = new ArrayList<ImageTvEntity>(lesson.tvs.size());
        for (var track : lesson.tvs) {
            var tv = new ImageTvEntity(track.pos(), track.state());
            tv.image = "";
            screens.add(tv);
        }
        tvs = List.copyOf(screens);
        var meshes = new ArrayList<CordMesh>(lesson.cords.size());
        for (var cord : lesson.cords) meshes.add(mesh(cord));
        cords = List.copyOf(meshes);
        var stacks = new ArrayList<ItemStack>(lesson.items.size());
        for (var item : lesson.items) {
            var stack = item.item().get();
            stacks.add(stack == null ? ItemStack.EMPTY : stack);
        }
        items = List.copyOf(stacks);
        laptop = lesson.laptops.isEmpty() ? null : new ModelLaptop<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLaptop.LAYER_LOCATION));
    }

    /** Ports come from the same code a real link uses, from the block states alone. */
    private static CordMesh mesh(CordTrack cord) {
        var source = CablePorts.at(cord.from(), CablePorts.connectionSpec(cord.fromState(), cord.slot(), cord.mode(), false, false));
        var target = CablePorts.at(cord.to(), CablePorts.connectionSpec(cord.toState(), 0, cord.mode(), true, false));
        if (cord.targetCompression() != 1)
            target = new CablePorts.Port(target.point(), target.normal(), target.kind(), target.slot(), cord.targetCompression());
        var points = cord.path() == null ? CableRoute.natural(source, target) : cord.path().route(source, target);
        return new CordMesh(points, source, target);
    }

    void render(GuideScene scene, PoseStack poses, MultiBufferSource.BufferSource buffers, float pt) {
        // Solid pass: block models, TV cases, laptops, item displays and the cords.
        for (int x = 0; x < lesson.size; x++)
            for (int z = 0; z < lesson.size; z++) block(floor, x, 0, z, 0, poses, buffers);
        float wallDrop = scene.wallDrop(pt);
        if (!Float.isNaN(wallDrop))
            for (int x = 0; x < lesson.size; x++)
                for (int y = 1; y <= lesson.wallHeight; y++) block(wall, x, y, lesson.wallZ, wallDrop, poses, buffers);
        for (var track : lesson.blocks) {
            float drop = scene.drop(track, pt);
            if (!Float.isNaN(drop)) block(scene.state(track), track.pos(), drop, poses, buffers);
        }
        for (int i = 0; i < lesson.tvs.size(); i++) {
            var track = lesson.tvs.get(i);
            float drop = scene.drop(track, pt);
            if (Float.isNaN(drop)) continue;
            var tv = tvs.get(i);
            tv.powered = scene.screenOnFor(track, pt) >= 0;
            poses.pushPose();
            poses.translate(track.pos().getX(), track.pos().getY() + drop, track.pos().getZ());
            tvRenderer.render(tv, pt, poses, buffers, LIGHT, OverlayTexture.NO_OVERLAY);
            poses.popPose();
        }
        for (var track : lesson.laptops) {
            float drop = scene.drop(track, pt);
            if (!Float.isNaN(drop)) laptop(track, scene.lid(track, pt), drop, poses, buffers);
        }
        for (int i = 0; i < lesson.items.size(); i++) {
            var track = lesson.items.get(i);
            if (scene.shown(track) && !items.get(i).isEmpty()) item(items.get(i), scene.position(track, pt), track.scale(), track.yaw(), poses, buffers);
        }
        for (int i = 0; i < lesson.cords.size(); i++) {
            float laid = scene.layProgress(lesson.cords.get(i), pt);
            if (laid >= 0) PowerCableRenderer.render(cords.get(i).baked, laid, poses, buffers, LIGHT);
        }
        buffers.endBatch();

        // Glow pass, drawn over finished solids: screen pictures, pulses, beams and outlines.
        for (int i = 0; i < lesson.tvs.size(); i++) {
            var track = lesson.tvs.get(i);
            float on = scene.screenOnFor(track, pt), drop = scene.drop(track, pt);
            if (on >= 0 && !Float.isNaN(drop)) picture(track, tvs.get(i), poses, buffers, on, drop, scene.picture(track));
        }
        float clock = scene.effectTime(pt), time = scene.time(pt);
        for (int i = 0; i < lesson.cords.size(); i++) {
            var cord = lesson.cords.get(i);
            float energized = scene.energizedFor(cord, pt);
            if (energized >= 0) pulses(cords.get(i), poses, buffers, Math.min(1, energized / 10), clock, cord.pulseColor());
        }
        for (var beam : scene.beams()) {
            float fade = Mth.clamp(Math.min(time - beam.start(), beam.start() + beam.duration() - time) / 4, 0, 1);
            var glow = new GlowQuads(buffers, poses, fade);
            for (int i = 0; i < BEAM_DOTS; i++) {
                double along = ((time - beam.start()) * .035 + (double) i / BEAM_DOTS) % 1;
                glow.cube(beam.from().lerp(beam.to(), along), .035, beam.color());
            }
        }
        for (var h : scene.highlights()) {
            float fade = Mth.clamp(Math.min(time - h.start(), h.start() + h.duration() - time) / 4, 0, 1);
            float pulse = .65f + .35f * Mth.sin(clock * .3f);
            new GlowQuads(buffers, poses, fade * pulse).edges(h.box().inflate(.02), 1 / 32d, h.color());
        }
        for (var b : scene.bursts()) {
            float p = Mth.clamp((time - b.start()) / GuideLesson.BURST_TICKS, 0, 1);
            new GlowQuads(buffers, poses, 1 - p).edges(b.box().inflate(.04 + .35 * p), .025, GuideLesson.GOOD);
        }
    }

    private static void block(BlockState state, BlockPos pos, float drop, PoseStack poses, MultiBufferSource buffers) {
        block(state, pos.getX(), pos.getY(), pos.getZ(), drop, poses, buffers);
    }

    private static void block(BlockState state, int x, int y, int z, float drop, PoseStack poses, MultiBufferSource buffers) {
        poses.pushPose();
        poses.translate(x, y + drop, z);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poses, buffers, LIGHT, OverlayTexture.NO_OVERLAY);
        poses.popPose();
    }

    /** A plain item model, centred on the point and turned to face the default view. */
    private static void item(ItemStack stack, Vec3 at, float scale, float yaw, PoseStack poses, MultiBufferSource buffers) {
        poses.pushPose();
        poses.translate(at.x, at.y, at.z);
        poses.mulPose(Axis.YP.rotationDegrees(180 + yaw));
        poses.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, LIGHT, OverlayTexture.NO_OVERLAY, poses, buffers, null, 0);
        poses.popPose();
    }

    /** The laptop's own model with the transforms of its block entity renderer and a scripted lid. */
    private void laptop(GuideLesson.LaptopTrack track, float open, float drop, PoseStack poses, MultiBufferSource buffers) {
        var state = track.state();
        laptop.lid.xRot = Mth.lerp(open, -(float) Math.PI / 2, (float) Math.PI / 8);
        poses.pushPose();
        poses.translate(track.pos().getX() + .5, track.pos().getY() + drop, track.pos().getZ() + .5);
        float angle = switch (state.getValue(LaptopBlock.FACING)) {
            case EAST -> 90;
            case SOUTH -> 0;
            case WEST -> -90;
            default -> 180;
        };
        poses.mulPose(Axis.YP.rotationDegrees(angle));
        poses.translate(0, 1.5, 0);
        poses.scale(-1, -1, 1);
        var texture = ((LaptopBlock) state.getBlock()).texture();
        laptop.renderToBuffer(poses, buffers.getBuffer(RenderType.entityCutout(texture)), LIGHT, OverlayTexture.NO_OVERLAY, -1);
        poses.popPose();
    }

    private static void pulses(CordMesh cord, PoseStack poses, MultiBufferSource buffers, float alpha, float clock, int color) {
        double length = cord.length();
        if (length < 1e-3) return;
        var glow = new GlowQuads(buffers, poses, alpha);
        for (int i = 0; i < PULSES; i++) {
            double along = (clock * PULSE_SPEED + i * length / PULSES) % length;
            glow.cube(cord.pointAt(along), PULSE_SIZE, color);
        }
    }

    /**
     * The lit display, placed with the TV renderer's own transforms: a warm-up flash, then the
     * bundled sample picture once the lesson has chosen an image (a black screen until then).
     */
    private static void picture(TvTrack track, ImageTvEntity tv, PoseStack poses, MultiBufferSource buffers, float on, float drop, boolean picture) {
        poses.pushPose();
        poses.translate(track.pos().getX(), track.pos().getY() + drop, track.pos().getZ());
        poses.translate(.5, 0, .5);
        poses.mulPose(Axis.YP.rotationDegrees(-90 * TvGeometry.turns(tv.facing())));
        poses.translate(0, 0, -.5);
        boolean wallTv = tv.wall();
        if (wallTv) {
            poses.translate(0, 0, -TvGeometry.extension(tv.yaw, tv.pitch, tv.roll) / 16);
            poses.translate(.5, 9.5 / 16, 14.5 / 16);
            poses.mulPose(Axis.YP.rotationDegrees(tv.yaw));
            poses.mulPose(Axis.XP.rotationDegrees(tv.pitch));
            poses.mulPose(Axis.ZP.rotationDegrees(tv.roll));
            poses.translate(-.5, -9.5 / 16, -14.5 / 16);
        }
        if (picture) new Picture(new GlowQuads(buffers, poses, Math.min(1, on / GuideLesson.SCREEN_WARM_UP)), wallTv, tv.roll).draw();
        // A brief white flash, like a panel warming up, fading as the screen settles.
        float flash = 1 - Math.min(1, on / (GuideLesson.SCREEN_WARM_UP * .75f));
        if (flash > 0) new Picture(new GlowQuads(buffers, poses, flash), wallTv, tv.roll).rect(0, 0, 1, 1, 6, 0xFFFFFFFF);
        poses.popPose();
    }

    /** A baked cord plus its centre line, for the pulses that travel from supply to device. */
    private static final class CordMesh {
        final PowerCableRenderer.Baked baked;
        private final List<Vec3> points;
        private final double[] distance;

        CordMesh(List<Vec3> points, CablePorts.Port source, CablePorts.Port target) {
            this.points = List.copyOf(points);
            baked = PowerCableRenderer.bake(this.points, source, target);
            distance = new double[this.points.size()];
            for (int i = 1; i < distance.length; i++) distance[i] = distance[i - 1] + this.points.get(i - 1).distanceTo(this.points.get(i));
        }

        double length() {
            return distance.length < 2 ? 0 : distance[distance.length - 1];
        }

        Vec3 pointAt(double along) {
            int i = 1;
            while (i < distance.length - 1 && distance[i] < along) i++;
            double span = distance[i] - distance[i - 1];
            return points.get(i - 1).lerp(points.get(i), span < 1e-9 ? 0 : (along - distance[i - 1]) / span);
        }
    }

    /** A small landscape in display fractions: s runs left to right, t top to bottom. */
    private record Picture(GlowQuads glow, boolean wall, int roll) {
        void draw() {
            gradient(0, 0, 1, .62, 0, 0xFF2B6CD8, 0xFFF6B26B);
            sun(.72, .38, .085, 1, 0xFFFFE27A);
            polygon(2, 0xFF3E8E4A, 0, .62, 0, .47, .38, .40, .70, .62);
            polygon(3, 0xFF2E7A3B, .32, .62, .66, .46, 1, .52, 1, .62);
            gradient(0, .62, 1, 1, 4, 0xFF3B9A47, 0xFF1F5E2A);
        }

        void rect(double s0, double t0, double s1, double t1, int layer, int color) {
            glow.quad(p(s0, t1, layer), p(s1, t1, layer), p(s1, t0, layer), p(s0, t0, layer), color);
        }

        private void gradient(double s0, double t0, double s1, double t1, int layer, int top, int bottom) {
            glow.quad(p(s0, t1, layer), p(s1, t1, layer), p(s1, t0, layer), p(s0, t0, layer), bottom, bottom, top, top);
        }

        private void polygon(int layer, int color, double... st) {
            glow.quad(p(st[0], st[1], layer), p(st[2], st[3], layer), p(st[4], st[5], layer), p(st[6], st[7], layer), color);
        }

        /** An octagon; t is scaled so it stays round on the 16:9 panel. */
        private void sun(double s, double t, double radius, int layer, int color) {
            var centre = p(s, t, layer);
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * i / 4, b = Math.PI * (i + 1) / 4;
                var first = p(s + Math.cos(a) * radius, t + Math.sin(a) * radius * 16 / 9, layer);
                var second = p(s + Math.cos(b) * radius, t + Math.sin(b) * radius * 16 / 9, layer);
                glow.quad(centre, first, second, second, color);
            }
        }

        private Vec3 p(double s, double t, int layer) {
            var point = TvGeometry.imagePoint(Math.clamp(s, 0, 1), Math.clamp(t, 0, 1), wall, roll);
            return point.subtract(0, 0, layer * LAYER);
        }
    }
}
