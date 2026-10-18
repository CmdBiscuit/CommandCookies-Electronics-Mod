package cem.browser;

import com.google.gson.*;
import java.awt.EventQueue;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import me.friwi.jcefmaven.*;
import org.cef.*;
import org.cef.browser.*;
import org.cef.callback.*;
import org.cef.handler.*;
import org.cef.misc.BoolRef;
import org.cef.network.CefRequest;
import org.cef.network.CefCookieManager;

/** Runs Chromium outside Minecraft. Its private channel carries only bounded input and pixels. */
public final class BrowserHelper {
 public static final int MAGIC=0x43454D42,MAX_WIDTH=2560,MAX_HEIGHT=1440,MAX_BYTES=MAX_WIDTH*MAX_HEIGHT*4;
 private static final Gson JSON=new Gson();
 private final int frameRate="60".equals(System.getenv("CEM_BROWSER_FPS"))?60:25;
 private final AtomicReference<Frame> latest=new AtomicReference<>();
 private final BlockingQueue<String> events=new ArrayBlockingQueue<>(64);
 private final CountDownLatch terminated=new CountDownLatch(1);
 private volatile CountDownLatch closingBrowsers=new CountDownLatch(0);
 private volatile boolean shuttingDown;
 private static final int MAX_TABS=8;
 private static final String HOME="https://www.google.com/";
 private final List<Tab> tabs=new CopyOnWriteArrayList<>();
 private volatile Tab selected;private Path sessionFile;private int renderWidth,renderHeight;private boolean readySent;
 private static final class Tab {volatile String url,title;volatile boolean loading,back,forward;volatile CemOffscreenBrowser browser;volatile TabPainter painter;Tab(String url,String title){this.url=url;this.title=title;}}
 private volatile boolean running=true,loading,back,forward,focused;
 private volatile String url="",title="Chromium";
 private volatile CefJSDialogCallback dialog;
 private final double[] wheelRemainder=new double[2];
 private int pressed,lastButton,lastX,lastY,clicks;private long lastPress;
 private CefApp app;private CefClient client;private CemOffscreenBrowser browser;
 private record Frame(int width,int height,byte[] bytes){}
 private void event(Map<String,Object> values){String message=JSON.toJson(values);if(message.length()>12000)return;if(!events.offer(message)){events.poll();events.offer(message);}}
 private Tab tab(CefBrowser value){for(var tab:tabs)if(tab.browser==value)return tab;return null;}
 private void state(){var tab=selected;if(tab==null)return;url=tab.url;title=tab.title;loading=tab.loading;back=tab.back;forward=tab.forward;event(Map.of("type","state","url",url,"title",title,"loading",loading,"back",back,"forward",forward,"active",Math.max(0,tabs.indexOf(tab)),"tabs",tabs.stream().map(t->Map.of("title",t.title)).toList()));}
 private void restoreTabs(Path profile){
  sessionFile=profile.resolve("tabs.json");int active=0;
  try{if(Files.isRegularFile(sessionFile)&&Files.size(sessionFile)<=65536){var saved=JsonParser.parseString(Files.readString(sessionFile)).getAsJsonObject();if(saved.get("version").getAsInt()!=1)throw new IOException("Unsupported tab state");for(var item:saved.getAsJsonArray("tabs")){if(tabs.size()==MAX_TABS)break;var data=item.getAsJsonObject();String address=data.get("url").getAsString();if(address.length()<=2048&&allowed(address))tabs.add(new Tab(address,limited(data.has("title")?data.get("title").getAsString():"Chromium",200)));}active=saved.get("active").getAsInt();}}
  catch(Exception ignored){tabs.clear();}
  if(tabs.isEmpty())tabs.add(new Tab(HOME,"Google"));selected=tabs.get(Math.max(0,Math.min(tabs.size()-1,active)));
 }
 private synchronized void saveTabs(){
  if(sessionFile==null||tabs.isEmpty())return;
  var snapshot=List.copyOf(tabs);var data=new JsonObject();data.addProperty("version",1);data.addProperty("active",Math.max(0,snapshot.indexOf(selected)));var list=new JsonArray();for(var tab:snapshot){var item=new JsonObject();item.addProperty("url",allowed(tab.url)?tab.url:HOME);item.addProperty("title",limited(tab.title,200));list.add(item);}data.add("tabs",list);var temp=sessionFile.resolveSibling("tabs.json.tmp");
  try{Files.writeString(temp,JSON.toJson(data));try{Files.move(temp,sessionFile,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException ignored){Files.move(temp,sessionFile,StandardCopyOption.REPLACE_EXISTING);}}
  catch(IOException failure){System.err.println("Browser tabs could not be saved.");}
 }
 private void activate(Tab tab){
  var old=selected;if(old!=null&&old.browser!=null){old.browser.setFocus(false);old.browser.setWindowVisibility(false);}var pending=dialog;dialog=null;if(pending!=null)pending.Continue(false,"");
  selected=tab;pressed=clicks=0;wheelRemainder[0]=wheelRemainder[1]=0;latest.set(null);focused=true;
  if(tab.browser==null){tab.painter=new TabPainter(tab);tab.browser=new CemOffscreenBrowser(client,tab.url,renderWidth,renderHeight,tab.painter,frameRate);browser=tab.browser;browser.createImmediately();}
  else{browser=tab.browser;browser.resize(renderWidth,renderHeight);browser.setWindowVisibility(true);browser.setFocus(true);tab.painter.publish();browser.refresh();}
  state();saveTabs();
 }
 private void openTab(String address){if(tabs.size()>=MAX_TABS){event(Map.of("type","notice","message","You can keep up to eight tabs open."));return;}var tab=new Tab(allowed(address)?address:HOME,"Chromium");tabs.add(tab);activate(tab);}
 private void closeTab(int index){if(index<0||index>=tabs.size())return;var tab=tabs.get(index);if(tabs.size()==1){tab.url=HOME;tab.title="Google";tab.browser.loadURL(HOME);saveTabs();return;}tabs.remove(tab);if(tab==selected)activate(tabs.get(Math.min(index,tabs.size()-1)));else{state();saveTabs();}if(tab.browser!=null){tab.browser.setCloseAllowed();tab.browser.close(true);}}
 private final class TabPainter implements CemOffscreenBrowser.Painter {
  private final Tab tab;private Frame base,overlay;private boolean visible;private java.awt.Rectangle bounds=new java.awt.Rectangle();TabPainter(Tab tab){this.tab=tab;}
  @Override public synchronized void paint(boolean isPopup,ByteBuffer pixels,int w,int h){if(!running||tab!=selected||w<1||h<1||w>MAX_WIDTH||h>MAX_HEIGHT||pixels.capacity()<w*h*4)return;byte[] data=new byte[w*h*4];var copy=pixels.duplicate();copy.position(0);copy.get(data);if(isPopup)overlay=new Frame(w,h,data);else base=new Frame(w,h,data);publish();}
  @Override public synchronized void popup(boolean show){visible=show;if(!show)overlay=null;if(tab==selected)event(Map.of("type","popup","show",show));publish();}
  @Override public synchronized void popupSize(java.awt.Rectangle rect){bounds=new java.awt.Rectangle(rect);if(tab==selected)event(Map.of("type","popupSize","x",rect.x,"y",rect.y,"width",rect.width,"height",rect.height));publish();}
  synchronized void publish(){if(base==null||tab!=selected||!running)return;if(!visible||overlay==null){latest.set(base);return;}byte[] result=base.bytes.clone();for(int yy=0;yy<overlay.height;yy++){int y=bounds.y+yy;if(y<0||y>=base.height)continue;int sourceX=Math.max(0,-bounds.x),x=Math.max(0,bounds.x),length=Math.min(overlay.width-sourceX,base.width-x);if(length>0)System.arraycopy(overlay.bytes,(yy*overlay.width+sourceX)*4,result,(y*base.width+x)*4,length*4);}latest.set(new Frame(base.width,base.height,result));}
 }
 private static boolean allowed(String value){try{var uri=new URI(value);return (uri.getScheme().equalsIgnoreCase("http")||uri.getScheme().equalsIgnoreCase("https"))&&uri.getHost()!=null&&uri.getUserInfo()==null;}catch(Exception ignored){return false;}}
 private static String limited(String value,int max){return value==null?"":value.substring(0,Math.min(value.length(),max));}
 public static void main(String[] args){Thread.setDefaultUncaughtExceptionHandler((thread,error)->{try(var log=new PrintWriter(new FileWriter("browser-thread-errors.log",true))){log.println(thread.getName());error.printStackTrace(log);}catch(IOException ignored){}});try{new BrowserHelper().run(args);}catch(Throwable failure){failure.printStackTrace();System.exit(1);}}
 private void run(String[] args)throws Exception {
  if(args.length!=6)throw new IllegalArgumentException("Invalid helper launch");
  var engine=Path.of(args[0]).toAbsolutePath();var profile=Path.of(args[1]).toAbsolutePath();int port=Integer.parseInt(args[2]);String token=args[3];int width=Integer.parseInt(args[4]),height=Integer.parseInt(args[5]);
  if(width<1||width>MAX_WIDTH||height<1||height>MAX_HEIGHT||token.length()!=64)throw new IllegalArgumentException("Invalid helper bounds");
  Files.createDirectories(profile);renderWidth=width;renderHeight=height;restoreTabs(profile);
  try(var socket=new Socket()){
   socket.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),port),10000);socket.setTcpNoDelay(true);
   var input=new DataInputStream(new BufferedInputStream(socket.getInputStream()));var output=new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
   output.writeUTF(token);output.writeInt(MAGIC);output.flush();
   var builder=new CefAppBuilder();builder.setInstallDir(engine.toFile());builder.setSkipInstallation(true);
   builder.addJcefArgs("--disable-gpu","--disable-extensions","--disable-component-update");
   var settings=builder.getCefSettings();settings.windowless_rendering_enabled=true;settings.persist_session_cookies=true;settings.root_cache_path=profile.toString();settings.cache_path=profile.resolve("Cache").toString();settings.browser_subprocess_path=engine.resolve("jcef_helper.exe").toString();settings.log_file=profile.resolve("cef.log").toString();
   builder.setAppHandler(new MavenCefAppHandlerAdapter(){@Override public void stateHasChanged(CefApp.CefAppState state){if(state==CefApp.CefAppState.TERMINATED)terminated.countDown();}});
   app=builder.build();client=app.createClient();
   client.addLifeSpanHandler(new CefLifeSpanHandlerAdapter(){@Override public void onAfterCreated(CefBrowser value){if(value==browser){value.setWindowVisibility(true);value.setFocus(true);if(!readySent){readySent=true;event(Map.of("type","ready"));}state();}else value.setWindowVisibility(false);}@Override public boolean onBeforePopup(CefBrowser value,CefFrame frame,String target,String name){if(allowed(target))EventQueue.invokeLater(()->openTab(target));return true;}@Override public void onBeforeClose(CefBrowser value){if(shuttingDown)closingBrowsers.countDown();}});
   client.addDisplayHandler(new CefDisplayHandlerAdapter(){@Override public void onAddressChange(CefBrowser value,CefFrame frame,String address){var tab=tab(value);if(running&&tab!=null&&frame.isMain()&&allowed(address)){tab.url=limited(address,2048);state();saveTabs();}}@Override public void onTitleChange(CefBrowser value,String text){var tab=tab(value);if(running&&tab!=null){tab.title=limited(text,200);state();saveTabs();}}});
   client.addLoadHandler(new CefLoadHandlerAdapter(){@Override public void onLoadingStateChange(CefBrowser value,boolean busy,boolean b,boolean f){var tab=tab(value);if(tab!=null&&running){tab.loading=busy;tab.back=b;tab.forward=f;state();if(tab==selected&&!busy&&focused)value.setFocus(true);}}@Override public void onLoadError(CefBrowser value,CefFrame frame,ErrorCode code,String text,String address){if(value==browser&&frame.isMain()&&code!=ErrorCode.ERR_ABORTED)event(Map.of("type","error","message",limited("Page could not load: "+text,300)));}});
   client.addRequestHandler(new CefRequestHandlerAdapter(){@Override public boolean onBeforeBrowse(CefBrowser value,CefFrame frame,CefRequest request,boolean gesture,boolean redirect){String address=request.getURL();return !(allowed(address)||address.equals("about:blank"));}@Override public boolean onOpenURLFromTab(CefBrowser value,CefFrame frame,String target,boolean gesture){if(allowed(target))EventQueue.invokeLater(()->openTab(target));return true;}});
   client.addDownloadHandler(new CefDownloadHandlerAdapter(){@Override public boolean onBeforeDownload(CefBrowser value,CefDownloadItem item,String name,CefBeforeDownloadCallback callback){event(Map.of("type","notice","message","Website file downloads are not enabled."));return true;}@Override public void onDownloadUpdated(CefBrowser value,CefDownloadItem item,CefDownloadItemCallback callback){callback.cancel();}});
   client.addDialogHandler((value,mode,dialogTitle,defaultPath,filters,extensions,descriptions,callback)->{callback.Cancel();event(Map.of("type","notice","message","Website file uploads are not enabled."));return true;});

   client.addContextMenuHandler(new CefContextMenuHandlerAdapter(){@Override public void onBeforeContextMenu(CefBrowser value,CefFrame frame,CefContextMenuParams params,CefMenuModel model){model.clear();}});
   client.addJSDialogHandler(new CefJSDialogHandlerAdapter(){@Override public boolean onJSDialog(CefBrowser value,String origin,JSDialogType type,String message,String initial,CefJSDialogCallback callback,BoolRef suppress){if(value!=browser){callback.Continue(false,"");return true;}var old=dialog;if(old!=null)old.Continue(false,"");dialog=callback;event(Map.of("type","dialog","kind",type.name(),"message",limited(message,1000),"initial",limited(initial,500)));return true;}@Override public boolean onBeforeUnloadDialog(CefBrowser value,String message,boolean reload,CefJSDialogCallback callback){callback.Continue(true,"");return true;}@Override public void onResetDialogState(CefBrowser value){if(value==browser)dialog=null;}});
   var writer=new Thread(()->{try{while(running){long frameStart=System.nanoTime();String message;while((message=events.poll())!=null){output.writeByte(1);output.writeUTF(message);}var frame=latest.getAndSet(null);if(frame!=null){output.writeByte(2);output.writeInt(frame.width);output.writeInt(frame.height);output.writeInt(frame.bytes.length);output.write(frame.bytes);}output.flush();long wait=1_000_000_000L/frameRate-(System.nanoTime()-frameStart);if(wait>0)Thread.sleep(wait/1_000_000,(int)(wait%1_000_000));}}catch(Exception e){running=false;try{socket.close();}catch(IOException ignored){}}},"CEM browser pixels");writer.setDaemon(true);writer.start();
   EventQueue.invokeLater(()->activate(selected));
   try{while(running){String text=input.readUTF();if(text.length()>8192)throw new IOException("Oversized browser input");var command=JsonParser.parseString(text).getAsJsonObject();if(command.get("action").getAsString().equals("close")){running=false;break;}EventQueue.invokeLater(()->{try{command(command);}catch(RuntimeException failure){event(Map.of("type","error","message","Browser input could not be processed."));}});}}catch(EOFException ignored){}finally{running=false;shutdown();}
  }
 }
 private void shutdown()throws InterruptedException {
  var pending=dialog;dialog=null;if(pending!=null)pending.Continue(false,"");
  saveTabs();shuttingDown=true;var opened=tabs.stream().map(t->t.browser).filter(Objects::nonNull).toList();closingBrowsers=new CountDownLatch(opened.size());EventQueue.invokeLater(()->{for(var value:opened){value.setCloseAllowed();value.close(true);}});closingBrowsers.await(3,TimeUnit.SECONDS);
  if(app!=null){var flushed=new CountDownLatch(1);try{if(CefCookieManager.getGlobalManager().flushStore(flushed::countDown)&&!flushed.await(2,TimeUnit.SECONDS))System.err.println("Browser cookie storage flush timed out.");}catch(RuntimeException failure){System.err.println("Browser cookie storage could not be flushed.");}}
  if(client!=null)client.dispose();if(app!=null){app.dispose();terminated.await(5,TimeUnit.SECONDS);}
  ProcessHandle.current().descendants().forEach(ProcessHandle::destroy);
 }
 private static int integer(JsonObject value,String key){return value.get(key).getAsInt();}
 private static int modifiers(int flags){int result=0;if((flags&1)!=0)result|=InputEvent.SHIFT_DOWN_MASK;if((flags&2)!=0)result|=InputEvent.CTRL_DOWN_MASK;if((flags&4)!=0)result|=InputEvent.ALT_DOWN_MASK;if((flags&8)!=0)result|=InputEvent.META_DOWN_MASK;return result;}
 private void command(JsonObject value){switch(value.get("action").getAsString()){
  case "navigate"->{String address=limited(value.get("url").getAsString(),2048);if(allowed(address))browser.loadURL(address);else event(Map.of("type","error","message","Use an HTTP or HTTPS address."));}
  case "newTab"->openTab(HOME);
  case "switchTab"->{int index=integer(value,"index");if(index>=0&&index<tabs.size())activate(tabs.get(index));}
  case "closeTab"->closeTab(integer(value,"index"));
  case "back"->browser.goBack();case "forward"->browser.goForward();case "reload"->browser.reload();
  case "resize"->{int w=integer(value,"width"),h=integer(value,"height");if(w>=1&&h>=1&&w<=MAX_WIDTH&&h<=MAX_HEIGHT){renderWidth=w;renderHeight=h;browser.resize(w,h);}}
  case "focus"->{focused=value.get("focus").getAsBoolean();browser.setFocus(focused);}
  case "mouse"->{int kind=integer(value,"kind"),button=integer(value,"button"),mods=modifiers(integer(value,"mods")),x=integer(value,"x"),y=integer(value,"y");if(button<0||button>2||kind<0||kind>2)return;int mapped=button==0?1:button==1?3:2;if(kind==1){focused=true;browser.setFocus(true);long now=System.currentTimeMillis();clicks=button==lastButton&&now-lastPress<500&&Math.abs(x-lastX)<4&&Math.abs(y-lastY)<4?Math.min(3,clicks+1):1;lastPress=now;lastButton=button;lastX=x;lastY=y;pressed|=1<<button;}if(kind==2)pressed&=~(1<<button);if((pressed&1)!=0)mods|=InputEvent.BUTTON1_DOWN_MASK;if((pressed&2)!=0)mods|=InputEvent.BUTTON3_DOWN_MASK;if((pressed&4)!=0)mods|=InputEvent.BUTTON2_DOWN_MASK;browser.mouse(kind==0?(pressed!=0?MouseEvent.MOUSE_DRAGGED:MouseEvent.MOUSE_MOVED):kind==1?MouseEvent.MOUSE_PRESSED:MouseEvent.MOUSE_RELEASED,x,y,kind==0?0:mapped,mods,kind==0?0:clicks);if(kind==2)pressed&=~(1<<button);}
  case "wheel"->{double amount=value.get("amount").getAsDouble();if(!Double.isFinite(amount)||Math.abs(amount)>100)return;int mods=modifiers(integer(value,"mods")),axis=(mods&InputEvent.SHIFT_DOWN_MASK)!=0?1:0;wheelRemainder[axis]+=amount;int whole=(int)wheelRemainder[axis];if(whole!=0){wheelRemainder[axis]-=whole;focused=true;browser.setFocus(true);browser.wheel(integer(value,"x"),integer(value,"y"),whole,mods);}}
  case "key"->{int key=integer(value,"key"),code=keyCode(key),scan=value.has("scan")?integer(value,"scan"):scanCode(key);if(code!=KeyEvent.VK_UNDEFINED&&scan>0&&scan<512)browser.key(value.get("release").getAsBoolean()?KeyEvent.KEY_RELEASED:KeyEvent.KEY_PRESSED,code,KeyEvent.CHAR_UNDEFINED,modifiers(integer(value,"mods")),scan);}
  case "char"->{int point=integer(value,"point");if(Character.isValidCodePoint(point)&&point>=32)for(char c:Character.toChars(point))browser.key(KeyEvent.KEY_TYPED,KeyEvent.VK_UNDEFINED,c,modifiers(integer(value,"mods")),0);}
  case "dialog"->{var callback=dialog;dialog=null;if(callback!=null)callback.Continue(value.get("accept").getAsBoolean(),limited(value.get("text").getAsString(),500));}
  default->{}
 }}
 private static int scanCode(int key){if(key>=65&&key<=90)return new int[]{30,48,46,32,18,33,34,35,23,36,37,38,50,49,24,25,16,19,31,20,22,47,17,45,21,44}[key-65];if(key>=49&&key<=57)return key-47;if(key==48)return 11;if(key>=290&&key<=299)return key-231;return switch(key){case 32->57;case 39->40;case 44->51;case 45->12;case 46->52;case 47->53;case 59->39;case 61->13;case 91->26;case 92->43;case 93->27;case 96->41;case 256->1;case 257->28;case 335->284;case 258->15;case 259->14;case 260->338;case 261->339;case 262->333;case 263->331;case 264->336;case 265->328;case 266->329;case 267->337;case 268->327;case 269->335;case 300->87;case 301->88;case 340->42;case 344->54;case 341->29;case 345->285;case 342->56;case 346->312;case 343->347;case 347->348;default->0;};}
 private static int keyCode(int key){if(key>=65&&key<=90||key>=48&&key<=57)return key;if(key>=290&&key<=301)return KeyEvent.VK_F1+key-290;if(key>=302&&key<=313)return KeyEvent.VK_F13+key-302;return switch(key){case 32->KeyEvent.VK_SPACE;case 39->KeyEvent.VK_QUOTE;case 44->KeyEvent.VK_COMMA;case 45->KeyEvent.VK_MINUS;case 46->KeyEvent.VK_PERIOD;case 47->KeyEvent.VK_SLASH;case 59->KeyEvent.VK_SEMICOLON;case 61->KeyEvent.VK_EQUALS;case 91->KeyEvent.VK_OPEN_BRACKET;case 92->KeyEvent.VK_BACK_SLASH;case 93->KeyEvent.VK_CLOSE_BRACKET;case 96->KeyEvent.VK_BACK_QUOTE;case 256->KeyEvent.VK_ESCAPE;case 257,335->KeyEvent.VK_ENTER;case 258->KeyEvent.VK_TAB;case 259->KeyEvent.VK_BACK_SPACE;case 260->KeyEvent.VK_INSERT;case 261->KeyEvent.VK_DELETE;case 262->KeyEvent.VK_RIGHT;case 263->KeyEvent.VK_LEFT;case 264->KeyEvent.VK_DOWN;case 265->KeyEvent.VK_UP;case 266->KeyEvent.VK_PAGE_UP;case 267->KeyEvent.VK_PAGE_DOWN;case 268->KeyEvent.VK_HOME;case 269->KeyEvent.VK_END;case 340,344->KeyEvent.VK_SHIFT;case 341,345->KeyEvent.VK_CONTROL;case 342,346->KeyEvent.VK_ALT;case 343,347->KeyEvent.VK_META;default->KeyEvent.VK_UNDEFINED;};}
}
