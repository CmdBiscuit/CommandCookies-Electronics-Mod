package cem.retro.bridge;

import com.sun.jna.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Standalone GPL-3.0-or-later frontend. No Minecraft classes or dependencies. */
public final class LibretroHost {
    public static final int READY=0x43455232, FRAME=0x46524d31, ACK=0x41434b31;
    public static final int MAX_STATE=64*1024*1024;
    public static class Size extends IntegerType {
        public Size(){this(0);} public Size(long n){super(Native.SIZE_T_SIZE,n,true);}
    }
    @Structure.FieldOrder({"path","data","size","meta"})
    public static class Game extends Structure {public String path;public Pointer data;public Size size=new Size();public String meta;}
    @Structure.FieldOrder({"baseWidth","baseHeight","maxWidth","maxHeight","aspect"})
    public static class Geometry extends Structure {public int baseWidth,baseHeight,maxWidth,maxHeight;public float aspect;}
    @Structure.FieldOrder({"fps","sampleRate"})
    public static class Timing extends Structure {public double fps,sampleRate;}
    @Structure.FieldOrder({"geometry","timing"})
    public static class AV extends Structure {public Geometry geometry=new Geometry();public Timing timing=new Timing();}
    @Structure.FieldOrder({"name","version","extensions","fullpath","blockExtract"})
    public static class Info extends Structure {public String name,version,extensions;public byte fullpath,blockExtract;}
    public interface Env extends Callback {boolean call(int cmd,Pointer data);}
    public interface Video extends Callback {void call(Pointer data,int width,int height,Size pitch);}
    public interface Audio extends Callback {void call(short left,short right);}
    public interface Batch extends Callback {Size call(Pointer data,Size frames);}
    public interface Poll extends Callback {void call();}
    public interface Input extends Callback {short call(int port,int device,int index,int id);}
    public interface Log extends Callback {void call(int level,Pointer text);}
    public interface Core extends Library {
        void retro_set_environment(Env c);void retro_set_video_refresh(Video c);
        void retro_set_audio_sample(Audio c);void retro_set_audio_sample_batch(Batch c);
        void retro_set_input_poll(Poll c);void retro_set_input_state(Input c);
        void retro_get_system_info(Info info);void retro_set_controller_port_device(int port,int device);
        void retro_init();void retro_deinit();void retro_run();void retro_reset();
        boolean retro_load_game(Game game);void retro_unload_game();void retro_get_system_av_info(AV info);
        Size retro_serialize_size();boolean retro_serialize(Pointer data,Size size);
        boolean retro_unserialize(Pointer data,Size size);Pointer retro_get_memory_data(int id);Size retro_get_memory_size(int id);
    }
    private final Core core;private Memory gameData;
    private final Path saves,systemDirectory;private final String system;
    private final Map<String,Memory> retained=new HashMap<>();
    private final Map<String,String> forced=new HashMap<>();
    private final Map<String,String> variables=new HashMap<>();
    private int format=0,width=256,height=240,buttons;private short axisX,axisY;
    private int[] pixels=new int[256*240];
    private final ByteArrayOutputStream sound=new ByteArrayOutputStream();
    private final Env environment=this::environment;
    private final Video video=this::video;
    private final Audio audio=(l,r)->{if(sound.size()<65536){sound.write(l&255);sound.write(l>>>8&255);sound.write(r&255);sound.write(r>>>8&255);}};
    private final Batch batch=(p,n)->{int count=(int)Math.min(16384,n.longValue());if(count>0&&sound.size()+count*4<=65536)sound.writeBytes(p.getByteArray(0,count*4));return new Size(n.longValue());};
    private final Poll poll=()->{};
    private final Input input=(port,device,index,id)->{if(port!=0)return 0;device&=255;if(device==1)return (short)(id==256?buttons:id>=0&&id<16?(buttons>>>id)&1:0);if(device==5&&index==0)return id==0?axisX:id==1?axisY:0;return 0;};
    private final Log log=(level,text)->{if(level>=2&&text!=null)System.err.print(text.getString(0));};
    private LibretroHost(Path library,Path saves,String system,Path systemDirectory){this.saves=saves;this.system=system;this.systemDirectory=systemDirectory;
        if(system.equals("n64")){forced.put("parallel-n64-gfxplugin","angrylion");forced.put("parallel-n64-rspplugin","cxd4");forced.put("parallel-n64-alt-map","enabled");forced.put("parallel-n64-upscaling","1x");forced.put("parallel-n64-cpucore","dynamic_recompiler");forced.put("parallel-n64-angrylion-multithread","4");}
        core=Native.load(library.toAbsolutePath().toString(),Core.class);
    }
    private void textPointer(Pointer target,String text){Memory m=retained.computeIfAbsent(text,k->{byte[] b=(k+"\0").getBytes(java.nio.charset.StandardCharsets.UTF_8);Memory p=new Memory(b.length);p.write(0,b,0,b.length);return p;});target.setPointer(0,m);}
    private boolean environment(int command,Pointer data){
        try {
            int cmd=command&0xffff;
            switch(cmd){
                case 3: data.setByte(0,(byte)1);return true;
                case 9: textPointer(data,systemDirectory.toAbsolutePath().toString());return true;
                case 31: textPointer(data,saves.toAbsolutePath().toString());return true;
                case 24: data.setLong(0,(1L<<1)|(1L<<5));return true;
                case 10: int f=data.getInt(0);if(f<0||f>2)return false;format=f;return true;
                case 15: Pointer key=data.getPointer(0);String value=key==null?null:forced.getOrDefault(key.getString(0),variables.get(key.getString(0)));if(value==null)return false;textPointer(data.share(Native.POINTER_SIZE),value);return true;
                case 16:
                    for(int i=0;i<256;i++){Pointer entry=data.share((long)i*Native.POINTER_SIZE*2);Pointer k=entry.getPointer(0),v=entry.getPointer(Native.POINTER_SIZE);if(k==null||v==null)break;String choices=v.getString(0);int split=choices.indexOf(';');if(split>=0)variables.put(k.getString(0),choices.substring(split+1).trim().split("\\|",2)[0]);}return true;
                case 17,49: data.setByte(0,(byte)0);return true;
                case 39: data.setInt(0,0);return true;
                case 27: data.setPointer(0,CallbackReference.getFunctionPointer(log));return true;
                case 51: return true;
                case 52: data.setInt(0,0);return true;
                case 11,18,35,37: return true;
                default:return false;
            }
        }catch(Throwable e){System.err.println("Environment "+command+": "+e);return false;}
    }
    private void video(Pointer source,int w,int h,Size stride){
        if(source==null||w<1||h<1||w>1024||h>1024||stride.longValue()>16384)return;
        int bytes=format==1?4:2;if(stride.longValue()<(long)w*bytes)return;
        if(pixels.length!=w*h)pixels=new int[w*h];width=w;height=h;
        for(int y=0;y<h;y++){
            if(format==1){int[] row=source.getIntArray(y*stride.longValue(),w);for(int x=0;x<w;x++)pixels[y*w+x]=0xff000000|(row[x]&0xffffff);}
            else {short[] row=source.getShortArray(y*stride.longValue(),w);for(int x=0;x<w;x++){int v=row[x]&0xffff;int r=format==2?v>>>11&31:v>>>10&31,g=format==2?v>>>5&63:v>>>5&31,b=v&31;pixels[y*w+x]=0xff000000|(r*255/31<<16)|(g*255/(format==2?63:31)<<8)|b*255/31;}}
        }
    }
    private void atomic(Path target,byte[] bytes)throws IOException{Path temp=target.resolveSibling(target.getFileName()+".tmp");Files.write(temp,bytes);try{Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}}
    private void save()throws IOException{
        long n=core.retro_serialize_size().longValue();if(n>0&&n<=MAX_STATE){Memory memory=new Memory(n);if(core.retro_serialize(memory,new Size(n)))atomic(saves.resolve("state.bin"),memory.getByteArray(0,(int)n));}
        n=core.retro_get_memory_size(0).longValue();Pointer memory=core.retro_get_memory_data(0);if(memory!=null&&n>0&&n<=MAX_STATE)atomic(saves.resolve("sram.bin"),memory.getByteArray(0,(int)n));
    }
    private void restore()throws IOException{
        Path sram=saves.resolve("sram.bin");long n=core.retro_get_memory_size(0).longValue();Pointer memory=core.retro_get_memory_data(0);
        if(memory!=null&&n>0&&n<=MAX_STATE&&Files.isRegularFile(sram)&&Files.size(sram)==n){byte[] b=Files.readAllBytes(sram);memory.write(0,b,0,b.length);}
        Path state=saves.resolve("state.bin");n=core.retro_serialize_size().longValue();
        if(n>0&&n<=MAX_STATE&&Files.isRegularFile(state)&&Files.size(state)==n){byte[] b=Files.readAllBytes(state);Memory m=new Memory(n);m.write(0,b,0,b.length);core.retro_unserialize(m,new Size(n));}
    }
    private void run(Path rom)throws Exception{
        Files.createDirectories(saves);Files.createDirectories(systemDirectory);
        long size=Files.size(rom);int cap=system.equals("n64")?128*1024*1024:16*1024*1024;int minimum=system.equals("nes")?16:system.equals("snes")?32768:4096;
        if(size<minimum||size>cap)throw new IOException("Invalid "+system+" game size");
        byte[] header;try(var file=Files.newInputStream(rom)){header=file.readNBytes(16);}
        if(system.equals("nes")&&(header[0]!='N'||header[1]!='E'||header[2]!='S'||header[3]!=26))throw new IOException("Expected an iNES game");
        if(system.equals("n64")){int signature=((header[0]&255)<<24)|((header[1]&255)<<16)|((header[2]&255)<<8)|(header[3]&255);if(signature!=0x80371240&&signature!=0x37804012&&signature!=0x40123780)throw new IOException("Expected an N64 cartridge image");}
        if(system.equals("snes")&&size%1024!=0&&size%1024!=512)throw new IOException("Expected a SNES cartridge image");
        core.retro_set_environment(environment);core.retro_init();core.retro_set_video_refresh(video);core.retro_set_audio_sample(audio);core.retro_set_audio_sample_batch(batch);core.retro_set_input_poll(poll);core.retro_set_input_state(input);
        Info info=new Info();core.retro_get_system_info(info);info.read();Game game=new Game();game.path=rom.toAbsolutePath().toString();Memory content=null;
        if(info.fullpath==0){byte[] bytes;try(var file=Files.newInputStream(rom)){bytes=file.readNBytes(cap+1);}if(bytes.length!=size)throw new IOException("ROM changed while reading");content=new Memory(bytes.length);content.write(0,bytes,0,bytes.length);game.data=content;game.size=new Size(bytes.length);}
        gameData=content;game.write();if(!core.retro_load_game(game)){core.retro_deinit();throw new IOException("The "+system+" core rejected this game");}
        core.retro_set_controller_port_device(0,1);
        try {
            core.retro_set_input_state(input);AV av=new AV();core.retro_get_system_av_info(av);av.read();
            if(av.timing.sampleRate<8000||av.timing.sampleRate>192000||av.timing.fps<20||av.timing.fps>120)throw new IOException("Invalid emulator timing");
            if(system.equals("n64")){core.retro_run();sound.reset();}restore();var out=new DataOutputStream(new BufferedOutputStream(System.out));var in=new DataInputStream(new BufferedInputStream(System.in));
            out.writeInt(READY);out.writeInt((int)av.timing.sampleRate);out.writeDouble(av.timing.fps);out.flush();
            while(true){int command;try{command=in.readUnsignedByte();}catch(EOFException e){save();break;}
                if(command==1){buttons=in.readInt()&0xffff;axisX=in.readShort();axisY=in.readShort();sound.reset();core.retro_run();out.writeInt(FRAME);out.writeInt(width);out.writeInt(height);for(int rgb:pixels)out.writeInt(rgb);byte[] pcm=sound.toByteArray();out.writeInt(pcm.length);out.write(pcm);out.flush();}
                else if(command==2){save();out.writeInt(ACK);out.flush();}
                else if(command==3){core.retro_reset();out.writeInt(ACK);out.flush();}
                else if(command==4){save();out.writeInt(ACK);out.flush();break;}
                else throw new IOException("Unknown bridge command");
            }
        }finally{core.retro_unload_game();core.retro_deinit();gameData=null;}
    }
    public static void main(String[] args){try{if(args.length!=5||!Set.of("nes","snes","n64").contains(args[3]))throw new IllegalArgumentException("core, game, save directory, console type, system directory");new LibretroHost(Path.of(args[0]),Path.of(args[2]),args[3],Path.of(args[4])).run(Path.of(args[1]));}catch(Throwable e){e.printStackTrace(System.err);System.exit(1);}}
}
