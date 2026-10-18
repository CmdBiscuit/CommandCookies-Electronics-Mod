package net.mcreator.commandcookiesretroexpansion.retro.mixin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.commandcookiesretroexpansion.retro.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets={"net.mcreator.commandcookieselectronicmod.block.SnesConsoleBlock","net.mcreator.commandcookieselectronicmod.block.N64ConsoleBlock"},remap=false)
public abstract class ExpansionRemovalMixin {
 @Inject(method="onRemove",at=@At("HEAD"))
 private void retro$ejectOnBreak(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving,CallbackInfo ci){
  if(level.isClientSide||state.is(next.getBlock()))return;var c=RetroConsoles.at(level,pos);if(c==null)return;
  RetroSessions.releaseConsole(level.getServer(),c.identity(),"Console removed.");var cartridge=c.cartridge().copy();c.cartridge(ItemStack.EMPTY);if(!cartridge.isEmpty())Block.popResource(level,pos,cartridge);
 }
}
