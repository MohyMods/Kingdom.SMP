package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import com.mohymods.kingdomsmp.economy.*;
/** Only settled, durable native transactions qualify. No preview, client tally, funding or mail claim is evidence. */
public final class MerchantTransactionQuests {
 public static final String EXCHANGE="kingdom_smp:merchant/a01";
 public static final String DEPOSIT="kingdom_smp:merchant/a02",SELL="kingdom_smp:merchant/a03",BUY="kingdom_smp:merchant/a04",SUPPLY="kingdom_smp:merchant/a05",ORDER="kingdom_smp:merchant/a06";
 private static final Set<String> IDS=Set.of(EXCHANGE,DEPOSIT,SELL,BUY,SUPPLY,ORDER);
 private static final ProfessionQuestCatalog CATALOG=ProfessionQuestCatalog.approved(ApprovedProfessionContent.definitions().values().stream().filter(d->d.profession()==Profession.MERCHANT).map(ApprovedProfessionContent.Definition::slot).toList());
 private MerchantTransactionQuests(){}
 public static Set<String> ids(){return IDS;}
 public static boolean contains(String id){return id!=null&&IDS.contains(id);}
 public static ApprovedProfessionContent.Definition get(String id){if(!contains(id))throw new IllegalArgumentException("No validated Merchant transaction objective");return ApprovedProfessionContent.get(id);}
 public static ProfessionQuestCatalog catalog(){return CATALOG;}
 public static int goal(String id){get(id);return SUPPLY.equals(id)?3:(ORDER.equals(id)||EXCHANGE.equals(id))?1:50;}
 public static String status(ServerPlayer p,String id){get(id);var proof=CommerceService.store(p.server).merchantProof(p.getUUID());int goal=MerchantQuestRules.goal(p.server,id);return switch(id){case EXCHANGE->"Exchange10copper for1silver and10silver for1gold at the real Bank. Recorded equal-value conversion kinds: "+proof.exchanges().size()+" /2.";case DEPOSIT->"Personally deposit and withdraw at least "+goal+" coins through real Bank conversions: "+proof.deposited()+" deposited, "+proof.withdrawn()+" withdrawn.";case SELL->"Settle a fixed-price sale to another player for at least "+goal+" coins; largest qualifying sale: "+proof.largestSale()+".";case BUY->"Buy another player's funded fixed-price listing for at least "+goal+" coins; largest qualifying purchase: "+proof.largestPurchase()+".";case SUPPLY->"Supply "+goal+" distinct Essentials categories while active: "+proof.resources().size()+" / "+goal+". Only actual settled native sales count.";default->"Fill another player's funded buy order through native item consumption: "+(proof.orderFilled()?"recorded":"not yet recorded")+".";};}
 public static boolean ready(ServerPlayer p,String id){if(!contains(id)||!TreeQuestService.real(p))return false;var s=CommerceService.store(p.server);var roster=s.professions(p.getUUID());return roster.isActive(Profession.MERCHANT)&&!roster.track(Profession.MERCHANT).completedQuestIds().contains(id)&&CATALOG.mayAttempt(Profession.MERCHANT,id,roster.track(Profession.MERCHANT).completedQuestIds())&&com.mohymods.kingdomsmp.contribution.ContributionService.kingdom(p.server).attainedTier()>=3&&ProfessionService.available(p,Profession.MERCHANT)&&s.merchantProof(p.getUUID()).ready(id,MerchantQuestRules.goal(p.server,id));}
 public static void complete(ServerPlayer p,UUID operation,String id)throws java.io.IOException{ContractService.requireCounter(p,Profession.MERCHANT);if(!ready(p,id))throw new IllegalStateException(status(p,id));CommerceService.store(p.server).completeMerchantTransactionQuest(operation,p.getUUID(),id,MerchantQuestRules.goal(p.server,id));}
}
