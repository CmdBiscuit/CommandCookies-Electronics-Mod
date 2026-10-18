package net.mcreator.commandcookieselectronicmod.email;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** A world-local mail account belongs to an authenticated player, not a computer. */
public final class EmailData extends SavedData {
    public static final int ACCOUNTS=4096,HISTORY=128,TOTAL_MESSAGES=32768;
    public record Entry(UUID id,String from,String to,String subject,String body,long time,boolean outgoing,boolean read,int photoWidth,int photoHeight) {
        public Entry(UUID id,String from,String to,String subject,String body,long time,boolean outgoing,boolean read){this(id,from,to,subject,body,time,outgoing,read,0,0);}
        public boolean hasPhoto(){return photoWidth>0&&photoHeight>0;}
    }
    private record StoredPhoto(int width,int height,byte[] bytes) {}
    private final Map<UUID,StoredPhoto> photos=new LinkedHashMap<>();
    private final Map<UUID,Integer> references=new HashMap<>();
    private int photoBytes;
    private static final class Account {
        final String address;final ArrayList<Entry> messages=new ArrayList<>();
        Account(String address){this.address=address;}
    }
    private final Map<UUID,Account> accounts=new LinkedHashMap<>();
    private final Map<String,UUID> addresses=new HashMap<>();
    private int total;
    public static EmailData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(EmailData::new,EmailData::load,null),"cem_email");}
    public static String normalize(String input){return input==null?"":input.strip().toLowerCase(Locale.ROOT);}
    public static boolean validAddress(String value){return value!=null&&value.matches("[a-z0-9][a-z0-9._-]{2,31}@email\\.com");}
    public static String registration(String input){String a=normalize(input);return a.contains("@")?a:a+"@email.com";}
    public static boolean validSubject(String s){return s!=null&&!s.isBlank()&&s.length()<=80&&s.codePoints().noneMatch(c->Character.isISOControl(c)||c==0xA7);}
    public static boolean validBody(String s){return validText(s,false);}
    private static boolean validText(String s,boolean picture){return s!=null&&(picture||!s.isBlank())&&s.length()<=2048&&s.codePoints().noneMatch(c->c==0xA7||Character.isISOControl(c)&&c!='\n'&&c!='\t');}
    public String address(UUID player){var a=accounts.get(player);return a==null?"":a.address;}
    public UUID owner(String address){return addresses.get(normalize(address));}
    public List<Entry> history(UUID player){var a=accounts.get(player);return a==null?List.of():List.copyOf(a.messages);}
    public int unread(UUID player){return (int)history(player).stream().filter(e->!e.outgoing()&&!e.read()).count();}
    public String create(UUID player,String input){
        if(accounts.containsKey(player))return "You already have an email address.";
        String address=registration(input);if(!validAddress(address))return "Use 3-32 letters, numbers, dots, dashes or underscores.";
        if(addresses.containsKey(address))return "That address is already taken.";
        if(accounts.size()>=ACCOUNTS)return "The server's email account storage is full.";
        accounts.put(player,new Account(address));addresses.put(address,player);setDirty();return "";
    }
    public String deliver(UUID sender,String recipient,String subject,String body){return deliver(sender,recipient,subject,body,new byte[0]);}
    public String deliver(UUID sender,String recipient,String subject,String body,byte[] image){
        var a=accounts.get(sender);var target=owner(recipient);var b=target==null?null:accounts.get(target);
        if(a==null)return "Create your email address first.";
        if(b==null)return "That email address does not exist in this world.";
        if(!validSubject(subject)||!validText(body,image!=null&&image.length>0))return "Add a subject and message (80 / 2048 characters max).";
        int needed=2;if(total+needed>TOTAL_MESSAGES)return "The server's mail storage is full.";
        if(a.messages.size()+(a==b?2:1)>HISTORY)return "Your mailbox is full. Delete some emails first.";
        if(b.messages.size()+(a==b?2:1)>HISTORY)return "That mailbox is full. Your email was not sent.";
        EmailPhoto.Size size=null;
        if(image==null)return "Invalid photo attachment.";
        if(image.length>0){
            if(photos.size()>=EmailPhoto.MAX_PHOTOS||image.length>EmailPhoto.MAX_STORAGE-photoBytes)return "The server photo storage is full. Send without a photo.";
            try{size=EmailPhoto.inspect(image,true);}catch(java.io.IOException ex){return "That photo could not be read. Attach it again.";}
        }
        UUID id=UUID.randomUUID();long now=System.currentTimeMillis();int w=size==null?0:size.width(),h=size==null?0:size.height();
        if(size!=null){photos.put(id,new StoredPhoto(w,h,image.clone()));references.put(id,2);photoBytes+=image.length;}
        a.messages.add(new Entry(id,a.address,b.address,subject,body,now,true,true,w,h));
        b.messages.add(new Entry(id,a.address,b.address,subject,body,now,false,false,w,h));total+=needed;setDirty();return "";
    }
    public boolean read(UUID player,UUID id){var a=accounts.get(player);if(a==null)return false;for(int i=0;i<a.messages.size();i++){var e=a.messages.get(i);if(e.id().equals(id)&&!e.outgoing()){if(!e.read()){a.messages.set(i,new Entry(e.id(),e.from(),e.to(),e.subject(),e.body(),e.time(),false,true,e.photoWidth(),e.photoHeight()));setDirty();}return true;}}return false;}
    public boolean delete(UUID player,UUID id,boolean outgoing){var a=accounts.get(player);if(a==null)return false;int before=a.messages.size();a.messages.removeIf(e->e.id().equals(id)&&e.outgoing()==outgoing);int removed=before-a.messages.size();if(removed>0){total-=removed;if(photos.containsKey(id)){int remaining=references.getOrDefault(id,removed)-removed;if(remaining<=0){photoBytes-=photos.remove(id).bytes().length;references.remove(id);}else references.put(id,remaining);}setDirty();}return removed>0;}
    /** The caller must own this exact Inbox/Sent entry; knowing an image UUID grants no access. */
    public byte[] attachment(UUID player,UUID id,boolean outgoing){
        if(history(player).stream().noneMatch(e->e.id().equals(id)&&e.outgoing()==outgoing&&e.hasPhoto()))return new byte[0];
        var photo=photos.get(id);return photo==null?new byte[0]:photo.bytes().clone();
    }
    public static EmailData load(CompoundTag tag,HolderLookup.Provider registries){
        var data=new EmailData();
        for(var raw:tag.getList("Photos",Tag.TAG_COMPOUND)){
            if(data.photos.size()>=EmailPhoto.MAX_PHOTOS)break;var t=(CompoundTag)raw;if(!t.hasUUID("Id")||data.photos.containsKey(t.getUUID("Id")))continue;
            byte[] bytes=t.getByteArray("Bytes");if(bytes.length>EmailPhoto.MAX_STORAGE-data.photoBytes)continue;
            try{var size=EmailPhoto.inspect(bytes,false);data.photos.put(t.getUUID("Id"),new StoredPhoto(size.width(),size.height(),bytes));data.photoBytes+=bytes.length;}catch(java.io.IOException ignored){}
        }
        for(var raw:tag.getList("Accounts",Tag.TAG_COMPOUND)){
            if(data.accounts.size()>=ACCOUNTS)break;var t=(CompoundTag)raw;String address=normalize(t.getString("Address"));
            if(!t.hasUUID("Player")||!validAddress(address)||data.addresses.containsKey(address)||data.accounts.containsKey(t.getUUID("Player")))continue;
            var a=new Account(address);var ids=new HashSet<String>();
            for(var item:t.getList("Messages",Tag.TAG_COMPOUND)){
                if(a.messages.size()>=HISTORY||data.total>=TOTAL_MESSAGES)break;var e=(CompoundTag)item;String from=e.getString("From"),to=e.getString("To"),subject=e.getString("Subject"),body=e.getString("Body");boolean outgoing=e.getBoolean("Outgoing");
                if(!e.hasUUID("Id")||!validAddress(from)||!validAddress(to)||!validSubject(subject)||!validText(body,true)||!(outgoing?from:to).equals(address)||!ids.add(e.getUUID("Id")+":"+outgoing))continue;
                var photo=e.getBoolean("Photo")?data.photos.get(e.getUUID("Id")):null;
                if(photo!=null)data.references.merge(e.getUUID("Id"),1,Integer::sum);
                if(photo==null&&body.isBlank())body="[Photo unavailable]";
                a.messages.add(new Entry(e.getUUID("Id"),from,to,subject,body,Math.max(0,e.getLong("Time")),outgoing,e.getBoolean("Read")||outgoing,photo==null?0:photo.width(),photo==null?0:photo.height()));data.total++;
            }
            UUID player=t.getUUID("Player");data.accounts.put(player,a);data.addresses.put(address,player);
        }
        data.photos.entrySet().removeIf(e->!data.references.containsKey(e.getKey()));data.photoBytes=data.photos.values().stream().mapToInt(e->e.bytes().length).sum();return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){var list=new ListTag();accounts.forEach((player,a)->{var t=new CompoundTag();t.putUUID("Player",player);t.putString("Address",a.address);var messages=new ListTag();for(var e:a.messages){var v=new CompoundTag();v.putUUID("Id",e.id());v.putString("From",e.from());v.putString("To",e.to());v.putString("Subject",e.subject());v.putString("Body",e.body());v.putLong("Time",e.time());v.putBoolean("Outgoing",e.outgoing());v.putBoolean("Read",e.read());v.putBoolean("Photo",e.hasPhoto());messages.add(v);}t.put("Messages",messages);list.add(t);});tag.put("Accounts",list);var images=new ListTag();photos.forEach((id,p)->{var v=new CompoundTag();v.putUUID("Id",id);v.putByteArray("Bytes",p.bytes());images.add(v);});tag.put("Photos",images);return tag;}
}
