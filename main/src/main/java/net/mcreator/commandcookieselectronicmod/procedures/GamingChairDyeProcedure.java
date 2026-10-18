package net.mcreator.commandcookieselectronicmod.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Reusable MCreator custom-code procedure; keep code locked. */
public class GamingChairDyeProcedure {
    private static final String MODID = "commandcookies_electronic_mod";

    public static boolean isChair(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!id.getNamespace().equals(MODID)) return false;
        for (DyeColor color : DyeColor.values()) {
            if (id.getPath().equals("gaming_chair_" + color.getName())) return true;
        }
        return false;
    }

    /** True means a dye was held: consume the interaction instead of sitting. */
    public static boolean execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (!(entity instanceof Player player)) return false;
        ItemStack stack = player.getMainHandItem().getItem() instanceof DyeItem
                ? player.getMainHandItem() : player.getOffhandItem();
        if (!(stack.getItem() instanceof DyeItem dye)) return false;
        if (!(world instanceof ServerLevel level) || player.isSpectator()
                || !player.getAbilities().mayBuild) return true;
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockState current = level.getBlockState(pos);
        if (!isChair(current) || !level.mayInteract(player, pos)
                || player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) > 36) return true;
        Block target = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MODID,
                "gaming_chair_" + dye.getDyeColor().getName()));
        if (current.is(target) || !isChair(target.defaultBlockState())) return true;
        BlockState replacement = target.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                current.getValue(HorizontalDirectionalBlock.FACING));
        // Swapping block state keeps facing and the existing invisible seat. No drops are spawned.
        if (level.setBlock(pos, replacement, Block.UPDATE_ALL) && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return true;
    }
}
