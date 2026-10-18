package net.mcreator.commandcookieselectronicmod.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModBlockEntities;

public class LaptopBlockEntity extends BlockEntity implements net.minecraft.world.WorldlyContainer {
    public final AnimationState animationState0 = new AnimationState();
    public final AnimationState animationState1 = new AnimationState();
    public boolean animationInitialized;
    public boolean lastOpen, animationInProgress;
    public int animationStartTick;
    public long nextInteraction;
    // A new ID on lid closure invalidates cached desktops, including clients that were away.
    public java.util.UUID desktopSessionId = java.util.UUID.randomUUID();
    // Persistent physical identity, separate from the lid/window session.
    public java.util.UUID deviceId;
    public java.util.UUID deviceId() {
        if(deviceId==null) {
            if(level!=null&&level.isClientSide)throw new IllegalStateException("Laptop identity not synchronized");
            deviceId=java.util.UUID.randomUUID();setChanged();
        }
        return deviceId;
    }
    public void freshDevice() { deviceId=java.util.UUID.randomUUID();resetDesktopSession(); }
    public LaptopBlockEntity(BlockPos pos, BlockState state) {
        this(CommandcookiesElectronicModModBlockEntities.LAPTOP.get(), pos, state);
    }
    protected LaptopBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    public void resetDesktopSession() { desktopSessionId = java.util.UUID.randomUUID(); setChanged(); }
    @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); tag.putUUID("DesktopSessionId", desktopSessionId);
        if(level==null||!level.isClientSide)deviceId();
        if(deviceId!=null)tag.putUUID("LaptopDeviceId",deviceId);
    }
    @Override protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); if(tag.hasUUID("DesktopSessionId")) desktopSessionId=tag.getUUID("DesktopSessionId");
        deviceId=tag.hasUUID("LaptopDeviceId")?tag.getUUID("LaptopDeviceId"):null;
    }
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    // MCreator's block-entity switch also registers a sided item capability.
    // The laptop exposes no storage slots.
    @Override public int getContainerSize() { return 0; }
    @Override public boolean isEmpty() { return true; }
    @Override public net.minecraft.world.item.ItemStack getItem(int slot) { return net.minecraft.world.item.ItemStack.EMPTY; }
    @Override public net.minecraft.world.item.ItemStack removeItem(int slot, int count) { return net.minecraft.world.item.ItemStack.EMPTY; }
    @Override public net.minecraft.world.item.ItemStack removeItemNoUpdate(int slot) { return net.minecraft.world.item.ItemStack.EMPTY; }
    @Override public void setItem(int slot, net.minecraft.world.item.ItemStack stack) {}
    @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
    @Override public void clearContent() {}
    @Override public int[] getSlotsForFace(net.minecraft.core.Direction side) { return new int[0]; }
    @Override public boolean canPlaceItemThroughFace(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return false; }
}
