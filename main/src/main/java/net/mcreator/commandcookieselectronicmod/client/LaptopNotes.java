package net.mcreator.commandcookieselectronicmod.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.neoforged.fml.loading.FMLPaths;
import net.mcreator.commandcookieselectronicmod.CommandcookiesElectronicModMod;

/** Private local notebook for one player's physical laptop. */
public final class LaptopNotes {
    public static final int MAX_NOTES=64,MAX_TEXT=8000;
    public static final class Note {
        public final String id;
        private String title,text;
        Note(String id,String title,String text){this.id=id;this.title=title;this.text=text;}
        public String title(){return title.isBlank()?"Untitled note":title;}
        public String rawTitle(){return title;}
        public String text(){return text;}
    }
    private final Path path;
    private final List<Note> notes=new ArrayList<>();
    private boolean dirty,loadFailed,saveFailed;
    private long editedAt,nextAttempt;
    LaptopNotes(Path path){
        this.path=path;
        try{
            if(Files.exists(path)){
                if(Files.size(path)>8_000_000)throw new IllegalStateException("Notebook file exceeds limit");
                JsonObject data=new Gson().fromJson(Files.readString(path),JsonObject.class);
                for(var entry:data.getAsJsonArray("notes")){
                    if(notes.size()>=MAX_NOTES)break;var n=entry.getAsJsonObject();String id=n.get("id").getAsString();
                    if(find(id)!=null)throw new IllegalStateException("Duplicate note ID");
                    notes.add(new Note(id,limit(n.get("title").getAsString(),48),limit(n.get("text").getAsString(),MAX_TEXT)));
                }
            }
        }catch(Exception error){notes.clear();loadFailed=true;CommandcookiesElectronicModMod.LOGGER.warn("Could not load laptop notes; preserving original before future saves",error);}
    }
    private static String limit(String s,int max){return s.substring(0,Math.min(max,s.length()));}
    public List<Note> all(){return List.copyOf(notes);}
    public Note find(String id){return notes.stream().filter(n->n.id.equals(id)).findFirst().orElse(null);}
    public Note create(){if(notes.size()>=MAX_NOTES)return null;Note n=new Note(UUID.randomUUID().toString(),"Untitled note","");notes.add(0,n);changed();return n;}
    public void update(String id,String title,String text){Note n=find(id);if(n==null)return;title=limit(title,48);text=limit(text,MAX_TEXT);if(!n.title.equals(title)||!n.text.equals(text)){n.title=title;n.text=text;changed();}}
    public void delete(String id){if(notes.removeIf(n->n.id.equals(id)))changed();}
    private void changed(){dirty=true;editedAt=System.currentTimeMillis();}
    public String status(){if(saveFailed)return "Save failed - try Save";if(loadFailed&&!dirty)return "Old file unreadable; backup kept on save";return dirty?"Saving...":"Saved on this computer";}
    public void autosave(){long now=System.currentTimeMillis();if(dirty&&now-editedAt>=800&&now>=nextAttempt)save();}
    public boolean save(){
        if(!dirty)return !saveFailed;
        try{
            Files.createDirectories(path.toAbsolutePath().getParent());
            if(loadFailed&&Files.exists(path)){Files.copy(path,path.resolveSibling(path.getFileName()+".backup-"+System.currentTimeMillis()));loadFailed=false;}
            JsonObject data=new JsonObject();data.addProperty("version",1);JsonArray list=new JsonArray();
            for(Note note:notes){JsonObject n=new JsonObject();n.addProperty("id",note.id);n.addProperty("title",note.title);n.addProperty("text",note.text);list.add(n);}data.add("notes",list);
            Path temp=path.resolveSibling(path.getFileName()+".tmp");Files.writeString(temp,new Gson().toJson(data));
            try{Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}
            dirty=false;saveFailed=false;return true;
        }catch(Exception error){saveFailed=true;nextAttempt=System.currentTimeMillis()+3000;CommandcookiesElectronicModMod.LOGGER.warn("Could not save laptop notes",error);return false;}
    }
}
