package net.mcreator.commandcookieselectronicmod.client;

import com.google.gson.*;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

/** Browser pixels and input are private to this player's physical computer. */
public final class ChromiumSession implements AutoCloseable {
 public record Tab(String title){}
 public record Dialog(String kind,String message,String initial){}
 private record Frame(int width,int height,byte[] pixels){}
 private static final Set<ChromiumSession> ACTIVE=ConcurrentHashMap.newKeySet();
 private static final String PREFIX="/assets/commandcookies_electronic_mod/browser/";
 private static final int MAX_W=2560,MAX_H=1440;
 private static final Gson JSON=new Gson();
 private static final ScheduledExecutorService STARTUP=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"CEM browser startup guard");t.setDaemon(true);return t;});
 private final UUID player,device;private final boolean media;private volatile int frameRate;
 private final AtomicReference<Frame> latest=new AtomicReference<>();
 private final BlockingQueue<String> commands=new ArrayBlockingQueue<>(128);
 private final Object inputLock=new Object();
 private volatile boolean closed,failed,ready,loading,back,forward,background,mediaPlaying;
 private volatile String status="Preparing browser...",url="",title="Chromium";
 private volatile double progress=-1;
 private volatile int browserWidth,browserHeight;
 private volatile Dialog dialog;
 private volatile List<Tab> tabs=List.of();private volatile int activeTab;
 private volatile Process process;
 private volatile ServerSocket listener;
 private volatile Socket socket;
 private volatile OutputStream download;
 private volatile InputStream downloadInput;
 private volatile ScheduledFuture<?> startupTimeout;
 private volatile CompletableFuture<Boolean> downloadConsent;
 private volatile long pendingDownloadBytes;
 private FileChannel profileLockChannel;private FileLock profileLock;
 private DynamicTexture texture;private ResourceLocation textureId;private int textureWidth,textureHeight;
 static {Runtime.getRuntime().addShutdownHook(new Thread(()->{for(var session:List.copyOf(ACTIVE))session.stopProcess();},"CEM browser shutdown"));}
 private ChromiumSession(UUID player,UUID device,int width,int height,boolean media,int frameRate){this.player=player;this.device=device;this.media=media;this.frameRate=frameRate==60?60:25;browserWidth=bound(width,1,MAX_W);browserHeight=bound(height,1,MAX_H);ACTIVE.add(this);var worker=new Thread(this::start,"CEM browser setup");worker.setDaemon(true);worker.start();}
 public static ChromiumSession open(UUID player,UUID device,int width,int height){return open(player,device,width,height,preferredMedia(player,device));}
 public static ChromiumSession open(UUID player,UUID device,int width,int height,boolean media){return open(player,device,width,height,media,LaptopPreferences.load(LaptopProfile.directory(player,device).resolve("settings.json")).chromiumFps);}
 public static ChromiumSession open(UUID player,UUID device,int width,int height,boolean media,int frameRate){return new ChromiumSession(player,device,width,height,media,frameRate);}
 boolean belongsTo(UUID player,UUID device){return this.player.equals(player)&&this.device.equals(device);}
 public boolean mediaPlaying(){return mediaPlaying;}public boolean background(){return background;}
 public void background(boolean value){if(!media||closed)return;background=value;releaseTexture();latest.set(null);if(dialog!=null)answerDialog(false,"");send("visibility","visible",!value);}
 public void mediaFrameRate(int fps){if(!media||closed)return;frameRate=fps==60?60:25;send("frameRate","fps",frameRate);}
 public boolean media(){return media;}public int frameRate(){return frameRate;}public boolean waitingForDownloadConsent(){return downloadConsent!=null&&pendingDownloadBytes!=0;}
 private static Path profile(UUID player,UUID device){return FMLPaths.GAMEDIR.get().resolve("commandcookies_browser/profiles").resolve(player.toString()).resolve(device.toString()).toAbsolutePath().normalize();}
 public static boolean preferredMedia(UUID player,UUID device){try{var file=profile(player,device).resolve("browser-mode.txt");return Files.isRegularFile(file)&&Files.size(file)<32&&Files.readString(file).equals("media");}catch(IOException ignored){return false;}}
 private static void savePreference(Path profile,boolean media)throws IOException{var file=profile.resolve("browser-mode.txt");var temp=profile.resolve("browser-mode-"+UUID.randomUUID()+".tmp");try{Files.writeString(temp,media?"media":"standard");Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}finally{Files.deleteIfExists(temp);}}
 private static int bound(int value,int min,int max){return Math.max(min,Math.min(max,value));}
 public boolean ready(){return ready&&!closed;}public boolean failed(){return failed;}public String status(){return status;}public double progress(){return progress;}public String url(){return url;}public String title(){return title;}public boolean canBack(){return back;}public boolean canForward(){return forward;}public boolean loading(){return loading;}public Dialog dialog(){return dialog;}
 public List<Tab> tabs(){return tabs;}public int activeTab(){return activeTab;}
 public void newTab(){send("newTab");}public void switchTab(int index){releaseTexture();latest.set(null);dialog=null;send("switchTab","index",index);}public void closeTab(int index){send("closeTab","index",index);}
 public long pendingDownloadBytes(){return pendingDownloadBytes;}
 public void confirmDownload(){var choice=downloadConsent;if(choice!=null&&!closed)choice.complete(true);}
 private void start(){try{
  if(!System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("windows")||!Set.of("amd64","x86_64").contains(System.getProperty("os.arch","").toLowerCase(Locale.ROOT)))throw new IOException("Chromium currently supports Windows 64-bit.");
  var root=FMLPaths.GAMEDIR.get().resolve("commandcookies_browser").toAbsolutePath().normalize();Files.createDirectories(root);
  JsonObject manifest;try(var in=resource("runtime.json")){manifest=JsonParser.parseReader(new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
  checkOpen();var profile=profile(player,device);Files.createDirectories(profile);
  profileLockChannel=FileChannel.open(profile.resolve("browser.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);for(int attempt=0;attempt<160&&profileLock==null;attempt++){checkOpen();try{profileLock=profileLockChannel.tryLock();}catch(OverlappingFileLockException ignored){profileLock=null;}if(profileLock==null)Thread.sleep(100);}if(profileLock==null)throw new IOException("This computer's browser is open in another game instance.");
  Path engine,mediaHelper=null,mediaData=null;var jars=new ArrayList<Path>();
  if(media){
   var asset=manifest.getAsJsonObject("media");String version=asset.get("version").getAsString();if(!version.matches("[a-zA-Z0-9.+-]+"))throw new IOException("Invalid media browser version.");
   var runtime=root.resolve("media-"+version);Files.createDirectories(runtime);engine=runtime.resolve("engine");String expected=asset.get("sha256").getAsString();
   var consent=profile.resolve("media-consent.txt");boolean approved=Files.isRegularFile(consent)&&Files.size(consent)<128&&Files.readString(consent).equals("widevine-component-updates-v1");
   synchronized(ChromiumSession.class){
    checkOpen();boolean installed=Files.isRegularFile(engine.resolve("cem-install.json"))&&Files.readString(engine.resolve("cem-install.json")).equals(expected)&&Files.isRegularFile(engine.resolve("electron.exe"));
    if(!installed){var archive=runtime.resolve("engine.zip");if(!Files.isRegularFile(archive)||!hash(archive).equals(expected)){awaitDownloadConsent(asset.get("bytes").getAsLong());approved=true;download(asset.get("url").getAsString(),archive,expected,asset.get("bytes").getAsLong());}checkOpen();status="Installing media browser...";progress=-1;BrowserMediaInstaller.install(archive,engine,expected,this::checkOpen);}
    for(String name:List.of("app.asar","electron.exe.sig")){var part=asset.getAsJsonObject(name.equals("app.asar")?"helper":"signature");var target=engine.resolve(name.equals("app.asar")?"resources/app.asar":name);String checksum=part.get("sha256").getAsString();if(!Files.isRegularFile(target)||!hash(target).equals(checksum))try(var in=resource("media/"+part.get("name").getAsString())){atomicCopy(in,target,checksum);}}
   }
   if(!approved)awaitDownloadConsent(-1);checkOpen();Files.writeString(consent,"widevine-component-updates-v1");mediaHelper=engine.resolve("resources/app.asar");
   String identity=Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest((player+"/"+device).getBytes(java.nio.charset.StandardCharsets.UTF_8)));mediaData=root.resolve("media-profiles").resolve(identity);Files.createDirectories(mediaData);
  }else{
  String version=manifest.get("version").getAsString();if(!version.matches("[0-9.]+"))throw new IOException("Invalid browser version");
  var runtime=root.resolve("runtime-"+version);Files.createDirectories(runtime);
  for(var value:manifest.getAsJsonArray("libraries")){var asset=value.getAsJsonObject();String name=asset.get("name").getAsString();if(!name.matches("[a-zA-Z0-9_-]+\\.jar"))throw new IOException("Invalid browser library");var path=runtime.resolve(name);String expected=asset.get("sha256").getAsString();if(!Files.isRegularFile(path)||!hash(path).equals(expected)){try(var in=resource("libs/"+name)){atomicCopy(in,path,expected);}}jars.add(path);}
  var nativeAsset=manifest.getAsJsonObject("native");engine=runtime.resolve("engine");String expected=nativeAsset.get("sha256").getAsString();
  synchronized(ChromiumSession.class){
   checkOpen();boolean installed=Files.isRegularFile(engine.resolve("cem-install.json"))&&Files.readString(engine.resolve("cem-install.json")).equals(expected)&&Files.isRegularFile(engine.resolve("jcef.dll"))&&Files.isRegularFile(engine.resolve("libcef.dll"));
   if(!installed){var archive=runtime.resolve("engine.jar");if(!Files.isRegularFile(archive)||!hash(archive).equals(expected)){awaitDownloadConsent(nativeAsset.get("bytes").getAsLong());download(nativeAsset.get("url").getAsString(),archive,expected,nativeAsset.get("bytes").getAsLong());}checkOpen();status="Installing browser...";progress=-1;BrowserNativeInstaller.install(archive,engine,expected,this::checkOpen);}
  }
  }
  savePreference(profile,media);
  status="Opening browser...";progress=-1;
  listener=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"));listener.setSoTimeout(1000);
  byte[] random=new byte[32];new SecureRandom().nextBytes(random);String token=HexFormat.of().formatHex(random);
  ProcessBuilder builder;
  if(media)builder=new ProcessBuilder(engine.resolve("electron.exe").toString(),mediaHelper.toString(),profile.toString(),mediaData.toString(),Integer.toString(listener.getLocalPort()),token,Integer.toString(browserWidth),Integer.toString(browserHeight));
  else{String classpath=String.join(File.pathSeparator,jars.stream().map(Path::toString).toList());var executable=Path.of(System.getProperty("java.home"),"bin","javaw.exe");if(!Files.isRegularFile(executable))executable=executable.resolveSibling("java.exe");builder=new ProcessBuilder(executable.toString(),"-cp",classpath,"cem.browser.BrowserHelper",engine.toString(),profile.toString(),Integer.toString(listener.getLocalPort()),token,Integer.toString(browserWidth),Integer.toString(browserHeight));}builder.environment().put("CEM_BROWSER_FPS",Integer.toString(frameRate));builder.directory(profile.toFile());builder.redirectErrorStream(true);builder.redirectOutput(ProcessBuilder.Redirect.appendTo(profile.resolve("browser.log").toFile()));process=builder.start();startupTimeout=STARTUP.schedule(()->{if(!closed&&!ready){failed=true;status="Browser startup timed out. Choose Try again.";stopProcess();}},media?120:30,TimeUnit.SECONDS);
  long deadline=System.nanoTime()+30_000_000_000L;
  while(!closed&&System.nanoTime()<deadline){Socket candidate=null;try{candidate=listener.accept();candidate.setSoTimeout(2000);var input=new DataInputStream(new BufferedInputStream(candidate.getInputStream()));if(!MessageDigest.isEqual(input.readUTF().getBytes(java.nio.charset.StandardCharsets.UTF_8),token.getBytes(java.nio.charset.StandardCharsets.UTF_8))||input.readInt()!=0x43454D42){candidate.close();continue;}socket=candidate;socket.setSoTimeout(0);socket.setTcpNoDelay(true);listener.close();listener=null;var output=new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));var writer=new Thread(()->writeCommands(output),"CEM browser input");writer.setDaemon(true);writer.start();readEvents(input);return;}catch(SocketTimeoutException ignored){if(candidate!=null)candidate.close();if(process!=null&&!process.isAlive())throw new IOException("Browser could not start. Try reopening the app.");}}
  if(!closed)throw new IOException("Browser startup timed out. Try reopening the app.");
 }catch(Exception failure){if(!closed){if(!failed){failed=true;status=failure instanceof IOException&&failure.getMessage()!=null?failure.getMessage():"Browser could not start. Try reopening the app.";}ready=false;net.mcreator.commandcookieselectronicmod.CommandcookiesElectronicModMod.LOGGER.warn("Chromium app unavailable",failure);}}finally{var timeout=startupTimeout;if(timeout!=null)timeout.cancel(false);stopProcess();}}
 private static InputStream resource(String name)throws IOException{var stream=ChromiumSession.class.getResourceAsStream(PREFIX+name);if(stream==null)throw new IOException("Browser files are missing from this mod build.");return stream;}
 private static String hash(Path file)throws IOException{try{var digest=MessageDigest.getInstance("SHA-256");try(var in=Files.newInputStream(file)){byte[] buffer=new byte[65536];int size;while((size=in.read(buffer))!=-1)digest.update(buffer,0,size);}return HexFormat.of().formatHex(digest.digest());}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private static void atomicCopy(InputStream in,Path path,String expected)throws IOException{var temporary=path.resolveSibling(path.getFileName()+".part-"+UUID.randomUUID());try{Files.copy(in,temporary);if(!hash(temporary).equals(expected))throw new IOException("Browser file verification failed.");Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}finally{Files.deleteIfExists(temporary);}}
 private void awaitDownloadConsent(long bytes)throws IOException {
  checkOpen();var choice=new CompletableFuture<Boolean>();downloadConsent=choice;
  pendingDownloadBytes=bytes;status="Waiting for download confirmation...";progress=-1;
  if(closed)choice.complete(false);
  try {if(!choice.get())throw new IOException("Browser download canceled.");checkOpen();}
  catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Browser setup canceled.",e);}
  catch(ExecutionException e){throw new IOException("Browser download could not start.",e);}
  finally {pendingDownloadBytes=0;downloadConsent=null;}
 }
 private void download(String address,Path path,String expected,long size)throws IOException{
  status="Downloading browser engine...";progress=0;var temporary=path.resolveSibling(path.getFileName()+".download-"+UUID.randomUUID());
  try{URI uri=URI.create(address);URLConnection connection=null;for(int redirects=0;redirects<5;redirects++){checkOpen();if(!"https".equalsIgnoreCase(uri.getScheme()))throw new IOException("Browser download requires HTTPS.");var http=(HttpURLConnection)uri.toURL().openConnection();http.setInstanceFollowRedirects(false);http.setConnectTimeout(15000);http.setReadTimeout(30000);http.setRequestProperty("User-Agent","CEM-Chromium/1.0");int code=http.getResponseCode();if(code>=300&&code<400){String location=http.getHeaderField("Location");http.disconnect();if(location==null)throw new IOException("Browser download could not start.");uri=uri.resolve(location);continue;}if(code!=200)throw new IOException("Browser download unavailable. Check your connection and reopen the app.");connection=http;break;}
   if(connection==null)throw new IOException("Browser download redirected too often.");
   try(var in=connection.getInputStream();var out=Files.newOutputStream(temporary)){downloadInput=in;download=out;byte[] bytes=new byte[65536];long total=0;int n;while((n=in.read(bytes))!=-1){checkOpen();total+=n;if(total>size)throw new IOException("Browser download has an unexpected size.");out.write(bytes,0,n);progress=(double)total/size;}if(total!=size)throw new IOException("Browser download was incomplete.");}finally{downloadInput=null;download=null;}
   checkOpen();status="Verifying browser...";progress=-1;if(!hash(temporary).equals(expected))throw new IOException("Browser verification failed. Reopen the app to retry.");Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
  }finally{Files.deleteIfExists(temporary);}
 }
 private void checkOpen()throws IOException{if(closed||Thread.currentThread().isInterrupted())throw new IOException("Browser setup canceled.");}
 private void writeCommands(DataOutputStream output){try{while(!closed){String command=commands.poll(1,TimeUnit.SECONDS);if(command!=null)synchronized(inputLock){output.writeUTF(command);output.flush();}}}catch(Exception e){if(!closed)status="Browser connection closed. Reopen the app to reconnect.";}}
 private void readEvents(DataInputStream input)throws IOException{while(!closed){int kind=input.readUnsignedByte();if(kind==1){String message=input.readUTF();if(message.length()>12000)throw new IOException("Invalid browser message.");var event=JsonParser.parseString(message).getAsJsonObject();String type=event.get("type").getAsString();switch(type){case "ready"->{ready=true;var timeout=startupTimeout;if(timeout!=null)timeout.cancel(false);status="";}case "state"->{url=event.get("url").getAsString();title=event.get("title").getAsString();loading=event.get("loading").getAsBoolean();back=event.get("back").getAsBoolean();forward=event.get("forward").getAsBoolean();if(event.has("tabs")){var list=new ArrayList<Tab>();for(var entry:event.getAsJsonArray("tabs")){if(list.size()==8)break;String label=entry.getAsJsonObject().get("title").getAsString();list.add(new Tab(label.substring(0,Math.min(200,label.length()))));}tabs=List.copyOf(list);activeTab=Math.max(0,Math.min(tabs.size()-1,event.get("active").getAsInt()));}if(loading)status="";}case "media"->mediaPlaying=media&&event.has("playing")&&event.get("playing").getAsBoolean();case "error","notice"->status=event.get("message").getAsString();case "dialog"->{if(background)send("dialog","accept",false,"text","");else dialog=new Dialog(event.get("kind").getAsString().replace("JSDIALOGTYPE_",""),event.get("message").getAsString(),event.get("initial").getAsString());}default->{}}}else if(kind==2){int width=input.readInt(),height=input.readInt(),length=input.readInt();if(width<1||height<1||width>MAX_W||height>MAX_H||length!=width*height*4)throw new IOException("Invalid browser frame.");if(background){input.skipNBytes(length);latest.set(null);}else{byte[] pixels=new byte[length];input.readFully(pixels);if(!closed&&!background)latest.set(new Frame(width,height,pixels));}}else throw new IOException("Invalid browser connection.");}}
 private void send(String action,Object... values){if(closed)return;var message=new JsonObject();message.addProperty("action",action);for(int i=0;i<values.length;i+=2){Object value=values[i+1];String key=(String)values[i];if(value instanceof Boolean b)message.addProperty(key,b);else if(value instanceof Number n)message.addProperty(key,n);else message.addProperty(key,value.toString());}String text=JSON.toJson(message);if(text.length()>8192)return;boolean queued=commands.offer(text);if(!queued&&Set.of("visibility","frameRate","dialog").contains(action)){commands.clear();commands.offer(text);}else if(!queued&&action.equals("mouse")){commands.removeIf(command->command.contains("\"action\":\"mouse\"")&&command.contains("\"kind\":0"));commands.offer(text);}}
 public static String address(String input){String value=input.strip();if(value.isEmpty())return "https://www.google.com/";try{var uri=new URI(value);if(uri.getScheme()!=null){if(!Set.of("http","https").contains(uri.getScheme().toLowerCase(Locale.ROOT))||uri.getHost()==null||uri.getUserInfo()!=null)throw new IllegalArgumentException("Use an HTTP or HTTPS address.");return uri.toASCIIString();}if(!value.contains(" ")&&(value.contains(".")||value.startsWith("localhost"))){uri=new URI("https://"+value);if(uri.getHost()!=null&&uri.getUserInfo()==null)return uri.toASCIIString();}}catch(URISyntaxException ignored){}return "https://www.google.com/search?q="+URLEncoder.encode(value,java.nio.charset.StandardCharsets.UTF_8);}
 public void navigate(String input){try{status="";send("navigate","url",address(input));}catch(IllegalArgumentException failure){status=failure.getMessage();}}public void back(){send("back");}public void forward(){send("forward");}public void reload(){status="";send("reload");}public void focus(boolean value){send("focus","focus",value);}public void resize(int width,int height){browserWidth=bound(width,1,MAX_W);browserHeight=bound(height,1,MAX_H);send("resize","width",browserWidth,"height",browserHeight);}public void mouse(int kind,int x,int y,int button,int mods){send("mouse","kind",kind,"x",x,"y",y,"button",button,"mods",mods);}public void wheel(int x,int y,double amount,int mods){send("wheel","x",x,"y",y,"amount",amount,"mods",mods);}public void key(int key,boolean release,int mods){int scan=org.lwjgl.glfw.GLFW.glfwGetKeyScancode(key);send("key","key",key,"scan",scan,"release",release,"mods",mods);}public void character(int point,int mods){send("char","point",point,"mods",mods);}public void answerDialog(boolean accept,String text){dialog=null;send("dialog","accept",accept,"text",text);}
 public boolean draw(GuiGraphics graphics,int x,int y,int width,int height){if(closed||background)return false;var frame=latest.getAndSet(null);if(frame!=null){if(texture==null||textureWidth!=frame.width||textureHeight!=frame.height){releaseTexture();texture=new DynamicTexture(new NativeImage(frame.width,frame.height,false));textureWidth=frame.width;textureHeight=frame.height;textureId=Minecraft.getInstance().getTextureManager().register("cem_chromium_"+UUID.randomUUID(),texture);}var image=texture.getPixels();for(int yy=0;yy<frame.height;yy++)for(int xx=0;xx<frame.width;xx++){int offset=(yy*frame.width+xx)*4;byte[] bytes=frame.pixels;image.setPixelRGBA(xx,yy,(bytes[offset+3]&255)<<24|(bytes[offset]&255)<<16|(bytes[offset+1]&255)<<8|(bytes[offset+2]&255));}texture.upload();}if(textureId==null)return false;graphics.blit(textureId,x,y,width,height,0,0,textureWidth,textureHeight,textureWidth,textureHeight);return true;}
 private void releaseTexture(){if(textureId!=null)Minecraft.getInstance().getTextureManager().release(textureId);texture=null;textureId=null;}
 @Override public void close(){if(closed)return;closed=true;var choice=downloadConsent;if(choice!=null)choice.complete(false);ready=false;mediaPlaying=false;releaseTexture();latest.set(null);var cleanup=new Thread(this::stopProcess,"CEM browser cleanup");cleanup.setDaemon(true);cleanup.start();}
 private synchronized void stopProcess(){
  ready=false;var child=process;
  if(child!=null&&child.isAlive()){
   var descendants=child.descendants().toList();
   try{
    if(socket!=null&&!socket.isClosed())synchronized(inputLock){var output=new DataOutputStream(socket.getOutputStream());output.writeUTF("{\"action\":\"close\"}");output.flush();}
    if(!child.waitFor(12,TimeUnit.SECONDS)){descendants.forEach(ProcessHandle::destroy);child.destroy();if(!child.waitFor(1,TimeUnit.SECONDS))child.destroyForcibly();}
   }catch(Exception ignored){descendants.forEach(ProcessHandle::destroy);child.destroyForcibly();}
   descendants.stream().filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroyForcibly);
  }
  for(var item:new AutoCloseable[]{listener,socket,downloadInput,download,profileLock,profileLockChannel})if(item!=null)try{item.close();}catch(Exception ignored){}
  ACTIVE.remove(this);
 }
}
