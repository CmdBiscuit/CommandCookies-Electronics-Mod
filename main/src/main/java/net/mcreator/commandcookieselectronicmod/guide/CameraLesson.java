package net.mcreator.commandcookieselectronicmod.guide;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.mcreator.commandcookieselectronicmod.guide.GuideCue.Action;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModBlocks;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems;

import static net.mcreator.commandcookieselectronicmod.guide.GuideLesson.GOOD;
import static net.mcreator.commandcookieselectronicmod.guide.GuideLesson.TARGET;
import static net.mcreator.commandcookieselectronicmod.guide.GuideStage.*;

/**
 * The security camera, following SecurityCameraBlock/Entity and SecurityCameraService: it is
 * battery powered (two AA batteries for 15 real-world days), never wired to an outlet, and is
 * paired by right-clicking it with a phone. The guide shows no camera feed.
 */
final class CameraLesson {
    private CameraLesson() {}

    static GuideLesson camera() {
        var b = GuideLesson.builder(GuideLessons.CAMERA).size(5).wall(4, 3, 10).timeline(620, 0, 100, 210, 330, 470).milestones(GuideLesson.NEVER, GuideLesson.NEVER, 160);
        var off = state(CommandcookiesElectronicModModBlocks.SECURITY_CAMERA);
        var camera = b.block(new BlockPos(2, 2, 3), 40, off).then(160, powered(off, true));
        var lens = front(camera);

        // Step 1: mount it on the side of a solid block.
        b.cue(15, 45, new Vec3(2.5, 2.5, 3.98), Action.RIGHT_CLICK, item(CommandcookiesElectronicModModItems.SECURITY_CAMERA));
        // Step 2: two AA batteries go in.
        var batteries = item(CommandcookiesElectronicModModItems.AA_BATTERY);
        b.item(batteries, new Vec3(2.35, 1.7, 1.6), 105, 165).moveTo(130, 30, centre(camera)).scale(.3f);
        b.item(batteries, new Vec3(2.65, 1.7, 1.6), 105, 165).moveTo(135, 30, centre(camera)).scale(.3f);
        b.cue(110, 50, lens, Action.RIGHT_CLICK, batteries);
        b.highlight(160, 50, box(camera), GOOD).burst(160, box(camera));
        // Step 3: no cables; an empty-hand click reports the battery time left.
        b.cue(230, 55, lens, Action.RIGHT_CLICK, null, "battery_status");
        b.highlight(230, 55, box(camera), TARGET);
        // Step 4: pair a phone.
        var phone = item(CommandcookiesElectronicModModItems.IPHONE_18_PRO_MAX);
        var hand = new Vec3(2.5, 1.75, 1.3);
        b.item(phone, hand, 335, 620).scale(.4f);
        b.cue(345, 55, lens, Action.RIGHT_CLICK, phone);
        b.beam(370, 60, hand, lens, TARGET);
        b.burst(400, box(camera));
        // Step 5: the phone's Security app.
        b.cue(480, 60, hand, Action.RIGHT_CLICK_AIR, phone, "security_app");
        b.beam(500, 90, lens, hand, GOOD);
        return b.build();
    }
}
