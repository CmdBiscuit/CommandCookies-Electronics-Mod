package net.mcreator.commandcookieselectronicmod.client;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.mcreator.commandcookieselectronicmod.block.LaptopBlock;
import net.mcreator.commandcookieselectronicmod.block.entity.LaptopBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Local record playback follows its laptop, independently of the desktop window. */
@EventBusSubscriber(modid="commandcookies_electronic_mod",value=Dist.CLIENT)
public final class LaptopMusic {
    private static SoundInstance sound;
    private static LaptopPreferences prefs;
    private static final class LaptopSound extends SimpleSoundInstance implements net.minecraft.client.resources.sounds.TickableSoundInstance {
        LaptopSound(PhoneMusic.Track track){super(track.song().soundEvent().value().getLocation(),SoundSource.RECORDS,prefs.musicVolume/100f,1,SoundInstance.createUnseededRandom(),false,0,SoundInstance.Attenuation.NONE,0,0,0,true);}
        @Override public void tick(){volume=prefs.musicVolume/100f;}
        @Override public boolean isStopped(){return LaptopMusic.sound!=this;}
        @Override public boolean canStartSilent(){return true;}
    }
    private static PhoneMusic.Track selected;
    private static BlockPos owner;
    private static ResourceKey<Level> dimension;
    private static UUID session;
    private static int elapsedTicks,graceTicks;
    private LaptopMusic(){}
    public static boolean owns(BlockPos pos){var g=Minecraft.getInstance();return g.level!=null&&pos.equals(owner)&&g.level.dimension().equals(dimension);}
    public static boolean playing(BlockPos pos){return owns(pos)&&sound!=null;}
    public static PhoneMusic.Track selected(BlockPos pos){return owns(pos)?selected:null;}
    public static float progress(BlockPos pos){return owns(pos)&&selected!=null?Math.min(1,elapsedTicks/(20f*selected.song().lengthInSeconds())):0;}
    public static String elapsed(BlockPos pos){return time(owns(pos)?elapsedTicks/20:0);}
    public static String time(int seconds){return String.format(java.util.Locale.ROOT,"%d:%02d",seconds/60,seconds%60);}
    public static void play(BlockPos pos,PhoneMusic.Track track){
        var g=Minecraft.getInstance();
        var access=DesktopAccess.at(g.level,pos);if(access==null)return;
        stop();PhoneMusic.stop();
        if(g.player==null)return;
        prefs=g.screen instanceof LaptopDesktopScreen desktop&&desktop.isLaptop(pos)?desktop.preferences():new LaptopProfile(g.player.getUUID(),access.device()).preferences;
        owner=pos.immutable();dimension=g.level.dimension();session=access.session();selected=track;elapsedTicks=0;graceTicks=30;
        sound=new LaptopSound(track);
        g.getMusicManager().stopPlaying();g.getSoundManager().play(sound);
    }
    public static void stop(BlockPos pos){if(owns(pos))stop();}
    public static void stop(){if(sound!=null)Minecraft.getInstance().getSoundManager().stop(sound);sound=null;}
    private static void clear(){stop();selected=null;owner=null;dimension=null;session=null;elapsedTicks=0;}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var g=Minecraft.getInstance();
        if(g.level==null||g.player==null){clear();return;}
        if(owner==null)return;
        var access=DesktopAccess.at(g.level,owner);
        if(!g.level.dimension().equals(dimension)||access==null||!access.session().equals(session)){clear();return;}
        if(sound==null||g.isPaused())return;
        if(PhoneMusic.playing()){stop();return;}
        elapsedTicks++;
        if(elapsedTicks%20==0)g.getMusicManager().stopPlaying();
        if(graceTicks>0)graceTicks--;else if(!g.getSoundManager().isActive(sound)){
            if(prefs.autoNext){var tracks=PhoneMusic.tracks();int index=0;for(int i=0;i<tracks.size();i++)if(tracks.get(i).id().equals(selected.id()))index=i;if(!tracks.isEmpty())play(owner,tracks.get((index+1)%tracks.size()));else stop();}else stop();
        }
    }
}
