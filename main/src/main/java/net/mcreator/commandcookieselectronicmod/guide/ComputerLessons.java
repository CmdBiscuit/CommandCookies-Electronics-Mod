package net.mcreator.commandcookieselectronicmod.guide;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.mcreator.commandcookieselectronicmod.guide.GuideCue.Action;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModBlocks;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems;
import net.mcreator.commandcookieselectronicmod.pc.MonitorAttachments;
import net.mcreator.commandcookieselectronicmod.power.PowerCables;

import static net.mcreator.commandcookieselectronicmod.guide.GuideLesson.GOOD;
import static net.mcreator.commandcookieselectronicmod.guide.GuideLesson.WARN;
import static net.mcreator.commandcookieselectronicmod.guide.GuideStage.*;

/**
 * Computer lessons, following PcSystem and MonitorAttachments: a gaming PC and a gaming monitor
 * each need their own wall power, one HDMI cable links them (16 blocks), and right-clicking the
 * monitor opens BlockOS. Keyboard and mouse items attach to the monitor as optional model parts.
 * Laptops need no power or cables.
 */
final class ComputerLessons {
    private ComputerLessons() {}

    private static final Supplier<ItemStack> WRENCH = item(CommandcookiesElectronicModModItems.WRENCH);
    private static final Supplier<ItemStack> HDMI = item(CommandcookiesElectronicModModItems.HDMI_CABLE);

    static GuideLesson pc() {
        var b = GuideLesson.builder(GuideLessons.PC).timeline(900, 0, 110, 250, 500, 650, 770).milestones(0, 0, 605);
        var mains = mains(b, new BlockPos(0, 1, 3), new BlockPos(3, 1, 5), null);
        var p = mains.power();
        var bare = state(CommandcookiesElectronicModModBlocks.GAMING_MONITOR);
        var keyboard = bare.setValue(MonitorAttachments.KEYBOARD, 1);
        var desk = keyboard.setValue(MonitorAttachments.MOUSE, 1);
        var monitor = b.block(new BlockPos(2, 1, 4), 45, bare).then(150, keyboard).then(200, desk).then(605, powered(desk, true)).then(805, desk);
        var towerOff = state(CommandcookiesElectronicModModBlocks.GAMING_PC);
        var tower = b.block(new BlockPos(4, 1, 4), 25, towerOff).then(605, powered(towerOff, true)).then(805, towerOff);

        // Step 1: place both.
        b.cue(10, 40, top(tower), Action.RIGHT_CLICK, item(CommandcookiesElectronicModModItems.GAMING_PC));
        b.cue(55, 40, top(monitor), Action.RIGHT_CLICK, item(CommandcookiesElectronicModModItems.GAMING_MONITOR));
        // Step 2: keyboard, then mouse, attach to the monitor's desk model.
        b.cue(120, 40, front(monitor), Action.RIGHT_CLICK, item(CommandcookiesElectronicModModItems.KEYBOARD));
        b.cue(170, 40, front(monitor), Action.RIGHT_CLICK, item(CommandcookiesElectronicModModItems.MOUSE));
        b.highlight(200, 40, box(monitor), GOOD);
        // Step 3: each device gets its own wall power; both sockets of one outlet are used.
        int t = link(b, 260, WRENCH, plate(p.pos()), box(p), top(tower), box(tower));
        b.cord(PowerCables.POWER, p.pos(), outlet(true), 0, tower.pos(), towerOff, t, t + 30);
        t = link(b, 365, WRENCH, plate(p.pos()), box(p), front(monitor), box(monitor));
        b.cord(PowerCables.POWER, p.pos(), outlet(true), 1, monitor.pos(), desk, t, t + 30);
        // Step 4: HDMI, PC first, then monitor; both screens come on.
        t = link(b, 510, HDMI, top(tower), box(tower), front(monitor), box(monitor));
        b.cord(PowerCables.HDMI, tower.pos(), towerOff, 0, monitor.pos(), desk, t, t + 30).removed(805).signal();
        b.burst(t + 30, box(monitor)).burst(t + 30, box(tower));
        // Step 5: open BlockOS from the monitor.
        b.cue(660, 60, front(monitor), Action.RIGHT_CLICK, null, "blockos");
        b.highlight(660, 60, box(monitor), GOOD);
        // Step 6: crouch-click with HDMI to unlink.
        b.cue(780, 50, front(monitor), Action.SNEAK_RIGHT_CLICK, HDMI);
        b.highlight(805, 45, box(monitor), WARN).highlight(805, 45, box(tower), WARN);
        return b.build();
    }

    /** The HDMI cable on its own, with the white PC and monitor already on wall power. */
    static GuideLesson hdmi() {
        var b = GuideLesson.builder(GuideLessons.HDMI).timeline(650, 0, 100, 270, 380, 480).milestones(0, 0, 330);
        var mains = mains(b, new BlockPos(0, 1, 3), new BlockPos(3, 1, 5), null);
        var monitorOff = state(CommandcookiesElectronicModModBlocks.GAMING_MONITOR_WHITE);
        var towerOff = state(CommandcookiesElectronicModModBlocks.GAMING_PC_WHITE);
        var monitor = b.block(new BlockPos(2, 1, 4), 0, monitorOff).fixed().then(330, powered(monitorOff, true)).then(415, monitorOff).then(585, powered(monitorOff, true));
        var tower = b.block(new BlockPos(4, 1, 4), 0, towerOff).fixed().then(330, powered(towerOff, true)).then(415, towerOff).then(585, powered(towerOff, true));
        prewired(b, PowerCables.POWER, mains.power(), 0, tower, towerOff);
        prewired(b, PowerCables.POWER, mains.power(), 1, monitor, monitorOff);

        // Step 1: the cable.
        var held = new Vec3(3.5, 1.8, 3.2);
        b.item(HDMI, held, 15, 95).scale(.4f);
        b.cue(20, 60, held, Action.HOLD, HDMI);
        // Step 2: select one device; unlinked devices of the other kind light up.
        b.cue(110, 45, top(tower), Action.RIGHT_CLICK, HDMI);
        b.highlight(110, 150, box(tower), GuideLesson.SOURCE);
        b.highlight(135, 125, box(monitor), GuideLesson.TARGET);
        // Step 3: the other device; both turn on.
        b.cue(280, 40, front(monitor), Action.RIGHT_CLICK, HDMI);
        b.cord(PowerCables.HDMI, tower.pos(), towerOff, 0, monitor.pos(), monitorOff, 300, 330).removed(415).signal();
        b.burst(330, box(monitor)).burst(330, box(tower));
        // Step 4: crouch-click to unlink; the cable drops back.
        b.cue(390, 50, front(monitor), Action.SNEAK_RIGHT_CLICK, HDMI);
        b.highlight(415, 45, box(monitor), WARN).highlight(415, 45, box(tower), WARN);
        // Step 5: relinked; HDMI carries the picture, the wall cords carry power.
        int t = link(b, 490, HDMI, top(tower), box(tower), front(monitor), box(monitor));
        b.cord(PowerCables.HDMI, tower.pos(), towerOff, 0, monitor.pos(), monitorOff, t, t + 30).signal();
        b.highlight(t + 30, 60, box(mains.power()), GOOD);
        return b.build();
    }

}
