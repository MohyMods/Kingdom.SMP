package com.mohymods.kingdomsmp.profession;
import com.mohymods.kingdomsmp.building.ConstructionService;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import java.io.IOException;
import java.util.*;
/** Approved quest food deliveries, including exact returned containers, through the native receipt protocol. */
public final class FarmerDeliveryService {
 public record Quote(UUID nonce,long revision,String quest,Map<String,Integer> inputs,List<ItemStack> before,List<ItemStack> after,long issued,String text){
  public Quote{inputs=Map.copyOf(inputs);before=before.stream().map(ItemStack::copy).toList();after=after.stream().map(ItemStack::copy).toList();}
  @Override public List<ItemStack> before(){return before.stream().map(ItemStack::copy).toList();}
  @Override public List<ItemStack> after(){return after.stream().map(ItemStack::copy).toList();}
 }
 private static final Map<ServerPlayer,Quote> QUOTES=new WeakHashMap<>();
 private FarmerDeliveryService(){ }
 public static void close(ServerPlayer p){QUOTES.remove(p);}
 public static boolean eligible(ServerPlayer p,String id){try{FarmerDeliveries.family(id);}catch(IllegalArgumentException notDelivery){return false;}var roster=CommerceService.store(p.server).professions(p.getUUID());return roster.isActive(Profession.FARMER)&&!roster.track(Profession.FARMER).completedQuestIds().contains(id)&&FarmerQuests.catalog().mayAttempt(Profession.FARMER,id,roster.track(Profession.FARMER).completedQuestIds());}
 public static Map<String,Integer> inputs(ServerPlayer p,String id){var rules=FarmerQuestRules.get(p.server);return switch(id){
  case "kingdom_smp:farmer/a07"->Map.of("minecraft:bread",rules.goal("a07"));
  case "kingdom_smp:farmer/j05"->Map.of("minecraft:egg",rules.goal("j05_eggs"),"minecraft:milk_bucket",rules.goal("j05_milk"));
  case "kingdom_smp:farmer/j06"->Map.of("minecraft:cake",rules.goal("j06"));
  case "kingdom_smp:farmer/m03"->Map.of("minecraft:bread",rules.goal("m03_bread"),"minecraft:cake",rules.goal("m03_cake"),"minecraft:pumpkin_pie",rules.goal("m03_pie"),"minecraft:beetroot_soup",rules.goal("m03_soup"));
  default->throw new IllegalArgumentException("Unknown approved food delivery");};}
 public static String describe(Map<String,Integer> counts){return counts.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->String.format(Locale.ROOT,"%,d",e.getValue())+" × "+new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(e.getKey()))).getHoverName().getString()).collect(java.util.stream.Collectors.joining(", "));}
 private static Map<Item,Integer> items(Map<String,Integer> counts){var result=new HashMap<Item,Integer>();counts.forEach((key,count)->{var id=ResourceLocation.parse(key);if(!BuiltInRegistries.ITEM.containsKey(id)||count<1||count>2304)throw new IllegalArgumentException("Invalid registered Farmer food/quantity");result.put(BuiltInRegistries.ITEM.get(id),count);});return Map.copyOf(result);}
 private static void requireCounter(ServerPlayer p){CommerceService.requireResidentCounter(p);if(!ProfessionService.available(p,Profession.FARMER))throw new IllegalStateException("Maintain the completed Farmer's House.");boolean near=ConstructionService.store(p.server).projects().stream().filter(v->v.blueprint().toString().equals(Profession.FARMER.buildingId())).anyMatch(v->{var entity=p.serverLevel().getEntity(v.npc());return entity instanceof com.mohymods.kingdomsmp.npc.ReeveEntity npc&&ConstructionService.registeredReeve(npc)&&p.distanceToSqr(npc)<=64&&p.hasLineOfSight(npc);});if(!near)throw new IllegalStateException("Deliver food to the assigned Farmer within eight blocks and clear sight.");}
 public static Quote review(ServerPlayer p,String id)throws IOException{
  requireCounter(p);if(!eligible(p,id))throw new IllegalStateException("This Farmer food objective is inactive, already completed or needs an earlier band.");if(id.equals("kingdom_smp:farmer/j06")&&FarmerQuestService.count(p,ProfessionRank.JOURNEYMAN,"cake")<FarmerQuestRules.get(p.server).goal("j06"))throw new IllegalStateException("Prepare the required cake through real crafting before delivering it.");
  var store=CommerceService.store(p.server);CommerceService.verifyReceipt(p,store);var inputs=inputs(p,id);List<ItemStack> before=new ArrayList<>();for(int i=0;i<36;i++)before.add(p.getInventory().getItem(i).copy());var after=FarmerInventoryExchange.plan(before,items(inputs),items(FarmerDeliveries.containers(inputs)));String text="Permanently deliver "+describe(inputs)+" to complete "+FarmerQuests.title(id)+". Earn "+FarmerQuests.xp(id)+" Farmer XP; no coins. Empty containers returned: "+(FarmerDeliveries.containers(inputs).isEmpty()?"none":describe(FarmerDeliveries.containers(inputs)))+". Check the quantities before confirming.";var q=new Quote(UUID.randomUUID(),store.revision(),id,inputs,before,after,System.nanoTime(),text);QUOTES.put(p,q);return q;
 }
 public static String confirm(ServerPlayer p,UUID nonce)throws IOException{
  var q=QUOTES.remove(p);var store=CommerceService.store(p.server);CommerceService.verifyReceipt(p,store);if(q==null||!q.nonce().equals(nonce)||q.revision()!=store.revision()||System.nanoTime()-q.issued()>30_000_000_000L)throw new IllegalStateException("Food delivery review expired; review again.");requireCounter(p);if(!eligible(p,q.quest())||!inputs(p,q.quest()).equals(q.inputs()))throw new IllegalStateException("Farmer objective or configuration changed; review again.");var before=q.before();for(int i=0;i<36;i++)if(!ItemStack.matches(p.getInventory().getItem(i),before.get(i)))throw new IllegalStateException("Inventory changed; review the full delivery again.");
  var old=p.getPersistentData().copy();boolean checkpointStarted=false;
  try{CommerceService.checkpoint(p);var pending=store.prepareFarmerDelivery(q.nonce(),p.getUUID(),q.quest(),"Consumed native food; returned exact containers",q.inputs());var after=q.after();for(int i=0;i<36;i++)p.getInventory().setItem(i,after.get(i).copy());CommerceService.putReceipt(p,new EconomyStore.Receipt(store.id(),pending.sequence()));checkpointStarted=true;CommerceService.checkpoint(p);if(!store.recoverItem(CommerceService.receipt(AtomicNbt.read(CommerceService.playerFile(p.server,p.getUUID())))))throw new IOException("Farmer delivery native receipt did not commit");p.getInventory().setChanged();p.inventoryMenu.sendAllDataToRemote();return FarmerQuests.title(q.quest())+" complete. Your Farmer XP is saved and empty containers have been returned.";}
  catch(IOException|RuntimeException e){if(!checkpointStarted){for(int i=0;i<36;i++)p.getInventory().setItem(i,before.get(i).copy());for(var key:new HashSet<>(p.getPersistentData().getAllKeys()))p.getPersistentData().remove(key);p.getPersistentData().merge(old);}if(!checkpointStarted&&store.pendingItem().isEmpty()&&!store.quarantined()&&(e instanceof IllegalArgumentException||e instanceof IllegalStateException))throw e;store.quarantine();p.connection.disconnect(net.minecraft.network.chat.Component.literal("Farmer delivery interrupted; restore matching inventory and canonical checkpoint before proceeding."));throw e;}
 }
}
