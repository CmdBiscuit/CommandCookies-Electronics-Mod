package net.mcreator.commandcookieselectronicmod.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Original pixel icons for BlockOS. App IDs match the desktop/catalog. */
final class LaptopAppIcons {
    static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "commandcookies_electronic_mod", "textures/gui/laptop_app_icons.png");
    private static final int CELL=24, COLUMNS=8, WIDTH=192, HEIGHT=72, COUNT=24;

    private LaptopAppIcons() {}

    static void draw(GuiGraphics graphics,int app,int x,int y,int size) {
        if(app==24&&size>0){for(int py=0;py<24;py++)for(int px=0;px<24;px++){double dx=px-11.5,dy=py-11.5;if(dx*dx+dy*dy>119)continue;int color=dx*dx+dy*dy<24?0xFFC2E4FF:dy<-3?0xFF8BBDEA:dx>0?0xFF5798DB:0xFF3974B7;int xx=x+px*size/24,yy=y+py*size/24,x2=x+(px+1)*size/24,y2=y+(py+1)*size/24;if(x2>xx&&y2>yy)graphics.fill(xx,yy,x2,y2,color);}return;}
        if(app<0 || app>=COUNT || size<=0)return;
        graphics.blit(TEXTURE,x,y,size,size,(app%COLUMNS)*CELL,(app/COLUMNS)*CELL,
            CELL,CELL,WIDTH,HEIGHT);
    }
}
