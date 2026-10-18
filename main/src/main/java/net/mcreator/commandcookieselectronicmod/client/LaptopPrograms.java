package net.mcreator.commandcookieselectronicmod.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import net.neoforged.fml.loading.FMLPaths;

/** Atomic local saves for one player and physical laptop. No executable downloads. */
public final class LaptopPrograms {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private Data data=new Data();
    private boolean dirty,broken;
    private int saveTicks;
    public static final class Task {public String text="";public boolean done;public Task(String t){text=t;}}
    public static final class Data {
        public Set<Integer> installed=new LinkedHashSet<>();
        public Map<Integer,Integer> downloads=new LinkedHashMap<>();
        public Map<String,Long> best=new HashMap<>();
        public List<String> bookmarks=new ArrayList<>(List.of("blocknet://apps","blocknet://guides")),history=new ArrayList<>();
        public List<Task> tasks=new ArrayList<>();
        public int[] paint=new int[1024],merge=new int[16];
        public int mergeScore;
        public long timerEnd,timerRemaining=25*60*1000;
        public boolean timerRunning,timerNotified;
    }
    public LaptopPrograms(Path file){this.file=file;try{if(Files.exists(file)){if(Files.size(file)>262144)throw new IllegalArgumentException("Save too large");Data saved=JSON.fromJson(Files.readString(file),Data.class);if(saved==null)throw new IllegalArgumentException("Empty save");data=saved;sanitize();}}catch(Exception error){broken=true;data=new Data();net.mcreator.commandcookieselectronicmod.CommandcookiesElectronicModMod.LOGGER.warn("Could not read laptop programs; preserving original save",error);}}
    private void sanitize(){
        if(data.installed==null)data.installed=new LinkedHashSet<>();data.installed.removeIf(id->id==null||LaptopCatalog.find(id)==null);
        if(data.downloads==null)data.downloads=new LinkedHashMap<>();data.downloads.entrySet().removeIf(e->e.getKey()==null||LaptopCatalog.find(e.getKey())==null||e.getValue()==null||e.getValue()<0||e.getValue()>40||data.installed.contains(e.getKey()));
        if(data.best==null)data.best=new HashMap<>();data.best.entrySet().removeIf(e->e.getKey()==null||e.getKey().length()>40||e.getValue()==null||e.getValue()<0||e.getValue()>1_000_000_000);
        if(data.bookmarks==null)data.bookmarks=new ArrayList<>();if(data.history==null)data.history=new ArrayList<>();data.bookmarks.removeIf(s->!LaptopBrowser.valid(s));data.history.removeIf(s->!LaptopBrowser.valid(s));while(data.bookmarks.size()>40)data.bookmarks.removeLast();while(data.history.size()>60)data.history.removeLast();
        if(data.tasks==null)data.tasks=new ArrayList<>();data.tasks.removeIf(t->t==null||t.text==null||t.text.isBlank());while(data.tasks.size()>64)data.tasks.removeLast();for(var task:data.tasks)task.text=task.text.substring(0,Math.min(100,task.text.length()));
        if(data.paint==null||data.paint.length!=1024)data.paint=new int[1024];for(int i=0;i<1024;i++)data.paint[i]=Math.max(0,Math.min(15,data.paint[i]));
        if(data.merge==null||data.merge.length!=16)data.merge=new int[16];for(int i=0;i<16;i++)if(data.merge[i]<0||data.merge[i]>65536||data.merge[i]!=0&&(data.merge[i]&(data.merge[i]-1))!=0)data.merge[i]=0;
        data.mergeScore=Math.max(0,Math.min(1_000_000_000,data.mergeScore));data.timerRemaining=Math.max(0,Math.min(3_600_000,data.timerRemaining));
        if(data.timerEnd<0||data.timerEnd>System.currentTimeMillis()+3_600_000){data.timerRunning=false;data.timerEnd=0;}
    }
    public Data data(){return data;}
    public boolean installed(int app){return app>=0&&(app<8||app==22||app==23||data.installed.contains(app));}
    public void changed(){dirty=true;}
    public boolean download(int app){if(LaptopCatalog.find(app)==null||installed(app)||data.downloads.containsKey(app))return false;data.downloads.put(app,0);changed();save();return true;}
    public void cancel(int app){data.downloads.remove(app);changed();save();}
    public void uninstall(int app){data.installed.remove(app);data.downloads.remove(app);changed();save();}
    public List<Integer> tick(){
        var completed=new ArrayList<Integer>();for(var e:new ArrayList<>(data.downloads.entrySet())){int n=e.getValue()+1;if(n>=40){data.downloads.remove(e.getKey());data.installed.add(e.getKey());completed.add(e.getKey());}else data.downloads.put(e.getKey(),n);changed();}
        if(!completed.isEmpty())save();else if(dirty&&++saveTicks>=20)save();return completed;
    }
    public long best(String game){return data.best.getOrDefault(game,0L);}
    public void score(String game,long score,boolean lower){long previous=best(game);if(score>0&&(previous==0||(lower?score<previous:score>previous))){data.best.put(game,score);changed();}}
    public void visit(String url){data.history.remove(url);data.history.addFirst(url);while(data.history.size()>60)data.history.removeLast();changed();}
    public void bookmark(String url){if(data.bookmarks.contains(url))data.bookmarks.remove(url);else if(data.bookmarks.size()<40)data.bookmarks.addFirst(url);changed();save();}
    public boolean addTask(String text){text=text.strip();if(text.isEmpty()||data.tasks.size()>=64)return false;data.tasks.add(new Task(text.substring(0,Math.min(100,text.length()))));changed();save();return true;}
    public long remaining(){return data.timerRunning?Math.max(0,data.timerEnd-System.currentTimeMillis()):data.timerRemaining;}
    public void timerToggle(){if(data.timerRunning){data.timerRemaining=remaining();data.timerRunning=false;}else{if(data.timerRemaining==0)data.timerRemaining=25*60*1000;data.timerEnd=System.currentTimeMillis()+data.timerRemaining;data.timerRunning=true;data.timerNotified=false;}changed();save();}
    public void timerReset(int minutes){data.timerRunning=false;data.timerRemaining=minutes*60*1000L;data.timerNotified=false;changed();save();}
    public boolean save(){if(!dirty)return true;try{sanitize();Files.createDirectories(file.toAbsolutePath().getParent());if(broken&&Files.exists(file)){Files.copy(file,file.resolveSibling(file.getFileName()+".backup-"+System.currentTimeMillis()));broken=false;}Path tmp=file.resolveSibling(file.getFileName()+".tmp");Files.writeString(tmp,JSON.toJson(data));try{Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException error){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}dirty=false;saveTicks=0;return true;}catch(Exception error){net.mcreator.commandcookieselectronicmod.CommandcookiesElectronicModMod.LOGGER.warn("Could not save laptop programs",error);return false;}}
}
