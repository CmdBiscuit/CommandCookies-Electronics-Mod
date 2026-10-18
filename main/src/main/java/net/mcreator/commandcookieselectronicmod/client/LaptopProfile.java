package net.mcreator.commandcookieselectronicmod.client;

import java.nio.file.Path;
import java.util.UUID;
import net.neoforged.fml.loading.FMLPaths;

/** Fresh devices use fresh files. Legacy shared files are preserved, never inherited. */
final class LaptopProfile {
    final LaptopNotes notes;
    final LaptopPreferences preferences;
    final LaptopPrograms programs;
    static Path directory(UUID player,UUID device){
        return Path.of(System.getProperty("laptop.profile.path",FMLPaths.CONFIGDIR.get().resolve("commandcookies_laptop_devices").toString()))
            .resolve(player.toString()).resolve(device.toString());
    }
    LaptopProfile(UUID player,UUID device){
        var dir=directory(player,device);
        notes=new LaptopNotes(dir.resolve("notes.json"));
        preferences=LaptopPreferences.load(dir.resolve("settings.json"));
        programs=new LaptopPrograms(dir.resolve("programs.json"));
    }
}
