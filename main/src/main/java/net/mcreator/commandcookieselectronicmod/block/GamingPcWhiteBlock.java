package net.mcreator.commandcookieselectronicmod.block;
import com.mojang.serialization.MapCodec;
import net.mcreator.commandcookieselectronicmod.pc.PcDeviceBlock;
public final class GamingPcWhiteBlock extends PcDeviceBlock {
    public static final MapCodec<GamingPcWhiteBlock> CODEC=simpleCodec(p->new GamingPcWhiteBlock());
    public GamingPcWhiteBlock(){super(false);}
    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec(){return CODEC;}
}
