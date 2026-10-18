package net.mcreator.commandcookieselectronicmod.block;
import com.mojang.serialization.MapCodec;
import net.mcreator.commandcookieselectronicmod.tv.ImageTvBlock;
public final class TvWithStandBlock extends ImageTvBlock {
    public static final MapCodec<TvWithStandBlock> CODEC=simpleCodec(p->new TvWithStandBlock());
    public TvWithStandBlock(){super(false);}
    @Override protected MapCodec<? extends ImageTvBlock> codec(){return CODEC;}
}
