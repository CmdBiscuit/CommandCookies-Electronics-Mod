package net.mcreator.commandcookieselectronicmod.guide;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems;

import static net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems.*;

/**
 * Every setup lesson and the items that open each one. Lessons are built lazily on the client the
 * first time one is opened, after registries are frozen, and are immutable afterwards. Retro
 * Expansion ROM cartridges are matched by registry name, so the main mod needs no addon classes.
 */
public final class GuideLessons {
    public static final String WALL_TV = "wall_tv", STAND_TV = "stand_tv", CRT_TV = "crt_tv", RETRO_MONITOR = "retro_monitor",
        GENERATOR = "generator", WRENCH = "wrench", POWER_OUTLET = "power_outlet", COAX_OUTLET = "coax_outlet",
        CAMERA = "camera", PC = "pc", HDMI = "hdmi", AV = "av", NES = "nes", SNES = "snes", N64 = "n64";

    /** Lesson ids in the order they are listed. */
    public static final List<String> IDS = List.of(WALL_TV, STAND_TV, CRT_TV, RETRO_MONITOR, GENERATOR, WRENCH, POWER_OUTLET,
        COAX_OUTLET, CAMERA, PC, HDMI, AV, NES, SNES, N64);

    private static final Map<String, String> ADDON_SUBJECTS = Map.of("rom_cartridge", NES, "snes_rom_cartridge", SNES, "n64_rom_cartridge", N64);

    private static Map<String, GuideLesson> lessons;
    private static Map<Item, String> subjects;

    private GuideLessons() {}

    public static GuideLesson get(String id) {
        var lesson = lessons().get(id);
        if (lesson == null) throw new IllegalArgumentException("Unknown setup guide " + id);
        return lesson;
    }

    private static Map<String, GuideLesson> lessons() {
        if (lessons == null) {
            var built = new LinkedHashMap<String, GuideLesson>();
            for (var lesson : List.of(PowerTvScene.lesson(), DisplayLessons.standTv(), DisplayLessons.crtTv(), DisplayLessons.retroMonitor(),
                PowerLessons.generatorLesson(), PowerLessons.wrenchLesson(), PowerLessons.powerOutletLesson(), PowerLessons.coaxOutletLesson(), CameraLesson.camera(),
                ComputerLessons.pc(), ComputerLessons.hdmi(), RetroLessons.av(), RetroLessons.nes(),
                RetroLessons.snes(), RetroLessons.n64()))
                built.put(lesson.id, lesson);
            if (!built.keySet().equals(new java.util.LinkedHashSet<>(IDS))) throw new IllegalStateException("Setup guide ids out of sync");
            lessons = java.util.Collections.unmodifiableMap(built);
        }
        return lessons;
    }

    /** The lesson an item opens, or null when it has none. */
    public static String subject(ItemStack stack) {
        if (stack.isEmpty()) return null;
        var id = subjects().get(stack.getItem());
        if (id != null) return id;
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key.getNamespace().equals(GuideStage.ADDON) ? ADDON_SUBJECTS.get(key.getPath()) : null;
    }

    /** All main-mod items mapped to a lesson, for tests and the inventory report. */
    public static Map<Item, String> subjects() {
        if (subjects == null) {
            var map = new LinkedHashMap<Item, String>();
            put(map, WALL_TV, TV_WALL_MOUNTED);
            put(map, STAND_TV, TV_WITH_STAND);
            put(map, CRT_TV, CommandcookiesElectronicModModItems.CRT_TV);
            put(map, RETRO_MONITOR, CommandcookiesElectronicModModItems.RETRO_MONITOR);
            put(map, GENERATOR, CommandcookiesElectronicModModItems.GENERATOR);
            put(map, WRENCH, CommandcookiesElectronicModModItems.WRENCH);
            put(map, POWER_OUTLET, WALL_POWER_OUTLET);
            put(map, COAX_OUTLET, WALL_COAX_OUTLET);
            // Camera and battery items open the pairing lesson; phones have no guide shortcut.
            put(map, CAMERA, SECURITY_CAMERA, AA_BATTERY);
            put(map, PC, GAMING_PC, GAMING_PC_WHITE, GAMING_MONITOR, GAMING_MONITOR_WHITE, KEYBOARD, KEYBOARD_WHITE, MOUSE, MOUSE_WHITE);
            put(map, HDMI, HDMI_CABLE);
            put(map, AV, AV_CABLE);
            put(map, NES, NES_CONSOLE, NES_CONTROLLER, NES_TETRIS_CARTRIDGE);
            put(map, SNES, SNES_CONSOLE, SNES_CONTROLLER);
            put(map, N64, N64_CONSOLE, N64_CONTROLLER);
            subjects = java.util.Collections.unmodifiableMap(map);
        }
        return subjects;
    }

    @SafeVarargs
    private static void put(Map<Item, String> map, String lesson, Supplier<? extends ItemLike>... items) {
        for (var item : items) map.put(item.get().asItem(), lesson);
    }
}
