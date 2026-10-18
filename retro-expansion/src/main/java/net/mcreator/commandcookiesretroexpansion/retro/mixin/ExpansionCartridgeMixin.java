package net.mcreator.commandcookiesretroexpansion.retro.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.mcreator.commandcookiesretroexpansion.retro.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Optional targets keep NES support usable with an older main mod. */
@Pseudo
@Mixin(targets={"net.mcreator.commandcookieselectronicmod.snes.SnesDeviceBlockEntity","net.mcreator.commandcookieselectronicmod.n64.N64DeviceBlockEntity"},remap=false)
public abstract class ExpansionCartridgeMixin implements RetroConsoleState {
 @Unique private ItemStack retro$game=ItemStack.EMPTY;
 @Unique private boolean retro$romLoaded;
 @Inject(method="saveAdditional",at=@At("RETURN"))
 private void retro$save(CompoundTag tag,HolderLookup.Provider registries,CallbackInfo ci){if(!retro$game.isEmpty())tag.put("RetroCartridge",retro$game.save(registries));}
 @Inject(method="loadAdditional",at=@At("RETURN"))
 private void retro$load(CompoundTag tag,HolderLookup.Provider registries,CallbackInfo ci){retro$game=tag.contains("RetroCartridge")?ItemStack.parseOptional(registries,tag.getCompound("RetroCartridge")):ItemStack.EMPTY;retro$romLoaded=tag.getBoolean("RetroRomLoaded");}
 @Inject(method="getUpdateTag",at=@At("RETURN"))
 private void retro$display(HolderLookup.Provider registries,CallbackInfoReturnable<CompoundTag> ci){ci.getReturnValue().putBoolean("RetroRomLoaded",RetroItems.hasGame(retro$game));}
 @Override public ItemStack retro$cartridge(){return retro$game;}
 @Override public void retro$cartridge(ItemStack stack){retro$game=stack;}
 @Override public boolean retro$hasRom(){return retro$romLoaded;}
}
