package net.mcreator.commandcookieselectronicmod.block;
import com.mojang.serialization.MapCodec;
import net.mcreator.commandcookieselectronicmod.pc.PcPeripheralBlock;
public final class KeyboardBlock extends PcPeripheralBlock {
    public static final MapCodec<KeyboardBlock> CODEC=simpleCodec(p->new KeyboardBlock());
    public KeyboardBlock(){super(false);}
    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec(){return CODEC;}
}
