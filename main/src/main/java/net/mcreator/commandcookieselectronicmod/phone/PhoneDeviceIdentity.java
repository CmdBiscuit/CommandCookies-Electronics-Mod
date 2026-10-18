package net.mcreator.commandcookieselectronicmod.phone;

import java.util.HashSet;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.mcreator.commandcookieselectronicmod.procedures.IphoneLogicProcedure;

/** Server-issued identity only. No app data is stored in or transmitted with the item. */
public final class PhoneDeviceIdentity {
    private static final String TAG="cem_phone_device";
    private PhoneDeviceIdentity(){}
    public static UUID id(ItemStack stack){var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();return tag.hasUUID(TAG)?tag.getUUID(TAG):null;}
    public static UUID ensure(ItemStack stack){
        if(IphoneLogicProcedure.color(stack)==null)throw new IllegalArgumentException("Not a phone");
        UUID id=id(stack);if(id==null){id=UUID.randomUUID();UUID fresh=id;CustomData.update(DataComponents.CUSTOM_DATA,stack,tag->tag.putUUID(TAG,fresh));}return id;
    }
    public static void ensureInventory(ServerPlayer player){
        var seen=new HashSet<UUID>();boolean changed=false;
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++){
            var stack=player.getInventory().getItem(slot);if(IphoneLogicProcedure.color(stack)==null)continue;
            UUID id=id(stack);
            if(id==null||!seen.add(id)){UUID fresh=UUID.randomUUID();CustomData.update(DataComponents.CUSTOM_DATA,stack,tag->tag.putUUID(TAG,fresh));seen.add(fresh);changed=true;}
        }
        if(changed){player.getInventory().setChanged();player.containerMenu.broadcastChanges();if(player.containerMenu!=player.inventoryMenu)player.inventoryMenu.broadcastChanges();}
    }
}
