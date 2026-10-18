package net.mcreator.commandcookiesretroexpansion.retro.client;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.mcreator.commandcookiesretroexpansion.retro.*;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RomLibraryScreen extends Screen {
 private final InteractionHand hand;private final RetroSystem system;private List<RomLibrary.Rom> roms=List.of();private int page,rows=6,statusY;private boolean busy=true;private String status="Loading local games…";private EditBox path;
 public RomLibraryScreen(InteractionHand hand){this(hand,RetroSystem.NES);}
 public RomLibraryScreen(InteractionHand hand,RetroSystem system){super(Component.literal(system.label+" cartridge library"));this.hand=hand;this.system=system;}
 @Override protected void init(){layout();refresh();}
 private void refresh(){busy=true;CompletableFuture.runAsync(()->{try{var found=RomLibrary.list(system);minecraft.execute(()->{if(minecraft.screen!=this)return;roms=found;busy=false;status=found.size()+" local game(s). Choose one for this cartridge.";layout();});}catch(Exception e){failure(e);}});}
 private void failure(Exception e){minecraft.execute(()->{busy=false;status=e.getMessage()==null?"Could not load this game.":e.getMessage();layout();});}
 private void layout(){String text=path==null?"":path.getValue();clearWidgets();int w=Math.min(320,width-20),x=(width-w)/2,y=42;rows=Math.max(1,Math.min(6,(height-171)/23));page=Math.min(page,Math.max(0,(roms.size()-1)/rows));int nav=y+rows*23+4,imp=nav+28,folders=imp+28;statusY=folders+24;int third=(w-16)/3,half=(w-12)/2;
  for(int i=page*rows;i<Math.min(roms.size(),page*rows+rows);i++){var r=roms.get(i);addRenderableWidget(Button.builder(Component.literal(r.title()),b->{if(minecraft.player==null||(!RetroItems.is(minecraft.player.getItemInHand(hand))||RetroItems.system(minecraft.player.getItemInHand(hand))!=system))return;PacketDistributor.sendToServer(new RetroPackets.Configure(hand==InteractionHand.OFF_HAND,r.hash(),r.title()));onClose();}).bounds(x,y+(i-page*rows)*23,w,20).build());}
  addRenderableWidget(Button.builder(Component.literal("Previous"),b->{if(page>0){page--;layout();}}).bounds(x,nav,third,20).build());
  addRenderableWidget(Button.builder(Component.literal("Refresh"),b->refresh()).bounds(x+third+8,nav,third,20).build());
  addRenderableWidget(Button.builder(Component.literal("Next"),b->{if((page+1)*rows<roms.size()){page++;layout();}}).bounds(x+w-third,nav,third,20).build());
  path=new EditBox(font,x,imp,w-82,20,Component.literal("Local "+system.label+" file path"));path.setMaxLength(1024);path.setValue(text);addRenderableWidget(path);
  var importButton=Button.builder(Component.literal("Import"),b->{String p=path.getValue();busy=true;status="Importing…";CompletableFuture.runAsync(()->{try{RomLibrary.importFile(p,system);minecraft.execute(this::refresh);}catch(Exception e){failure(e);}});}).bounds(x+w-74,imp,74,20).build();importButton.active=!busy;addRenderableWidget(importButton);
  addRenderableWidget(Button.builder(Component.literal("Open ROM folder"),b->Util.getPlatform().openFile(RomLibrary.folder(system).toFile())).bounds(x,folders,half,20).build());
  addRenderableWidget(Button.builder(Component.literal("Open core folder"),b->Util.getPlatform().openFile(RomLibrary.root().resolve("cores").toFile())).bounds(x+w-half,folders,half,20).build());
  addRenderableWidget(Button.builder(Component.literal("Done"),b->onClose()).bounds(width/2-45,height-25,90,20).build());
 }
 @Override public void render(GuiGraphics g,int mx,int my,float dt){g.fill(0,0,width,height,0xff16202a);g.drawCenteredString(font,title,width/2,14,0xffffff);g.drawCenteredString(font,"Choose a game or import "+system.formats()+".",width/2,29,0xb6c6d7);super.render(g,mx,my,dt);int w=Math.min(320,width-20);g.drawWordWrap(font,Component.literal(status), (width-w)/2,statusY,w,0xb6c6d7);}
 @Override public boolean isPauseScreen(){return false;}
 @Override public void renderBackground(GuiGraphics g,int mx,int my,float dt){}
}
