package net.mcreator.commandcookieselectronicmod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.mcreator.commandcookieselectronicmod.block.entity.LaptopBlockEntity;

public class LaptopBlock extends BaseEntityBlock {
    public static final MapCodec<LaptopBlock> CODEC = simpleCodec(properties -> new LaptopBlock());
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private final net.minecraft.resources.ResourceLocation texture;
    public net.minecraft.resources.ResourceLocation texture() { return texture; }
    public LaptopBlock() { this("laptop_graphite"); }
    protected LaptopBlock(String textureName) {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.5f, 4f).noOcclusion().isRedstoneConductor((s,w,p)->false));
        texture=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod","textures/block/"+textureName+".png");
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, OPEN); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite()); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LaptopBlockEntity(pos, state); }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity entity,net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level,pos,state,entity,stack);
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof LaptopBlockEntity laptop) {
            laptop.freshDevice();level.sendBlockUpdated(pos,state,state,3);
        }
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            if (state.getValue(OPEN)) {
                if(!level.isClientSide&&player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                    &&player.isAlive()&&!player.isSpectator()&&player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))<=64
                    &&level.getBlockEntity(pos) instanceof LaptopBlockEntity laptop) {
                    var device=laptop.deviceId();level.sendBlockUpdated(pos,state,state,3);
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer,
                        new net.mcreator.commandcookieselectronicmod.network.LaptopOpenResponse(pos,device,laptop.desktopSessionId));
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (level.isClientSide) player.displayClientMessage(net.minecraft.network.chat.Component.literal("Crouch + right-click to open the laptop lid."), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LaptopBlockEntity laptop) {
            if (level.getGameTime() < laptop.nextInteraction) return InteractionResult.CONSUME;
            laptop.nextInteraction = level.getGameTime() + 12;
            if (state.getValue(OPEN)) laptop.resetDesktopSession();
            level.setBlock(pos, state.cycle(OPEN), 3);
            if (state.getValue(OPEN)) level.sendBlockUpdated(pos, state, state.cycle(OPEN), 3);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), net.minecraft.sounds.SoundSource.BLOCKS, .25f, state.getValue(OPEN) ? .8f : 1.1f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock()))net.mcreator.commandcookieselectronicmod.power.PowerNetwork.forget(level,pos);super.onRemove(state,level,pos,next,moving);}
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = state.getValue(OPEN)
            ? Shapes.or(box(1,0,4,15,2.2,14), box(1,2.2,.5,15,10.45,5))
            : box(1,0,4,15,2.2,14);
        int turns = switch(state.getValue(FACING)) {case NORTH->2; case EAST->3; case SOUTH->0; default->1;};
        for (int i=0;i<turns;i++) {
            VoxelShape[] rotated={Shapes.empty()};
            shape.forAllBoxes((x1,y1,z1,x2,y2,z2)->rotated[0]=Shapes.or(rotated[0], Shapes.box(1-z2,y1,x1,1-z1,y2,x2)));
            shape=rotated[0];
        }
        return shape;
    }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return rotate(state, mirror.getRotation(state.getValue(FACING))); }
}
