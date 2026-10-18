package net.mcreator.commandcookieselectronicmod.client;

import java.util.List;
import java.util.Comparator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.JukeboxSong;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid="commandcookies_electronic_mod", value=Dist.CLIENT)
public final class PhoneMusic {
    public record Track(ResourceLocation id, JukeboxSong song) {
        public String title() {
            String description = song.description().getString();
            int separator = description.indexOf(" - ");
            return separator < 0 ? description : description.substring(separator + 3);
        }
    }
    private static Track selected;
    private static PhoneSound sound;
    private static PhonePreferences activePrefs;
    private static final class PhoneSound extends SimpleSoundInstance implements net.minecraft.client.resources.sounds.TickableSoundInstance {
        PhoneSound(Track track,float volume) {
            super(track.song().soundEvent().value().getLocation(), SoundSource.RECORDS,volume,1.0f,SoundInstance.createUnseededRandom(),false,0,SoundInstance.Attenuation.NONE,0,0,0,true);
        }
        void volume(float value){this.volume=value;}
        @Override public void tick(){}
        @Override public boolean isStopped(){return false;}
        @Override public boolean canStartSilent(){return true;}
    }
    public static void applyPreferences(PhonePreferences prefs) {
        if(sound!=null&&activePrefs==prefs) {
            sound.volume(prefs.silent?0:prefs.musicVolume/100f);
        }
    }
    private static int elapsedTicks, graceTicks;
    private PhoneMusic() {}

    public static List<Track> tracks() {
        var game = Minecraft.getInstance();
        if (game.level == null) return List.of();
        var registry = game.level.registryAccess().registryOrThrow(Registries.JUKEBOX_SONG);
        return registry.entrySet().stream().filter(e -> e.getKey().location().getNamespace().equals("minecraft"))
            .map(e -> new Track(e.getKey().location(), e.getValue()))
            .sorted(Comparator.comparing(Track::title, String.CASE_INSENSITIVE_ORDER)).toList();
    }
    public static Track selected() { return selected; }
    public static void restoreSelection(String id) {
        if (playing() || id == null || id.isEmpty()) return;
        selected = tracks().stream().filter(track -> track.id().toString().equals(id)).findFirst().orElse(null);
    }
    public static boolean playing() { return sound != null; }
    public static void play(Track track) { play(track,null); }
    public static void play(Track track,PhonePreferences prefs) {
        stop();
        var game = Minecraft.getInstance();
        if (game.level == null) return;
        selected = track;
        activePrefs=prefs;sound=new PhoneSound(track,prefs==null?1:prefs.silent?0:prefs.musicVolume/100f);
        elapsedTicks = 0; graceTicks = 20;
        game.getMusicManager().stopPlaying();
        game.getSoundManager().play(sound);
    }
    public static void playSelected() { playSelected(null); }
    public static void playSelected(PhonePreferences prefs) {
        var all = tracks();
        if (all.isEmpty()) return;
        Track track = selected == null ? all.get(0) : all.stream().filter(t -> t.id().equals(selected.id())).findFirst().orElse(all.get(0));
        play(track,prefs);
    }
    public static void skip(int direction) { skip(direction,null); }
    public static void skip(int direction,PhonePreferences prefs) {
        var all = tracks();
        if (all.isEmpty()) return;
        int index = -1;
        for (int i = 0; i < all.size(); i++) if (selected != null && all.get(i).id().equals(selected.id())) index = i;
        play(all.get(index < 0 ? 0 : Math.floorMod(index + direction, all.size())),prefs);
    }
    public static void stop() {
        if (sound != null) Minecraft.getInstance().getSoundManager().stop(sound);
        sound = null;
    }
    public static String elapsed() { return time(elapsedTicks / 20); }
    public static String duration() { return time(selected == null ? 0 : (int)selected.song().lengthInSeconds()); }
    private static String time(int seconds) { return String.format("%d:%02d", seconds / 60, seconds % 60); }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var game = Minecraft.getInstance();
        if (game.level == null || game.player == null) { stop(); selected = null; elapsedTicks = 0; return; }
        if (sound == null || game.isPaused()) return;
        elapsedTicks++;
        if (elapsedTicks % 20 == 0) game.getMusicManager().stopPlaying();
        if (graceTicks > 0) graceTicks--;
        else if (!game.getSoundManager().isActive(sound)) {if(activePrefs!=null&&activePrefs.autoNext)skip(1,activePrefs);else stop();}
    }
}
