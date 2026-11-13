package com.mohymods.kingdomsmp.economy;

import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import net.minecraft.nbt.*;
import com.mohymods.kingdomsmp.profession.*;
import com.mohymods.kingdomsmp.equipment.FarmerGear;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.file.*;
import java.util.*;

/**
 * One canonical coin/stock/mail journal. A purchase is one durable operation,
 * never a debit followed by a second stock write. Construction records are
 * idempotent projections of Purchase; blueprint mail is an authority pointer,
 * not an exactly-once physical ItemStack delivery mechanism.
 *
 * Inventory sales deliberately use the contribution-style receipt protocol:
 * prepare, remove native items and durably save the player's new receipt,
 * then recoverSale(receipt). All other monetary mutations pause while prepared.
 * Root callers must authenticate players, prices, location and item identity.
 * AtomicNbt's process-crash contract applies; inconsistent backup restores fail
 * closed. History is bounded and never evicted to make old operations replayable.
 */
public final class EconomyStore {
    public static final long MAX_COINS=9_000_000_000_000L;
    public static final int MAX_OPERATIONS=50_000;
    public static final long TIMED_DURATION=86_400_000L;
    public static final long ANTI_SNIPE_WINDOW=120_000L;
    public static final int MAX_LISTING_JOURNAL_BYTES=8*1024*1024;
    public static final int MAX_JOURNAL_BYTES=12*1024*1024;
    public enum Kind { ADMIN_SET, QUEUE_COINS, CLAIM_COINS, PURCHASE, SALE, TRANSFER, COIN_TRANSFER, LIST_ITEM, BUY_ITEM, RETURN_ITEM, CLAIM_ITEM, LIST_TIMED_ITEM, BID_ITEM, SETTLE_TIMED, CREATE_ORDER, FILL_ORDER, CLOSE_ORDER, SHOP_BUY, PROFESSION_CHANGE, COMPLETE_CONTRACT, PROFESSION_QUEST, FARMER_DELIVERY, FARMER_INSPECTION, COMMUNITY_DELIVERY, GEAR_BUY, HUNT_ACCEPT, HUNT_CLOSE, AIRCRAFT_BUY, AIRCRAFT_DEPLOY, AIRCRAFT_ACTIVATE, AIRCRAFT_PACK, AIRCRAFT_SHARE, AIRCRAFT_REPAIR_BUY, AIRCRAFT_REPAIR_APPLY, AIRCRAFT_REPAIR_REFUND, AIRCRAFT_FLIGHT_REWARD, AIRCRAFT_CARGO_ACCEPT, AIRCRAFT_CARGO_RESERVE, AIRCRAFT_CARGO_DELIVER, AIRCRAFT_CARGO_CANCEL, AIRCRAFT_SCOUT_REWARD, PROFESSION_COMMISSION, PROFESSION_INSPECTION, PROFESSION_DELIVERY, PROFESSION_WORKSITE, PROFESSION_RETAINED, PROFESSION_TREE, PROFESSION_REPAIR, PROFESSION_ENCHANT_OUTPUT, PROFESSION_ENCHANT_DELIVERY, PROFESSION_SMITH_REGISTRATION, MERCHANT_TRANSACTION_QUEST, COIN_EXCHANGE, PROFESSION_MINING, PROFESSION_ENCOUNTER }
    public enum MailKind { COINS, BLUEPRINT, HUNT_ESCROW }
    public record Receipt(UUID store,long sequence) {
        public Receipt { if(sequence<0||store==null&&sequence!=0)throw new IllegalArgumentException("Invalid economy receipt"); }
        public static Receipt empty(){return new Receipt(null,0);}
    }
    public record Purchase(UUID operation,UUID buyer,String buyerName,String building,long price) { }
    public record Mail(UUID id,UUID owner,MailKind kind,long amount,String building,String description,boolean claimed) { }
    /** Exact single-stack escrow. Components are copied at every public boundary. */
    public record Listing(UUID id,UUID seller,String sellerName,String title,long price,boolean master,long created,long expires,CompoundTag item,boolean active,boolean timed,UUID bidder,long bid,long lastBid) {
        public Listing(UUID id,UUID seller,String sellerName,String title,long price,boolean master,long created,long expires,CompoundTag item,boolean active){this(id,seller,sellerName,title,price,master,created,expires,item,active,false,null,0,0);}
        public Listing {
            Objects.requireNonNull(id);Objects.requireNonNull(seller);text(sellerName,48);text(title,64);money(price);money(bid);
            if(price<1||created<0||expires<=created||lastBid<0||!timed&&(bidder!=null||bid!=0||lastBid!=0)
                    ||bidder==null&&(bid!=0||lastBid!=0)||bidder!=null&&(!timed||bidder.equals(seller)||bid<price||lastBid<created||lastBid>=expires))throw new IllegalArgumentException("Invalid listing");
            item=checkedItem(item);
        }
        @Override public CompoundTag item(){return item.copy();}
        private Listing closed(){return new Listing(id,seller,sellerName,title,price,master,created,expires,item,false,timed,bidder,bid,lastBid);}
        private Listing withBid(UUID player,long amount,long now){
            long end=expires-now<=ANTI_SNIPE_WINDOW?Math.max(expires,Math.addExact(now,ANTI_SNIPE_WINDOW)):expires;
            return new Listing(id,seller,sellerName,title,price,master,created,end,item,active,true,player,amount,now);
        }
    }
    /** Funded demand. Inactive remaining units are unfulfilled and have already been refunded. */
    public record BuyOrder(UUID id,UUID owner,String buyerName,String title,CompoundTag item,int quantity,int remaining,long unitPrice,boolean master,long created,long expires,long lastFill,boolean active) {
        public BuyOrder {
            Objects.requireNonNull(id);Objects.requireNonNull(owner);text(buyerName,48);text(title,64);item=checkedItem(item);
            if(item.getInt("count")!=1||quantity<1||quantity>2304||remaining<0||remaining>quantity||unitPrice<1||unitPrice>1_000_000_000L
                    ||created<0||expires<=created||expires-created>2_592_000_000L||lastFill<created||lastFill>=expires||active&&remaining==0)
                throw new IllegalArgumentException("Invalid funded buy order");
            money(Math.multiplyExact(unitPrice,quantity));
        }
        @Override public CompoundTag item(){return item.copy();}
        public long fundsHeld(){return active?Math.multiplyExact(unitPrice,remaining):0;}
        private BuyOrder filled(int amount,long now){int left=remaining-amount;return new BuyOrder(id,owner,buyerName,title,item,quantity,left,unitPrice,master,created,expires,now,left>0);}
        private BuyOrder closed(){return new BuyOrder(id,owner,buyerName,title,item,quantity,remaining,unitPrice,master,created,expires,lastFill,false);}
    }
    public record ItemMail(UUID id,UUID owner,CompoundTag item,String description,boolean claimed) {
        public ItemMail {Objects.requireNonNull(id);Objects.requireNonNull(owner);text(description,128);item=checkedItem(item);}
        @Override public CompoundTag item(){return item.copy();}
    }
    public enum ItemAction { LIST, CLAIM, TIMED_LIST, ORDER_FILL, CONTRACT_DELIVERY, FARMER_DELIVERY, COMMUNITY_DELIVERY, AIRCRAFT_DEPLOY, PROFESSION_DELIVERY, PROFESSION_ENCHANT_DELIVERY, PROFESSION_SMITH_REGISTRATION, COIN_EXCHANGE }
    /** Payload is the bounded encoded canonical operation; never client authority. */
    public record PendingItem(long sequence,UUID operation,UUID player,ItemAction action,CompoundTag payload,long previousReceipt) {
        public PendingItem {
            Objects.requireNonNull(operation);Objects.requireNonNull(player);Objects.requireNonNull(action);payload=Objects.requireNonNull(payload).copy();
            if(sequence<1||previousReceipt<0||previousReceipt>=sequence)throw new IllegalArgumentException("Invalid pending item receipt");
        }
        @Override public CompoundTag payload(){return payload.copy();}
    }
    public record Usage(long sharedSpent,long resourceQuantity,long playerProceeds) { }
    public record PendingSale(long sequence,UUID operation,UUID player,String resource,int quantity,long proceeds,long period,long previousReceipt) {
        public PendingSale {
            Objects.requireNonNull(operation);Objects.requireNonNull(player);identifier(resource);
            if(sequence<1||quantity<1||quantity>2304||proceeds<1||proceeds>MAX_COINS||period<0||previousReceipt<0||previousReceipt>=sequence)
                throw new IllegalArgumentException("Invalid prepared sale");
        }
    }
    /** A native inventory conversion: positive deposits, negative withdrawals. */
    public record PendingTransfer(long sequence,UUID operation,UUID player,long delta,long previousReceipt) {
        public PendingTransfer {
            Objects.requireNonNull(operation);Objects.requireNonNull(player);signedMoney(delta);
            if(sequence<1||previousReceipt<0||previousReceipt>=sequence)throw new IllegalArgumentException("Invalid prepared transfer");
        }
    }
    private record Operation(long sequence,UUID id,Kind kind,UUID actor,UUID target,long amount,String key,String detail,UUID reference,long period,int quantity,CompoundTag payload) {
        Operation(long sequence,UUID id,Kind kind,UUID actor,UUID target,long amount,String key,String detail,UUID reference,long period,int quantity){this(sequence,id,kind,actor,target,amount,key,detail,reference,period,quantity,new CompoundTag());}
        Operation {
            payload=Objects.requireNonNull(payload).copy();
            Objects.requireNonNull(id);Objects.requireNonNull(kind);Objects.requireNonNull(actor);Objects.requireNonNull(target);
            text(key,128);text(detail,128);if(kind==Kind.TRANSFER)signedMoney(amount);else money(amount);
            if(sequence<1||period<0||quantity<0)throw new IllegalArgumentException("Invalid economy operation");
        }
    }
    @FunctionalInterface interface Writer { void write(Path path,CompoundTag tag)throws IOException; }
    private final Path file;private final UUID identity;private final Writer writer;
    private List<Operation> history;
    /** Sharing the immutable closed prefix avoids copying years of audit references on every transaction. */
    private static final class JournalHistory extends AbstractList<Operation> implements RandomAccess {
        final List<Operation> closed,active;
        JournalHistory(List<Operation> closed,List<Operation> active){this.closed=List.copyOf(closed);this.active=List.copyOf(active);}
        @Override public int size(){return closed.size()+active.size();}
        @Override public Operation get(int index){Objects.checkIndex(index,size());return index<closed.size()?closed.get(index):active.get(index-closed.size());}
    }
    private static List<Operation> appended(List<Operation> history,Operation op){
        if(history instanceof JournalHistory journal){var next=new ArrayList<>(journal.active);next.add(op);return new JournalHistory(journal.closed,next);}
        var next=new ArrayList<>(history);next.add(op);return List.copyOf(next);
    }
    private List<Operation> aligned(List<Operation> records){return archivedCount==0?List.copyOf(records):new JournalHistory(records.subList(0,archivedCount),records.subList(archivedCount,records.size()));}
    private List<EconomyArchive.Segment> archives=List.of();
    private int archivedCount;
    private Map<UUID,Long> balances=new HashMap<>();
    private Map<String,Purchase> purchases=new HashMap<>();
    private Map<UUID,Mail> mail=new HashMap<>();
    private PendingSale pending;
    private PendingTransfer transfer;
    private PendingItem pendingItem;
    private Map<UUID,Listing> listings=new HashMap<>();
    private Map<UUID,ItemMail> itemMail=new HashMap<>();
    private Map<UUID,BuyOrder> orders=new HashMap<>();
    private record ShopDay(long day,String resource) { }
    private Map<ShopDay,Long> shopSold=new HashMap<>();
    private Map<UUID,ProfessionRoster> professionRosters=new HashMap<>();
    private ContractState contracts=new ContractState();
    private CommunityContractState community=new CommunityContractState();
    private EquipmentState equipment=new EquipmentState();
    private EnchantedOutputState enchantedOutputs=new EnchantedOutputState();
    private AircraftState aircraft=new AircraftState();
    // Rebuilt only from the canonical ordered journal. Never persisted as a second money authority.
    private record PlayerDay(long day,UUID player) { }
    private record FarmerFood(UUID player,ProfessionRank rank,String item) { }
    private final Map<UUID,Operation> operationIndex=new HashMap<>();
    private final Map<UUID,Long> receiptIndex=new HashMap<>();
    private final Map<Long,Long> dailyNpcSpent=new HashMap<>();
    private final Map<ShopDay,Long> dailyResourceSold=new HashMap<>();
    private final Map<PlayerDay,Long> dailyPlayerProceeds=new HashMap<>();
    private final Map<FarmerFood,Long> farmerFoodTotals=new HashMap<>();
    private long latestSalePeriod,equipmentSpent,shopSpent,removedTax;
    private boolean quarantined;
    private EconomyStore(Path file,UUID identity,Writer writer,List<Operation> history,PendingSale pending,PendingTransfer transfer,PendingItem pendingItem){this(file,identity,writer,history,pending,transfer,pendingItem,List.of());}
    private EconomyStore(Path file,UUID identity,Writer writer,List<Operation> history,PendingSale pending,PendingTransfer transfer,PendingItem pendingItem,List<EconomyArchive.Segment> archives){
        this.file=file;this.identity=identity;this.writer=writer;this.archives=List.copyOf(archives);this.archivedCount=archives.isEmpty()?0:Math.toIntExact(archives.getLast().last());this.history=aligned(history);
        Set<UUID> seen=new HashSet<>();
        for(int i=0;i<history.size();i++){
            var op=history.get(i);if(op.sequence()!=i+1L||!seen.add(op.id()))throw new IllegalArgumentException("Duplicate or unordered economy operation");
            apply(op,balances,purchases,mail,listings,itemMail,orders,shopSold,professionRosters,contracts,community,equipment,aircraft,enchantedOutputs);
            index(op);
        }
        this.pending=pending;this.transfer=transfer;this.pendingItem=pendingItem;
        if((pending!=null?1:0)+(transfer!=null?1:0)+(pendingItem!=null?1:0)>1)throw new IllegalArgumentException("Two inventory operations prepared simultaneously");
        if(pending!=null&&(pending.sequence()!=history.size()+1L||seen.contains(pending.operation())||pending.previousReceipt()!=expectedReceipt(pending.player())
                ||Math.addExact(balance(pending.player()),pending.proceeds())>MAX_COINS))throw new IllegalArgumentException("Invalid pending economy sale");
        if(transfer!=null){
            if(transfer.sequence()!=history.size()+1L||seen.contains(transfer.operation())||transfer.previousReceipt()!=expectedReceipt(transfer.player()))throw new IllegalArgumentException("Invalid pending bank transfer");
            money(Math.addExact(balance(transfer.player()),transfer.delta()));
        }
        if(pendingItem!=null){
            var op=decode(pendingItem.payload());
            if(op.sequence()!=history.size()+1L||seen.contains(op.id())||!op.id().equals(pendingItem.operation())||!op.actor().equals(pendingItem.player())||!op.target().equals(pendingItem.player())
                    ||op.sequence()!=pendingItem.sequence()||pendingItem.previousReceipt()!=expectedReceipt(pendingItem.player())
                    ||op.kind()!=switch(pendingItem.action()){case COIN_EXCHANGE->Kind.COIN_EXCHANGE;case LIST->Kind.LIST_ITEM;case CLAIM->Kind.CLAIM_ITEM;case TIMED_LIST->Kind.LIST_TIMED_ITEM;case ORDER_FILL->Kind.FILL_ORDER;case CONTRACT_DELIVERY->Kind.COMPLETE_CONTRACT;case FARMER_DELIVERY->Kind.FARMER_DELIVERY;case PROFESSION_DELIVERY->Kind.PROFESSION_DELIVERY;case PROFESSION_ENCHANT_DELIVERY->Kind.PROFESSION_ENCHANT_DELIVERY;case PROFESSION_SMITH_REGISTRATION->Kind.PROFESSION_SMITH_REGISTRATION;case COMMUNITY_DELIVERY->Kind.COMMUNITY_DELIVERY;case AIRCRAFT_DEPLOY->Kind.AIRCRAFT_DEPLOY;})throw new IllegalArgumentException("Invalid pending item operation");
            var nextMail=new HashMap<>(mail);var nextListings=new HashMap<>(listings);var nextItems=new HashMap<>(itemMail);var nextOrders=new HashMap<>(orders);
            apply(op,new HashMap<>(balances),new HashMap<>(purchases),nextMail,nextListings,nextItems,nextOrders,new HashMap<>(shopSold),new HashMap<>(professionRosters),contracts.copy(),community.copy(),equipment.copy(),aircraft.copy(),enchantedOutputs.copy());var nextHistory=appended(history,op);budget(nextHistory,nextListings,nextItems,nextMail,nextOrders,false,aircraft.copy());
        }
        budget(history,listings,itemMail,mail,orders,false,aircraft);
    }
    public static EconomyStore open(Path directory)throws IOException{return open(directory,AtomicNbt::write);}
    static EconomyStore open(Path directory,Writer writer)throws IOException{
        Path file=directory.resolve("kingdomsmp_economy.dat"),seal=directory.resolve("kingdomsmp_economy.created");
        if(Files.exists(file,LinkOption.NOFOLLOW_LINKS)!=Files.exists(seal,LinkOption.NOFOLLOW_LINKS))throw new IOException("Economy journal/identity mismatch; restore matching backup");
        if(!Files.exists(file,LinkOption.NOFOLLOW_LINKS)){
            Path closed=EconomyArchive.directory(directory);
            if(Files.exists(closed,LinkOption.NOFOLLOW_LINKS)){
                if(!Files.isDirectory(closed,LinkOption.NOFOLLOW_LINKS))throw new IOException("Invalid linked economy archive directory; matching journal recovery required");
                try(var entries=Files.list(closed)){if(entries.findAny().isPresent())throw new IOException("Existing economy archives require their matching journal and identity seal; do not regenerate financial authority");}
            }
            var s=new EconomyStore(file,UUID.randomUUID(),writer,List.of(),null,null,null);var mark=new CompoundTag();mark.putUUID("identity",s.identity);
            AtomicNbt.write(seal,mark);writer.write(file,s.encode(List.of(),null));return s;
        }
        try{
            var tag=AtomicNbt.read(file);var mark=AtomicNbt.read(seal);
            require(tag,"schema",Tag.TAG_INT);require(tag,"revision",Tag.TAG_LONG);require(tag,"history",Tag.TAG_LIST);
            int schema=tag.getInt("schema");if((schema!=1&&schema!=2)||!tag.hasUUID("identity")||!mark.hasUUID("identity")||!tag.getUUID("identity").equals(mark.getUUID("identity")))throw new IOException("Invalid economy identity/schema");
            if(schema==1&&tag.contains("archives"))throw new IOException("Legacy economy cannot reference archives");
            List<EconomyArchive.Segment> archives=schema==2?EconomyArchive.descriptors(tag):List.of();
            var list=tag.getList("history",Tag.TAG_COMPOUND);long archived=archives.isEmpty()?0:archives.getLast().last();
            if(list.size()>MAX_OPERATIONS||list.size()!=((ListTag)tag.get("history")).size()||tag.getLong("revision")!=archived+list.size()||tag.getLong("revision")>EconomyArchive.MAX_TOTAL_OPERATIONS)throw new IOException("Invalid economy history bounds");
            List<Operation> operations=new ArrayList<>();if(!archives.isEmpty())for(var row:EconomyArchive.read(directory,tag.getUUID("identity"),archives))operations.add(decode(row));for(int i=0;i<list.size();i++)operations.add(decode(list.getCompound(i)));
            PendingSale pending=null;
            if(tag.contains("pending")){
                require(tag,"pending",Tag.TAG_COMPOUND);var p=tag.getCompound("pending");
                for(String key:List.of("sequence","proceeds","period","previousReceipt"))require(p,key,Tag.TAG_LONG);
                require(p,"resource",Tag.TAG_STRING);require(p,"quantity",Tag.TAG_INT);
                if(!p.hasUUID("operation")||!p.hasUUID("player"))throw new IOException("Invalid pending economy identity");
                pending=new PendingSale(p.getLong("sequence"),p.getUUID("operation"),p.getUUID("player"),p.getString("resource"),p.getInt("quantity"),p.getLong("proceeds"),p.getLong("period"),p.getLong("previousReceipt"));
            }
            PendingTransfer transfer=null;
            if(tag.contains("transfer")){
                require(tag,"transfer",Tag.TAG_COMPOUND);var p=tag.getCompound("transfer");
                for(String key:List.of("sequence","delta","previousReceipt"))require(p,key,Tag.TAG_LONG);
                if(!p.hasUUID("operation")||!p.hasUUID("player"))throw new IOException("Invalid bank transfer identity");
                transfer=new PendingTransfer(p.getLong("sequence"),p.getUUID("operation"),p.getUUID("player"),p.getLong("delta"),p.getLong("previousReceipt"));
            }
            PendingItem pendingItem=null;
            if(tag.contains("pendingItem")){
                require(tag,"pendingItem",Tag.TAG_COMPOUND);var p=tag.getCompound("pendingItem");
                for(String key:List.of("sequence","previousReceipt"))require(p,key,Tag.TAG_LONG);
                require(p,"action",Tag.TAG_STRING);require(p,"payload",Tag.TAG_COMPOUND);
                if(!p.hasUUID("operation")||!p.hasUUID("player"))throw new IOException("Invalid pending item identity");
                pendingItem=new PendingItem(p.getLong("sequence"),p.getUUID("operation"),p.getUUID("player"),ItemAction.valueOf(p.getString("action")),p.getCompound("payload"),p.getLong("previousReceipt"));
            }
            return new EconomyStore(file,tag.getUUID("identity"),writer,operations,pending,transfer,pendingItem,archives);
        }catch(RuntimeException e){throw new IOException("Corrupt economy checkpoint",e);}
    }
    public synchronized UUID id(){return identity;}
    public synchronized long revision(){return history.size();}
    public synchronized boolean quarantined(){return quarantined;}
    public synchronized void quarantine(){quarantined=true;}
    public synchronized long balance(UUID player){return balances.getOrDefault(Objects.requireNonNull(player),0L);}
    public synchronized Optional<Purchase> purchase(String building){return Optional.ofNullable(purchases.get(building));}
    public synchronized Collection<Purchase> purchases(){return List.copyOf(purchases.values());}
    public synchronized List<Mail> mailbox(UUID owner){return mail.values().stream().filter(m->m.owner().equals(owner)).sorted(Comparator.comparing(Mail::id)).toList();}
    public synchronized Optional<PendingSale> pendingSale(){return Optional.ofNullable(pending);}
    public synchronized Optional<PendingTransfer> pendingTransfer(){return Optional.ofNullable(transfer);}
    public synchronized Optional<PendingItem> pendingItem(){return Optional.ofNullable(pendingItem);}
    public synchronized List<Listing> listings(){return listings.values().stream().sorted(Comparator.comparing(Listing::id)).toList();}
    public synchronized List<BuyOrder> orders(){return orders.values().stream().sorted(Comparator.comparingLong(BuyOrder::unitPrice).reversed().thenComparingLong(BuyOrder::created).thenComparing(BuyOrder::id)).toList();}
    public synchronized Optional<BuyOrder> order(UUID id){return Optional.ofNullable(orders.get(id));}
    public synchronized Optional<Listing> listing(UUID id){return Optional.ofNullable(listings.get(id));}
    public synchronized List<ItemMail> itemMailbox(UUID owner){return itemMail.values().stream().filter(m->m.owner().equals(owner)).sorted(Comparator.comparing(ItemMail::id)).toList();}
    public synchronized boolean contractCompleted(UUID player,String profession,String cycle,long period){return contracts.completed(player,profession,cycle,period);}
    public synchronized int equipmentPurchases(UUID player,String item){FarmerGear.byId(item);return equipment.count(player,item);}
    public synchronized ItemMail buyFarmerGear(UUID operation,UUID player,String gearId,long basePrice,CompoundTag item)throws IOException{
        var gear=FarmerGear.byId(gearId);var data=new CompoundTag();data.putLong("basePrice",basePrice);data.put("item",checkedItem(item));var old=find(operation);long price=old==null?gear.price(basePrice,equipment.count(player,gearId)):old.amount();
        var op=new Operation(history.size()+1L,operation,Kind.GEAR_BUY,player,player,price,gearId,"Farmer equipment purchase",null,0,1,data);if(!replayed(op))append(op);return itemMail.get(operation);
    }
    public synchronized Optional<HuntState> hunt(UUID player,long day){return contracts.hunts.values().stream().filter(h->h.player().equals(player)&&h.day()==day).findFirst();}
    public synchronized List<HuntState> hunts(){return List.copyOf(contracts.hunts.values());}
    public synchronized long huntSpent(long day){return contracts.huntSpent.getOrDefault(day,0L);}
    public synchronized long huntBudget(long day,long configured){return contracts.huntBudgets.getOrDefault(day,configured);}
    /** Reserve the full promised NPC payment and future close/claim journal capacity at acceptance. */
    public synchronized HuntState acceptHunt(UUID operation,UUID player,long day,int goal,long reward,long pool)throws IOException{
        var data=new CompoundTag();data.putLong("budget",pool);
        var op=new Operation(history.size()+1L,operation,Kind.HUNT_ACCEPT,player,player,reward,HuntState.target(day),"Accepted daily hunt",null,day,goal,data);
        if(!replayed(op))append(op);return contracts.hunts.get(operation);
    }
    /** Kill evidence is checked by the server service; a closed entitlement cannot pay again. */
    public synchronized HuntState closeHunt(UUID operation,UUID player,UUID accepted,long today,boolean earned)throws IOException{
        var hunt=contracts.hunts.get(accepted);if(hunt==null)throw new IllegalArgumentException("Unknown accepted hunt");
        var op=operation(operation,Kind.HUNT_CLOSE,player,player,hunt.reward(),hunt.target(),"Closed daily hunt",accepted,today,earned?hunt.goal():0);
        if(!replayed(op))append(op);return contracts.hunts.get(accepted);
    }
    public synchronized Optional<CommunityContractState.Project> communityProject(String project,long week){return community.project(project,week);}
    public synchronized long communitySpent(long week){return community.spent(week);}
    public synchronized long fishingContractSpent(long week){return community.fishingSpent(week);}
    public synchronized long communityMasterProofs(UUID player){return community.masterProofs(player);}
    public synchronized PendingItem prepareCommunityDelivery(UUID operation,UUID player,String project,long week,String item,int quantity,int goal,int unitReward,long budget)throws IOException{
        var data=new CompoundTag();data.putString("item",item);data.putInt("goal",goal);data.putInt("unitReward",unitReward);data.putLong("budget",budget);
        return prepareItem(new Operation(history.size()+1L,operation,Kind.COMMUNITY_DELIVERY,player,player,Math.multiplyExact((long)quantity,unitReward),project,project.startsWith("kingdom_smp:fishing_contract/")?"Wharf fishing contract":"Community food contract",null,week,quantity,data),ItemAction.COMMUNITY_DELIVERY);
    }
    public synchronized long farmerFoodConsumed(UUID player,ProfessionRank band,String item){return farmerFoodTotals.getOrDefault(new FarmerFood(player,band,item),0L);}
    public synchronized long professionContractPeriods(UUID player,Profession profession,ProfessionRank band,String cycle){healthy();Objects.requireNonNull(player);Objects.requireNonNull(profession);Objects.requireNonNull(band);if(!Set.of("daily","weekly").contains(cycle))throw new IllegalArgumentException("Unknown contract cycle");return contracts.professionPeriods.stream().filter(p->p.player().equals(player)&&p.profession().equals(profession.id())&&p.band().equals(band.id())&&p.cycle().equals(cycle)).count();}
    public synchronized long commissionContractPeriods(UUID player,String quest){
        healthy();return commissionPeriods(contracts,player,quest);
    }
    private static long commissionPeriods(ContractState contracts,UUID player,String quest){
        Objects.requireNonNull(player);var d=CommissionQuests.get(quest);int minimum=ApprovedProfessionContent.get(quest).minimumTier();
        return contracts.professionPeriods.stream().filter(p->p.player().equals(player)&&p.profession().equals(d.profession().id())&&p.band().equals(d.rank().id())&&p.cycle().equals(d.cycle())&&(p.tier()>=minimum||p.tier()==0&&d.rank()==ProfessionRank.APPRENTICE&&(d.profession()==Profession.BUILDER||d.profession()==Profession.LUMBERJACK))).count();
    }
    public synchronized long farmerContractPeriods(UUID player,ProfessionRank band,String cycle){return professionContractPeriods(player,Profession.FARMER,band,cycle);}
    public synchronized long contractPoolSpent(String cycle,long period){return contracts.spent(cycle,period);}
    /** Root pins a real approved repeatable delivery's item/terms and removes actual native inventory. */
    public synchronized PendingItem prepareContract(UUID operation,UUID player,String profession,String cycle,long period,int quantity,long reward,long pool,CompoundTag item)throws IOException{
        return prepareContractInternal(operation,player,profession,cycle,period,quantity,reward,pool,item,0);
    }
    /** New native receipts pin actual world tier; legacy receipts stay readable without invented history. */
    public synchronized PendingItem prepareContract(UUID operation,UUID player,String profession,String cycle,long period,int quantity,long reward,long pool,CompoundTag item,int tier)throws IOException{
        if(tier<1||tier>12)throw new IllegalArgumentException("Invalid authoritative contract tier");return prepareContractInternal(operation,player,profession,cycle,period,quantity,reward,pool,item,tier);
    }
    private PendingItem prepareContractInternal(UUID operation,UUID player,String profession,String cycle,long period,int quantity,long reward,long pool,CompoundTag item,int tier)throws IOException{
        var data=new CompoundTag();data.putString("profession",profession);data.putString("cycle",cycle);data.putInt("required",quantity);data.putLong("pool",pool);data.put("item",checkedItem(item));if(tier>0)data.putInt("questTier",tier);
        var op=new Operation(history.size()+1L,operation,Kind.COMPLETE_CONTRACT,player,player,reward,"kingdom_smp:contract/"+profession+"/"+cycle,"Profession delivery contract",null,period,quantity,data);
        return prepareItem(op,ItemAction.CONTRACT_DELIVERY);
    }
    public synchronized ProfessionRoster professions(UUID player){return professionRosters.getOrDefault(Objects.requireNonNull(player),ProfessionRoster.empty());}
    /** The profession state and its exact switching fee are one canonical operation. Root checks real services/gates. */
    public synchronized ProfessionRoster changeProfession(UUID operation,UUID player,String action,String from,String to)throws IOException{
        var data=new CompoundTag();data.putString("action",action);data.putString("from",from);data.putString("to",to);
        var old=find(operation);long fee=old==null?professionTransition(professions(player),data).fee():old.amount();
        var op=new Operation(history.size()+1L,operation,Kind.PROFESSION_CHANGE,player,player,fee,"profession","Profession change",null,0,0,data);
        if(!replayed(op))append(op);return professions(player);
    }
    /** Native input removal, returned containers and personal quest XP share one receipt boundary. */
    public synchronized PendingItem prepareFarmerDelivery(UUID operation,UUID player,String quest,String evidence,Map<String,Integer> inputs)throws IOException{
        var data=new CompoundTag();data.putString("evidence",evidence);data.putInt("contentVersion",1);var encoded=new CompoundTag();inputs.forEach(encoded::putInt);data.put("inputs",encoded);
        return prepareItem(new Operation(history.size()+1L,operation,Kind.FARMER_DELIVERY,player,player,FarmerQuests.xp(quest),quest,"Farmer food delivery",null,0,0,data),ItemAction.FARMER_DELIVERY);
    }
    /** Permission-checked administrator site review; actor and evidence are permanently attributed. */
    public synchronized ProfessionRoster approveFarmerInspection(UUID operation,UUID admin,UUID player,String quest,String evidence)throws IOException{
        if(!FarmerQuests.inspection(quest)||evidence==null||evidence.isBlank()||evidence.length()>256)throw new IllegalArgumentException("Approved bounded Farmer inspection required");
        var data=new CompoundTag();data.putString("evidence",evidence);data.putInt("contentVersion",1);
        var op=new Operation(history.size()+1L,operation,Kind.FARMER_INSPECTION,admin,player,FarmerQuests.xp(quest),quest,"Farmer site inspection",null,0,0,data);
        if(!replayed(op))append(op);return professions(player);
    }
    /** Root verifies persisted real objective evidence; no packet can supply XP or completion state. */
    public synchronized ProfessionRoster completeFarmerQuest(UUID operation,UUID player,String quest,String evidence)throws IOException{
        if(evidence==null||evidence.isBlank()||evidence.length()>256)throw new IllegalArgumentException("Bounded server objective evidence required");
        var data=new CompoundTag();data.putString("evidence",evidence);data.putInt("contentVersion",1);
        var op=new Operation(history.size()+1L,operation,Kind.PROFESSION_QUEST,player,player,FarmerQuests.xp(quest),quest,"Farmer objective completed",null,0,0,data);
        if(!replayed(op))append(op);return professions(player);
    }
    public synchronized PendingItem prepareCoinExchange(UUID operation,UUID player,CoinExchangeTerms terms)throws IOException{
        var data=new CompoundTag();data.putString("source",terms.source());data.putString("target",terms.target());
        return prepareItem(new Operation(history.size()+1L,operation,Kind.COIN_EXCHANGE,player,player,0,"coins","Equal-value physical coin exchange",null,0,terms.batches(),data),ItemAction.COIN_EXCHANGE);
    }
    public synchronized MerchantTransactionState.Proof merchantProof(UUID player){healthy();return contracts.merchant.get(player);}
    public synchronized ProfessionRoster completeMerchantTransactionQuest(UUID operation,UUID player,String quest)throws IOException{
        return completeMerchantTransactionQuest(operation,player,quest,MerchantTransactionQuests.goal(quest));
    }
    public synchronized ProfessionRoster completeMerchantTransactionQuest(UUID operation,UUID player,String quest,int required)throws IOException{
        var d=MerchantTransactionQuests.get(quest);MerchantQuestRules.validate(quest,required);var data=new CompoundTag();data.putInt("contentVersion",1);data.putInt("required",required);
        var op=new Operation(history.size()+1L,operation,Kind.MERCHANT_TRANSACTION_QUEST,player,player,d.xp(),quest,"Merchant settled transactions",null,0,0,data);
        if(!replayed(op))append(op);return professions(player);
    }
    public synchronized List<BuilderWorksite> builderWorksites(){healthy();return contracts.currentSites.values().stream().map(contracts.workSites::get).sorted(Comparator.comparing(BuilderWorksite::id)).toList();}
    public synchronized List<BuilderWorksite> builderAuthorizationHistory(){healthy();return List.copyOf(contracts.workSites.values()).reversed();}
    public synchronized Optional<BuilderWorksite> builderWorksite(UUID player,String quest){healthy();return Optional.ofNullable(contracts.workSites.get(contracts.currentSites.get(new ContractState.WorksiteSlot(player,quest))));}
    /** Operator permission, live claims/reservations and bounded local review are verified by the root. */
    public synchronized BuilderWorksite authorizeBuilderWorksite(UUID operation,UUID admin,UUID player,String quest,String dimension,net.minecraft.core.BlockPos from,net.minecraft.core.BlockPos to,String reason)throws IOException{
        var site=new BuilderWorksite(operation,admin,player,quest,dimension,from,to,reason);var data=new CompoundTag();data.putInt("contentVersion",1);data.put("site",site.save());
        var op=new Operation(history.size()+1L,operation,Kind.PROFESSION_WORKSITE,admin,player,0,quest,"Builder worksite authorization",null,0,0,data);
        if(!replayed(op))append(op);return site;
    }
    /** Root verifies actual retained native placements and support; a packet never supplies these facts. */
    public synchronized ProfessionRoster completeRetainedBuilding(UUID operation,UUID player,String quest,UUID site,int required,int retained,String snapshot)throws IOException{
        if(!RetainedBuildingQuests.contains(quest))throw new IllegalArgumentException("This building objective requires administrator inspection");var d=RetainedBuildingQuests.get(quest);var data=new CompoundTag();data.putInt("contentVersion",1);data.putUUID("site",site);data.putInt("required",required);data.putString("snapshot",snapshot);
        var op=new Operation(history.size()+1L,operation,Kind.PROFESSION_RETAINED,player,player,d.xp(),quest,"Retained player construction",null,0,retained,data);
        if(!replayed(op))append(op);return professions(player);
    }
    /** Root reads native server-owned tree evidence; no packet supplies a count, rank or XP. */
    public synchronized ProfessionRoster completeTreeQuest(UUID operation,UUID player,String quest,int required,int actual)throws IOException{
        var definition=TreeQuestService.get(quest);var data=new CompoundTag();data.putInt("contentVersion",1);data.putInt("required",required);
        var op=new Operation(history.size()+1L,operation,Kind.PROFESSION_TREE,player,player,ApprovedProfessionContent.get(quest).xp(),quest,"Native tree objective",null,0,actual,data);
        if(!replayed(op))append(op);return professions(player);
    }
    /** Counts are read from authenticated successful native deaths by the server producer. */
    public synchronized ProfessionRoster completeEncounterQuest(UUID operation,UUID player,int total,int perSpecies,Map<String,Integer> counts)throws IOException{
        var data=new CompoundTag();data.putInt("contentVersion",1);data.putInt("total",total);data.putInt("perSpecies",perSpecies);var rows=new CompoundTag();counts.forEach(rows::putInt);data.put("counts",rows);
        var op=new Operation(history.size()+1L,operation,Kind.PROFESSION_ENCOUNTER,player,player,100,EncounterQuests.QUEST,"Native natural encounters",null,0,0,data);if(!replayed(op))append(op);return professions(player);
    }
    /** Successful native ore removal proof is read server-side; packets cannot provide counts or XP. */
    public synchronized ProfessionRoster completeNativeOreQuest(UUID operation,UUID player,String quest,int required,int actual)throws IOException{
        if(!NativeOreQuests.contains(quest))throw new IllegalArgumentException("Unvalidated native mining objective");var data=new CompoundTag();data.putInt("contentVersion",1);data.putInt("required",required);
        var op=new Operation(history.size()+1L,operation,Kind.PROFESSION_MINING,player,player,ApprovedProfessionContent.get(quest).xp(),quest,"Native ore extraction",null,0,actual,data);if(!replayed(op))append(op);return professions(player);
    }
    public synchronized ProfessionRoster completeRepairQuest(UUID operation,UUID player,int required,int actual)throws IOException{return completeRepairQuest(operation,player,RepairService.QUEST,required,actual);}
    public synchronized ProfessionRoster completeRepairQuest(UUID operation,UUID player,String quest,int required,int actual)throws IOException{
        if(!RepairService.contains(quest))throw new IllegalArgumentException("No validated repair objective");var data=new CompoundTag();data.putInt("contentVersion",1);data.putInt("required",required);var op=new Operation(history.size()+1L,operation,Kind.PROFESSION_REPAIR,player,player,ApprovedProfessionContent.get(quest).xp(),quest,"Native item repairs",null,0,actual,data);if(!replayed(op))append(op);return professions(player);
    }
    /** Only the native successful-anvil handler issues these server-owned records. No preview or packet does. */
    public synchronized void issueEnchantedOutput(UUID id,UUID smith,CompoundTag item)throws IOException{issueNativeOutput(id,smith,EnchantedToolService.QUEST,item);}
    public synchronized void issueNativeOutput(UUID id,UUID smith,String quest,CompoundTag item)throws IOException{issueNativeOutput(id,smith,quest,item,NativeBookProof.EMPTY);}
    public synchronized void issueNativeOutput(UUID id,UUID smith,String quest,CompoundTag item,NativeBookProof attributes)throws IOException{
        var data=new CompoundTag();data.put("item",checkedItem(item));if(!attributes.equals(NativeBookProof.EMPTY))data.put("attributes",attributes.save());var op=new Operation(history.size()+1L,id,Kind.PROFESSION_ENCHANT_OUTPUT,smith,smith,0,quest,NativeOutputQuests.issueDetail(quest),null,0,0,data);if(!replayed(op))append(op);
    }
    public synchronized String enchantedOutputAudit(UUID player){healthy();var records=enchantedOutputs.outputs;long issued=records.values().stream().filter(o->o.smith().equals(player)).count();long spent=records.values().stream().filter(o->o.smith().equals(player)&&o.spent()).count();return "Native authenticated outputs: "+issued+" issued, "+spent+" used for delivery or registration; bounded IDs "+records.entrySet().stream().filter(e->e.getValue().smith().equals(player)).map(e->e.getKey()+":"+(e.getValue().spent()?"spent":"available")).sorted().limit(16).toList();}
    public synchronized boolean matchesEnchantedOutput(UUID id,UUID smith,CompoundTag item){return matchesNativeOutput(id,smith,EnchantedToolService.QUEST,item);}
    public synchronized Optional<NativeBookProof> nativeOutputAttributes(UUID id,UUID owner,String quest,CompoundTag item){healthy();return enchantedOutputs.matches(id,owner,quest,item)?Optional.of(enchantedOutputs.outputs.get(id).attributes()):Optional.empty();}
    public synchronized boolean matchesNativeOutput(UUID id,UUID smith,String quest,CompoundTag item){healthy();return enchantedOutputs.matches(id,smith,quest,item);}
    public synchronized PendingItem prepareEnchantedDelivery(UUID operation,UUID player,Map<UUID,CompoundTag> items,int required)throws IOException{return prepareNativeOutputReceipt(operation,player,EnchantedToolService.QUEST,items,required);}
    public synchronized PendingItem prepareNativeOutputDelivery(UUID operation,UUID player,String quest,Map<UUID,CompoundTag> items,int required)throws IOException{if(!NativeOutputQuests.consume(quest))throw new IllegalArgumentException("Use the registration receipt");return prepareNativeOutputReceipt(operation,player,quest,items,required);}
    public synchronized PendingItem prepareNativeSmithRegistration(UUID operation,UUID player,Map<UUID,CompoundTag> items,int required)throws IOException{return prepareNativeOutputReceipt(operation,player,SmithingQuestService.QUEST,items,required);}
    private PendingItem prepareNativeOutputReceipt(UUID operation,UUID player,String quest,Map<UUID,CompoundTag> items,int required)throws IOException{
        var data=new CompoundTag();data.putInt("required",required);var rows=new ListTag();items.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->{var row=new CompoundTag();row.putUUID("id",e.getKey());row.put("item",checkedItem(e.getValue()));rows.add(row);});data.put("outputs",rows);boolean smith=quest.equals(SmithingQuestService.QUEST);
        return prepareItem(new Operation(history.size()+1L,operation,smith?Kind.PROFESSION_SMITH_REGISTRATION:Kind.PROFESSION_ENCHANT_DELIVERY,player,player,NativeOutputQuests.get(quest).xp(),quest,NativeOutputQuests.receiptDetail(quest),null,0,0,data),smith?ItemAction.PROFESSION_SMITH_REGISTRATION:ItemAction.PROFESSION_ENCHANT_DELIVERY);
    }
    /** Verified native crafting plus permanently consumed outputs share one native receipt boundary. */
    public synchronized PendingItem prepareProfessionDelivery(UUID operation,UUID player,String quest,String evidence,Map<String,Integer> inputs)throws IOException{
        var d=CraftDeliveryQuests.get(quest);var data=new CompoundTag();data.putInt("contentVersion",1);data.putString("evidence",evidence);
        var encoded=new CompoundTag();inputs.forEach(encoded::putInt);CraftDeliveryQuests.validate(quest,encoded);data.put("inputs",encoded);
        return prepareItem(new Operation(history.size()+1L,operation,Kind.PROFESSION_DELIVERY,player,player,d.xp(),quest,"Profession crafted delivery",null,0,0,data),ItemAction.PROFESSION_DELIVERY);
    }
    /** Original canonical site audit, including immutable archived operations; never inferred from inventory data. */
    public synchronized Optional<String> professionInspectionEvidence(UUID player,String quest){healthy();return operationIndex.values().stream().filter(op->op.kind()==Kind.PROFESSION_INSPECTION&&op.target().equals(player)&&op.key().equals(quest)).map(op->op.payload().getString("evidence")).findFirst();}
    /** OP2 site authority is checked by the root; immutable actor/site/snapshot audit is stored with XP. */
    public synchronized ProfessionRoster approveProfessionInspection(UUID operation,UUID admin,UUID player,String quest,String evidence)throws IOException{
        var d=InspectionQuests.get(quest);
        if(evidence==null||evidence.isBlank()||evidence.length()>256)throw new IllegalArgumentException("Bounded site inspection evidence required");
        var data=new CompoundTag();data.putInt("contentVersion",1);data.putString("evidence",evidence);
        var op=new Operation(history.size()+1L,operation,Kind.PROFESSION_INSPECTION,admin,player,d.xp(),quest,"Profession site inspection",null,0,0,data);
        if(!replayed(op))append(op);return professions(player);
    }
    /** Canonical distinct consumed-contract proofs, never a client-supplied evidence count. */
    public synchronized ProfessionRoster completeCommissionQuest(UUID operation,UUID player,String quest,int required)throws IOException{
        var d=CommissionQuests.get(quest);
        if(required<1||required>365)throw new IllegalArgumentException("Invalid commission goal");
        var data=new CompoundTag();data.putInt("contentVersion",1);data.putInt("required",required);
        var op=new Operation(history.size()+1L,operation,Kind.PROFESSION_COMMISSION,player,player,CommissionQuests.xp(quest),quest,"Profession commissions completed",null,0,0,data);
        if(!replayed(op))append(op);return professions(player);
    }
    private static ProfessionRoster.Transition professionTransition(ProfessionRoster roster,CompoundTag data){
        if(!data.getAllKeys().equals(Set.of("action","from","to")))throw new IllegalArgumentException("Invalid profession transition payload");
        for(var k:List.of("action","from","to"))require(data,k,Tag.TAG_STRING);
        String action=data.getString("action"),from=data.getString("from"),to=data.getString("to");
        return switch(action){
            case "enroll"->{if(!from.isEmpty())throw new IllegalArgumentException("Enrollment cannot remove a profession");yield roster.enroll(Profession.byId(to));}
            case "switch"->roster.switchProfession(Profession.byId(from),Profession.byId(to));
            default->throw new IllegalArgumentException("Unknown profession action");
        };
    }
    public synchronized long shopSold(long period,String resource){if(period<0)throw new IllegalArgumentException("Invalid trading day");identifier(resource);return shopSold.getOrDefault(new ShopDay(period,resource),0L);}
    /** Canonical origin only: client item names/tags never authorize an earlier claim gate. */
    public synchronized boolean shopDelivery(UUID mail){var op=find(mail);return op!=null&&(op.kind()==Kind.SHOP_BUY||op.kind()==Kind.GEAR_BUY||op.kind()==Kind.AIRCRAFT_BUY||op.kind()==Kind.AIRCRAFT_PACK);}
    /** Canonical purchased-item provenance, never a client supplied resource or name. */
    public synchronized Optional<String> shopItemResource(UUID mail){healthy();var op=find(mail);return op!=null&&op.kind()==Kind.SHOP_BUY&&itemMail.containsKey(mail)?Optional.of(op.key()):Optional.empty();}
    public synchronized boolean aircraftDelivery(UUID mail){healthy();var op=find(mail);return op!=null&&(op.kind()==Kind.AIRCRAFT_BUY||op.kind()==Kind.AIRCRAFT_PACK)&&itemMail.containsKey(mail);}
    public synchronized List<AircraftState.Registration> aircraft(){healthy();return aircraft.records();}
    public synchronized Optional<AircraftState.Registration> aircraft(UUID id){healthy();return aircraft.get(id);}
    public synchronized ItemMail buyAircraft(UUID operation,UUID buyer,String type,long price,CompoundTag item)throws IOException{
        var data=new CompoundTag();data.put("item",checkedItem(item));var op=new Operation(history.size()+1L,operation,Kind.AIRCRAFT_BUY,buyer,buyer,price,type,"Registered aircraft purchase",null,0,1,data);if(!replayed(op))append(op);return itemMail.get(operation);
    }
    public synchronized PendingItem prepareAircraftDeployment(UUID operation,UUID actor,UUID registration,CompoundTag entity,String dimension)throws IOException{
        var data=new CompoundTag();data.put("entity",entity.copy());data.putString("dimension",dimension);data.putUUID("actor",actor);return prepareItem(new Operation(history.size()+1L,operation,Kind.AIRCRAFT_DEPLOY,actor,actor,0,"aircraft","Aircraft deployment",registration,0,1,data),ItemAction.AIRCRAFT_DEPLOY);
    }
    public synchronized void activateAircraft(UUID operation,UUID owner,UUID registration)throws IOException{var op=operation(operation,Kind.AIRCRAFT_ACTIVATE,owner,owner,0,"aircraft","Aircraft activation",registration,0,0);if(!replayed(op))append(op);}
    public synchronized ItemMail packAircraft(UUID operation,UUID owner,UUID registration,CompoundTag item)throws IOException{
        var data=new CompoundTag();data.put("item",checkedItem(item));var op=new Operation(history.size()+1L,operation,Kind.AIRCRAFT_PACK,owner,owner,0,"aircraft","Recovered aircraft airframe",registration,0,1,data);if(!replayed(op))append(op);return itemMail.get(operation);
    }
    public synchronized void shareAircraft(UUID operation,UUID owner,UUID registration,UUID target,boolean allow)throws IOException{
        var data=new CompoundTag();data.putUUID("crew",target);data.putBoolean("allow",allow);var op=new Operation(history.size()+1L,operation,Kind.AIRCRAFT_SHARE,owner,owner,0,"aircraft","Aircraft shared access",registration,0,0,data);if(!replayed(op))append(op);
    }
    public synchronized List<AircraftState.Repair> aircraftRepairs(){healthy();return aircraft.repairs();}
    public synchronized Optional<AircraftState.Repair> aircraftRepair(UUID registration){healthy();return aircraft.repair(registration);}
    public synchronized Optional<UUID> deliveredAircraftRepair(UUID registration){healthy();return aircraft.deliveredRepair(registration);}
    public synchronized long aircraftRepairEscrow(){healthy();return aircraft.repairs().stream().mapToLong(AircraftState.Repair::price).sum();}
    public synchronized void buyAircraftRepair(UUID operation,UUID actor,UUID registration,float health,long rate)throws IOException{
        var record=aircraft.get(registration).orElseThrow();var data=new CompoundTag();data.putUUID("entity",record.deployment().getCompound("entity").getUUID("UUID"));data.putLong("generation",record.generation());data.putFloat("health",health);data.putLong("rate",rate);var op=new Operation(history.size()+1L,operation,Kind.AIRCRAFT_REPAIR_BUY,actor,actor,AircraftState.repairPrice(health,rate),"aircraft_repair","Reserved aircraft repair",registration,0,0,data);if(!replayed(op))append(op);
    }
    public synchronized void settleAircraftRepair(UUID operation,UUID actor,UUID ticket,boolean refund)throws IOException{
        var old=find(operation);long amount=refund?(old==null?aircraft.repairTicket(ticket).orElseThrow().price():old.amount()):0;var op=operation(operation,refund?Kind.AIRCRAFT_REPAIR_REFUND:Kind.AIRCRAFT_REPAIR_APPLY,actor,actor,amount,"aircraft_repair",refund?"Refunded undelivered aircraft repair":"Applied aircraft repair",ticket,0,0);if(!replayed(op))append(op);
    }
    public synchronized boolean surveyed(long chunk){healthy();return aircraft.scouts.known(chunk);}
    public synchronized int surveyedRegions(){healthy();return aircraft.scouts.count();}
    public synchronized Optional<ScoutState.Recognition> surveyRecognition(long chunk){healthy();return aircraft.scouts.recognition(chunk);}
    public synchronized Optional<ScoutState.Award> scoutAward(UUID pilot,long day){healthy();return aircraft.scouts.award(pilot,day);}
    public synchronized long scoutSpent(long day){healthy();return aircraft.scouts.spent(day);}
    public synchronized OptionalLong scoutBudget(long day){healthy();return aircraft.scouts.budget(day);}
    public synchronized Mail awardScout(UUID operation,UUID pilot,long day,long reward,long budget,UUID registration,long generation,int required,int distance,int minimum,long meters,int samples,int originX,int originZ,List<ScoutState.Region> regions)throws IOException{
        var data=new CompoundTag();data.putLong("budget",budget);data.putLong("generation",generation);data.putInt("required",required);data.putInt("distance",distance);data.putInt("minimum",minimum);data.putLong("meters",meters);data.putInt("samples",samples);data.putInt("originX",originX);data.putInt("originZ",originZ);data.put("regions",ScoutData.encode(regions));var op=new Operation(history.size()+1L,operation,Kind.AIRCRAFT_SCOUT_REWARD,pilot,pilot,reward,"kingdom_smp:air_scout","Royal reconnaissance",registration,day,0,data);if(!replayed(op))append(op);return mail.get(operation);
    }
    public synchronized Optional<CargoState.Contract> cargo(UUID pilot,long day){healthy();return aircraft.cargo.get(pilot,day);}
    public synchronized Optional<CargoState.Contract> cargoContract(UUID id){healthy();return aircraft.cargo.get(id);}
    public synchronized Optional<CargoState.Reservation> pendingCargo(UUID registration){healthy();return aircraft.cargo.pending(registration);}
    public synchronized List<CargoState.Contract> pendingCargo(){healthy();return aircraft.cargo.pending();}
    public synchronized Optional<UUID> deliveredCargoReceipt(UUID registration){healthy();return aircraft.cargo.deliveredReceipt(registration);}
    public synchronized Optional<CargoState.Delivery> deliveredCargo(UUID registration){healthy();return aircraft.cargo.delivered(registration);}
    public synchronized long cargoPaid(long day){healthy();return aircraft.cargo.paid(day);}
    public synchronized long cargoHeld(long day){healthy();return aircraft.cargo.held(day);}
    public synchronized OptionalLong cargoBudget(long day){healthy();return aircraft.cargo.budget(day);}
    public synchronized void acceptCargo(CargoState.Terms terms)throws IOException{
        var data=new CompoundTag();data.putString("item",terms.item());data.putInt("minimum",terms.minimum());data.putLong("budget",terms.budget());data.putUUID("origin",terms.origin());data.putUUID("target",terms.target());
        var op=new Operation(history.size()+1L,terms.id(),Kind.AIRCRAFT_CARGO_ACCEPT,terms.pilot(),terms.pilot(),terms.reward(),"kingdom_smp:air_cargo","Accepted air cargo",null,terms.day(),terms.quantity(),data);if(!replayed(op))append(op);
    }
    public synchronized void reserveCargo(UUID actor,CargoState.Reservation reservation,long meters,int samples)throws IOException{
        var terms=aircraft.cargo.get(reservation.contract()).orElseThrow().terms();var data=CargoData.encode(reservation);data.putLong("meters",meters);data.putInt("samples",samples);
        var op=new Operation(history.size()+1L,reservation.id(),Kind.AIRCRAFT_CARGO_RESERVE,actor,actor,0,"kingdom_smp:air_cargo","Reserved native cargo unload",reservation.contract(),terms.day(),0,data);if(!replayed(op))append(op);
    }
    public synchronized void settleCargo(UUID operation,UUID actor,UUID ticket,boolean delivered)throws IOException{
        var old=find(operation);var terms=old==null?aircraft.cargo.ticket(ticket).orElseThrow().terms():null;long amount=old!=null?old.amount():delivered?terms.reward():0;long day=old!=null?old.period():terms.day();
        var op=operation(operation,delivered?Kind.AIRCRAFT_CARGO_DELIVER:Kind.AIRCRAFT_CARGO_CANCEL,actor,actor,amount,"kingdom_smp:air_cargo",delivered?"Delivered native air cargo":"Canceled undelivered native air cargo",ticket,day,0);if(!replayed(op))append(op);
    }
    public synchronized Optional<AircraftState.FlightAward> flightAward(UUID pilot,long day){healthy();return aircraft.flightAward(pilot,day);}
    public synchronized long flightSpent(long day){healthy();return aircraft.flightSpent(day);}
    public synchronized OptionalLong flightBudget(long day){healthy();return aircraft.flightBudget(day);}
    public synchronized Mail awardFlight(UUID operation,UUID pilot,long day,long reward,long budget,UUID registration,long generation,int minimum,long meters,int samples,UUID origin,UUID target)throws IOException{
        var data=new CompoundTag();data.putLong("budget",budget);data.putLong("generation",generation);data.putInt("minimum",minimum);data.putLong("meters",meters);data.putInt("samples",samples);data.putUUID("origin",origin);data.putUUID("target",target);var op=new Operation(history.size()+1L,operation,Kind.AIRCRAFT_FLIGHT_REWARD,pilot,pilot,reward,"kingdom_smp:flight_contract/settlement_dispatch","Royal air dispatch",registration,day,0,data);if(!replayed(op))append(op);return mail.get(operation);
    }
    public synchronized long equipmentCoinsSpent(){return equipmentSpent;}
    public synchronized long shopCoinsSpent(){return shopSpent;}
    /** Debit, finite stock consumption and recoverable delivery commit together. Root validates real counter/catalogue. */
    public synchronized ItemMail shopBuy(UUID operation,UUID buyer,String resource,String title,CompoundTag item,int quantity,long period,int stock,long minimum,long maximum)throws IOException{
        identifier(resource);var data=new CompoundTag();data.put("item",checkedItem(item));data.putInt("stock",stock);data.putLong("minimum",minimum);data.putLong("maximum",maximum);
        var old=find(operation);long amount=old==null?MarketPricing.total(minimum,maximum,shopSold(period,resource),stock,quantity,true):old.amount();
        var op=new Operation(history.size()+1L,operation,Kind.SHOP_BUY,buyer,buyer,amount,resource,title,null,period,quantity,data);
        if(!replayed(op))append(op);return itemMail.get(operation);
    }
    /** Permanently removed from circulation by explicit D-029 approval; retained for audit only. */
    public synchronized long taxRemoved(){return removedTax;}
    public synchronized long expectedReceipt(UUID player){return receiptIndex.getOrDefault(player,0L);}
    /** Caller must require operator permission, exact-target confirmation and a reason. */
    public synchronized void adminSet(UUID operation,UUID actor,UUID target,long amount,String reason)throws IOException{
        var op=operation(operation,Kind.ADMIN_SET,actor,target,amount,"admin",reason,null,0,0);
        if(replayed(op))return;append(op);
    }
    /** Internal server reward enqueue, not player-supplied grants. */
    public synchronized Mail queueCoins(UUID operation,UUID owner,long amount,String reason)throws IOException{
        if(amount<1)throw new IllegalArgumentException("Mail coin amount must be positive");
        var op=operation(operation,Kind.QUEUE_COINS,owner,owner,amount,"coins",reason,null,0,0);
        if(!replayed(op))append(op);return mail.get(operation);
    }
    public synchronized Mail claimCoins(UUID owner,UUID mailId)throws IOException{
        healthy();var entry=mail.get(mailId);
        if(entry==null||!entry.owner().equals(owner)||entry.kind()!=MailKind.COINS)throw new IllegalArgumentException("No such coin delivery");
        if(entry.claimed())return entry;
        append(operation(UUID.randomUUID(),Kind.CLAIM_COINS,owner,owner,entry.amount(),"coins","Mailbox claim",mailId,0,0));return mail.get(mailId);
    }
    /** Root must validate tier, real catalogue price and completed Hall before calling. */
    public synchronized Purchase purchase(UUID operation,UUID buyer,String name,String building,long price)throws IOException{
        identifier(building);text(name,48);
        if(price<500||price>15_000)throw new IllegalArgumentException("Blueprint price outside approved range");
        var op=operation(operation,Kind.PURCHASE,buyer,buyer,price,building,name,null,0,0);
        if(replayed(op))return purchases.get(building);
        append(op);return purchases.get(building);
    }
    /** One atomic account-to-account payment. Caller checks both players at the actual Bank counter. */
    public synchronized void transferCoins(UUID operation,UUID sender,UUID recipient,long amount)throws IOException{
        if(amount<1)throw new IllegalArgumentException("Transfer amount must be positive");
        var op=operation(operation,Kind.COIN_TRANSFER,sender,recipient,amount,"transfer","In-person bank transfer",null,0,0);
        if(replayed(op))return;append(op);
    }
    public synchronized Usage usage(long period,String resource,UUID player){
        if(period<0)throw new IllegalArgumentException("Invalid trading period");identifier(resource);Objects.requireNonNull(player);
        return new Usage(dailyNpcSpent.getOrDefault(period,0L),dailyResourceSold.getOrDefault(new ShopDay(period,resource),0L),dailyPlayerProceeds.getOrDefault(new PlayerDay(period,player),0L));
    }
    /** Budget/allowance are coins, resourceQuota is units; period is server-derived UTC day. */
    public synchronized PendingSale prepareSale(UUID operation,UUID player,String resource,int quantity,long proceeds,long period,long sharedBudget,long resourceQuota,long playerAllowance)throws IOException{
        healthy();if(pending!=null||transfer!=null||pendingItem!=null)throw new IllegalStateException("Economy has an inventory operation pending recovery");
        if(find(operation)!=null)throw new IllegalArgumentException("Inventory sale operation already committed");
        long latest=latestSalePeriod;
        if(period<latest)throw new IllegalArgumentException("Trading period cannot move backwards");
        money(sharedBudget);money(playerAllowance);if(resourceQuota<0)throw new IllegalArgumentException("Invalid resource quota");
        var next=new PendingSale(history.size()+1L,operation,player,resource,quantity,proceeds,period,expectedReceipt(player));
        var used=usage(period,resource,player);
        if(Math.addExact(used.sharedSpent(),proceeds)>sharedBudget||Math.addExact(used.resourceQuantity(),quantity)>resourceQuota||Math.addExact(used.playerProceeds(),proceeds)>playerAllowance)
            throw new IllegalStateException("Daily buying budget, resource quota or player allowance exhausted");
        money(Math.addExact(balance(player),proceeds));capacity();persist(history,next);pending=next;return next;
    }
    /** True commits credited sale; false cancels preparation proven to precede inventory removal. */
    public synchronized boolean recoverSale(Receipt receipt)throws IOException{
        healthy();Objects.requireNonNull(receipt);if(pending==null)return false;
        var sale=pending;boolean same=identity.equals(receipt.store());
        if(same&&receipt.sequence()==sale.sequence()){
            var op=new Operation(sale.sequence(),sale.operation(),Kind.SALE,sale.player(),sale.player(),sale.proceeds(),sale.resource(),"Resource sale",null,sale.period(),sale.quantity());
            appendResolved(op);return true;
        }
        if(receipt.sequence()==sale.previousReceipt()&&(same||receipt.store()==null&&receipt.sequence()==0)){
            persist(history,null);pending=null;return false;
        }
        throw new IOException("Player economy receipt differs from prepared sale; restore matching data");
    }
    /** Reserve one whole native conversion before inventory changes. No other money operation may commit until reconciliation. */
    public synchronized PendingTransfer prepareTransfer(UUID operation,UUID player,long delta)throws IOException{
        healthy();if(pending!=null||transfer!=null||pendingItem!=null)throw new IllegalStateException("Economy has an inventory operation pending recovery");
        if(find(operation)!=null)throw new IllegalArgumentException("Bank operation already committed");
        var next=new PendingTransfer(history.size()+1L,operation,player,delta,expectedReceipt(player));
        money(Math.addExact(balance(player),delta));capacity();persist(history,null,next);transfer=next;return next;
    }
    /** Exact native post-conversion receipt commits; exact previous receipt cancels; anything else refuses. */
    public synchronized boolean recoverTransfer(Receipt receipt)throws IOException{
        healthy();Objects.requireNonNull(receipt);if(transfer==null)return false;
        var pending=transfer;boolean same=identity.equals(receipt.store());
        if(same&&receipt.sequence()==pending.sequence()){
            appendResolved(new Operation(pending.sequence(),pending.operation(),Kind.TRANSFER,pending.player(),pending.player(),pending.delta(),"coins","Bank conversion",null,0,0));return true;
        }
        if(receipt.sequence()==pending.previousReceipt()&&(same||receipt.store()==null&&receipt.sequence()==0)){
            persist(history,null,null);transfer=null;return false;
        }
        throw new IOException("Player bank receipt differs from prepared conversion; restore matching data");
    }
    /** Caller serializes and validates the selected native stack and server-derived tax classification. */
    public synchronized PendingItem prepareListing(UUID operation,UUID seller,String sellerName,String title,long price,boolean master,long created,long expires,CompoundTag item)throws IOException{
        var listing=new Listing(operation,seller,sellerName,title,price,master,created,expires,item,true);
        var data=new CompoundTag();data.putString("sellerName",listing.sellerName());data.putLong("expires",expires);data.putBoolean("master",master);data.put("item",listing.item());
        var op=new Operation(history.size()+1L,operation,Kind.LIST_ITEM,seller,seller,price,"auction",title,null,created,0,data);
        return prepareItem(op,ItemAction.LIST);
    }
    /** Timed escrow starts with exactly twenty-four hours; the service supplies authoritative UTC time. */
    public synchronized PendingItem prepareTimedListing(UUID operation,UUID seller,String sellerName,String title,long price,boolean master,long created,long expires,CompoundTag item)throws IOException{
        if(expires!=Math.addExact(created,TIMED_DURATION))throw new IllegalArgumentException("Timed auctions start with exactly twenty-four hours");
        var listing=new Listing(operation,seller,sellerName,title,price,master,created,expires,item,true,true,null,0,0);
        var data=new CompoundTag();data.putString("sellerName",listing.sellerName());data.putLong("expires",expires);data.putBoolean("master",master);data.put("item",listing.item());
        var op=new Operation(history.size()+1L,operation,Kind.LIST_TIMED_ITEM,seller,seller,price,"auction",title,null,created,0,data);
        return prepareItem(op,ItemAction.TIMED_LIST);
    }
    public synchronized PendingItem prepareItemClaim(UUID operation,UUID owner,UUID delivery)throws IOException{
        var op=operation(operation,Kind.CLAIM_ITEM,owner,owner,0,"auction","Item mail claim",delivery,0,0);
        return prepareItem(op,ItemAction.CLAIM);
    }
    private PendingItem prepareItem(Operation op,ItemAction action)throws IOException{
        healthy();if(pending!=null||transfer!=null||pendingItem!=null)throw new IllegalStateException("Economy paused for inventory reconciliation");
        if(find(op.id())!=null)throw new IllegalArgumentException("Native item operation already committed");
        var nextMail=new HashMap<>(mail);var nextListings=new HashMap<>(listings);var nextItems=new HashMap<>(itemMail);var nextOrders=new HashMap<>(orders);
        apply(op,new HashMap<>(balances),new HashMap<>(purchases),nextMail,nextListings,nextItems,nextOrders,new HashMap<>(shopSold),new HashMap<>(professionRosters),contracts.copy(),community.copy(),equipment.copy(),aircraft.copy(),enchantedOutputs.copy());var nextHistory=appended(history,op);
        maybeArchive();
        budget(nextHistory,nextListings,nextItems,nextMail,nextOrders,action==ItemAction.LIST||action==ItemAction.TIMED_LIST||action==ItemAction.ORDER_FILL||action==ItemAction.CONTRACT_DELIVERY||action==ItemAction.COMMUNITY_DELIVERY,aircraft);
        var item=new PendingItem(op.sequence(),op.id(),op.actor(),action,encodeOperation(op),expectedReceipt(op.actor()));persist(history,null,null,item);pendingItem=item;return item;
    }
    /** Exact post-native receipt commits; the exact previous receipt cancels; no guessed delivery. */
    public synchronized boolean recoverItem(Receipt receipt)throws IOException{
        healthy();Objects.requireNonNull(receipt);if(pendingItem==null)return false;var item=pendingItem;boolean same=identity.equals(receipt.store());
        if(same&&receipt.sequence()==item.sequence()){appendResolved(decode(item.payload()));return true;}
        if(receipt.sequence()==item.previousReceipt()&&(same||receipt.store()==null&&receipt.sequence()==0)){persist(history,null,null,null);pendingItem=null;return false;}
        throw new IOException("Player item receipt differs from prepared operation; restore matching checkpoints");
    }
    /** Sale price/tax class are taken from canonical escrow. Caller validates Bank access and review revision. */
    public synchronized void buyListing(UUID operation,UUID buyer,UUID listingId,long now)throws IOException{
        var listing=Objects.requireNonNull(listings.get(listingId),"Unknown listing");
        var op=operation(operation,Kind.BUY_ITEM,buyer,listing.seller(),listing.price(),"auction","Fixed-price purchase",listingId,now,0);
        if(replayed(op))return;append(op);
    }
    /** Seller may cancel an unsold listing; any server caller may return one after expiry. */
    public synchronized void returnListing(UUID operation,UUID actor,UUID listingId,long now)throws IOException{
        var listing=Objects.requireNonNull(listings.get(listingId),"Unknown listing");
        var op=operation(operation,Kind.RETURN_ITEM,actor,listing.seller(),0,"auction","Listing return",listingId,now,0);
        if(replayed(op))return;append(op);
    }
    /** The first funded bid may equal the starting price; subsequent bids rise by at least ceil(5%). */
    public static long minimumBid(Listing listing){
        Objects.requireNonNull(listing);if(!listing.timed()||!listing.active())throw new IllegalStateException("Not an active timed auction");
        return listing.bidder()==null?listing.price():Math.addExact(listing.bid(),Math.max(1,Math.floorDiv(Math.addExact(listing.bid(),19),20)));
    }
    /** Full bid funding and any prior bidder refund are a single canonical operation. */
    public synchronized void bidListing(UUID operation,UUID bidder,UUID listingId,long amount,long now)throws IOException{
        var listing=Objects.requireNonNull(listings.get(listingId),"Unknown listing");
        var op=operation(operation,Kind.BID_ITEM,bidder,listing.seller(),amount,"auction","Funded timed bid",listingId,now,0);
        if(replayed(op))return;append(op);
    }
    /** Expired funded escrow becomes exactly one buyer item mail and one seller proceeds mail. */
    public synchronized void settleTimed(UUID operation,UUID actor,UUID listingId,long now)throws IOException{
        settleTimed(operation,actor,listingId,now,"");
    }
    /** Nonempty reason is reserved for an explicitly enabled, operator-authorized sandbox deadline test. */
    public synchronized void settleTimed(UUID operation,UUID actor,UUID listingId,long now,String sandboxReason)throws IOException{
        var listing=Objects.requireNonNull(listings.get(listingId),"Unknown listing");
        var payload=new CompoundTag();if(!sandboxReason.isEmpty()){text(sandboxReason,128);payload.putString("sandboxReason",sandboxReason);}
        var op=new Operation(history.size()+1L,operation,Kind.SETTLE_TIMED,actor,listing.seller(),listing.bid(),"auction","Timed auction settlement",listingId,now,0,payload);
        if(replayed(op))return;append(op);
    }
    /** Service validates a plain registered template; payment and ownership commit together. */
    public synchronized void createOrder(UUID operation,UUID buyer,String buyerName,String title,CompoundTag item,int quantity,long unitPrice,boolean master,long created,long expires)throws IOException{
        var order=new BuyOrder(operation,buyer,buyerName,title,item,quantity,quantity,unitPrice,master,created,expires,created,true);
        var data=new CompoundTag();data.putString("buyerName",buyerName);data.put("item",order.item());data.putLong("unitPrice",unitPrice);data.putBoolean("master",master);data.putLong("expires",expires);
        var op=new Operation(history.size()+1L,operation,Kind.CREATE_ORDER,buyer,buyer,Math.multiplyExact(unitPrice,quantity),"buy_order",title,null,created,quantity,data);
        if(replayed(op))return;append(op);
    }
    /** A native removal receipt atomically releases existing order funding and issues exact item mail. */
    public synchronized PendingItem prepareOrderFill(UUID operation,UUID seller,UUID orderId,int quantity,CompoundTag item,long now)throws IOException{
        var order=orders.get(orderId);if(order==null)throw new IllegalStateException("Unknown buy order");var data=new CompoundTag();data.put("item",checkedItem(item));
        var op=new Operation(history.size()+1L,operation,Kind.FILL_ORDER,seller,seller,Math.multiplyExact(order.unitPrice(),quantity),"buy_order","Buy order fill",orderId,now,quantity,data);
        return prepareItem(op,ItemAction.ORDER_FILL);
    }
    /** Only unspent funding is returned; filled units and their history never reset. */
    public synchronized void closeOrder(UUID operation,UUID actor,UUID orderId,long now)throws IOException{closeOrder(operation,actor,orderId,now,"");}
    /** Optional reason belongs to the explicitly enabled operator sandbox route. */
    public synchronized void closeOrder(UUID operation,UUID actor,UUID orderId,long now,String sandboxReason)throws IOException{
        var order=orders.get(orderId);if(order==null)throw new IllegalStateException("Unknown buy order");
        var payload=new CompoundTag();if(!sandboxReason.isEmpty()){text(sandboxReason,128);payload.putString("sandboxReason",sandboxReason);}
        var op=new Operation(history.size()+1L,operation,Kind.CLOSE_ORDER,actor,order.owner(),Math.multiplyExact(order.remaining(),order.unitPrice()),"buy_order","Buy order refund",orderId,now,0,payload);
        if(replayed(op))return;append(op);
    }
    public static long tax(long price,boolean master){money(price);if(price<1)throw new IllegalArgumentException("Invalid auction price");return Math.floorDiv(Math.addExact(Math.multiplyExact(price,master?10L:5L),99),100);}
    private static void auctionShape(Operation op,boolean own){
        if(!op.key().equals("auction")||op.quantity()!=0||own&&!op.actor().equals(op.target()))throw new IllegalArgumentException("Invalid auction operation");
    }
    private static CompoundTag checkedItem(CompoundTag item){
        Objects.requireNonNull(item);require(item,"id",Tag.TAG_STRING);require(item,"count",Tag.TAG_INT);identifier(item.getString("id"));
        if(item.getString("id").equals("minecraft:air")||item.getInt("count")<1||item.getInt("count")>64)throw new IllegalArgumentException("Auction escrow supports one stack of 1–64 items");
        if(item.contains("components"))require(item,"components",Tag.TAG_COMPOUND);
        try{var bytes=new ByteArrayOutputStream();NbtIo.write(item,new DataOutputStream(bytes));if(bytes.size()>65536)throw new IllegalArgumentException("Auction stack exceeds 64 KiB");}
        catch(IOException e){throw new IllegalArgumentException("Invalid item serialization",e);}
        return item.copy();
    }
    private Operation operation(UUID id,Kind kind,UUID actor,UUID target,long amount,String key,String detail,UUID reference,long period,int quantity){
        return new Operation(history.size()+1L,id,kind,actor,target,amount,key,detail,reference,period,quantity);
    }
    private Operation find(UUID id){return operationIndex.get(Objects.requireNonNull(id));}
    private boolean replayed(Operation next){
        healthy();var old=find(next.id());if(old==null)return false;
        if(old.kind()!=next.kind()||!old.actor().equals(next.actor())||!old.target().equals(next.target())||old.amount()!=next.amount()||!old.key().equals(next.key())
                ||!old.detail().equals(next.detail())||!Objects.equals(old.reference(),next.reference())||old.period()!=next.period()||old.quantity()!=next.quantity()||!old.payload().equals(next.payload()))throw new IllegalArgumentException("Operation ID reused for different transaction");
        return true;
    }
    /** Publish derived lookup state only after a validated replay or successful durable append. */
    private void index(Operation op){
        operationIndex.put(op.id(),op);
        if(switch(op.kind()){case COIN_EXCHANGE,SALE,TRANSFER,LIST_ITEM,CLAIM_ITEM,LIST_TIMED_ITEM,FILL_ORDER,COMPLETE_CONTRACT,FARMER_DELIVERY,PROFESSION_DELIVERY,PROFESSION_ENCHANT_DELIVERY,PROFESSION_SMITH_REGISTRATION,COMMUNITY_DELIVERY,AIRCRAFT_DEPLOY->true;default->false;})receiptIndex.put(op.target(),op.sequence());
        if(op.kind()==Kind.SALE){latestSalePeriod=Math.max(latestSalePeriod,op.period());dailyNpcSpent.merge(op.period(),op.amount(),Math::addExact);dailyResourceSold.merge(new ShopDay(op.period(),op.key()),(long)op.quantity(),Math::addExact);dailyPlayerProceeds.merge(new PlayerDay(op.period(),op.target()),op.amount(),Math::addExact);}
        if(op.kind()==Kind.AIRCRAFT_REPAIR_APPLY)shopSpent=Math.addExact(shopSpent,find(op.reference()).amount());
        if(op.kind()==Kind.GEAR_BUY)equipmentSpent=Math.addExact(equipmentSpent,op.amount());
        if(op.kind()==Kind.SHOP_BUY||op.kind()==Kind.AIRCRAFT_BUY)shopSpent=Math.addExact(shopSpent,op.amount());
        if(op.kind()==Kind.BUY_ITEM||op.kind()==Kind.SETTLE_TIMED)removedTax=Math.addExact(removedTax,tax(op.amount(),listings.get(op.reference()).master()));
        else if(op.kind()==Kind.FILL_ORDER)removedTax=Math.addExact(removedTax,tax(op.amount(),orders.get(op.reference()).master()));
        if(op.kind()==Kind.FARMER_DELIVERY){var rank=FarmerQuests.catalog().definition(op.key()).orElseThrow().rank();var inputs=op.payload().getCompound("inputs");for(var item:inputs.getAllKeys())farmerFoodTotals.merge(new FarmerFood(op.target(),rank,item),(long)inputs.getInt(item),Math::addExact);}
    }
    public record ArchiveStatus(long revision,int activeOperations,int closedSegments,long reservedOperations,int activeBytes,boolean nativePending){}
    public synchronized ArchiveStatus archiveStatus(){healthy();return new ArchiveStatus(history.size(),history.size()-archivedCount,archives.size(),(reservedOperations(listings,itemMail,mail,orders)+3L*aircraft.records().size()+2L*aircraft.cargo.pending().size()),encodedBytes(encode(history,null,null,null)),pending!=null||transfer!=null||pendingItem!=null);}
    /** Server-authorized metadata maintenance only. Does not reset IDs, balances, global sequences or native receipts. */
    public synchronized ArchiveStatus archive(UUID actor,String reason)throws IOException{healthy();return archiveTo(history.size(),actor,reason);}
    private void maybeArchive()throws IOException{
        if(pending!=null||transfer!=null||pendingItem!=null)return;
        int active=history.size()-archivedCount;
        if(active>=8192||active>128&&encodedBytes(encode(history,null,null,null))>=MAX_LISTING_JOURNAL_BYTES)archiveTo(history.size()-128,new UUID(0,0),"Automatic closed-journal rotation");
    }
    private ArchiveStatus archiveTo(int cutoff,UUID actor,String reason)throws IOException{
        if(pending!=null||transfer!=null||pendingItem!=null)throw new IllegalStateException("Resolve the matching native inventory checkpoint before archiving");
        Objects.requireNonNull(actor);text(reason,128);if(cutoff<=archivedCount)return archiveStatus();
        List<ListTag> batches=new ArrayList<>();int cursor=archivedCount;
        while(cursor<cutoff){var batch=new ListTag();int bytes=512;while(cursor<cutoff&&batch.size()<EconomyArchive.MAX_SEGMENT_OPERATIONS){var row=encodeOperation(history.get(cursor));int size=encodedBytes(row);if(!batch.isEmpty()&&bytes+size>EconomyArchive.MAX_SEGMENT_BYTES)break;if(bytes+size>EconomyArchive.MAX_SEGMENT_BYTES)throw new IllegalStateException("An economy record exceeds immutable segment capacity");batch.add(row);bytes+=size;cursor++;}batches.add(batch);}
        if(archives.size()+batches.size()>EconomyArchive.MAX_SEGMENTS)throw new IllegalStateException("Economy archive manifest limit reached; preserve and review the complete audit before extending capacity");
        var oldArchives=archives;int oldCount=archivedCount;var next=new ArrayList<>(archives);String previous=next.isEmpty()?"":next.getLast().sha256();long first=archivedCount+1L;
        try{
            for(var batch:batches){var segment=EconomyArchive.seal(file.getParent(),identity,first,previous,batch,actor,reason);next.add(segment);first=segment.last()+1;previous=segment.sha256();}
            // Reject any malformed new chain before changing the authoritative pointer or native inventory.
            EconomyArchive.read(file.getParent(),identity,next);
            archives=List.copyOf(next);archivedCount=cutoff;persist(history,null,null,null);history=aligned(history);
        }catch(IOException|RuntimeException failure){archives=oldArchives;archivedCount=oldCount;quarantined=true;throw failure;}
        com.mojang.logging.LogUtils.getLogger().info("ECONOMY_ARCHIVE identity={} actor={} reason={} closedThrough={} segments={} revision={}",identity,actor,reason,archivedCount,archives.size(),history.size());
        return archiveStatus();
    }
    private void healthy(){if(quarantined)throw new IllegalStateException("Economy storage quarantined; restart and reconcile matching checkpoints");}
    private static long reservedOperations(Map<UUID,Listing> listings,Map<UUID,ItemMail> items,Map<UUID,Mail> mail,Map<UUID,BuyOrder> orders){
        return 2L*orders.values().stream().filter(BuyOrder::active).count()+3L*listings.values().stream().filter(Listing::active).count()+items.values().stream().filter(m->!m.claimed()).count()+mail.values().stream().filter(m->m.kind()==MailKind.COINS&&!m.claimed()).count()+2L*mail.values().stream().filter(m->m.kind()==MailKind.HUNT_ESCROW&&!m.claimed()).count();
    }
    private void budget(List<Operation> operations,Map<UUID,Listing> listings,Map<UUID,ItemMail> items,Map<UUID,Mail> mail,Map<UUID,BuyOrder> orders,boolean listingAdmission,AircraftState aircraft){
        long reserved=reservedOperations(listings,items,mail,orders)+3L*aircraft.records().size()+2L*aircraft.cargo.pending().size();int bytes=encodedBytes(encode(operations,null,null,null));
        if(operations.size()+reserved>EconomyArchive.MAX_TOTAL_OPERATIONS||operations.size()-archivedCount+reserved>MAX_OPERATIONS||bytes+reserved*1024>MAX_JOURNAL_BYTES||listingAdmission&&bytes>MAX_LISTING_JOURNAL_BYTES)
            throw new IllegalStateException("Economy journal/archive admission limit reached; inspect administrator archive status. Existing escrow recovery capacity remains reserved");
    }
    private void capacity()throws IOException{
        maybeArchive();long reserved=(reservedOperations(listings,itemMail,mail,orders)+3L*aircraft.records().size()+2L*aircraft.cargo.pending().size());
        if(history.size()+1L+reserved>EconomyArchive.MAX_TOTAL_OPERATIONS||history.size()-archivedCount+1L+reserved>MAX_OPERATIONS||encodedBytes(encode(history,null,null,null))+1024L+reserved*1024>MAX_JOURNAL_BYTES)
            throw new IllegalStateException("Economy journal/archive admission limit reached; inspect administrator archive status. Existing escrow recovery capacity remains reserved");
    }
    private static int encodedBytes(CompoundTag data){
        try{var bytes=new ByteArrayOutputStream();NbtIo.write(data,new DataOutputStream(bytes));return bytes.size();}
        catch(IOException e){throw new IllegalArgumentException("Invalid checkpoint serialization",e);}
    }
    private void append(Operation op)throws IOException{
        healthy();if(pending!=null||transfer!=null||pendingItem!=null)throw new IllegalStateException("Economy paused for inventory reconciliation");appendResolved(op);
    }
    private void appendResolved(Operation op)throws IOException{
        var nextBalances=new HashMap<>(balances);var nextPurchases=new HashMap<>(purchases);var nextMail=new HashMap<>(mail);var nextListings=new HashMap<>(listings);var nextItemMail=new HashMap<>(itemMail);var nextOrders=new HashMap<>(orders);
        var nextStock=new HashMap<>(shopSold);var nextRosters=new HashMap<>(professionRosters);var nextContracts=contracts.copy();var nextCommunity=community.copy();var nextEquipment=equipment.copy();var nextAircraft=aircraft.copy();var nextEnchanted=enchantedOutputs.copy();apply(op,nextBalances,nextPurchases,nextMail,nextListings,nextItemMail,nextOrders,nextStock,nextRosters,nextContracts,nextCommunity,nextEquipment,nextAircraft,nextEnchanted);var next=appended(history,op);
        if(pending==null&&transfer==null&&pendingItem==null)maybeArchive();
        budget(next,nextListings,nextItemMail,nextMail,nextOrders,op.kind()==Kind.LIST_ITEM||op.kind()==Kind.LIST_TIMED_ITEM||op.kind()==Kind.CREATE_ORDER||op.kind()==Kind.FILL_ORDER||op.kind()==Kind.SHOP_BUY||op.kind()==Kind.COMPLETE_CONTRACT||op.kind()==Kind.COMMUNITY_DELIVERY||op.kind()==Kind.GEAR_BUY||op.kind()==Kind.HUNT_ACCEPT||op.kind()==Kind.AIRCRAFT_BUY||op.kind()==Kind.AIRCRAFT_REPAIR_BUY||op.kind()==Kind.AIRCRAFT_FLIGHT_REWARD||op.kind()==Kind.AIRCRAFT_CARGO_ACCEPT||op.kind()==Kind.AIRCRAFT_CARGO_RESERVE||op.kind()==Kind.AIRCRAFT_SCOUT_REWARD,nextAircraft);persist(next,null);
        balances=nextBalances;purchases=nextPurchases;mail=nextMail;history=appended(history,op);pending=null;transfer=null;listings=nextListings;itemMail=nextItemMail;orders=nextOrders;shopSold=nextStock;professionRosters=nextRosters;contracts=nextContracts;community=nextCommunity;equipment=nextEquipment;aircraft=nextAircraft;enchantedOutputs=nextEnchanted;pendingItem=null;index(op);
    }
    private static void apply(Operation op,Map<UUID,Long> balances,Map<String,Purchase> purchases,Map<UUID,Mail> mail,Map<UUID,Listing> listings,Map<UUID,ItemMail> itemMail,Map<UUID,BuyOrder> orders,Map<ShopDay,Long> shopSold,Map<UUID,ProfessionRoster> professionRosters,ContractState contracts,CommunityContractState community,EquipmentState equipment,AircraftState aircraft,EnchantedOutputState enchantedOutputs){
        if(op.kind().ordinal()<Kind.LIST_ITEM.ordinal()&&!op.payload().isEmpty())throw new IllegalArgumentException("Unexpected economy payload");
        long balance=balances.getOrDefault(op.target(),0L);
        switch(op.kind()){
            case AIRCRAFT_SCOUT_REWARD->{
                var d=op.payload();if(!op.actor().equals(op.target())||op.reference()==null||op.quantity()!=0||!op.key().equals("kingdom_smp:air_scout")||!op.detail().equals("Royal reconnaissance")||mail.containsKey(op.id())||!d.getAllKeys().equals(Set.of("budget","generation","required","distance","minimum","meters","samples","originX","originZ","regions")))throw new IllegalArgumentException("Invalid reconnaissance reward shape");for(String k:List.of("budget","generation","meters"))require(d,k,Tag.TAG_LONG);for(String k:List.of("required","distance","minimum","samples","originX","originZ"))require(d,k,Tag.TAG_INT);var regions=ScoutData.decode(d.get("regions"));aircraft.scouts.award(op.id(),op.actor(),op.period(),op.amount(),d.getLong("budget"),aircraft.get(op.reference()).orElseThrow(),d.getLong("generation"),d.getInt("required"),d.getInt("distance"),d.getInt("minimum"),d.getLong("meters"),d.getInt("samples"),d.getInt("originX"),d.getInt("originZ"),regions);mail.put(op.id(),new Mail(op.id(),op.actor(),MailKind.COINS,op.amount(),"","Royal reconnaissance reward",false));
            }
            case AIRCRAFT_CARGO_ACCEPT->{
                var data=op.payload();if(!op.actor().equals(op.target())||op.reference()!=null||!op.key().equals("kingdom_smp:air_cargo")||!op.detail().equals("Accepted air cargo")||!data.getAllKeys().equals(Set.of("item","minimum","budget","origin","target"))||!data.contains("item",Tag.TAG_STRING)||!data.contains("minimum",Tag.TAG_INT)||!data.contains("budget",Tag.TAG_LONG)||!data.hasUUID("origin")||!data.hasUUID("target"))throw new IllegalArgumentException("Invalid air cargo acceptance");
                aircraft.cargo.accept(new CargoState.Terms(op.id(),op.actor(),op.period(),data.getString("item"),op.quantity(),data.getInt("minimum"),op.amount(),data.getLong("budget"),data.getUUID("origin"),data.getUUID("target")));
            }
            case AIRCRAFT_CARGO_RESERVE->{
                var data=op.payload();if(!op.actor().equals(op.target())||op.reference()==null||op.amount()!=0||op.quantity()!=0||!op.key().equals("kingdom_smp:air_cargo")||!op.detail().equals("Reserved native cargo unload"))throw new IllegalArgumentException("Invalid native cargo reservation");
                var reservation=CargoData.decode(op.id(),op.reference(),data);var terms=aircraft.cargo.get(op.reference()).orElseThrow().terms();if(terms.day()!=op.period())throw new IllegalArgumentException("Cargo period differs from accepted terms");aircraft.cargo.reserve(op.actor(),reservation,aircraft.get(reservation.registration()).orElseThrow(),aircraft.repair(reservation.registration()).isPresent(),data.getLong("meters"),data.getInt("samples"));
            }
            case AIRCRAFT_CARGO_DELIVER,AIRCRAFT_CARGO_CANCEL->{
                boolean delivered=op.kind()==Kind.AIRCRAFT_CARGO_DELIVER;if(!op.actor().equals(op.target())||op.reference()==null||op.quantity()!=0||!op.key().equals("kingdom_smp:air_cargo")||!op.detail().equals(delivered?"Delivered native air cargo":"Canceled undelivered native air cargo")||!op.payload().isEmpty())throw new IllegalArgumentException("Invalid native cargo settlement");var terms=aircraft.cargo.ticket(op.reference()).orElseThrow().terms();if(terms.day()!=op.period()||op.amount()!=(delivered?terms.reward():0)||mail.containsKey(op.reference()))throw new IllegalArgumentException("Cargo settlement differs from its canonical reservation");aircraft.cargo.settle(op.reference(),op.actor(),delivered);if(delivered)mail.put(op.reference(),new Mail(op.reference(),op.actor(),MailKind.COINS,terms.reward(),"","Air cargo delivery reward",false));
            }
            case AIRCRAFT_FLIGHT_REWARD->{
                var data=op.payload();if(!op.actor().equals(op.target())||op.reference()==null||op.quantity()!=0||!op.key().equals("kingdom_smp:flight_contract/settlement_dispatch")||!op.detail().equals("Royal air dispatch")||mail.containsKey(op.id())||!data.getAllKeys().equals(Set.of("budget","generation","minimum","meters","samples","origin","target"))||!data.contains("budget",Tag.TAG_LONG)||!data.contains("generation",Tag.TAG_LONG)||!data.contains("minimum",Tag.TAG_INT)||!data.contains("meters",Tag.TAG_LONG)||!data.contains("samples",Tag.TAG_INT)||!data.hasUUID("origin")||!data.hasUUID("target")||data.getUUID("origin").equals(data.getUUID("target")))throw new IllegalArgumentException("Invalid royal air dispatch proof");aircraft.awardFlight(op.id(),op.actor(),op.period(),op.amount(),data.getLong("budget"),op.reference(),data.getLong("generation"),data.getInt("minimum"),data.getLong("meters"),data.getInt("samples"));mail.put(op.id(),new Mail(op.id(),op.actor(),MailKind.COINS,op.amount(),"","Royal air dispatch reward",false));
            }
            case AIRCRAFT_REPAIR_BUY->{
                var data=op.payload();if(!op.actor().equals(op.target())||op.reference()==null||op.quantity()!=0||op.period()!=0||!op.key().equals("aircraft_repair")||!op.detail().equals("Reserved aircraft repair")||!data.getAllKeys().equals(Set.of("entity","generation","health","rate"))||!data.hasUUID("entity")||!data.contains("generation",Tag.TAG_LONG)||!data.contains("health",Tag.TAG_FLOAT)||!data.contains("rate",Tag.TAG_LONG)||balance<op.amount())throw new IllegalStateException("Invalid or unfunded repair reservation");aircraft.buyRepair(op.id(),op.actor(),op.reference(),data.getUUID("entity"),data.getLong("generation"),data.getFloat("health"),data.getLong("rate"),op.amount());balances.put(op.actor(),balance-op.amount());
            }
            case AIRCRAFT_REPAIR_APPLY,AIRCRAFT_REPAIR_REFUND->{
                boolean refund=op.kind()==Kind.AIRCRAFT_REPAIR_REFUND;if(!op.actor().equals(op.target())||op.reference()==null||op.quantity()!=0||op.period()!=0||!op.payload().isEmpty()||!op.key().equals("aircraft_repair")||!op.detail().equals(refund?"Refunded undelivered aircraft repair":"Applied aircraft repair"))throw new IllegalArgumentException("Invalid aircraft repair settlement");var repair=aircraft.repairTicket(op.reference()).orElseThrow();if(op.amount()!=(refund?repair.price():0))throw new IllegalStateException("Repair settlement differs from its canonical reservation");aircraft.settleRepair(op.reference(),op.actor(),!refund);if(refund){long next=Math.addExact(balance,repair.price());money(next);balances.put(op.actor(),next);}
            }
            case AIRCRAFT_BUY->{
                if(!op.actor().equals(op.target())||op.reference()!=null||op.quantity()!=1||op.period()!=0||op.amount()<1||op.amount()>1_000_000_000L||!op.detail().equals("Registered aircraft purchase")||!op.payload().getAllKeys().equals(Set.of("item"))||balance<op.amount()||itemMail.containsKey(op.id()))throw new IllegalStateException("Aircraft purchase cannot be funded");
                var item=checkedItem(op.payload().getCompound("item"));aircraft.buy(op.id(),op.actor(),op.key(),item);balances.put(op.target(),balance-op.amount());itemMail.put(op.id(),new ItemMail(op.id(),op.target(),item,"Buyer-registered aircraft",false));
            }
            case AIRCRAFT_DEPLOY, AIRCRAFT_ACTIVATE, AIRCRAFT_PACK, AIRCRAFT_SHARE->{
                if(!op.actor().equals(op.target())||op.reference()==null||op.amount()!=0||op.period()!=0||!op.key().equals("aircraft"))throw new IllegalArgumentException("Invalid aircraft operation");
                if(op.kind()==Kind.AIRCRAFT_DEPLOY){if(op.quantity()!=1||!op.detail().equals("Aircraft deployment"))throw new IllegalArgumentException("Invalid deployment");aircraft.deploy(op.reference(),op.actor(),op.payload());}
                else if(op.kind()==Kind.AIRCRAFT_ACTIVATE){if(op.quantity()!=0||!op.payload().isEmpty()||!op.detail().equals("Aircraft activation"))throw new IllegalArgumentException("Invalid activation");aircraft.activate(op.reference(),op.actor());}
                else if(op.kind()==Kind.AIRCRAFT_PACK){if(op.quantity()!=1||!op.payload().getAllKeys().equals(Set.of("item"))||!op.detail().equals("Recovered aircraft airframe")||itemMail.containsKey(op.id()))throw new IllegalArgumentException("Invalid return");var item=checkedItem(op.payload().getCompound("item"));aircraft.pack(op.reference(),op.actor(),item);itemMail.put(op.id(),new ItemMail(op.id(),op.actor(),item,"Recovered registered airframe",false));}
                else{if(op.quantity()!=0||!op.detail().equals("Aircraft shared access")||!op.payload().getAllKeys().equals(Set.of("crew","allow"))||!op.payload().hasUUID("crew")||!op.payload().contains("allow",Tag.TAG_BYTE))throw new IllegalArgumentException("Invalid aircraft crew");aircraft.share(op.reference(),op.actor(),op.payload().getUUID("crew"),op.payload().getBoolean("allow"));}
            }
            case HUNT_ACCEPT->{
                var data=op.payload();
                if(!op.actor().equals(op.target())||op.reference()!=null||!op.detail().equals("Accepted daily hunt")||!data.getAllKeys().equals(Set.of("budget")))throw new IllegalArgumentException("Invalid hunt acceptance");
                require(data,"budget",Tag.TAG_LONG);long pool=data.getLong("budget");
                if(!op.key().equals(HuntState.target(op.period()))||op.quantity()<1||op.quantity()>256||op.amount()<1||op.amount()>100_000||pool<10*op.amount()||pool>1_000_000_000||mail.containsKey(op.id()))throw new IllegalArgumentException("Invalid hunt terms");
                if(contracts.hunts.values().stream().anyMatch(h->h.player().equals(op.target())&&h.day()==op.period()))throw new IllegalStateException("One accepted hunt per player per UTC day");
                if(contracts.huntBudgets.getOrDefault(op.period(),pool)!=pool)throw new IllegalStateException("Hunt pool is pinned for the day");
                long spent=contracts.huntSpent.getOrDefault(op.period(),0L);if(op.amount()>pool-spent)throw new IllegalStateException("Shared hunting reward pool exhausted");
                contracts.huntBudgets.put(op.period(),pool);contracts.huntSpent.put(op.period(),Math.addExact(spent,op.amount()));
                contracts.hunts.put(op.id(),new HuntState(op.id(),op.target(),op.period(),op.key(),op.quantity(),op.amount(),false,false));
                mail.put(op.id(),new Mail(op.id(),op.target(),MailKind.HUNT_ESCROW,op.amount(),"","Reserved hunt payment; complete and return to Garrick before the UTC day ends",false));
            }
            case HUNT_CLOSE->{
                var hunt=contracts.hunts.get(op.reference());
                if(hunt==null||hunt.closed()||!op.actor().equals(op.target())||!hunt.player().equals(op.target())||!op.payload().isEmpty()||!op.key().equals(hunt.target())||!op.detail().equals("Closed daily hunt")||op.amount()!=hunt.reward())throw new IllegalStateException("Hunt cannot be paid or closed twice");
                boolean earned=op.quantity()==hunt.goal();
                if(earned?op.period()!=hunt.day():op.quantity()!=0||op.period()<=hunt.day())throw new IllegalStateException("Hunt expired or cannot expire early");
                var escrow=mail.get(hunt.id());if(escrow==null||escrow.kind()!=MailKind.HUNT_ESCROW||escrow.claimed()||escrow.amount()!=hunt.reward())throw new IllegalStateException("Hunt payment reservation is missing");
                contracts.hunts.put(hunt.id(),hunt.close(earned));
                mail.put(hunt.id(),new Mail(hunt.id(),hunt.player(),earned?MailKind.COINS:MailKind.HUNT_ESCROW,hunt.reward(),"",earned?"Completed daily hunt reward":"Expired daily hunt; unpaid reservation closed",!earned));
            }
            case GEAR_BUY->{
                var data=op.payload();if(!op.actor().equals(op.target())||op.reference()!=null||op.period()!=0||op.quantity()!=1||!op.detail().equals("Farmer equipment purchase")||!data.getAllKeys().equals(Set.of("basePrice","item")))throw new IllegalArgumentException("Invalid Farmer equipment purchase");
                require(data,"basePrice",Tag.TAG_LONG);require(data,"item",Tag.TAG_COMPOUND);var gear=FarmerGear.byId(op.key());var item=checkedItem(data.getCompound("item"));var roster=professionRosters.getOrDefault(op.target(),ProfessionRoster.empty());int previous=equipment.count(op.target(),op.key());
                if(!roster.perksEligible(Profession.FARMER,gear.rank(),FarmerQuests.catalog()))throw new IllegalStateException("Corresponding active profession rank required to purchase equipment");
                if(!item.getString("id").equals(gear.id())||item.getInt("count")!=1||op.amount()!=gear.price(data.getLong("basePrice"),previous)||itemMail.containsKey(op.id()))throw new IllegalArgumentException("Equipment item or exact compounded price differs from approved catalogue");if(balance<op.amount())throw new IllegalStateException("Insufficient equipment funds");
                balances.put(op.target(),balance-op.amount());equipment.counts.put(new EquipmentState.Key(op.target(),op.key()),Math.addExact(previous,1));itemMail.put(op.id(),new ItemMail(op.id(),op.target(),item,gear.title()+" purchased equipment",false));
            }
            case COMMUNITY_DELIVERY->{
                boolean fishing=op.key().startsWith("kingdom_smp:fishing_contract/");var data=op.payload();if(!op.actor().equals(op.target())||op.reference()!=null||!op.detail().equals(fishing?"Wharf fishing contract":"Community food contract")||!data.getAllKeys().equals(Set.of("item","goal","unitReward","budget")))throw new IllegalArgumentException("Invalid community delivery");
                require(data,"item",Tag.TAG_STRING);require(data,"goal",Tag.TAG_INT);require(data,"unitReward",Tag.TAG_INT);require(data,"budget",Tag.TAG_LONG);
                String item=data.getString("item");int goal=data.getInt("goal"),unit=data.getInt("unitReward");long budget=data.getLong("budget");
                var approvedItems=Map.of("kingdom_smp:community/grain","minecraft:wheat","kingdom_smp:community/bread","minecraft:bread","kingdom_smp:community/roots","minecraft:carrot","kingdom_smp:fishing_contract/bluegill","aquaculture:bluegill","kingdom_smp:fishing_contract/brown_trout","aquaculture:brown_trout","kingdom_smp:fishing_contract/catfish","aquaculture:catfish");
                if(!approvedItems.containsKey(op.key())||!approvedItems.get(op.key()).equals(item)||goal<10||goal>65536||unit<1||unit>100||budget<1||budget>1_000_000_000||op.quantity()<1||op.quantity()>64||op.amount()!=Math.multiplyExact((long)op.quantity(),unit)||mail.containsKey(op.id()))throw new IllegalArgumentException("Invalid community contract terms");
                if(fishing&&community.projects.values().stream().anyMatch(p->p.key().week()==op.period()&&p.key().project().startsWith("kingdom_smp:fishing_contract/")&&p.budget()!=budget))throw new IllegalStateException("Fishing weekly pool is pinned by the first delivery");
                var key=new CommunityContractState.Key(op.key(),op.period());var current=community.projects.get(key);int delivered=current==null?0:current.delivered();
                if(current!=null&&(!current.item().equals(item)||current.goal()!=goal||current.unitReward()!=unit||current.budget()!=budget))throw new IllegalStateException("Community project terms are pinned until its period ends");
                if(op.quantity()>goal-delivered||op.amount()>budget-(fishing?community.fishingSpent(op.period()):community.spent(op.period())))throw new IllegalStateException("Community contract completed or weekly reward pool exhausted");
                var contributions=current==null?new HashMap<UUID,CommunityContractState.Contribution>():new HashMap<>(current.contributors());var before=contributions.getOrDefault(op.target(),new CommunityContractState.Contribution(0,0));var roster=professionRosters.getOrDefault(op.target(),ProfessionRoster.empty());boolean master=!fishing&&roster.isActive(Profession.FARMER)&&FarmerQuests.band(roster)==ProfessionRank.MASTER;
                contributions.put(op.target(),new CommunityContractState.Contribution(before.total()+op.quantity(),before.master()+(master?op.quantity():0)));community.projects.put(key,new CommunityContractState.Project(key,item,goal,unit,budget,delivered+op.quantity(),contributions));mail.put(op.id(),new Mail(op.id(),op.target(),MailKind.COINS,op.amount(),"",fishing?"Wharf fishing contract reward":"Community food contract reward",false));
            }
            case FARMER_DELIVERY->{
                var data=op.payload();if(!op.actor().equals(op.target())||op.reference()!=null||op.period()!=0||op.quantity()!=0||!op.detail().equals("Farmer food delivery")
                        ||!data.getAllKeys().equals(Set.of("evidence","contentVersion","inputs")))throw new IllegalArgumentException("Invalid Farmer delivery");
                require(data,"evidence",Tag.TAG_STRING);require(data,"contentVersion",Tag.TAG_INT);require(data,"inputs",Tag.TAG_COMPOUND);
                if(data.getInt("contentVersion")!=1||data.getString("evidence").isBlank()||data.getString("evidence").length()>256||op.amount()!=FarmerQuests.xp(op.key()))throw new IllegalArgumentException("Invalid Farmer delivery content");
                FarmerDeliveries.validate(op.key(),data.getCompound("inputs"));
                var roster=professionRosters.getOrDefault(op.target(),ProfessionRoster.empty());if(roster.track(Profession.FARMER).completedQuestIds().contains(op.key()))throw new IllegalStateException("Farmer delivery already completed");
                professionRosters.put(op.target(),roster.completeQuest(Profession.FARMER,op.key(),op.amount(),FarmerQuests.catalog()));
            }
            case FARMER_INSPECTION->{
                var data=op.payload();if(!FarmerQuests.inspection(op.key())||op.reference()!=null||op.period()!=0||op.quantity()!=0||!op.detail().equals("Farmer site inspection")||!data.getAllKeys().equals(Set.of("evidence","contentVersion")))throw new IllegalArgumentException("Invalid Farmer inspection");
                require(data,"evidence",Tag.TAG_STRING);require(data,"contentVersion",Tag.TAG_INT);
                if(data.getInt("contentVersion")!=1||data.getString("evidence").isBlank()||data.getString("evidence").length()>256||op.amount()!=FarmerQuests.xp(op.key()))throw new IllegalArgumentException("Invalid Farmer inspection content");
                var roster=professionRosters.getOrDefault(op.target(),ProfessionRoster.empty());if(roster.track(Profession.FARMER).completedQuestIds().contains(op.key()))throw new IllegalStateException("Farmer inspection already completed");professionRosters.put(op.target(),roster.completeQuest(Profession.FARMER,op.key(),op.amount(),FarmerQuests.catalog()));
            }
            case PROFESSION_QUEST->{
                var data=op.payload();
                if(FarmerQuests.inspection(op.key())||FarmerQuests.food(op.key())||!op.actor().equals(op.target())||op.reference()!=null||op.period()!=0||op.quantity()!=0||!op.detail().equals("Farmer objective completed")
                        ||!data.getAllKeys().equals(Set.of("evidence","contentVersion")))throw new IllegalArgumentException("Invalid Farmer completion");
                require(data,"evidence",Tag.TAG_STRING);require(data,"contentVersion",Tag.TAG_INT);
                if(data.getInt("contentVersion")!=1||data.getString("evidence").isBlank()||data.getString("evidence").length()>256||op.amount()!=FarmerQuests.xp(op.key()))throw new IllegalArgumentException("Invalid approved Farmer content/XP");
                var roster=professionRosters.getOrDefault(op.target(),ProfessionRoster.empty());
                if(roster.track(Profession.FARMER).completedQuestIds().contains(op.key()))throw new IllegalStateException("Farmer objective already completed");
                professionRosters.put(op.target(),roster.completeQuest(Profession.FARMER,op.key(),op.amount(),FarmerQuests.catalog()));
            }
            case PROFESSION_ENCHANT_OUTPUT->{
                var data=op.payload();if(!op.actor().equals(op.target())||op.reference()!=null||op.period()!=0||op.quantity()!=0||op.amount()!=0||!NativeOutputQuests.contains(op.key())||!op.detail().equals(NativeOutputQuests.issueDetail(op.key()))||(!data.getAllKeys().equals(Set.of("item"))&&!data.getAllKeys().equals(Set.of("item","attributes")))||!data.contains("item",Tag.TAG_COMPOUND)||data.contains("attributes")&&!data.contains("attributes",Tag.TAG_COMPOUND))throw new IllegalArgumentException("Invalid native enchanted output authority");
                var item=checkedItem(data.getCompound("item"));if(item.getInt("count")!=1||!NativeOutputQuests.output(op.key(),item.getString("id")))throw new IllegalArgumentException("Ordinary single native tool required");
                if(BrewQuests.contains(op.key()))BrewQuests.validateEncoded(op.key(),item);var role=NativeOutputQuests.get(op.key()).profession();var roster=professionRosters.getOrDefault(op.target(),ProfessionRoster.empty());if(!roster.isActive(role)||roster.track(role).completedQuestIds().contains(op.key())||!NativeOutputQuests.catalog().mayAttempt(role,op.key(),roster.track(role).completedQuestIds()))throw new IllegalStateException("Enchanted commission unavailable");var attributes=data.contains("attributes")?NativeBookProof.load(data.getCompound("attributes")):NativeBookProof.EMPTY;if(op.key().equals(NativeOutputQuests.LIBRARY_FAMILIES)&&attributes.families()==0||op.key().equals(NativeOutputQuests.LIBRARY_MAXIMUM)&&!attributes.maximum())throw new IllegalArgumentException("Missing verified native book facts");enchantedOutputs.issue(op.id(),op.actor(),op.key(),item,attributes);
            }
            case PROFESSION_ENCHANT_DELIVERY,PROFESSION_SMITH_REGISTRATION->{
                boolean smith=op.kind()==Kind.PROFESSION_SMITH_REGISTRATION;var data=op.payload();if(!op.actor().equals(op.target())||op.reference()!=null||op.period()!=0||op.quantity()!=0||!NativeOutputQuests.contains(op.key())||op.amount()!=NativeOutputQuests.get(op.key()).xp()||smith==NativeOutputQuests.consume(op.key())||!op.detail().equals(NativeOutputQuests.receiptDetail(op.key()))||!data.getAllKeys().equals(Set.of("required","outputs"))||!data.contains("required",Tag.TAG_INT)||!(data.get("outputs") instanceof ListTag rows)||rows.size()<1||rows.size()>128||rows.getElementType()!=Tag.TAG_COMPOUND)throw new IllegalArgumentException("Invalid authenticated enchanted delivery");
                Map<UUID,CompoundTag> items=new HashMap<>();for(var value:rows){var row=(CompoundTag)value;if(!row.getAllKeys().equals(Set.of("id","item"))||!row.hasUUID("id")||!row.contains("item",Tag.TAG_COMPOUND)||items.put(row.getUUID("id"),checkedItem(row.getCompound("item")))!=null)throw new IllegalArgumentException("Duplicate or malformed enchanted output");}
                var role=NativeOutputQuests.get(op.key()).profession();var roster=professionRosters.getOrDefault(op.target(),ProfessionRoster.empty());if(roster.track(role).completedQuestIds().contains(op.key()))throw new IllegalStateException("Native output objective already completed");if(BrewQuests.contains(op.key()))BrewQuests.validateDelivery(op.key(),items.values(),data.getInt("required"));enchantedOutputs.spend(op.actor(),op.key(),items,data.getInt("required"));professionRosters.put(op.target(),roster.completeQuest(role,op.key(),op.amount(),NativeOutputQuests.catalog()));
            }
            case PROFESSION_REPAIR->{
                var data=op.payload();if(!op.actor().equals(op.target())||op.reference()!=null||op.period()!=0||!RepairService.contains(op.key())||!op.detail().equals("Native item repairs")||op.amount()!=ApprovedProfessionContent.get(op.key()).xp()||!data.getAllKeys().equals(Set.of("contentVersion","required"))||!data.contains("contentVersion",Tag.TAG_INT)||data.getInt("contentVersion")!=1||!data.contains("required",Tag.TAG_INT)||data.getInt("required")<1||data.getInt("required")>128||op.quantity()<data.getInt("required")||op.quantity()>RepairEvidence.LIMIT)throw new IllegalArgumentException("Invalid native repair authority or proof count");
                var roster=professionRosters.getOrDefault(op.target(),ProfessionRoster.empty());if(roster.track(Profession.BLACKSMITH).completedQuestIds().contains(op.key()))throw new IllegalStateException("Repair objective already completed");professionRosters.put(op.target(),roster.completeQuest(Profession.BLACKSMITH,op.key(),op.amount(),RepairService.catalog()));
            }
            case PROFESSION_ENCOUNTER->{
                var data=op.payload();if(!op.key().equals(EncounterQuests.QUEST)||!op.actor().equals(op.target())||op.amount()!=100||op.reference()!=null||op.period()!=0||op.quantity()!=0||!op.detail().equals("Native natural encounters")||!data.getAllKeys().equals(Set.of("contentVersion","total","perSpecies","counts")))throw new IllegalArgumentException("Invalid encounter authority");
                for(var key:List.of("contentVersion","total","perSpecies"))require(data,key,Tag.TAG_INT);require(data,"counts",Tag.TAG_COMPOUND);if(data.getInt("contentVersion")!=1)throw new IllegalArgumentException("Invalid encounter version");var rules=new EncounterRules(data.getInt("total"),data.getInt("perSpecies"));var counts=data.getCompound("counts");if(!counts.getAllKeys().equals(Set.copyOf(HuntState.TARGETS)))throw new IllegalArgumentException("Missing encounter species");int total=0;for(var type:HuntState.TARGETS){require(counts,type,Tag.TAG_INT);int count=counts.getInt(type);if(count<rules.perSpecies()||count>EncounterEvidence.LIMIT)throw new IllegalArgumentException("Invalid encounter count");total=Math.addExact(total,count);}if(total<rules.total()||total>EncounterEvidence.LIMIT)throw new IllegalArgumentException("Incomplete encounters");
                var roster=professionRosters.getOrDefault(op.target(),ProfessionRoster.empty());if(roster.track(Profession.ADVENTURER).completedQuestIds().contains(op.key()))throw new IllegalStateException("Encounter objective already completed");professionRosters.put(op.target(),roster.completeQuest(Profession.ADVENTURER,op.key(),100,EncounterQuests.catalog()));
            }
            case PROFESSION_MINING->{
                if(!NativeOreQuests.contains(op.key()))throw new IllegalArgumentException("Unvalidated native mining objective");var approved=ApprovedProfessionContent.get(op.key());var data=op.payload();
                if(!op.actor().equals(op.target())||op.reference()!=null||op.period()!=0||!op.detail().equals("Native ore extraction")||!data.getAllKeys().equals(Set.of("contentVersion","required")))throw new IllegalArgumentException("Invalid mining completion authority");require(data,"contentVersion",Tag.TAG_INT);require(data,"required",Tag.TAG_INT);
                if(data.getInt("contentVersion")!=1||data.getInt("required")<1||data.getInt("required")>100000||op.quantity()<data.getInt("required")||op.quantity()>ProfessionEvidence.MAX_COUNTER||op.amount()!=approved.xp())throw new IllegalArgumentException("Invalid mining count or XP");
                var roster=professionRosters.getOrDefault(op.target(),ProfessionRoster.empty());if(roster.track(Profession.MINER).completedQuestIds().contains(op.key()))throw new IllegalStateException("Mining objective already completed");professionRosters.put(op.target(),roster.completeQuest(Profession.MINER,op.key(),op.amount(),NativeOreQuests.catalog()));
            }
            case PROFESSION_TREE->{
                var d=TreeQuestService.get(op.key());var approved=ApprovedProfessionContent.get(op.key());var data=op.payload();
                if(!op.actor().equals(op.target())||op.reference()!=null||op.period()!=0||!op.detail().equals("Native tree objective")||!data.getAllKeys().equals(Set.of("contentVersion","required")))throw new IllegalArgumentException("Invalid native tree completion");
                require(data,"contentVersion",Tag.TAG_INT);require(data,"required",Tag.TAG_INT);
                if(data.getInt("contentVersion")!=1||data.getInt("required")<1||data.getInt("required")>100000||d.metric().equals("mosaic")&&(data.getInt("required")<6||data.getInt("required")%6!=0)||op.quantity()<data.getInt("required")||op.quantity()>ProfessionEvidence.MAX_COUNTER||op.amount()!=approved.xp())throw new IllegalArgumentException("Invalid native tree count or XP");
                var roster=professionRosters.getOrDefault(op.target(),ProfessionRoster.empty());if(roster.track(Profession.LUMBERJACK).completedQuestIds().contains(op.key()))throw new IllegalStateException("Tree objective already completed");
                professionRosters.put(op.target(),roster.completeQuest(Profession.LUMBERJACK,op.key(),op.amount(),TreeQuestService.catalog()));
            }
            case PROFESSION_WORKSITE->{
                var data=op.payload();
                if(!data.getAllKeys().equals(Set.of("contentVersion","site"))||op.amount()!=0||op.reference()!=null||op.period()!=0||op.quantity()!=0||!op.detail().equals("Builder worksite authorization"))throw new IllegalArgumentException("Invalid worksite operation");
                require(data,"contentVersion",Tag.TAG_INT);require(data,"site",Tag.TAG_COMPOUND);if(data.getInt("contentVersion")!=1)throw new IllegalArgumentException("Unknown worksite content version");
                var site=BuilderWorksite.load(data.getCompound("site"));var d=RetainedBuildingQuests.get(op.key());
                if(!site.id().equals(op.id())||!site.admin().equals(op.actor())||!site.player().equals(op.target())||!site.quest().equals(op.key())||contracts.workSites.containsKey(site.id())||contracts.workSites.size()>=RetainedPlacementEvidence.MAX_SITES)throw new IllegalArgumentException("Invalid worksite identity or capacity");
                var roster=professionRosters.getOrDefault(op.target(),ProfessionRoster.empty());
                if(!roster.isActive(d.profession())||roster.track(d.profession()).completedQuestIds().contains(op.key())||!RetainedBuildingQuests.catalog().mayAttempt(d.profession(),op.key(),roster.track(d.profession()).completedQuestIds()))throw new IllegalStateException("Worksite profession/objective unavailable");
                contracts.workSites.put(site.id(),site);contracts.currentSites.put(new ContractState.WorksiteSlot(site.player(),site.quest()),site.id());
            }
            case PROFESSION_RETAINED->{
                if(!RetainedBuildingQuests.contains(op.key()))throw new IllegalArgumentException("Inspection objectives cannot use automatic retained awards");
                var d=RetainedBuildingQuests.get(op.key());var data=op.payload();
                if(!op.actor().equals(op.target())||op.reference()!=null||op.period()!=0||!op.detail().equals("Retained player construction")||!data.getAllKeys().equals(Set.of("contentVersion","site","required","snapshot")))throw new IllegalArgumentException("Invalid retained construction completion");
                require(data,"contentVersion",Tag.TAG_INT);require(data,"required",Tag.TAG_INT);require(data,"snapshot",Tag.TAG_STRING);
                if(!data.hasUUID("site")||data.getInt("contentVersion")!=1||data.getInt("required")<1||data.getInt("required")>2048||op.quantity()<data.getInt("required")||op.quantity()>32768||op.amount()!=d.xp()||!data.getString("snapshot").matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid retained quantity, snapshot or XP");
                var site=contracts.workSites.get(data.getUUID("site"));
                if(site==null||!site.player().equals(op.target())||!site.quest().equals(op.key())||!site.id().equals(contracts.currentSites.get(new ContractState.WorksiteSlot(op.target(),op.key()))))throw new IllegalStateException("Retained evidence has no matching current authorization");
                var roster=professionRosters.getOrDefault(op.target(),ProfessionRoster.empty());if(roster.track(d.profession()).completedQuestIds().contains(op.key()))throw new IllegalStateException("Retained objective already completed");
                professionRosters.put(op.target(),roster.completeQuest(d.profession(),op.key(),op.amount(),RetainedBuildingQuests.catalog()));
            }
            case PROFESSION_DELIVERY->{
                var d=CraftDeliveryQuests.get(op.key());var data=op.payload();
                if(!op.actor().equals(op.target())||op.reference()!=null||op.period()!=0||op.quantity()!=0||!op.detail().equals("Profession crafted delivery")||!data.getAllKeys().equals(Set.of("contentVersion","evidence","inputs")))throw new IllegalArgumentException("Invalid crafted profession delivery");
                require(data,"contentVersion",Tag.TAG_INT);require(data,"evidence",Tag.TAG_STRING);require(data,"inputs",Tag.TAG_COMPOUND);
                if(data.getInt("contentVersion")!=1||data.getString("evidence").isBlank()||data.getString("evidence").length()>256||op.amount()!=d.xp())throw new IllegalArgumentException("Invalid crafted delivery evidence/content/XP");
                CraftDeliveryQuests.validate(op.key(),data.getCompound("inputs"));
                var roster=professionRosters.getOrDefault(op.target(),ProfessionRoster.empty());
                if(roster.track(d.profession()).completedQuestIds().contains(op.key()))throw new IllegalStateException("Craft delivery already completed");
                professionRosters.put(op.target(),roster.completeQuest(d.profession(),op.key(),op.amount(),CraftDeliveryQuests.catalog()));
            }
            case PROFESSION_INSPECTION->{
                var d=InspectionQuests.get(op.key());var data=op.payload();
                if(op.reference()!=null||op.period()!=0||op.quantity()!=0||!op.detail().equals("Profession site inspection")||!data.getAllKeys().equals(Set.of("contentVersion","evidence")))throw new IllegalArgumentException("Invalid profession inspection");
                require(data,"contentVersion",Tag.TAG_INT);require(data,"evidence",Tag.TAG_STRING);
                if(data.getInt("contentVersion")!=1||data.getString("evidence").isBlank()||data.getString("evidence").length()>256||op.amount()!=d.xp())throw new IllegalArgumentException("Invalid inspection audit/content/XP");
                var roster=professionRosters.getOrDefault(op.target(),ProfessionRoster.empty());
                if(roster.track(d.profession()).completedQuestIds().contains(op.key()))throw new IllegalStateException("Profession inspection already completed");
                professionRosters.put(op.target(),roster.completeQuest(d.profession(),op.key(),op.amount(),InspectionQuests.catalog()));
            }
            case COIN_EXCHANGE->{
                var data=op.payload();if(!op.actor().equals(op.target())||op.reference()!=null||op.period()!=0||op.amount()!=0||!op.key().equals("coins")||!op.detail().equals("Equal-value physical coin exchange")||!data.getAllKeys().equals(Set.of("source","target"))||!data.contains("source",Tag.TAG_STRING)||!data.contains("target",Tag.TAG_STRING))throw new IllegalArgumentException("Invalid coin exchange");
                var terms=new CoinExchangeTerms(data.getString("source"),data.getString("target"),op.quantity());contracts.merchant.exchanged(op.target(),terms,professionRosters);
            }
            case MERCHANT_TRANSACTION_QUEST->{
                var d=MerchantTransactionQuests.get(op.key());var data=op.payload();
                if(!op.actor().equals(op.target())||op.reference()!=null||op.period()!=0||op.quantity()!=0||!op.detail().equals("Merchant settled transactions")||!data.getAllKeys().equals(Set.of("contentVersion","required"))||!data.contains("contentVersion",Tag.TAG_INT)||!data.contains("required",Tag.TAG_INT)||data.getInt("contentVersion")!=1||op.amount()!=d.xp())throw new IllegalArgumentException("Invalid Merchant transaction objective");
                var roster=professionRosters.getOrDefault(op.target(),ProfessionRoster.empty());
                if(roster.track(Profession.MERCHANT).completedQuestIds().contains(op.key())||!contracts.merchant.get(op.target()).ready(op.key(),data.getInt("required")))throw new IllegalStateException("No qualifying settled Merchant transactions");
                professionRosters.put(op.target(),roster.completeQuest(Profession.MERCHANT,op.key(),op.amount(),MerchantTransactionQuests.catalog()));
            }
            case PROFESSION_COMMISSION->{
                var d=CommissionQuests.get(op.key());var data=op.payload();
                if(!op.actor().equals(op.target())||op.reference()!=null||op.period()!=0||op.quantity()!=0||!op.detail().equals("Profession commissions completed")
                    ||!data.getAllKeys().equals(Set.of("contentVersion","required")))throw new IllegalArgumentException("Invalid profession commission");
                require(data,"contentVersion",Tag.TAG_INT);require(data,"required",Tag.TAG_INT);
                int required=data.getInt("required");
                if(data.getInt("contentVersion")!=1||required<1||required>365||op.amount()!=CommissionQuests.xp(op.key()))throw new IllegalArgumentException("Invalid commission content/XP");
                var roster=professionRosters.getOrDefault(op.target(),ProfessionRoster.empty());
                if(roster.track(d.profession()).completedQuestIds().contains(op.key()))throw new IllegalStateException("Commission quest already completed");
                long count=commissionPeriods(contracts,op.target(),op.key());
                if(count<required)throw new IllegalStateException("Not enough distinct canonical commission periods");
                professionRosters.put(op.target(),roster.completeQuest(d.profession(),op.key(),op.amount(),CommissionQuests.catalog()));
            }
            case COMPLETE_CONTRACT->{
                var data=op.payload();if(!data.getAllKeys().equals(Set.of("profession","cycle","required","pool","item"))&&!data.getAllKeys().equals(Set.of("profession","cycle","required","pool","item","questTier")))throw new IllegalArgumentException("Invalid contract payload");
                int tier=0;if(data.contains("questTier")){require(data,"questTier",Tag.TAG_INT);tier=data.getInt("questTier");if(tier<1||tier>12)throw new IllegalArgumentException("Invalid contract objective tier");}
                require(data,"profession",Tag.TAG_STRING);require(data,"cycle",Tag.TAG_STRING);require(data,"required",Tag.TAG_INT);require(data,"pool",Tag.TAG_LONG);require(data,"item",Tag.TAG_COMPOUND);
                var role=Profession.byId(data.getString("profession"));String cycle=data.getString("cycle");long pool=data.getLong("pool");var item=checkedItem(data.getCompound("item"));
                if(!Set.of("daily","weekly").contains(cycle)||!op.actor().equals(op.target())||op.reference()!=null||op.quantity()<1||op.quantity()>2304||op.quantity()!=data.getInt("required")
                        ||op.amount()<1||op.amount()>1_000_000||pool<op.amount()||pool>1_000_000_000L||!op.key().equals("kingdom_smp:contract/"+role.id()+"/"+cycle)||!op.detail().equals("Profession delivery contract")
                        ||item.getInt("count")!=1||mail.containsKey(op.id()))throw new IllegalArgumentException("Invalid contract terms");
                if(!professionRosters.getOrDefault(op.target(),ProfessionRoster.empty()).isActive(role))throw new IllegalStateException("Contract profession is inactive");
                var slot=new ContractState.Slot(op.target(),role.id(),cycle,op.period());var key=new ContractState.Pool(cycle,op.period());long spent=contracts.spent(cycle,op.period());
                if(contracts.completed.contains(slot)||op.amount()>pool-spent)throw new IllegalStateException("Contract already completed or shared reward pool exhausted");
                contracts.completed.add(slot);contracts.spent.put(key,Math.addExact(spent,op.amount()));
                contracts.professionPeriods.add(new ContractState.QuestPeriod(op.target(),role.id(),professionRosters.get(op.target()).progressBand(role).id(),cycle,op.period(),tier));
                mail.put(op.id(),new Mail(op.id(),op.target(),MailKind.COINS,op.amount(),"",cycle+" "+role.id()+" contract reward",false));
            }
            case PROFESSION_CHANGE->{
                if(!op.actor().equals(op.target())||op.reference()!=null||!op.key().equals("profession")||!op.detail().equals("Profession change")||op.quantity()!=0||op.period()!=0)throw new IllegalArgumentException("Invalid profession change");
                var transition=professionTransition(professionRosters.getOrDefault(op.target(),ProfessionRoster.empty()),op.payload());
                if(!transition.changed()||op.amount()!=transition.fee())throw new IllegalArgumentException("Profession change/fee differs from authoritative roster");
                if(balance<op.amount())throw new IllegalStateException("Profession changes cost15000coins; insufficient balance");
                balances.put(op.target(),balance-op.amount());professionRosters.put(op.target(),transition.roster());
            }
            case SHOP_BUY->{
                var data=op.payload();
                if(!op.actor().equals(op.target())||op.reference()!=null||op.quantity()<1||op.quantity()>64||itemMail.containsKey(op.id())
                        ||!data.getAllKeys().equals(Set.of("item","stock","minimum","maximum")))throw new IllegalArgumentException("Invalid shop purchase");
                identifier(op.key());require(data,"item",Tag.TAG_COMPOUND);require(data,"stock",Tag.TAG_INT);require(data,"minimum",Tag.TAG_LONG);require(data,"maximum",Tag.TAG_LONG);
                var item=checkedItem(data.getCompound("item"));var key=new ShopDay(op.period(),op.key());long sold=shopSold.getOrDefault(key,0L);
                long expected=MarketPricing.total(data.getLong("minimum"),data.getLong("maximum"),sold,data.getInt("stock"),op.quantity(),true);
                if(op.amount()!=expected||item.getInt("count")!=op.quantity())throw new IllegalArgumentException("Shop price or item count differs from pinned stock policy");
                if(balance<op.amount())throw new IllegalStateException("Insufficient coins for shop purchase");
                balances.put(op.target(),balance-op.amount());shopSold.put(key,Math.addExact(sold,op.quantity()));
                itemMail.put(op.id(),new ItemMail(op.id(),op.target(),item,op.detail(),false));
            }
            case CREATE_ORDER->{
                var data=op.payload();
                if(!op.key().equals("buy_order")||!op.actor().equals(op.target())||op.reference()!=null||orders.containsKey(op.id())
                        ||!data.getAllKeys().equals(Set.of("buyerName","item","unitPrice","master","expires")))throw new IllegalArgumentException("Invalid buy order creation");
                require(data,"buyerName",Tag.TAG_STRING);require(data,"item",Tag.TAG_COMPOUND);require(data,"unitPrice",Tag.TAG_LONG);require(data,"master",Tag.TAG_BYTE);require(data,"expires",Tag.TAG_LONG);
                var order=new BuyOrder(op.id(),op.actor(),data.getString("buyerName"),op.detail(),data.getCompound("item"),op.quantity(),op.quantity(),data.getLong("unitPrice"),data.getBoolean("master"),op.period(),data.getLong("expires"),op.period(),true);
                if(op.amount()!=Math.multiplyExact(order.quantity(),order.unitPrice()))throw new IllegalArgumentException("Order funding differs from cost");
                if(orders.values().stream().filter(BuyOrder::active).count()>=100||orders.values().stream().filter(v->v.active()&&v.owner().equals(op.actor())).count()>=10)throw new IllegalStateException("Buy order slots exhausted (ten per player, one hundred per kingdom)");
                if(balance<op.amount())throw new IllegalStateException("Insufficient coins to fund the whole order");
                balances.put(op.actor(),balance-op.amount());orders.put(op.id(),order);
            }
            case FILL_ORDER->{
                var order=orders.get(op.reference());var data=op.payload();
                if(order==null||!order.active()||order.owner().equals(op.actor())||!op.actor().equals(op.target())||!op.key().equals("buy_order")||!op.detail().equals("Buy order fill")
                        ||op.period()<order.lastFill()||op.period()>=order.expires()||op.quantity()<1||op.quantity()>64||op.quantity()>order.remaining()
                        ||op.amount()!=Math.multiplyExact(order.unitPrice(),op.quantity())||!data.getAllKeys().equals(Set.of("item"))||mail.containsKey(op.id())||itemMail.containsKey(op.id()))throw new IllegalStateException("Buy order fill is unavailable");
                require(data,"item",Tag.TAG_COMPOUND);var actual=checkedItem(data.getCompound("item"));var template=actual.copy();template.putInt("count",1);
                if(actual.getInt("count")!=op.quantity()||!template.equals(order.item()))throw new IllegalArgumentException("Filled item differs from the exact order template");
                long proceeds=op.amount()-tax(op.amount(),order.master());
                if(proceeds>0)mail.put(op.id(),new Mail(op.id(),op.actor(),MailKind.COINS,proceeds,"","Buy order proceeds",false));
                itemMail.put(op.id(),new ItemMail(op.id(),order.owner(),actual,order.title(),false));orders.put(order.id(),order.filled(op.quantity(),op.period()));
            }
            case CLOSE_ORDER->{
                if(!op.payload().isEmpty()){
                    if(!op.payload().getAllKeys().equals(Set.of("sandboxReason")))throw new IllegalArgumentException("Invalid sandbox order audit");
                    require(op.payload(),"sandboxReason",Tag.TAG_STRING);text(op.payload().getString("sandboxReason"),128);
                }
                var order=orders.get(op.reference());
                if(order==null||!order.active()||!order.owner().equals(op.target())||!op.key().equals("buy_order")||!op.detail().equals("Buy order refund")||op.quantity()!=0
                        ||op.period()<order.lastFill()||!op.actor().equals(order.owner())&&op.period()<order.expires()||op.amount()!=Math.multiplyExact(order.remaining(),order.unitPrice())
                        ||mail.containsKey(op.id()))throw new IllegalStateException("Buy order cannot be refunded");
                mail.put(op.id(),new Mail(op.id(),order.owner(),MailKind.COINS,op.amount(),"","Unspent buy order funding",false));orders.put(order.id(),order.closed());
            }
            case LIST_ITEM, LIST_TIMED_ITEM->{
                auctionShape(op,true);if(op.reference()!=null||op.amount()<1||listings.containsKey(op.id()))throw new IllegalArgumentException("Invalid listing record");
                var data=op.payload();require(data,"sellerName",Tag.TAG_STRING);require(data,"expires",Tag.TAG_LONG);require(data,"master",Tag.TAG_BYTE);require(data,"item",Tag.TAG_COMPOUND);
                boolean timed=op.kind()==Kind.LIST_TIMED_ITEM;
                if(timed&&data.getLong("expires")!=Math.addExact(op.period(),TIMED_DURATION))throw new IllegalArgumentException("Timed auction duration changed");
                var listing=new Listing(op.id(),op.actor(),data.getString("sellerName"),op.detail(),op.amount(),data.getBoolean("master"),op.period(),data.getLong("expires"),data.getCompound("item"),true,timed,null,0,0);
                listings.put(op.id(),listing);
            }
            case BUY_ITEM->{
                auctionShape(op,false);var listing=listings.get(op.reference());
                if(listing==null||!listing.active()||listing.timed()||!listing.seller().equals(op.target())||listing.seller().equals(op.actor())||op.amount()!=listing.price()||op.period()<listing.created()||op.period()>=listing.expires()
                        ||!op.detail().equals("Fixed-price purchase")||!op.payload().isEmpty()||mail.containsKey(op.id())||itemMail.containsKey(op.id()))throw new IllegalStateException("Listing unavailable or invalid purchase");
                long source=balances.getOrDefault(op.actor(),0L);if(source<op.amount())throw new IllegalStateException("Insufficient coins");
                long proceeds=op.amount()-tax(op.amount(),listing.master());balances.put(op.actor(),source-op.amount());listings.put(listing.id(),listing.closed());
                if(proceeds>0)mail.put(op.id(),new Mail(op.id(),listing.seller(),MailKind.COINS,proceeds,"","Auction proceeds",false));
                itemMail.put(op.id(),new ItemMail(op.id(),op.actor(),listing.item(),listing.title(),false));
            }
            case RETURN_ITEM->{
                auctionShape(op,false);var listing=listings.get(op.reference());
                if(listing==null||!listing.active()||listing.timed()&&listing.bidder()!=null||!listing.seller().equals(op.target())||op.amount()!=0||op.period()<listing.created()||!op.actor().equals(listing.seller())&&op.period()<listing.expires()
                        ||!op.detail().equals("Listing return")||!op.payload().isEmpty()||itemMail.containsKey(op.id()))throw new IllegalStateException("Listing cannot be returned");
                listings.put(listing.id(),listing.closed());itemMail.put(op.id(),new ItemMail(op.id(),listing.seller(),listing.item(),listing.title(),false));
            }
            case BID_ITEM->{
                auctionShape(op,false);var listing=listings.get(op.reference());
                if(listing==null||!listing.active()||!listing.timed()||!listing.seller().equals(op.target())||listing.seller().equals(op.actor())||op.amount()<minimumBid(listing)
                        ||op.period()<listing.created()||op.period()<listing.lastBid()||op.period()>=listing.expires()||!op.detail().equals("Funded timed bid")||!op.payload().isEmpty()||mail.containsKey(op.id()))throw new IllegalStateException("Timed bid is not eligible");
                boolean raising=op.actor().equals(listing.bidder());long debit=raising?Math.subtractExact(op.amount(),listing.bid()):op.amount();
                long available=balances.getOrDefault(op.actor(),0L);if(available<debit)throw new IllegalStateException("Insufficient coins to fund this bid");
                var next=listing.withBid(op.actor(),op.amount(),op.period());
                balances.put(op.actor(),available-debit);
                if(listing.bidder()!=null&&!raising)mail.put(op.id(),new Mail(op.id(),listing.bidder(),MailKind.COINS,listing.bid(),"","Outbid auction refund",false));
                listings.put(listing.id(),next);
            }
            case SETTLE_TIMED->{
                auctionShape(op,false);var listing=listings.get(op.reference());
                if(!op.payload().isEmpty()){
                    if(!op.payload().getAllKeys().equals(Set.of("sandboxReason")))throw new IllegalArgumentException("Invalid sandbox audit payload");
                    require(op.payload(),"sandboxReason",Tag.TAG_STRING);text(op.payload().getString("sandboxReason"),128);
                }
                if(listing==null||!listing.active()||!listing.timed()||listing.bidder()==null||!listing.seller().equals(op.target())||op.amount()!=listing.bid()||op.period()<listing.expires()
                        ||!op.detail().equals("Timed auction settlement")||mail.containsKey(op.id())||itemMail.containsKey(op.id()))throw new IllegalStateException("Timed auction cannot settle");
                long proceeds=op.amount()-tax(op.amount(),listing.master());
                if(proceeds>0)mail.put(op.id(),new Mail(op.id(),listing.seller(),MailKind.COINS,proceeds,"","Timed auction proceeds",false));
                itemMail.put(op.id(),new ItemMail(op.id(),listing.bidder(),listing.item(),listing.title(),false));listings.put(listing.id(),listing.closed());
            }
            case CLAIM_ITEM->{
                auctionShape(op,true);var entry=itemMail.get(op.reference());
                if(entry==null||entry.claimed()||!entry.owner().equals(op.actor())||op.amount()!=0||op.period()!=0||!op.detail().equals("Item mail claim")||!op.payload().isEmpty())throw new IllegalStateException("No unclaimed item delivery for this player");
                itemMail.put(entry.id(),new ItemMail(entry.id(),entry.owner(),entry.item(),entry.description(),true));
            }
            case ADMIN_SET->{if(!op.key().equals("admin")||op.reference()!=null||op.quantity()!=0||op.period()!=0)throw new IllegalArgumentException("Invalid administrative record");balances.put(op.target(),op.amount());}
            case QUEUE_COINS->{
                if(op.amount()<1||!op.actor().equals(op.target())||!op.key().equals("coins")||op.reference()!=null||op.quantity()!=0||op.period()!=0||mail.containsKey(op.id()))throw new IllegalArgumentException("Invalid queued coin record");
                mail.put(op.id(),new Mail(op.id(),op.target(),MailKind.COINS,op.amount(),"",op.detail(),false));
            }
            case CLAIM_COINS->{
                var entry=mail.get(op.reference());
                if(entry==null||entry.claimed()||entry.kind()!=MailKind.COINS||!entry.owner().equals(op.target())||!op.actor().equals(op.target())||entry.amount()!=op.amount()
                        ||!op.key().equals("coins")||op.quantity()!=0||op.period()!=0)throw new IllegalArgumentException("Invalid or duplicate coin claim");
                long next=Math.addExact(balance,entry.amount());money(next);balances.put(op.target(),next);
                mail.put(entry.id(),new Mail(entry.id(),entry.owner(),entry.kind(),entry.amount(),entry.building(),entry.description(),true));
            }
            case PURCHASE->{
                identifier(op.key());text(op.detail(),48);
                if(op.amount()<500||op.amount()>15_000||!op.actor().equals(op.target())||op.reference()!=null||op.period()!=0||op.quantity()!=0)throw new IllegalArgumentException("Invalid blueprint purchase record");
                if(purchases.containsKey(op.key()))throw new IllegalStateException("Blueprint sold out");
                if(balance<op.amount())throw new IllegalStateException("Insufficient coins");
                balances.put(op.target(),balance-op.amount());purchases.put(op.key(),new Purchase(op.id(),op.target(),op.detail(),op.key(),op.amount()));
                mail.put(op.id(),new Mail(op.id(),op.target(),MailKind.BLUEPRINT,0,op.key(),"Purchased blueprint",false));
            }
            case COIN_TRANSFER->{
                if(op.amount()<1||op.actor().equals(op.target())||!op.key().equals("transfer")||!op.detail().equals("In-person bank transfer")||op.reference()!=null||op.quantity()!=0||op.period()!=0)throw new IllegalArgumentException("Invalid in-person coin transfer");
                long source=balances.getOrDefault(op.actor(),0L);if(source<op.amount())throw new IllegalStateException("Insufficient coins");
                long next=Math.addExact(balance,op.amount());money(next);balances.put(op.actor(),source-op.amount());balances.put(op.target(),next);
            }
            case TRANSFER->{
                if(!op.actor().equals(op.target())||!op.key().equals("coins")||!op.detail().equals("Bank conversion")||op.reference()!=null||op.quantity()!=0||op.period()!=0)throw new IllegalArgumentException("Invalid bank conversion record");
                long next=Math.addExact(balance,op.amount());money(next);balances.put(op.target(),next);
            }
            case SALE->{
                identifier(op.key());if(op.amount()<1||op.quantity()<1||op.quantity()>2304||!op.actor().equals(op.target())||op.reference()!=null)throw new IllegalArgumentException("Invalid sale record");
                long next=Math.addExact(balance,op.amount());money(next);balances.put(op.target(),next);
            }
        }
        contracts.merchant.observe(op.kind(),op.actor(),op.target(),op.amount(),op.key(),op.reference(),professionRosters,orders);
    }
    private void persist(List<Operation> operations,PendingSale next)throws IOException{persist(operations,next,null);}
    private void persist(List<Operation> operations,PendingSale next,PendingTransfer transfer)throws IOException{
        persist(operations,next,transfer,null);
    }
    private void persist(List<Operation> operations,PendingSale next,PendingTransfer transfer,PendingItem item)throws IOException{
        var encoded=encode(operations,next,transfer,item);
        if(encodedBytes(encoded)>MAX_JOURNAL_BYTES)throw new IllegalStateException("Economy journal serialized size exceeded; no native mutation may begin");
        try{writer.write(file,encoded);}catch(IOException|RuntimeException e){quarantined=true;throw e;}
    }
    private CompoundTag encode(List<Operation> operations,PendingSale next){return encode(operations,next,null);}
    private CompoundTag encode(List<Operation> operations,PendingSale next,PendingTransfer transfer){return encode(operations,next,transfer,null);}
    private CompoundTag encode(List<Operation> operations,PendingSale next,PendingTransfer transfer,PendingItem item){
        var tag=new CompoundTag();tag.putInt("schema",archives.isEmpty()?1:2);tag.putUUID("identity",identity);tag.putLong("revision",operations.size());
        if(!archives.isEmpty())tag.put("archives",EconomyArchive.descriptors(archives));
        var list=new ListTag();for(int i=archivedCount;i<operations.size();i++)list.add(encodeOperation(operations.get(i)));tag.put("history",list);
        if(next!=null){var p=new CompoundTag();p.putLong("sequence",next.sequence());p.putUUID("operation",next.operation());p.putUUID("player",next.player());p.putString("resource",next.resource());p.putInt("quantity",next.quantity());p.putLong("proceeds",next.proceeds());p.putLong("period",next.period());p.putLong("previousReceipt",next.previousReceipt());tag.put("pending",p);}
        if(transfer!=null){var p=new CompoundTag();p.putLong("sequence",transfer.sequence());p.putUUID("operation",transfer.operation());p.putUUID("player",transfer.player());p.putLong("delta",transfer.delta());p.putLong("previousReceipt",transfer.previousReceipt());tag.put("transfer",p);}
        if(item!=null){var p=new CompoundTag();p.putLong("sequence",item.sequence());p.putUUID("operation",item.operation());p.putUUID("player",item.player());p.putString("action",item.action().name());p.put("payload",item.payload());p.putLong("previousReceipt",item.previousReceipt());tag.put("pendingItem",p);}
        return tag;
    }
    private static CompoundTag encodeOperation(Operation op){
        var t=new CompoundTag();t.putLong("sequence",op.sequence());t.putUUID("id",op.id());t.putString("kind",op.kind().name());t.putUUID("actor",op.actor());t.putUUID("target",op.target());t.putLong("amount",op.amount());t.putString("key",op.key());t.putString("detail",op.detail());if(op.reference()!=null)t.putUUID("reference",op.reference());t.putLong("period",op.period());t.putInt("quantity",op.quantity());if(!op.payload().isEmpty())t.put("payload",op.payload().copy());return t;
    }
    private static Operation decode(CompoundTag t){
        for(String k:List.of("sequence","amount","period"))require(t,k,Tag.TAG_LONG);
        for(String k:List.of("kind","key","detail"))require(t,k,Tag.TAG_STRING);require(t,"quantity",Tag.TAG_INT);
        for(String k:List.of("id","actor","target"))if(!t.hasUUID(k))throw new IllegalArgumentException("Invalid economy UUID");
        if(t.contains("reference")&&!t.hasUUID("reference"))throw new IllegalArgumentException("Invalid economy reference");
        if(t.contains("payload"))require(t,"payload",Tag.TAG_COMPOUND);
        return new Operation(t.getLong("sequence"),t.getUUID("id"),Kind.valueOf(t.getString("kind")),t.getUUID("actor"),t.getUUID("target"),t.getLong("amount"),t.getString("key"),t.getString("detail"),t.hasUUID("reference")?t.getUUID("reference"):null,t.getLong("period"),t.getInt("quantity"),t.getCompound("payload"));
    }
    private static void require(CompoundTag t,String key,int type){if(!t.contains(key,type))throw new IllegalArgumentException("Invalid economy field: "+key);}
    private static void signedMoney(long amount){if(amount==0||amount < -MAX_COINS||amount>MAX_COINS)throw new IllegalArgumentException("Invalid bank coin delta");}
    private static void money(long amount){if(amount<0||amount>MAX_COINS)throw new IllegalArgumentException("Coin amount outside supported bounds");}
    private static void identifier(String value){if(value==null||value.length()>128||!value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))throw new IllegalArgumentException("Invalid resource/building identifier");}
    private static void text(String value,int max){if(value==null||value.isBlank()||value.length()>max||value.codePoints().anyMatch(c->Character.isISOControl(c)||c=='§'))throw new IllegalArgumentException("Invalid economy text");}
}
