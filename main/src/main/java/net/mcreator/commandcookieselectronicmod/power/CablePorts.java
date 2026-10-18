package net.mcreator.commandcookieselectronicmod.power;
import net.minecraft.core.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.mcreator.commandcookieselectronicmod.block.*;
import net.mcreator.commandcookieselectronicmod.nes.NesDeviceBlock;
import net.mcreator.commandcookieselectronicmod.pc.PcDeviceBlock;
import net.mcreator.commandcookieselectronicmod.pc.MonitorAttachments;
/** Ports measured from the installed models, in model pixels. Outlets use a south-facing base. */
public final class CablePorts {
 public static final int GENERATOR=0,OUTLET=1,COAX=2,PC=3,MONITOR=4,NES=5,RETRO=6,TV=7,FEED=8,COAX_FEED=9,SPLITTER=10,RETRO_COAX=11,TV_COAX=12,NES_AV=13,RETRO_AV=14,PC_HDMI=15,MONITOR_HDMI=16,SNES=17,SNES_AV=18,N64=19,N64_AV=20,CAMERA=21,STAND_TV=22,WALL_TV=23;
 public record Port(Vec3 point,Vec3 normal,int kind,int slot,double compression){
  public Port(Vec3 point,Vec3 normal,int kind,int slot){this(point,normal,kind,slot,1);}
  public double depthScale(){return (kind==PC_HDMI?.7:1)*compression;}
  public Vec3 exit(){return point.add(normal.scale(kind==FEED||kind==COAX_FEED?.1125*compression:.2*depthScale()));}
 }
 public static int kind(BlockState s){var b=s.getBlock();return b instanceof net.mcreator.commandcookieselectronicmod.tv.ImageTvBlock tv?(tv.wall?WALL_TV:STAND_TV):b instanceof GeneratorBlock?GENERATOR:b instanceof PowerOutletBlock o?(o.coax?COAX:OUTLET):b instanceof PcDeviceBlock pc?(pc.monitor?MONITOR:PC):b instanceof SecurityCameraBlock?CAMERA:b instanceof N64ConsoleBlock?N64:b instanceof SnesConsoleBlock?SNES:b instanceof NesDeviceBlock n?(n.monitor?RETRO:NES):TV;}
 private static int positionBits(BlockState s){return s.hasProperty(MonitorAttachments.KEYBOARD)&&s.getValue(MonitorAttachments.KEYBOARD)>0?1<<9:0;}
 public static int spec(BlockState s,int slot,boolean feed,boolean split){int k=kind(s);if(feed)k=k==COAX?COAX_FEED:FEED;if(split&&k==COAX)k=SPLITTER;return k|(s.getValue(BlockStateProperties.HORIZONTAL_FACING).get3DDataValue()<<5)|(slot<<8)|positionBits(s)|neutralAngles(k);}
 public static int connectionSpec(BlockState s,int slot,int mode,boolean target,boolean split){int k=kind(s);if(mode==PowerCables.FEED)return spec(s,slot,target,false);if(mode==PowerCables.COAX){if(!target)return spec(s,slot,false,split);k=k==RETRO?RETRO_COAX:TV_COAX;}else if(mode==PowerCables.AV)k=k==N64?N64_AV:k==SNES?SNES_AV:k==NES?NES_AV:RETRO_AV;else if(mode==PowerCables.HDMI)k=k==PC?PC_HDMI:MONITOR_HDMI;return k|(s.getValue(BlockStateProperties.HORIZONTAL_FACING).get3DDataValue()<<5)|(slot<<8)|positionBits(s)|neutralAngles(k);}
 private static int neutralAngles(int kind){return kind==WALL_TV?(45<<10)|(45<<17):0;}
 public static int connectionSpec(net.minecraft.world.level.Level level,BlockPos pos,int slot,int mode,boolean target,boolean split){
  int spec=connectionSpec(level.getBlockState(pos),slot,mode,target,split);
  if(level.getBlockEntity(pos) instanceof net.mcreator.commandcookieselectronicmod.tv.ImageTvEntity tv&&tv.wall()){
   int roll=Math.clamp(tv.roll,-90,90),rotation=roll<0?90-roll:roll;
   spec=(spec&~((127<<10)|(127<<17)|(255<<24)))|((tv.yaw+45)<<10)|((tv.pitch+45)<<17)|(rotation<<24);
  }
  return spec;
 }
 public static Port at(BlockPos pos,int spec){
  int k=spec&31,slot=spec>>>8&1;var facing=Direction.from3DDataValue(spec>>>5&7);double x=8,y=6,z=16;Vec3 n=new Vec3(0,0,1);
  if(k==STAND_TV||k==WALL_TV){
   boolean wall=k==WALL_TV;int yaw=wall?Math.clamp((spec>>>10&127)-45,-45,45):0,pitch=wall?Math.clamp((spec>>>17&127)-45,-45,45):0;
   int rotation=spec>>>24&255,roll=wall?Math.clamp(rotation<=90?rotation:90-rotation,-90,90):0;
   double px=17.4,py=wall?4.4:6.9,pz=wall?15:8.5;
   var point=net.mcreator.commandcookieselectronicmod.tv.TvGeometry.point(px,py,pz,facing,wall,yaw,pitch,roll);
   var normal=net.mcreator.commandcookieselectronicmod.tv.TvGeometry.point(px,py,pz+1,facing,wall,yaw,pitch,roll).subtract(point).normalize();
   return new Port(point.add(pos.getX(),pos.getY(),pos.getZ()),normal,k,slot);
  }
  switch(k){
   case OUTLET->{y=slot==0?10.375:5.625;z=1.375;}
   case COAX->{y=8;z=2.75;}
   case SPLITTER->{x=slot==0?6.75:9.25;y=8;z=4.5;}
   case PC->{x=6.2;y=3.1;z=14;}
   case MONITOR->{x=3.781481;y=5.575;z=(spec&(1<<9))!=0?11.79:8.79;}
   case NES->{x=10.4;y=1.75;z=14.5;}
   case SNES->{x=11.375;y=1.3;z=14.2;}
   case N64->{x=5.85;y=1.8;z=13.7;}
   case CAMERA->{x=9.215;y=9.919;z=11.981;n=new Vec3(0,.382683432,.923879533);}
   case N64_AV->{x=11.115;y=1.455;z=13.49;}
   case SNES_AV->{x=5.25;y=1.3;z=14.2;}
   case RETRO->{x=5.375;y=6.05;z=15.5;}
   case TV->{x=12;y=4;z=16;}
   case RETRO_COAX->{x=11.4;y=8.1;z=15.23;}
   case TV_COAX->{x=8;y=6.5;z=16;}
   case NES_AV->{x=.5;y=1.5;z=5;n=new Vec3(-1,0,0);}
   case RETRO_AV->{x=9.975;y=5.95;z=15.5;}
   case PC_HDMI->{x=8;y=6.5;z=13.7;}
   case MONITOR_HDMI->{x=12.962963;y=5.575;z=(spec&(1<<9))!=0?11.79:8.79;}
   case FEED,COAX_FEED->{y=3.15;z=.75;n=new Vec3(0,-1,0);}
  }
  boolean outlet=k==OUTLET||k==COAX||k==FEED||k==COAX_FEED||k==SPLITTER;
  int turns=switch(facing){case NORTH->0;case EAST->1;case SOUTH->2;default->3;};if(outlet)turns=(turns+2)%4;
  Vec3 p=new Vec3(x/16-.5,y/16,z/16-.5);for(int i=0;i<turns;i++){p=new Vec3(-p.z,p.y,p.x);n=new Vec3(-n.z,n.y,n.x);}
  return new Port(p.add(pos.getX()+.5,pos.getY(),pos.getZ()+.5),n,k,slot);
 }
}
