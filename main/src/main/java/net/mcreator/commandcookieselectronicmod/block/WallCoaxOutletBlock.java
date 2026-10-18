package net.mcreator.commandcookieselectronicmod.block;
import com.mojang.serialization.MapCodec;
public final class WallCoaxOutletBlock extends PowerOutletBlock {
    public static final MapCodec<WallCoaxOutletBlock> CODEC=simpleCodec(p->new WallCoaxOutletBlock());
    public WallCoaxOutletBlock(){super(true);}
    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec(){return CODEC;}
}
