package net.mcreator.commandcookieselectronicmod.guide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.mcreator.commandcookieselectronicmod.guide.GuideCue.Action;
import net.mcreator.commandcookieselectronicmod.power.CablePorts;
import net.mcreator.commandcookieselectronicmod.tv.ImageTvBlock;
import net.mcreator.commandcookieselectronicmod.tv.TvGeometry;

/**
 * One authored setup lesson: its texts, the model instances in its arrangement and the tick at
 * which each thing happens. It is plain immutable data, built once on the client; a
 * {@link GuideScene} derives everything visible from its playhead, so nothing here is created,
 * mutated or registered in a real level.
 *
 * Scene coordinates are in blocks: the floor is y=0 over x,z 0..size-1, devices stand on it
 * at y>=1 and an optional wall runs along x at {@code wallZ}.
 */
public final class GuideLesson {
    public static final String LANG = "commandcookies_electronic_mod.guide.";
    /** Outline colours: a selected source, its valid targets, a live result and a refusal. */
    public static final int SOURCE = 0xE6B84D, TARGET = 0x7CC4E8, GOOD = 0x82DFA3, WARN = 0xE8735A;
    /** A part drops into place over this many ticks after it is shown. */
    public static final int DROP_TICKS = 12;
    /** A cord is laid from its supplying plug to its receiving plug over this many ticks. */
    public static final int LAY_TICKS = 20;
    public static final int BURST_TICKS = 18;
    public static final int SCREEN_WARM_UP = 12;
    public static final int LID_TICKS = 12;
    /** Means "never" for optional ticks. */
    public static final int NEVER = Integer.MAX_VALUE;

    public record Step(Component title, Component body) {}

    /** A value that takes effect at a tick. */
    public record Keyed<T>(int at, T value) {}

    /** A block model whose state may change over the lesson (powered, attachments, ...). */
    public record BlockTrack(BlockPos pos, List<Keyed<BlockState>> states, int hiddenAt, boolean drops) {
        public int shownAt() {
            return states.get(0).at();
        }

        public BlockState stateAt(int time) {
            var state = states.get(0).value();
            for (var keyed : states) if (keyed.at() <= time) state = keyed.value();
            return state;
        }
    }

    /** A TV case drawn by CEM's own TV renderer; its screen turns on and shows the sample picture. */
    public record TvTrack(BlockPos pos, BlockState state, int shownAt, int onAt, int offAt, int pictureAt) {
        public boolean wall() {
            return ((ImageTvBlock) state.getBlock()).wall;
        }
    }

    /** A laptop drawn with its own model; {@code lid} lists when it opens (true) or closes. */
    public record LaptopTrack(BlockPos pos, BlockState state, int shownAt, List<Keyed<Boolean>> lid) {}

    /** A plain item display, optionally gliding from one point to another (batteries, a cartridge). */
    public record ItemTrack(Supplier<ItemStack> item, Vec3 from, Vec3 to, int shownAt, int moveAt, int moveTicks, int hiddenAt, float scale, float yaw) {}

    /** Custom routing for a cord when the production route would not suit the demonstration. */
    public interface CordPath {
        List<Vec3> route(CablePorts.Port source, CablePorts.Port target);
    }

    /**
     * A production cable between two scene blocks. Ports come from the same code a real link
     * uses, from the block states alone. {@code mode} is a {@code PowerCables} mode.
     */
    public record CordTrack(int mode, BlockPos from, BlockState fromState, int slot, BlockPos to, BlockState toState,
                            int laidAt, int energizedAt, int removedAt, double targetCompression, CordPath path, int pulseColor) {
        public boolean present(int time) {
            return time >= laidAt && time < removedAt;
        }
    }

    public record Cue(int start, int duration, Vec3 target, Action action, Supplier<ItemStack> item, String label) {
        boolean covers(int time) {
            return time >= start && time < start + duration;
        }
    }

    /** A pulsing outline on a box, in place of an in-world selection highlight. */
    public record Highlight(int start, int duration, AABB box, int color) {
        boolean covers(int time) {
            return time >= start && time < start + duration;
        }
    }

    /** A short expanding outline confirming that a part became powered or connected. */
    public record Burst(int start, AABB box) {
        boolean covers(int time) {
            return time >= start && time < start + BURST_TICKS;
        }
    }

    /** Travelling dots between two points for a link that has no cable, such as phone pairing. */
    public record Beam(int start, int duration, Vec3 from, Vec3 to, int color) {
        boolean covers(int time) {
            return time >= start && time < start + duration;
        }
    }

    public final String id;
    /** Language key segment; the wall TV keeps its original "power_tv" texts. */
    public final String lang;
    public final int size, wallZ, wallHeight, wallShownAt, total;
    private final int[] keyframes;
    public final List<BlockTrack> blocks;
    public final List<TvTrack> tvs;
    public final List<LaptopTrack> laptops;
    public final List<ItemTrack> items;
    public final List<CordTrack> cords;
    public final List<Cue> cues;
    public final List<Highlight> highlights;
    public final List<Burst> bursts;
    public final List<Beam> beams;
    /** Milestones for simple state queries: generator running, first outlet live, lesson device on. */
    public final int generatorOn, outletOn, poweredAt;

    private GuideLesson(Builder b) {
        id = b.id;
        lang = b.lang;
        size = b.size;
        wallZ = b.wallZ;
        wallHeight = b.wallHeight;
        wallShownAt = b.wallShownAt;
        keyframes = b.keyframes.clone();
        total = b.total;
        blocks = b.blocks.stream().map(BlockBuilder::build).toList();
        tvs = List.copyOf(b.tvs);
        laptops = b.laptops.stream().map(LaptopBuilder::build).toList();
        items = b.items.stream().map(ItemBuilder::build).toList();
        cords = b.cords.stream().map(CordBuilder::build).toList();
        cues = List.copyOf(b.cues);
        highlights = List.copyOf(b.highlights);
        bursts = List.copyOf(b.bursts);
        beams = List.copyOf(b.beams);
        generatorOn = b.generatorOn;
        outletOn = b.outletOn;
        poweredAt = b.poweredAt;
    }

    public Component title() {
        return Component.translatable(LANG + lang + ".title");
    }

    public List<Step> steps() {
        return IntStream.rangeClosed(1, keyframes.length)
            .mapToObj(i -> new Step(Component.translatable(LANG + lang + ".step" + i + ".title"), Component.translatable(LANG + lang + ".step" + i + ".body")))
            .toList();
    }

    public int keyframeCount() {
        return keyframes.length;
    }

    public int keyframe(int index) {
        return keyframes[java.util.Objects.checkIndex(index, keyframes.length)];
    }

    public boolean hasWall() {
        return wallZ >= 0;
    }

    @Override
    public String toString() {
        return "GuideLesson[" + id + ", " + keyframes.length + " steps, " + total + " ticks]";
    }

    // Shared geometry ------------------------------------------------------------------------

    /** The block's own outline at a scene position, from its state alone. */
    public static AABB box(BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof ImageTvBlock tv)
            return TvGeometry.bounds(tv.wall, state.getValue(ImageTvBlock.FACING), 0, 0, 0).move(pos);
        var shape = state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        return shape.isEmpty() ? new AABB(pos) : shape.bounds().move(pos);
    }

    public static Vec3 centre(AABB box) {
        return box.getCenter();
    }

    /** Middle of the box's top face, where a click on a low device is shown. */
    public static Vec3 top(AABB box) {
        return new Vec3((box.minX + box.maxX) / 2, box.maxY, (box.minZ + box.maxZ) / 2);
    }

    /** Middle of the box's north (viewer-facing) face. */
    public static Vec3 front(AABB box) {
        return new Vec3((box.minX + box.maxX) / 2, (box.minY + box.maxY) / 2, box.minZ);
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    // Builder --------------------------------------------------------------------------------

    public static final class Builder {
        private final String id;
        private String lang;
        private int size = 7, wallZ = 6, wallHeight = 3, wallShownAt = 10, total;
        private int[] keyframes = {0};
        private final List<BlockBuilder> blocks = new ArrayList<>();
        private final List<TvTrack> tvs = new ArrayList<>();
        private final List<LaptopBuilder> laptops = new ArrayList<>();
        private final List<ItemBuilder> items = new ArrayList<>();
        private final List<CordBuilder> cords = new ArrayList<>();
        private final List<Cue> cues = new ArrayList<>();
        private final List<Highlight> highlights = new ArrayList<>();
        private final List<Burst> bursts = new ArrayList<>();
        private final List<Beam> beams = new ArrayList<>();
        private int generatorOn = NEVER, outletOn = NEVER, poweredAt = NEVER;

        private Builder(String id) {
            this.id = id;
            lang = id;
        }

        public Builder lang(String lang) {
            this.lang = lang;
            return this;
        }

        public Builder size(int size) {
            this.size = size;
            return this;
        }

        public Builder wall(int z, int height, int shownAt) {
            wallZ = z;
            wallHeight = height;
            wallShownAt = shownAt;
            return this;
        }

        public Builder noWall() {
            wallZ = -1;
            return this;
        }

        /** Start tick of each step, ascending from 0, and the lesson's length. */
        public Builder timeline(int total, int... keyframes) {
            if (keyframes.length == 0 || keyframes[0] != 0) throw new IllegalArgumentException("The first step must start at 0");
            for (int i = 1; i < keyframes.length; i++)
                if (keyframes[i] <= keyframes[i - 1]) throw new IllegalArgumentException("Steps must be ascending");
            if (total <= keyframes[keyframes.length - 1]) throw new IllegalArgumentException("The last step needs time to play");
            this.keyframes = keyframes.clone();
            this.total = total;
            return this;
        }

        public Builder milestones(int generatorOn, int outletOn, int poweredAt) {
            this.generatorOn = generatorOn;
            this.outletOn = outletOn;
            this.poweredAt = poweredAt;
            return this;
        }

        public BlockBuilder block(BlockPos pos, int shownAt, BlockState state) {
            var block = new BlockBuilder(pos, shownAt, state);
            blocks.add(block);
            return block;
        }

        public Builder tv(BlockPos pos, BlockState state, int shownAt, int onAt, int offAt, int pictureAt) {
            tvs.add(new TvTrack(pos.immutable(), state, shownAt, onAt, offAt, pictureAt));
            return this;
        }

        public LaptopBuilder laptop(BlockPos pos, BlockState state, int shownAt) {
            var laptop = new LaptopBuilder(pos, state, shownAt);
            laptops.add(laptop);
            return laptop;
        }

        public ItemBuilder item(Supplier<ItemStack> item, Vec3 at, int shownAt, int hiddenAt) {
            var display = new ItemBuilder(item, at, shownAt, hiddenAt);
            items.add(display);
            return display;
        }

        /**
         * A cable between two placed blocks, laid at {@code laidAt}; {@link #NEVER} leaves it dark.
         * Negative ticks describe a cable that was already in place when the lesson starts.
         */
        public CordBuilder cord(int mode, BlockPos from, BlockState fromState, int slot, BlockPos to, BlockState toState, int laidAt, int energizedAt) {
            var cord = new CordBuilder(mode, from, fromState, slot, to, toState, laidAt, energizedAt);
            cords.add(cord);
            return cord;
        }

        public Builder cue(int start, int duration, Vec3 target, Action action, Supplier<ItemStack> item) {
            return cue(start, duration, target, action, item, null);
        }

        /** {@code label} is a key under {@code guide.label.} shown instead of the item name. */
        public Builder cue(int start, int duration, Vec3 target, Action action, Supplier<ItemStack> item, String label) {
            cues.add(new Cue(start, duration, target, action, item, label));
            return this;
        }

        public Builder highlight(int start, int duration, AABB box, int color) {
            highlights.add(new Highlight(start, duration, box, color));
            return this;
        }

        public Builder burst(int start, AABB box) {
            bursts.add(new Burst(start, box));
            return this;
        }

        public Builder beam(int start, int duration, Vec3 from, Vec3 to, int color) {
            beams.add(new Beam(start, duration, from, to, color));
            return this;
        }

        public GuideLesson build() {
            if (total <= 0) throw new IllegalStateException("Lesson " + id + " has no timeline");
            cues.sort(Comparator.comparingInt(Cue::start));
            return new GuideLesson(this);
        }
    }

    public static final class BlockBuilder {
        private final BlockPos pos;
        private final List<Keyed<BlockState>> states = new ArrayList<>();
        private int hiddenAt = NEVER;
        private boolean drops = true;

        private BlockBuilder(BlockPos pos, int shownAt, BlockState state) {
            this.pos = pos.immutable();
            states.add(new Keyed<>(shownAt, state));
        }

        /** Switches to another state of the same block, e.g. when it becomes powered. */
        public BlockBuilder then(int at, BlockState state) {
            if (at < states.get(states.size() - 1).at()) throw new IllegalArgumentException("States must be in time order");
            states.add(new Keyed<>(at, state));
            return this;
        }

        public BlockBuilder hidden(int at) {
            hiddenAt = at;
            return this;
        }

        /** Present from the start without the drop-in animation. */
        public BlockBuilder fixed() {
            drops = false;
            return this;
        }

        public AABB box() {
            return GuideLesson.box(states.get(states.size() - 1).value(), pos);
        }

        public BlockPos pos() {
            return pos;
        }

        public BlockState state() {
            return states.get(0).value();
        }

        BlockTrack build() {
            return new BlockTrack(pos, List.copyOf(states), hiddenAt, drops);
        }
    }

    public static final class LaptopBuilder {
        private final BlockPos pos;
        private final BlockState state;
        private final int shownAt;
        private final List<Keyed<Boolean>> lid = new ArrayList<>();

        private LaptopBuilder(BlockPos pos, BlockState state, int shownAt) {
            this.pos = pos.immutable();
            this.state = state;
            this.shownAt = shownAt;
        }

        public LaptopBuilder lid(int at, boolean open) {
            lid.add(new Keyed<>(at, open));
            return this;
        }

        LaptopTrack build() {
            lid.sort(Comparator.comparingInt(Keyed::at));
            return new LaptopTrack(pos, state, shownAt, List.copyOf(lid));
        }
    }

    public static final class ItemBuilder {
        private final Supplier<ItemStack> item;
        private final Vec3 from;
        private Vec3 to;
        private final int shownAt, hiddenAt;
        private int moveAt = NEVER, moveTicks = 1;
        private float scale = .45f, yaw;

        private ItemBuilder(Supplier<ItemStack> item, Vec3 at, int shownAt, int hiddenAt) {
            this.item = item;
            from = to = at;
            this.shownAt = shownAt;
            this.hiddenAt = hiddenAt;
        }

        /** Glides to {@code to} over {@code ticks}, starting at {@code at}. */
        public ItemBuilder moveTo(int at, int ticks, Vec3 to) {
            moveAt = at;
            moveTicks = Math.max(1, ticks);
            this.to = to;
            return this;
        }

        public ItemBuilder scale(float scale) {
            this.scale = scale;
            return this;
        }

        public ItemBuilder yaw(float yaw) {
            this.yaw = yaw;
            return this;
        }

        ItemTrack build() {
            return new ItemTrack(item, from, to, shownAt, moveAt, moveTicks, hiddenAt, scale, yaw);
        }
    }

    public static final class CordBuilder {
        private final int mode, slot, laidAt, energizedAt;
        private final BlockPos from, to;
        private final BlockState fromState, toState;
        private int removedAt = NEVER, pulseColor = 0xFFFFD84A;
        private double targetCompression = 1;
        private CordPath path;

        private CordBuilder(int mode, BlockPos from, BlockState fromState, int slot, BlockPos to, BlockState toState, int laidAt, int energizedAt) {
            this.mode = mode;
            this.from = from.immutable();
            this.fromState = fromState;
            this.slot = slot;
            this.to = to.immutable();
            this.toState = toState;
            this.laidAt = laidAt;
            this.energizedAt = energizedAt;
        }

        public CordBuilder removed(int at) {
            removedAt = at;
            return this;
        }

        public CordBuilder path(CordPath path) {
            this.path = path;
            return this;
        }

        /** Folds the receiving plug's depth into a tight gap, as the in-world router does. */
        public CordBuilder targetCompression(double compression) {
            targetCompression = compression;
            return this;
        }

        /** Signal cables (coax, AV, HDMI) carry cool-coloured pulses instead of yellow power. */
        public CordBuilder signal() {
            pulseColor = 0xFF8FE3FF;
            return this;
        }

        CordTrack build() {
            return new CordTrack(mode, from, fromState, slot, to, toState, laidAt, energizedAt, removedAt, targetCompression, path, pulseColor);
        }
    }

    /** For tests and logs: the lesson's step start ticks. */
    public int[] keyframes() {
        return Arrays.copyOf(keyframes, keyframes.length);
    }
}
