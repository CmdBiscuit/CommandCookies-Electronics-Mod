package net.mcreator.commandcookieselectronicmod.client;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

/** Short local arcade effects; bounded voices and no audio work during rendering. */
final class LaptopGameSounds {
    private static final EnumMap<LaptopGames.Cue,Long> LAST=new EnumMap<>(LaptopGames.Cue.class);
    private static final ArrayDeque<SoundInstance> VOICES=new ArrayDeque<>();
    private static long lastPlay;
    private LaptopGameSounds() {}

    static ResourceLocation event(LaptopGames.Cue cue) {
        return ResourceLocation.fromNamespaceAndPath("cem_arcade","laptop."+cue.name().toLowerCase(Locale.ROOT));
    }
    private static int priority(LaptopGames.Cue cue) {
        return switch(cue) {
            case WIN,LOSE,DRAW -> 3;
            case START,PAUSE,EAT,MERGE,BRICK,LINE,MATCH,MISS,READY,TOO_SOON,UNDO,LIFE -> 2;
            default -> 1;
        };
    }
    static void play(List<LaptopGames.Cue> cues) {
        if(cues.isEmpty())return;
        var chosen=cues.getFirst();
        for(var cue:cues)if(priority(cue)>=priority(chosen))chosen=cue;
        play(chosen);
    }
    static void play(LaptopGames.Cue cue) {
        var game=Minecraft.getInstance();
        if(!(game.screen instanceof LaptopDesktopScreen desktop)||desktop.preferences().gameVolume<=0
            ||game.options.getSoundSourceVolume(SoundSource.MASTER)<=0)return;
        long now=System.nanoTime();
        if(now-lastPlay<25_000_000L||now-LAST.getOrDefault(cue,0L)<45_000_000L)return;
        var manager=game.getSoundManager();
        VOICES.removeIf(sound->!manager.isActive(sound));
        if(priority(cue)==3)stop();
        while(VOICES.size()>=4)manager.stop(VOICES.removeFirst());
        var sound=new SimpleSoundInstance(event(cue),SoundSource.MASTER,
            desktop.preferences().gameVolume/100f,1,SoundInstance.createUnseededRandom(),
            false,0,SoundInstance.Attenuation.NONE,0,0,0,true);
        VOICES.addLast(sound);LAST.put(cue,now);lastPlay=now;
        manager.play(sound);
    }
    static void stop() {
        var manager=Minecraft.getInstance().getSoundManager();
        for(var sound:VOICES)manager.stop(sound);
        VOICES.clear();LAST.clear();lastPlay=0;
    }
}
