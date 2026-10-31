package com.mohymods.kingdomsmp.economy;

import com.mohymods.kingdomsmp.network.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import com.mojang.logging.LogUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import java.io.IOException;
import java.util.*;

/** Fixed-price item escrow at the real Bank. No client item data or wallet authority. */
public final class AuctionService {
    public record Quote(UUID nonce,long revision,int tab,String selected,long price,int slot,
                        ItemStack item,long issued,List<ItemStack> inventory,String text) {
        public Quote { item=item.copy();inventory=inventory.stream().map(ItemStack::copy).toList(); }
        @Override public ItemStack item(){return item.copy();}
    }
    private static final Map<ServerPlayer,Quote> QUOTES=new WeakHashMap<>();
    private static final long DURATION=86_400_000L;
    private AuctionService(){ }
    public static void close(ServerPlayer p){QUOTES.remove(p);}
    public static Optional<Quote> quote(ServerPlayer p){
        var q=QUOTES.get(p);
        if(q!=null&&(System.nanoTime()-q.issued()>30_000_000_000L||CommerceService.store(p.server).revision()!=q.revision())){close(p);q=null;}
        return Optional.ofNullable(q);
    }
    /** Supported initial scope: vanilla stacks, with simple components retained exactly. */
    public static void validateItem(ItemStack item){
        if(item.isEmpty()||item.getCount()>Math.min(64,item.getMaxStackSize())
                ||item.getItem() instanceof BlockItem block&&block.getBlock() instanceof ShulkerBoxBlock
                ||!BuiltInRegistries.ITEM.getKey(item.getItem()).getNamespace().equals("minecraft")
                ||item.has(DataComponents.CONTAINER)||item.has(DataComponents.BUNDLE_CONTENTS)
                ||item.has(DataComponents.BLOCK_ENTITY_DATA)||item.has(DataComponents.ENTITY_DATA)
                ||item.has(DataComponents.CONTAINER_LOOT)||item.has(DataComponents.CUSTOM_DATA)
                ||item.has(DataComponents.CHARGED_PROJECTILES))
            throw new IllegalStateException("Use a vanilla item stack without stored items, entity data or custom ownership data. Kingdom plans and coins cannot be auctioned.");
    }
    private static ItemStack decode(ServerPlayer p,CompoundTag tag){
        var item=ItemStack.parse(p.registryAccess(),tag).orElseThrow(()->new IllegalStateException("Escrow item requires registry recovery."));
        validateItem(item);return item;
    }
    private static EconomyStore.ItemMail mail(ServerPlayer p,String selected){
        UUID id;try{id=UUID.fromString(selected);}catch(IllegalArgumentException e){throw new IllegalArgumentException("Choose a valid item delivery.");}
        return CommerceService.store(p.server).itemMailbox(p.getUUID()).stream().filter(m->m.id().equals(id)&&!m.claimed()).findFirst().orElseThrow(()->new IllegalStateException("No such unclaimed item in your mailbox."));
    }
    public static boolean itemMail(ServerPlayer p,String selected){
        return CommerceService.store(p.server).itemMailbox(p.getUUID()).stream().anyMatch(m->m.id().toString().equals(selected));
    }
    /** Expiry returns escrow by canonical operation, never by adding world items. */
    public static void expire(ServerPlayer p)throws IOException{
        var s=CommerceService.store(p.server);if(s.pendingItem().isPresent()||s.pendingSale().isPresent()||s.pendingTransfer().isPresent()||s.quarantined())return;
        long now=System.currentTimeMillis();
        for(var listing:s.listings())if(listing.active()&&listing.expires()<=now)s.returnListing(UUID.randomUUID(),p.getUUID(),listing.id(),now);
    }
    public static List<CommerceView.Entry> entries(ServerPlayer p){
        var s=CommerceService.store(p.server);List<CommerceView.Entry> out=new ArrayList<>();boolean usable=true;
        try{BankService.requireCounter(p);}catch(IllegalStateException e){usable=false;}
        out.add(new CommerceView.Entry("sell","List held item","Hold one supported vanilla stack. Choose quantity and TOTAL price. Escrow removes only the reviewed items. Listings expire after 24 hours. Normal sales tax is 5%, rounded up.",0,usable?"LIST":"LOCKED"));
        for(var l:s.listings().stream().filter(EconomyStore.Listing::active).sorted(Comparator.comparingLong(EconomyStore.Listing::created).thenComparing(EconomyStore.Listing::id)).limit(30).toList()){
            boolean mine=l.seller().equals(p.getUUID());long minutes=Math.max(0,(l.expires()-System.currentTimeMillis()+59_999)/60_000);
            out.add(new CommerceView.Entry((mine?"return:":"buy:")+l.id(),l.title(),"Seller: "+l.sellerName()+". Whole-stack price; "+minutes+" minutes remaining. "+(mine?"Cancel to return escrow to your private mailbox.":"Purchase delivers exact escrow to your private mailbox."),l.price(),usable?(mine?"RETURN":"BUY"):"LOCKED"));
        }
        return List.copyOf(out);
    }
    public static List<CommerceView.Entry> mailEntries(ServerPlayer p){
        List<CommerceView.Entry> out=new ArrayList<>();
        for(var m:CommerceService.store(p.server).itemMailbox(p.getUUID()).stream().filter(m->!m.claimed()).toList()){
            var item=decode(p,m.item());String title=item.getCount()+" × "+item.getHoverName().getString();
            out.add(new CommerceView.Entry(m.id().toString(),bounded(title,64),bounded(m.description()+". Exact components retained. Claim at the Royal Bank with enough inventory space; nothing is dropped.",256),0,"ITEMS"));
        }
        return out;
    }
    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> adminCommands(){
        return net.minecraft.commands.Commands.literal("auctions")
                .then(net.minecraft.commands.Commands.literal("inspect").executes(c->{
                    var s=CommerceService.store(c.getSource().getServer());
                    c.getSource().sendSuccess(()->Component.literal("Auction journal "+s.id()+" revision "+s.revision()+" | active listings "+s.listings().stream().filter(EconomyStore.Listing::active).count()+" | recorded listings "+s.listings().size()+" | tax removed "+s.taxRemoved()+" | pending item receipt "+s.pendingItem().isPresent()+" | quarantined "+s.quarantined()),false);
                    for(var listing:s.listings().stream().filter(EconomyStore.Listing::active).limit(30).toList())c.getSource().sendSuccess(()->Component.literal(listing.id()+" | "+listing.title()+" | seller "+listing.sellerName()+" | total "+listing.price()+" | expires UTC epoch "+listing.expires()),false);
                    return 1;
                }));
    }
    public static Quote review(ServerPlayer p,int tab,String selected,int requested,long price)throws IOException{
        BankService.requireCounter(p);var s=CommerceService.store(p.server);CommerceService.verifyReceipt(p,s);expire(p);
        if(s.pendingItem().isPresent()||s.pendingSale().isPresent()||s.pendingTransfer().isPresent()||s.quarantined())throw new IllegalStateException("Auction inventory reconciliation is pending.");
        ItemStack item=ItemStack.EMPTY;int slot=p.getInventory().selected;String text;
        if(tab==2){
            item=decode(p,mail(p,selected).item());if(capacity(p,item)<item.getCount())throw new IllegalStateException("Make enough main-inventory space for the entire item delivery.");
            text="Claim exactly "+item.getCount()+" × "+item.getHoverName().getString()+" with its saved components. Native inventory receipt and mailbox claim are reconciled together. No items are dropped.";
        }else if(selected.equals("sell")){
            if(price<1||price>1_000_000_000L)throw new IllegalArgumentException("Enter a total price between 1 and 1,000,000,000 coins.");
            if(requested<1||requested>2304)throw new IllegalArgumentException("Invalid listing quantity.");
            if(s.listings().stream().filter(EconomyStore.Listing::active).count()>=30)throw new IllegalStateException("The market holds 30 active listings. Wait for a sale/expiry or cancel your own listing.");
            item=p.getInventory().getItem(slot).copy();validateItem(item);item.setCount(Math.min(item.getCount(),Math.min(64,requested)));
            long tax=(price+19)/20;
            text="Escrow exactly "+item.getCount()+" × "+item.getHoverName().getString()+" from your held slot. TOTAL asking price: "+price+" coins. After a sale, "+tax+" tax leaves circulation and "+(price-tax)+" coins go to your private mailbox. Expires after 24 hours; cancellation returns the items. No payment is charged for listing.";
        }else{
            UUID id=listingId(selected);var l=s.listing(id).orElseThrow(()->new IllegalStateException("Listing no longer exists."));
            if(!l.active()||l.expires()<=System.currentTimeMillis())throw new IllegalStateException("Listing sold, cancelled or expired. Refresh the market.");
            price=l.price();
            if(selected.startsWith("buy:")){
                if(l.seller().equals(p.getUUID()))throw new IllegalStateException("You cannot buy your own listing.");
                if(s.balance(p.getUUID())<price)throw new IllegalStateException("Insufficient coins for this whole-stack purchase.");
                text="Buy "+l.title()+" from "+l.sellerName()+" for exactly "+price+" coins. Payment, sold status, seller proceeds and your item-mail entitlement commit together. Claim the item separately at the Bank.";
            }else if(selected.startsWith("return:")&&l.seller().equals(p.getUUID()))text="Cancel your listing of "+l.title()+". Its escrow returns to your private item mailbox once. No coins are charged.";
            else throw new IllegalStateException("Only the seller can cancel an active listing.");
        }
        var q=new Quote(UUID.randomUUID(),s.revision(),tab,selected,price,slot,item,System.nanoTime(),inventory(p),bounded(text,1024));QUOTES.put(p,q);return q;
    }
    public static String confirm(ServerPlayer p,UUID nonce)throws IOException{
        BankService.requireCounter(p);var q=QUOTES.remove(p);var s=CommerceService.store(p.server);CommerceService.verifyReceipt(p,s);
        if(q==null||!q.nonce().equals(nonce)||System.nanoTime()-q.issued()>30_000_000_000L||q.revision()!=s.revision())throw new IllegalStateException("Auction review expired or market state changed. Review again.");
        if(q.tab()!=2&&!q.selected().equals("sell")){
            if(q.selected().startsWith("buy:")){s.buyListing(q.nonce(),p.getUUID(),listingId(q.selected()),System.currentTimeMillis());return "Purchase saved. Claim your exact items through Mailbox at the Bank.";}
            s.returnListing(q.nonce(),p.getUUID(),listingId(q.selected()),System.currentTimeMillis());return "Listing cancelled. Escrow is in your private item mailbox.";
        }
        if(!sameInventory(p,q.inventory()))throw new IllegalStateException("Inventory changed. Review again before moving escrow items.");
        var old=p.getPersistentData().copy();boolean checkpointStarted=false;
        try{
            CommerceService.checkpoint(p);EconomyStore.PendingItem pending;
            if(q.tab()==2){
                var item=decode(p,mail(p,q.selected()).item());if(capacity(p,item)<item.getCount())throw new IllegalStateException("Make room for the entire delivery.");
                pending=s.prepareItemClaim(q.nonce(),p.getUUID(),UUID.fromString(q.selected()));insert(p,item);
            }else{
                var item=q.item();validateItem(item);long now=System.currentTimeMillis();
                pending=s.prepareListing(q.nonce(),p.getUUID(),p.getGameProfile().getName(),bounded(item.getCount()+" × "+item.getHoverName().getString(),64),q.price(),false,now,Math.addExact(now,DURATION),(CompoundTag)item.save(p.registryAccess()));
                p.getInventory().getItem(q.slot()).shrink(item.getCount());
            }
            CommerceService.putReceipt(p,new EconomyStore.Receipt(s.id(),pending.sequence()));checkpointStarted=true;CommerceService.checkpoint(p);
            if(!s.recoverItem(CommerceService.receipt(AtomicNbt.read(CommerceService.playerFile(p.server,p.getUUID())))))throw new IOException("Native auction item receipt was not committed.");
            p.getInventory().setChanged();p.inventoryMenu.sendAllDataToRemote();
            LogUtils.getLogger().info("ROYAL_AUCTION_ITEM operation={} player={} action={} item={}",q.nonce(),p.getUUID(),q.tab()==2?"CLAIM":"LIST",q.item());
            return q.tab()==2?"Exact item delivery saved. This mail cannot be claimed again.":"Listing saved. Items are held in durable escrow until sold, cancelled or expired.";
        }catch(IOException|RuntimeException e){
            if(!checkpointStarted){for(int i=0;i<36;i++)p.getInventory().setItem(i,q.inventory().get(i).copy());for(var k:new HashSet<>(p.getPersistentData().getAllKeys()))p.getPersistentData().remove(k);p.getPersistentData().merge(old);}
            // A rejected projection has neither written a pending receipt nor touched escrow.
            // Capacity/input refusals can be shown normally; uncertain IO still quarantines.
            if(!checkpointStarted&&s.pendingItem().isEmpty()&&!s.quarantined()
                    &&(e instanceof IllegalArgumentException||e instanceof IllegalStateException))throw e;
            s.quarantine();p.connection.disconnect(Component.literal("Auction inventory operation interrupted. Reconnect only after matching checkpoint recovery."));throw e;
        }
    }
    private static UUID listingId(String selected){
        if(!selected.startsWith("buy:")&&!selected.startsWith("return:"))throw new IllegalArgumentException("Select an actual listing.");
        try{return UUID.fromString(selected.substring(selected.indexOf(':')+1));}catch(IllegalArgumentException e){throw new IllegalArgumentException("Invalid listing identity.");}
    }
    public static int capacity(ServerPlayer p,ItemStack item){int count=0;for(int i=0;i<36;i++){var current=p.getInventory().getItem(i);if(current.isEmpty())count+=Math.min(64,item.getMaxStackSize());else if(ItemStack.isSameItemSameComponents(current,item))count+=Math.max(0,current.getMaxStackSize()-current.getCount());}return count;}
    private static void insert(ServerPlayer p,ItemStack value){
        var item=value.copy();for(int pass=0;pass<2;pass++)for(int i=0;i<36&&!item.isEmpty();i++){
            var current=p.getInventory().getItem(i);if(pass==0&&!current.isEmpty()&&ItemStack.isSameItemSameComponents(current,item)){int n=Math.min(item.getCount(),Math.max(0,current.getMaxStackSize()-current.getCount()));current.grow(n);item.shrink(n);}
            else if(pass==1&&current.isEmpty()){int n=Math.min(item.getCount(),item.getMaxStackSize());p.getInventory().setItem(i,item.copyWithCount(n));item.shrink(n);}
        }if(!item.isEmpty())throw new IllegalStateException("Inventory capacity changed during delivery.");
    }
    private static List<ItemStack> inventory(ServerPlayer p){List<ItemStack> out=new ArrayList<>();for(int i=0;i<36;i++)out.add(p.getInventory().getItem(i).copy());return out;}
    private static boolean sameInventory(ServerPlayer p,List<ItemStack> before){for(int i=0;i<36;i++)if(!ItemStack.matches(p.getInventory().getItem(i),before.get(i)))return false;return true;}
    private static String bounded(String text,int length){text=text.replaceAll("[\\p{Cntrl}§]","");return text.length()>length?text.substring(0,length):text;}
}
