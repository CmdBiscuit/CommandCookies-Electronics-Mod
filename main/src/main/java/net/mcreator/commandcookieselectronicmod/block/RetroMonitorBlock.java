package net.mcreator.commandcookieselectronicmod.block;
import com.mojang.serialization.MapCodec;
import net.mcreator.commandcookieselectronicmod.nes.NesDeviceBlock;
public final class RetroMonitorBlock extends NesDeviceBlock {
    public static final MapCodec<RetroMonitorBlock> CODEC=simpleCodec(p->new RetroMonitorBlock());
    public RetroMonitorBlock(){super(true);}
    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec(){return CODEC;}
}
