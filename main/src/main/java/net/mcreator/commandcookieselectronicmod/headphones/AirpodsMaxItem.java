package net.mcreator.commandcookieselectronicmod.headphones;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Cosmetic wearable, rendered through Minecraft's fitted HEAD item pose. */
public class AirpodsMaxItem extends Item implements Equipable {
    public AirpodsMaxItem(){super(new Item.Properties().stacksTo(1));}
    @Override public EquipmentSlot getEquipmentSlot(){return EquipmentSlot.HEAD;}
    @Override public EquipmentSlot getEquipmentSlot(ItemStack stack){return EquipmentSlot.HEAD;}
    @Override public Holder<SoundEvent> getEquipSound(){return SoundEvents.ARMOR_EQUIP_LEATHER;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        return swapWithEquipmentSlot(this,level,player,hand);
    }
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,List<Component> lines,TooltipFlag flag){
        super.appendHoverText(stack,context,lines,flag);
        lines.add(Component.translatable("item.commandcookies_electronic_mod.airpods_max.wear").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.commandcookies_electronic_mod.airpods_max.cosmetic").withStyle(ChatFormatting.DARK_GRAY));
    }
}
