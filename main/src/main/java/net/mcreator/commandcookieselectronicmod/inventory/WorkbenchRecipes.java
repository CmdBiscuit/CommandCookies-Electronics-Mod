package net.mcreator.commandcookieselectronicmod.inventory;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.mcreator.commandcookieselectronicmod.CommandcookiesElectronicModMod;

/** Shared, server-authoritative workbench recipes. No client or screen dependencies. */
public final class WorkbenchRecipes {
    public static final int READY=0,MISSING=1,FULL=2;
    public record Part(String label,String item,int amount) {
        public boolean matches(ItemStack stack) {
            if(stack.isEmpty())return false;
            if(item.startsWith("#"))return stack.is(TagKey.create(Registries.ITEM,ResourceLocation.parse(item.substring(1))));
            return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(item)).map(stack::is).orElse(false);
        }
        public int count(Inventory inventory) {
            int count=0;for(var stack:inventory.items)if(matches(stack))count+=stack.getCount();return count;
        }
    }
    public record Recipe(String id,String title,String family,String output,int outputCount,List<Part> parts) {
        public boolean hasMaterials(Inventory inventory){return parts.stream().allMatch(part -> part.count(inventory)>=part.amount());}
    }
    public record Plan(int status,List<ItemStack> inventory) {}
    private static final class Holder { private static final List<Recipe> ALL=load(); }
    private WorkbenchRecipes(){}
    public static List<Recipe> all(){return Holder.ALL;}
    public static ItemStack stack(String id) {
        if(id==null||id.isBlank())return ItemStack.EMPTY;
        if(id.equals("#minecraft:wool"))id="minecraft:white_wool";
        var key=ResourceLocation.tryParse(id);return key==null?ItemStack.EMPTY:BuiltInRegistries.ITEM.getOptional(key).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }
    private static List<Recipe> load() {
        var recipes=new ArrayList<Recipe>();
        try(var input=WorkbenchRecipes.class.getResourceAsStream("/assets/commandcookies_electronic_mod/workbench/recipes.json")) {
            if(input==null)throw new IllegalStateException("Missing workbench recipes");
            try(var reader=new InputStreamReader(input,StandardCharsets.UTF_8)) {
                for(var element:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("recipes")) {
                    var entry=element.getAsJsonObject();var parts=new ArrayList<Part>();
                    for(var p:entry.getAsJsonArray("parts")) {
                        var part=p.getAsJsonObject();int amount=part.get("amount").getAsInt();
                        if(amount<=0||amount>2304)throw new IllegalStateException("Invalid part amount");
                        parts.add(new Part(part.get("label").getAsString(),part.get("item").getAsString(),amount));
                    }
                    if(parts.isEmpty())throw new IllegalStateException("Recipe has no parts");
                    recipes.add(new Recipe(entry.get("id").getAsString(),entry.get("title").getAsString(),entry.get("family").getAsString(),entry.get("output").getAsString(),1,List.copyOf(parts)));
                }
            }
        }catch(Exception error){CommandcookiesElectronicModMod.LOGGER.error("Could not load workbench recipes",error);return List.of();}
        return List.copyOf(recipes);
    }
    /** Simulate both consumption and output insertion before changing a single real slot. */
    public static Plan plan(Inventory inventory,Recipe recipe) {
        List<ItemStack> items=inventory.items.stream().map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for(var part:recipe.parts()) {
            int remaining=part.amount();
            for(var item:items)if(part.matches(item)) {
                int take=Math.min(remaining,item.getCount());item.shrink(take);remaining-=take;if(remaining==0)break;
            }
            if(remaining!=0)return new Plan(MISSING,List.of());
        }
        ItemStack result=stack(recipe.output());if(result.isEmpty())return new Plan(MISSING,List.of());result.setCount(recipe.outputCount());
        for(var item:items)if(!item.isEmpty()&&ItemStack.isSameItemSameComponents(item,result)) {
            int transfer=Math.min(result.getCount(),Math.max(0,Math.min(inventory.getMaxStackSize(),item.getMaxStackSize())-item.getCount()));
            item.grow(transfer);result.shrink(transfer);if(result.isEmpty())break;
        }
        if(!result.isEmpty())for(int i=0;i<items.size();i++)if(items.get(i).isEmpty()) {
            int transfer=Math.min(result.getCount(),Math.min(inventory.getMaxStackSize(),result.getMaxStackSize()));items.set(i,result.copyWithCount(transfer));result.shrink(transfer);if(result.isEmpty())break;
        }
        return result.isEmpty()?new Plan(READY,List.copyOf(items)):new Plan(FULL,List.of());
    }
}
