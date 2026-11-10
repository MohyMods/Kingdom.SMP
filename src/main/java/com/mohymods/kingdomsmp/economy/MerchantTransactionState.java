package com.mohymods.kingdomsmp.economy;
import java.util.*;
import com.mohymods.kingdomsmp.profession.*;
/** Immutable bounded projection rebuilt from the canonical journal, including closed archive segments. */
public final class MerchantTransactionState {
 public record Proof(long deposited,long withdrawn,long largestSale,long largestPurchase,Set<String> resources,boolean orderFilled,Set<String> exchanges){
  public Proof{resources=Set.copyOf(resources);exchanges=Set.copyOf(exchanges);}
  public boolean ready(String id){return ready(id,MerchantTransactionQuests.goal(id));}
  public boolean ready(String id,int goal){MerchantQuestRules.validate(id,goal);return switch(id){case MerchantTransactionQuests.EXCHANGE->exchanges.containsAll(Set.of("copper:silver","silver:gold"));case MerchantTransactionQuests.DEPOSIT->deposited>=goal&&withdrawn>=goal;case MerchantTransactionQuests.SELL->largestSale>=goal;case MerchantTransactionQuests.BUY->largestPurchase>=goal;case MerchantTransactionQuests.SUPPLY->resources.size()>=goal;default->orderFilled;};}
 }
 static final Proof EMPTY=new Proof(0,0,0,0,Set.of(),false,Set.of());
 final Map<UUID,Proof> proofs=new HashMap<>();
 MerchantTransactionState copy(){var n=new MerchantTransactionState();n.proofs.putAll(proofs);return n;}
 Proof get(UUID id){return proofs.getOrDefault(id,EMPTY);}
 private static boolean active(Map<UUID,ProfessionRoster> rosters,UUID id){var r=rosters.getOrDefault(id,ProfessionRoster.empty());return r.isActive(Profession.MERCHANT)&&r.progressBand(Profession.MERCHANT)==ProfessionRank.APPRENTICE;}
 private static long add(long before,long amount){return before+Math.min(amount,EconomyStore.MAX_COINS-before);}
 void exchanged(UUID player,CoinExchangeTerms terms,Map<UUID,ProfessionRoster> rosters){if(!active(rosters,player))return;var p=get(player);var kinds=new HashSet<>(p.exchanges());if(Set.of("copper:silver","silver:gold").contains(terms.source()+":"+terms.target()))kinds.add(terms.source()+":"+terms.target());proofs.put(player,new Proof(p.deposited(),p.withdrawn(),p.largestSale(),p.largestPurchase(),p.resources(),p.orderFilled(),kinds));}
 void observe(EconomyStore.Kind kind,UUID actor,UUID target,long amount,String key,UUID reference,Map<UUID,ProfessionRoster> rosters,Map<UUID,EconomyStore.BuyOrder> orders){
  switch(kind){
   case TRANSFER->{if(active(rosters,target)){var p=get(target);proofs.put(target,new Proof(amount>0?add(p.deposited(),amount):p.deposited(),amount<0?add(p.withdrawn(),-amount):p.withdrawn(),p.largestSale(),p.largestPurchase(),p.resources(),p.orderFilled(),p.exchanges()));}}
   case BUY_ITEM->{if(active(rosters,actor)){var p=get(actor);proofs.put(actor,new Proof(p.deposited(),p.withdrawn(),p.largestSale(),Math.max(p.largestPurchase(),amount),p.resources(),p.orderFilled(),p.exchanges()));}if(active(rosters,target)){var p=get(target);proofs.put(target,new Proof(p.deposited(),p.withdrawn(),Math.max(p.largestSale(),amount),p.largestPurchase(),p.resources(),p.orderFilled(),p.exchanges()));}}
   case SALE->{if(active(rosters,target)&&Set.of("kingdom_smp:supplies/logs","kingdom_smp:supplies/stone","kingdom_smp:supplies/wheat","kingdom_smp:supplies/iron").contains(key)){var p=get(target);var goods=new HashSet<>(p.resources());goods.add(key);proofs.put(target,new Proof(p.deposited(),p.withdrawn(),p.largestSale(),p.largestPurchase(),goods,p.orderFilled(),p.exchanges()));}}
   case FILL_ORDER->{var order=orders.get(reference);if(order!=null&&!order.owner().equals(actor)&&active(rosters,actor)){var p=get(actor);proofs.put(actor,new Proof(p.deposited(),p.withdrawn(),p.largestSale(),p.largestPurchase(),p.resources(),true,p.exchanges()));}}
   default->{}
  }
 }
}
