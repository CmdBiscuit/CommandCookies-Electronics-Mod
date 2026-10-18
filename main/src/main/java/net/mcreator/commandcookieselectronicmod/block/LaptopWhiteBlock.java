package net.mcreator.commandcookieselectronicmod.block;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.commandcookieselectronicmod.block.entity.LaptopWhiteBlockEntity;
public class LaptopWhiteBlock extends LaptopBlock {
    public static final MapCodec<LaptopWhiteBlock> CODEC=simpleCodec(properties -> new LaptopWhiteBlock());
    public LaptopWhiteBlock() { super("laptop_white"); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new LaptopWhiteBlockEntity(pos,state); }
}
