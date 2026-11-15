package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.nbt.*;
/** Separate Nether boundary; original Overworld counters, areas and spent format are untouched. */
public final class NetherOreEvidence {
 public static final int LIMIT=131072;
 private final Set<Long> spent=new HashSet<>();private ProfessionEvidence counts=new ProfessionEvidence(Profession.MINER,Set.copyOf(NetherOres.BLOCKS.values()));
 public int count(UUID player,String metric){return counts.count(player,ProfessionRank.MASTER,metric);}
 public boolean record(UUID player,String dimension,long pos,String metric){if(!dimension.equals("minecraft:the_nether")||!NetherOres.BLOCKS.containsValue(metric)||spent.size()>=LIMIT||spent.contains(pos))return false;if(!counts.record(player,ProfessionRank.MASTER,metric,1,null))return false;spent.add(pos);return true;}
 public CompoundTag save(){var root=new CompoundTag();root.putInt("schema",1);var rows=new ListTag();spent.stream().sorted().forEach(n->rows.add(LongTag.valueOf(n)));root.put("spent",rows);root.put("counts",counts.save());return root;}
 public static NetherOreEvidence load(CompoundTag root){if(!root.getAllKeys().equals(Set.of("schema","spent","counts"))||!root.contains("schema",Tag.TAG_INT)||root.getInt("schema")!=1||!(root.get("spent") instanceof ListTag rows)||rows.size()>LIMIT||!rows.isEmpty()&&rows.getElementType()!=Tag.TAG_LONG||!root.contains("counts",Tag.TAG_COMPOUND))throw new IllegalArgumentException("Invalid Nether ore history");var result=new NetherOreEvidence();result.counts=ProfessionEvidence.load(root.getCompound("counts"),Profession.MINER,Set.copyOf(NetherOres.BLOCKS.values()));for(var value:rows)if(!result.spent.add(((LongTag)value).getAsLong()))throw new IllegalArgumentException("Duplicate spent Nether position");long total=0;for(var player:result.counts.save().getCompound("evidence").getList("players",Tag.TAG_COMPOUND)){var values=((CompoundTag)player).getCompound("counters");for(String metric:values.getAllKeys()){if(!metric.startsWith("master/"))throw new IllegalArgumentException("Invalid Nether mining band");total+=values.getInt(metric);}}if(total!=result.spent.size())throw new IllegalArgumentException("Nether counts and spent positions disagree");return result;}
}
