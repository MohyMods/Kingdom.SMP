package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.contribution.ContributionService;
/** Initially activated only after verified coal/iron native feature/removal acceptance. */
public final class NativeOreQuests {
 public static final Map<String,String> BLOCKS=Map.of("minecraft:coal_ore","coal","minecraft:deepslate_coal_ore","coal","minecraft:iron_ore","iron","minecraft:deepslate_iron_ore","iron");
 public static final Map<String,Integer> GOALS=Map.of("kingdom_smp:miner/a01",64,"kingdom_smp:miner/a02",48);
 public static final Set<String> METRICS=Set.of("coal","iron");
 private static final ProfessionQuestCatalog CATALOG=ProfessionQuestCatalog.approved(GOALS.keySet().stream().map(id->ApprovedProfessionContent.get(id).slot()).toList());
 private NativeOreQuests(){}
 public static boolean contains(String id){return GOALS.containsKey(id);}public static ProfessionQuestCatalog catalog(){return CATALOG;}
 public static int count(ServerPlayer p,String id){if(!contains(id))throw new IllegalArgumentException("Unvalidated mining objective");return NativeOreSavedData.get(p.server).evidence().count(p.getUUID(),ApprovedProfessionContent.get(id).rank(),id.endsWith("a01")?"coal":"iron");}
 public static boolean ready(ServerPlayer p,String id){var r=CommerceService.store(p.server).professions(p.getUUID());return contains(id)&&TreeQuestService.real(p)&&r.isActive(Profession.MINER)&&!r.track(Profession.MINER).completedQuestIds().contains(id)&&CATALOG.mayAttempt(Profession.MINER,id,r.track(Profession.MINER).completedQuestIds())&&ContributionService.kingdom(p.server).attainedTier()>=3&&ProfessionService.available(p,Profession.MINER)&&count(p,id)>=NativeOreRules.get(p.server).goal(id);}
 public static String status(ServerPlayer p,String id){return count(p,id)+" / "+NativeOreRules.get(p.server).goal(id)+" personally mined tracked native "+(id.endsWith("a01")?"coal":"iron")+" ore. Explore newly generated Overworld seams and use a correct tool with current site permission. Unknown legacy/placed ores still work normally but grant no retrospective provenance.";}
 public static void complete(ServerPlayer p,UUID op,String id)throws java.io.IOException{ContractService.requireCounter(p,Profession.MINER);if(!ready(p,id))throw new IllegalStateException(status(p,id));CommerceService.store(p.server).completeNativeOreQuest(op,p.getUUID(),id,NativeOreRules.get(p.server).goal(id),count(p,id));}
}
