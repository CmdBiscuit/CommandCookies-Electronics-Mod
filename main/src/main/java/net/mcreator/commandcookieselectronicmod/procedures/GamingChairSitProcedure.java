package net.mcreator.commandcookieselectronicmod.procedures;

import net.mcreator.commandcookieselectronicmod.CommandcookiesElectronicModMod;
import net.mcreator.commandcookieselectronicmod.block.GamingChairRedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** MCreator custom-code procedure. Keep this procedure's code locked. */
@EventBusSubscriber(modid = "commandcookies_electronic_mod")
public class GamingChairSitProcedure {
    private static final String SEAT = "commandcookies_electronic_mod:gaming_chair_seat";
    private static final String CHAIR_POS = "ChairPos";
    private static final double SEAT_HEIGHT = 9.3 / 16.0;

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!GamingChairDyeProcedure.isChair(event.getLevel().getBlockState(event.getPos()))
                || player.isSpectator()) return;
        // Consume both hands so held items cannot place blocks through the chair.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            BlockPos pos = event.getPos();
            if (!GamingChairDyeProcedure.execute(event.getLevel(), pos.getX(), pos.getY(), pos.getZ(), player)) {
                execute(event.getLevel(), pos.getX(), pos.getY(), pos.getZ(), player);
            }
        }
    }

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (!(world instanceof ServerLevel level) || !(entity instanceof ServerPlayer player)
                || !player.isAlive() || player.isPassenger() || player.isSleeping()
                || player.isSpectator()) return;
        BlockPos pos = BlockPos.containing(x, y, z);
        var state = level.getBlockState(pos);
        if (!GamingChairDyeProcedure.isChair(state)
                || player.distanceToSqr(Vec3.atCenterOf(pos)) > 36) return;
        Direction facing = state.getValue(GamingChairRedBlock.FACING);
        double seatX = pos.getX() + .5 + facing.getStepX() * .125;
        double seatZ = pos.getZ() + .5 + facing.getStepZ() * .125;
        // Keep the seated player clear of the backrest in every orientation.
        AABB headroom = new AABB(seatX - .3, pos.getY() + 1.001, seatZ - .3,
                seatX + .3, pos.getY() + 1.85, seatZ + .3);
        if (!level.noCollision(headroom)) return;
        for (ArmorStand seat : level.getEntitiesOfClass(ArmorStand.class, new AABB(pos).inflate(.5),
                e -> e.getPersistentData().getBoolean(SEAT))) {
            if (seat.getPersistentData().getLong(CHAIR_POS) != pos.asLong()) continue;
            if (seat.isVehicle()) return;
            seat.discard();
        }
        ArmorStand seat = new ArmorStand(level, seatX, pos.getY() + SEAT_HEIGHT, seatZ);
        CompoundTag flags = new CompoundTag();
        flags.putBoolean("Marker", true);
        flags.putBoolean("Invisible", true);
        flags.putBoolean("NoBasePlate", true);
        flags.putInt("DisabledSlots", 4144959);
        seat.readAdditionalSaveData(flags);
        seat.setInvisible(true);
        seat.setNoGravity(true);
        seat.setInvulnerable(true);
        seat.setSilent(true);
        seat.getPersistentData().putBoolean(SEAT, true);
        seat.getPersistentData().putLong(CHAIR_POS, pos.asLong());
        seat.setYRot(facing.toYRot());
        if (!level.addFreshEntity(seat)) return;
        if (!player.startRiding(seat, true)) {
            seat.discard();
            return;
        }
        player.setYRot(facing.toYRot());
        player.setYHeadRot(facing.toYRot());
        player.setYBodyRot(facing.toYRot());
        player.fallDistance = 0;
    }

    @SubscribeEvent
    public static void cleanUpSeat(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ArmorStand seat) || seat.level().isClientSide
                || !seat.getPersistentData().getBoolean(SEAT)) return;
        BlockPos pos = BlockPos.of(seat.getPersistentData().getLong(CHAIR_POS));
        if (!seat.isVehicle() || !GamingChairDyeProcedure.isChair(seat.level().getBlockState(pos))
                || seat.getPassengers().stream().anyMatch(passenger -> !passenger.isAlive())) {
            seat.ejectPassengers();
            seat.discard();
        }
    }

    @SubscribeEvent
    public static void onDismount(EntityMountEvent event) {
        Entity seat = event.getEntityBeingMounted();
        if (!event.isDismounting() || !(event.getEntityMounting() instanceof ServerPlayer player)
                || !(seat instanceof ArmorStand) || !seat.getPersistentData().getBoolean(SEAT)) return;
        ServerLevel level = player.serverLevel();
        BlockPos pos = BlockPos.of(seat.getPersistentData().getLong(CHAIR_POS));
        Direction front = Direction.fromYRot(seat.getYRot());
        // Wait until vanilla has finished dismounting before moving beside the chair.
        CommandcookiesElectronicModMod.queueServerWork(1, () -> {
            if (!player.isAlive() || player.isPassenger() || player.serverLevel() != level
                    || player.distanceToSqr(Vec3.atCenterOf(pos)) > 16) return;
            for (Direction dir : new Direction[] {front, front.getClockWise(), front.getCounterClockWise(), front.getOpposite()}) {
                for (int dy : new int[] {0, 1, -1}) {
                    BlockPos candidate = pos.relative(dir).offset(0, dy, 0);
                    if (!level.hasChunkAt(candidate)) continue;
                    Vec3 safe = DismountHelper.findSafeDismountLocation(EntityType.PLAYER, level, candidate, true);
                    if (safe != null) {
                        player.teleportTo(safe.x, safe.y, safe.z);
                        player.fallDistance = 0;
                        return;
                    }
                }
            }
            Vec3 safe = DismountHelper.findSafeDismountLocation(EntityType.PLAYER, level, pos.above(), true);
            if (safe != null) player.teleportTo(safe.x, safe.y, safe.z);
        });
    }
}
