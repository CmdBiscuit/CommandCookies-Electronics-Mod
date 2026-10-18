package net.mcreator.commandcookieselectronicmod.block;
import com.mojang.serialization.MapCodec;
import net.mcreator.commandcookieselectronicmod.tv.ImageTvBlock;
public final class TvWallMountedBlock extends ImageTvBlock {
    public static final MapCodec<TvWallMountedBlock> CODEC=simpleCodec(p->new TvWallMountedBlock());
    public TvWallMountedBlock(){super(true);}
    @Override protected MapCodec<? extends ImageTvBlock> codec(){return CODEC;}
}
