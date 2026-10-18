package net.mcreator.commandcookieselectronicmod.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.mcreator.commandcookieselectronicmod.inventory.WorkbenchMenu;
import org.lwjgl.glfw.GLFW;
import net.mcreator.commandcookieselectronicmod.inventory.WorkbenchRecipes;

public class WorkbenchScreen extends AbstractContainerScreen<WorkbenchMenu> {
    private final List<WorkbenchRecipes.Recipe> recipes=WorkbenchRecipes.all();
    private List<WorkbenchRecipes.Recipe> filtered=recipes;
    private record RecipeEntry(WorkbenchRecipes.Recipe recipe,String group,int count) {}
    private List<RecipeEntry> entries=List.of();
    private final java.util.Set<String> expandedGroups=new java.util.HashSet<>();
    private static String group(WorkbenchRecipes.Recipe recipe){
        if(recipe.id().startsWith("gaming_chair_"))return "chairs";
        if(recipe.id().startsWith("airpods_max_"))return "headphones";
        if(recipe.id().endsWith("cartridge")||recipe.family().equalsIgnoreCase("Cartridges"))return "cartridges";
        return "";
    }
    private static String groupTitle(String group){return switch(group){case "chairs"->"Gaming Chairs";case "cartridges"->"Cartridges";case "headphones"->"AirPods Max";default->"";};}
    private void rebuildEntries(){
        var result=new ArrayList<RecipeEntry>();var seen=new java.util.HashSet<String>();
        for(var recipe:filtered){
            String key=group(recipe);
            if(key.isEmpty())continue;
            if(!seen.add(key))continue;
            var members=filtered.stream().filter(r->group(r).equals(key)).toList();
            result.add(new RecipeEntry(null,key,members.size()));
            if(expandedGroups.contains(key))for(var member:members)result.add(new RecipeEntry(member,key,0));
        }
        for(var recipe:filtered)if(group(recipe).isEmpty())result.add(new RecipeEntry(recipe,"",0));
        entries=List.copyOf(result);scroll=Math.min(scroll,maxScroll());
    }
    private void toggleGroup(String key){if(!expandedGroups.remove(key))expandedGroups.add(key);rebuildEntries();rebuildRows();}
    private int displayedIndex(WorkbenchRecipes.Recipe recipe){for(int i=0;i<entries.size();i++)if(entries.get(i).recipe()==recipe)return i;return -1;}
    private final List<Button> rows=new ArrayList<>();
    private WorkbenchRecipes.Recipe selected=recipes.isEmpty()?null:recipes.get(0);
    private EditBox search;
    private String query="";
    private int scroll,partScroll;
    private boolean dragging;
    private Button craft;
    private int craftStatus=WorkbenchRecipes.MISSING,lastSequence;
    private long feedbackUntil;
    private static final int VISIBLE=6,ROW_HEIGHT=24;
    public WorkbenchScreen(WorkbenchMenu menu,Inventory inv,Component title) {
        super(menu,inv,title);imageWidth=398;imageHeight=238;
    }
    @Override protected void init() {
        super.init();rows.clear();
        search=addRenderableWidget(new EditBox(font,leftPos+15,topPos+40,115,17,Component.literal("Search workbench recipes")));
        search.setMaxLength(80);search.setHint(Component.literal("Search recipes"));search.setValue(query);
        search.setResponder(value -> {query=value;filter();});
        craft=addRenderableWidget(Button.builder(Component.literal("Craft"),button -> {
            if(selected!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,recipes.indexOf(selected));
        }).bounds(leftPos+308,topPos+125,72,13).build());
        updateCraft();
        rebuildEntries();rebuildRows();
    }
    private void updateCraft() {
        craftStatus=selected==null?WorkbenchRecipes.MISSING:WorkbenchRecipes.plan(minecraft.player.getInventory(),selected).status();
        if(craft!=null) {
            craft.active=selected!=null&&craftStatus==WorkbenchRecipes.READY;
            craft.setTooltip(Tooltip.create(Component.literal(craftStatus==WorkbenchRecipes.READY?"Craft one item using parts from your inventory":craftStatus==WorkbenchRecipes.FULL?"Make room in your inventory first":"Collect the missing parts first")));
        }
    }
    @Override protected void containerTick(){
        super.containerTick();updateCraft();
        if(lastSequence!=menu.craftSequence()){lastSequence=menu.craftSequence();feedbackUntil=System.currentTimeMillis()+2500;}
    }
    private void filter() {
        String needle=query.trim().toLowerCase(Locale.ROOT);
        filtered=recipes.stream().filter(recipe -> (recipe.title()+" "+recipe.family()+" "+groupTitle(group(recipe))).toLowerCase(Locale.ROOT).contains(needle)).toList();
        scroll=0;partScroll=0;
        if(selected==null||!filtered.contains(selected))selected=filtered.isEmpty()?null:filtered.get(0);
        if(!needle.isEmpty())for(var recipe:filtered)if(!group(recipe).isEmpty())expandedGroups.add(group(recipe));
        rebuildEntries();rebuildRows();updateCraft();
    }
    private int maxScroll(){return Math.max(0,entries.size()-VISIBLE);}
    private void scrollTo(int value){scroll=Math.max(0,Math.min(maxScroll(),value));rebuildRows();}
    private void rebuildRows() {
        for(Button row:rows)removeWidget(row);rows.clear();scroll=Math.min(scroll,maxScroll());
        for(int i=0;i<VISIBLE&&scroll+i<entries.size();i++) {
            var entry=entries.get(scroll+i);Button row;
            if(entry.recipe()==null)row=new GroupRow(leftPos+13,topPos+66+i*ROW_HEIGHT,119,22,entry.group(),entry.count());
            else {int indent=entry.group().isEmpty()?0:5;row=new RecipeRow(leftPos+13+indent,topPos+66+i*ROW_HEIGHT,119-indent,22,entry.recipe());}
            rows.add(addRenderableWidget(row));
        }
    }
    private static void panel(GuiGraphics g,int x,int y,int w,int h,int base) {
        g.fill(x,y,x+w,y+h,base);g.fill(x,y,x+w,y+1,0xFF64716F);g.fill(x,y,x+1,y+h,0xFF64716F);
        g.fill(x,y+h-1,x+w,y+h,0xFF121D22);g.fill(x+w-1,y,x+w,y+h,0xFF121D22);
    }
    private static void slot(GuiGraphics g,int x,int y) {
        g.fill(x-1,y-1,x+17,y+17,0xFF101D21);g.fill(x,y,x+16,y+16,0xFF233638);
        g.fill(x,y+16,x+17,y+17,0xFF63736B);g.fill(x+16,y,x+17,y+16,0xFF63736B);
    }
    private int thumbHeight(){return Math.max(15,(VISIBLE*ROW_HEIGHT)*VISIBLE/Math.max(VISIBLE,entries.size()));}
    private int thumbY(){return topPos+66+(maxScroll()==0?0:(VISIBLE*ROW_HEIGHT-thumbHeight())*scroll/maxScroll());}
    @Override protected void renderBg(GuiGraphics g,float delta,int mouseX,int mouseY) {
        int x=leftPos,y=topPos;
        g.fill(x+4,y+4,x+imageWidth+4,y+imageHeight+4,0x88000000);panel(g,x,y,imageWidth,imageHeight,0xFF303B40);
        panel(g,x+5,y+5,imageWidth-10,25,0xFF663E50);
        g.fill(x+376,y+15,x+381,y+20,0xFF71CC9E);
        panel(g,x+8,y+35,138,195,0xFF202E32);
        panel(g,x+153,y+35,237,103,0xFF243638);
        panel(g,x+178,y+146,178,59,0xFF29353A);
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)slot(g,x+185+col*18,y+151+row*18);
        for(int col=0;col<9;col++)slot(g,x+185+col*18,y+209);
        g.fill(x+136,y+66,x+141,y+210,0xFF111F24);
        g.fill(x+136,thumbY(),x+141,thumbY()+thumbHeight(),maxScroll()==0?0xFF3B5053:dragging?0xFFB7D6B8:0xFF81AB94);
        if(selected!=null) {
            slot(g,x+165,y+40);g.renderItem(WorkbenchRecipes.stack(selected.output()),x+165,y+40);
            List<WorkbenchRecipes.Part> parts=selected.parts();
            for(int i=0;i<Math.min(5,parts.size()-partScroll);i++) {
                var part=parts.get(partScroll+i);int py=y+70+i*11;
                g.fill(x+162,py,x+380,py+10,0xFF1A2B2E);
                var item=WorkbenchRecipes.stack(part.item());
                if(item.isEmpty()) {
                    g.renderOutline(x+165,py+1,10,10,0xFF556E69);g.drawString(font,"?",x+168,py+2,0xFF8FA49A,false);
                }else {
                    g.pose().pushPose();g.pose().translate(x+165,py,0);g.pose().scale(.625f,.625f,1);g.renderItem(item,0,0);g.pose().popPose();
                }
                int have=part.count(minecraft.player.getInventory());String amount=have+"/"+part.amount();
                g.drawString(font,font.plainSubstrByWidth(part.label(),148),x+182,py+1,0xFFCAD8CF,false);
                g.drawString(font,amount,x+375-font.width(amount),py+1,have>=part.amount()?0xFF9BD6A7:0xFFE59B91,false);
            }
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY) {
        g.drawString(font,"Electronics Workbench",14,14,0xFFF4E6E6,false);
        g.drawString(font,"WORKBENCH RECIPES",249,14,0xFFCDB0BB,false);
        g.drawString(font,filtered.isEmpty()?"No matching recipes":filtered.size()+(filtered.size()==1?" recipe":" recipes"),15,218,0xFF94AAA1,false);
        if(selected!=null) {
            g.drawString(font,font.plainSubstrByWidth(selected.title(),190),189,41,0xFFE5DBCB,false);
            g.drawString(font,"Required parts",162,59,0xFFB5CFC0,false);
            String status=craftStatus==WorkbenchRecipes.READY?"Ready to craft":craftStatus==WorkbenchRecipes.FULL?"Inventory is full":"Missing parts";
            if(menu.lastRecipe()==recipes.indexOf(selected)&&System.currentTimeMillis()<feedbackUntil)status=menu.craftResult()==1?"Crafted!":menu.craftResult()==3?"Inventory is full":"Missing parts";
            g.drawString(font,status,162,128,craftStatus==WorkbenchRecipes.READY?0xFF9BD6A7:0xFFB6A583,false);
            if(selected.parts().size()>5)g.drawString(font,(partScroll+1)+"-"+Math.min(partScroll+5,selected.parts().size())+" / "+selected.parts().size(),310,59,0xFFB5CFC0,false);
        }else {
            g.drawString(font,"No recipe selected",163,49,0xFFB5CFC0,false);
            g.drawString(font,"Try a different search.",163,67,0xFF93ABA0,false);
        }
        g.drawString(font,playerInventoryTitle,185,140,0xFFC3CCC7,false);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float delta) {
        super.render(g,mouseX,mouseY,delta);renderTooltip(g,mouseX,mouseY);
        if(selected!=null&&mouseX>=leftPos+162&&mouseX<leftPos+185&&mouseY>=topPos+40&&mouseY<topPos+65)
            g.renderTooltip(font,Component.literal(selected.title()),mouseX,mouseY);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        if(mx>=leftPos+8&&mx<leftPos+146&&my>=topPos+35&&my<topPos+230){scrollTo(scroll+(vertical<0?1:vertical>0?-1:0));return true;}
        if(selected!=null&&selected.parts().size()>5&&mx>=leftPos+153&&mx<leftPos+390&&my>=topPos+70&&my<topPos+124) {
            partScroll=Math.max(0,Math.min(selected.parts().size()-5,partScroll+(vertical<0?1:-1)));return true;
        }
        return super.mouseScrolled(mx,my,horizontal,vertical);
    }
    private void dragTo(double my) {
        int travel=VISIBLE*ROW_HEIGHT-thumbHeight();
        if(travel>0)scrollTo((int)Math.round((my-topPos-66-thumbHeight()/2.0)*maxScroll()/travel));
    }
    @Override public boolean mouseClicked(double mx,double my,int button) {
        if(button==0&&maxScroll()>0&&mx>=leftPos+134&&mx<=leftPos+143&&my>=topPos+66&&my<topPos+210){dragging=true;dragTo(my);return true;}
        return super.mouseClicked(mx,my,button);
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy) {
        if(dragging&&button==0){dragTo(my);return true;}return super.mouseDragged(mx,my,button,dx,dy);
    }
    @Override public boolean mouseReleased(double mx,double my,int button){dragging=false;return super.mouseReleased(mx,my,button);}
    @Override public boolean keyPressed(int key,int scan,int mods) {
        if(search!=null&&search.isFocused()) {
            if(key==GLFW.GLFW_KEY_ESCAPE){search.setFocused(false);return true;}
            if(key!=GLFW.GLFW_KEY_ESCAPE)return search.keyPressed(key,scan,mods);
        }
        int delta=key==GLFW.GLFW_KEY_DOWN?1:key==GLFW.GLFW_KEY_UP?-1:key==GLFW.GLFW_KEY_PAGE_DOWN?VISIBLE:key==GLFW.GLFW_KEY_PAGE_UP?-VISIBLE:0;
        if(delta!=0&&!filtered.isEmpty()) {
            int index=Math.max(0,Math.min(filtered.size()-1,filtered.indexOf(selected)+delta));selected=filtered.get(index);partScroll=0;updateCraft();
            String groupKey=group(selected);if(!groupKey.isEmpty())expandedGroups.add(groupKey);rebuildEntries();
            int shown=displayedIndex(selected);if(shown<scroll)scrollTo(shown);else if(shown>=scroll+VISIBLE)scrollTo(shown-VISIBLE+1);else rebuildRows();return true;
        }
        return super.keyPressed(key,scan,mods);
    }
    private final class GroupRow extends Button {
        private final String key;private final int count;
        GroupRow(int x,int y,int w,int h,String key,int count){
            super(x,y,w,h,Component.literal(groupTitle(key)),button->toggleGroup(key),DEFAULT_NARRATION);this.key=key;this.count=count;
            setTooltip(Tooltip.create(Component.literal("Click to expand or collapse "+groupTitle(key).toLowerCase(Locale.ROOT))));
        }
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta){
            int x=getX(),y=getY(),w=getWidth(),h=getHeight();boolean open=expandedGroups.contains(key);
            g.fill(x,y,x+w,y+h,open?0xFF374C4C:0xFF304246);g.fill(x,y+h-1,x+w,y+h,0xFF122327);
            if(isHoveredOrFocused())g.renderOutline(x,y,w,h,0xFF7D9D8C);
            if(open){for(int i=0;i<4;i++)g.fill(x+4+i,y+7+i,x+11-i,y+8+i,0xFFABD3B5);}
            else {for(int i=0;i<4;i++)g.fill(x+5+i,y+5+i,x+6+i,y+12-i,0xFFABD3B5);}
            g.drawString(font,groupTitle(key),x+15,y+3,0xFFE1EAE2,false);
            g.drawString(font,count+(key.equals("chairs")?" variants":" game"+(count==1?"":"s")),x+15,y+12,0xFF92AD9C,false);
        }
    }
    private final class RecipeRow extends Button {
        private final WorkbenchRecipes.Recipe recipe;
        RecipeRow(int x,int y,int w,int h,WorkbenchRecipes.Recipe recipe) {
            super(x,y,w,h,Component.literal(recipe.title()),button -> {selected=recipe;partScroll=0;updateCraft();},DEFAULT_NARRATION);this.recipe=recipe;
            setTooltip(Tooltip.create(Component.literal(recipe.title()+"\n"+recipe.family())));
        }
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta) {
            int x=getX(),y=getY(),w=getWidth(),h=getHeight();g.fill(x,y,x+w,y+h,selected==recipe?0xFF385947:0xFF293D40);
            g.fill(x,y+h-1,x+w,y+h,0xFF122327);
            if(selected==recipe)g.fill(x,y,x+2,y+h,0xFF9BC7A4);
            if(isHoveredOrFocused())g.renderOutline(x,y,w,h,0xFF7D9D8C);
            g.renderItem(WorkbenchRecipes.stack(recipe.output()),x+4,y+3);
            String shortName=recipe.title().replace("iPhone 18 Pro Max - ","iPhone - ").replace("Gaming Chair - ","Chair - ").replace("S26 Ultra - ","S26 - ");
            g.drawString(font,font.plainSubstrByWidth(shortName,w-29),x+25,y+3,0xFFE1EAE2,false);
            g.drawString(font,recipe.hasMaterials(minecraft.player.getInventory())?"Parts ready":"Missing parts",x+25,y+12,0xFF92AD9C,false);
        }
    }
    @EventBusSubscriber(modid="commandcookies_electronic_mod",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static class Registration {
        @SubscribeEvent public static void screens(RegisterMenuScreensEvent e){e.register(WorkbenchMenu.TYPE,WorkbenchScreen::new);}
    }
}
