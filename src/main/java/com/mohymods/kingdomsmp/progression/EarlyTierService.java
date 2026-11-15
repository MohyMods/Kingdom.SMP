package com.mohymods.kingdomsmp.progression;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.network.CommerceView;
import com.mohymods.kingdomsmp.persistence.*;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;

/** Three independent pinned journals, using the proven prepare/native-receipt recovery protocol. */
public final class EarlyTierService {
    private record Offer(UUID nonce,int tier,long revision,String resource,int amount,long issued,List<ItemStack> before) { }
    private static final Map<MinecraftServer,Map<Integer,TierTwoLedger>> STORES=new WeakHashMap<>();
    private static final Map<ServerPlayer,Offer> OFFERS=new WeakHashMap<>();
    private EarlyTierService() { }
    public static void initialize(MinecraftServer s){STORES.remove(s);OFFERS.keySet().removeIf(p->p.server==s);}
    public static TierTwoLedger ledger(MinecraftServer s,int tier){
        EarlyTierRules.defaults(tier);
        if(!ConstructionService.royal(s))throw new IllegalStateException("Legacy progression is preserved");
        var stores=STORES.computeIfAbsent(s,k->new HashMap<>());var current=stores.get(tier);if(current!=null)return current;
        try {
            var costs=EarlyTierRules.load(s.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-tier-"+tier+".json"),tier);
            var directory=s.getWorldPath(LevelResource.ROOT).resolve("data/kingdomsmp_early_tiers/tier_"+tier);Files.createDirectories(directory);
            var opened=TierTwoLedger.open(directory,costs);
            if(opened.pending().isPresent()){var d=opened.pending().orElseThrow().donation();var file=playerFile(s,d.player());opened.recover(Files.exists(file)?receipt(AtomicNbt.read(file).getCompound("NeoForgeData"),tier):TierTwoLedger.Receipt.empty());}
            stores.put(tier,opened);return opened;
        }catch(IOException e){throw new IllegalStateException("Tier "+tier+" inventory journal/configuration needs matching-backup recovery",e);}
    }
    public static int attainedTier(MinecraftServer s){
        if(!ConstructionService.royal(s)||!TierTwoService.ledger(s).complete())return 1;
        int current=3;while(current<6&&ledger(s,current).resourcesComplete())current++;return current;
    }
    public static long revision(MinecraftServer s){if(!ConstructionService.royal(s)||!TierTwoService.ledger(s).complete())return 0;long n=0;for(int t=3;t<=Math.min(attainedTier(s),5);t++)n=Math.addExact(n,ledger(s,t).revision());return n;}
    public static List<String> status(MinecraftServer server){
        if(!ConstructionService.royal(server)||!TierTwoService.ledger(server).complete())return List.of("Early alpha milestones await the original Tier2 completion; existing records are unchanged.");
        List<String> rows=new ArrayList<>();int attained=attainedTier(server);rows.add("Alpha earned tier "+attained+". Tier7–12: Coming Soon — Future Update.");
        for(int tier=3;tier<=Math.min(attained,5);tier++){
            var s=ledger(server,tier);rows.add("Tier"+tier+" journal "+s.id()+" revision "+s.revision()+" complete "+s.resourcesComplete()+" pending "+s.pending().isPresent()+" quarantined "+s.quarantined());
            s.requirements().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->rows.add(e.getKey()+": "+s.total(e.getKey())+" / "+e.getValue()));
            if(s.resourcesComplete())rows.add("Highest normalized contributor: "+s.standings().getFirst().name()+" ("+s.standings().getFirst().player()+")");
        }return List.copyOf(rows);
    }
    public static void close(ServerPlayer p){OFFERS.remove(p);}
    private static void requirePlayer(ServerPlayer p,int tier){
        var economy=com.mohymods.kingdomsmp.economy.CommerceService.store(p.server);
        if(economy.quarantined()||economy.pendingSale().isPresent()||economy.pendingTransfer().isPresent()||economy.pendingItem().isPresent())throw new IllegalStateException("Resolve the pending economic inventory checkpoint first");
        try{com.mohymods.kingdomsmp.economy.CommerceService.verifyReceipt(p,economy);}catch(IOException e){throw new IllegalStateException("Economic inventory receipt requires matching-backup recovery",e);}
        if(!p.isAlive()||p.gameMode.getGameModeForPlayer()!=GameType.SURVIVAL&&p.gameMode.getGameModeForPlayer()!=GameType.ADVENTURE||p.containerMenu!=p.inventoryMenu||!p.containerMenu.getCarried().isEmpty())throw new IllegalStateException("Contribute alive in Survival/Adventure with containers closed");
        if(tier<3||tier>5||attainedTier(p.server)!=tier)throw new IllegalStateException("That milestone is no longer current");
        var hall=ConstructionService.lodge(p.server);
        if(hall==null||hall.phase()!=ConstructionStore.Phase.COMPLETE||!ConstructionService.structureIntact(p.server.overworld(),hall))throw new IllegalStateException("Maintain the completed Town Hall");
        var hut=BuilderHutSavedData.get(p.server).hut().orElseThrow();if(!BuilderHutService.structureIntact(p.server.overworld(),hut)||hut.npcDead())throw new IllegalStateException("Maintain the Builder's Hut and Aldric");
        boolean bank=ConstructionService.store(p.server).projects().stream().anyMatch(v->v.blueprint().toString().equals("kingdom_smp:royal_bank")&&v.phase()==ConstructionStore.Phase.COMPLETE&&!v.npcDead()&&ConstructionService.structureIntact(p.server.overworld(),v));
        if(!bank)throw new IllegalStateException("Complete and maintain the Tier 3 Royal Bank before the next community milestone");
    }
    private static Item item(String id){var key=ResourceLocation.parse(id);if(!BuiltInRegistries.ITEM.containsKey(key)||BuiltInRegistries.ITEM.get(key)==Items.AIR)throw new IllegalStateException("Required resource is not registered");return BuiltInRegistries.ITEM.get(key);}
    private static boolean matches(ItemStack stack,String id){return !stack.isEmpty()&&stack.is(item(id))&&ItemStack.isSameItemSameComponents(stack,new ItemStack(stack.getItem()));}
    public static UUID review(ServerPlayer p,String resource,int requested){
        int tier=attainedTier(p.server);requirePlayer(p,tier);if(requested<1||requested>2304)throw new IllegalArgumentException("Review at most one main inventory");var s=ledger(p.server,tier);
        if(!s.requirements().containsKey(resource)||s.quarantined()||s.pending().isPresent())throw new IllegalStateException("Unknown resource or journal awaiting recovery");
        try{verifyAll(p);}catch(IOException e){throw new IllegalStateException("Inventory receipt needs recovery",e);}
        int have=0;List<ItemStack> before=new ArrayList<>();for(int i=0;i<36;i++){var stack=p.getInventory().getItem(i);before.add(stack.copy());if(matches(stack,resource))have+=stack.getCount();}
        int amount=Math.min(requested,Math.min(have,s.requirements().get(resource)-s.total(resource)));if(amount<1)throw new IllegalStateException("No eligible supplies or resource target complete");
        var q=new Offer(UUID.randomUUID(),tier,s.revision(),resource,amount,System.nanoTime(),List.copyOf(before));OFFERS.put(p,q);return q.nonce();
    }
    public static Optional<TierTwoService.Quote> quote(ServerPlayer p){var q=OFFERS.get(p);if(q==null)return Optional.empty();if(System.nanoTime()-q.issued()>30_000_000_000L||attainedTier(p.server)!=q.tier()||ledger(p.server,q.tier()).revision()!=q.revision()){close(p);return Optional.empty();}return Optional.of(new TierTwoService.Quote(q.nonce(),"Permanently contribute "+q.amount()+" "+new ItemStack(item(q.resource())).getHoverName().getString()+" toward Tier "+(q.tier()+1)+". Shared progress and normalized personal recognition are saved. No blueprint or free currency is created."));}
    public static boolean hasOffer(ServerPlayer p){return OFFERS.containsKey(p);}
    public static String confirm(ServerPlayer p,UUID nonce){
        var q=OFFERS.remove(p);if(q==null||!q.nonce().equals(nonce)||System.nanoTime()-q.issued()>30_000_000_000L)throw new IllegalStateException("Milestone review expired");requirePlayer(p,q.tier());var s=ledger(p.server,q.tier());
        try{verifyAll(p);}catch(IOException e){throw new IllegalStateException("Inventory receipt needs matching-backup recovery",e);}
        if(s.quarantined()||s.pending().isPresent()||s.revision()!=q.revision())throw new IllegalStateException("Milestone changed; review again");for(int i=0;i<36;i++)if(!ItemStack.matches(p.getInventory().getItem(i),q.before().get(i)))throw new IllegalStateException("Inventory changed; review again");
        var old=p.getPersistentData().copy();boolean checkpointStarted=false;
        try {
            checkpoint(p);var pending=s.prepare(q.nonce(),p.getUUID(),p.getGameProfile().getName(),q.resource(),q.amount());int remaining=q.amount();
            for(int i=0;i<36&&remaining>0;i++){var stack=p.getInventory().getItem(i);if(matches(stack,q.resource())){int take=Math.min(remaining,stack.getCount());stack.shrink(take);remaining-=take;}}
            if(remaining!=0)throw new IllegalStateException("Inventory changed during native removal");putReceipt(p,q.tier(),new TierTwoLedger.Receipt(s.id(),pending.donation().sequence()));checkpointStarted=true;checkpoint(p);
            if(!s.recover(receipt(AtomicNbt.read(playerFile(p.server,p.getUUID())).getCompound("NeoForgeData"),q.tier())))throw new IOException("Missing native removal receipt");
            p.getInventory().setChanged();p.inventoryMenu.sendAllDataToRemote();
            LogUtils.getLogger().info("EARLY_TIER_CONTRIBUTION tier={} operation={} player={} resource={} amount={}",q.tier(),q.nonce(),p.getUUID(),q.resource(),q.amount());
            if(s.resourcesComplete()){var winner=s.standings().getFirst();p.server.getPlayerList().broadcastSystemMessage(Component.literal("Kingdom Tier "+(q.tier()+1)+" reached. Highest normalized contributor: "+winner.name()+". "+(q.tier()==5?"Aerodrome plans unlock; aircraft still require its completed building.":"New buildings and dimension opportunities are available.")),false);}
            return "Contributed "+q.amount()+"; persistent community milestone and personal recognition saved.";
        }catch(IOException|RuntimeException e){
            if(!checkpointStarted){for(int i=0;i<36;i++)p.getInventory().setItem(i,q.before().get(i).copy());for(var key:new HashSet<>(p.getPersistentData().getAllKeys()))p.getPersistentData().remove(key);p.getPersistentData().merge(old);}s.quarantine();p.connection.disconnect(Component.literal("Milestone interrupted. Restore matching native inventory and kingdom checkpoints before continuing."));throw new IllegalStateException("Milestone paused for safe recovery",e);
        }
    }
    public static List<CommerceView.Entry> entries(ServerPlayer p){
        int tier=attainedTier(p.server);if(tier>=6)return List.of(new CommerceView.Entry("alpha_finale","Tier 6 reached","Construct and maintain the Aerodrome for aircraft access. Tier 7–12: Coming Soon — Future Update. Higher historical progress remains preserved.",0,"COMPLETE"));
        var s=ledger(p.server,tier);boolean ready;try{requirePlayer(p,tier);ready=true;}catch(IllegalStateException e){ready=false;}
        List<CommerceView.Entry> rows=new ArrayList<>();rows.add(new CommerceView.Entry("early_premises","Community foundations","Requires completed intact Town Hall, Builder's Hut/Aldric and Royal Bank. Alpha costs are versioned configurable defaults, pinned once the journal is created.",0,ready?"COMPLETE":"LOCKED"));
        final boolean available=ready;
        s.requirements().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->rows.add(new CommerceView.Entry(e.getKey(),new ItemStack(item(e.getKey())).getHoverName().getString(),s.total(e.getKey())+" / "+e.getValue()+" shared toward Tier "+(tier+1)+". Your contribution: "+s.personal(p.getUUID(),e.getKey())+". Permanent consumption; equal normalized category weight.",0,s.total(e.getKey())==e.getValue()?"COMPLETE":available?"CONTRIBUTE":"LOCKED")));
        return List.copyOf(rows);
    }
    private static Path playerFile(MinecraftServer s,UUID player){return s.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player+".dat");}
    private static String key(int tier){EarlyTierRules.defaults(tier);return "KingdomEarlyTier"+tier+"Receipt";}
    private static TierTwoLedger.Receipt receipt(CompoundTag data,int tier){if(data.contains(Player.PERSISTED_NBT_TAG)&&!data.contains(Player.PERSISTED_NBT_TAG,Tag.TAG_COMPOUND))throw new IllegalStateException("Malformed persisted data");var root=data.getCompound(Player.PERSISTED_NBT_TAG);if(!root.contains(key(tier)))return TierTwoLedger.Receipt.empty();if(!root.contains(key(tier),Tag.TAG_COMPOUND))throw new IllegalStateException("Malformed milestone receipt");var t=root.getCompound(key(tier));if(!t.hasUUID("store")||!t.contains("sequence",Tag.TAG_LONG))throw new IllegalStateException("Malformed milestone receipt");return new TierTwoLedger.Receipt(t.getUUID("store"),t.getLong("sequence"));}
    private static void putReceipt(ServerPlayer p,int tier,TierTwoLedger.Receipt receipt){var root=p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).copy();var t=new CompoundTag();t.putUUID("store",receipt.store());t.putLong("sequence",receipt.sequence());root.put(key(tier),t);p.getPersistentData().put(Player.PERSISTED_NBT_TAG,root);}
    public static void verifyAll(ServerPlayer p)throws IOException {TierTwoService.verifyReceipt(p,TierTwoService.ledger(p.server));if(TierTwoService.ledger(p.server).complete())for(int tier=3;tier<=Math.min(attainedTier(p.server),5);tier++)verifyReceipt(p,ledger(p.server,tier),tier);}
    private static void verifyReceipt(ServerPlayer p,TierTwoLedger s,int tier)throws IOException {
        long expected=s.expectedReceipt(p.getUUID());var loaded=receipt(p.getPersistentData(),tier);if(loaded.sequence()==expected&&(expected==0&&loaded.store()==null||s.id().equals(loaded.store())))return;
        var disk=AtomicNbt.read(playerFile(p.server,p.getUUID()));var saved=receipt(disk.getCompound("NeoForgeData"),tier);if(!disk.hasUUID("UUID")||!disk.getUUID("UUID").equals(p.getUUID())||!s.id().equals(saved.store())||saved.sequence()!=expected||loaded.sequence()>expected)throw new IOException("Milestone inventory restore mismatch");p.getInventory().load(disk.getList("Inventory",Tag.TAG_COMPOUND));putReceipt(p,tier,saved);p.inventoryMenu.sendAllDataToRemote();
    }
    private static void checkpoint(ServerPlayer p)throws IOException {
        var inventory=p.getInventory().save(new ListTag());var data=p.saveWithoutId(new CompoundTag());if(!data.getList("Inventory",Tag.TAG_COMPOUND).equals(inventory))throw new IOException("Native inventory serialization changed");var file=playerFile(p.server,p.getUUID());if(Files.exists(file))AtomicNbt.write(file.resolveSibling(p.getUUID()+".dat_old"),AtomicNbt.read(file));AtomicNbt.write(file,data);net.neoforged.neoforge.event.EventHooks.firePlayerSavingEvent(p,file.getParent().toFile(),p.getStringUUID());var saved=AtomicNbt.read(file);if(!saved.getCompound("NeoForgeData").equals(data.getCompound("NeoForgeData"))||!saved.getList("Inventory",Tag.TAG_COMPOUND).equals(inventory)||!p.getInventory().save(new ListTag()).equals(inventory))throw new IOException("Native player checkpoint verification failed");
    }
}
