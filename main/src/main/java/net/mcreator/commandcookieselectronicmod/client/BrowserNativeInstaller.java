package net.mcreator.commandcookieselectronicmod.client;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Extracts only regular engine files beneath a dedicated, staged cache directory. */
final class BrowserNativeInstaller {
 interface Cancel {void check()throws IOException;}
 static void install(Path archive,Path destination,String digest,Cancel cancel)throws IOException {
  var root=destination.getParent().toAbsolutePath().normalize();var temporary=root.resolve("engine-stage-"+UUID.randomUUID());Files.createDirectory(temporary);Path old=null;
  try{
   try(var zip=new ZipFile(archive.toFile())){var entries=zip.stream().filter(e->e.getName().endsWith(".tar.gz")).toList();if(entries.size()!=1)throw new IOException("Invalid browser engine package.");try(var input=new GZIPInputStream(zip.getInputStream(entries.getFirst()))){extract(input,temporary,cancel);}}
   for(String name:List.of("jcef.dll","libcef.dll","jcef_helper.exe","chrome_elf.dll","LICENSE.txt"))if(!Files.isRegularFile(temporary.resolve(name)))throw new IOException("Browser engine package is incomplete.");
   Files.writeString(temporary.resolve("cem-install.json"),digest);cancel.check();
   if(Files.exists(destination,LinkOption.NOFOLLOW_LINKS)){old=root.resolve("engine-old-"+UUID.randomUUID());Files.move(destination,old,StandardCopyOption.ATOMIC_MOVE);}
   try{Files.move(temporary,destination,StandardCopyOption.ATOMIC_MOVE);}catch(IOException failure){if(old!=null&&Files.exists(old))Files.move(old,destination,StandardCopyOption.ATOMIC_MOVE);throw failure;}
   if(old!=null)delete(root,old);
  }finally{if(Files.exists(temporary,LinkOption.NOFOLLOW_LINKS))delete(root,temporary);}
 }
 private static void extract(InputStream input,Path root,Cancel cancel)throws IOException {
  byte[] header=new byte[512],buffer=new byte[65536];long total=0;int entries=0;
  while(true){cancel.check();read(input,header,512);boolean empty=true;for(byte value:header)if(value!=0){empty=false;break;}if(empty)return;
   if(++entries>2048)throw new IOException("Browser engine package has too many files.");
   long checksum=octal(header,148,8),sum=0;for(int i=0;i<header.length;i++)sum+=i>=148&&i<156?32:header[i]&255;if(sum!=checksum)throw new IOException("Invalid browser archive checksum.");
   String name=text(header,0,100),prefix=text(header,345,155);if(!prefix.isEmpty())name=prefix+"/"+name;
   long size=octal(header,124,12);if(size<0||size>1_000_000_000L||(total+=size)>1_500_000_000L)throw new IOException("Browser engine package is too large.");
   int type=header[156];if(type!=0&&type!='0'&&type!='5')throw new IOException("Browser engine package contains unsupported links or entries.");
   if(name.indexOf(':')>=0||name.indexOf('\\')>=0||name.startsWith("/"))throw new IOException("Invalid browser engine path.");
   var target=root.resolve(name).normalize();if(!target.startsWith(root)||target.equals(root)&&type!='5')throw new IOException("Browser archive path leaves its cache.");
   if(type=='5'){Files.createDirectories(target);if(size!=0)throw new IOException("Invalid browser directory.");}
   else {Files.createDirectories(target.getParent());try(var output=Files.newOutputStream(target,StandardOpenOption.CREATE_NEW)){long remaining=size;while(remaining>0){cancel.check();int count=input.read(buffer,0,(int)Math.min(buffer.length,remaining));if(count<0)throw new EOFException("Browser engine archive was truncated.");output.write(buffer,0,count);remaining-=count;}}}
   int padding=(int)((512-size%512)%512);if(padding>0)read(input,header,padding);
  }
 }
 private static String text(byte[] header,int offset,int length){int end=offset;while(end<offset+length&&header[end]!=0)end++;return new String(header,offset,end-offset,StandardCharsets.UTF_8);}
 private static long octal(byte[] header,int offset,int length)throws IOException{String value=text(header,offset,length).strip();try{return value.isEmpty()?0:Long.parseLong(value,8);}catch(NumberFormatException e){throw new IOException("Invalid browser archive size.",e);}}
 private static void read(InputStream input,byte[] bytes,int length)throws IOException{int offset=0;while(offset<length){int count=input.read(bytes,offset,length-offset);if(count<0)throw new EOFException("Truncated browser package.");offset+=count;}}
 private static void delete(Path root,Path path)throws IOException{var target=path.toAbsolutePath().normalize();if(!target.getParent().equals(root)||!target.getFileName().toString().matches("engine-(stage|old)-[0-9a-f-]+"))throw new IOException("Refusing to remove a path outside the browser cache.");if(Files.isSymbolicLink(target)){Files.delete(target);return;}try(var paths=Files.walk(target)){for(var file:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(file);}}
}
