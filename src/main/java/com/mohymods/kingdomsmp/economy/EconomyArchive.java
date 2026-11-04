package com.mohymods.kingdomsmp.economy;

import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import net.minecraft.nbt.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Immutable closed journal segments. Only the atomically published main journal selects authoritative segments. */
final class EconomyArchive {
    static final int MAX_SEGMENTS=256,MAX_SEGMENT_OPERATIONS=4096,MAX_SEGMENT_BYTES=4*1024*1024,MAX_TOTAL_OPERATIONS=1_000_000;
    record Segment(String file,String sha256,long first,long last){
        Segment {
            if(file==null||!file.matches("segment-[1-9][0-9]*-[1-9][0-9]*-[0-9a-f]{32}\\.dat")||sha256==null||!sha256.matches("[0-9a-f]{64}")||first<1||last<first||last-first>=MAX_SEGMENT_OPERATIONS||last>MAX_TOTAL_OPERATIONS||!file.equals("segment-"+first+"-"+last+"-"+sha256.substring(0,32)+".dat"))throw new IllegalArgumentException("Invalid economy archive descriptor");
        }
    }
    private EconomyArchive(){}
    static Path directory(Path data){return data.resolve("kingdomsmp_economy_archive");}
    static ListTag descriptors(List<Segment> segments){
        var rows=new ListTag();for(var s:segments){var tag=new CompoundTag();tag.putString("file",s.file());tag.putString("sha256",s.sha256());tag.putLong("first",s.first());tag.putLong("last",s.last());rows.add(tag);}return rows;
    }
    static List<Segment> descriptors(CompoundTag journal){
        if(!(journal.get("archives") instanceof ListTag list)||list.isEmpty()||list.size()>MAX_SEGMENTS||list.getElementType()!=Tag.TAG_COMPOUND)throw new IllegalArgumentException("Invalid economy archive manifest");
        List<Segment> result=new ArrayList<>();long expected=1;Set<String> files=new HashSet<>();
        for(var value:list){var row=(CompoundTag)value;fields(row,Set.of("file","sha256","first","last"));typed(row,"file",Tag.TAG_STRING);typed(row,"sha256",Tag.TAG_STRING);typed(row,"first",Tag.TAG_LONG);typed(row,"last",Tag.TAG_LONG);var s=new Segment(row.getString("file"),row.getString("sha256"),row.getLong("first"),row.getLong("last"));if(s.first()!=expected||!files.add(s.file()))throw new IllegalArgumentException("Unordered economy archive manifest");result.add(s);expected=s.last()+1;}
        return List.copyOf(result);
    }
    static List<CompoundTag> read(Path data,UUID identity,List<Segment> segments)throws IOException{
        Path dir=directory(data);if(!Files.isDirectory(dir,LinkOption.NOFOLLOW_LINKS))throw new IOException("Missing or linked economy archive directory");List<CompoundTag> result=new ArrayList<>();String previous="";long expected=1;
        for(var segment:segments){
            if(segment.first()!=expected)throw new IOException("Economy archive sequence gap");Path file=dir.resolve(segment.file());if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)||Files.size(file)>MAX_SEGMENT_BYTES+8192||!hash(file).equals(segment.sha256()))throw new IOException("Missing, changed or oversized economy archive: "+segment.file());
            var tag=AtomicNbt.read(file);fields(tag,Set.of("schema","identity","first","last","previous","actor","reason","createdAt","history"));typed(tag,"schema",Tag.TAG_INT);typed(tag,"first",Tag.TAG_LONG);typed(tag,"last",Tag.TAG_LONG);typed(tag,"previous",Tag.TAG_STRING);typed(tag,"reason",Tag.TAG_STRING);typed(tag,"createdAt",Tag.TAG_LONG);
            if(tag.getInt("schema")!=1||!tag.hasUUID("identity")||!tag.getUUID("identity").equals(identity)||!tag.hasUUID("actor")||tag.getLong("first")!=segment.first()||tag.getLong("last")!=segment.last()||!tag.getString("previous").equals(previous)||tag.getLong("createdAt")<0||encodedBytes(tag)>MAX_SEGMENT_BYTES)throw new IOException("Economy archive identity, order or chain mismatch");validateReason(tag.getString("reason"));
            if(!(tag.get("history") instanceof ListTag rows)||rows.getElementType()!=Tag.TAG_COMPOUND||rows.size()!=segment.last()-segment.first()+1)throw new IOException("Economy archive record count mismatch");
            for(var value:rows){var row=(CompoundTag)value;if(!row.contains("sequence",Tag.TAG_LONG)||row.getLong("sequence")!=expected++)throw new IOException("Economy archive record sequence mismatch");result.add(row.copy());}previous=segment.sha256();
        }
        return List.copyOf(result);
    }
    static Segment seal(Path data,UUID identity,long first,String previous,ListTag rows,UUID actor,String reason)throws IOException{
        validateReason(reason);Objects.requireNonNull(actor);if(rows.isEmpty()||rows.size()>MAX_SEGMENT_OPERATIONS||rows.getElementType()!=Tag.TAG_COMPOUND||!previous.isEmpty()&&!previous.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Invalid closed economy segment");
        long last=Math.addExact(first,rows.size()-1L);var tag=new CompoundTag();tag.putInt("schema",1);tag.putUUID("identity",identity);tag.putLong("first",first);tag.putLong("last",last);tag.putString("previous",previous);tag.putUUID("actor",actor);tag.putString("reason",reason);tag.putLong("createdAt",System.currentTimeMillis());tag.put("history",rows.copy());
        if(first<1||last>MAX_TOTAL_OPERATIONS||encodedBytes(tag)>MAX_SEGMENT_BYTES)throw new IllegalArgumentException("Closed economy segment exceeds its bounds");
        Path dir=directory(data);Files.createDirectories(dir);if(!Files.isDirectory(dir,LinkOption.NOFOLLOW_LINKS))throw new IOException("Linked or invalid economy archive directory");Path temporary=Files.createTempFile(dir,".closing-",".tmp");
        try{
            AtomicNbt.write(temporary,tag);String digest=hash(temporary);String name="segment-"+first+"-"+last+"-"+digest.substring(0,32)+".dat";Path target=dir.resolve(name);
            try{Files.createLink(target,temporary);}catch(FileAlreadyExistsException raced){if(!Files.isRegularFile(target,LinkOption.NOFOLLOW_LINKS)||!hash(target).equals(digest))throw new IOException("Economy archive publication collision",raced);}
            if(!System.getProperty("os.name").startsWith("Windows"))try(var channel=FileChannel.open(dir,StandardOpenOption.READ)){channel.force(true);}
            return new Segment(name,digest,first,last);
        }finally{Files.deleteIfExists(temporary);}
    }
    static int encodedBytes(CompoundTag tag){try{var output=new ByteArrayOutputStream();NbtIo.write(tag,new DataOutputStream(output));return output.size();}catch(IOException e){throw new IllegalArgumentException("Invalid archive serialization",e);}}
    private static String hash(Path file)throws IOException{try{var hash=MessageDigest.getInstance("SHA-256");try(var input=Files.newInputStream(file)){byte[] bytes=new byte[8192];for(int count;(count=input.read(bytes))!=-1;)hash.update(bytes,0,count);}return HexFormat.of().formatHex(hash.digest());}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    static void validateReason(String value){if(value==null||value.isBlank()||value.length()>128||value.codePoints().anyMatch(c->Character.isISOControl(c)||c=='§'))throw new IllegalArgumentException("Invalid archive audit reason");}
    private static void typed(CompoundTag tag,String key,int type){if(!tag.contains(key,type))throw new IllegalArgumentException("Invalid economy archive field: "+key);}
    private static void fields(CompoundTag tag,Set<String> keys){if(!tag.getAllKeys().equals(keys))throw new IllegalArgumentException("Invalid economy archive fields");}
}
