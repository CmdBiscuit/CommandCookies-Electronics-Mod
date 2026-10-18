package net.mcreator.commandcookiesretroexpansion.retro.client;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import com.google.gson.JsonParser;
import net.mcreator.commandcookiesretroexpansion.retro.*;
import net.neoforged.fml.loading.FMLPaths;

/** Local libraries only. Network packets contain game hashes, never paths or ROM bytes. */
public final class RomLibrary {
 public record Rom(Path file,String hash,String title,RetroSystem system){}
 public static Path root(){return FMLPaths.CONFIGDIR.get().resolve("commandcookies-retro");}
 public static Path folder(RetroSystem s){return root().resolve("roms").resolve(s.id);}
 public static void ensureFolders()throws IOException{for(var system:RetroSystem.values())Files.createDirectories(folder(system));Files.createDirectories(root().resolve("cores"));}
 public static Path core(){return core(RetroSystem.NES);}
 public static Path core(RetroSystem s){String os=System.getProperty("os.name").toLowerCase(Locale.ROOT);return root().resolve("cores").resolve(s.core+"_libretro."+(os.startsWith("windows")?"dll":os.contains("mac")||os.contains("darwin")?"dylib":"so"));}
 public static String corePlatform(String os,String arch)throws IOException{
  os=os.toLowerCase(Locale.ROOT);arch=arch.toLowerCase(Locale.ROOT);
  String platform=os.startsWith("windows")?"windows":os.contains("mac")||os.contains("darwin")?"macos":os.contains("linux")?"linux":"";
  String cpu=switch(arch){case "amd64","x86_64"->"x86_64";case "aarch64","arm64"->"aarch64";default->"";};
  if(platform.isEmpty()||cpu.isEmpty())throw new IOException("No bundled emulator for "+os+" / "+arch+". Use a supported 64-bit Java runtime.");return platform+"-"+cpu;
 }
 public static Path readyCore()throws Exception{return readyCore(RetroSystem.NES);}
 public static synchronized Path readyCore(RetroSystem system)throws Exception{
  Path custom=core(system);if(Files.isRegularFile(custom))return custom.toAbsolutePath();
  String platform=corePlatform(System.getProperty("os.name"),System.getProperty("os.arch"));com.google.gson.JsonObject manifest;
  try(var input=RomLibrary.class.getResourceAsStream("/retro_bridge/"+system.core+"-manifest.json")){
   if(input==null)throw new IOException("Bundled "+system.label+" emulator manifest missing.");manifest=JsonParser.parseReader(new InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
  }
  var entry=manifest.getAsJsonObject("cores").getAsJsonObject(platform);if(entry==null)throw new IOException("No bundled "+system.label+" emulator for "+platform+". Install a matching core in the core folder.");
  String resource=entry.get("resource").getAsString(),hash=entry.get("sha256").getAsString();
  if(!hash.matches("[0-9a-f]{64}")||!resource.startsWith("retro_bridge/cores/"+platform+"/")||resource.contains(".."))throw new IOException("Invalid bundled emulator manifest.");
  byte[] bytes;try(var input=RomLibrary.class.getResourceAsStream("/"+resource)){if(input==null)throw new IOException("Bundled emulator missing.");bytes=input.readNBytes(64*1024*1024+1);}
  if(bytes.length>64*1024*1024||!digest(bytes).equals(hash))throw new IOException("Bundled emulator is damaged. Reinstall the addon.");
  Path dir=root().resolve("cores").resolve(platform).resolve(hash);Files.createDirectories(dir);Path file=dir.resolve(resource.substring(resource.lastIndexOf('/')+1));
  if(!Files.isRegularFile(file)||Files.size(file)!=bytes.length||!digest(Files.readAllBytes(file)).equals(hash)){
   Path temp=Files.createTempFile(dir,system.core+"-",".tmp");try{Files.write(temp,bytes);try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException ignored){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}
  }
  return file.toAbsolutePath();
 }
 public static String digest(byte[] b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
 public static synchronized Path readyFrontend()throws Exception{
  String platform=corePlatform(System.getProperty("os.name"),System.getProperty("os.arch"));com.google.gson.JsonObject manifest;
  try(var in=RomLibrary.class.getResourceAsStream("/retro_bridge/frontend-manifest.json")){if(in==null)throw new IOException("Bundled emulator frontend missing.");manifest=JsonParser.parseReader(new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
  var entry=manifest.getAsJsonObject("platforms").getAsJsonObject(platform);if(entry==null)throw new IOException("No bundled emulator frontend for "+platform);
  String resource=entry.get("resource").getAsString(),hash=entry.get("sha256").getAsString();if(!hash.matches("[0-9a-f]{64}")||!resource.startsWith("retro_bridge/frontends/"+platform+"/")||resource.contains(".."))throw new IOException("Invalid emulator frontend manifest.");
  byte[] bytes;try(var in=RomLibrary.class.getResourceAsStream("/"+resource)){if(in==null)throw new IOException("Bundled emulator frontend missing.");bytes=in.readNBytes(16*1024*1024+1);}if(bytes.length>16*1024*1024||!digest(bytes).equals(hash))throw new IOException("Bundled emulator frontend is damaged.");
  Path dir=root().resolve("runtime").resolve(hash);Files.createDirectories(dir);Path file=dir.resolve(platform.startsWith("windows")?"native-host.exe":"native-host");
  if(!Files.isRegularFile(file)||Files.size(file)!=bytes.length||!digest(Files.readAllBytes(file)).equals(hash)){Path temp=Files.createTempFile(dir,"frontend-",".tmp");try{Files.write(temp,bytes);try{Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException ignored){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}}
  if(!platform.startsWith("windows")){var permissions=Files.getPosixFilePermissions(file);permissions.add(java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE);Files.setPosixFilePermissions(file,permissions);}return file.toAbsolutePath();
 }
 public static byte[] readRom(Path p)throws IOException{return readRom(p,RetroSystem.NES);}
 public static byte[] readRom(Path p,RetroSystem system)throws IOException{
  long n=Files.size(p);int minimum=system==RetroSystem.NES?16:system==RetroSystem.SNES?32768:4096;
  if(!system.accepts(p.getFileName().toString())||n<minimum||n>system.maxBytes)throw new IOException("Choose a "+system.label+" game ("+system.formats()+"), at most "+system.maxBytes/1024/1024+" MB.");
  byte[] b;try(var in=Files.newInputStream(p)){b=in.readNBytes(system.maxBytes+1);}if(b.length!=n)throw new IOException("The ROM file changed while reading it.");
  if(system==RetroSystem.NES&&(b[0]!='N'||b[1]!='E'||b[2]!='S'||b[3]!=26))throw new IOException("Choose an iNES .nes game.");
  if(system==RetroSystem.SNES&&b.length%1024!=0&&b.length%1024!=512)throw new IOException("Invalid SNES ROM size; choose an uncompressed .sfc or .smc file.");
  if(system==RetroSystem.N64){int signature=((b[0]&255)<<24)|((b[1]&255)<<16)|((b[2]&255)<<8)|(b[3]&255);if(signature!=0x80371240&&signature!=0x37804012&&signature!=0x40123780)throw new IOException("Invalid N64 cartridge header; choose a .z64, .n64 or .v64 file.");}
  return b;
 }
 public static List<Rom> list()throws Exception{return list(RetroSystem.NES);}
 public static List<Rom> list(RetroSystem system)throws Exception{
  ensureFolders();var result=new LinkedHashMap<String,Rom>();
  // Keep existing NES cartridges valid: the original flat ROM folder is
  // still indexed, while new imports and Open ROM folder use roms/nes.
  var folders=system==RetroSystem.NES?List.of(folder(system),root().resolve("roms")):List.of(folder(system));
  for(Path folder:folders)try(var paths=Files.list(folder)){
   for(Path p:paths.filter(f->Files.isRegularFile(f)&&system.accepts(f.getFileName().toString())).sorted().limit(256).toList())try{
    String name=p.getFileName().toString().replaceFirst("\\.[^.]+$","").replaceAll("[\\p{Cntrl}\u00a7]","");String hash=digest(readRom(p,system));
    if(RetroItems.isGameHash(hash))result.putIfAbsent(hash,new Rom(p.toAbsolutePath(),hash,name.substring(0,Math.min(48,name.length())),system));
    if(result.size()>=256)break;
   }catch(IOException invalid){}
   if(result.size()>=256)break;
  }
  return List.copyOf(result.values());
 }
 public static Rom find(String hash)throws Exception{return find(hash,RetroSystem.NES);}
 public static Rom find(String hash,RetroSystem system)throws Exception{if(!RetroItems.isGameHash(hash))throw new IOException("No game selected. Choose a local "+system.label+" game using its ROM cartridge.");return list(system).stream().filter(r->r.hash.equals(hash)).findFirst().orElseThrow(()->new IOException("This "+system.label+" game is not in your local library. Import it using the matching ROM cartridge."));}
 public static void importFile(String path)throws Exception{importFile(path,RetroSystem.NES);}
 public static void importFile(String path,RetroSystem system)throws Exception{
  Path source=Path.of(path.trim().replaceAll("^\"|\"$","")).toAbsolutePath();byte[] bytes=readRom(source,system);if(!RetroItems.isGameHash(digest(bytes)))throw new IOException("The removed demo is not available. Choose another game.");
  list(system);Path dest=folder(system).resolve(source.getFileName().toString());if(Files.exists(dest)&&!digest(readRom(dest,system)).equals(digest(bytes)))throw new IOException("A different game already has this name. Rename the file first.");if(!Files.exists(dest))Files.write(dest,bytes,StandardOpenOption.CREATE_NEW);
 }
 public static Path runtime(String name)throws Exception{
  if(!name.equals("helper.jar")&&!name.equals("jna.jar"))throw new IOException("Unknown runtime resource.");byte[] b;try(var in=RomLibrary.class.getResourceAsStream("/retro_bridge/"+name)){if(in==null)throw new IOException("Emulator helper missing.");b=in.readAllBytes();}
  Path dir=root().resolve("runtime").resolve(digest(b));Files.createDirectories(dir);Path p=dir.resolve(name);if(!Files.isRegularFile(p)||!digest(Files.readAllBytes(p)).equals(digest(b)))Files.write(p,b);return p.toAbsolutePath();
 }
}
