package net.mcreator.commandcookieselectronicmod.client;

import java.util.*;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/** Local BlockNet navigation; never fetches real web pages or runs external code. */
public final class LaptopBrowser {
    public static final class Tab {
        public String url="blocknet://home",draft=url;
        public int scroll,cursor;
        public final List<String> history=new ArrayList<>(List.of(url));
        public void navigate(String input){url=normalize(input);draft=url;scroll=0;while(history.size()>cursor+1)history.removeLast();if(!history.getLast().equals(url)){history.add(url);cursor++;}if(history.size()>60){history.removeFirst();cursor--;} }
        public void move(int direction){cursor=Math.max(0,Math.min(history.size()-1,cursor+direction));url=history.get(cursor);draft=url;scroll=0;}
    }
    public final List<Tab> tabs=new ArrayList<>(List.of(new Tab()));
    public int selected;
    public Tab tab(){selected=Math.max(0,Math.min(tabs.size()-1,selected));return tabs.get(selected);}
    public boolean add(){if(tabs.size()>=5)return false;tabs.add(new Tab());selected=tabs.size()-1;return true;}
    public void close(){if(tabs.size()==1){tabs.set(0,new Tab());selected=0;}else{tabs.remove(selected);selected=Math.min(selected,tabs.size()-1);}}
    public static boolean valid(String s){return s!=null&&s.startsWith("blocknet://")&&s.length()<=256;}
    public static String normalize(String input){
        input=input.strip();if(input.length()>160)input=input.substring(0,160);if(input.isEmpty())return "blocknet://home";
        if(input.toLowerCase(Locale.ROOT).startsWith("http://")||input.toLowerCase(Locale.ROOT).startsWith("https://")||input.toLowerCase(Locale.ROOT).startsWith("www."))return "blocknet://external?url="+URLEncoder.encode(input,StandardCharsets.UTF_8);
        String candidate=input.toLowerCase(Locale.ROOT).replaceFirst("^blocknet://","");
        if(candidate.matches("(?:home|apps|games|tools|library|guides|news|wallpapers|scores|bookmarks|history|portal)(?:/.*)?")||candidate.startsWith("search?")||candidate.startsWith("external?"))return "blocknet://"+candidate;
        return "blocknet://search?q="+URLEncoder.encode(input,StandardCharsets.UTF_8);
    }
    public static String query(String url){int i=url.indexOf('=');if(i<0)return "";try{return URLDecoder.decode(url.substring(i+1),StandardCharsets.UTF_8);}catch(Exception error){return "";}}
    public static String title(String url){String route=url.substring(11).split("[/?]",2)[0];return switch(route){case "home"->"BlockNet";case "apps"->"App Store";case "games"->"Arcade";case "tools"->"Toolbox";case "library"->"My Apps";case "guides"->"Help";case "news"->"BlockOS News";case "wallpapers"->"Wallpapers";case "scores"->"High Scores";case "bookmarks"->"Bookmarks";case "history"->"History";case "portal"->"Discover";case "external"->"Local browser";default->"Search";};}
}
