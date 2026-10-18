package net.mcreator.commandcookieselectronicmod.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModBlocks;

/** Recipe browser with the player's inventory; no crafting input or output slots. */
public class WorkbenchMenu extends AbstractContainerMenu {
    public static final MenuType<WorkbenchMenu> TYPE=IMenuTypeExtension.create(WorkbenchMenu::new);
    public final BlockPos position;
    private final DataSlot craftSequence=DataSlot.standalone(),craftResult=DataSlot.standalone(),lastRecipe=DataSlot.standalone();
    public int craftSequence(){return craftSequence.get();}
    public int craftResult(){return craftResult.get();}
    public int lastRecipe(){return lastRecipe.get();}
    public WorkbenchMenu(int id,Inventory inv,RegistryFriendlyByteBuf buf) {
        this(id,inv,buf.readBlockPos(),new SimpleContainer(16));
    }
    public WorkbenchMenu(int id,Inventory inv,BlockPos pos,Container legacy) {
        super(TYPE,id);position=pos;addDataSlot(craftSequence);addDataSlot(craftResult);lastRecipe.set(-1);addDataSlot(lastRecipe);
        checkContainerSize(legacy,16);
        // Recover old grid contents on the server exactly once. A full inventory drops the remainder.
        if(!inv.player.level().isClientSide) {
            for(int i=0;i<legacy.getContainerSize();i++) {
                ItemStack stack=legacy.removeItemNoUpdate(i);
                if(!stack.isEmpty())inv.placeItemBackInInventory(stack);
            }
            legacy.setChanged();
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,col+row*9+9,185+col*18,151+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,185+col*18,209));
    }
    @Override public boolean clickMenuButton(Player player,int index) {
        if(player.level().isClientSide||player.isSpectator()||!player.isAlive()||!stillValid(player)||index<0||index>=WorkbenchRecipes.all().size())return false;
        var plan=WorkbenchRecipes.plan(player.getInventory(),WorkbenchRecipes.all().get(index));
        lastRecipe.set(index);craftResult.set(plan.status()+1);craftSequence.set((craftSequence.get()+1)&32767);
        if(plan.status()!=WorkbenchRecipes.READY){broadcastChanges();return false;}
        for(int i=0;i<plan.inventory().size();i++)player.getInventory().setItem(i,plan.inventory().get(i));
        player.getInventory().setChanged();broadcastChanges();
        player.level().playSound(null,position,net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE,net.minecraft.sounds.SoundSource.BLOCKS,.35f,1.4f);
        return true;
    }
    @Override public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(player.level(),position),player,CommandcookiesElectronicModModBlocks.ELECTRONICS_WORKBENCH.get());
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),copy=stack.copy();
        if(index<27){if(!moveItemStackTo(stack,27,36,false))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,0,27,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        if(stack.getCount()==copy.getCount())return ItemStack.EMPTY;
        slot.onTake(player,stack);return copy;
    }
}
