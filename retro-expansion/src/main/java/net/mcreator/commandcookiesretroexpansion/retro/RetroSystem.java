package net.mcreator.commandcookiesretroexpansion.retro;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Shared console identities; no client or emulator dependencies. */
public enum RetroSystem {
 NES("nes","NES","mesen",16,List.of("nes")),
 SNES("snes","SNES","bsnes",16,List.of("sfc","smc")),
 N64("n64","N64","parallel_n64",128,List.of("z64","n64","v64"));
 public final String id,label,core;public final int maxBytes;public final List<String> extensions;
 RetroSystem(String id,String label,String core,int mb,List<String> extensions){this.id=id;this.label=label;this.core=core;this.maxBytes=mb*1024*1024;this.extensions=extensions;}
 public static RetroSystem fromId(String id){for(var s:values())if(s.id.equals(id))return s;throw new IllegalArgumentException("Unknown console type");}
 public boolean accepts(String file){String name=file.toLowerCase(Locale.ROOT);return extensions.stream().anyMatch(e->name.endsWith("."+e));}
 public String formats(){return String.join(" / ",extensions.stream().map(e->"."+e).toList());}
 public boolean controller(Player player){return controller(player.getMainHandItem())||controller(player.getOffhandItem());}
 private boolean controller(ItemStack stack){var key=BuiltInRegistries.ITEM.getKey(stack.getItem());return key.getNamespace().equals("commandcookies_electronic_mod")&&key.getPath().equals(id+"_controller");}
}
