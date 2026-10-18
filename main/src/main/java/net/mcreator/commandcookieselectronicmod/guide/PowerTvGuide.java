package net.mcreator.commandcookieselectronicmod.guide;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The original "Powering a TV" guide entry points, kept for existing callers. The wall-mounted TV
 * lesson is now one of several; {@link GuideLessons} routes every subject item to its own lesson.
 */
public final class PowerTvGuide {
    public record Step(Component title, Component body) {}

    private PowerTvGuide() {}

    public static Component title() {
        return lesson().title();
    }

    public static List<Step> steps() {
        return lesson().steps().stream().map(s -> new Step(s.title(), s.body())).toList();
    }

    /** Items whose tooltip offers a setup guide (any lesson). */
    public static boolean isSubject(ItemStack stack) {
        return GuideLessons.subject(stack) != null;
    }

    /** A fresh wall TV playhead at the start; nothing is shared between openings. */
    public static GuideScene newScene() {
        return new GuideScene(lesson());
    }

    private static GuideLesson lesson() {
        return GuideLessons.get(GuideLessons.WALL_TV);
    }
}
