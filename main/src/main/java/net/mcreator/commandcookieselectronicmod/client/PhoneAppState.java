package net.mcreator.commandcookieselectronicmod.client;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import net.mcreator.commandcookieselectronicmod.calculator.CalculatorEngine;
import net.mcreator.commandcookieselectronicmod.CommandcookiesElectronicModMod;
import net.neoforged.fml.loading.FMLPaths;

/** Private app data per player and physical phone. Legacy color-based files are preserved. */
public final class PhoneAppState {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    public static final PhoneAppState INSTANCE=new PhoneAppState(Path.of(System.getProperty("phone.state.path",FMLPaths.CONFIGDIR.get().resolve("commandcookies_phone_apps.json").toString())));
    private static final String UUID_PATTERN="[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}";
    private final Path directory;
    private final LinkedHashMap<String,Profile> profiles=new LinkedHashMap<>(32,.75f,true);
    private final Set<String> dirty=new HashSet<>(),loadFailed=new HashSet<>();
    private Profile active;
    private long changedAt;
    public static final class Profile {
        public transient CalculatorEngine calculator = new CalculatorEngine();
        private CalculatorEngine.Snapshot calculation;
        public int homePage, musicPage;
        private String notes="";
        public String notes(){return notes;}
        public void setNotes(String value){notes=value.substring(0,Math.min(4000,value.length()));}
        public PhonePreferences settings = new PhonePreferences();
        public String musicTrack = "";
        private long elapsed, startedAt;
        private boolean running;
        private ArrayList<Long> laps = new ArrayList<>();
        // Bound elapsed time so damaged save files cannot overflow arithmetic or the display.
        private static final long MAX_TIME = 315_360_000_000L;
        public long elapsedMillis() {
            long delta = running ? Math.max(0, Math.min(MAX_TIME, System.currentTimeMillis() - startedAt)) : 0;
            return Math.min(MAX_TIME, elapsed + delta);
        }
        public boolean running() { return running; }
        public java.util.List<Long> laps() { return java.util.List.copyOf(laps); }
        public void toggle() {
            if (running) elapsed = elapsedMillis();
            else startedAt = System.currentTimeMillis();
            running = !running;
        }
        public void reset() { elapsed = startedAt = 0; running = false; laps.clear(); }
        public void lap() { if (running && laps.size() < 99) laps.add(elapsedMillis()); }
        private void sanitize() {
            if(notes==null)notes="";if(notes.length()>4000)notes=notes.substring(0,4000);
            if (settings == null) settings = new PhonePreferences(); settings.sanitize();
            calculator = new CalculatorEngine(); calculator.restore(calculation);
            homePage = Math.max(0, Math.min(1, homePage)); musicPage = Math.max(0, Math.min(100, musicPage));
            if (musicTrack == null || musicTrack.length() > 128) musicTrack = "";
            elapsed = Math.max(0, Math.min(MAX_TIME, elapsed));
            if (startedAt <= 0 || startedAt > System.currentTimeMillis()) { running = false; startedAt = 0; }
            if (laps == null) laps = new ArrayList<>();
            laps.removeIf(value -> value == null || value < 0 || value > MAX_TIME);
            while (laps.size() > 99) laps.remove(laps.size() - 1);
        }
    }

    public PhoneAppState(Path legacyFile){directory=legacyFile.toAbsolutePath().normalize().resolveSibling(legacyFile.getFileName()+".devices");}
    public static String key(UUID player,UUID device){return "device/"+player+"/"+device;}
    private static boolean valid(String name){return name!=null&&name.matches("device/"+UUID_PATTERN+"/"+UUID_PATTERN);}
    private Path file(String name){if(!valid(name))throw new IllegalArgumentException("Invalid phone profile");String[] parts=name.split("/");return directory.resolve(parts[1]).resolve(parts[2]+".json");}
    public Profile profile(String name){
        Path path=file(name);var known=profiles.get(name);if(known!=null)return known;
        Profile profile=new Profile();
        try{if(Files.exists(path)){if(Files.size(path)>65536)throw new IllegalStateException("Phone profile too large");var saved=JSON.fromJson(Files.readString(path),Profile.class);if(saved==null)throw new IllegalStateException("Invalid phone profile");saved.sanitize();profile=saved;}}
        catch(Exception error){loadFailed.add(name);CommandcookiesElectronicModMod.LOGGER.warn("Could not load this phone; original will be backed up on save",error);}
        profiles.put(name,profile);trim();return profile;
    }
    public void activate(Profile profile){active=profile;}
    private void trim(){if(profiles.size()<=256)return;var it=profiles.entrySet().iterator();while(it.hasNext()&&profiles.size()>256){var entry=it.next();if(entry.getValue()!=active&&!dirty.contains(entry.getKey()))it.remove();}}
    public void changed(Profile profile){for(var entry:profiles.entrySet())if(entry.getValue()==profile){dirty.add(entry.getKey());changedAt=System.currentTimeMillis();return;}}
    public void flushIfReady(){if(!dirty.isEmpty()&&System.currentTimeMillis()-changedAt>=750)save();}
    public boolean save(){
        boolean success=true;
        for(String name:List.copyOf(dirty)){
            var profile=profiles.get(name);Path path=file(name);
            try{Files.createDirectories(path.getParent());if(loadFailed.contains(name)&&Files.exists(path)){Files.copy(path,path.resolveSibling(path.getFileName()+".backup-"+System.currentTimeMillis()));loadFailed.remove(name);}
                profile.calculation=profile.calculator.snapshot();String data=JSON.toJson(profile);if(data.length()>65536)throw new IllegalStateException("Phone save too large");Path temp=path.resolveSibling(path.getFileName()+".tmp");Files.writeString(temp,data);
                try{Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException ex){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}dirty.remove(name);
            }catch(Exception error){success=false;CommandcookiesElectronicModMod.LOGGER.warn("Could not save this phone's private profile",error);}
        }
        trim();return success;
    }
    public void applyToSavedPhones(String current,PhonePreferences settings){
        if(!valid(current))return;String player=current.split("/")[1];Path folder=directory.resolve(player);
        var names=new LinkedHashSet<String>();for(String name:profiles.keySet())if(name.startsWith("device/"+player+"/"))names.add(name);
        try{if(Files.isDirectory(folder))try(var files=Files.list(folder)){files.filter(p->p.getFileName().toString().matches(UUID_PATTERN+"\\.json")).forEach(p->names.add("device/"+player+"/"+p.getFileName().toString().replace(".json","")));}}
        catch(Exception error){CommandcookiesElectronicModMod.LOGGER.warn("Could not list saved phones",error);}
        for(String name:names){var p=profile(name);p.settings.copyFrom(settings);changed(p);if(!save())break;}
    }
}
