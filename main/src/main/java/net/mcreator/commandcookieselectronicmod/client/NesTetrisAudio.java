package net.mcreator.commandcookieselectronicmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

/** Streaming soundtrack is local to the player using the monitor. */
public final class NesTetrisAudio {
    private SoundInstance music;
    private String current="";
    private int grace;
    private boolean victoryPending;
    public void play(String name,boolean loop){
        if(current.equals(name))return;stop();current=name;if(name.isEmpty())return;
        PhoneMusic.stop();LaptopMusic.stop();var g=Minecraft.getInstance();g.getMusicManager().stopPlaying();
        music=new SimpleSoundInstance(ResourceLocation.fromNamespaceAndPath("cem_nes",name),SoundSource.RECORDS,.55f,1,
            SoundInstance.createUnseededRandom(),loop,0,SoundInstance.Attenuation.NONE,0,0,0,true);
        grace=30;g.getSoundManager().play(music);
    }
    public void success(){play("success",false);victoryPending=true;}
    public void tick(){
        var g=Minecraft.getInstance();if(music==null)return;g.getMusicManager().stopPlaying();
        if(grace>0)grace--;else if(victoryPending&&!g.getSoundManager().isActive(music)){victoryPending=false;play("victory",false);}
    }
    public void stop(){if(music!=null)Minecraft.getInstance().getSoundManager().stop(music);music=null;current="";victoryPending=false;}
    public static void effect(String name){
        var g=Minecraft.getInstance();g.getSoundManager().play(new SimpleSoundInstance(ResourceLocation.fromNamespaceAndPath("cem_arcade","laptop."+name),SoundSource.PLAYERS,.24f,1,SoundInstance.createUnseededRandom(),false,0,SoundInstance.Attenuation.NONE,0,0,0,true));
    }
}
