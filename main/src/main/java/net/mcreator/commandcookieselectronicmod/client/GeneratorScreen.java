package net.mcreator.commandcookieselectronicmod.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.mcreator.commandcookieselectronicmod.power.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
public final class GeneratorScreen extends AbstractContainerScreen<GeneratorMenu> {
    private Button toggle;
    public GeneratorScreen(GeneratorMenu m,Inventory inv,Component title){super(m,inv,title);imageWidth=176;imageHeight=227;inventoryLabelY=133;titleLabelY=13;titleLabelX=12;}
    @Override protected void init(){super.init();toggle=addRenderableWidget(Button.builder(Component.literal("Pause"),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);}).bounds(leftPos+111,topPos+108,53,16).build());}
    @Override protected void containerTick(){super.containerTick();toggle.setMessage(Component.literal(menu.enabled()?"Pause":"Start"));}
    private void rect(GuiGraphics g,int x,int y,int w,int h,int c){g.fill(leftPos+x,topPos+y,leftPos+x+w,topPos+y+h,c);}
    private void text(GuiGraphics g,String s,int x,int y,int c){g.drawString(font,s,leftPos+x,topPos+y,c,false);}
    private void centeredText(GuiGraphics g,String s,int center,int y,int c){text(g,s,center-font.width(s)/2,y,c);}
    private void slot(GuiGraphics g,int x,int y){rect(g,x-1,y-1,18,18,0xFF101A22);rect(g,x,y,16,16,0xFF263743);g.renderOutline(leftPos+x-1,topPos+y-1,18,18,0xFF4A5D66);}
    public static String duration(int ticks){long sec=(ticks+19L)/20;long days=sec/86400,hours=sec/3600%24,minutes=sec/60%60;if(days>0)return days+"d "+hours+"h "+minutes+"m";if(hours>0)return hours+"h "+minutes+"m";return minutes+"m "+sec%60+"s";}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        rect(g,3,3,imageWidth,imageHeight,0x66000000);rect(g,0,0,imageWidth,imageHeight,0xFF1B2A34);g.renderOutline(leftPos,topPos,imageWidth,imageHeight,0xFF60717A);
        rect(g,1,1,174,30,0xFF293C46);rect(g,1,31,174,2,0xFFE6B84D);
        rect(g,12,42,46,55,0xFF131F27);g.renderOutline(leftPos+12,topPos+42,46,55,0xFF3D535E);centeredText(g,"FUEL",35,47,0xFFA8BFC4);slot(g,27,61);
        int status=menu.powered()?0xFF82DFA3:0xFFE6B84D;rect(g,68,45,5,5,status);text(g,menu.powered()?"ONLINE":menu.enabled()?"NO FUEL":"PAUSED",79,44,status);
        text(g,"Fuel remaining",68,60,0xFFA8BFC4);text(g,duration(menu.total()),68,73,0xFFEAF1DE);
        rect(g,68,89,96,5,0xFF0D1820);rect(g,68,89,Math.min(96,(int)(96L*menu.total()/GeneratorEntity.STACK_TICKS)),5,status);
        rect(g,8,127,160,1,0xFF425762);
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)slot(g,8+col*18,145+row*18);
        for(int col=0;col<9;col++)slot(g,8+col*18,203);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){g.drawString(font,title,12,13,0xFFEAF1DE,false);g.drawString(font,playerInventoryTitle,8,133,0xFFA8BFC4,false);}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){super.render(g,mx,my,partial);renderTooltip(g,mx,my);}
    @EventBusSubscriber(modid="commandcookies_electronic_mod",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class Registration {@SubscribeEvent public static void register(RegisterMenuScreensEvent e){e.register(GeneratorMenu.TYPE,GeneratorScreen::new);}}
}
