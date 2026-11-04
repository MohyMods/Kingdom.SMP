package com.mohymods.kingdomsmp.profession;

import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import java.io.IOException;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;

/** Native recipe evidence where required plus exact native material consumption, with the existing receipt protocol. */
public final class CraftDeliveryService {
    public record Quote(UUID nonce,long revision,String quest,Map<String,Integer> inputs,List<ItemStack> before,List<ItemStack> after,long issued,String text){
        public Quote{inputs=Map.copyOf(inputs);before=before.stream().map(ItemStack::copy).toList();after=after.stream().map(ItemStack::copy).toList();}
        @Override public List<ItemStack> before(){return before.stream().map(ItemStack::copy).toList();}
        @Override public List<ItemStack> after(){return after.stream().map(ItemStack::copy).toList();}
    }
    private static final Map<ServerPlayer,Quote> QUOTES=new WeakHashMap<>();
    private CraftDeliveryService() { }
    public static void close(ServerPlayer p){QUOTES.remove(p);}
    public static boolean eligible(ServerPlayer p,String id){
        if(!CraftDeliveryQuests.contains(id)||!TreeQuestService.real(p))return false;
        var d=CraftDeliveryQuests.get(id);var roster=CommerceService.store(p.server).professions(p.getUUID());
        if(!roster.isActive(d.profession())||roster.track(d.profession()).completedQuestIds().contains(id)||!CraftDeliveryQuests.catalog().mayAttempt(d.profession(),id,roster.track(d.profession()).completedQuestIds())
            ||ContributionService.kingdom(p.server).attainedTier()<d.minimumTier()||!ProfessionService.available(p,d.profession()))return false;
        if(id.equals(CraftDeliveryQuests.CHARCOAL)&&TreeEvidence.SPECIES.stream().mapToInt(s->TreeSavedData.get(p.server).evidence().evidence().count(p.getUUID(),d.rank(),s)).sum()<CraftDeliveryRules.get(p.server).inputs(id).get("minecraft:charcoal"))return false;
        return !d.validation().contains(ApprovedProfessionContent.Validation.MAKE)||CraftDeliveryRules.get(p.server).inputs(id).entrySet().stream().allMatch(e->CraftQuestEvidence.count(p,d.profession(),d.rank(),e.getKey())>=e.getValue());
    }
    public static String status(ServerPlayer p,String id){
        var d=CraftDeliveryQuests.get(id);var roster=CommerceService.store(p.server).professions(p.getUUID());
        if(roster.track(d.profession()).completedQuestIds().contains(id))return "Complete. Material delivery and earned XP are saved.";
        if(!roster.isActive(d.profession()))return "Activate "+ProfessionService.name(d.profession())+" before earning crafting evidence.";
        var inputs=CraftDeliveryRules.get(p.server).inputs(id);
        if(id.equals(CraftDeliveryQuests.FUEL))return "Permanently supply "+inputs.get("fuel")+" plain coal or charcoal at the Blacksmith resident. Variants may be mixed; review consumes nothing.";
        if(id.equals(CraftDeliveryQuests.CHARCOAL))return CraftQuestEvidence.count(p,d.profession(),d.rank(),"minecraft:charcoal")+" / "+inputs.get("minecraft:charcoal")+" native charcoal smelted. Personally harvest this amount of tracked timber in this band, then permanently deliver plain charcoal at your resident.";
        if(id.equals(CraftDeliveryQuests.FITTINGS))return inputs.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->CraftQuestEvidence.count(p,d.profession(),d.rank(),e.getKey())+"/"+e.getValue()+" "+e.getKey().replace('_',' ')).collect(java.util.stream.Collectors.joining(", "))+". Craft then permanently deliver matching plain Overworld wood variants; categories can be mixed.";
        if(id.equals(CraftDeliveryQuests.TIMBER))return CraftQuestEvidence.count(p,d.profession(),d.rank(),"planks")+"/"+inputs.get("planks")+" crafted planks; "+CraftQuestEvidence.count(p,d.profession(),d.rank(),"stripped_logs")+"/"+inputs.get("stripped_logs")+" stripped logs. Permanently deliver these amounts in any plain Overworld wood species at your resident.";
        String counts=inputs.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->CraftQuestEvidence.count(p,d.profession(),d.rank(),e.getKey())+"/"+e.getValue()+(CraftDeliveryQuests.metrics(d.profession()).get(e.getKey()).startsWith("smelt_")?" smelted ":" crafted ")+new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(e.getKey()))).getHoverName().getString()).collect(java.util.stream.Collectors.joining(", "));
        return counts+". Permanently deliver these quantities at your profession resident. Both native production and delivery are required.";
    }
    private static Map<Item,Integer> items(Map<String,Integer> counts){
        Map<Item,Integer> result=new HashMap<>();counts.forEach((id,n)->{
            var key=ResourceLocation.parse(id);if(!BuiltInRegistries.ITEM.containsKey(key)||BuiltInRegistries.ITEM.get(key)==Items.AIR||n<1||n>2304)throw new IllegalArgumentException("Unregistered or invalid delivery input");
            result.put(BuiltInRegistries.ITEM.get(key),n);
        });return Map.copyOf(result);
    }
    private static Map<String,Integer> resolve(String id,Map<String,Integer> goals,List<ItemStack> inventory){
        if(!CraftDeliveryQuests.grouped(id))return goals;
        var remaining=new HashMap<>(goals);Map<String,Integer> actual=new HashMap<>();
        for(var stack:inventory){
            if(stack.isEmpty()||!ItemStack.isSameItemSameComponents(stack,new ItemStack(stack.getItem())))continue;
            String item=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),category=CraftDeliveryQuests.materialCategory(id,item);if(category==null||!remaining.containsKey(category))continue;
            int take=Math.min(stack.getCount(),remaining.get(category));if(take>0){actual.merge(item,take,Math::addExact);remaining.put(category,remaining.get(category)-take);}
        }
        if(remaining.values().stream().anyMatch(n->n>0))throw new IllegalStateException("Bring every required plain material category; variants can be mixed and review consumes nothing");return Map.copyOf(actual);
    }
    public static Quote review(ServerPlayer p,String id)throws IOException{
        var d=CraftDeliveryQuests.get(id);ContractService.requireCounter(p,d.profession());
        if(!eligible(p,id))throw new IllegalStateException(status(p,id));
        var store=CommerceService.store(p.server);CommerceService.verifyReceipt(p,store);var inputs=CraftDeliveryRules.get(p.server).inputs(id);
        List<ItemStack> before=new ArrayList<>();for(int i=0;i<36;i++)before.add(p.getInventory().getItem(i).copy());
        // Reuse the existing tested exact-component native inventory exchange, without returned containers.
        inputs=resolve(id,inputs,before);
        var after=FarmerInventoryExchange.plan(before,items(inputs),Map.of());
        String text="Permanently deliver "+FarmerDeliveryService.describe(inputs)+" to complete "+d.name()+" for "+d.xp()+" profession XP once. Your native production evidence is verified. No coin or equipment reward is minted.";
        var q=new Quote(UUID.randomUUID(),store.revision(),id,inputs,before,after,System.nanoTime(),text);QUOTES.put(p,q);return q;
    }
    public static String confirm(ServerPlayer p,UUID nonce)throws IOException{
        var q=QUOTES.remove(p);var store=CommerceService.store(p.server);CommerceService.verifyReceipt(p,store);
        if(q==null||!q.nonce().equals(nonce)||q.revision()!=store.revision()||System.nanoTime()-q.issued()>30_000_000_000L)throw new IllegalStateException("Craft delivery review expired; review again");
        var d=CraftDeliveryQuests.get(q.quest());ContractService.requireCounter(p,d.profession());
        if(!eligible(p,q.quest())||!CraftDeliveryRules.get(p.server).inputs(q.quest()).equals(CraftDeliveryQuests.totals(q.quest(),q.inputs())))throw new IllegalStateException("Craft objective or configuration changed; review again");
        var before=q.before();for(int i=0;i<36;i++)if(!ItemStack.matches(p.getInventory().getItem(i),before.get(i)))throw new IllegalStateException("Inventory changed; review the full delivery again");
        var old=p.getPersistentData().copy();boolean checkpointStarted=false;
        try{
            CommerceService.checkpoint(p);
            var pending=store.prepareProfessionDelivery(q.nonce(),p.getUUID(),q.quest(),"Actual native production verified; reviewed plain outputs consumed",q.inputs());
            var after=q.after();for(int i=0;i<36;i++)p.getInventory().setItem(i,after.get(i).copy());
            CommerceService.putReceipt(p,new EconomyStore.Receipt(store.id(),pending.sequence()));checkpointStarted=true;CommerceService.checkpoint(p);
            if(!store.recoverItem(CommerceService.receipt(AtomicNbt.read(CommerceService.playerFile(p.server,p.getUUID())))))throw new IOException("Craft delivery native receipt did not commit");
            p.getInventory().setChanged();p.inventoryMenu.sendAllDataToRemote();return d.name()+" complete. Exact materials consumed and earned XP saved once.";
        }catch(IOException|RuntimeException e){
            if(!checkpointStarted){for(int i=0;i<36;i++)p.getInventory().setItem(i,before.get(i).copy());for(var key:new HashSet<>(p.getPersistentData().getAllKeys()))p.getPersistentData().remove(key);p.getPersistentData().merge(old);}
            if(!checkpointStarted&&store.pendingItem().isEmpty()&&!store.quarantined()&&(e instanceof IllegalArgumentException||e instanceof IllegalStateException))throw e;
            store.quarantine();p.connection.disconnect(net.minecraft.network.chat.Component.literal("Craft delivery interrupted; restore matching native inventory and canonical checkpoint before proceeding"));throw e;
        }
    }
}
