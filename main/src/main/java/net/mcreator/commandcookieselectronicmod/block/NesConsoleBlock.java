package net.mcreator.commandcookieselectronicmod.block;
import com.mojang.serialization.MapCodec;
import net.mcreator.commandcookieselectronicmod.nes.NesDeviceBlock;
public final class NesConsoleBlock extends NesDeviceBlock {
    public static final MapCodec<NesConsoleBlock> CODEC=simpleCodec(p->new NesConsoleBlock());
    public NesConsoleBlock(){super(false);}
    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec(){return CODEC;}
}
