package net.mcreator.commandcookiesretroexpansion.retro.client;
import java.io.*;
import java.nio.file.*;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import javax.sound.sampled.*;
import net.mcreator.commandcookiesretroexpansion.retro.RetroPackets;

/** IPC worker: emulation, disk I/O and audio never run on the Minecraft render thread. */
public final class EmulatorRunner implements AutoCloseable {
 public record Frame(int width,int height,int[] argb){}
 public final AtomicReference<Frame> picture=new AtomicReference<>();
 public volatile String status="Starting emulator…";public volatile boolean paused;public volatile int buttons;public volatile short axisX,axisY;public volatile float volume=1;public volatile long emulatedFrames;
 private volatile boolean stop,save,reset;private volatile Process process;private volatile long progress=System.nanoTime();private volatile SourceDataLine audio;private volatile N64Audio n64Audio;
 private final Thread worker;private final ScheduledExecutorService watchdog=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"Retro emulator watchdog");t.setDaemon(true);return t;});
 public EmulatorRunner(RetroPackets.Open session){worker=new Thread(()->run(session),"Retro emulator IPC");worker.setDaemon(true);worker.start();watchdog.scheduleAtFixedRate(()->{var p=process;if(p!=null&&System.nanoTime()-progress>TimeUnit.SECONDS.toNanos(emulatedFrames==0?30:15)){status="The emulator stopped responding. Close and reopen the monitor.";p.destroyForcibly();}},1,1,TimeUnit.SECONDS);}
 public void save(){save=true;}public void reset(){reset=true;}
 private int ack(DataInputStream in)throws IOException{int n=in.readInt();if(n!=0x41434b31)throw new IOException("Invalid emulator reply.");return n;}
 private void run(RetroPackets.Open session){
  try{
   status="Starting "+session.system().label+"…";Path core=RomLibrary.readyCore(session.system());
   var rom=RomLibrary.find(session.hash(),session.system());Path helper=RomLibrary.runtime("helper.jar"),jna=RomLibrary.runtime("jna.jar");
   Path saves=RomLibrary.root().resolve("saves").resolve(session.world().toString()).resolve(session.console().toString()).resolve(session.hash()).resolve(RomLibrary.digest(Files.readAllBytes(core)));Files.createDirectories(saves);
   Path java=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java");
   var builder=new ProcessBuilder(java.toString(),"--enable-native-access=ALL-UNNAMED","-cp",helper+File.pathSeparator+jna,"cem.retro.bridge.LibretroHost",core.toAbsolutePath().toString(),rom.file().toString(),saves.toAbsolutePath().toString(),session.system().id,RomLibrary.root().resolve("system").toAbsolutePath().toString());
   Files.createDirectories(RomLibrary.root().resolve("system"));
   if(session.system()!=net.mcreator.commandcookiesretroexpansion.retro.RetroSystem.NES)builder=new ProcessBuilder(RomLibrary.readyFrontend().toString(),core.toAbsolutePath().toString(),rom.file().toString(),saves.toAbsolutePath().toString(),session.system().id,RomLibrary.root().resolve("system").toAbsolutePath().toString());
   // Stderr is drained with a cap so a verbose native core cannot stall or fill the disk.
   process=builder.start();var p=process;if(stop){p.destroyForcibly();return;}
   Thread logs=new Thread(()->{try(var reader=new BufferedReader(new InputStreamReader(p.getErrorStream()));var writer=Files.newBufferedWriter(saves.resolve("emulator.log"))){String s;int chars=0;while((s=reader.readLine())!=null){if(chars<65536){writer.write(s);writer.newLine();chars+=s.length();}}}catch(IOException ignored){}},"Retro emulator log");logs.setDaemon(true);logs.start();
   try(var in=new DataInputStream(new BufferedInputStream(p.getInputStream()));var out=new DataOutputStream(new BufferedOutputStream(p.getOutputStream()))){
    if(in.readInt()!=0x43455232)throw new IOException("Invalid emulator startup.");int rate=in.readInt();double fps=in.readDouble();if(rate<8000||rate>192000||!Double.isFinite(fps)||fps<20||fps>120)throw new IOException("Invalid emulator timing.");
    if(session.system()==net.mcreator.commandcookiesretroexpansion.retro.RetroSystem.N64){
     try{n64Audio=new N64Audio(rate);}catch(LineUnavailableException|IllegalArgumentException e){n64Audio=null;}
    }else{try{var format=new AudioFormat(rate,16,2,true,false);audio=AudioSystem.getSourceDataLine(format);audio.open(format,(rate/8)*4);audio.start();}catch(LineUnavailableException|IllegalArgumentException e){audio=null;}}
    status="";byte[] encodedPixels=new byte[0];int frames=0;long next=System.nanoTime(),period=(long)(1_000_000_000d/fps);
    while(!stop){progress=System.nanoTime();
     if(save){save=false;out.writeByte(2);out.flush();ack(in);}
     if(reset){reset=false;out.writeByte(3);out.flush();ack(in);if(audio!=null)audio.flush();if(n64Audio!=null)n64Audio.flush();}
     if(paused){buttons=0;axisX=axisY=0;if(audio!=null)audio.flush();if(n64Audio!=null)n64Audio.flush();next=System.nanoTime();LockSupport.parkNanos(10_000_000);continue;}
     out.writeByte(1);out.writeInt(buttons);out.writeShort(axisX);out.writeShort(axisY);out.flush();if(in.readInt()!=0x46524d31)throw new IOException("Invalid emulator frame.");int w=in.readInt(),h=in.readInt();if(w<1||h<1||w>2048||h>1024)throw new IOException("Invalid emulator picture.");
     int[] pixels=new int[w*h];if(session.system()==net.mcreator.commandcookiesretroexpansion.retro.RetroSystem.N64){int size=pixels.length*4;if(encodedPixels.length<size)encodedPixels=new byte[size];in.readFully(encodedPixels,0,size);ByteBuffer.wrap(encodedPixels,0,size).asIntBuffer().get(pixels);}else for(int i=0;i<pixels.length;i++)pixels[i]=in.readInt();int n=in.readInt();if(n<0||n>65536||n%4!=0)throw new IOException("Invalid emulator audio.");byte[] pcm=in.readNBytes(n);if(pcm.length!=n)throw new EOFException();picture.set(new Frame(w,h,pixels));
     if((audio!=null||n64Audio!=null)&&n>0){float v=Math.max(0,Math.min(1,volume));if(v<.999f)for(int i=0;i<n;i+=2){short sample=(short)((pcm[i]&255)|(pcm[i+1]<<8));int scaled=(int)(sample*v);pcm[i]=(byte)scaled;pcm[i+1]=(byte)(scaled>>8);}if(n64Audio!=null)n64Audio.offer(pcm);else audio.write(pcm,0,n);}
     emulatedFrames=++frames;if(frames%600==0){out.writeByte(2);out.flush();ack(in);}
     next+=period;long now=System.nanoTime();if(now-next>period*4)next=now;LockSupport.parkNanos(Math.max(0,next-now));
    }
    out.writeByte(4);out.flush();ack(in);p.waitFor(3,TimeUnit.SECONDS);
   }
  }catch(Exception e){if(!stop)status=e instanceof EOFException?"The "+session.system().label+" emulator could not start or stopped. See emulator.log in its save folder.":e.getMessage()==null?"Emulator failed. See emulator.log in the save folder.":e.getMessage();}
  finally{var output=n64Audio;if(output!=null)output.close();var a=audio;if(a!=null){a.stop();a.close();}var p=process;if(p!=null&&p.isAlive())p.destroyForcibly();watchdog.shutdownNow();}
 }
 /** Java Sound can block for several tenths of a second at N64 rates.
  * Keep it off the emulation worker; a bounded queue also prevents stale
  * audio accumulating if an output device stops accepting samples. */
 private static final class N64Audio implements AutoCloseable {
  private record Chunk(long epoch,long sequence,byte[] pcm){}
  private final ArrayBlockingQueue<Chunk> pending=new ArrayBlockingQueue<>(6);
  private final SourceDataLine line;private final Thread thread;private final PcmResampler resampler;
  private volatile boolean closed;private volatile long epoch;private long sequence;
  N64Audio(int rate)throws LineUnavailableException {
   resampler=new PcmResampler(rate,48000);
   var format=new AudioFormat(48000,16,2,true,false);
   line=AudioSystem.getSourceDataLine(format);
   try{line.open(format,19200);line.start();}catch(LineUnavailableException|IllegalArgumentException e){line.close();throw e;}
   thread=new Thread(this::play,"Retro N64 audio");thread.setDaemon(true);thread.start();
  }
  void offer(byte[] pcm){
   if(closed)return;var chunk=new Chunk(epoch,sequence++,pcm);
   // Prefer current sound over a growing backlog. This never waits for the device.
   if(!pending.offer(chunk)){pending.poll();pending.offer(chunk);}
  }
  void flush(){epoch++;pending.clear();}
  private void play(){long currentEpoch=epoch,lastSequence=-1;
   try{while(!closed){
    if(currentEpoch!=epoch){line.flush();resampler.reset();currentEpoch=epoch;lastSequence=-1;}
    var chunk=pending.poll(10,TimeUnit.MILLISECONDS);if(chunk==null||chunk.epoch()!=epoch)continue;
    if(lastSequence>=0&&chunk.sequence()!=lastSequence+1){line.flush();resampler.reset();}
    lastSequence=chunk.sequence();int length=resampler.convert(chunk.pcm());
    for(int offset=0;offset<length&&!closed&&chunk.epoch()==epoch;){int written=line.write(resampler.output,offset,length-offset);if(written<=0)break;offset+=written;}
   }}catch(InterruptedException ignored){Thread.currentThread().interrupt();}
   catch(RuntimeException ignored){/* A failed sound device must not stop the game. */}
   finally{closed=true;pending.clear();try{line.stop();}finally{line.close();}}
  }
  @Override public void close(){closed=true;pending.clear();thread.interrupt();}
 }
 /** Stateful stereo interpolation preserves sample timing across audio packets. */
 private static final class PcmResampler {
  private final double step;private double fraction;private boolean previous;private int left,right;private byte[] output=new byte[0];
  PcmResampler(int inputRate,int outputRate){step=(double)inputRate/outputRate;}
  void reset(){fraction=0;previous=false;}
  int convert(byte[] input){
   int capacity=(int)Math.ceil((input.length/4+2)/step)*4;
   if(output.length<capacity)output=new byte[capacity];int length=0;
   for(int i=0;i<input.length;i+=4){int nextLeft=(short)((input[i]&255)|(input[i+1]<<8)),nextRight=(short)((input[i+2]&255)|(input[i+3]<<8));
    if(previous){while(fraction<1){int l=(int)Math.round(left+(nextLeft-left)*fraction),r=(int)Math.round(right+(nextRight-right)*fraction);output[length++]=(byte)l;output[length++]=(byte)(l>>8);output[length++]=(byte)r;output[length++]=(byte)(r>>8);fraction+=step;}fraction-=1;}
    previous=true;left=nextLeft;right=nextRight;
   }return length;
  }
 }

 @Override public void close(){stop=true;buttons=0;axisX=axisY=0;paused=false;var output=n64Audio;if(output!=null)output.close();var a=audio;if(a!=null)a.flush();worker.interrupt();var p=process;if(p!=null)CompletableFuture.delayedExecutor(4,TimeUnit.SECONDS).execute(()->{if(p.isAlive())p.destroyForcibly();});}
}
