package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.nbt.*;
/** Global consumed coordinates and personal counters share one saved file, independent of chunk rollback. */
public final class NativeOreEvidence {
 public static final int LIMIT=131072;
 private final Set<String> spent=new HashSet<>();private ProfessionEvidence counts=new ProfessionEvidence(Profession.MINER,NativeOreQuests.METRICS);
 public int count(UUID player,ProfessionRank band,String metric){return counts.count(player,band,metric);}
 public boolean record(UUID player,ProfessionRank band,String metric,String dimension,long position){if(!dimension.equals("minecraft:overworld")||!NativeOreQuests.METRICS.contains(metric))return false;String key=dimension+"@"+position;if(spent.size()>=LIMIT||spent.contains(key))return false;if(!counts.record(player,band,metric,1,null))return false;spent.add(key);return true;}
 public CompoundTag save(){var t=new CompoundTag();t.putInt("schema",1);var seen=new ListTag();spent.stream().sorted().forEach(s->seen.add(StringTag.valueOf(s)));t.put("spent",seen);t.put("counts",counts.save());return t;}
 public static NativeOreEvidence load(CompoundTag tag){if(!tag.getAllKeys().equals(Set.of("schema","spent","counts"))||!tag.contains("schema",Tag.TAG_INT)||tag.getInt("schema")!=1||!(tag.get("spent") instanceof ListTag list)||list.size()>LIMIT||!list.isEmpty()&&list.getElementType()!=Tag.TAG_STRING||!tag.contains("counts",Tag.TAG_COMPOUND))throw new IllegalArgumentException("Invalid native mining evidence");var e=new NativeOreEvidence();e.counts=ProfessionEvidence.load(tag.getCompound("counts"),Profession.MINER,NativeOreQuests.METRICS);for(var v:list){var s=v.getAsString();if(!s.startsWith("minecraft:overworld@")||!e.spent.add(s))throw new IllegalArgumentException("Invalid spent ore coordinate");long coordinate=Long.parseLong(s.substring("minecraft:overworld@".length()));if(!s.equals("minecraft:overworld@"+coordinate))throw new IllegalArgumentException("Noncanonical spent coordinate");}long total=0;for(var value:e.counts.save().getCompound("evidence").getList("players",Tag.TAG_COMPOUND)){var counters=((CompoundTag)value).getCompound("counters");for(String key:counters.getAllKeys())total+=counters.getInt(key);}if(total!=e.spent.size())throw new IllegalArgumentException("Mining counters and spent coordinates disagree");return e;}
}
