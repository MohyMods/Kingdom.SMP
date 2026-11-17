package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
/** Actual maintained native-planted forest, current apiary and the earned personal renewal history. */
public final class ForestReserveInspection {
 public static final String RESERVE="kingdom_smp:lumberjack/m07",FINAL="kingdom_smp:lumberjack/m08";
 private ForestReserveInspection(){}
 public static boolean contains(String id){return RESERVE.equals(id)||FINAL.equals(id);}
 static String summarize(Map<String,String> logs,Set<String> species,Set<String> grown,int renewals){if(species.size()<2||grown.size()<6||renewals<1)throw new IllegalStateException("Maintain two genuine native-planted tree species and your recorded six-species/renewal history; placed logs or unknown old trees cannot substitute");var facts=new TreeMap<>(logs);String text=facts+"|"+new TreeSet<>(species)+"|"+new TreeSet<>(grown)+"|"+renewals;try{return "reserve="+java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)),0,16)+",species="+species.size();}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
 public static String validate(ServerPlayer p,BlockPos center){
  String apiary=ApiaryInspection.validate(p,center);var level=p.serverLevel();var forest=TreeSavedData.get(p.server).evidence();var logs=new TreeMap<String,String>();Set<String> species=new HashSet<>(),grown=new HashSet<>();
  for(var entry:forest.plantedNear(level.dimension().location().toString(),center).entrySet()){var pos=BlockPos.of(entry.getKey().position());if(Math.abs((long)pos.getX()-center.getX())>8||Math.abs((long)pos.getZ()-center.getZ())>8||pos.getY()<center.getY()-3||pos.getY()>center.getY()+10)continue;if(!level.hasChunkAt(pos))throw new IllegalStateException("Visit the entire maintained woodland; inspection never loads chunks");if(!level.getBlockState(pos).toString().equals(entry.getValue().state()))continue;species.add(entry.getValue().species());logs.put(Long.toString(pos.asLong()),entry.getValue().state()+":"+entry.getValue().species());}
  var personal=forest.evidence();for(var kind:TreeEvidence.SPECIES)if(personal.count(p.getUUID(),ProfessionRank.MASTER,"mature_"+kind)>0)grown.add(kind);
  logs.put("apiary",apiary);return summarize(logs,species,grown,personal.count(p.getUUID(),ProfessionRank.MASTER,"renewal_cycles"));
 }
}
