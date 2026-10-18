package net.mcreator.commandcookieselectronicmod.guide;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.mcreator.commandcookieselectronicmod.guide.GuideCue.Action;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModBlocks;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems;
import net.mcreator.commandcookieselectronicmod.power.PowerCables;

import static net.mcreator.commandcookieselectronicmod.guide.GuideLesson.GOOD;
import static net.mcreator.commandcookieselectronicmod.guide.GuideLesson.WARN;
import static net.mcreator.commandcookieselectronicmod.guide.GuideStage.*;

/**
 * Console lessons. The main mod links NES, SNES and N64 consoles to a retro monitor (NesSystem,
 * SnesSystem, N64System) and plays its own NES Tetris cartridge; the optional Retro Expansion
 * (RetroSessions, RetroItems) adds ROM cartridges for all three, filled from the player's own
 * local game files. The guide only shows the items; it never opens a library or emulator.
 */
final class RetroLessons {
    private RetroLessons() {}

    private static final Supplier<ItemStack> WRENCH = item(CommandcookiesElectronicModModItems.WRENCH);
    private static final Supplier<ItemStack> AV = item(CommandcookiesElectronicModModItems.AV_CABLE);

    /**
     * One console's full setup: place, power both devices, AV link, cartridge, play, eject.
     *
     * @param cartridge the cartridge shown being inserted
     * @param romLibrary whether the cartridge first needs a local game chosen (Retro Expansion)
     */
    private static GuideLesson console(String id, Supplier<? extends Block> block, Supplier<? extends ItemLike> consoleItem,
                                       Supplier<? extends ItemLike> controllerItem, Supplier<ItemStack> cartridge, boolean romLibrary) {
        boolean expansion = net.neoforged.fml.ModList.get().isLoaded(GuideStage.ADDON);
        boolean decorative = romLibrary && !expansion;
        var b = GuideLesson.builder(id)
            .lang(decorative || id.equals(GuideLessons.NES) && !expansion ? id + "_base" : id)
            .timeline(decorative ? 750 : 1040, decorative ? new int[]{0, 110, 450, 590} : new int[]{0, 110, 450, 590, 740, 880})
            .milestones(0, 0, 555);
        var mains = mains(b, new BlockPos(0, 1, 3), new BlockPos(3, 1, 5), new BlockPos(2, 1, 5));
        var p = mains.power();
        var c = mains.coax();
        var off = state(CommandcookiesElectronicModModBlocks.RETRO_MONITOR);
        var idle = powered(off, true);
        var monitor = b.block(new BlockPos(2, 1, 4), 25, off).then(420, idle);
        var console = b.block(new BlockPos(4, 1, 4), 45, state(block));
        var consoleOn = powered(console.state(), true);
        console.then(210, consoleOn);

        // Step 1: place the monitor and the console.
        b.cue(10, 40, top(monitor), Action.RIGHT_CLICK, item(CommandcookiesElectronicModModItems.RETRO_MONITOR));
        b.cue(55, 40, top(console), Action.RIGHT_CLICK, item(consoleItem));
        // Step 2: wall power to the console; wall power and coax to the monitor.
        int t = link(b, 120, WRENCH, plate(p.pos()), box(p), top(console), box(console));
        b.cord(PowerCables.POWER, p.pos(), outlet(true), 0, console.pos(), consoleOn, t, t + 25);
        t = link(b, 225, WRENCH, plate(p.pos()), box(p), front(monitor), box(monitor));
        b.cord(PowerCables.POWER, p.pos(), outlet(true), 1, monitor.pos(), idle, t, t + 25);
        t = link(b, 330, WRENCH, plate(c.pos()), box(c), front(monitor), box(monitor));
        b.cord(PowerCables.COAX, c.pos(), coaxOutlet(true), 0, monitor.pos(), idle, t, t + 25).signal();
        b.burst(420, box(monitor));
        // Step 3: AV cable from the console to the monitor.
        t = link(b, 460, AV, top(console), box(console), front(monitor), box(monitor));
        b.cord(PowerCables.AV, console.pos(), consoleOn, 0, monitor.pos(), idle, t, t + 30).removed(decorative ? 640 : 975).signal();
        b.burst(t + 30, box(monitor));
        if (decorative) {
            // The base mod has no SNES/N64 game library or play action.
            b.cue(605, 55, front(monitor), Action.SNEAK_RIGHT_CLICK, AV);
            b.highlight(640, 70, box(monitor), WARN);
            return b.build();
        }
        // Step 4: the cartridge (a ROM cartridge first gets a local game chosen in the air).
        var above = top(console).add(0, .55, 0);
        var slot = top(console).add(0, .04, 0);
        int insert = 600;
        if (romLibrary) {
            b.item(cartridge, above, 600, 700).moveTo(670, 25, slot).scale(.35f);
            b.cue(600, 55, above, Action.RIGHT_CLICK_AIR, cartridge, "choose_game");
            insert = 660;
        } else b.item(cartridge, above, 600, 650).moveTo(615, 30, slot).scale(.35f);
        b.cue(insert, 45, top(console), Action.RIGHT_CLICK, cartridge);
        b.highlight(insert + 25, 45, box(console), GOOD);
        // Step 5: hold the matching controller and use the monitor.
        var controller = item(controllerItem);
        var hand = new Vec3(2.5, 1.85, 3.25);
        b.item(controller, hand, 745, 880).scale(.4f);
        b.cue(750, 40, hand, Action.HOLD, controller);
        b.cue(795, 50, front(monitor), Action.RIGHT_CLICK, controller);
        b.highlight(795, 50, box(monitor), GOOD);
        // Step 6: crouch-click the console to eject; crouch-click with the AV cable to unplug.
        b.cue(890, 45, top(console), Action.SNEAK_RIGHT_CLICK, null, "eject");
        b.item(cartridge, slot, 905, 950).moveTo(905, 20, above).scale(.35f);
        b.cue(950, 45, front(monitor), Action.SNEAK_RIGHT_CLICK, AV);
        b.highlight(975, 40, box(monitor), WARN);
        return b.build();
    }

    static GuideLesson nes() {
        return console(GuideLessons.NES, CommandcookiesElectronicModModBlocks.NES_CONSOLE, CommandcookiesElectronicModModItems.NES_CONSOLE,
            CommandcookiesElectronicModModItems.NES_CONTROLLER,
            net.neoforged.fml.ModList.get().isLoaded(GuideStage.ADDON) ? addon("rom_cartridge") : item(CommandcookiesElectronicModModItems.NES_TETRIS_CARTRIDGE),
            net.neoforged.fml.ModList.get().isLoaded(GuideStage.ADDON));
    }

    static GuideLesson snes() {
        return console(GuideLessons.SNES, CommandcookiesElectronicModModBlocks.SNES_CONSOLE, CommandcookiesElectronicModModItems.SNES_CONSOLE,
            CommandcookiesElectronicModModItems.SNES_CONTROLLER, addon("snes_rom_cartridge"), true);
    }

    static GuideLesson n64() {
        return console(GuideLessons.N64, CommandcookiesElectronicModModBlocks.N64_CONSOLE, CommandcookiesElectronicModModItems.N64_CONSOLE,
            CommandcookiesElectronicModModItems.N64_CONTROLLER, addon("n64_rom_cartridge"), true);
    }

    /** The AV cable on its own: select, link, unplug and relink a SNES and a retro monitor. */
    static GuideLesson av() {
        var b = GuideLesson.builder(GuideLessons.AV).timeline(650, 0, 100, 270, 380, 480).milestones(0, 0, 330);
        var mains = mains(b, new BlockPos(0, 1, 3), new BlockPos(3, 1, 5), new BlockPos(2, 1, 5));
        var idle = powered(state(CommandcookiesElectronicModModBlocks.RETRO_MONITOR), true);
        var monitor = b.block(new BlockPos(2, 1, 4), 0, idle).fixed();
        var snes = b.block(new BlockPos(4, 1, 4), 0, powered(state(CommandcookiesElectronicModModBlocks.SNES_CONSOLE), true)).fixed();
        prewired(b, PowerCables.POWER, mains.power(), 0, snes, snes.state());
        prewired(b, PowerCables.POWER, mains.power(), 1, monitor, idle);
        prewired(b, PowerCables.COAX, mains.coax(), 0, monitor, idle);

        // Step 1: the cable.
        var held = new Vec3(3.5, 1.8, 3.2);
        b.item(AV, held, 15, 95).scale(.4f);
        b.cue(20, 60, held, Action.HOLD, AV);
        // Steps 2-3: either end first, then the other.
        b.cue(110, 45, top(snes), Action.RIGHT_CLICK, AV);
        b.highlight(110, 150, box(snes), GuideLesson.SOURCE);
        b.highlight(135, 125, box(monitor), GuideLesson.TARGET);
        b.cue(280, 40, front(monitor), Action.RIGHT_CLICK, AV);
        b.cord(PowerCables.AV, snes.pos(), snes.state(), 0, monitor.pos(), idle, 300, 330).removed(415).signal();
        b.burst(330, box(monitor));
        // Step 4: crouch-click either end to unplug.
        b.cue(390, 50, top(snes), Action.SNEAK_RIGHT_CLICK, AV);
        b.highlight(415, 45, box(snes), WARN);
        // Step 5: relinked; the wall cords stay separate.
        int t = link(b, 490, AV, top(snes), box(snes), front(monitor), box(monitor));
        b.cord(PowerCables.AV, snes.pos(), snes.state(), 0, monitor.pos(), idle, t, t + 30).signal();
        b.highlight(t + 30, 60, box(mains.power()), GOOD).highlight(t + 30, 60, box(mains.coax()), GOOD);
        return b.build();
    }
}
