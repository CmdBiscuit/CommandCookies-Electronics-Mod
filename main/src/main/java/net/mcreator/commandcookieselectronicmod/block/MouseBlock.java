package net.mcreator.commandcookieselectronicmod.block;
import com.mojang.serialization.MapCodec;
import net.mcreator.commandcookieselectronicmod.pc.PcPeripheralBlock;
public final class MouseBlock extends PcPeripheralBlock {
    public static final MapCodec<MouseBlock> CODEC=simpleCodec(p->new MouseBlock());
    public MouseBlock(){super(true);}
    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec(){return CODEC;}
}
