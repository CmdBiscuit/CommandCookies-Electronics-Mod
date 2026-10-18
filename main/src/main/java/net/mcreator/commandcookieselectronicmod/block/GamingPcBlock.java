package net.mcreator.commandcookieselectronicmod.block;
import com.mojang.serialization.MapCodec;
import net.mcreator.commandcookieselectronicmod.pc.PcDeviceBlock;
public final class GamingPcBlock extends PcDeviceBlock {
    public static final MapCodec<GamingPcBlock> CODEC=simpleCodec(p->new GamingPcBlock());
    public GamingPcBlock() {super(false);}
    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec(){return CODEC;}
}
