package net.mcreator.commandcookieselectronicmod.block;
import com.mojang.serialization.MapCodec;
import net.mcreator.commandcookieselectronicmod.pc.PcDeviceBlock;
import net.mcreator.commandcookieselectronicmod.pc.MonitorAttachments;
public final class GamingMonitorBlock extends PcDeviceBlock {
    public static final MapCodec<GamingMonitorBlock> CODEC=simpleCodec(p->new GamingMonitorBlock());
    public GamingMonitorBlock() {super(true);}
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block,net.minecraft.world.level.block.state.BlockState> builder){super.createBlockStateDefinition(builder);builder.add(MonitorAttachments.KEYBOARD,MonitorAttachments.MOUSE);}
    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec(){return CODEC;}
}
