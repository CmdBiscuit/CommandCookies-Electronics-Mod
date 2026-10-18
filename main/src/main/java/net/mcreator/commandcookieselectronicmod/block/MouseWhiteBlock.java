package net.mcreator.commandcookieselectronicmod.block;
import com.mojang.serialization.MapCodec;
import net.mcreator.commandcookieselectronicmod.pc.PcPeripheralBlock;
public final class MouseWhiteBlock extends PcPeripheralBlock {
    public static final MapCodec<MouseWhiteBlock> CODEC=simpleCodec(p->new MouseWhiteBlock());
    public MouseWhiteBlock(){super(true);}
    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec(){return CODEC;}
}
