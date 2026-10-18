package net.mcreator.commandcookieselectronicmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

final class PhonePhotoButton extends Button {
    private final int color;
    PhonePhotoButton(int x,int y,int width,int height,String label,int color,Runnable action) {
        super(x,y,width,height,Component.literal(label),button -> action.run(),DEFAULT_NARRATION);this.color=color;
    }
    @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { PhoneWallpapers.click(manager,PhoneWallpapers.preferences(Minecraft.getInstance().screen)); }
    @Override protected void renderWidget(GuiGraphics g,int mouseX,int mouseY,float tick) {
        int x=getX(),y=getY(),w=getWidth(),h=getHeight();
        int shade=active ? color : 0xFF333D49;
        g.fill(x+2,y,x+w-2,y+h,shade);g.fill(x,y+2,x+w,y+h-2,shade);
        g.fill(x+2,y+h-2,x+w-2,y+h,0x44000000);
        if(isHoveredOrFocused()&&active)g.renderOutline(x,y,w,h,0xFFD0E6E6);
        var font=Minecraft.getInstance().font;
        var message=getMessage().getString().equals("Take photo")?getMessage().copy().withStyle(ChatFormatting.BOLD):getMessage();
        float scale=Math.min(PhoneWallpapers.preferences(Minecraft.getInstance().screen).textScale(),(w-8f)/Math.max(1,font.width(message)));
        g.pose().pushPose();g.pose().translate(x+w/2,y+Math.round((h-font.lineHeight*scale)/2f),0);g.pose().scale(scale,scale,1);
        int ink=((color>>16)&255)+((color>>8)&255)+(color&255)>570?0xFF25313C:0xFFEDF4F4;
        g.drawString(font,message,-font.width(message)/2,0,active?ink:0xFF8895A4,false);g.pose().popPose();
    }
}
