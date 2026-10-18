package net.mcreator.commandcookiesretroexpansion.retro.mixin;

import java.util.ArrayList;
import java.util.List;
import net.mcreator.commandcookieselectronicmod.inventory.WorkbenchRecipes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds the addon cartridge to the existing workbench without replacing the base mod. */
@Mixin(value=WorkbenchRecipes.class,remap=false)
public abstract class WorkbenchRecipesMixin {
    @Inject(method="load",at=@At("RETURN"),cancellable=true,remap=false)
    private static void retro$addCartridge(CallbackInfoReturnable<List<WorkbenchRecipes.Recipe>> result) {
        var original=result.getReturnValue();
        if(original.isEmpty() || original.stream().anyMatch(r->r.id().equals("retro_rom_cartridge")))return;
        var recipes=new ArrayList<>(original);
        int after=recipes.size();
        for(int i=0;i<recipes.size();i++)if(recipes.get(i).family().equalsIgnoreCase("Cartridges"))after=i+1;
        for(var system:net.mcreator.commandcookiesretroexpansion.retro.RetroSystem.values()){
            String item=system==net.mcreator.commandcookiesretroexpansion.retro.RetroSystem.NES?"rom_cartridge":system.id+"_rom_cartridge";
            String id=system==net.mcreator.commandcookiesretroexpansion.retro.RetroSystem.NES?"retro_rom_cartridge":"retro_"+system.id+"_rom_cartridge";
            var recipe=new WorkbenchRecipes.Recipe(id,system.label+" ROM Cartridge","Cartridges",
                "commandcookies_retro_expansion:"+item,1,List.of(
                    new WorkbenchRecipes.Part("Iron Ingot","minecraft:iron_ingot",2),
                    new WorkbenchRecipes.Part("Basic Circuit Board","commandcookies_electronic_mod:basic_circuit_board",1),
                    new WorkbenchRecipes.Part("Redstone Dust","minecraft:redstone",1),
                    new WorkbenchRecipes.Part("Paper","minecraft:paper",1)));
            recipes.add(after++,recipe);
        }
        result.setReturnValue(List.copyOf(recipes));
    }
}
