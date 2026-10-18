package net.mcreator.commandcookieselectronicmod.block;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.commandcookieselectronicmod.block.entity.LaptopNavyBlueBlockEntity;
public class LaptopNavyBlueBlock extends LaptopBlock {
    public static final MapCodec<LaptopNavyBlueBlock> CODEC=simpleCodec(properties -> new LaptopNavyBlueBlock());
    public LaptopNavyBlueBlock() { super("laptop_navy_blue"); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new LaptopNavyBlueBlockEntity(pos,state); }
}
