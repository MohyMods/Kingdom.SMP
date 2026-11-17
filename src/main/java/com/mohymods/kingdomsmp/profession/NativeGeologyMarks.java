package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.nbt.*;
/** Separate additive attachment: original ore attachment/schema remains unchanged. */
public final class NativeGeologyMarks {
 private final Map<Long,String> hosts=new HashMap<>();private CompoundTag rejected;private boolean overflow;
 public synchronized String at(long position){return rejected!=null||overflow?null:hosts.get(position);}
 public synchronized void mark(long position,String host){if(rejected!=null||overflow)return;if(!NativeGeologyEvidence.HOSTS.contains(host)){hosts.remove(position);return;}if(hosts.size()>=8192&&!hosts.containsKey(position)){hosts.clear();overflow=true;return;}hosts.put(position,host);}
 public synchronized boolean invalidate(long position){return hosts.remove(position)!=null;}
 public synchronized CompoundTag save(){if(rejected!=null)return rejected.copy();var root=new CompoundTag();root.putInt("schema",1);root.putBoolean("overflow",overflow);var rows=new ListTag();hosts.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->{var row=new CompoundTag();row.putLong("position",e.getKey());row.putString("host",e.getValue());rows.add(row);});root.put("hosts",rows);return root;}
 public static NativeGeologyMarks load(CompoundTag root){var result=new NativeGeologyMarks();try{if(!root.getAllKeys().equals(Set.of("schema","overflow","hosts"))||!root.contains("schema",Tag.TAG_INT)||root.getInt("schema")!=1||!root.contains("overflow",Tag.TAG_BYTE)||!(root.get("hosts") instanceof ListTag rows)||rows.size()>8192||!rows.isEmpty()&&rows.getElementType()!=Tag.TAG_COMPOUND)throw new IllegalArgumentException("Invalid geological host attachment");result.overflow=root.getBoolean("overflow");if(result.overflow&&!rows.isEmpty())throw new IllegalArgumentException("Overflow host attachment must be empty");for(var value:rows){var row=(CompoundTag)value;if(!row.getAllKeys().equals(Set.of("position","host"))||!row.contains("position",Tag.TAG_LONG)||!row.contains("host",Tag.TAG_STRING)||!NativeGeologyEvidence.HOSTS.contains(row.getString("host"))||result.hosts.put(row.getLong("position"),row.getString("host"))!=null)throw new IllegalArgumentException("Invalid original host");}}catch(RuntimeException bad){result.hosts.clear();result.rejected=root.copy();}return result;}
}
