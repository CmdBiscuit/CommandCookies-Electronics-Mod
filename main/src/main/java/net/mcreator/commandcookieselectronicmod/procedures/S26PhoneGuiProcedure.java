package net.mcreator.commandcookieselectronicmod.procedures;

import net.minecraft.world.item.ItemStack;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems;

/** Locked custom MCreator procedure: connects all six Samsung finishes to phone apps. */
public final class S26PhoneGuiProcedure {
    public static String color(ItemStack stack) {
        if (stack.is(CommandcookiesElectronicModModItems.S_26_ULTRA.get())) return "samsung_black";
        if (stack.is(CommandcookiesElectronicModModItems.S_26_ULTRA_COBALT_VIOLET.get())) return "samsung_cobalt_violet";
        if (stack.is(CommandcookiesElectronicModModItems.S_26_ULTRA_SKY_BLUE.get())) return "samsung_sky_blue";
        if (stack.is(CommandcookiesElectronicModModItems.S_26_ULTRA_WHITE.get())) return "samsung_white";
        if (stack.is(CommandcookiesElectronicModModItems.S_26_ULTRA_PINK_GOLD.get())) return "samsung_pink_gold";
        if (stack.is(CommandcookiesElectronicModModItems.S_26_ULTRA_SILVER_SHADOW.get())) return "samsung_silver_shadow";
        return null;
    }
}
