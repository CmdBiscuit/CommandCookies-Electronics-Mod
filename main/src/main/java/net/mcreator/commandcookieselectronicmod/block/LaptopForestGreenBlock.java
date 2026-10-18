package net.mcreator.commandcookieselectronicmod.block;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.commandcookieselectronicmod.block.entity.LaptopForestGreenBlockEntity;
public class LaptopForestGreenBlock extends LaptopBlock {
    public static final MapCodec<LaptopForestGreenBlock> CODEC=simpleCodec(properties -> new LaptopForestGreenBlock());
    public LaptopForestGreenBlock() { super("laptop_forest_green"); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new LaptopForestGreenBlockEntity(pos,state); }
}
