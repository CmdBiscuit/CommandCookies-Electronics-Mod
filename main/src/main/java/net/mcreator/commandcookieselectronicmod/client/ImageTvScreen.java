package net.mcreator.commandcookieselectronicmod.client;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.mcreator.commandcookieselectronicmod.tv.*;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ImageTvScreen extends Screen {
    private final BlockPos pos;
    private final UUID identity;
    private final boolean wall;
    private String text,status="";
    private boolean fill,pending,compact;
    private int yaw,pitch,roll,x,y,w,h;
    private long request;
    private EditBox link;
    private AngleSlider horizontal,vertical,rotation;
    private Button fit,apply;
    public static void open(BlockPos pos){var mc=Minecraft.getInstance();if(mc.level!=null&&mc.level.getBlockEntity(pos) instanceof ImageTvEntity tv)mc.setScreen(new ImageTvScreen(tv));}
    public ImageTvScreen(ImageTvEntity tv){super(Component.literal(tv.wall()?"Wall-Mounted TV":"TV with Stand"));pos=tv.getBlockPos();identity=tv.identity;wall=tv.wall();text=tv.image;fill=tv.fill;yaw=tv.yaw;pitch=tv.pitch;roll=tv.roll;}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float partial){}
    private Button button(int xx,int yy,int ww,String label,Runnable action){return addRenderableWidget(Button.builder(Component.literal(label),b->action.run()).bounds(xx,yy,ww,20).build());}
    @Override protected void init(){
        compact=wall&&height<322;w=Math.min(320,width-20);h=wall?(compact?230:310):208;x=(width-w)/2;y=Math.max(5,(height-h)/2);
        link=new EditBox(font,x+12,y+(compact?36:45),w-24,20,Component.literal("Imgur image link"));link.setMaxLength(512);link.setValue(text);link.setHint(Component.literal("https://imgur.com/your-image"));link.setResponder(v->text=v);addRenderableWidget(link);
        fit=button(x+12,y+(compact?59:74),w-24,"",()->{fill=!fill;fitLabel();});fitLabel();
        int columns=compact?3:2,column=(w-24-(columns-1)*6)/columns,actionY=y+(compact?82:101);
        button(x+12,actionY,column,"Clear image",()->{link.setValue("");status="The screen will be black after applying.";});
        button(x+18+column,actionY,column,"Reload image",()->{try{ImageTvImages.retry(ImgurLink.normalize(text));status="Reloading image...";}catch(IllegalArgumentException ex){status=ex.getMessage();}});
        if(wall){
            horizontal=addRenderableWidget(new AngleSlider(x+12,y+(compact?105:133),w-24,"Horizontal",yaw,0));
            vertical=addRenderableWidget(new AngleSlider(x+12,y+(compact?128:160),w-24,"Vertical tilt",pitch,1));
            rotation=addRenderableWidget(new AngleSlider(x+12,y+(compact?151:187),w-24,"Screen rotation",roll,2));
            if(compact)button(x+24+2*column,actionY,column,"Reset",this::resetAngles);
            else button(x+w-88,y+216,76,"Reset",this::resetAngles);
        }
        apply=button(x+12,y+h-32,(w-30)/2,"Apply & Close",this::save);
        apply.active=!pending;
        button(x+18+(w-30)/2,y+h-32,(w-30)/2,"Cancel",this::onClose);
        setInitialFocus(link);
    }
    private void resetAngles(){yaw=pitch=roll=0;horizontal.reset();vertical.reset();rotation.reset();}
    private void fitLabel(){fit.setMessage(Component.literal(fill?"Image fit: Fill screen (crop)":"Image fit: Whole image"));}
    private void save(){
        if(pending)return;
        String url;try{url=ImgurLink.normalize(text);}catch(IllegalArgumentException error){status=error.getMessage();return;}
        pending=true;apply.active=false;request++;status="Saving...";
        PacketDistributor.sendToServer(new TvPackets.Edit(pos,identity,request,url,fill,yaw,pitch,roll));
    }
    public void response(TvPackets.Result result){if(!result.pos().equals(pos)||!result.identity().equals(identity)||result.request()!=request)return;pending=false;apply.active=true;if(result.success())onClose();else status=result.message();}
    @Override public void tick(){
        if(minecraft.player==null||minecraft.level==null||!(minecraft.level.getBlockEntity(pos) instanceof ImageTvEntity tv)||!tv.identity.equals(identity)||minecraft.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))>100){onClose();return;}
        if(status.isEmpty()&&tv.powered&&!tv.image.isEmpty()){var entry=ImageTvImages.get(tv.image);if(entry!=null&&entry.error!=null)status=entry.error;}
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        super.renderBackground(g,mx,my,partial);g.fill(x,y,x+w,y+h,0xF0182029);g.fill(x,y,x+w,y+(compact?24:28),0xFF293744);
        g.drawCenteredString(font,title,x+w/2,y+(compact?8:10),0xFFE3E9EC);g.drawString(font,"Imgur image link",x+12,y+(compact?25:33),0xFFB7C6CF,false);
        if(wall&&!compact)g.drawString(font,"Swivel/tilt ±45°; rotate ±90°",x+12,y+222,0xFF9DB4BD,false);
        String message=status.isEmpty()?(text.isBlank()?"No image: black screen.":"PNG, JPG and GIF images. Aspect ratio stays intact."):status;
        if(status.isEmpty()&&minecraft.level!=null&&minecraft.level.getBlockEntity(pos) instanceof ImageTvEntity tv&&!tv.powered)message="No power. Use the wrench to connect a powered outlet.";
        int yy=y+(wall?(compact?174:245):136);for(var line:font.split(Component.literal(message),w-24)){g.drawString(font,line,x+12,yy,0xFFB7C6CF,false);yy+=10;if(yy>y+h-36)break;}
        super.render(g,mx,my,partial);
    }
    private final class AngleSlider extends AbstractSliderButton {
        private final String label;private final int axis;
        AngleSlider(int xx,int yy,int ww,String label,int angle,int axis){super(xx,yy,ww,compact?20:22,Component.empty(),(angle+(axis==2?TvGeometry.MAX_ROLL:TvGeometry.MAX_ANGLE))/(2d*(axis==2?TvGeometry.MAX_ROLL:TvGeometry.MAX_ANGLE)));this.label=label;this.axis=axis;updateMessage();}
        private int limit(){return axis==2?TvGeometry.MAX_ROLL:TvGeometry.MAX_ANGLE;}
        private int angle(){return (int)Math.round(value*2*limit())-limit();}
        @Override protected void updateMessage(){int angle=angle();setMessage(Component.literal(label+": "+(angle>0?"+":"")+angle+"°"));}
        @Override protected void applyValue(){int angle=angle();if(axis==0)yaw=angle;else if(axis==1)pitch=angle;else roll=angle;updateMessage();}
        void reset(){value=.5;updateMessage();}
    }
}
