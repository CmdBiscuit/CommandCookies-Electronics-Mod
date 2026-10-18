package net.mcreator.commandcookieselectronicmod.guide;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Opens the CEM setup guide: press the guide key (G by default, rebindable under
 * CommandCookies Electronic Mod in Controls) while hovering a supported device or tool in any
 * inventory or item list, or while holding one in the world. Each item opens its own lesson
 * (see {@link GuideLessons}).
 */
public final class GuideClientSetup {
    public static final KeyMapping OPEN_GUIDE = new KeyMapping("key.commandcookies_electronic_mod.setup_guide",
        KeyConflictContext.UNIVERSAL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.commandcookies_electronic_mod");
    /** A tooltip counts as hovered for this long after it was last built. */
    private static final long HOVER_NANOS = 150_000_000L;
    private static final Style HINT = Style.EMPTY.withColor(TextColor.fromRgb(0xE6B84D));

    private static long tooltipShownAt;
    /** The lesson of the item whose tooltip was built last; null after any other item's tooltip. */
    private static String hoveredLesson;

    private GuideClientSetup() {}

    @EventBusSubscriber(modid = "commandcookies_electronic_mod", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_GUIDE);
        }
    }

    @EventBusSubscriber(modid = "commandcookies_electronic_mod", value = Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent
        public static void tooltip(ItemTooltipEvent event) {
            if (event.getEntity() == null) return;
            // Every hovered tooltip replaces the last subject, so an unrelated item never opens a stale lesson.
            hoveredLesson = GuideLessons.subject(event.getItemStack());
            tooltipShownAt = System.nanoTime();
            if (hoveredLesson == null) return;
            var key = OPEN_GUIDE.getTranslatedKeyMessage();
            var line = Component.translatable("commandcookies_electronic_mod.guide.tooltip", key).withStyle(HINT);
            var lines = event.getToolTip();
            lines.add(Math.min(1, lines.size()), line);
        }

        @SubscribeEvent
        public static void screenKey(ScreenEvent.KeyPressed.Pre event) {
            if (!OPEN_GUIDE.isActiveAndMatches(InputConstants.getKey(event.getKeyCode(), event.getScanCode()))) return;
            var lesson = hoveredLesson;
            if (lesson == null || event.getScreen() instanceof CemGuideScreen || System.nanoTime() - tooltipShownAt > HOVER_NANOS) return;
            // Typing in a search box must not open the guide.
            if (event.getScreen().getFocused() instanceof net.minecraft.client.gui.components.EditBox) return;
            event.setCanceled(true);
            open(lesson);
        }

        @SubscribeEvent
        public static void worldKey(ClientTickEvent.Post event) {
            var mc = Minecraft.getInstance();
            while (OPEN_GUIDE.consumeClick()) {
                if (mc.screen != null || mc.player == null) continue;
                // The main hand decides; the offhand counts when the main hand holds no subject.
                var lesson = GuideLessons.subject(mc.player.getMainHandItem());
                if (lesson == null) lesson = GuideLessons.subject(mc.player.getOffhandItem());
                if (lesson != null) open(lesson);
            }
        }
    }

    private static void open(String lesson) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        tooltipShownAt = 0;
        hoveredLesson = null;
        mc.setScreen(new CemGuideScreen(mc.screen, GuideLessons.get(lesson)));
    }
}
