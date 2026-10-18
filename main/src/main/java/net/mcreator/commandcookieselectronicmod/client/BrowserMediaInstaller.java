package net.mcreator.commandcookieselectronicmod.client;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Stages verified media-engine files before replacing the dedicated cache. */
final class BrowserMediaInstaller {
 static void install(Path archive,Path destination,String digest,BrowserNativeInstaller.Cancel cancel)throws IOException {
  var root=destination.getParent().toAbsolutePath().normalize();
  var temporary=root.resolve("media-stage-"+UUID.randomUUID());Files.createDirectory(temporary);Path old=null;
  try {
   long total=0;int count=0;var names=new HashSet<String>();byte[] buffer=new byte[65536];
   try(var zip=new ZipFile(archive.toFile())) {
    var entries=zip.entries();while(entries.hasMoreElements()) {
     cancel.check();var entry=entries.nextElement();String name=entry.getName();
     if(++count>2048||name.isEmpty()||name.indexOf(':')>=0||name.indexOf('\\')>=0||name.startsWith("/")||!names.add(name.toLowerCase(Locale.ROOT)))throw new IOException("Invalid media browser package.");
     var target=temporary.resolve(name).normalize();
     if(!target.startsWith(temporary)||target.equals(temporary))throw new IOException("Media browser archive path leaves its cache.");
     if(entry.isDirectory()){Files.createDirectories(target);continue;}
     long size=entry.getSize();if(size<0||size>500_000_000L||(total+=size)>900_000_000L)throw new IOException("Media browser package is too large.");
     Files.createDirectories(target.getParent());long written=0;var crc=new CRC32();
     try(var input=zip.getInputStream(entry);var output=Files.newOutputStream(target,StandardOpenOption.CREATE_NEW)) {
      int n;while((n=input.read(buffer))!=-1){cancel.check();written+=n;if(written>size)throw new IOException("Invalid media browser file size.");crc.update(buffer,0,n);output.write(buffer,0,n);}
     }
     if(written!=size||crc.getValue()!=entry.getCrc())throw new IOException("Media browser archive checksum failed.");
    }
   }
   for(String name:List.of("electron.exe","electron.exe.sig","ffmpeg.dll","icudtl.dat","LICENSE","LICENSES.chromium.html"))if(!Files.isRegularFile(temporary.resolve(name)))throw new IOException("Media browser package is incomplete.");
   Files.writeString(temporary.resolve("cem-install.json"),digest);cancel.check();
   if(Files.exists(destination,LinkOption.NOFOLLOW_LINKS)){old=root.resolve("media-old-"+UUID.randomUUID());Files.move(destination,old,StandardCopyOption.ATOMIC_MOVE);}
   try{Files.move(temporary,destination,StandardCopyOption.ATOMIC_MOVE);}catch(IOException failure){if(old!=null&&Files.exists(old))Files.move(old,destination,StandardCopyOption.ATOMIC_MOVE);throw failure;}
   if(old!=null)delete(root,old);
  }finally{if(Files.exists(temporary,LinkOption.NOFOLLOW_LINKS))delete(root,temporary);}
 }
 private static void delete(Path root,Path path)throws IOException {
  var target=path.toAbsolutePath().normalize();
  if(!target.getParent().equals(root)||!target.getFileName().toString().matches("media-(stage|old)-[0-9a-f-]+"))throw new IOException("Refusing to remove a path outside the media browser cache.");
  if(Files.isSymbolicLink(target)){Files.delete(target);return;}
  try(var paths=Files.walk(target)){for(var file:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(file);}
 }
}
