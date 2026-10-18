package net.mcreator.commandcookieselectronicmod.block;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.commandcookieselectronicmod.block.entity.LaptopBlackBlockEntity;
public class LaptopBlackBlock extends LaptopBlock {
    public static final MapCodec<LaptopBlackBlock> CODEC=simpleCodec(properties -> new LaptopBlackBlock());
    public LaptopBlackBlock() { super("laptop_black"); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new LaptopBlackBlockEntity(pos,state); }
}
