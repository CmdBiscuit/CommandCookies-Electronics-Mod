package net.mcreator.commandcookieselectronicmod.procedures;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;
import net.mcreator.commandcookieselectronicmod.block.LaptopBlock;
public class LaptopIsClosedProcedure {
 public static boolean execute(LevelAccessor world,double x,double y,double z) {
  boolean open=world.getBlockState(BlockPos.containing(x,y,z)).getOptionalValue(LaptopBlock.OPEN).orElse(false);
  return !open;
 }
}
