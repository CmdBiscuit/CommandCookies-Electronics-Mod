package net.mcreator.commandcookieselectronicmod.phone;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** World-local inboxes, private to a player and one server-issued physical phone. */
public final class PhoneMessageData extends SavedData {
    public static final int HISTORY=128,MAILBOXES=2048;
    public record Key(UUID player,UUID device){}
    public record Entry(UUID id,UUID peer,String name,String text,long time,boolean outgoing,boolean read){}
    private final Map<Key,ArrayList<Entry>> boxes=new LinkedHashMap<>();
    public static PhoneMessageData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(PhoneMessageData::new,PhoneMessageData::load,null),"cem_phone_messages");}
    public List<Entry> history(UUID player,UUID device){var box=boxes.get(new Key(player,device));return box==null?List.of():List.copyOf(box);}
    public boolean canStore(Key a,Key b){return boxes.size()+(boxes.containsKey(a)?0:1)+(boxes.containsKey(b)||a.equals(b)?0:1)<=MAILBOXES;}
    public void append(Key key,Entry entry){var box=boxes.computeIfAbsent(key,k->new ArrayList<>());box.add(entry);if(box.size()>HISTORY)box.removeFirst();setDirty();}
    public void read(UUID player,UUID device,UUID peer){var box=boxes.get(new Key(player,device));if(box==null)return;boolean changed=false;for(int i=0;i<box.size();i++){var e=box.get(i);if(!e.outgoing()&&!e.read()&&e.peer().equals(peer)){box.set(i,new Entry(e.id(),e.peer(),e.name(),e.text(),e.time(),false,true));changed=true;}}if(changed)setDirty();}
    public static boolean validText(String text){return text!=null&&!text.isBlank()&&text.length()<=256&&text.codePoints().noneMatch(c->Character.isISOControl(c)||c==0xA7);}
    public static PhoneMessageData load(CompoundTag tag,HolderLookup.Provider registries){
        var data=new PhoneMessageData();
        for(var item:tag.getList("Mailboxes",Tag.TAG_COMPOUND)){
            if(data.boxes.size()>=MAILBOXES)break;var box=(CompoundTag)item;if(!box.hasUUID("Player")||!box.hasUUID("Device"))continue;
            var key=new Key(box.getUUID("Player"),box.getUUID("Device"));var entries=new ArrayList<Entry>();
            for(var raw:box.getList("Messages",Tag.TAG_COMPOUND)){
                var e=(CompoundTag)raw;String text=e.getString("Text"),name=e.getString("Name");
                if(!e.hasUUID("Id")||!e.hasUUID("Peer")||name.length()>64||!validText(text))continue;
                entries.add(new Entry(e.getUUID("Id"),e.getUUID("Peer"),name,text,Math.max(0,e.getLong("Time")),e.getBoolean("Outgoing"),e.getBoolean("Read")));if(entries.size()>HISTORY)entries.removeFirst();
            }
            if(!entries.isEmpty())data.boxes.put(key,entries);
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){
        var mailboxes=new ListTag();boxes.forEach((key,entries)->{var box=new CompoundTag();box.putUUID("Player",key.player());box.putUUID("Device",key.device());var list=new ListTag();
            for(var e:entries){var message=new CompoundTag();message.putUUID("Id",e.id());message.putUUID("Peer",e.peer());message.putString("Name",e.name());message.putString("Text",e.text());message.putLong("Time",e.time());message.putBoolean("Outgoing",e.outgoing());message.putBoolean("Read",e.read());list.add(message);}box.put("Messages",list);mailboxes.add(box);
        });tag.put("Mailboxes",mailboxes);return tag;
    }
}
