/* Copyright 2026 CommandCookie. SPDX-License-Identifier: GPL-3.0-or-later.
 * Standalone libretro frontend. All callbacks stay in native code: cores may
 * switch coroutine stacks, which must never invoke Java/JNI on that stack.
 * No Minecraft dependencies. Commands/frames match LibretroHost protocol v2.
 */
#define _POSIX_C_SOURCE 200809L
#include "libretro.h"
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <stdbool.h>
#include <string.h>
#include <stdarg.h>
#include <errno.h>
#include <math.h>
#ifdef _WIN32
#include <windows.h>
#include <wchar.h>
#include <io.h>
#include <fcntl.h>
#define dup _dup
#define dup2 _dup2
#define fileno _fileno
#define fdopen _fdopen
static wchar_t *wide(const char *s){int n=MultiByteToWideChar(CP_UTF8,0,s,-1,NULL,0);if(n<=0)return NULL;wchar_t *p=malloc(n*sizeof(*p));if(p)MultiByteToWideChar(CP_UTF8,0,s,-1,p,n);return p;}
/* The save directory includes world, console, ROM and core identities. Use
 * extended absolute paths for every Windows file API: these paths commonly
 * exceed MAX_PATH, even though Java created the directory successfully. */
static wchar_t *file_path(const char *s){
 wchar_t *p=wide(s);if(!p)return NULL;
 for(wchar_t *c=p;*c;c++)if(*c==L'/')*c=L'\\';
 if(!wcsncmp(p,L"\\\\?\\",4))return p;
 size_t n=wcslen(p);
 if(!(n>=3&&p[1]==L':'&&p[2]==L'\\')&&!(n>=2&&p[0]==L'\\'&&p[1]==L'\\')){
  DWORD cap=GetFullPathNameW(p,0,NULL,NULL);if(!cap){free(p);return NULL;}
  wchar_t *full=malloc(cap*sizeof(*full));if(!full){free(p);return NULL;}
  DWORD used=GetFullPathNameW(p,cap,full,NULL);free(p);
  if(!used||used>=cap){free(full);return NULL;}p=full;n=used;
 }
 bool unc=n>=2&&p[0]==L'\\'&&p[1]==L'\\';size_t skip=unc?2:0,prefix=unc?8:4;
 wchar_t *result=malloc((prefix+n-skip+1)*sizeof(*result));
 if(result){memcpy(result,unc?L"\\\\?\\UNC\\":L"\\\\?\\",prefix*sizeof(*result));memcpy(result+prefix,p+skip,(n-skip+1)*sizeof(*result));}
 free(p);return result;
}
static FILE *open_file(const char *s,const char *mode){wchar_t *p=file_path(s),*m=wide(mode);FILE *f=p&&m?_wfopen(p,m):NULL;free(p);free(m);return f;}
static bool replace_file(const char *a,const char *b){wchar_t *x=file_path(a),*y=file_path(b);bool ok=x&&y&&MoveFileExW(x,y,MOVEFILE_REPLACE_EXISTING|MOVEFILE_WRITE_THROUGH);free(x);free(y);return ok;}
static void *open_core(const char *path){wchar_t *p=file_path(path);void *h=p?LoadLibraryW(p):NULL;free(p);return h;}
#define symbol(h,n) (void*)GetProcAddress((HMODULE)(h),(n))
#define close_core(h) FreeLibrary((HMODULE)(h))
#else
#include <dlfcn.h>
#include <unistd.h>
#define open_file fopen
static bool replace_file(const char *a,const char *b){return rename(a,b)==0;}
static void *open_core(const char *path){return dlopen(path,RTLD_NOW|RTLD_LOCAL);}
#define symbol dlsym
#define close_core dlclose
#endif

#define MAX_STATE (64u*1024u*1024u)
#define MAX_PIXELS (2048u*1024u)
static uint32_t pixels[MAX_PIXELS],software_pixels[MAX_PIXELS];
static unsigned width=256,height=240,format=0,buttons=0;
static int16_t axis_x,axis_y;
static uint8_t pcm[65536];static size_t pcm_bytes;
static const char *system_kind,*save_dir,*system_dir;
static char error[512];
typedef struct{char key[128],value[256];} Option;
static Option options[256];static unsigned option_count;
static struct{
 void (*set_environment)(retro_environment_t);
 void (*set_video_refresh)(retro_video_refresh_t);
 void (*set_audio_sample)(retro_audio_sample_t);
 void (*set_audio_sample_batch)(retro_audio_sample_batch_t);
 void (*set_input_poll)(retro_input_poll_t);
 void (*set_input_state)(retro_input_state_t);
 void (*get_system_info)(struct retro_system_info*);
 void (*get_system_av_info)(struct retro_system_av_info*);
 void (*set_controller_port_device)(unsigned,unsigned);
 void (*init)(void);void (*deinit)(void);void (*run)(void);void (*reset)(void);
 bool (*load_game)(const struct retro_game_info*);void (*unload_game)(void);
 size_t (*serialize_size)(void);bool (*serialize)(void*,size_t);bool (*unserialize)(const void*,size_t);
 void* (*get_memory_data)(unsigned);size_t (*get_memory_size)(unsigned);
} core;
#define LOAD(name) do{*(void**)(&core.name)=symbol(library,"retro_"#name);if(!core.name){snprintf(error,sizeof(error),"Missing libretro function: %s",#name);goto cleanup;}}while(0)
static const char *forced(const char *key){
 if(strcmp(system_kind,"n64"))return NULL;
 if(!strcmp(key,"parallel-n64-gfxplugin"))return "angrylion";
 if(!strcmp(key,"parallel-n64-rspplugin"))return "auto";
 if(!strcmp(key,"parallel-n64-alt-map"))return "enabled";
 if(!strcmp(key,"parallel-n64-upscaling"))return "2x";
 if(!strcmp(key,"parallel-n64-angrylion-vioverlay"))return "AA+Dedither";
 if(!strcmp(key,"parallel-n64-angrylion-overscan"))return "disabled";
 if(!strcmp(key,"parallel-n64-cpucore"))return "dynamic_recompiler";
 if(!strcmp(key,"parallel-n64-angrylion-multithread"))return "all threads";
 return NULL;
}
static void logger(enum retro_log_level level,const char *fmt,...){if(level<RETRO_LOG_WARN)return;va_list args;va_start(args,fmt);vfprintf(stderr,fmt,args);va_end(args);}
static bool environment(unsigned command,void *data){
 switch(command&0xffff){
  case RETRO_ENVIRONMENT_GET_CAN_DUPE:*(bool*)data=true;return true;
  case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:*(const char**)data=system_dir;return true;
  case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:*(const char**)data=save_dir;return true;
  case (RETRO_ENVIRONMENT_GET_INPUT_DEVICE_CAPABILITIES&0xffff):*(uint64_t*)data=(1ull<<RETRO_DEVICE_JOYPAD)|(1ull<<RETRO_DEVICE_ANALOG);return true;
  case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:{unsigned f=*(unsigned*)data;if(f>2)return false;format=f;return true;}
  case (RETRO_ENVIRONMENT_GET_CURRENT_SOFTWARE_FRAMEBUFFER&0xffff):{
   struct retro_framebuffer *fb=data;
   if(strcmp(system_kind,"n64")||format!=RETRO_PIXEL_FORMAT_XRGB8888||!fb->width||!fb->height||fb->width>2048||fb->height>1024)return false;
   fb->data=software_pixels;fb->pitch=fb->width*4;fb->format=RETRO_PIXEL_FORMAT_XRGB8888;fb->memory_flags=RETRO_MEMORY_TYPE_CACHED;return true;
  }
  case RETRO_ENVIRONMENT_GET_VARIABLE:{struct retro_variable *v=data;v->value=forced(v->key);if(v->value)return true;for(unsigned i=0;i<option_count;i++)if(!strcmp(v->key,options[i].key)){v->value=options[i].value;return true;}return false;}
  case RETRO_ENVIRONMENT_SET_VARIABLES:{const struct retro_variable *v=data;option_count=0;for(;v&&v->key&&option_count<256;v++){const char *start=v->value?strchr(v->value,';'):NULL;if(!start)continue;start++;while(*start==' ')start++;size_t n=strcspn(start,"|");Option *o=&options[option_count++];snprintf(o->key,sizeof(o->key),"%s",v->key);if(n>=sizeof(o->value))n=sizeof(o->value)-1;memcpy(o->value,start,n);o->value[n]=0;}return true;}
  case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:*(bool*)data=false;return true;
  case RETRO_ENVIRONMENT_GET_LANGUAGE:*(unsigned*)data=RETRO_LANGUAGE_ENGLISH;return true;
  case (RETRO_ENVIRONMENT_GET_FASTFORWARDING&0xffff):*(bool*)data=false;return true;
  case RETRO_ENVIRONMENT_GET_LOG_INTERFACE:((struct retro_log_callback*)data)->log=logger;return true;
  case (RETRO_ENVIRONMENT_GET_INPUT_BITMASKS&0xffff):return true;
  case RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION:*(unsigned*)data=0;return true;
  case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS:
  case RETRO_ENVIRONMENT_SET_SUPPORT_NO_GAME:
  case RETRO_ENVIRONMENT_SET_CONTROLLER_INFO:
  case RETRO_ENVIRONMENT_SET_GEOMETRY:return true;
  default:return false;
 }
}
static uint32_t color(const uint8_t *row,unsigned x){
 if(format==1){uint32_t c;memcpy(&c,row+x*4,4);return c&0xffffff;}
 uint16_t v;memcpy(&v,row+x*2,2);unsigned r=format==2?(v>>11)&31:(v>>10)&31,g=format==2?(v>>5)&63:(v>>5)&31,b=v&31;return (r*255/31<<16)|(g*255/(format==2?63:31)<<8)|b*255/31;
}
static void video(const void *source,unsigned w,unsigned h,size_t pitch){
 if(!source||source==RETRO_HW_FRAME_BUFFER_VALID||w<1||h<1||w>2048||h>1024||pitch>16384||pitch<w*(format==1?4:2))return;
 unsigned xborder=0,yborder=0,step=1;
 if(!strcmp(system_kind,"n64")){
  /* Keep a fixed TV-safe viewport. The renderer's variable overscan crop
   * still exposes border noise and can resize the texture frame by frame. */
  if(w>=128)xborder=w/80;if(h>=128)yborder=(h+29)/30;
  /* The filtered VI doubles horizontal samples for low-resolution games.
   * Average those sample pairs before IPC; retain genuine high-res modes. */
  if(w>=1024&&w>h*2)step=2;
 }
 width=(w-xborder*2)/step;height=h-yborder*2;
 for(unsigned y=0;y<height;y++){
  const uint8_t *row=(const uint8_t*)source+(y+yborder)*pitch;
  for(unsigned x=0;x<width;x++){
   unsigned index=xborder+x*step;uint32_t c=color(row,index);
   if(step==2){uint32_t b=color(row,index+1);c=((c&0xfefefe)>>1)+((b&0xfefefe)>>1)+(c&b&0x010101);}
   pixels[y*width+x]=0xff000000|c;
  }
 }
}
static void audio(int16_t left,int16_t right){if(pcm_bytes+4>sizeof(pcm))return;pcm[pcm_bytes++]=left&255;pcm[pcm_bytes++]=((uint16_t)left>>8)&255;pcm[pcm_bytes++]=right&255;pcm[pcm_bytes++]=((uint16_t)right>>8)&255;}
static size_t audio_batch(const int16_t *data,size_t frames){size_t n=frames;if(n>(sizeof(pcm)-pcm_bytes)/4)n=(sizeof(pcm)-pcm_bytes)/4;for(size_t i=0;i<n;i++)audio(data[i*2],data[i*2+1]);return frames;}
static void poll(void){}
static int16_t input(unsigned port,unsigned device,unsigned index,unsigned id){if(port)return 0;device&=255;if(device==RETRO_DEVICE_JOYPAD)return id==RETRO_DEVICE_ID_JOYPAD_MASK?(int16_t)buttons:id<16?(buttons>>id)&1:0;if(device==RETRO_DEVICE_ANALOG&&index==RETRO_DEVICE_INDEX_ANALOG_LEFT)return id==0?axis_x:id==1?axis_y:0;return 0;}
static bool path(char *out,size_t cap,const char *file){return snprintf(out,cap,"%s/%s",save_dir,file)>0&&strlen(save_dir)+strlen(file)+2<cap;}
static bool atomic_save(const char *name,const void *data,size_t n){char file[4096],temp[4096],shortname[100];snprintf(shortname,sizeof(shortname),"%s.tmp",name);if(!path(file,sizeof(file),name)||!path(temp,sizeof(temp),shortname))return false;FILE *f=open_file(temp,"wb");if(!f)return false;bool ok=fwrite(data,1,n,f)==n;ok=fclose(f)==0&&ok;return ok&&replace_file(temp,file);}
static bool save(void){
 size_t n=core.serialize_size();if(n&&n<=MAX_STATE){void *data=malloc(n);if(!data)return false;bool ok=core.serialize(data,n)&&atomic_save("state.bin",data,n);free(data);if(!ok)return false;}
 n=core.get_memory_size(RETRO_MEMORY_SAVE_RAM);void *data=core.get_memory_data(RETRO_MEMORY_SAVE_RAM);return !data||!n||n>MAX_STATE||atomic_save("sram.bin",data,n);
}
static bool read_save(const char *name,void *data,size_t n){char file[4096];if(!path(file,sizeof(file),name))return false;FILE *f=open_file(file,"rb");if(!f)return false;if(fseek(f,0,SEEK_END)||ftell(f)!=(long)n){fclose(f);return false;}rewind(f);bool ok=fread(data,1,n,f)==n;fclose(f);return ok;}
static void restore(void){
 size_t n=core.get_memory_size(RETRO_MEMORY_SAVE_RAM);void *data=core.get_memory_data(RETRO_MEMORY_SAVE_RAM);if(data&&n&&n<=MAX_STATE)read_save("sram.bin",data,n);
 n=core.serialize_size();if(n&&n<=MAX_STATE){void *state=malloc(n);if(state){if(read_save("state.bin",state,n)&&!core.unserialize(state,n))fprintf(stderr,"Existing state was rejected by the emulator; starting normally.\n");free(state);}}
}
static bool read_num(FILE *in,unsigned bytes,uint64_t *v){*v=0;for(unsigned i=0;i<bytes;i++){int c=fgetc(in);if(c==EOF)return false;*v=(*v<<8)|(unsigned char)c;}return true;}
static void write_num(FILE *out,uint64_t v,unsigned bytes){for(unsigned i=0;i<bytes;i++)fputc((v>>(8*(bytes-1-i)))&255,out);}
static bool frame(FILE *out){write_num(out,0x46524d31,4);write_num(out,width,4);write_num(out,height,4);static uint8_t bytes[MAX_PIXELS*4];for(size_t i=0;i<(size_t)width*height;i++){uint32_t c=pixels[i];bytes[i*4]=c>>24;bytes[i*4+1]=c>>16;bytes[i*4+2]=c>>8;bytes[i*4+3]=c;}if(fwrite(bytes,4,(size_t)width*height,out)!=(size_t)width*height)return false;write_num(out,pcm_bytes,4);return fwrite(pcm,1,pcm_bytes,out)==pcm_bytes&&fflush(out)==0;}
static int run_host(int argc,char **argv){
 if(argc!=6){fprintf(stderr,"core, ROM, save directory, system, system directory required\n");return 2;}
 system_kind=argv[4];save_dir=argv[3];system_dir=argv[5];if(strcmp(system_kind,"snes")&&strcmp(system_kind,"n64")&&strcmp(system_kind,"nes"))return 2;
 void *library=NULL,*rom_data=NULL;bool initialized=false,loaded=false;int result=1;FILE *protocol=NULL,*rom=NULL;
#ifdef _WIN32
 _setmode(_fileno(stdin),_O_BINARY);_setmode(_fileno(stdout),_O_BINARY);_setmode(_fileno(stderr),_O_BINARY);
#endif
 protocol=fdopen(dup(fileno(stdout)),"wb");if(!protocol)return 1;dup2(fileno(stderr),fileno(stdout));
 library=open_core(argv[1]);if(!library){snprintf(error,sizeof(error),"Could not load the native core for this platform.");goto cleanup;}
 LOAD(set_environment);LOAD(set_video_refresh);LOAD(set_audio_sample);LOAD(set_audio_sample_batch);LOAD(set_input_poll);LOAD(set_input_state);LOAD(get_system_info);LOAD(get_system_av_info);LOAD(set_controller_port_device);LOAD(init);LOAD(deinit);LOAD(run);LOAD(reset);LOAD(load_game);LOAD(unload_game);LOAD(serialize_size);LOAD(serialize);LOAD(unserialize);LOAD(get_memory_data);LOAD(get_memory_size);
 rom=open_file(argv[2],"rb");if(!rom||fseek(rom,0,SEEK_END)){snprintf(error,sizeof(error),"Could not read local game.");goto cleanup;}long size=ftell(rom);rewind(rom);long minimum=!strcmp(system_kind,"n64")?4096:!strcmp(system_kind,"snes")?32768:16,cap=!strcmp(system_kind,"n64")?128*1024*1024:16*1024*1024;
 uint8_t header[16];if(size<minimum||size>cap||fread(header,1,16,rom)!=16){snprintf(error,sizeof(error),"Invalid local ROM size.");goto cleanup;}
 uint32_t signature=(uint32_t)header[0]<<24|(uint32_t)header[1]<<16|(uint32_t)header[2]<<8|header[3];
 if((!strcmp(system_kind,"nes")&&signature!=0x4e45531a)||(!strcmp(system_kind,"n64")&&signature!=0x80371240&&signature!=0x37804012&&signature!=0x40123780)||(!strcmp(system_kind,"snes")&&size%1024!=0&&size%1024!=512)){snprintf(error,sizeof(error),"Invalid ROM format for this console.");goto cleanup;}
 core.set_environment(environment);core.init();initialized=true;core.set_video_refresh(video);core.set_audio_sample(audio);core.set_audio_sample_batch(audio_batch);core.set_input_poll(poll);core.set_input_state(input);
 struct retro_system_info info={0};core.get_system_info(&info);struct retro_game_info game={0};game.path=argv[2];
 if(!info.need_fullpath){rom_data=malloc(size);if(!rom_data){snprintf(error,sizeof(error),"Not enough memory for ROM.");goto cleanup;}rewind(rom);if(fread(rom_data,1,size,rom)!=(size_t)size){snprintf(error,sizeof(error),"ROM changed while reading.");goto cleanup;}game.data=rom_data;game.size=size;}
 fclose(rom);rom=NULL;if(!core.load_game(&game)){snprintf(error,sizeof(error),"The %s core rejected this ROM.",system_kind);goto cleanup;}loaded=true;core.set_controller_port_device(0,RETRO_DEVICE_JOYPAD);
 if(!strcmp(system_kind,"n64"))core.run();pcm_bytes=0;restore();
 struct retro_system_av_info av={0};core.get_system_av_info(&av);if(!isfinite(av.timing.fps)||av.timing.fps<20||av.timing.fps>120||av.timing.sample_rate<8000||av.timing.sample_rate>192000){snprintf(error,sizeof(error),"Invalid emulator timing.");goto cleanup;}
 fprintf(stderr,"%s %s; %s; %.6f FPS / %.0f Hz\n",info.library_name?info.library_name:"Core",info.library_version?info.library_version:"",system_kind,av.timing.fps,av.timing.sample_rate);
 write_num(protocol,0x43455232,4);write_num(protocol,(unsigned)av.timing.sample_rate,4);union{double d;uint64_t u;} fps={.d=av.timing.fps};write_num(protocol,fps.u,8);if(fflush(protocol))goto cleanup;
 for(;;){uint64_t command,a,b,c;if(!read_num(stdin,1,&command)){result=save()?0:1;break;}
  if(command==1){if(!read_num(stdin,4,&a)||!read_num(stdin,2,&b)||!read_num(stdin,2,&c))break;buttons=a&0xffff;axis_x=(int16_t)b;axis_y=(int16_t)c;pcm_bytes=0;core.run();if(!frame(protocol))break;}
  else if(command==2||command==4){if(!save()){snprintf(error,sizeof(error),"Unable to save emulator progress.");break;}write_num(protocol,0x41434b31,4);if(fflush(protocol))break;if(command==4){result=0;break;}}
  else if(command==3){core.reset();write_num(protocol,0x41434b31,4);if(fflush(protocol))break;}
  else{snprintf(error,sizeof(error),"Invalid helper command.");break;}
 }
cleanup:
 if(rom)fclose(rom);if(loaded)core.unload_game();if(initialized)core.deinit();if(library)close_core(library);free(rom_data);if(protocol)fclose(protocol);if(result&&error[0])fprintf(stderr,"%s\n",error);return result;
}
#ifdef _WIN32
int wmain(int argc,wchar_t **args){char **argv=calloc(argc,sizeof(*argv));if(!argv)return 1;for(int i=0;i<argc;i++){int n=WideCharToMultiByte(CP_UTF8,0,args[i],-1,NULL,0,NULL,NULL);argv[i]=malloc(n);if(!argv[i])return 1;WideCharToMultiByte(CP_UTF8,0,args[i],-1,argv[i],n,NULL,NULL);}int code=run_host(argc,argv);for(int i=0;i<argc;i++)free(argv[i]);free(argv);return code;}
#else
int main(int argc,char **argv){return run_host(argc,argv);}
#endif
