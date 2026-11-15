package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.contribution.ContributionService;
/** Validated Overworld native generation/removal objectives; metadata closure does not activate adapters. */
public final class NativeOreQuests {
 public static final Map<String,String> BLOCKS=blocks();
 public static final Map<String,Integer> GOALS=Map.of("kingdom_smp:miner/a01",64,"kingdom_smp:miner/a02",48,"kingdom_smp:miner/a03",64,"kingdom_smp:miner/j01",48,"kingdom_smp:miner/j02",24,"kingdom_smp:miner/j03",24,"kingdom_smp:miner/j04",8);
 private static final Map<String,String> QUEST_METRICS=Map.of("kingdom_smp:miner/a01","coal","kingdom_smp:miner/a02","iron","kingdom_smp:miner/a03","copper","kingdom_smp:miner/j01","redstone","kingdom_smp:miner/j02","lapis","kingdom_smp:miner/j03","gold","kingdom_smp:miner/j04","diamond");
 public static final Set<String> METRICS=Set.copyOf(QUEST_METRICS.values());
 private static Map<String,String> blocks(){var result=new HashMap<String,String>();for(String metric:List.of("coal","iron","copper","redstone","lapis","gold","diamond"))for(String prefix:List.of("minecraft:","minecraft:deepslate_"))result.put(prefix+metric+"_ore",metric);return Map.copyOf(result);}
 private static final ProfessionQuestCatalog CATALOG=ProfessionQuestCatalog.approved(ApprovedProfessionContent.definitions().values().stream().filter(d->d.profession()==Profession.MINER&&(d.rank()==ProfessionRank.APPRENTICE||GOALS.containsKey(d.id()))).map(ApprovedProfessionContent.Definition::slot).toList());
 public static boolean eligible(String metric,ProfessionRank band,int tier){return QUEST_METRICS.entrySet().stream().anyMatch(e->{var d=ApprovedProfessionContent.get(e.getKey());return e.getValue().equals(metric)&&d.rank()==band&&tier>=d.minimumTier();});}
 public static String metric(String id){if(!contains(id))throw new IllegalArgumentException("Unvalidated mining objective");return QUEST_METRICS.get(id);}
 private NativeOreQuests(){}
 public static boolean contains(String id){return GOALS.containsKey(id);}public static ProfessionQuestCatalog catalog(){return CATALOG;}
 public static int count(ServerPlayer p,String id){if(!contains(id))throw new IllegalArgumentException("Unvalidated mining objective");if(id.endsWith("a03"))return NativeOreSavedData.get(p.server).evidence().copper(p.getUUID(),ProfessionRank.APPRENTICE,NativeOreRules.get(p.server).separation());return NativeOreSavedData.get(p.server).evidence().count(p.getUUID(),ApprovedProfessionContent.get(id).rank(),metric(id));}
 public static boolean ready(ServerPlayer p,String id){var r=CommerceService.store(p.server).professions(p.getUUID());return contains(id)&&TreeQuestService.real(p)&&r.isActive(Profession.MINER)&&!r.track(Profession.MINER).completedQuestIds().contains(id)&&CATALOG.mayAttempt(Profession.MINER,id,r.track(Profession.MINER).completedQuestIds())&&ContributionService.kingdom(p.server).attainedTier()>=ApprovedProfessionContent.get(id).minimumTier()&&ProfessionService.available(p,Profession.MINER)&&count(p,id)>=NativeOreRules.get(p.server).goal(id);}
 public static String status(ServerPlayer p,String id){if(id.endsWith("a03"))return count(p,id)+" / "+NativeOreRules.get(p.server).goal(id)+" native copper from separate working areas. Mine copper at least "+NativeOreRules.get(p.server).separation()+" chunks from your tracked coal/iron excavation areas. Mine at least one tracked coal/iron seam to establish a personal reference area; legacy location ownership is not guessed. Copper can be extracted first; areas are checked when reviewed.";return count(p,id)+" / "+NativeOreRules.get(p.server).goal(id)+" personally mined tracked native "+(metric(id))+" ore. Explore newly generated Overworld seams and use a correct tool with current site permission. Unknown legacy/placed ores still work normally but grant no retrospective provenance.";}
 public static void complete(ServerPlayer p,UUID op,String id)throws java.io.IOException{ContractService.requireCounter(p,Profession.MINER);if(!ready(p,id))throw new IllegalStateException(status(p,id));CommerceService.store(p.server).completeNativeOreQuest(op,p.getUUID(),id,NativeOreRules.get(p.server).goal(id),count(p,id));}
}
