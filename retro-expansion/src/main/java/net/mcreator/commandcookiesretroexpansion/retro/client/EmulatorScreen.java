package net.mcreator.commandcookiesretroexpansion.retro.client;
import java.util.*;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.mcreator.commandcookiesretroexpansion.retro.*;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class EmulatorScreen extends Screen {
 public final RetroPackets.Open session;public final EmulatorRunner runner;
 private ResourceLocation location;private DynamicTexture texture;private EmulatorRunner.Frame displayed;private int ticks;private Button pause;
 private final Set<Integer> pressed=new HashSet<>();
 public EmulatorScreen(RetroPackets.Open session){super(Component.literal(session.title()));this.session=session;runner=new EmulatorRunner(session);}
 @Override protected void init(){pause=addRenderableWidget(Button.builder(Component.literal(runner.paused?"Resume":"Pause"),b->{runner.paused=!runner.paused;clearInput();b.setMessage(Component.literal(runner.paused?"Resume":"Pause"));}).bounds(width/2-145,height-23,68,20).build());
  addRenderableWidget(Button.builder(Component.literal("Save"),b->runner.save()).bounds(width/2-72,height-23,68,20).build());
  addRenderableWidget(Button.builder(Component.literal("Reset"),b->{clearInput();runner.reset();}).bounds(width/2+1,height-23,68,20).build());
  addRenderableWidget(Button.builder(Component.literal("Close"),b->onClose()).bounds(width/2+74,height-23,68,20).build());
 }
 private void clearInput(){pressed.clear();runner.buttons=0;runner.axisX=0;runner.axisY=0;}
 @Override public void tick(){ticks++;if(minecraft.player==null||minecraft.level==null||!session.system().controller(minecraft.player)){onClose();return;}if(ticks%20==0)PacketDistributor.sendToServer(new RetroPackets.Pulse(session.session(),false));runner.volume=minecraft.options.getSoundSourceVolume(SoundSource.MASTER)*minecraft.options.getSoundSourceVolume(SoundSource.RECORDS);if(!minecraft.isWindowActive()||runner.paused)clearInput();}
 @Override public void render(GuiGraphics g,int mx,int my,float dt){g.fill(0,0,width,height,0xff090f16);var frame=runner.picture.getAndSet(null);
  if(frame!=null){if(texture==null||displayed.width()!=frame.width()||displayed.height()!=frame.height()){release();texture=new DynamicTexture(new NativeImage(frame.width(),frame.height(),false));texture.setFilter(false,false);location=minecraft.getTextureManager().register("cem_retro_screen",texture);}var image=texture.getPixels();if(session.system()==RetroSystem.N64){int[] index={0};image.applyToAllPixels(ignored->{int c=frame.argb()[index[0]++];return (c&0xff00ff00)|((c>>>16)&255)|((c&255)<<16);});}else for(int y=0;y<frame.height();y++)for(int x=0;x<frame.width();x++){int c=frame.argb()[y*frame.width()+x];image.setPixelRGBA(x,y,(c&0xff00ff00)|((c>>>16)&255)|((c&255)<<16));}texture.upload();if(session.system()==RetroSystem.N64)texture.setFilter(true,false);displayed=frame;}
  g.drawCenteredString(font,session.system().label+" Â· "+title.getString(),width/2,5,0xffffff);
  if(texture!=null){int h=Math.max(1,Math.min(height-72,(width-12)*3/4)),w=h*4/3,x=(width-w)/2,y=17;g.blit(location,x,y,w,h,0,0,displayed.width(),displayed.height(),displayed.width(),displayed.height());if(runner.paused)g.drawCenteredString(font,"PAUSED",width/2,y+h/2,0xffffff);}
  if(!runner.status.isEmpty()){g.fill(width/2-165,height/2-20,width/2+165,height/2+26,0xe0182533);g.drawWordWrap(font,Component.literal(runner.status),width/2-155,height/2-14,310,0xffffff);}
  String first=switch(session.system()){case NES->"ARROWS D-pad Â· Z A Â· X B";case SNES->"ARROWS D-pad Â· Z A Â· X B Â· A Y Â· S X Â· Q L Â· W R";case N64->"WASD stick Â· ARROWS D-pad Â· Z A Â· X B Â· C Z Â· Q L Â· E R";};
  String second=session.system()==RetroSystem.N64?"I / J / K / L C-buttons Â· ENTER start Â· SHIFT gentle stick":"ENTER start Â· RIGHT SHIFT select Â· SPACE pause";
  g.drawCenteredString(font,first,width/2,height-48,0xb6c6d7);g.drawCenteredString(font,second,width/2,height-36,0xb6c6d7);super.render(g,mx,my,dt);
 }
 public static int bit(RetroSystem system,int key){
  int dpad=switch(key){case GLFW.GLFW_KEY_UP->4;case GLFW.GLFW_KEY_DOWN->5;case GLFW.GLFW_KEY_LEFT->6;case GLFW.GLFW_KEY_RIGHT->7;case GLFW.GLFW_KEY_ENTER,GLFW.GLFW_KEY_KP_ENTER->3;default->-1;};if(dpad>=0)return dpad;
  if(system==RetroSystem.N64)return switch(key){case GLFW.GLFW_KEY_Z->0;case GLFW.GLFW_KEY_X->1;case GLFW.GLFW_KEY_C->12;case GLFW.GLFW_KEY_Q->2;case GLFW.GLFW_KEY_E->13;case GLFW.GLFW_KEY_I->9;case GLFW.GLFW_KEY_J->10;case GLFW.GLFW_KEY_K->8;case GLFW.GLFW_KEY_L->11;default->-1;};
  int common=switch(key){case GLFW.GLFW_KEY_X->0;case GLFW.GLFW_KEY_Z->8;case GLFW.GLFW_KEY_RIGHT_SHIFT->2;default->-1;};if(common>=0)return common;
  return system==RetroSystem.SNES?switch(key){case GLFW.GLFW_KEY_A->1;case GLFW.GLFW_KEY_S->9;case GLFW.GLFW_KEY_Q->10;case GLFW.GLFW_KEY_W->11;default->-1;}:-1;
 }
 private void input(){int mask=0;for(int key:pressed){int bit=bit(session.system(),key);if(bit>=0)mask|=1<<bit;}runner.buttons=mask;runner.axisX=runner.axisY=0;
  if(session.system()==RetroSystem.N64){int x=(pressed.contains(GLFW.GLFW_KEY_D)?1:0)-(pressed.contains(GLFW.GLFW_KEY_A)?1:0),y=(pressed.contains(GLFW.GLFW_KEY_S)?1:0)-(pressed.contains(GLFW.GLFW_KEY_W)?1:0);int magnitude=pressed.contains(GLFW.GLFW_KEY_LEFT_SHIFT)?16384:32767;double diagonal=x!=0&&y!=0?Math.sqrt(2):1;runner.axisX=(short)(x*magnitude/diagonal);runner.axisY=(short)(y*magnitude/diagonal);}
 }
 @Override public boolean keyPressed(int key,int scan,int mods){boolean stick=session.system()==RetroSystem.N64&&(key==GLFW.GLFW_KEY_W||key==GLFW.GLFW_KEY_A||key==GLFW.GLFW_KEY_S||key==GLFW.GLFW_KEY_D||key==GLFW.GLFW_KEY_LEFT_SHIFT);if(bit(session.system(),key)>=0||stick){if(!runner.paused){pressed.add(key);input();}return true;}if(key==GLFW.GLFW_KEY_SPACE){pause.onPress();return true;}return super.keyPressed(key,scan,mods);}
 @Override public boolean keyReleased(int key,int scan,int mods){if(pressed.remove(key)){input();return true;}return super.keyReleased(key,scan,mods);}
 private void release(){if(location!=null&&minecraft!=null)minecraft.getTextureManager().release(location);location=null;texture=null;}
 @Override public void renderBackground(GuiGraphics g,int mx,int my,float dt){}
 @Override public void removed(){clearInput();runner.close();release();if(minecraft.getConnection()!=null)PacketDistributor.sendToServer(new RetroPackets.Pulse(session.session(),true));super.removed();}
 @Override public boolean isPauseScreen(){return false;}
}
