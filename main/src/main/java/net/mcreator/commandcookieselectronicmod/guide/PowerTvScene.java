package net.mcreator.commandcookieselectronicmod.guide;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.mcreator.commandcookieselectronicmod.guide.GuideCue.Action;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModBlocks;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems;
import net.mcreator.commandcookieselectronicmod.power.CablePorts;
import net.mcreator.commandcookieselectronicmod.power.PowerCables;
import net.mcreator.commandcookieselectronicmod.tv.TvGeometry;

/**
 * The authored "Powering a TV" lesson for the wall-mounted TV, the guide's original scene:
 * generator, wall outlet and wall TV, with the timings the approved guide used.
 *
 * Interactions mirror the real game: GeneratorBlock opens its menu on right-click (fuel slot,
 * Start/Pause); a wrench click on the generator then the outlet lays the feed cable; a wrench
 * click on the outlet then a device plugs in its power cord.
 *
 * Layout in scene blocks: floor y=0 over x,z 0..6; wall x=0..6, y=1..3, z=6; generator (1,1,3),
 * outlet (3,1,5) and wall TV anchor (4,2,5), all facing north.
 */
public final class PowerTvScene {
    /** One keyframe per step of this lesson. */
    public static final int STEPS = 6;

    public static final int SIZE = 7, WALL_Z = 6, WALL_HEIGHT = 3;
    public static final Direction FACING = Direction.NORTH;
    public static final BlockPos GENERATOR = new BlockPos(1, 1, 3);
    public static final BlockPos OUTLET = new BlockPos(3, 1, 5);
    public static final BlockPos TV = new BlockPos(4, 2, 5);

    public static final AABB GENERATOR_BOX = new AABB(GENERATOR);
    /** The north-facing outlet plate: PowerOutletBlock's shape for facing=north. */
    public static final AABB OUTLET_BOX = new AABB(5 / 16d, 3 / 16d, 14.5 / 16d, 11 / 16d, 13 / 16d, 1).move(OUTLET);
    public static final AABB TV_BOX = TvGeometry.bounds(true, FACING, 0, 0, 0).move(TV);
    public static final Vec3 GENERATOR_TOP = Vec3.atCenterOf(GENERATOR).add(0, .5, 0);
    public static final Vec3 OUTLET_FACE = new Vec3(OUTLET.getX() + .5, OUTLET.getY() + .5, OUTLET.getZ() + 14.5 / 16);
    public static final Vec3 TV_FACE = new Vec3(TV.getX() + 1, TV.getY() + .6, TV.getZ() + .6);

    // Timeline, in ticks --------------------------------------------------------------------

    /** Start of each step. */
    static final int[] KEYFRAMES = {0, 95, 260, 345, 500, 705};
    public static final int TOTAL = 845;
    public static final int DROP_TICKS = GuideLesson.DROP_TICKS;
    public static final int LAY_TICKS = GuideLesson.LAY_TICKS;
    public static final int GENERATOR_ON = 200;
    public static final int TV_WARM_UP = GuideLesson.SCREEN_WARM_UP;
    public static final int BURST_TICKS = GuideLesson.BURST_TICKS;

    /** Models in the arrangement and the tick at which each appears. */
    public enum Part {
        FLOOR(0, false), WALL(10, true), GENERATOR(35, true), OUTLET(275, true), TV(515, true);

        public final int shownAt;
        public final boolean drops;

        Part(int shownAt, boolean drops) {
            this.shownAt = shownAt;
            this.drops = drops;
        }
    }

    /** The two cords: generator -> outlet feed cable, then outlet -> TV power cord. */
    public enum Cord {
        FEED(410, 440), POWER(615, 645);

        public final int laidAt, energizedAt;

        Cord(int laidAt, int energizedAt) {
            this.laidAt = laidAt;
            this.energizedAt = energizedAt;
        }
    }

    public static final int SOURCE = GuideLesson.SOURCE, TARGET = GuideLesson.TARGET, GOOD = GuideLesson.GOOD;

    private PowerTvScene() {}

    static GuideLesson lesson() {
        var b = GuideLesson.builder(GuideLessons.WALL_TV).lang("power_tv").size(SIZE).wall(WALL_Z, WALL_HEIGHT, Part.WALL.shownAt)
            .timeline(TOTAL, KEYFRAMES).milestones(GENERATOR_ON, Cord.FEED.energizedAt, Cord.POWER.energizedAt);
        var generatorOn = GuideStage.generator(true);
        var outletOn = GuideStage.outlet(true);
        var tv = GuideStage.state(CommandcookiesElectronicModModBlocks.TV_WALL_MOUNTED);
        b.block(GENERATOR, Part.GENERATOR.shownAt, GuideStage.generator(false)).then(GENERATOR_ON, generatorOn);
        b.block(OUTLET, Part.OUTLET.shownAt, GuideStage.outlet(false)).then(Cord.FEED.energizedAt, outletOn);
        b.tv(TV, tv, Part.TV.shownAt, Cord.POWER.energizedAt, GuideLesson.NEVER, Cord.POWER.energizedAt);
        b.cord(PowerCables.FEED, GENERATOR, generatorOn, 0, OUTLET, outletOn, Cord.FEED.laidAt, Cord.FEED.energizedAt);
        // The rear socket is only 1/16 block from the wall. Fold this demonstration's plug
        // depth into that gap, as the in-world router does when space behind a device is tight.
        b.cord(PowerCables.POWER, OUTLET, outletOn, 0, TV, tv, Cord.POWER.laidAt, Cord.POWER.energizedAt)
            .targetCompression(.125).path(PowerTvScene::tvCordPath);

        var generator = GuideStage.item(CommandcookiesElectronicModModItems.GENERATOR);
        var outlet = GuideStage.item(CommandcookiesElectronicModModItems.WALL_POWER_OUTLET);
        var wrench = GuideStage.item(CommandcookiesElectronicModModItems.WRENCH);
        // Step 1: place the generator.
        b.cue(20, 45, GENERATOR_TOP, Action.RIGHT_CLICK, generator);
        // Step 2: open its menu, fuel it.
        b.cue(95, 40, GENERATOR_TOP, Action.RIGHT_CLICK, null);
        b.cue(145, 50, GENERATOR_TOP, Action.INSERT_FUEL, () -> new ItemStack(Items.COAL));
        // Step 3: mount the outlet.
        b.cue(260, 45, OUTLET_FACE, Action.RIGHT_CLICK, outlet);
        // Step 4: wrench the generator, then the outlet.
        b.cue(345, 40, GENERATOR_TOP, Action.RIGHT_CLICK, wrench);
        b.cue(395, 40, OUTLET_FACE, Action.RIGHT_CLICK, wrench);
        // Step 5: mount the TV, wrench the outlet, then the TV.
        b.cue(500, 40, TV_FACE, Action.RIGHT_CLICK, GuideStage.item(CommandcookiesElectronicModModItems.TV_WALL_MOUNTED));
        b.cue(555, 40, OUTLET_FACE, Action.RIGHT_CLICK, wrench);
        b.cue(600, 40, TV_FACE, Action.RIGHT_CLICK, wrench);
        // Step 6: choosing an image; unplugging.
        b.cue(705, 60, TV_FACE, Action.RIGHT_CLICK, null);
        b.cue(775, 60, TV_FACE, Action.SNEAK_RIGHT_CLICK, wrench);

        b.highlight(GENERATOR_ON, 50, GENERATOR_BOX, GOOD);
        b.highlight(345, 70, GENERATOR_BOX, SOURCE);
        // Outlets in range light up after the first wrench click.
        b.highlight(370, 70, OUTLET_BOX, TARGET);
        b.highlight(Cord.FEED.energizedAt, 60, OUTLET_BOX, GOOD);
        b.highlight(555, 60, OUTLET_BOX, SOURCE);
        b.highlight(600, 45, TV_BOX, TARGET);
        b.highlight(Cord.POWER.energizedAt, 60, TV_BOX, GOOD);

        b.burst(GENERATOR_ON, GENERATOR_BOX);
        b.burst(Cord.FEED.energizedAt, OUTLET_BOX);
        b.burst(Cord.POWER.energizedAt, TV_BOX);
        return b.build();
    }

    /**
     * The TV cord drops from the outlet socket, runs low along the wall and rises in the gap
     * behind the case to the TV's mains socket, so it never cuts through the case or the wall.
     */
    static List<Vec3> tvCordPath(CablePorts.Port socket, CablePorts.Port tv) {
        var start = socket.exit();
        var end = tv.exit();
        double wallGap = WALL_Z - .14;
        var knots = List.of(
            start,
            new Vec3(start.x + .1, start.y - .38, start.z + .05),
            new Vec3(Mth.lerp(.4, start.x, end.x), 1.12, Mth.lerp(.55, start.z, wallGap)),
            new Vec3(Mth.lerp(.8, start.x, end.x), 1.3, wallGap),
            new Vec3(end.x, 1.85, wallGap),
            end);
        return hermite(knots, socket.normal(), tv.normal().scale(-1), 14);
    }

    /** A smooth curve through the knots: Catmull-Rom tangents inside, given end directions. */
    static List<Vec3> hermite(List<Vec3> knots, Vec3 startDirection, Vec3 endDirection, int samples) {
        int n = knots.size();
        var tangents = new Vec3[n];
        for (int i = 1; i < n - 1; i++) tangents[i] = knots.get(i + 1).subtract(knots.get(i - 1)).scale(.5);
        tangents[0] = startDirection.normalize().scale(knots.get(1).distanceTo(knots.get(0)));
        tangents[n - 1] = endDirection.normalize().scale(knots.get(n - 1).distanceTo(knots.get(n - 2)) * .25);
        var out = new ArrayList<Vec3>();
        out.add(knots.get(0));
        for (int i = 0; i + 1 < n; i++) {
            var p0 = knots.get(i);
            var p1 = knots.get(i + 1);
            for (int s = 1; s <= samples; s++) {
                double t = (double) s / samples, t2 = t * t, t3 = t2 * t;
                double h00 = 2 * t3 - 3 * t2 + 1, h10 = t3 - 2 * t2 + t, h01 = -2 * t3 + 3 * t2, h11 = t3 - t2;
                out.add(p0.scale(h00).add(tangents[i].scale(h10)).add(p1.scale(h01)).add(tangents[i + 1].scale(h11)));
            }
        }
        return List.copyOf(out);
    }
}
