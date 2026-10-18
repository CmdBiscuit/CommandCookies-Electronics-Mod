package net.mcreator.commandcookiesretroexpansion.retro;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
public final class RetroWorld extends SavedData {
 public UUID id=UUID.randomUUID();
 public RetroWorld(){setDirty();}
 public static RetroWorld get(MinecraftServer s){return s.overworld().getDataStorage().computeIfAbsent(new Factory<>(RetroWorld::new,RetroWorld::load,null),"cem_retro_world");}
 private static RetroWorld load(CompoundTag t,HolderLookup.Provider p){var d=new RetroWorld();if(t.hasUUID("World"))d.id=t.getUUID("World");return d;}
 @Override public CompoundTag save(CompoundTag t,HolderLookup.Provider p){t.putUUID("World",id);return t;}
}
