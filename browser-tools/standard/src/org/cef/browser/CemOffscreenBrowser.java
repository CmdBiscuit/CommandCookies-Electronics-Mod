package org.cef.browser;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.cef.*;
import org.cef.callback.CefDragData;
import org.cef.handler.*;

/** Paints into the parent-owned pixel buffer without an AWT window or OpenGL context. */
public final class CemOffscreenBrowser extends CefBrowser_N implements CefRenderHandler {
 public interface Painter {void paint(boolean popup,ByteBuffer pixels,int width,int height);default void popup(boolean show){}default void popupSize(Rectangle bounds){}}
 private final Canvas input=new Canvas();
 private final Painter painter;
 private volatile Rectangle view;
 public CemOffscreenBrowser(CefClient client,String url,int width,int height,Painter painter){this(client,url,width,height,painter,25);}
 public CemOffscreenBrowser(CefClient client,String url,int width,int height,Painter painter,int frameRate){super(client,url,null,null,null,settings(frameRate));this.painter=painter;view=new Rectangle(0,0,width,height);input.setSize(width,height);}
 private static CefBrowserSettings settings(int frameRate){var value=new CefBrowserSettings();value.windowless_frame_rate=frameRate==60?60:25;return value;}
 @Override public void createImmediately(){createBrowser(getClient(),0,getUrl(),true,false,null,getRequestContext());}
 @Override public Component getUIComponent(){return input;}
 @Override public CefRenderHandler getRenderHandler(){return this;}
 @Override protected CefBrowser_N createDevToolsBrowser(CefClient client,String url,CefRequestContext context,CefBrowser_N parent,Point at){throw new UnsupportedOperationException("Developer tools are not enabled.");}
 @Override public CompletableFuture<BufferedImage> createScreenshot(boolean nativeResolution){return CompletableFuture.failedFuture(new UnsupportedOperationException("Use the parent frame buffer."));}
 public void resize(int width,int height){view=new Rectangle(0,0,width,height);input.setSize(width,height);wasResized(width,height);}
 public void refresh(){invalidate();}
 public void mouse(int event,int x,int y,int button,int modifiers,int clicks){sendMouseEvent(new MouseEvent(input,event,System.currentTimeMillis(),modifiers,x,y,clicks,false,button));}
 // JCEF forwards these units directly; CEF uses positive deltas to scroll upward.
 public void wheel(int x,int y,double amount,int modifiers){sendMouseWheelEvent(new MouseWheelEvent(input,MouseEvent.MOUSE_WHEEL,System.currentTimeMillis(),modifiers,x,y,x,y,0,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,120,-(int)Math.round(amount),-amount));}
 public void key(int event,int code,char text,int modifiers,int scan){sendKeyEvent(new BrowserKeyEvent(input,event,modifiers,code,text,scan));}
 /** JCEF on Windows reads this field directly; synthetic JDK events have no hardware scancode. */
 private static final class BrowserKeyEvent extends KeyEvent {
  public final long scancode;
  BrowserKeyEvent(Component source,int event,int modifiers,int code,char text,int scan){super(source,event,System.currentTimeMillis(),modifiers,code,text);scancode=scan;}
 }
 @Override public Rectangle getViewRect(CefBrowser browser){return new Rectangle(view);}
 // Use CEF's view-rectangle fallback; JCEF 152 double-releases a supplied screen-info reference.
 @Override public boolean getScreenInfo(CefBrowser browser,CefScreenInfo info){return false;}
 @Override public Point getScreenPoint(CefBrowser browser,Point point){return new Point(point);}
 @Override public void onPopupShow(CefBrowser browser,boolean show){painter.popup(show);}
 @Override public void onPopupSize(CefBrowser browser,Rectangle size){painter.popupSize(size);}
 @Override public void onPaint(CefBrowser browser,boolean popup,Rectangle[] dirty,ByteBuffer pixels,int width,int height){painter.paint(popup,pixels,width,height);}
 @Override public void addOnPaintListener(Consumer<CefPaintEvent> listener){}
 @Override public void setOnPaintListener(Consumer<CefPaintEvent> listener){}
 @Override public void removeOnPaintListener(Consumer<CefPaintEvent> listener){}
 @Override public boolean onCursorChange(CefBrowser browser,int type){return true;}
 @Override public boolean startDragging(CefBrowser browser,CefDragData data,int mask,int x,int y){return false;}
 @Override public void updateDragCursor(CefBrowser browser,int operation){}
}
