package com.mohymods.kingdomsmp.economy;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.commerce.CommerceCatalog;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.network.*;
import com.mohymods.kingdomsmp.npc.ReeveEntity;
import com.mohymods.kingdomsmp.persistence.*;
import com.mohymods.kingdomsmp.persistence.ConstructionStore.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.*;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

/** Native inventory checkpoint adapter and server-only storefront coordinator. */
public final class CommerceService {
    private static final String RECEIPT="KingdomEconomyReceipt";
    private static final Map<MinecraftServer,EconomyStore> STORES=new WeakHashMap<>();
    private static final Map<MinecraftServer,CommerceCatalog> CATALOGS=new WeakHashMap<>();
    private static final Map<ServerPlayer,Quote> QUOTES=new WeakHashMap<>();
    private static final Map<ServerPlayer,UUID> COUNTERS=new WeakHashMap<>();
    private record Quote(UUID nonce,long revision,int tab,String selected,int quantity,long amount,long period,long issued,List<ItemStack> inventory) { }
    private CommerceService() { }
    public static void initialize(MinecraftServer server){STORES.remove(server);CATALOGS.remove(server);QUOTES.keySet().removeIf(p->p.server==server);COUNTERS.keySet().removeIf(p->p.server==server);store(server);}
    public static CommerceCatalog catalog(MinecraftServer server){
        var c=CATALOGS.get(server);if(c!=null)return c;
        try{c=CommerceCatalog.load(server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-commerce.json"));CATALOGS.put(server,c);return c;}
        catch(IOException e){throw new IllegalStateException("Commerce configuration requires recovery; no transactions enabled.",e);}
    }
    public static EconomyStore store(MinecraftServer server){
        var s=STORES.get(server);
        if(s==null)try{
            catalog(server);s=EconomyStore.open(server.getWorldPath(LevelResource.ROOT).resolve("data"));
            if(s.pendingTransfer().isPresent()){var p=s.pendingTransfer().orElseThrow();Path f=playerFile(server,p.player());s.recoverTransfer(Files.exists(f)?receipt(AtomicNbt.read(f)):EconomyStore.Receipt.empty());}
            if(s.pendingSale().isPresent()){var p=s.pendingSale().orElseThrow();Path f=playerFile(server,p.player());s.recoverSale(Files.exists(f)?receipt(AtomicNbt.read(f)):EconomyStore.Receipt.empty());}
            STORES.put(server,s);
        }catch(IOException e){throw new IllegalStateException("Economy requires matching player/journal recovery.",e);}
        materialize(server,s);return s;
    }
    private static void materialize(MinecraftServer server,EconomyStore store){
        if(store.quarantined())throw new IllegalStateException("Economy quarantined; restart and inspect matching saves.");
        var construction=ConstructionService.store(server);
        for(var p:store.purchases())try{
            var b=catalog(server).building(p.building());
            construction.award("market:"+b.id(),net.minecraft.resources.ResourceLocation.parse(b.id()),1,p.buyer(),p.buyerName());
        }catch(IOException e){throw new IllegalStateException("Purchase is committed; blueprint delivery awaits storage recovery. Do not buy again.",e);}
    }
    public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(!(event.getEntity() instanceof ServerPlayer p))return;
        try{verifyReceipt(p,store(p.server));}
        catch(IOException|RuntimeException e){LogUtils.getLogger().error("Economy player checkpoint mismatch {}",p.getUUID(),e);p.connection.disconnect(Component.literal("Economy checkpoint needs recovery. Restore matching player and kingdom data."));}
    }
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p){QUOTES.remove(p);COUNTERS.remove(p);BankService.close(p);com.mohymods.kingdomsmp.progression.TierTwoService.close(p);}}
    public static void openResident(ServerPlayer player,ReeveEntity resident){
        if(!ConstructionService.registeredReeve(resident)||!player.hasLineOfSight(resident)||player.distanceToSqr(resident)>64)throw new IllegalStateException("Visit the registered resident at the completed building.");
        COUNTERS.put(player,resident.getUUID());var project=ConstructionService.projectForResident(resident);int tab=project!=null&&project.blueprint().equals(RoyalBankBlueprint.ID)?3:1;String greeting=project==null?"":resident.getName().getString()+" — "+BlueprintRegistry.greeting(project.blueprint());net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,snapshot(player,0,tab,greeting).withOpen(true));
    }
    private static void requireCounter(ServerPlayer player){
        if(!player.isAlive()||player.isSpectator()||player.serverLevel()!=player.server.overworld()||player.containerMenu!=player.inventoryMenu||!player.containerMenu.getCarried().isEmpty()
                ||player.gameMode.getGameModeForPlayer()!=GameType.SURVIVAL&&player.gameMode.getGameModeForPlayer()!=GameType.ADVENTURE)throw new IllegalStateException("Trade alive in Survival/Adventure with containers closed.");
        if(ContributionService.kingdom(player.server).attainedTier()<2)throw new IllegalStateException("Royal commerce opens at Tier 2.");
        UUID selected=COUNTERS.get(player);var home=ConstructionService.lodge(player.server);
        if(selected==null&&home!=null)selected=home.npc();
        var entity=selected==null?null:player.serverLevel().getEntity(selected);
        if(!(entity instanceof ReeveEntity npc)||!npc.isAlive()||!ConstructionService.registeredReeve(npc)||player.distanceToSqr(npc)>64||!player.hasLineOfSight(npc))throw new IllegalStateException("Visit Elara or an assigned resident within eight blocks and clear sight.");
        var p=ConstructionService.projectForResident(npc);
        if(p==null||!ConstructionService.structureIntact(player.serverLevel(),p))throw new IllegalStateException("The service building is damaged or unloaded.");
        var hall=ConstructionService.lodge(player.server);
        if(hall==null||hall.phase()!=Phase.COMPLETE||hall.npcDead()||!ConstructionService.structureIntact(player.serverLevel(),hall))throw new IllegalStateException("Complete and maintain the kingdom headquarters before using commerce.");
    }
    public static CommerceView handle(ServerPlayer player,CommerceRequest request){
        if(request.action()==4){QUOTES.remove(player);COUNTERS.remove(player);BankService.close(player);com.mohymods.kingdomsmp.progression.TierTwoService.close(player);return snapshot(player,request.requestId(),request.tab(),"");}
        String notice="";
        try{
            if(request.tab()>=3){
                if(request.action()==0){BankService.close(player);com.mohymods.kingdomsmp.progression.TierTwoService.close(player);}
                else if(request.tab()==3&&request.action()==1)notice=BankService.review(player,request.selected(),request.quantity()).text();
                else if(request.tab()==3&&request.action()==2)notice=BankService.confirm(player,request.review());
                else if(request.tab()==4&&request.action()==1){com.mohymods.kingdomsmp.progression.TierTwoService.handleReview(player,request.selected(),request.quantity());notice="Review your exact shared contribution.";}
                else if(request.tab()==4&&request.action()==2)notice=com.mohymods.kingdomsmp.progression.TierTwoService.confirm(player,request.review());
                else throw new IllegalArgumentException("Unsupported service action.");
                return snapshot(player,request.requestId(),request.tab(),notice);
            }
            if(request.action()==0)QUOTES.remove(player);
            else if(request.action()==1)notice=review(player,request.tab(),request.selected(),request.quantity());
            else if(request.action()==2)notice=confirm(player,request.review());
            else if(request.action()==3){
                requireCounter(player);var s=store(player.server);var mail=mail(player,s,request.selected());
                if(mail.kind()!=EconomyStore.MailKind.BLUEPRINT)throw new IllegalStateException("Choose a blueprint delivery.");
                var p=ConstructionService.store(player.server).reward("market:"+mail.building()).orElseThrow(()->new IllegalStateException("Committed blueprint awaits recovery."));
                if(!p.owner().equals(player.getUUID()))throw new IllegalStateException("Blueprint delivery belongs to another player.");
                QUOTES.remove(player);ConstructionService.select(player,p.id());ConstructionNetworking.open(player,"Purchased plan · "+catalog(player.server).building(mail.building()).founder()+". Claim or recover the authenticated item here.");
            }else throw new IllegalArgumentException("Unknown commerce action.");
        }catch(IOException e){store(player.server).quarantine();throw new IllegalStateException("Commerce storage requires recovery before retrying.",e);}
        return snapshot(player,request.requestId(),request.tab(),notice);
    }
    private static String review(ServerPlayer player,int tab,String selected,int requested)throws IOException{
        requireCounter(player);var s=store(player.server);verifyReceipt(player,s);
        if(s.quarantined()||s.pendingSale().isPresent()||s.pendingTransfer().isPresent())throw new IllegalStateException("Commerce paused for recovery.");
        long amount;int quantity=1;long day=day();
        if(tab==0){var b=catalog(player.server).building(selected);if(ContributionService.kingdom(player.server).attainedTier()<b.tier())throw new IllegalStateException("Requires kingdom Tier "+b.tier()+".");if(s.purchase(selected).isPresent())throw new IllegalStateException("Sold out: one founder already owns this entitlement.");amount=b.price();if(s.balance(player.getUUID())<amount)throw new IllegalStateException("Insufficient coins. Earn coins through Essentials.");}
        else if(tab==1){var supply=catalog(player.server).supply(selected);var used=s.usage(day,key(supply.id()),player.getUUID());long max=Math.min(supply.quota()-used.resourceQuantity(),Math.min((catalog(player.server).budget()-used.sharedSpent())/supply.price(),(catalog(player.server).allowance()-used.playerProceeds())/supply.price()));quantity=(int)Math.min(requested,Math.min(eligible(player,supply),Math.max(0,max)));if(quantity<1)throw new IllegalStateException("No eligible items, or today's buying allowance/quota is exhausted.");amount=Math.multiplyExact(quantity,supply.price());}
        else{var mail=mail(player,s,selected);if(mail.kind()!=EconomyStore.MailKind.COINS||mail.claimed())throw new IllegalStateException("This delivery has no unclaimed coins.");amount=mail.amount();}
        QUOTES.put(player,new Quote(UUID.randomUUID(),s.revision(),tab,selected,quantity,amount,day,System.nanoTime(),inventory(player)));return "Review the exact transaction; nothing has been changed yet.";
    }
    public static String confirm(ServerPlayer player,UUID nonce)throws IOException{
        requireCounter(player);Quote q=QUOTES.remove(player);var s=store(player.server);
        if(q==null||!q.nonce().equals(nonce)||System.nanoTime()-q.issued()>30_000_000_000L)throw new IllegalStateException("Review expired. Review the transaction again.");
        if(s.revision()!=q.revision()||day()!=q.period())throw new IllegalStateException("Stock, balance or trading day changed. Review again.");
        if(q.tab()==0){var b=catalog(player.server).building(q.selected());if(ContributionService.kingdom(player.server).attainedTier()<b.tier()||b.price()!=q.amount())throw new IllegalStateException("Building eligibility changed.");
            s.purchase(q.nonce(),player.getUUID(),player.getGameProfile().getName(),b.id(),b.price());
            try{materialize(player.server,s);}catch(IllegalStateException e){return "Purchase committed. Blueprint delivery awaits recovery; no second payment is required.";}
            player.server.getPlayerList().broadcastSystemMessage(Component.literal(player.getGameProfile().getName()+" is "+b.founder()+". "+b.title()+" blueprint sold out."),false);
            return "Purchased "+b.title()+". Your unique plan is in Mailbox; materials are included.";
        }
        if(q.tab()==2){var m=mail(player,s,q.selected());s.claimCoins(player.getUUID(),m.id());return "Claimed "+m.amount()+" coins to your available purse.";}
        var supply=catalog(player.server).supply(q.selected());
        if(!sameInventory(player,q.inventory()))throw new IllegalStateException("Inventory changed. Review the sale again.");
        verifyReceipt(player,s);CompoundTag old=player.getPersistentData().copy();boolean checkpointStarted=false;
        try{
            checkpoint(player);var pending=s.prepareSale(q.nonce(),player.getUUID(),key(supply.id()),q.quantity(),q.amount(),q.period(),catalog(player.server).budget(),supply.quota(),catalog(player.server).allowance());
            int remaining=q.quantity();for(int i=0;i<36&&remaining>0;i++){var stack=player.getInventory().getItem(i);if(matches(stack,supply)){int n=Math.min(remaining,stack.getCount());stack.shrink(n);remaining-=n;}}
            if(remaining!=0)throw new IllegalStateException("Inventory changed during sale.");
            putReceipt(player,new EconomyStore.Receipt(s.id(),pending.sequence()));checkpointStarted=true;checkpoint(player);
            if(!s.recoverSale(receipt(AtomicNbt.read(playerFile(player.server,player.getUUID())))))throw new IOException("Native inventory receipt was not committed");
            player.getInventory().setChanged();player.inventoryMenu.sendAllDataToRemote();
            LogUtils.getLogger().info("ROYAL_RESOURCE_SALE operation={} player={} resource={} quantity={} coins={}",q.nonce(),player.getUUID(),q.selected(),q.quantity(),q.amount());
            return "Sold "+q.quantity()+" "+supply.title()+" for "+q.amount()+" coins. Payment saved.";
        }catch(IOException|RuntimeException failure){
            if(!checkpointStarted){for(int i=0;i<36;i++)player.getInventory().setItem(i,q.inventory().get(i).copy());for(var k:new HashSet<>(player.getPersistentData().getAllKeys()))player.getPersistentData().remove(k);player.getPersistentData().merge(old);}
            s.quarantine();player.connection.disconnect(Component.literal("Commerce interrupted. Inventory and payment require checkpoint recovery."));throw failure;
        }
    }
    public static CommerceView snapshot(ServerPlayer player,long requestId,int tab,String notice){
        var s=store(player.server);var c=catalog(player.server);long version=s.revision()+ConstructionService.store(player.server).revision();if(ConstructionService.royal(player.server))version+=com.mohymods.kingdomsmp.progression.TierTwoService.ledger(player.server).revision();int tier=ContributionService.kingdom(player.server).attainedTier();List<CommerceView.Entry> rows=new ArrayList<>();
        if(tab==3){var q=BankService.quote(player).orElse(null);return new CommerceView(requestId,version,false,tab,tier,s.balance(player.getUUID()),BankService.entries(player),q==null?CommerceRequest.NONE:q.nonce(),q==null?"":q.text(),notice);}
        if(tab==4){var q=com.mohymods.kingdomsmp.progression.TierTwoService.quote(player).orElse(null);return new CommerceView(requestId,version,false,tab,tier,s.balance(player.getUUID()),com.mohymods.kingdomsmp.progression.TierTwoService.entries(player),q==null?CommerceRequest.NONE:q.nonce(),q==null?"":q.text(),notice);}
        if(tab==0)for(var b:c.buildings()){
            var sold=s.purchase(b.id());String state=sold.isPresent()?"SOLD":tier<b.tier()?"LOCKED":"AVAILABLE";
            rows.add(new CommerceView.Entry(b.id(),b.title(),sold.isPresent()?"Founder: "+sold.get().buyerName()+". Unique entitlement sold.":"Tier "+b.tier()+" · Materials included · "+b.founder(),b.price(),state));
        }else if(tab==1)for(var item:c.supplies()){
            var used=s.usage(day(),key(item.id()),player.getUUID());rows.add(new CommerceView.Entry(item.id(),item.title(),"We buy plain items. Quota "+used.resourceQuantity()+" / "+item.quota()+". Shared spent "+used.sharedSpent()+" / "+c.budget()+"; your sales "+used.playerProceeds()+" / "+c.allowance()+" coins. Resets at UTC midnight.",item.price(),tier<2?"LOCKED":"SELL"));
        }else for(var m:s.mailbox(player.getUUID()).stream().filter(m->!m.claimed()).limit(32).toList()){
            boolean bp=m.kind()==EconomyStore.MailKind.BLUEPRINT;
            rows.add(new CommerceView.Entry(m.id().toString(),bp?c.building(m.building()).title()+" plan":"Coin delivery",bp?c.building(m.building()).founder()+" · Manage your permanent, recoverable plan. One building only.":m.description(),m.amount(),bp?"BLUEPRINT":"COINS"));
        }
        Quote q=QUOTES.get(player);if(q!=null&&(System.nanoTime()-q.issued()>30_000_000_000L||q.revision()!=s.revision()||q.tab()!=tab)){QUOTES.remove(player);q=null;}
        String details=q==null?"":q.tab()==0?"Purchase "+c.building(q.selected()).title()+" for "+q.amount()+" coins. One kingdom entitlement; the first confirmed buyer wins. Payment and stock commit together. Delivery goes to your mailbox.":q.tab()==1?"Sell exactly "+q.quantity()+" "+c.supply(q.selected()).title()+" for "+q.amount()+" coins. These items are permanently consumed. Your actual inventory and today's limits are rechecked. Review expires after 30 seconds.":"Claim "+q.amount()+" coins to your purse exactly once.";
        return new CommerceView(requestId,version,false,tab,tier,s.balance(player.getUUID()),rows,q==null?CommerceRequest.NONE:q.nonce(),details,notice);
    }
    private static EconomyStore.Mail mail(ServerPlayer player,EconomyStore s,String id){UUID uuid;try{uuid=UUID.fromString(id);}catch(IllegalArgumentException e){throw new IllegalArgumentException("Choose a valid private mailbox entry.");}return s.mailbox(player.getUUID()).stream().filter(m->m.id().equals(uuid)).findFirst().orElseThrow(()->new IllegalStateException("No such delivery in your mailbox."));}
    private static String key(String id){return "kingdom_smp:supplies/"+id;}
    private static long day(){return Math.floorDiv(System.currentTimeMillis(),86_400_000L);}
    public static boolean matches(ItemStack stack,CommerceCatalog.Supply s){return !stack.isEmpty()&&ItemStack.isSameItemSameComponents(stack,new ItemStack(s.item()));}
    public static int eligible(ServerPlayer p,CommerceCatalog.Supply s){int count=0;for(int i=0;i<36;i++)if(matches(p.getInventory().getItem(i),s))count+=p.getInventory().getItem(i).getCount();return count;}
    private static List<ItemStack> inventory(ServerPlayer p){List<ItemStack> out=new ArrayList<>();for(int i=0;i<36;i++)out.add(p.getInventory().getItem(i).copy());return out;}
    private static boolean sameInventory(ServerPlayer p,List<ItemStack> before){for(int i=0;i<36;i++)if(!ItemStack.matches(p.getInventory().getItem(i),before.get(i)))return false;return true;}
    public static Path playerFile(MinecraftServer s,UUID player){return s.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player+".dat");}
    public static EconomyStore.Receipt loadedReceipt(ServerPlayer p){return fromPersistent(p.getPersistentData());}
    public static EconomyStore.Receipt receipt(CompoundTag player){return fromPersistent(player.getCompound("NeoForgeData"));}
    private static EconomyStore.Receipt fromPersistent(CompoundTag data){
        if(data.contains(Player.PERSISTED_NBT_TAG)&&!data.contains(Player.PERSISTED_NBT_TAG,Tag.TAG_COMPOUND))throw new IllegalStateException("Malformed player persistent data");
        var root=data.getCompound(Player.PERSISTED_NBT_TAG);if(!root.contains(RECEIPT))return EconomyStore.Receipt.empty();
        if(!root.contains(RECEIPT,Tag.TAG_COMPOUND))throw new IllegalStateException("Malformed economy receipt");var r=root.getCompound(RECEIPT);
        if(!r.hasUUID("store")||!r.contains("sequence",Tag.TAG_LONG))throw new IllegalStateException("Malformed economy receipt");return new EconomyStore.Receipt(r.getUUID("store"),r.getLong("sequence"));
    }
    public static void putReceipt(ServerPlayer p,EconomyStore.Receipt receipt){var root=p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).copy();var t=new CompoundTag();t.putUUID("store",receipt.store());t.putLong("sequence",receipt.sequence());root.put(RECEIPT,t);p.getPersistentData().put(Player.PERSISTED_NBT_TAG,root);}
    public static void verifyReceipt(ServerPlayer p)throws IOException{verifyReceipt(p,store(p.server));}
    public static void verifyReceipt(ServerPlayer p,EconomyStore s)throws IOException{
        long expected=s.expectedReceipt(p.getUUID());var loaded=loadedReceipt(p);
        if(loaded.sequence()==expected&&(expected==0&&loaded.store()==null||s.id().equals(loaded.store())))return;
        var disk=AtomicNbt.read(playerFile(p.server,p.getUUID()));var saved=receipt(disk);
        if(!disk.hasUUID("UUID")||!disk.getUUID("UUID").equals(p.getUUID())||!s.id().equals(saved.store())||saved.sequence()!=expected||loaded.sequence()>expected)throw new IOException("Inventory/economy receipt mismatch");
        p.getInventory().load(disk.getList("Inventory",Tag.TAG_COMPOUND));putReceipt(p,saved);p.inventoryMenu.sendAllDataToRemote();
    }
    public static void checkpoint(ServerPlayer p)throws IOException{
        var inventory=p.getInventory().save(new ListTag());var serialized=p.saveWithoutId(new CompoundTag());
        if(!serialized.getList("Inventory",Tag.TAG_COMPOUND).equals(inventory))throw new IOException("Inventory serialization changed");
        var file=playerFile(p.server,p.getUUID());if(Files.exists(file))AtomicNbt.write(file.resolveSibling(p.getUUID()+".dat_old"),AtomicNbt.read(file));
        AtomicNbt.write(file,serialized);net.neoforged.neoforge.event.EventHooks.firePlayerSavingEvent(p,file.getParent().toFile(),p.getStringUUID());
        var saved=AtomicNbt.read(file);if(!receipt(saved).equals(loadedReceipt(p))||!saved.getList("Inventory",Tag.TAG_COMPOUND).equals(inventory)||!p.getInventory().save(new ListTag()).equals(inventory))throw new IOException("Inventory checkpoint verification failed");
    }
    public static LiteralArgumentBuilder<CommandSourceStack> commands(){return Commands.literal("commerce")
        .then(Commands.literal("open").executes(c->{CommerceNetworking.open(c.getSource().getPlayerOrException(),0);return 1;}))
        .then(Commands.literal("balance").executes(c->{var p=c.getSource().getPlayerOrException();c.getSource().sendSuccess(()->Component.literal("Available purse: "+store(p.server).balance(p.getUUID())+" coins."),false);return 1;}));}
    public static LiteralArgumentBuilder<CommandSourceStack> adminCommands(){return Commands.literal("economy")
        .then(Commands.literal("inspect").then(Commands.argument("player",EntityArgument.player()).executes(c->{var p=EntityArgument.getPlayer(c,"player");var s=store(p.server);c.getSource().sendSuccess(()->Component.literal("Economy "+s.id()+" revision "+s.revision()+" · "+p.getGameProfile().getName()+": "+s.balance(p.getUUID())+" coins · pending "+s.pendingSale().isPresent()+" · quarantined "+s.quarantined()),false);return 1;})))
        .then(Commands.literal("set").requires(s->s.hasPermission(4)).then(Commands.argument("player",EntityArgument.player()).then(Commands.argument("coins",LongArgumentType.longArg(0,EconomyStore.MAX_COINS))
            .executes(c->{c.getSource().sendSuccess(()->Component.literal("Review the exact player/coin amount; append confirm <reason> to change the real balance."),false);return 1;})
            .then(Commands.literal("confirm").then(Commands.argument("reason",StringArgumentType.greedyString()).executes(c->{var p=EntityArgument.getPlayer(c,"player");long amount=LongArgumentType.getLong(c,"coins");try{store(p.server).adminSet(UUID.randomUUID(),c.getSource().getEntity()==null?CommerceRequest.NONE:c.getSource().getEntity().getUUID(),p.getUUID(),amount,StringArgumentType.getString(c,"reason"));c.getSource().sendSuccess(()->Component.literal("Balance set and audited: "+amount+" coins."),false);return 1;}catch(IOException|IllegalArgumentException|IllegalStateException e){c.getSource().sendFailure(Component.literal(e.getMessage()));return 0;}}))))));}
}
