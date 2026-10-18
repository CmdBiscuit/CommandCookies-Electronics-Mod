package net.mcreator.commandcookieselectronicmod.guide;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.mcreator.commandcookieselectronicmod.guide.GuideCue.Action;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModBlocks;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems;
import net.mcreator.commandcookieselectronicmod.power.PowerCables;

import static net.mcreator.commandcookieselectronicmod.guide.GuideLesson.GOOD;
import static net.mcreator.commandcookieselectronicmod.guide.GuideLesson.TARGET;
import static net.mcreator.commandcookieselectronicmod.guide.GuideLesson.WARN;
import static net.mcreator.commandcookieselectronicmod.guide.GuideStage.*;

/**
 * Lessons for the power network itself, following PowerNetwork, GeneratorEntity/Menu and
 * PowerCables: generators feed outlets within 120 blocks, outlets feed compatible devices within
 * 15 blocks, power outlets hold two cords and coax outlets one.
 */
final class PowerLessons {
    private PowerLessons() {}

    private static final java.util.function.Supplier<ItemStack> WRENCH = item(CommandcookiesElectronicModModItems.WRENCH);

    /** Placing, fuelling, pausing and wiring the generator. */
    static GuideLesson generatorLesson() {
        var b = GuideLesson.builder(GuideLessons.GENERATOR).timeline(920, 0, 90, 260, 430, 740).milestones(190, 575, 855);
        var g = b.block(new BlockPos(2, 1, 3), 25, generator(false)).then(190, generator(true)).then(300, generator(false)).then(380, generator(true));
        var p = b.block(new BlockPos(1, 1, 5), 440, outlet(false)).then(575, outlet(true));
        var c = b.block(new BlockPos(4, 1, 5), 455, coaxOutlet(false)).then(695, coaxOutlet(true));
        var nes = b.block(new BlockPos(3, 1, 4), 745, state(CommandcookiesElectronicModModBlocks.NES_CONSOLE));
        nes.then(855, powered(nes.state(), true));

        // Step 1: place it.
        b.cue(10, 45, top(g), Action.RIGHT_CLICK, item(CommandcookiesElectronicModModItems.GENERATOR));
        // Step 2: open the menu and fuel it.
        b.cue(95, 40, top(g), Action.RIGHT_CLICK, null);
        b.cue(140, 50, top(g), Action.INSERT_FUEL, () -> new ItemStack(Items.COAL));
        b.highlight(190, 50, box(g), GOOD).burst(190, box(g));
        // Step 3: Pause stops it without using fuel; Start resumes.
        b.cue(270, 40, top(g), Action.MENU_BUTTON, null, "pause");
        b.highlight(300, 40, box(g), WARN);
        b.cue(340, 45, top(g), Action.MENU_BUTTON, null, "start");
        b.highlight(380, 40, box(g), GOOD).burst(380, box(g));
        // Step 4: wrench the generator, then each outlet; both are in range.
        int feed = link(b, 480, WRENCH, top(g), box(g), plate(p.pos()), box(p));
        b.highlight(500, 65, box(c), TARGET);
        b.cord(PowerCables.FEED, g.pos(), generator(true), 0, p.pos(), outlet(true), feed, feed + 30);
        b.burst(feed + 30, box(p));
        feed = link(b, 600, WRENCH, top(g), box(g), plate(c.pos()), box(c));
        b.cord(PowerCables.FEED, g.pos(), generator(true), 0, c.pos(), coaxOutlet(true), feed, feed + 30);
        b.burst(feed + 30, box(c));
        // Step 5: a device plugs into an outlet.
        int cord = link(b, 760, WRENCH, plate(p.pos()), box(p), top(nes), box(nes));
        b.cord(PowerCables.POWER, p.pos(), outlet(true), 0, nes.pos(), nes.state(), cord, cord + 30);
        b.highlight(cord + 30, 60, box(nes), GOOD).burst(cord + 30, box(nes));
        return b.build();
    }

    /** Selecting, linking, clearing and disconnecting with the wrench. */
    static GuideLesson wrenchLesson() {
        var b = GuideLesson.builder(GuideLessons.WRENCH).timeline(1000, 0, 150, 400, 660, 830).milestones(0, 255, 615);
        var g = b.block(new BlockPos(1, 1, 3), 0, generator(true)).fixed();
        var p = b.block(new BlockPos(2, 1, 5), 0, outlet(false)).fixed().then(255, outlet(true)).then(770, outlet(false)).then(935, outlet(true));
        var c = b.block(new BlockPos(4, 1, 5), 0, coaxOutlet(false)).fixed().then(365, coaxOutlet(true));
        var crt = b.block(new BlockPos(3, 1, 4), 0, state(CommandcookiesElectronicModModBlocks.CRT_TV)).fixed();
        var crtOn = powered(crt.state(), true);
        crt.then(615, crtOn).then(700, crt.state());

        // Step 1: select a source; clicking it again clears the selection.
        b.cue(15, 40, top(g), Action.RIGHT_CLICK, WRENCH);
        b.highlight(15, 80, box(g), GuideLesson.SOURCE);
        b.highlight(35, 60, box(p), TARGET).highlight(35, 60, box(c), TARGET);
        b.cue(85, 40, top(g), Action.RIGHT_CLICK, WRENCH, "clear_selection");
        // Step 2: generator to each outlet.
        int t = link(b, 160, WRENCH, top(g), box(g), plate(p.pos()), box(p));
        b.highlight(180, 65, box(c), TARGET);
        b.cord(PowerCables.FEED, g.pos(), generator(true), 0, p.pos(), outlet(true), t, t + 30).removed(770);
        b.burst(t + 30, box(p));
        t = link(b, 270, WRENCH, top(g), box(g), plate(c.pos()), box(c));
        b.cord(PowerCables.FEED, g.pos(), generator(true), 0, c.pos(), coaxOutlet(true), t, t + 30);
        b.burst(t + 30, box(c));
        // Step 3: outlet to device, power and then coax.
        t = link(b, 410, WRENCH, plate(p.pos()), box(p), top(crt), box(crt));
        b.cord(PowerCables.POWER, p.pos(), outlet(true), 0, crt.pos(), crtOn, t, t + 30).removed(700);
        t = link(b, 520, WRENCH, plate(c.pos()), box(c), top(crt), box(crt));
        b.cord(PowerCables.COAX, c.pos(), coaxOutlet(true), 0, crt.pos(), crtOn, t, t + 30).removed(700).signal();
        b.burst(t + 30, box(crt));
        // Step 4: sneak-click a device to unplug it, an outlet to cut its feed.
        b.cue(670, 45, top(crt), Action.SNEAK_RIGHT_CLICK, WRENCH);
        b.highlight(700, 40, box(crt), WARN);
        b.cue(740, 45, plate(p.pos()), Action.SNEAK_RIGHT_CLICK, WRENCH);
        b.highlight(770, 40, box(p), WARN);
        // Step 5: the outlet is reconnected; every endpoint the wrench accepts is shown.
        t = link(b, 840, WRENCH, top(g), box(g), plate(p.pos()), box(p));
        b.cord(PowerCables.FEED, g.pos(), generator(true), 0, p.pos(), outlet(true), t, t + 30);
        b.highlight(t + 30, 60, box(g), GOOD).highlight(t + 30, 60, box(p), GOOD).highlight(t + 30, 60, box(c), GOOD).highlight(t + 30, 60, box(crt), TARGET);
        return b.build();
    }

    /** A power outlet: wall mounting, generator feed and its two sockets. */
    static GuideLesson powerOutletLesson() {
        var b = GuideLesson.builder(GuideLessons.POWER_OUTLET).timeline(880, 0, 130, 280, 440, 600, 760).milestones(0, 235, 395);
        var g = b.block(new BlockPos(0, 1, 3), 0, generator(true)).fixed();
        var p = b.block(new BlockPos(3, 1, 5), 40, outlet(false)).then(235, outlet(true));
        var tvPos = new BlockPos(1, 1, 4);
        var tv = state(CommandcookiesElectronicModModBlocks.TV_WITH_STAND);
        var tvBox = GuideLesson.box(tv, tvPos);
        var tvFace = DisplayLessons.standTvFace(tvPos);
        var nes = b.block(new BlockPos(4, 1, 4), 445, state(CommandcookiesElectronicModModBlocks.NES_CONSOLE));
        nes.then(555, powered(nes.state(), true));
        var n64 = b.block(new BlockPos(5, 1, 4), 605, state(CommandcookiesElectronicModModBlocks.N64_CONSOLE));

        // Step 1: mount it on a wall and check it.
        b.cue(15, 40, plate(p.pos()), Action.RIGHT_CLICK, item(CommandcookiesElectronicModModItems.WALL_POWER_OUTLET));
        b.cue(70, 40, plate(p.pos()), Action.RIGHT_CLICK, null, "outlet_status");
        // Step 2: feed it from the generator.
        int t = link(b, 140, WRENCH, top(g), box(g), plate(p.pos()), box(p));
        b.cord(PowerCables.FEED, g.pos(), generator(true), 0, p.pos(), outlet(true), t, t + 30);
        b.burst(t + 30, box(p));
        // Step 3: the first device takes the upper socket.
        b.tv(tvPos, tv, 285, 395, GuideLesson.NEVER, GuideLesson.NEVER);
        t = link(b, 300, WRENCH, plate(p.pos()), box(p), tvFace, tvBox);
        b.cord(PowerCables.POWER, p.pos(), outlet(true), 0, tvPos, tv, t, t + 30);
        b.burst(t + 30, tvBox);
        // Step 4: the second device takes the lower socket.
        t = link(b, 460, WRENCH, plate(p.pos()), box(p), top(nes), box(nes));
        b.cord(PowerCables.POWER, p.pos(), outlet(true), 1, nes.pos(), nes.state(), t, t + 30);
        b.burst(t + 30, box(nes));
        // Step 5: a third device is refused; the outlet is full.
        link(b, 620, WRENCH, plate(p.pos()), box(p), top(n64), box(n64));
        b.highlight(665, 70, box(n64), WARN).highlight(665, 70, box(p), WARN);
        // Step 6: what can be plugged in.
        b.highlight(770, 90, tvBox, GOOD).highlight(770, 90, box(nes), GOOD).highlight(770, 90, box(p), GOOD);
        return b.build();
    }

    /** A coax outlet: generator feed, its single coax socket and the displays that accept it. */
    static GuideLesson coaxOutletLesson() {
        var b = GuideLesson.builder(GuideLessons.COAX_OUTLET).timeline(740, 0, 120, 270, 420, 570).milestones(0, 0, 525);
        var mains = mains(b, new BlockPos(0, 1, 3), new BlockPos(2, 1, 5), null);
        var g = mains.generator();
        var p = mains.power();
        var c = b.block(new BlockPos(4, 1, 5), 40, coaxOutlet(false)).then(225, coaxOutlet(true));
        var crt = b.block(new BlockPos(3, 1, 4), 275, state(CommandcookiesElectronicModModBlocks.CRT_TV));
        var crtOn = powered(crt.state(), true);
        crt.then(525, crtOn);
        var monitor = b.block(new BlockPos(5, 1, 4), 575, state(CommandcookiesElectronicModModBlocks.RETRO_MONITOR));

        // Step 1: mount it.
        b.cue(15, 40, plate(c.pos()), Action.RIGHT_CLICK, item(CommandcookiesElectronicModModItems.WALL_COAX_OUTLET));
        // Step 2: coax outlets are fed by the generator too.
        int t = link(b, 130, WRENCH, top(g), box(g), plate(c.pos()), box(c));
        b.cord(PowerCables.FEED, g.pos(), generator(true), 0, c.pos(), coaxOutlet(true), t, t + 30);
        b.burst(t + 30, box(c));
        // Step 3: coax to the CRT TV; it stays dark without wall power.
        t = link(b, 285, WRENCH, plate(c.pos()), box(c), top(crt), box(crt));
        b.cord(PowerCables.COAX, c.pos(), coaxOutlet(true), 0, crt.pos(), crtOn, t, t + 30).signal();
        // Step 4: a power cord from the power outlet; now it turns on.
        t = link(b, 430, WRENCH, plate(p.pos()), box(p), top(crt), box(crt));
        b.cord(PowerCables.POWER, p.pos(), outlet(true), 0, crt.pos(), crtOn, t, t + 30);
        b.highlight(t + 30, 60, box(crt), GOOD).burst(t + 30, box(crt));
        // Step 5: the single coax socket is taken; the second display needs its own outlet.
        link(b, 590, WRENCH, plate(c.pos()), box(c), top(monitor), box(monitor));
        b.highlight(635, 70, box(monitor), WARN).highlight(635, 70, box(c), WARN);
        return b.build();
    }
}
