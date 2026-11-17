package com.mohymods.kingdomsmp.fishing;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.network.CommerceView;
import com.mohymods.kingdomsmp.persistence.ConstructionStore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import java.io.IOException;
import java.util.*;

/** The actual Fishmonger's reviewed inventory trades use the existing canonical purse, quotas and receipts. */
public final class FishingService {
    public record Quote(UUID nonce,long revision,long day,String species,int quantity,long amount,long issued,List<ItemStack> inventory,String text){
        public Quote{inventory=inventory.stream().map(ItemStack::copy).toList();}
        @Override public List<ItemStack> inventory(){return inventory.stream().map(ItemStack::copy).toList();}
    }
    private static final Map<ServerPlayer,Quote> QUOTES=new WeakHashMap<>();
    private FishingService() { }
    public static void initialize(MinecraftServer server){QUOTES.keySet().removeIf(p->p.server==server);FishingMarketRules.clear(server);FishingContractRules.clear(server);}
    public static void close(ServerPlayer p){QUOTES.remove(p);FishingContractService.close(p);}
    public static Optional<Quote> quote(ServerPlayer p){var q=QUOTES.get(p);if(q!=null&&(q.revision()!=CommerceService.store(p.server).revision()||q.day()!=MarketService.day()||System.nanoTime()-q.issued()>30_000_000_000L)){close(p);q=null;}return Optional.ofNullable(q);}
    public static boolean atCounter(ServerPlayer p){var project=CommerceService.counterProject(p);return project!=null&&project.blueprint().equals(FishermansWharfBlueprint.ID);}
    public static void requireCounter(ServerPlayer p){
        AquacultureAccess.require();CommerceService.requireResidentCounter(p);
        if(p instanceof FakePlayer||p.isCreative()||p.isSpectator()||!p.isAlive()||p.server.getPlayerList().getPlayer(p.getUUID())!=p||ContributionService.kingdom(p.server).attainedTier()<3||!atCounter(p))throw new IllegalStateException("Visit the assigned Fishmonger in the completed Tier3 Fisherman's Wharf as a living Survival/Adventure player.");
    }
    private static String key(String fish){return "kingdom_smp:fishing/"+(fish.equals("rod")?"iron_rod":"aquaculture/"+fish);}
    private static String title(String path){return switch(path){case "smallmouth_bass"->"Smallmouth bass";case "brown_trout"->"Brown trout";case "bluegill"->"Bluegill";case "carp"->"Carp";case "catfish"->"Catfish";case "perch"->"Perch";default->"Iron fishing rod";};}
    private static int count(ServerPlayer p,Item item){int n=0;var expected=new ItemStack(item);for(int slot=0;slot<36;slot++){var stack=p.getInventory().getItem(slot);if(ItemStack.isSameItemSameComponents(stack,expected))n+=stack.getCount();}return n;}
    public static Quote review(ServerPlayer p,String selected,int requested)throws IOException{
        requireCounter(p);if(!selected.startsWith("fish:"))throw new IllegalArgumentException("Select a Fishmonger trade");String fish=selected.substring(5);boolean rod=fish.equals("rod");if(!rod&&!AquacultureAccess.FISH.contains(fish))throw new IllegalArgumentException("Unknown Fishmonger trade");
        if(requested<1||requested>2304)throw new IllegalArgumentException("Choose a bounded sale quantity");var store=CommerceService.store(p.server);CommerceService.verifyReceipt(p,store);var rules=FishingMarketRules.get(p.server);var config=CommerceService.catalog(p.server);long day=MarketService.day(),amount;int quantity;String text;
        if(rod){if(store.shopSold(day,key(fish))>=rules.rodStock())throw new IllegalStateException("Today's fishing-rod stock is exhausted");if(store.balance(p.getUUID())<rules.rodPrice())throw new IllegalStateException("Insufficient coins for the iron fishing rod");quantity=1;amount=rules.rodPrice();text="Buy one pristine Aquaculture iron fishing rod, with empty equipment slots, for "+amount+" coins. Payment, daily stock and exact private item mail commit together. Claim the rod from your Mailbox at a resident. No bait or hooks are included. Attached equipment is never stripped from an existing rod. This is not a Master profession reward.";}
        else{var order=rules.orders().get(fish);var used=store.usage(day,key(fish),p.getUUID());long budget=Math.min(config.budget()-used.sharedSpent(),config.allowance()-used.playerProceeds());int available=Math.min(requested,count(p,AquacultureAccess.fish(fish)));if(available<1||used.resourceQuantity()>=order.quota())throw new IllegalStateException("Bring plain matching fish for an unfinished daily order");quantity=MarketPricing.affordable(order.floor(),order.ceiling(),used.resourceQuantity(),order.quota(),available,Math.max(0,budget),false);if(quantity<1)throw new IllegalStateException("Bring plain matching fish, with remaining species quota and shared/personal buying allowance");amount=MarketPricing.total(order.floor(),order.ceiling(),used.resourceQuantity(),order.quota(),quantity,false);text="Permanently sell "+quantity+" plain "+title(fish)+" for exactly "+amount+" coins to your purse. Each fish follows the current supply-price band; splitting batches cannot retain an earlier rate. This shares the existing kingdom NPC buying budget and your personal sales allowance with Essentials. Species quota and prices reset at 00:00 UTC. Native inventory and purse records are checkpointed together. Review expires after 30 seconds.";}
        List<ItemStack> inventory=new ArrayList<>();for(int i=0;i<36;i++)inventory.add(p.getInventory().getItem(i).copy());var q=new Quote(UUID.randomUUID(),store.revision(),day,fish,quantity,amount,System.nanoTime(),inventory,text);QUOTES.put(p,q);return q;
    }
    public static String confirm(ServerPlayer p,UUID nonce)throws IOException{
        var q=QUOTES.remove(p);var store=CommerceService.store(p.server);CommerceService.verifyReceipt(p,store);
        if(q==null||!q.nonce().equals(nonce)||q.day()!=MarketService.day()||q.revision()!=store.revision()||System.nanoTime()-q.issued()>30_000_000_000L)throw new IllegalStateException("Fish trade review expired or accounts changed; review again");requireCounter(p);var rules=FishingMarketRules.get(p.server);
        if(q.species().equals("rod")){if(q.amount()!=rules.rodPrice()||q.quantity()!=1)throw new IllegalStateException("Fishing rod terms changed");var rod=new ItemStack(AquacultureAccess.rod());store.shopBuy(q.nonce(),p.getUUID(),key("rod"),"Fishmonger iron fishing rod",(CompoundTag)rod.save(p.registryAccess()),1,q.day(),rules.rodStock(),rules.rodPrice(),rules.rodPrice());return "Iron fishing rod purchased once. Claim your exact empty rod through Mailbox at a resident.";}
        var order=rules.orders().get(q.species());var used=store.usage(q.day(),key(q.species()),p.getUUID());long expected=MarketPricing.total(order.floor(),order.ceiling(),used.resourceQuantity(),order.quota(),q.quantity(),false);if(expected!=q.amount())throw new IllegalStateException("Fish price changed; review again");var config=CommerceService.catalog(p.server);
        return CommerceService.sellInventory(p,q.nonce(),key(q.species()),AquacultureAccess.fish(q.species()),title(q.species()),q.quantity(),q.amount(),q.day(),config.budget(),order.quota(),config.allowance(),q.inventory());
    }
    public static List<CommerceView.Entry> entries(ServerPlayer p){
        String unavailable=AquacultureAccess.unavailable();if(!unavailable.isEmpty())return List.of(new CommerceView.Entry("fish:unavailable","Aquaculture fishing services",unavailable,0,"LOCKED"));
        var rules=FishingMarketRules.get(p.server);var config=CommerceService.catalog(p.server);var store=CommerceService.store(p.server);long day=MarketService.day();List<CommerceView.Entry> rows=new ArrayList<>();
        for(var fish:AquacultureAccess.FISH){var order=rules.orders().get(fish);var used=store.usage(day,key(fish),p.getUUID());long price=used.resourceQuantity()>=order.quota()?0:MarketPricing.unit(order.floor(),order.ceiling(),used.resourceQuantity(),order.quota(),false);rows.add(new CommerceView.Entry("fish:"+fish,"Sell: "+title(fish),"Plain fish only. Quota "+used.resourceQuantity()+"/"+order.quota()+"; price "+order.floor()+"–"+order.ceiling()+". Shared NPC budget "+used.sharedSpent()+"/"+config.budget()+"; your sales "+used.playerProceeds()+"/"+config.allowance()+". Uses the same budget as Essentials. UTC reset.",price,used.resourceQuantity()>=order.quota()?"QUOTA_FULL":"SELL"));}
        long stock=store.shopSold(day,key("rod"));rows.add(new CommerceView.Entry("fish:rod","Buy: Iron fishing rod","Aquaculture iron rod, empty equipment slots. Remaining stock "+Math.max(0,rules.rodStock()-stock)+"/"+rules.rodStock()+". Exact private item mail; claim at a resident. No Master entitlement or free bait.",stock>=rules.rodStock()?0:rules.rodPrice(),stock>=rules.rodStock()?"OUT_OF_STOCK":"SHOP"));
        return List.copyOf(rows);
    }
}
