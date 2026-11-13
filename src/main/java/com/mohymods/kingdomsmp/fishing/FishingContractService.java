package com.mohymods.kingdomsmp.fishing;
import com.mohymods.kingdomsmp.bounty.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.network.CommerceView;
import com.mohymods.kingdomsmp.profession.ContractRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import java.io.IOException;
import java.util.*;
/** Weekly native fish deliveries, pinned shared goals and a separate finite NPC reward pool. */
public final class FishingContractService {
 public record Quote(UUID nonce,long revision,long week,CommunityContractRules.Terms terms,int quantity,List<ItemStack> inventory,long issued,String text){
  public Quote{inventory=inventory.stream().map(ItemStack::copy).toList();}
  @Override public List<ItemStack> inventory(){return inventory.stream().map(ItemStack::copy).toList();}
 }
 private static final Map<ServerPlayer,Quote> QUOTES=new WeakHashMap<>();
 private FishingContractService(){}
 public static void close(ServerPlayer p){QUOTES.remove(p);}
 public static Optional<Quote> quote(ServerPlayer p){var q=QUOTES.get(p);if(q!=null&&(q.revision()!=CommerceService.store(p.server).revision()||q.week()!=ContractRules.period("weekly")||System.nanoTime()-q.issued()>30_000_000_000L)){close(p);q=null;}return Optional.ofNullable(q);}
 private static CommunityContractRules.Terms terms(ServerPlayer p,String id,long week){
  var all=FishingContractRules.get(p.server).orders();var configured=all.stream().filter(t->t.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown fishing order"));var store=CommerceService.store(p.server);var pinned=store.communityProject(id,week).orElse(null);
  long pool=all.stream().map(t->store.communityProject(t.id(),week)).flatMap(Optional::stream).mapToLong(CommunityContractState.Project::budget).findFirst().orElse(configured.budget());
  return pinned==null?new CommunityContractRules.Terms(id,configured.title(),configured.item(),configured.goal(),configured.unitReward(),pool):new CommunityContractRules.Terms(id,configured.title(),pinned.item(),pinned.goal(),pinned.unitReward(),pinned.budget());
 }
 public static Quote review(ServerPlayer p,String selected,int requested)throws IOException{
  FishingService.requireCounter(p);if(!selected.startsWith("fishing:"))throw new IllegalArgumentException("Choose a fishing contract");if(requested<1||requested>2304)throw new IllegalArgumentException("Invalid delivery quantity");var store=CommerceService.store(p.server);CommerceService.verifyReceipt(p,store);long week=ContractRules.period("weekly");var t=terms(p,selected.substring(8),week);var plain=new ItemStack(AquacultureAccess.fish(t.item().substring(12)));int available=0;List<ItemStack> inventory=new ArrayList<>();for(int i=0;i<36;i++){var stack=p.getInventory().getItem(i);inventory.add(stack.copy());if(ItemStack.isSameItemSameComponents(stack,plain))available+=stack.getCount();}int delivered=store.communityProject(t.id(),week).map(CommunityContractState.Project::delivered).orElse(0);int quantity=Math.min(Math.min(requested,64),Math.min(available,t.goal()-delivered));if(quantity<1)throw new IllegalStateException("Bring matching plain fish for this unfinished weekly order");long reward=(long)quantity*t.unitReward();if(reward>t.budget()-store.fishingContractSpent(week))throw new IllegalStateException("Weekly fishing reward pool exhausted");String text="Permanently deliver "+quantity+" plain "+plain.getHoverName().getString()+" to "+t.title()+". Shared progress "+delivered+"/"+t.goal()+"; exactly "+reward+" coins in private Mailbox after native inventory checkpoint. These fish cannot also be sold. The separate weekly Fishmonger pool is "+t.budget()+" coins, pinned after the first delivery. Orders reset Monday at 00:00 UTC. This supply order accepts traded fish and grants no Farmer rank or collection proof. Review expires after 30 seconds.";var q=new Quote(UUID.randomUUID(),store.revision(),week,t,quantity,inventory,System.nanoTime(),text);QUOTES.put(p,q);return q;
 }
 public static String confirm(ServerPlayer p,UUID nonce)throws IOException{
  var q=QUOTES.remove(p);var store=CommerceService.store(p.server);CommerceService.verifyReceipt(p,store);if(q==null||!q.nonce().equals(nonce)||q.revision()!=store.revision()||q.week()!=ContractRules.period("weekly")||System.nanoTime()-q.issued()>30_000_000_000L)throw new IllegalStateException("Fishing delivery review expired or accounts changed");FishingService.requireCounter(p);if(!q.terms().equals(terms(p,q.terms().id(),q.week())))throw new IllegalStateException("Fishing delivery terms changed; review again");return CommunityContractService.deliverInventory(p,q.nonce(),q.terms(),q.week(),q.quantity(),q.inventory());
 }
 public static List<CommerceView.Entry> entries(ServerPlayer p){
  if(!AquacultureAccess.available())return List.of(new CommerceView.Entry("fishing:unavailable","Fishing contracts",AquacultureAccess.unavailable(),0,"LOCKED"));var store=CommerceService.store(p.server);long week=ContractRules.period("weekly");List<CommerceView.Entry> rows=new ArrayList<>();for(var configured:FishingContractRules.get(p.server).orders()){var t=terms(p,configured.id(),week);var progress=store.communityProject(t.id(),week).orElse(null);int delivered=progress==null?0:progress.delivered();rows.add(new CommerceView.Entry("fishing:"+t.id(),t.title(),"Shared fish delivery "+delivered+"/"+t.goal()+". "+t.unitReward()+" coins/fish via Mailbox. Weekly fishing pool "+store.fishingContractSpent(week)+"/"+t.budget()+". Up to 64/batch; Monday 00:00 UTC reset. Traded plain fish accepted; no Farmer quest credit.",0,delivered==t.goal()?"COMPLETE":"COMMUNITY"));}rows.addAll(FishingCatchService.entries(p));return List.copyOf(rows);
 }
}
