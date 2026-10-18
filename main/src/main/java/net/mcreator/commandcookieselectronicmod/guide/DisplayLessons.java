package net.mcreator.commandcookieselectronicmod.guide;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.mcreator.commandcookieselectronicmod.guide.GuideCue.Action;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModBlocks;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems;
import net.mcreator.commandcookieselectronicmod.nes.NesDeviceBlock;
import net.mcreator.commandcookieselectronicmod.power.PowerCables;

import static net.mcreator.commandcookieselectronicmod.guide.GuideLesson.GOOD;
import static net.mcreator.commandcookieselectronicmod.guide.GuideLesson.WARN;
import static net.mcreator.commandcookieselectronicmod.guide.GuideStage.*;

/**
 * Lessons for displays other than the wall TV. The TV with Stand needs only wall power and
 * takes an Imgur image; the CRT TV and the retro monitor need wall power and coax (PowerNetwork
 * requiresCoax); only the retro monitor takes an AV cable from a console.
 */
final class DisplayLessons {
    private DisplayLessons() {}

    private static final java.util.function.Supplier<ItemStack> WRENCH = item(CommandcookiesElectronicModModItems.WRENCH);

    /** Middle of the stand TV's screen, from TvGeometry's stand panel for a north-facing anchor. */
    static Vec3 standTvFace(BlockPos tv) {
        return new Vec3(tv.getX() + 1, tv.getY() + .77, tv.getZ() + .36);
    }

    static GuideLesson standTv() {
        var b = GuideLesson.builder(GuideLessons.STAND_TV).timeline(540, 0, 110, 260, 420).milestones(0, 0, 215);
        var mains = mains(b, new BlockPos(6, 1, 3), new BlockPos(5, 1, 5), null);
        var p = mains.power();
        var pos = new BlockPos(2, 1, 4);
        var tv = state(CommandcookiesElectronicModModBlocks.TV_WITH_STAND);
        var tvBox = GuideLesson.box(tv, pos);
        var face = standTvFace(pos);
        // The screen is black until an image is applied at 345; the cord is pulled at 455.
        b.tv(pos, tv, 35, 215, 455, 345);

        // Step 1: place it on a two-block-wide surface.
        b.cue(10, 45, new Vec3(pos.getX() + 1, pos.getY(), pos.getZ() + .4), Action.RIGHT_CLICK, item(CommandcookiesElectronicModModItems.TV_WITH_STAND));
        // Step 2: outlet, then TV.
        int t = link(b, 120, WRENCH, plate(p.pos()), box(p), face, tvBox);
        b.cord(PowerCables.POWER, p.pos(), outlet(true), 0, pos, tv, t, t + 30).removed(455);
        b.burst(t + 30, tvBox);
        // Step 3: the image menu.
        b.cue(270, 55, face, Action.RIGHT_CLICK, null, "imgur_link");
        b.cue(330, 45, face, Action.MENU_BUTTON, null, "apply_close");
        b.highlight(345, 50, tvBox, GOOD);
        // Step 4: unplug.
        b.cue(430, 50, face, Action.SNEAK_RIGHT_CLICK, WRENCH);
        b.highlight(455, 45, tvBox, WARN);
        return b.build();
    }

    static GuideLesson crtTv() {
        var b = GuideLesson.builder(GuideLessons.CRT_TV).timeline(520, 0, 100, 250, 400).milestones(0, 0, 355);
        var mains = mains(b, new BlockPos(0, 1, 3), new BlockPos(2, 1, 5), new BlockPos(4, 1, 5));
        var p = mains.power();
        var c = mains.coax();
        var crt = b.block(new BlockPos(3, 1, 4), 30, state(CommandcookiesElectronicModModBlocks.CRT_TV));
        var crtOn = powered(crt.state(), true);
        crt.then(355, crtOn).then(435, crt.state());

        b.cue(10, 45, top(crt), Action.RIGHT_CLICK, item(CommandcookiesElectronicModModItems.CRT_TV));
        // Step 2: wall power alone leaves it dark.
        int t = link(b, 110, WRENCH, plate(p.pos()), box(p), top(crt), box(crt));
        b.cord(PowerCables.POWER, p.pos(), outlet(true), 0, crt.pos(), crtOn, t, t + 30).removed(435);
        b.highlight(t + 35, 45, box(crt), WARN);
        // Step 3: coax as well; now the screen lights.
        t = link(b, 260, WRENCH, plate(c.pos()), box(c), top(crt), box(crt));
        b.cord(PowerCables.COAX, c.pos(), coaxOutlet(true), 0, crt.pos(), crtOn, t, t + 30).removed(435).signal();
        b.highlight(t + 30, 60, box(crt), GOOD).burst(t + 30, box(crt));
        // Step 4: one sneak-click unplugs both cords.
        b.cue(410, 50, top(crt), Action.SNEAK_RIGHT_CLICK, WRENCH);
        b.highlight(435, 45, box(crt), WARN);
        return b.build();
    }

    static GuideLesson retroMonitor() {
        var b = GuideLesson.builder(GuideLessons.RETRO_MONITOR).lang(net.neoforged.fml.ModList.get().isLoaded(GuideStage.ADDON) ? "retro_monitor" : "retro_monitor_base").timeline(780, 0, 100, 350, 620).milestones(0, 0, 315);
        var mains = mains(b, new BlockPos(0, 1, 3), new BlockPos(3, 1, 5), new BlockPos(2, 1, 5));
        var p = mains.power();
        var c = mains.coax();
        var off = state(CommandcookiesElectronicModModBlocks.RETRO_MONITOR);
        var idle = powered(off, true).setValue(NesDeviceBlock.GAME_LOADED, false);
        var monitor = b.block(new BlockPos(2, 1, 4), 30, off).then(315, idle);
        var nes = b.block(new BlockPos(4, 1, 4), 355, state(CommandcookiesElectronicModModBlocks.NES_CONSOLE));
        nes.then(465, powered(nes.state(), true));

        b.cue(10, 45, top(monitor), Action.RIGHT_CLICK, item(CommandcookiesElectronicModModItems.RETRO_MONITOR));
        // Step 2: power cord and coax cable.
        int t = link(b, 110, WRENCH, plate(p.pos()), box(p), front(monitor), box(monitor));
        b.cord(PowerCables.POWER, p.pos(), outlet(true), 0, monitor.pos(), idle, t, t + 30);
        t = link(b, 220, WRENCH, plate(c.pos()), box(c), front(monitor), box(monitor));
        b.cord(PowerCables.COAX, c.pos(), coaxOutlet(true), 0, monitor.pos(), idle, t, t + 30).signal();
        b.burst(t + 30, box(monitor));
        // Step 3: a powered console and the AV cable.
        t = link(b, 370, WRENCH, plate(p.pos()), box(p), top(nes), box(nes));
        b.cord(PowerCables.POWER, p.pos(), outlet(true), 1, nes.pos(), nes.state(), t, t + 30);
        t = link(b, 480, item(CommandcookiesElectronicModModItems.AV_CABLE), top(nes), box(nes), front(monitor), box(monitor));
        b.cord(PowerCables.AV, nes.pos(), nes.state(), 0, monitor.pos(), idle, t, t + 30).signal();
        b.burst(t + 30, box(monitor));
        // Step 4: hold the console's controller and use the monitor.
        var controller = item(CommandcookiesElectronicModModItems.NES_CONTROLLER);
        var hand = new Vec3(2.5, 1.85, 3.25);
        b.item(controller, hand, 625, 780).scale(.4f);
        b.cue(630, 50, hand, Action.HOLD, controller);
        b.cue(690, 60, front(monitor), Action.RIGHT_CLICK, controller);
        b.highlight(690, 60, box(monitor), GOOD);
        return b.build();
    }
}
