package com.mohymods.kingdomsmp.economy;

import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import net.minecraft.nbt.*;
import com.mohymods.kingdomsmp.profession.*;
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
    public enum Kind { ADMIN_SET, QUEUE_COINS, CLAIM_COINS, PURCHASE, SALE, TRANSFER, COIN_TRANSFER, LIST_ITEM, BUY_ITEM, RETURN_ITEM, CLAIM_ITEM, LIST_TIMED_ITEM, BID_ITEM, SETTLE_TIMED, CREATE_ORDER, FILL_ORDER, CLOSE_ORDER, SHOP_BUY, PROFESSION_CHANGE, COMPLETE_CONTRACT }
    public enum MailKind { COINS, BLUEPRINT }
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
    public enum ItemAction { LIST, CLAIM, TIMED_LIST, ORDER_FILL, CONTRACT_DELIVERY }
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
    private boolean quarantined;
    private EconomyStore(Path file,UUID identity,Writer writer,List<Operation> history,PendingSale pending,PendingTransfer transfer,PendingItem pendingItem){
        this.file=file;this.identity=identity;this.writer=writer;this.history=List.copyOf(history);
        Set<UUID> seen=new HashSet<>();
        for(int i=0;i<history.size();i++){
            var op=history.get(i);if(op.sequence()!=i+1L||!seen.add(op.id()))throw new IllegalArgumentException("Duplicate or unordered economy operation");
            apply(op,balances,purchases,mail,listings,itemMail,orders,shopSold,professionRosters,contracts);
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
                    ||op.kind()!=switch(pendingItem.action()){case LIST->Kind.LIST_ITEM;case CLAIM->Kind.CLAIM_ITEM;case TIMED_LIST->Kind.LIST_TIMED_ITEM;case ORDER_FILL->Kind.FILL_ORDER;case CONTRACT_DELIVERY->Kind.COMPLETE_CONTRACT;})throw new IllegalArgumentException("Invalid pending item operation");
            var nextMail=new HashMap<>(mail);var nextListings=new HashMap<>(listings);var nextItems=new HashMap<>(itemMail);var nextOrders=new HashMap<>(orders);
            apply(op,new HashMap<>(balances),new HashMap<>(purchases),nextMail,nextListings,nextItems,nextOrders,new HashMap<>(shopSold),new HashMap<>(professionRosters),contracts.copy());var nextHistory=new ArrayList<>(history);nextHistory.add(op);budget(nextHistory,nextListings,nextItems,nextMail,nextOrders,false);
        }
        budget(history,listings,itemMail,mail,orders,false);
    }
    public static EconomyStore open(Path directory)throws IOException{return open(directory,AtomicNbt::write);}
    static EconomyStore open(Path directory,Writer writer)throws IOException{
        Path file=directory.resolve("kingdomsmp_economy.dat"),seal=directory.resolve("kingdomsmp_economy.created");
        if(Files.exists(file,LinkOption.NOFOLLOW_LINKS)!=Files.exists(seal,LinkOption.NOFOLLOW_LINKS))throw new IOException("Economy journal/identity mismatch; restore matching backup");
        if(!Files.exists(file,LinkOption.NOFOLLOW_LINKS)){
            var s=new EconomyStore(file,UUID.randomUUID(),writer,List.of(),null,null,null);var mark=new CompoundTag();mark.putUUID("identity",s.identity);
            AtomicNbt.write(seal,mark);writer.write(file,s.encode(List.of(),null));return s;
        }
        try{
            var tag=AtomicNbt.read(file);var mark=AtomicNbt.read(seal);
            require(tag,"schema",Tag.TAG_INT);require(tag,"revision",Tag.TAG_LONG);require(tag,"history",Tag.TAG_LIST);
            if(tag.getInt("schema")!=1||!tag.hasUUID("identity")||!mark.hasUUID("identity")||!tag.getUUID("identity").equals(mark.getUUID("identity")))throw new IOException("Invalid economy identity/schema");
            var list=tag.getList("history",Tag.TAG_COMPOUND);
            if(list.size()>MAX_OPERATIONS||list.size()!=((ListTag)tag.get("history")).size()||tag.getLong("revision")!=list.size())throw new IOException("Invalid economy history bounds");
            List<Operation> operations=new ArrayList<>();for(int i=0;i<list.size();i++)operations.add(decode(list.getCompound(i)));
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
            return new EconomyStore(file,tag.getUUID("identity"),writer,operations,pending,transfer,pendingItem);
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
    public synchronized long contractPoolSpent(String cycle,long period){return contracts.spent(cycle,period);}
    /** Root pins a real approved repeatable delivery's item/terms and removes actual native inventory. */
    public synchronized PendingItem prepareContract(UUID operation,UUID player,String profession,String cycle,long period,int quantity,long reward,long pool,CompoundTag item)throws IOException{
        var data=new CompoundTag();data.putString("profession",profession);data.putString("cycle",cycle);data.putInt("required",quantity);data.putLong("pool",pool);data.put("item",checkedItem(item));
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
    public synchronized boolean shopDelivery(UUID mail){var op=find(mail);return op!=null&&op.kind()==Kind.SHOP_BUY;}
    public synchronized long shopCoinsSpent(){long sum=0;for(var op:history)if(op.kind()==Kind.SHOP_BUY)sum=Math.addExact(sum,op.amount());return sum;}
    /** Debit, finite stock consumption and recoverable delivery commit together. Root validates real counter/catalogue. */
    public synchronized ItemMail shopBuy(UUID operation,UUID buyer,String resource,String title,CompoundTag item,int quantity,long period,int stock,long minimum,long maximum)throws IOException{
        identifier(resource);var data=new CompoundTag();data.put("item",checkedItem(item));data.putInt("stock",stock);data.putLong("minimum",minimum);data.putLong("maximum",maximum);
        var old=find(operation);long amount=old==null?MarketPricing.total(minimum,maximum,shopSold(period,resource),stock,quantity,true):old.amount();
        var op=new Operation(history.size()+1L,operation,Kind.SHOP_BUY,buyer,buyer,amount,resource,title,null,period,quantity,data);
        if(!replayed(op))append(op);return itemMail.get(operation);
    }
    /** Permanently removed from circulation by explicit D-029 approval; retained for audit only. */
    public synchronized long taxRemoved(){long total=0;for(var op:history){
        if(op.kind()==Kind.BUY_ITEM||op.kind()==Kind.SETTLE_TIMED)total=Math.addExact(total,tax(op.amount(),listings.get(op.reference()).master()));
        else if(op.kind()==Kind.FILL_ORDER)total=Math.addExact(total,tax(op.amount(),orders.get(op.reference()).master()));
    }return total;}
    public synchronized long expectedReceipt(UUID player){
        for(int i=history.size()-1;i>=0;i--){var op=history.get(i);if((op.kind()==Kind.SALE||op.kind()==Kind.TRANSFER||op.kind()==Kind.LIST_ITEM||op.kind()==Kind.CLAIM_ITEM||op.kind()==Kind.LIST_TIMED_ITEM||op.kind()==Kind.FILL_ORDER||op.kind()==Kind.COMPLETE_CONTRACT)&&op.target().equals(player))return op.sequence();}return 0;
    }
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
        long spent=0,quantity=0,personal=0;
        for(var op:history)if(op.kind()==Kind.SALE&&op.period()==period){spent=Math.addExact(spent,op.amount());if(op.key().equals(resource))quantity=Math.addExact(quantity,op.quantity());if(op.target().equals(player))personal=Math.addExact(personal,op.amount());}
        return new Usage(spent,quantity,personal);
    }
    /** Budget/allowance are coins, resourceQuota is units; period is server-derived UTC day. */
    public synchronized PendingSale prepareSale(UUID operation,UUID player,String resource,int quantity,long proceeds,long period,long sharedBudget,long resourceQuota,long playerAllowance)throws IOException{
        healthy();if(pending!=null||transfer!=null||pendingItem!=null)throw new IllegalStateException("Economy has an inventory operation pending recovery");
        if(find(operation)!=null)throw new IllegalArgumentException("Inventory sale operation already committed");
        long latest=history.stream().filter(o->o.kind()==Kind.SALE).mapToLong(Operation::period).max().orElse(0);
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
        apply(op,new HashMap<>(balances),new HashMap<>(purchases),nextMail,nextListings,nextItems,nextOrders,new HashMap<>(shopSold),new HashMap<>(professionRosters),contracts.copy());var nextHistory=new ArrayList<>(history);nextHistory.add(op);
        budget(nextHistory,nextListings,nextItems,nextMail,nextOrders,action==ItemAction.LIST||action==ItemAction.TIMED_LIST||action==ItemAction.ORDER_FILL||action==ItemAction.CONTRACT_DELIVERY);
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
    private Operation find(UUID id){Objects.requireNonNull(id);return history.stream().filter(o->o.id().equals(id)).findFirst().orElse(null);}
    private boolean replayed(Operation next){
        healthy();var old=find(next.id());if(old==null)return false;
        if(old.kind()!=next.kind()||!old.actor().equals(next.actor())||!old.target().equals(next.target())||old.amount()!=next.amount()||!old.key().equals(next.key())
                ||!old.detail().equals(next.detail())||!Objects.equals(old.reference(),next.reference())||old.period()!=next.period()||old.quantity()!=next.quantity()||!old.payload().equals(next.payload()))throw new IllegalArgumentException("Operation ID reused for different transaction");
        return true;
    }
    private void healthy(){if(quarantined)throw new IllegalStateException("Economy storage quarantined; restart and reconcile matching checkpoints");}
    private static long reservedOperations(Map<UUID,Listing> listings,Map<UUID,ItemMail> items,Map<UUID,Mail> mail,Map<UUID,BuyOrder> orders){
        return 2L*orders.values().stream().filter(BuyOrder::active).count()+3L*listings.values().stream().filter(Listing::active).count()+items.values().stream().filter(m->!m.claimed()).count()+mail.values().stream().filter(m->m.kind()==MailKind.COINS&&!m.claimed()).count();
    }
    private void budget(List<Operation> operations,Map<UUID,Listing> listings,Map<UUID,ItemMail> items,Map<UUID,Mail> mail,Map<UUID,BuyOrder> orders,boolean listingAdmission){
        long reserved=reservedOperations(listings,items,mail,orders);int bytes=encodedBytes(encode(operations,null,null,null));
        if(operations.size()+reserved>MAX_OPERATIONS||bytes+reserved*1024>MAX_JOURNAL_BYTES||listingAdmission&&bytes>MAX_LISTING_JOURNAL_BYTES)
            throw new IllegalStateException("Economy archive required; existing escrow recovery capacity is reserved");
    }
    private void capacity(){
        long reserved=reservedOperations(listings,itemMail,mail,orders);
        if(history.size()+1L+reserved>MAX_OPERATIONS||encodedBytes(encode(history,null,null,null))+1024L+reserved*1024>MAX_JOURNAL_BYTES)
            throw new IllegalStateException("Economy archive required; existing escrow recovery capacity is reserved");
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
        var nextStock=new HashMap<>(shopSold);var nextRosters=new HashMap<>(professionRosters);var nextContracts=contracts.copy();apply(op,nextBalances,nextPurchases,nextMail,nextListings,nextItemMail,nextOrders,nextStock,nextRosters,nextContracts);var next=new ArrayList<>(history);next.add(op);
        budget(next,nextListings,nextItemMail,nextMail,nextOrders,op.kind()==Kind.LIST_ITEM||op.kind()==Kind.LIST_TIMED_ITEM||op.kind()==Kind.CREATE_ORDER||op.kind()==Kind.FILL_ORDER||op.kind()==Kind.SHOP_BUY||op.kind()==Kind.COMPLETE_CONTRACT);persist(next,null);
        balances=nextBalances;purchases=nextPurchases;mail=nextMail;history=List.copyOf(next);pending=null;transfer=null;listings=nextListings;itemMail=nextItemMail;orders=nextOrders;shopSold=nextStock;professionRosters=nextRosters;contracts=nextContracts;pendingItem=null;
    }
    private static void apply(Operation op,Map<UUID,Long> balances,Map<String,Purchase> purchases,Map<UUID,Mail> mail,Map<UUID,Listing> listings,Map<UUID,ItemMail> itemMail,Map<UUID,BuyOrder> orders,Map<ShopDay,Long> shopSold,Map<UUID,ProfessionRoster> professionRosters,ContractState contracts){
        if(op.kind().ordinal()<Kind.LIST_ITEM.ordinal()&&!op.payload().isEmpty())throw new IllegalArgumentException("Unexpected economy payload");
        long balance=balances.getOrDefault(op.target(),0L);
        switch(op.kind()){
            case COMPLETE_CONTRACT->{
                var data=op.payload();if(!data.getAllKeys().equals(Set.of("profession","cycle","required","pool","item")))throw new IllegalArgumentException("Invalid contract payload");
                require(data,"profession",Tag.TAG_STRING);require(data,"cycle",Tag.TAG_STRING);require(data,"required",Tag.TAG_INT);require(data,"pool",Tag.TAG_LONG);require(data,"item",Tag.TAG_COMPOUND);
                var role=Profession.byId(data.getString("profession"));String cycle=data.getString("cycle");long pool=data.getLong("pool");var item=checkedItem(data.getCompound("item"));
                if(!Set.of("daily","weekly").contains(cycle)||!op.actor().equals(op.target())||op.reference()!=null||op.quantity()<1||op.quantity()>2304||op.quantity()!=data.getInt("required")
                        ||op.amount()<1||op.amount()>1_000_000||pool<op.amount()||pool>1_000_000_000L||!op.key().equals("kingdom_smp:contract/"+role.id()+"/"+cycle)||!op.detail().equals("Profession delivery contract")
                        ||item.getInt("count")!=1||mail.containsKey(op.id()))throw new IllegalArgumentException("Invalid contract terms");
                if(!professionRosters.getOrDefault(op.target(),ProfessionRoster.empty()).isActive(role))throw new IllegalStateException("Contract profession is inactive");
                var slot=new ContractState.Slot(op.target(),role.id(),cycle,op.period());var key=new ContractState.Pool(cycle,op.period());long spent=contracts.spent(cycle,op.period());
                if(contracts.completed.contains(slot)||op.amount()>pool-spent)throw new IllegalStateException("Contract already completed or shared reward pool exhausted");
                contracts.completed.add(slot);contracts.spent.put(key,Math.addExact(spent,op.amount()));
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
        var tag=new CompoundTag();tag.putInt("schema",1);tag.putUUID("identity",identity);tag.putLong("revision",operations.size());
        var list=new ListTag();for(var op:operations)list.add(encodeOperation(op));tag.put("history",list);
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
