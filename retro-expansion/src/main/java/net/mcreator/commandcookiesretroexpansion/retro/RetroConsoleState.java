package net.mcreator.commandcookiesretroexpansion.retro;
import net.minecraft.world.item.ItemStack;
/** Addon-owned cartridge storage and the small marker synchronized to clients. */
public interface RetroConsoleState extends RetroDisplayState {
 ItemStack retro$cartridge();
 void retro$cartridge(ItemStack stack);
}
