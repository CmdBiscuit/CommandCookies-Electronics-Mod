package net.mcreator.commandcookiesretroexpansion.retro.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.mcreator.commandcookieselectronicmod.nes.NesDeviceBlockEntity;
import net.mcreator.commandcookiesretroexpansion.retro.RetroConsoleState;
import net.mcreator.commandcookiesretroexpansion.retro.RetroItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=NesDeviceBlockEntity.class,remap=false)
public abstract class NesDisplayMixin implements RetroConsoleState {
    @Shadow public ItemStack cartridge;
    @Unique private boolean retro$romLoaded;

    @Inject(method="getUpdateTag",at=@At("RETURN"))
    private void retro$syncDisplay(HolderLookup.Provider registries,CallbackInfoReturnable<CompoundTag> result) {
        result.getReturnValue().putBoolean("RetroRomLoaded",RetroItems.is(cartridge));
    }

    @Inject(method="loadAdditional",at=@At("RETURN"))
    private void retro$readDisplay(CompoundTag tag,HolderLookup.Provider registries,CallbackInfo result) {
        retro$romLoaded=tag.getBoolean("RetroRomLoaded");
    }

    @Override public boolean retro$hasRom(){return retro$romLoaded;}
    @Override public ItemStack retro$cartridge(){return cartridge;}
    @Override public void retro$cartridge(ItemStack stack){cartridge=stack;}
}
