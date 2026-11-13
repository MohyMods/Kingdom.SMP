package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.nbt.*;
/** Bounded chunk-owned provenance. Unknown/corrupt history never becomes natural ore. */
public final class NativeOreMarks {
 public static final int LIMIT=8192;
 private final Map<Long,String> cells=new HashMap<>();private CompoundTag rejected;private boolean overflow;
 public synchronized void mark(long pos,String block){if(rejected!=null||overflow)return;if(!NativeOreQuests.BLOCKS.containsKey(block))return;if(cells.size()>=LIMIT&&!cells.containsKey(pos)){overflow=true;cells.clear();return;}cells.put(pos,block);}
 public synchronized String at(long pos){return rejected!=null||overflow?null:cells.get(pos);}
 public synchronized boolean invalidate(long pos){return cells.remove(pos)!=null;}
 public synchronized Set<Long> positions(){return Set.copyOf(cells.keySet());}
 public synchronized CompoundTag save(){if(rejected!=null)return rejected.copy();var tag=new CompoundTag();tag.putInt("schema",1);tag.putBoolean("overflow",overflow);var rows=new ListTag();cells.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->{var row=new CompoundTag();row.putLong("position",e.getKey());row.putString("block",e.getValue());rows.add(row);});tag.put("ores",rows);return tag;}
 public static NativeOreMarks load(CompoundTag tag){var m=new NativeOreMarks();try{if(!tag.getAllKeys().equals(Set.of("schema","overflow","ores"))||!tag.contains("schema",Tag.TAG_INT)||tag.getInt("schema")!=1||!tag.contains("overflow",Tag.TAG_BYTE)||!(tag.get("ores") instanceof ListTag list)||list.size()>LIMIT||!list.isEmpty()&&list.getElementType()!=Tag.TAG_COMPOUND)throw new IllegalArgumentException("Invalid native ore provenance");m.overflow=tag.getBoolean("overflow");if(m.overflow&&!list.isEmpty())throw new IllegalArgumentException("Overflow marks must be empty");for(var entry:list){var row=(CompoundTag)entry;if(!row.getAllKeys().equals(Set.of("position","block"))||!row.contains("position",Tag.TAG_LONG)||!row.contains("block",Tag.TAG_STRING)||!NativeOreQuests.BLOCKS.containsKey(row.getString("block"))||m.cells.put(row.getLong("position"),row.getString("block"))!=null)throw new IllegalArgumentException("Invalid native ore mark");}}catch(RuntimeException invalid){m.cells.clear();m.rejected=tag.copy();}return m;}
}
