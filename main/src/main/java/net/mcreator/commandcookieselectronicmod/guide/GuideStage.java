package net.mcreator.commandcookieselectronicmod.guide;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModBlocks;
import net.mcreator.commandcookieselectronicmod.power.PowerCables;

/**
 * Shared staging for the authored lessons: the registered block states they show, item displays
 * for cues, and a ready-made power supply for lessons that are about something else.
 *
 * All lessons use one layout convention: devices stand on the floor facing north (towards the
 * default view) with their rear sockets towards the wall at z=6; wall outlets hang on that wall
 * in the row z=5.
 */
final class GuideStage {
    static final Direction FACING = Direction.NORTH;
    static final int WALL_Z = 6;
    static final String ADDON = "commandcookies_retro_expansion";

    private GuideStage() {}

    /** The block's default registered state, turned to face the viewer. */
    static BlockState state(Supplier<? extends Block> block) {
        var state = block.get().defaultBlockState();
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? state.setValue(BlockStateProperties.HORIZONTAL_FACING, FACING) : state;
    }

    static <T extends Comparable<T>> BlockState with(BlockState state, Property<T> property, T value) {
        return state.hasProperty(property) ? state.setValue(property, value) : state;
    }

    static BlockState powered(BlockState state, boolean on) {
        return with(state, BlockStateProperties.POWERED, on);
    }

    static BlockState generator(boolean on) {
        return powered(state(CommandcookiesElectronicModModBlocks.GENERATOR), on);
    }

    static BlockState outlet(boolean on) {
        return powered(state(CommandcookiesElectronicModModBlocks.WALL_POWER_OUTLET), on);
    }

    static BlockState coaxOutlet(boolean on) {
        return powered(state(CommandcookiesElectronicModModBlocks.WALL_COAX_OUTLET), on);
    }

    static Supplier<ItemStack> item(Supplier<? extends ItemLike> item) {
        return () -> new ItemStack(item.get());
    }

    /** An optional Retro Expansion item, or nothing when the addon is not installed. */
    static Supplier<ItemStack> addon(String path) {
        return () -> BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(ADDON, path)).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    static AABB box(GuideLesson.BlockBuilder block) {
        return block.box();
    }

    static Vec3 top(GuideLesson.BlockBuilder block) {
        return GuideLesson.top(block.box());
    }

    static Vec3 front(GuideLesson.BlockBuilder block) {
        return GuideLesson.front(block.box());
    }

    static Vec3 centre(GuideLesson.BlockBuilder block) {
        return block.box().getCenter();
    }

    /** Where a click on a wall outlet's plate is shown. */
    static Vec3 plate(BlockPos outlet) {
        return new Vec3(outlet.getX() + .5, outlet.getY() + .5, outlet.getZ() + 14.5 / 16);
    }

    /** Ticks from the first click of {@link #link} until its cable starts to be laid. */
    static final int LINK_TICKS = 65;

    /**
     * Two clicks with a linking tool (wrench, HDMI or AV cable): the source at {@code t}, then the
     * target 45 ticks later, outlined like the in-world selection preview. Returns the tick at
     * which the resulting cable starts to be laid.
     */
    static int link(GuideLesson.Builder b, int t, Supplier<ItemStack> tool, Vec3 sourceAt, AABB source, Vec3 targetAt, AABB target) {
        b.cue(t, 40, sourceAt, GuideCue.Action.RIGHT_CLICK, tool);
        b.highlight(t, 85, source, GuideLesson.SOURCE);
        b.highlight(t + 20, 65, target, GuideLesson.TARGET);
        b.cue(t + 45, 40, targetAt, GuideCue.Action.RIGHT_CLICK, tool);
        return t + LINK_TICKS;
    }

    /** A cable that is already plugged in and live when the lesson starts. */
    static void prewired(GuideLesson.Builder b, int mode, GuideLesson.BlockBuilder from, int slot, GuideLesson.BlockBuilder to, BlockState toState) {
        var cord = b.cord(mode, from.pos(), from.state(), slot, to.pos(), toState, -GuideLesson.LAY_TICKS - 20, -20);
        if (mode != PowerCables.POWER && mode != PowerCables.FEED) cord.signal();
    }

    /**
     * A generator that is already running and outlets already fed by it, present from the first
     * tick; the Generator and outlet lessons show how to set this up.
     */
    static Mains mains(GuideLesson.Builder b, BlockPos generator, BlockPos power, BlockPos coax) {
        var gen = b.block(generator, 0, generator(true)).fixed();
        GuideLesson.BlockBuilder outlet = null, coaxOutlet = null;
        if (power != null) {
            outlet = b.block(power, 0, outlet(true)).fixed();
            b.cord(PowerCables.FEED, generator, generator(true), 0, power, outlet(true), -GuideLesson.LAY_TICKS - 20, -20);
        }
        if (coax != null) {
            coaxOutlet = b.block(coax, 0, coaxOutlet(true)).fixed();
            b.cord(PowerCables.FEED, generator, generator(true), 0, coax, coaxOutlet(true), -GuideLesson.LAY_TICKS - 20, -20);
        }
        return new Mains(gen, outlet, coaxOutlet);
    }

    record Mains(GuideLesson.BlockBuilder generator, GuideLesson.BlockBuilder power, GuideLesson.BlockBuilder coax) {}
}
