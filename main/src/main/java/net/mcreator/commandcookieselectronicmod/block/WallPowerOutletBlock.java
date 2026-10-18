package net.mcreator.commandcookieselectronicmod.block;
import com.mojang.serialization.MapCodec;
public final class WallPowerOutletBlock extends PowerOutletBlock {
    public static final MapCodec<WallPowerOutletBlock> CODEC=simpleCodec(p->new WallPowerOutletBlock());
    public WallPowerOutletBlock(){super(false);}
    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec(){return CODEC;}
}
