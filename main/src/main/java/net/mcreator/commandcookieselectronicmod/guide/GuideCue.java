package net.mcreator.commandcookieselectronicmod.guide;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * What the player does at the current playhead (a mouse action with an optional held item) and
 * where in the scene to point. {@link GuideScene#cue()} derives a fresh value from the time, so
 * seeking, Back/Next and Replay always show the matching cue.
 *
 * @param target scene-local point to mark, or null when no action is shown
 * @param age    ticks since this cue started, for the marker's pulse
 * @param label  replaces the held item's name in the action chip (a menu button, a key), or null
 */
public record GuideCue(Vec3 target, Action action, ItemStack item, int age, Component label) {
    public enum Action {
        NONE(false, false), RIGHT_CLICK(true, false), SNEAK_RIGHT_CLICK(true, false), INSERT_FUEL(false, false),
        /** Using the held item without looking at a block, such as choosing a game for a ROM cartridge. */
        RIGHT_CLICK_AIR(true, false),
        /** A button in the block's own menu. */
        MENU_BUTTON(true, true),
        /** Keeping an item in either hand while interacting. */
        HOLD(false, false);

        /** Whether the cue shows a mouse; {@code left} lights the left button instead of the right. */
        public final boolean mouse, left;

        Action(boolean mouse, boolean left) {
            this.mouse = mouse;
            this.left = left;
        }

        public boolean sneaks() {
            return this == SNEAK_RIGHT_CLICK;
        }
    }

    public static final GuideCue NONE = new GuideCue(null, Action.NONE, ItemStack.EMPTY, 0, null);

    public GuideCue(Vec3 target, Action action, ItemStack item, int age) {
        this(target, action, item, age, null);
    }

    public boolean active() {
        return target != null;
    }
}
