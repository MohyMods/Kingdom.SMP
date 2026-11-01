package com.mohymods.kingdomsmp.economy;

import com.google.gson.*;
import com.mohymods.kingdomsmp.network.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

/** Funded demand at the actual Bank. A template never supplies client item or balance authority. */
public final class BuyOrderService {
    public record Quote(UUID nonce,long revision,String selected,int quantity,long unitPrice,long issued,ItemStack item,List<ItemStack> inventory,String text){
        public Quote{item=item.copy();inventory=inventory.stream().map(ItemStack::copy).toList();}
        @Override public ItemStack item(){return item.copy();}
    }
    private static final Map<ServerPlayer,Quote> QUOTES=new WeakHashMap<>();
    private static final Map<MinecraftServer,Long> DURATION=new WeakHashMap<>();
    private BuyOrderService(){ }
    public static void initialize(MinecraftServer server){DURATION.remove(server);QUOTES.keySet().removeIf(p->p.server==server);}
    public static void close(ServerPlayer p){QUOTES.remove(p);}
    public static Optional<Quote> quote(ServerPlayer p){var q=QUOTES.get(p);if(q!=null&&(System.nanoTime()-q.issued()>30_000_000_000L||CommerceService.store(p.server).revision()!=q.revision())){close(p);q=null;}return Optional.ofNullable(q);}
    private static long duration(MinecraftServer server)throws IOException{
        var saved=DURATION.get(server);if(saved!=null)return saved;
        var path=server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-buy-orders.json");
        if(!Files.exists(path)){Files.createDirectories(path.getParent());Files.writeString(path,"{\n  \"schema\": 1,\n  \"durationHours\": 168\n}\n",StandardOpenOption.CREATE_NEW);}
        try{var data=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if(!data.keySet().equals(Set.of("schema","durationHours"))||data.get("schema").getAsBigDecimal().intValueExact()!=1)throw new IllegalArgumentException("Invalid schema");
            int hours=data.get("durationHours").getAsBigDecimal().intValueExact();if(hours<1||hours>720)throw new IllegalArgumentException("Duration must be 1–720 hours");
            long value=Math.multiplyExact(hours,3_600_000L);DURATION.put(server,value);return value;
        }catch(RuntimeException e){throw new IOException("Buy-order configuration needs valid schema 1 and durationHours 1–720",e);}
    }
    public static ItemStack template(ItemStack source){
        if(source.isEmpty())throw new IllegalStateException("Hold one plain vanilla sample of the item you want to buy. The sample is not consumed.");
        var item=source.copyWithCount(1);AuctionService.validateItem(item);
        if(!ItemStack.isSameItemSameComponents(item,new ItemStack(item.getItem())))throw new IllegalStateException("Buy orders match plain default items only. Named, damaged, enchanted or customized samples are not accepted.");
        return item;
    }
    private static ItemStack decode(ServerPlayer p,CompoundTag value){return template(ItemStack.parse(p.registryAccess(),value).orElseThrow(()->new IllegalStateException("Order template requires registry recovery.")));}
    public static List<CommerceView.Entry> entries(ServerPlayer p){
        var s=CommerceService.store(p.server);boolean usable=true;try{BankService.requireCounter(p);}catch(IllegalStateException e){usable=false;}
        List<CommerceView.Entry> rows=new ArrayList<>();
        rows.add(new CommerceView.Entry("create","Post a buy order","Hold a plain vanilla sample. Set total units and price PER ITEM. The whole payment is reserved immediately; your sample stays in your inventory. Ten active orders per player.",0,usable?"ORDER_CREATE":"LOCKED"));
        for(var o:s.orders().stream().filter(EconomyStore.BuyOrder::active).toList()){
            boolean mine=o.owner().equals(p.getUUID());long hours=Math.max(0,(o.expires()-System.currentTimeMillis()+3_599_999)/3_600_000);
            rows.add(new CommerceView.Entry((mine?"cancel:":"fill:")+o.id(),o.title(),bounded("Buyer: "+o.buyerName()+". Needs "+o.remaining()+" / "+o.quantity()+" units; "+o.unitPrice()+" coins PER ITEM. Held funding "+o.fundsHeld()+". "+hours+" hours remaining. "+(mine?"Cancel to refund only unspent funding through coin mail.":"Fill up to one stack of exact plain items per review. Tax is included in each fill."),256),o.unitPrice(),usable?(mine?"ORDER_CANCEL":"ORDER_FILL"):"LOCKED"));
        }
        return List.copyOf(rows);
    }
    public static Quote review(ServerPlayer p,String selected,int requested,long unitPrice)throws IOException{
        BankService.requireCounter(p);var s=CommerceService.store(p.server);CommerceService.verifyReceipt(p,s);AuctionService.maintain(p.server,System.currentTimeMillis());
        if(s.pendingItem().isPresent()||s.pendingSale().isPresent()||s.pendingTransfer().isPresent()||s.quarantined())throw new IllegalStateException("Order trading paused for inventory reconciliation.");
        int quantity;ItemStack item;String text;
        if(selected.equals("create")){
            if(requested<1||requested>2304||unitPrice<1||unitPrice>1_000_000_000L)throw new IllegalArgumentException("Choose 1–2304 units and a unit price of 1–1,000,000,000 coins.");
            if(s.orders().stream().filter(EconomyStore.BuyOrder::active).count()>=100||s.orders().stream().filter(o->o.active()&&o.owner().equals(p.getUUID())).count()>=10)throw new IllegalStateException("Order slots full: ten active per player and one hundred for this kingdom.");
            item=template(p.getMainHandItem());quantity=requested;long gross=Math.multiplyExact(quantity,unitPrice);
            if(s.balance(p.getUUID())<gross)throw new IllegalStateException("Insufficient coins to fund every requested unit.");
            text="Order exactly "+quantity+" plain "+item.getHoverName().getString()+" at "+unitPrice+" coins PER ITEM. Reserve "+gross+" coins now; your sample is not consumed. Matching sellers may fill partially. Each accepted fill goes to your private item mail. Unspent funding can be cancelled/refunded; already filled units cannot. Expiry: "+duration(p.server)/3_600_000L+" hours.";
        }else{
            var order=order(p,selected);item=decode(p,order.item());unitPrice=order.unitPrice();
            if(!order.active())throw new IllegalStateException("Order is filled or closed. Refresh.");
            if(selected.startsWith("cancel:")){
                if(!order.owner().equals(p.getUUID()))throw new IllegalStateException("Only the order owner can cancel it.");
                quantity=order.remaining();text="Cancel the unfilled portion of "+order.title()+". Return exactly "+order.fundsHeld()+" coins to your private coin mail for "+quantity+" remaining units. Earlier item deliveries/proceeds/tax remain unchanged. No refund tax is charged.";
            }else if(selected.startsWith("fill:")){
                if(order.owner().equals(p.getUUID()))throw new IllegalStateException("You cannot fill your own order.");
                if(order.expires()<=System.currentTimeMillis())throw new IllegalStateException("Order expired. Refresh for its refund.");
                if(requested<1||requested>2304)throw new IllegalArgumentException("Invalid fill quantity.");
                quantity=Math.min(Math.min(64,item.getMaxStackSize()),Math.min(requested,Math.min(order.remaining(),eligible(p,item))));
                if(quantity<1)throw new IllegalStateException("No exact plain matching items in your main inventory.");
                long gross=Math.multiplyExact(quantity,unitPrice),tax=EconomyStore.tax(gross,order.master());
                text="Permanently remove exactly "+quantity+" plain "+item.getHoverName().getString()+" from your main inventory to fill this order. Gross "+gross+" coins; tax "+tax+" permanently leaves circulation; seller proceeds "+(gross-tax)+" go to your private coin mail. The buyer receives exactly these items in mail. This partial fill consumes only its already funded payment.";
            }else throw new IllegalArgumentException("Unknown buy-order action.");
        }
        var q=new Quote(UUID.randomUUID(),s.revision(),selected,quantity,unitPrice,System.nanoTime(),item,inventory(p),bounded(text,1024));QUOTES.put(p,q);return q;
    }
    public static String confirm(ServerPlayer p,UUID nonce)throws IOException{
        BankService.requireCounter(p);var q=QUOTES.remove(p);var s=CommerceService.store(p.server);CommerceService.verifyReceipt(p,s);
        if(q==null||!q.nonce().equals(nonce)||q.revision()!=s.revision()||System.nanoTime()-q.issued()>30_000_000_000L)throw new IllegalStateException("Order review expired or funding changed. Review again.");
        if(q.selected().startsWith("cancel:")){s.closeOrder(q.nonce(),p.getUUID(),order(p,q.selected()).id(),System.currentTimeMillis());return "Order closed. Only its unspent funding is available in private coin mail.";}
        if(!sameInventory(p,q.inventory()))throw new IllegalStateException("Inventory changed. Review the order transaction again.");
        if(q.selected().equals("create")){
            long now=System.currentTimeMillis();var item=q.item();
            s.createOrder(q.nonce(),p.getUUID(),p.getGameProfile().getName(),bounded(item.getHoverName().getString()+" order",64),(CompoundTag)item.save(p.registryAccess()),q.quantity(),q.unitPrice(),false,now,Math.addExact(now,duration(p.server)));
            return "Whole order funding saved. Your sample remains; matching sellers can now fill this order.";
        }
        var order=order(p,q.selected());var item=q.item().copyWithCount(q.quantity());
        var old=p.getPersistentData().copy();boolean checkpointStarted=false;
        try{
            CommerceService.checkpoint(p);var pending=s.prepareOrderFill(q.nonce(),p.getUUID(),order.id(),q.quantity(),(CompoundTag)item.save(p.registryAccess()),System.currentTimeMillis());
            int left=q.quantity();for(int i=0;i<36&&left>0;i++){var stack=p.getInventory().getItem(i);if(!stack.isEmpty()&&ItemStack.isSameItemSameComponents(stack,item)){int n=Math.min(left,stack.getCount());stack.shrink(n);left-=n;}}
            if(left!=0)throw new IllegalStateException("Matching inventory changed during fill.");
            CommerceService.putReceipt(p,new EconomyStore.Receipt(s.id(),pending.sequence()));checkpointStarted=true;CommerceService.checkpoint(p);
            if(!s.recoverItem(CommerceService.receipt(AtomicNbt.read(CommerceService.playerFile(p.server,p.getUUID())))))throw new IOException("Native fill receipt was not committed.");
            p.getInventory().setChanged();p.inventoryMenu.sendAllDataToRemote();
            LogUtils.getLogger().info("ROYAL_BUY_ORDER_FILL operation={} order={} seller={} quantity={} gross={}",q.nonce(),order.id(),p.getUUID(),q.quantity(),Math.multiplyExact(q.quantity(),q.unitPrice()));
            return "Partial fill saved. Buyer item mail and your funded proceeds are ready to claim.";
        }catch(IOException|RuntimeException e){
            if(!checkpointStarted){for(int i=0;i<36;i++)p.getInventory().setItem(i,q.inventory().get(i).copy());for(var key:new HashSet<>(p.getPersistentData().getAllKeys()))p.getPersistentData().remove(key);p.getPersistentData().merge(old);}
            if(!checkpointStarted&&s.pendingItem().isEmpty()&&!s.quarantined()&&(e instanceof IllegalArgumentException||e instanceof IllegalStateException))throw e;
            s.quarantine();p.connection.disconnect(Component.literal("Buy-order inventory operation interrupted. Matching checkpoint recovery is required."));throw e;
        }
    }
    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> adminCommands(){
        return net.minecraft.commands.Commands.literal("orders")
            .then(net.minecraft.commands.Commands.literal("inspect").executes(c->{var store=CommerceService.store(c.getSource().getServer());
                c.getSource().sendSuccess(()->Component.literal("Buy-order journal "+store.id()+" revision "+store.revision()+" | active "+store.orders().stream().filter(EconomyStore.BuyOrder::active).count()+" | recorded "+store.orders().size()+" | held coins "+store.orders().stream().mapToLong(EconomyStore.BuyOrder::fundsHeld).sum()+" | pending item "+store.pendingItem().isPresent()),false);
                for(var o:store.orders().stream().filter(EconomyStore.BuyOrder::active).toList())c.getSource().sendSuccess(()->Component.literal(o.id()+" | buyer "+o.buyerName()+" | "+o.title()+" | remaining "+o.remaining()+"/"+o.quantity()+" | unit price "+o.unitPrice()+" | funding "+o.fundsHeld()+" | expires "+o.expires()),false);return 1;}))
            .then(net.minecraft.commands.Commands.literal("expire-test").requires(s->s.hasPermission(4)).then(net.minecraft.commands.Commands.argument("order",net.minecraft.commands.arguments.UuidArgument.uuid())
                .executes(c->{c.getSource().sendSuccess(()->Component.literal("Isolated sandbox only: review this exact order; append confirm <reason> to refund only its unspent funding at its saved deadline."),false);return 1;})
                .then(net.minecraft.commands.Commands.literal("confirm").then(net.minecraft.commands.Commands.argument("reason",com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(c->{
                    var source=c.getSource();if(!AuctionSandboxCommands.enabled(source)){source.sendFailure(Component.literal("Sandbox marker absent; no order changed."));return 0;}
                    try{var store=CommerceService.store(source.getServer());var o=store.order(net.minecraft.commands.arguments.UuidArgument.getUuid(c,"order")).orElseThrow(()->new IllegalStateException("Unknown order."));
                        UUID op=UUID.randomUUID(),actor=source.getEntity()==null?CommerceRequest.NONE:source.getEntity().getUUID();
                        store.closeOrder(op,actor,o.id(),Math.max(System.currentTimeMillis(),o.expires()),com.mojang.brigadier.arguments.StringArgumentType.getString(c,"reason"));source.sendSuccess(()->Component.literal("Isolated order expiry saved and audited: "+op+". Only unspent funding returned to private coin mail."),true);return 1;
                    }catch(IOException|IllegalArgumentException|IllegalStateException failure){source.sendFailure(Component.literal(failure.getMessage()==null?"Order recovery required.":failure.getMessage()));return 0;}
                })))));
    }
    private static EconomyStore.BuyOrder order(ServerPlayer p,String selected){
        if(!selected.startsWith("fill:")&&!selected.startsWith("cancel:"))throw new IllegalArgumentException("Choose a valid order.");
        try{return CommerceService.store(p.server).order(UUID.fromString(selected.substring(selected.indexOf(':')+1))).orElseThrow(()->new IllegalStateException("Order no longer exists."));}catch(IllegalArgumentException e){throw new IllegalArgumentException("Invalid order identity.");}
    }
    private static int eligible(ServerPlayer p,ItemStack item){int total=0;for(int i=0;i<36;i++)if(ItemStack.isSameItemSameComponents(p.getInventory().getItem(i),item))total+=p.getInventory().getItem(i).getCount();return total;}
    private static List<ItemStack> inventory(ServerPlayer p){List<ItemStack> result=new ArrayList<>();for(int i=0;i<36;i++)result.add(p.getInventory().getItem(i).copy());return result;}
    private static boolean sameInventory(ServerPlayer p,List<ItemStack> before){for(int i=0;i<36;i++)if(!ItemStack.matches(p.getInventory().getItem(i),before.get(i)))return false;return true;}
    private static String bounded(String text,int length){text=text.replaceAll("[\\p{Cntrl}§]","");return text.length()>length?text.substring(0,length):text;}
}
