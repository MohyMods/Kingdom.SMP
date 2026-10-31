package com.mohymods.kingdomsmp.progression;

import com.google.gson.*;
import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.network.CommerceView;
import com.mohymods.kingdomsmp.persistence.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

/** Native Tier 2 donations. New versioned defaults do not reinterpret Tier 1 or legacy records. */
public final class TierTwoService {
    public enum Resource {
        LOGS("minecraft:logs","Logs",null,2048,"timber"), COBBLESTONE("minecraft:cobblestone","Cobblestone",Items.COBBLESTONE,3072,"bulk"),
        WHEAT("minecraft:wheat","Wheat",Items.WHEAT,768,"farming"), IRON("minecraft:iron_ingot","Iron ingots",Items.IRON_INGOT,192,"refined"),
        COPPER("minecraft:copper_ingot","Copper ingots",Items.COPPER_INGOT,384,"refined"), GLASS("minecraft:glass","Glass",Items.GLASS,256,"masonry"),
        BRICK("minecraft:brick","Bricks",Items.BRICK,384,"masonry"), LEATHER("minecraft:leather","Leather",Items.LEATHER,64,"trade");
        public final String id,title,category;public final Item item;public final int defaultQuantity;
        Resource(String id,String title,Item item,int quantity,String category){this.id=id;this.title=title;this.item=item;defaultQuantity=quantity;this.category=category;}
        public static Resource byId(String id){return Arrays.stream(values()).filter(r->r.id.equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown Tier 2 resource."));}
    }
    public record Quote(UUID nonce,String text){ }
    private record Offer(UUID nonce,long revision,Resource resource,int amount,long issued,List<ItemStack> inventory){ }
    private static final String RECEIPT="KingdomTierTwoReceipt";
    private static final Map<MinecraftServer,TierTwoLedger> STORES=new WeakHashMap<>();
    private static final Map<ServerPlayer,Offer> OFFERS=new WeakHashMap<>();
    private TierTwoService(){ }
    public static void initialize(MinecraftServer server){STORES.remove(server);OFFERS.keySet().removeIf(p->p.server==server);if(royal(server))ledger(server);}
    private static boolean royal(MinecraftServer server){return WorldProgression.get(server).mode()==WorldProgression.Mode.ROYAL_HALL;}
    public static TierTwoLedger ledger(MinecraftServer server){
        if(!royal(server))throw new IllegalStateException("Legacy progression remains unchanged; this new milestone is not applied to Lodge saves.");
        var current=STORES.get(server);if(current!=null)return current;
        try{
            var s=TierTwoLedger.open(server.getWorldPath(LevelResource.ROOT).resolve("data"),loadRequirements(server));
            if(s.pending().isPresent()){var d=s.pending().orElseThrow().donation();Path file=playerFile(server,d.player());s.recover(Files.exists(file)?receipt(AtomicNbt.read(file)):TierTwoLedger.Receipt.empty());}
            STORES.put(server,s);return s;
        }catch(IOException e){throw new IllegalStateException("Tier 2 costs or inventory checkpoint requires matching-backup recovery.",e);}
    }
    /** No calls to ContributionService.kingdom here: root composes this with existing attained tiers. */
    public static int attainedTier(MinecraftServer server){return royal(server)&&ledger(server).complete()?3:1;}
    private static Map<String,Integer> loadRequirements(MinecraftServer server)throws IOException{
        Path file=server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-tier-two.json");
        if(!Files.exists(file)){
            var root=new JsonObject();root.addProperty("schema",1);root.addProperty("status","versioned_defaults_authorized_by_end_to_end_directive");
            var rows=new JsonObject();for(var r:Resource.values())rows.addProperty(r.id,r.defaultQuantity);root.add("quantities",rows);
            Files.createDirectories(file.getParent());Files.writeString(file,new GsonBuilder().setPrettyPrinting().create().toJson(root),StandardOpenOption.CREATE_NEW);
        }
        try{
            if(Files.size(file)>65536)throw new IOException("Tier 2 configuration too large");
            var root=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if(root.get("schema").getAsBigDecimal().intValueExact()!=1)throw new IllegalArgumentException("Unsupported schema");var rows=root.getAsJsonObject("quantities");
            if(rows.size()!=Resource.values().length)throw new IllegalArgumentException("Expected exactly the eight safe Overworld resource definitions");
            Map<String,Integer> costs=new LinkedHashMap<>();for(var r:Resource.values()){var raw=rows.get(r.id);if(raw==null||!raw.isJsonPrimitive()||!raw.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Missing cost");int amount=raw.getAsBigDecimal().intValueExact();if(amount<1||amount>1_000_000)throw new IllegalArgumentException("Cost out of bounds");costs.put(r.id,amount);}return costs;
        }catch(RuntimeException e){throw new IOException("Invalid bounded Tier 2 schema-1 configuration",e);}
    }
    private static void requireSettlement(MinecraftServer server){
        if(!royal(server))throw new IllegalStateException("Legacy Lodge progression is preserved.");
        if(!ContributionService.ledger(server).progress().complete())throw new IllegalStateException("Complete First Hearth to reach Tier 2 first.");
        var hut=BuilderHutSavedData.get(server).hut().orElseThrow(()->new IllegalStateException("Establish the Builder's Hut first."));
        var hall=ConstructionService.lodge(server);
        if(hut.phase()!=BuilderHutSavedData.Phase.ACTIVE||hall==null||!ConstructionService.hall(hall)||hall.phase()!=ConstructionStore.Phase.COMPLETE
                ||!ConstructionService.structureIntact(server.overworld(),hall))throw new IllegalStateException("Complete and maintain the Town Hall before this milestone.");
    }
    private static void requirePlayer(ServerPlayer player){
        requireSettlement(player.server);var type=player.gameMode.getGameModeForPlayer();
        if(!player.isAlive()||type!=GameType.SURVIVAL&&type!=GameType.ADVENTURE||player.containerMenu!=player.inventoryMenu||!player.containerMenu.getCarried().isEmpty())throw new IllegalStateException("Contribute alive in Survival/Adventure with containers closed.");
    }
    public static UUID handleReview(ServerPlayer player,String resourceID,int requested){
        requirePlayer(player);if(requested<1||requested>2304)throw new IllegalArgumentException("Review at most a main inventory of supplies.");
        var r=Resource.byId(resourceID);var s=ledger(player.server);
        if(s.quarantined()||s.pending().isPresent())throw new IllegalStateException("Tier 2 contributions paused for recovery.");
        try{verifyReceipt(player,s);}catch(IOException e){throw new IllegalStateException("Your Tier 2 inventory receipt needs recovery.",e);}
        int amount=Math.min(requested,Math.min(eligible(player,r),s.requirements().get(r.id)-s.total(r.id)));
        if(amount<=0)throw new IllegalStateException("No eligible supplies, or that target is complete.");
        var offer=new Offer(UUID.randomUUID(),s.revision(),r,amount,System.nanoTime(),inventory(player));OFFERS.put(player,offer);return offer.nonce();
    }
    public static Optional<Quote> quote(ServerPlayer player){
        var offer=OFFERS.get(player);if(offer==null)return Optional.empty();
        var s=ledger(player.server);if(System.nanoTime()-offer.issued()>30_000_000_000L||s.revision()!=offer.revision()){OFFERS.remove(player);return Optional.empty();}
        return Optional.of(new Quote(offer.nonce(),"Contribute exactly "+offer.amount()+" "+offer.resource().title+". Accepted items are permanently consumed. Resources are shared; each category has equal normalized score weight. Review expires after 30 seconds."));
    }
    public static void close(ServerPlayer player){OFFERS.remove(player);}
    public static String confirm(ServerPlayer player,UUID nonce){
        requirePlayer(player);var offer=OFFERS.remove(player);var s=ledger(player.server);
        if(offer==null||!offer.nonce().equals(nonce)||System.nanoTime()-offer.issued()>30_000_000_000L)throw new IllegalStateException("Tier 2 contribution review expired.");
        if(s.quarantined()||s.pending().isPresent()||s.revision()!=offer.revision()||!sameInventory(player,offer.inventory()))throw new IllegalStateException("Inventory or milestone changed. Review again.");
        boolean before=s.complete();CompoundTag old=player.getPersistentData().copy();boolean checkpointStarted=false;
        try{
            verifyReceipt(player,s);checkpoint(player);
            var pending=s.prepare(offer.nonce(),player.getUUID(),player.getGameProfile().getName(),offer.resource().id,offer.amount());
            int remaining=offer.amount();for(int slot=0;slot<36&&remaining>0;slot++){var stack=player.getInventory().getItem(slot);if(matches(stack,offer.resource())){int n=Math.min(remaining,stack.getCount());stack.shrink(n);remaining-=n;}}
            if(remaining!=0)throw new IllegalStateException("Inventory changed during contribution.");
            putReceipt(player,new TierTwoLedger.Receipt(s.id(),pending.donation().sequence()));checkpointStarted=true;checkpoint(player);
            if(!s.recover(receipt(AtomicNbt.read(playerFile(player.server,player.getUUID())))))throw new IOException("Missing native Tier 2 removal receipt");
            player.getInventory().setChanged();player.inventoryMenu.sendAllDataToRemote();
            LogUtils.getLogger().info("TIER_TWO_CONTRIBUTION operation={} player={} resource={} count={} revision={}",offer.nonce(),player.getUUID(),offer.resource().id,offer.amount(),s.revision());
            if(!before&&s.complete())announce(player.server,s);
            return "Contributed "+offer.amount()+" "+offer.resource().title+". Shared progress and personal recognition saved.";
        }catch(IOException|RuntimeException failure){
            if(!checkpointStarted){for(int i=0;i<36;i++)player.getInventory().setItem(i,offer.inventory().get(i).copy());for(String key:new HashSet<>(player.getPersistentData().getAllKeys()))player.getPersistentData().remove(key);player.getPersistentData().merge(old);}
            s.quarantine();LogUtils.getLogger().error("Tier 2 contribution interrupted; recovery required {}",offer.nonce(),failure);player.connection.disconnect(Component.literal("Tier 2 contribution interrupted. Restore matching player and kingdom checkpoints before retrying."));throw new IllegalStateException("Tier 2 contribution paused for safe recovery.",failure);
        }
    }
    public static List<CommerceView.Entry> entries(ServerPlayer player){
        if(!royal(player.server))return List.of(new CommerceView.Entry("legacy","Preserved legacy progression","This profile retains its Lodge, history and entitlements. New Tier 2 costs are not applied retroactively.",0,"LOCKED"));
        var s=ledger(player.server);boolean ready;
        try{requireSettlement(player.server);ready=true;}catch(IllegalStateException denied){ready=false;}
        List<CommerceView.Entry> out=new ArrayList<>();
        out.add(new CommerceView.Entry("road","Kingdom road",s.road().isPresent()?"Operator-approved connection between the Hut and Town Hall. Approval is permanent and audited.":"An operator must inspect and approve a road connecting the Hut and Town Hall with /kingdom admin milestone road confirm <reason>.",0,s.road().isPresent()?"COMPLETE":"LOCKED"));
        for(var r:Resource.values())out.add(new CommerceView.Entry(r.id,r.title,s.total(r.id)+" / "+s.requirements().get(r.id)+" shared · You contributed "+s.personal(player.getUUID(),r.id)+" · "+r.category+". All resources are available in the Overworld before Tier 3.",0,s.total(r.id)==s.requirements().get(r.id)?"COMPLETE":ready?"CONTRIBUTE":"LOCKED"));
        if(s.complete()){var winner=s.winner().orElseThrow();out.add(new CommerceView.Entry("recognition","Tier 3 attained",winner.name()+" is the highest normalized Tier 2 contributor. New building plans are purchased through the Architect; no extra reward blueprint is created.",0,"COMPLETE"));}
        return List.copyOf(out);
    }
    public static void login(PlayerEvent.PlayerLoggedInEvent event){if(!(event.getEntity() instanceof ServerPlayer player)||!royal(player.server))return;try{verifyReceipt(player,ledger(player.server));}catch(IOException|RuntimeException e){LogUtils.getLogger().error("Tier 2 loaded inventory mismatch {}",player.getUUID(),e);player.connection.disconnect(Component.literal("Tier 2 inventory receipt needs matching-backup recovery."));}}
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer player)close(player);}
    private static void announce(MinecraftServer server,TierTwoLedger s){var winner=s.winner().orElseThrow();server.getPlayerList().broadcastSystemMessage(Component.literal("Roads of Commerce is complete! Kingdom Tier 3 attained. Highest contributor: "+winner.name()+". Royal Bank, Bounty Hall and Fisherman's Wharf unlock gates reached; each service still requires its implemented building."),false);}
    public static LiteralArgumentBuilder<CommandSourceStack> commands(){return Commands.literal("milestone")
        .then(Commands.literal("inspect").executes(c->{if(!royal(c.getSource().getServer())){c.getSource().sendSuccess(()->Component.literal("Legacy progression preserved; new Tier 2 milestone inactive."),false);return 1;}var s=ledger(c.getSource().getServer());c.getSource().sendSuccess(()->Component.literal("Tier 2 journal "+s.id()+" revision "+s.revision()+" · road "+s.road().isPresent()+" · complete "+s.complete()+" · pending "+s.pending().isPresent()+" · quarantined "+s.quarantined()),false);for(var r:Resource.values())c.getSource().sendSuccess(()->Component.literal(r.title+": "+s.total(r.id)+" / "+s.requirements().get(r.id)),false);return 1;}))
        .then(Commands.literal("road").requires(source->source.hasPermission(4))
            .executes(c->{c.getSource().sendSuccess(()->Component.literal("Inspect the actual road linking the completed Hut and Town Hall, then append confirm <reason>. No terrain is changed."),false);return 1;})
            .then(Commands.literal("confirm").then(Commands.argument("reason",StringArgumentType.greedyString()).executes(c->{
                try{var server=c.getSource().getServer();requireSettlement(server);var s=ledger(server);boolean before=s.complete();UUID actor=c.getSource().getEntity()==null?new UUID(0,0):c.getSource().getEntity().getUUID();boolean changed=s.approveRoad(actor,StringArgumentType.getString(c,"reason"));if(!before&&s.complete())announce(server,s);LogUtils.getLogger().info("TIER_TWO_ROAD_APPROVAL actor={} changed={} reason={}",actor,changed,StringArgumentType.getString(c,"reason"));c.getSource().sendSuccess(()->Component.literal(changed?"Road approval saved and audited.":"Road was already approved; original approval retained."),false);return 1;}
                catch(IOException|IllegalArgumentException|IllegalStateException e){c.getSource().sendFailure(Component.literal(e.getMessage()));return 0;}
            }))));}
    public static boolean matches(ItemStack stack,Resource r){return !stack.isEmpty()&&(r==Resource.LOGS?stack.is(ItemTags.LOGS):stack.is(r.item))&&ItemStack.isSameItemSameComponents(stack,new ItemStack(stack.getItem()));}
    public static int eligible(ServerPlayer p,Resource r){int n=0;for(int i=0;i<36;i++)if(matches(p.getInventory().getItem(i),r))n+=p.getInventory().getItem(i).getCount();return n;}
    private static List<ItemStack> inventory(ServerPlayer p){List<ItemStack> out=new ArrayList<>();for(int i=0;i<36;i++)out.add(p.getInventory().getItem(i).copy());return out;}
    private static boolean sameInventory(ServerPlayer p,List<ItemStack> before){for(int i=0;i<36;i++)if(!ItemStack.matches(p.getInventory().getItem(i),before.get(i)))return false;return true;}
    private static Path playerFile(MinecraftServer s,UUID player){return s.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player+".dat");}
    private static TierTwoLedger.Receipt loadedReceipt(ServerPlayer p){return fromPersistent(p.getPersistentData());}
    private static TierTwoLedger.Receipt receipt(CompoundTag player){return fromPersistent(player.getCompound("NeoForgeData"));}
    private static TierTwoLedger.Receipt fromPersistent(CompoundTag data){
        if(data.contains(Player.PERSISTED_NBT_TAG)&&!data.contains(Player.PERSISTED_NBT_TAG,Tag.TAG_COMPOUND))throw new IllegalStateException("Invalid persisted player data");var root=data.getCompound(Player.PERSISTED_NBT_TAG);if(!root.contains(RECEIPT))return TierTwoLedger.Receipt.empty();if(!root.contains(RECEIPT,Tag.TAG_COMPOUND))throw new IllegalStateException("Invalid Tier 2 receipt");var tag=root.getCompound(RECEIPT);if(!tag.hasUUID("store")||!tag.contains("sequence",Tag.TAG_LONG))throw new IllegalStateException("Malformed Tier 2 receipt");return new TierTwoLedger.Receipt(tag.getUUID("store"),tag.getLong("sequence"));
    }
    private static void putReceipt(ServerPlayer p,TierTwoLedger.Receipt receipt){var root=p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).copy();var tag=new CompoundTag();tag.putUUID("store",receipt.store());tag.putLong("sequence",receipt.sequence());root.put(RECEIPT,tag);p.getPersistentData().put(Player.PERSISTED_NBT_TAG,root);}
    public static void verifyReceipt(ServerPlayer p,TierTwoLedger s)throws IOException{
        long expected=s.expectedReceipt(p.getUUID());var loaded=loadedReceipt(p);if(loaded.sequence()==expected&&(expected==0&&loaded.store()==null||s.id().equals(loaded.store())))return;
        var disk=AtomicNbt.read(playerFile(p.server,p.getUUID()));var saved=receipt(disk);if(!disk.hasUUID("UUID")||!disk.getUUID("UUID").equals(p.getUUID())||!s.id().equals(saved.store())||saved.sequence()!=expected||loaded.sequence()>expected)throw new IOException("Tier 2 inventory restore mismatch");p.getInventory().load(disk.getList("Inventory",Tag.TAG_COMPOUND));putReceipt(p,saved);p.inventoryMenu.sendAllDataToRemote();
    }
    private static void checkpoint(ServerPlayer p)throws IOException{
        var expected=p.getInventory().save(new ListTag());var serialized=p.saveWithoutId(new CompoundTag());if(!serialized.getList("Inventory",Tag.TAG_COMPOUND).equals(expected))throw new IOException("Inventory serialization changed during Tier 2 donation");var file=playerFile(p.server,p.getUUID());if(Files.exists(file))AtomicNbt.write(file.resolveSibling(p.getUUID()+".dat_old"),AtomicNbt.read(file));AtomicNbt.write(file,serialized);net.neoforged.neoforge.event.EventHooks.firePlayerSavingEvent(p,file.getParent().toFile(),p.getStringUUID());var saved=AtomicNbt.read(file);if(!receipt(saved).equals(loadedReceipt(p))||!saved.getList("Inventory",Tag.TAG_COMPOUND).equals(expected)||!p.getInventory().save(new ListTag()).equals(expected))throw new IOException("Tier 2 player checkpoint verification failed");
    }
}
