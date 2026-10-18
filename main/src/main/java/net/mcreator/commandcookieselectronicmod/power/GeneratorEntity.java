package net.mcreator.commandcookieselectronicmod.power;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.commandcookieselectronicmod.block.GeneratorBlock;
/** Furnace fuels burn proportionally while this chunk ticks. 64 coal = seven real days at 20 TPS. */
public class GeneratorEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int COAL_TICKS=189000,STACK_TICKS=COAL_TICKS*64;
    private NonNullList<ItemStack> items=NonNullList.withSize(1,ItemStack.EMPTY);
    public UUID identity=UUID.randomUUID();
    public int remaining;
    public boolean enabled=true;
    public GeneratorEntity(BlockPos p,BlockState s){super(PowerRegistration.GENERATOR,p,s);}
    public boolean powered(){return enabled&&remaining>0;}
    public static int fuelTicks(ItemStack stack){int burn=stack.getBurnTime(RecipeType.SMELTING);return burn<=0?0:(int)Math.min(Integer.MAX_VALUE,Math.max(1,(long)burn*COAL_TICKS/1600));}
    public static boolean isFuel(ItemStack stack){return fuelTicks(stack)>0;}
    public int total(){return (int)Math.min(Integer.MAX_VALUE,(long)remaining+(long)fuelTicks(getItem(0))*getItem(0).getCount());}
    @Override protected Component getDefaultName(){return Component.literal("Generator");}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inv){return new GeneratorMenu(id,inv,worldPosition,this);}
    @Override public int getContainerSize(){return 1;}
    @Override public int getMaxStackSize(){return 64;}
    @Override protected NonNullList<ItemStack> getItems(){return items;}
    @Override protected void setItems(NonNullList<ItemStack> list){items=list;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==0&&isFuel(stack);}
    @Override public boolean stillValid(Player p){return Container.stillValidBlockEntity(this,p);}
    @Override public int[] getSlotsForFace(Direction side){return new int[]{0};}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return canPlaceItem(slot,stack);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return slot==0&&!stack.isEmpty()&&!isFuel(stack);}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);ContainerHelper.saveAllItems(t,items,r);t.putUUID("PowerIdentity",identity);t.putInt("FuelRemaining",remaining);t.putBoolean("Enabled",enabled);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);items=NonNullList.withSize(1,ItemStack.EMPTY);ContainerHelper.loadAllItems(t,items,r);identity=t.hasUUID("PowerIdentity")?t.getUUID("PowerIdentity"):UUID.randomUUID();remaining=Math.max(0,t.getInt(t.contains("FuelRemaining")?"FuelRemaining":"CoalRemaining"));enabled=!t.contains("Enabled")||t.getBoolean("Enabled");}
    private void consumeFuel(Level l){
        var stack=getItem(0);int ticks=fuelTicks(stack);if(ticks==0)return;
        var used=removeItem(0,1);remaining=ticks;var residue=used.getCraftingRemainingItem();
        if(!residue.isEmpty()){if(getItem(0).isEmpty())setItem(0,residue);else Containers.dropItemStack(l,worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5,residue);}
        setChanged();
    }
    public static void tick(Level l,BlockPos p,BlockState s,GeneratorEntity d){
        if(d.enabled){
            if(d.remaining==0)d.consumeFuel(l);
            if(d.remaining>0){d.remaining--;if(l.getGameTime()%20==0||d.remaining==0)d.setChanged();}
            // Queue the next fuel immediately so the supply does not flicker between pieces.
            if(d.remaining==0)d.consumeFuel(l);
        }
        if(s.getValue(GeneratorBlock.POWERED)!=d.powered())l.setBlock(p,s.setValue(GeneratorBlock.POWERED,d.powered()),3);
    }
}
