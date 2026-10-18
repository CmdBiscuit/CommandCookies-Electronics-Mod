package net.mcreator.commandcookieselectronicmod.pc;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems;

/** Static decorative model parts, stored in the monitor's block state. */
public final class MonitorAttachments {
    public static final IntegerProperty KEYBOARD=IntegerProperty.create("keyboard",0,2);
    public static final IntegerProperty MOUSE=IntegerProperty.create("mouse",0,2);
    private MonitorAttachments(){}

    private static int color(ItemStack stack,boolean mouse){
        if(stack.is(mouse?CommandcookiesElectronicModModItems.MOUSE.get():CommandcookiesElectronicModModItems.KEYBOARD.get()))return 1;
        if(stack.is(mouse?CommandcookiesElectronicModModItems.MOUSE_WHITE.get():CommandcookiesElectronicModModItems.KEYBOARD_WHITE.get()))return 2;
        return 0;
    }
    private static Item item(boolean mouse,int color){
        return mouse?(color==2?CommandcookiesElectronicModModItems.MOUSE_WHITE.get():CommandcookiesElectronicModModItems.MOUSE.get()):
            (color==2?CommandcookiesElectronicModModItems.KEYBOARD_WHITE.get():CommandcookiesElectronicModModItems.KEYBOARD.get());
    }
    private static boolean allowed(Player player,BlockPos pos){
        return player instanceof ServerPlayer&&player.isAlive()&&!player.isSpectator()&&player.distanceToSqr(Vec3.atCenterOf(pos))<=64;
    }
    private static void refund(Player player,Item item){
        var stack=new ItemStack(item);
        if(!player.addItem(stack))player.drop(stack,false);
    }
    public static boolean interact(Level level,BlockPos pos,Player player,InteractionHand hand){
        if(!(level.getBlockState(pos).getBlock() instanceof PcDeviceBlock block)||!block.monitor)return false;
        var stack=player.getItemInHand(hand);
        if(attach(stack,level,pos,player))return true;
        if(stack.isEmpty()&&hand==InteractionHand.MAIN_HAND&&attach(player.getOffhandItem(),level,pos,player))return true;
        return stack.isEmpty()&&player.getMainHandItem().isEmpty()&&player.getOffhandItem().isEmpty()&&remove(level,pos,player);
    }
    public static boolean attach(ItemStack stack,Level level,BlockPos pos,Player player){
        int color=color(stack,false);boolean mouse=false;
        if(color==0){color=color(stack,true);mouse=true;}
        if(color==0)return false;
        if(level.isClientSide)return true;
        if(!allowed(player,pos)||!(level.getBlockEntity(pos) instanceof PcDeviceEntity device)||!device.monitor())return true;
        var state=level.getBlockState(pos);var property=mouse?MOUSE:KEYBOARD;
        int old=state.getValue(property);
        if(old==color){player.displayClientMessage(Component.literal((mouse?"Mouse":"Keyboard")+" already attached."),true);return true;}
        boolean wasPaid=mouse?device.paidMouseAttachment:device.paidKeyboardAttachment;
        boolean paid=!player.getAbilities().instabuild;
        level.setBlock(pos,state.setValue(property,color),3);
        if(mouse)device.paidMouseAttachment=paid;else device.paidKeyboardAttachment=paid;
        if(paid)stack.shrink(1);
        device.changed();
        if(!mouse)net.mcreator.commandcookieselectronicmod.power.PowerCables.refreshPorts(level,pos);
        if(old!=0&&wasPaid)refund(player,item(mouse,old));
        player.displayClientMessage(Component.literal((mouse?"Mouse":"Keyboard")+" attached. Crouch-right-click with an empty hand to remove decorations."),true);
        return true;
    }
    public static boolean remove(Level level,BlockPos pos,Player player){
        var state=level.getBlockState(pos);
        if(!player.isShiftKeyDown()||!state.hasProperty(KEYBOARD)||(state.getValue(KEYBOARD)==0&&state.getValue(MOUSE)==0))return false;
        if(level.isClientSide)return true;
        if(!allowed(player,pos)||!(level.getBlockEntity(pos) instanceof PcDeviceEntity device))return true;
        boolean mouse=state.getValue(MOUSE)!=0;var property=mouse?MOUSE:KEYBOARD;
        int color=state.getValue(property);boolean paid=mouse?device.paidMouseAttachment:device.paidKeyboardAttachment;
        level.setBlock(pos,state.setValue(property,0),3);
        if(mouse)device.paidMouseAttachment=false;else device.paidKeyboardAttachment=false;
        device.changed();
        if(!mouse)net.mcreator.commandcookieselectronicmod.power.PowerCables.refreshPorts(level,pos);
        if(paid)refund(player,item(mouse,color));
        player.displayClientMessage(Component.literal((mouse?"Mouse":"Keyboard")+" removed."),true);
        return true;
    }
    public static void drop(BlockState state,Level level,BlockPos pos){
        if(level.isClientSide||!state.hasProperty(KEYBOARD)||!(level.getBlockEntity(pos) instanceof PcDeviceEntity device))return;
        int keyboard=state.getValue(KEYBOARD),mouse=state.getValue(MOUSE);
        // Clear the ownership flags before spawning, so nested removal cannot refund twice.
        boolean keyPaid=device.paidKeyboardAttachment,mousePaid=device.paidMouseAttachment;
        device.paidKeyboardAttachment=false;device.paidMouseAttachment=false;
        if(keyboard!=0&&keyPaid)Block.popResource(level,pos,new ItemStack(item(false,keyboard)));
        if(mouse!=0&&mousePaid)Block.popResource(level,pos,new ItemStack(item(true,mouse)));
    }
}
