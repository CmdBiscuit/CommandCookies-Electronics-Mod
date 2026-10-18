package net.mcreator.commandcookieselectronicmod.block;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.commandcookieselectronicmod.block.entity.LaptopBurgundyBlockEntity;
public class LaptopBurgundyBlock extends LaptopBlock {
    public static final MapCodec<LaptopBurgundyBlock> CODEC=simpleCodec(properties -> new LaptopBurgundyBlock());
    public LaptopBurgundyBlock() { super("laptop_burgundy"); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new LaptopBurgundyBlockEntity(pos,state); }
}
