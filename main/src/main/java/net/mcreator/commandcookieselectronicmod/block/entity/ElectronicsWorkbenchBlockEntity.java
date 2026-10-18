package net.mcreator.commandcookieselectronicmod.block.entity;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModBlockEntities;
import net.mcreator.commandcookieselectronicmod.inventory.WorkbenchMenu;

public class ElectronicsWorkbenchBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    private NonNullList<ItemStack> items=NonNullList.withSize(16,ItemStack.EMPTY);
    public ElectronicsWorkbenchBlockEntity(BlockPos pos,BlockState state) {
        super(CommandcookiesElectronicModModBlockEntities.ELECTRONICS_WORKBENCH.get(),pos,state);
    }
    @Override protected Component getDefaultName() { return Component.translatable("block.commandcookies_electronic_mod.electronics_workbench"); }
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inv) { return new WorkbenchMenu(id,inv,worldPosition,this); }
    @Override public int getContainerSize() { return 16; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> list) { items=list; }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries); tag.putInt("GridWidth",4); ContainerHelper.saveAllItems(tag,items,registries);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries);
        items=NonNullList.withSize(16,ItemStack.EMPTY);
        if(tag.getInt("GridWidth")==4) {
            ContainerHelper.loadAllItems(tag,items,registries);
        } else {
            // Keep each old 3x3 ingredient in the same row and column after expansion.
            var old=NonNullList.withSize(9,ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag,old,registries);
            for(int row=0;row<3;row++) for(int col=0;col<3;col++) items.set(row*4+col,old.get(row*3+col));
        }
    }
    @Override public boolean stillValid(Player p) { return Container.stillValidBlockEntity(this,p); }
    // This bench is manually operated. Automation can be enabled separately later.
    @Override public int[] getSlotsForFace(Direction side) { return new int[0]; }
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side) { return false; }
}
