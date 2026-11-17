package com.mohymods.kingdomsmp.economy;

import com.mohymods.kingdomsmp.commerce.CommerceCatalog;
import com.mohymods.kingdomsmp.network.CommerceView;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.util.*;

/** Marginal resource prices and finite purchases at real existing resident counters. */
public final class MarketService {
    private static final Map<MinecraftServer,MarketCatalog> CATALOGS=new WeakHashMap<>();
    public record Sale(int quantity,long amount) { }
    private MarketService(){ }
    public static void initialize(MinecraftServer server){CATALOGS.remove(server);}
    public static MarketCatalog catalog(MinecraftServer server){
        var c=CATALOGS.get(server);if(c!=null)return c;
        try{c=MarketCatalog.load(server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-market.json"),CommerceService.catalog(server));CATALOGS.put(server,c);return c;}
        catch(IOException e){throw new IllegalStateException("NPC market configuration requires recovery; no resource trade is authorized.",e);}
    }
    public static long day(){return Math.floorDiv(System.currentTimeMillis(),86_400_000L);}
    public static String key(String id){return "kingdom_smp:supplies/"+id;}
    public static Sale sale(ServerPlayer p,String id,int requested){
        if(requested<1||requested>2304)throw new IllegalArgumentException("Choose a sale quantity from 1 to 2304.");
        var c=CommerceService.catalog(p.server);var item=c.supply(id);var rule=catalog(p.server).rule(id);var used=CommerceService.store(p.server).usage(day(),key(id),p.getUUID());
        int available=Math.min(requested,CommerceService.eligible(p,item));
        if(available<1)throw new IllegalStateException("No exact plain matching items in your main inventory.");
        long budget=Math.max(0,Math.min(c.budget()-used.sharedSpent(),com.mohymods.kingdomsmp.profession.ProfessionService.allowance(p,id,c.allowance(),c.budget())-used.playerProceeds()));
        if(used.resourceQuantity()>=item.quota())throw new IllegalStateException("Today's resource quota is exhausted.");
        int quantity=MarketPricing.affordable(rule.minimumBuy(),item.price(),used.resourceQuantity(),item.quota(),available,budget,false);
        if(quantity<1)throw new IllegalStateException("Today's buying budget or your personal allowance is exhausted.");
        return new Sale(quantity,MarketPricing.total(rule.minimumBuy(),item.price(),used.resourceQuantity(),item.quota(),quantity,false));
    }
    public static Sale purchase(ServerPlayer p,String id,int requested){
        if(requested<1||requested>2304)throw new IllegalArgumentException("Choose a purchase quantity from 1 to 2304.");
        var item=CommerceService.catalog(p.server).supply(id);var rule=catalog(p.server).rule(id);var s=CommerceService.store(p.server);long used=s.shopSold(day(),key(id));
        if(used>=rule.stock())throw new IllegalStateException("Shop stock is exhausted until the next UTC day.");
        int quantity=MarketPricing.affordable(rule.minimumSell(),rule.maximumSell(),used,rule.stock(),Math.min(Math.min(64,item.item().getDefaultInstance().getMaxStackSize()),requested),s.balance(p.getUUID()),true);
        if(quantity<1)throw new IllegalStateException("Insufficient coins for any remaining stock.");
        return new Sale(quantity,MarketPricing.total(rule.minimumSell(),rule.maximumSell(),used,rule.stock(),quantity,true));
    }
    public static String buy(ServerPlayer p,UUID operation,String id,int quantity,long amount,long period)throws IOException{
        var s=CommerceService.store(p.server);var item=CommerceService.catalog(p.server).supply(id);var rule=catalog(p.server).rule(id);
        long expected=MarketPricing.total(rule.minimumSell(),rule.maximumSell(),s.shopSold(period,key(id)),rule.stock(),quantity,true);
        if(amount!=expected||period!=day())throw new IllegalStateException("Shop price, stock or trading day changed. Review again.");
        var stack=new ItemStack(item.item(),quantity);
        s.shopBuy(operation,p.getUUID(),key(id),"NPC shop: "+item.title(),(CompoundTag)stack.save(p.registryAccess()),quantity,period,rule.stock(),rule.minimumSell(),rule.maximumSell());
        return "Purchased "+quantity+" "+item.title()+" for "+amount+" coins. Claim the exact items through Mailbox at a resident counter.";
    }
    public static List<CommerceView.Entry> entries(ServerPlayer p,int tier){
        var c=CommerceService.catalog(p.server);var s=CommerceService.store(p.server);var market=catalog(p.server);List<CommerceView.Entry> rows=new ArrayList<>();long day=day();
        for(var item:c.supplies()){
            var rule=market.rule(item.id());var used=s.usage(day,key(item.id()),p.getUUID());long stock=s.shopSold(day,key(item.id()));
            long buy=used.resourceQuantity()>=item.quota()?rule.minimumBuy():MarketPricing.unit(rule.minimumBuy(),item.price(),used.resourceQuantity(),item.quota(),false);
            rows.add(new CommerceView.Entry(item.id(),"Sell: "+item.title(),"NPC buys plain items. Quota "+used.resourceQuantity()+"/"+item.quota()+"; next unit "+buy+" coins, range "+rule.minimumBuy()+"–"+item.price()+". Budget "+used.sharedSpent()+"/"+c.budget()+"; your sales "+used.playerProceeds()+"/"+com.mohymods.kingdomsmp.profession.ProfessionService.allowance(p,item.id(),c.allowance(),c.budget())+". Each unit reprices; UTC reset.",buy,tier<2?"LOCKED":"SELL"));
            long sell=stock>=rule.stock()?rule.maximumSell():MarketPricing.unit(rule.minimumSell(),rule.maximumSell(),stock,rule.stock(),true);
            rows.add(new CommerceView.Entry("shop:"+item.id(),"Buy: "+item.title(),"NPC sells plain items. Remaining stock "+Math.max(0,rule.stock()-stock)+"/"+rule.stock()+". Next unit "+sell+" coins; range "+rule.minimumSell()+"–"+rule.maximumSell()+". One stack per purchase, delivered through private Mailbox. Restocks at UTC midnight.",stock>=rule.stock()?0:sell,tier<2?"LOCKED":stock>=rule.stock()?"OUT_OF_STOCK":"SHOP"));
        }
        return List.copyOf(rows);
    }
    public static String reviewText(ServerPlayer p,String selected,int quantity,long amount){
        boolean buying=selected.startsWith("shop:");String id=buying?selected.substring(5):selected;var item=CommerceService.catalog(p.server).supply(id);
        return buying?"Spend exactly "+amount+" coins for "+quantity+" plain "+item.title()+". Each unit follows the displayed stock-price bands. Payment, stock and private item mail commit together. Claim at a resident counter with enough inventory space. Nothing is dropped. Purchase coins leave circulation; they do not refill the daily NPC buying budget.":"Permanently sell exactly "+quantity+" plain "+item.title()+" for "+amount+" coins. This total sums every unit's current supply-price band; larger batches cannot retain an earlier higher rate. Actual items, shared budget, resource quota and personal allowance are rechecked. Review expires in 30 seconds.";
    }
    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> adminCommands(){
        return net.minecraft.commands.Commands.literal("market").then(net.minecraft.commands.Commands.literal("inspect").executes(c->{var server=c.getSource().getServer();var s=CommerceService.store(server);var config=CommerceService.catalog(server);var market=catalog(server);long today=day();
            c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("NPC market UTC day "+today+" | journal revision "+s.revision()+" | spent purchasing shop items "+s.shopCoinsSpent()+" coins | daily budget "+config.budget()+" | personal allowance "+config.allowance()),false);
            for(var supply:config.supplies()){var rule=market.rule(supply.id());var used=s.usage(today,key(supply.id()),new UUID(0,0));long stock=s.shopSold(today,key(supply.id()));c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal(supply.title()+" | purchased from players "+used.resourceQuantity()+"/"+supply.quota()+" | shared spent "+used.sharedSpent()+" | sold stock "+stock+"/"+rule.stock()+" | buy bounds "+rule.minimumBuy()+"–"+supply.price()+" | sell bounds "+rule.minimumSell()+"–"+rule.maximumSell()),false);}return 1;}));
    }
}
