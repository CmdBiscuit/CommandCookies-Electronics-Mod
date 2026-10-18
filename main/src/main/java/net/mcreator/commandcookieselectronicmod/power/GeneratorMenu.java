package net.mcreator.commandcookieselectronicmod.power;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModBlocks;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
public final class GeneratorMenu extends AbstractContainerMenu {
    public static final MenuType<GeneratorMenu> TYPE=IMenuTypeExtension.create(GeneratorMenu::new);
    public final BlockPos position;
    private final Container fuel;
    private final DataSlot low=DataSlot.standalone(),high=DataSlot.standalone(),flags=DataSlot.standalone();
    public GeneratorMenu(int id,Inventory inv,RegistryFriendlyByteBuf b){this(id,inv,b.readBlockPos(),new SimpleContainer(1));}
    public GeneratorMenu(int id,Inventory inv,BlockPos p,Container c){
        super(TYPE,id);position=p;fuel=c;checkContainerSize(c,1);
        addSlot(new Slot(c,0,27,61){@Override public boolean mayPlace(ItemStack s){return GeneratorEntity.isFuel(s);}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,col+row*9+9,8+col*18,145+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,203));
        addDataSlot(low);addDataSlot(high);addDataSlot(flags);sync();
    }
    private void sync(){if(fuel instanceof GeneratorEntity d){int total=d.total();low.set(total&65535);high.set(total>>>16);flags.set((d.enabled?1:0)|(d.powered()?2:0));}}
    @Override public void broadcastChanges(){sync();super.broadcastChanges();}
    public int total(){return (high.get()&65535)<<16|(low.get()&65535);}
    public boolean enabled(){return (flags.get()&1)!=0;}public boolean powered(){return (flags.get()&2)!=0;}
    @Override public boolean stillValid(Player p){return stillValid(ContainerLevelAccess.create(p.level(),position),p,CommandcookiesElectronicModModBlocks.GENERATOR.get());}
    @Override public boolean clickMenuButton(Player p,int id){if(id!=0||p.level().isClientSide||p.isSpectator()||!p.isAlive()||!stillValid(p)||!(fuel instanceof GeneratorEntity d))return false;d.enabled=!d.enabled;d.setChanged();broadcastChanges();return true;}
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var before=stack.copy();
        if(index==0){if(!moveItemStackTo(stack,1,37,true))return ItemStack.EMPTY;}
        else if(GeneratorEntity.isFuel(stack)){if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;}
        else if(index<28){if(!moveItemStackTo(stack,28,37,false))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,1,28,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        if(before.getCount()==stack.getCount())return ItemStack.EMPTY;slot.onTake(p,stack);return before;
    }
}
