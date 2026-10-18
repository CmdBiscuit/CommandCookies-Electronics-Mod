package net.mcreator.commandcookieselectronicmod.client;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.mcreator.commandcookieselectronicmod.block.LaptopBlock;
import net.mcreator.commandcookieselectronicmod.block.entity.LaptopBlockEntity;
import net.mcreator.commandcookieselectronicmod.pc.*;
/** Private app profiles belong to a physical computer; a monitor is only its screen. */
public record DesktopAccess(UUID device,UUID session) {
    public static DesktopAccess at(Level level,BlockPos pos){if(level==null||!level.hasChunkAt(pos))return null;var e=level.getBlockEntity(pos);if(e instanceof LaptopBlockEntity laptop&&laptop.deviceId!=null&&laptop.getBlockState().getBlock() instanceof LaptopBlock&&laptop.getBlockState().getValue(LaptopBlock.OPEN))return new DesktopAccess(laptop.deviceId,laptop.desktopSessionId);if(e instanceof PcDeviceEntity monitor&&monitor.monitor()&&monitor.getBlockState().getValue(PcDeviceBlock.POWERED)){var pc=PcSystem.peer(monitor);if(pc!=null&&!pc.monitor()&&pc.getBlockState().getValue(PcDeviceBlock.POWERED))return new DesktopAccess(pc.identity,pc.desktopSession);}return null;}
}
