package net.mcreator.commandcookieselectronicmod.guide;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.mcreator.commandcookieselectronicmod.guide.GuideLesson.BlockTrack;
import net.mcreator.commandcookieselectronicmod.guide.GuideLesson.Burst;
import net.mcreator.commandcookieselectronicmod.guide.GuideLesson.Beam;
import net.mcreator.commandcookieselectronicmod.guide.GuideLesson.CordTrack;
import net.mcreator.commandcookieselectronicmod.guide.GuideLesson.Cue;
import net.mcreator.commandcookieselectronicmod.guide.GuideLesson.Highlight;
import net.mcreator.commandcookieselectronicmod.guide.GuideLesson.ItemTrack;
import net.mcreator.commandcookieselectronicmod.guide.GuideLesson.LaptopTrack;
import net.mcreator.commandcookieselectronicmod.guide.GuideLesson.TvTrack;

/**
 * Playhead for one {@link GuideLesson}. It stores only two integers: the timeline position,
 * clamped to the authored length, and an effect age that keeps counting while the guide plays
 * (also after the end, so the finished scene still animates). Everything visible is derived
 * from these and the immutable lesson, so replaying or seeking in either direction never creates
 * objects in a world, touches saved data or accumulates animation state.
 *
 * Methods taking {@code pt} accept the frame's fraction of a tick (0..1); pass the same frozen
 * value while paused.
 */
public final class GuideScene {
    private final GuideLesson lesson;
    private final List<ItemStack> cueItems;
    private int time, age;

    /** The wall-mounted TV lesson, as the guide's original single scene. */
    public GuideScene() {
        this(GuideLessons.get(GuideLessons.WALL_TV));
    }

    public GuideScene(GuideLesson lesson) {
        this.lesson = lesson;
        var items = new ArrayList<ItemStack>(lesson.cues.size());
        for (var cue : lesson.cues) items.add(stack(cue.item() == null ? null : cue.item().get()));
        cueItems = List.copyOf(items);
    }

    private static ItemStack stack(ItemStack stack) {
        return stack == null ? ItemStack.EMPTY : stack;
    }

    public GuideLesson lesson() {
        return lesson;
    }

    // Timeline -------------------------------------------------------------------------------

    public int getCurrentTime() {
        return time;
    }

    public int getTotalTime() {
        return lesson.total;
    }

    public int getKeyframeCount() {
        return lesson.keyframeCount();
    }

    public int getKeyframeTime(int index) {
        return lesson.keyframe(index);
    }

    /** Advances one tick: the playhead until the end, the effect age always. */
    public void tick() {
        if (time < lesson.total) time++;
        age++;
    }

    /** Back to the start of the lesson, as when the guide is first opened. */
    public void begin() {
        time = 0;
        age = 0;
    }

    public void reset() {
        begin();
    }

    /** Moves the playhead; the state at the new time is derived, whichever direction. */
    public void seekToTime(int target) {
        time = Mth.clamp(target, 0, lesson.total);
    }

    public boolean ended() {
        return time >= lesson.total;
    }

    /** Ticks played since opening or the last {@link #begin()}, including after the end. */
    public int age() {
        return age;
    }

    /** The playhead with the frame's partial tick; it stops at the end. */
    public float time(float pt) {
        return ended() ? time : time + Mth.clamp(pt, 0, 1);
    }

    /** A free-running clock for looping effects such as power pulses. */
    public float effectTime(float pt) {
        return age + Mth.clamp(pt, 0, 1);
    }

    // Lesson milestones ----------------------------------------------------------------------

    /** Fuel is in and the lesson's generator shows ONLINE. */
    public boolean generatorRunning() {
        return time >= lesson.generatorOn;
    }

    public boolean outletPowered() {
        return time >= lesson.outletOn;
    }

    /** The lesson's device is fully set up (powered, linked or charged). */
    public boolean powered() {
        return time >= lesson.poweredAt;
    }

    // Derived state --------------------------------------------------------------------------

    private boolean visible(int shownAt, int hiddenAt) {
        return time >= shownAt && time < hiddenAt;
    }

    public boolean shown(BlockTrack block) {
        return visible(block.shownAt(), block.hiddenAt());
    }

    public BlockState state(BlockTrack block) {
        return block.stateAt(time);
    }

    /** Height above its resting place while a part drops in; 0 once settled, NaN while hidden. */
    public float drop(BlockTrack block, float pt) {
        if (!shown(block)) return Float.NaN;
        return block.drops() ? drop(block.shownAt(), pt) : 0;
    }

    public float drop(TvTrack tv, float pt) {
        return time >= tv.shownAt() ? drop(tv.shownAt(), pt) : Float.NaN;
    }

    public float drop(LaptopTrack laptop, float pt) {
        return time >= laptop.shownAt() ? drop(laptop.shownAt(), pt) : Float.NaN;
    }

    /** The wall drops in with the room; NaN before. */
    public float wallDrop(float pt) {
        return lesson.hasWall() && time >= lesson.wallShownAt ? drop(lesson.wallShownAt, pt) : Float.NaN;
    }

    private float drop(int shownAt, float pt) {
        if (shownAt <= 0) return 0;
        float t = Mth.clamp((time(pt) - shownAt) / GuideLesson.DROP_TICKS, 0, 1);
        float left = 1 - t;
        return left * left * left;
    }

    /** Ticks since a TV screen switched on; negative while off. */
    public float screenOnFor(TvTrack tv, float pt) {
        return time >= tv.onAt() && time < tv.offAt() ? time(pt) - tv.onAt() : -1;
    }

    /** Ticks since the first TV's screen switched on; negative while off or without a TV. */
    public float screenOnFor(float pt) {
        return lesson.tvs.isEmpty() ? -1 : screenOnFor(lesson.tvs.get(0), pt);
    }

    public boolean picture(TvTrack tv) {
        return screenOnFor(tv, 0) >= 0 && time >= tv.pictureAt();
    }

    /** Lid opening, 0 closed .. 1 open, eased over {@link GuideLesson#LID_TICKS}. */
    public float lid(LaptopTrack laptop, float pt) {
        GuideLesson.Keyed<Boolean> last = null;
        for (var keyed : laptop.lid()) if (keyed.at() <= time) last = keyed;
        if (last == null) return 0;
        float t = Mth.clamp((time(pt) - last.at()) / GuideLesson.LID_TICKS, 0, 1);
        float eased = t * t * (3 - 2 * t);
        return last.value() ? eased : 1 - eased;
    }

    public boolean shown(ItemTrack item) {
        return visible(item.shownAt(), item.hiddenAt());
    }

    /** Where an item display is, gliding between its two points. */
    public Vec3 position(ItemTrack item, float pt) {
        if (item.moveAt() == GuideLesson.NEVER) return item.from();
        float t = Mth.clamp((time(pt) - item.moveAt()) / item.moveTicks(), 0, 1);
        return item.from().lerp(item.to(), t * t * (3 - 2 * t));
    }

    public boolean laid(CordTrack cord) {
        return cord.present(time);
    }

    public boolean energized(CordTrack cord) {
        return laid(cord) && time >= cord.energizedAt();
    }

    /** How much of a cord is laid, 0..1; negative while it is absent. */
    public float layProgress(CordTrack cord, float pt) {
        if (!laid(cord)) return -1;
        return Mth.clamp((time(pt) - cord.laidAt()) / GuideLesson.LAY_TICKS, 0, 1);
    }

    /** Ticks since power or signal started flowing through a cord; negative before. */
    public float energizedFor(CordTrack cord, float pt) {
        return energized(cord) ? time(pt) - cord.energizedAt() : -1;
    }

    /** Models, cords, items and lit screens present at the playhead (not cues or outlines). */
    public int elementCount() {
        int count = 1;
        if (lesson.hasWall() && time >= lesson.wallShownAt) count++;
        for (var block : lesson.blocks) if (shown(block)) count++;
        for (var tv : lesson.tvs) if (time >= tv.shownAt()) count += screenOnFor(tv, 0) >= 0 ? 2 : 1;
        for (var laptop : lesson.laptops) if (time >= laptop.shownAt()) count++;
        for (var item : lesson.items) if (shown(item)) count++;
        for (var cord : lesson.cords) if (laid(cord)) count++;
        return count;
    }

    /** The action shown at the playhead; {@link GuideCue#NONE} between actions. */
    public GuideCue cue() {
        for (int i = lesson.cues.size() - 1; i >= 0; i--) {
            Cue cue = lesson.cues.get(i);
            if (cue.covers(time)) {
                var label = cue.label() == null ? null : Component.translatable(GuideLesson.LANG + "label." + cue.label());
                return new GuideCue(cue.target(), cue.action(), cueItems.get(i), time - cue.start(), label);
            }
        }
        return GuideCue.NONE;
    }

    public List<Highlight> highlights() {
        return lesson.highlights.stream().filter(h -> h.covers(time)).toList();
    }

    public List<Burst> bursts() {
        return lesson.bursts.stream().filter(b -> b.covers(time)).toList();
    }

    public List<Beam> beams() {
        return lesson.beams.stream().filter(b -> b.covers(time)).toList();
    }
}
