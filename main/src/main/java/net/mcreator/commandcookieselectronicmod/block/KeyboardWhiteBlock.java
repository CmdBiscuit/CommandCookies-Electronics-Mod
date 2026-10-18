package net.mcreator.commandcookieselectronicmod.block;
import com.mojang.serialization.MapCodec;
import net.mcreator.commandcookieselectronicmod.pc.PcPeripheralBlock;
public final class KeyboardWhiteBlock extends PcPeripheralBlock {
    public static final MapCodec<KeyboardWhiteBlock> CODEC=simpleCodec(p->new KeyboardWhiteBlock());
    public KeyboardWhiteBlock(){super(false);}
    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec(){return CODEC;}
}
