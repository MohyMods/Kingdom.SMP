package com.mohymods.kingdomsmp.economy;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.network.CommerceView;
import com.mohymods.kingdomsmp.npc.ReeveEntity;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import com.mohymods.kingdomsmp.persistence.ConstructionStore.Phase;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.io.IOException;
import java.util.*;

/** Real in-person bank conversion; native inventory and wallet use one recoverable staged operation. */
public final class BankService {
    public record Quote(UUID nonce,long revision,String selected,int quantity,long delta,long issued,List<ItemStack> inventory,String text) { }
    private static final Map<ServerPlayer,Quote> QUOTES=new WeakHashMap<>();
    private BankService() { }
    public static void close(ServerPlayer player){QUOTES.remove(player);}
    public static Optional<Quote> quote(ServerPlayer player){
        var q=QUOTES.get(player);
        if(q!=null&&(System.nanoTime()-q.issued()>30_000_000_000L||CommerceService.store(player.server).revision()!=q.revision())){QUOTES.remove(player);q=null;}
        return Optional.ofNullable(q);
    }
    public static void requireCounter(ServerPlayer player){
        if(!player.isAlive()||player.isSpectator()||player.serverLevel()!=player.server.overworld()||player.containerMenu!=player.inventoryMenu||!player.containerMenu.getCarried().isEmpty()
                ||player.gameMode.getGameModeForPlayer()!=GameType.SURVIVAL&&player.gameMode.getGameModeForPlayer()!=GameType.ADVENTURE)
            throw new IllegalStateException("Visit the bank alive in Survival/Adventure with containers closed.");
        if(ContributionService.kingdom(player.server).attainedTier()<3)throw new IllegalStateException("Royal banking requires kingdom Tier 3.");
        for(var project:ConstructionService.store(player.server).projects()){
            if(!project.blueprint().equals(RoyalBankBlueprint.ID)||project.phase()!=Phase.COMPLETE||project.npcDead()||!project.npcIssued())continue;
            var entity=player.serverLevel().getEntity(project.npc());
            if(entity instanceof ReeveEntity banker&&banker.isAlive()&&ConstructionService.registeredReeve(banker)
                    &&player.distanceToSqr(banker)<=64&&player.hasLineOfSight(banker)&&ConstructionService.structureIntact(player.serverLevel(),project))return;
        }
        throw new IllegalStateException("Visit the registered Banker within eight blocks at the intact completed Royal Bank.");
    }
    public static List<CommerceView.Entry> entries(ServerPlayer player){
        int tier=ContributionService.kingdom(player.server).attainedTier();boolean usable=true;
        try{requireCounter(player);}catch(IllegalStateException unavailable){usable=false;}
        String state=usable?"BANK":"LOCKED";
        List<CommerceView.Entry> entries=new ArrayList<>();
        entries.add(new CommerceView.Entry("deposit","Deposit physical coins","Deposit all unmodified kingdom coins from your main inventory at the completed Tier 3 Bank. Copper 1; Silver 10; Gold 100; Platinum 1,000.",depositValue(player),state));
        for(var d:CurrencyItems.Denomination.values())entries.add(new CommerceView.Entry(d.id(),"Withdraw "+d.title()+"s","Each physical "+d.title()+" is worth "+d.value()+" kingdom coins. Choose quantity; balance and actual inventory capacity are rechecked.",d.value(),state));
        if(usable)for(var other:player.server.getPlayerList().getPlayers()){
            if(other==player||player.distanceToSqr(other)>64||!player.hasLineOfSight(other))continue;
            try{requireCounter(other);}catch(IllegalStateException unavailable){continue;}
            entries.add(new CommerceView.Entry("transfer:"+other.getUUID(),"Transfer to "+other.getGameProfile().getName(),"Send coins to this player in person at the registered Bank. Both players must remain nearby and eligible until confirmation.",1,"BANK"));
        }
        return List.copyOf(entries);
    }
    private static ServerPlayer recipient(ServerPlayer sender,String selected){
        if(!selected.startsWith("transfer:"))throw new IllegalArgumentException("Select an in-person recipient");
        UUID id;try{id=UUID.fromString(selected.substring("transfer:".length()));}catch(IllegalArgumentException e){throw new IllegalArgumentException("Invalid transfer recipient");}
        if(id.equals(sender.getUUID()))throw new IllegalArgumentException("Cannot transfer coins to yourself");
        var other=sender.server.getPlayerList().getPlayer(id);
        if(other==null||sender.distanceToSqr(other)>64||!sender.hasLineOfSight(other))throw new IllegalStateException("Both players must stay nearby at the Bank");
        requireCounter(other);return other;
    }
    public static Quote review(ServerPlayer player,String selected,int requested)throws IOException{
        requireCounter(player);var store=CommerceService.store(player.server);CommerceService.verifyReceipt(player);
        if(store.pendingSale().isPresent()||store.pendingTransfer().isPresent()||store.pendingItem().isPresent()||store.quarantined())throw new IllegalStateException("Bank paused for inventory reconciliation.");
        if(requested<1||requested>2304)throw new IllegalArgumentException("Invalid coin quantity.");
        long delta;int quantity;String text;
        if(selected.equals("deposit")){
            delta=depositValue(player);quantity=0;
            if(delta<1)throw new IllegalStateException("No unmodified kingdom coins in your main inventory.");
            if(Math.addExact(store.balance(player.getUUID()),delta)>EconomyStore.MAX_COINS)throw new IllegalStateException("Virtual coin balance limit reached.");
            text="Deposit all unmodified physical coins in your main inventory for "+delta+" virtual coins. These physical coins are permanently consumed. Review expires after 30 seconds.";
        }else if(selected.startsWith("transfer:")){
            var other=recipient(player,selected);quantity=requested==2304?(int)Math.min(2304,Math.min(store.balance(player.getUUID()),EconomyStore.MAX_COINS-store.balance(other.getUUID()))):requested;
            if(quantity<1)throw new IllegalStateException("No transferable coins or recipient capacity remain");
            if(store.balance(player.getUUID())<quantity)throw new IllegalStateException("Insufficient coins for this transfer");
            if(Math.addExact(store.balance(other.getUUID()),quantity)>EconomyStore.MAX_COINS)throw new IllegalStateException("Recipient balance limit reached");
            delta=-quantity;text="Transfer exactly "+quantity+" virtual coins to "+other.getGameProfile().getName()+". Both players must remain at the Bank. This payment is final and cannot be reclaimed by repeating confirmation.";
        }else{
            var denomination=CurrencyItems.denomination(selected);quantity=requested==2304?(int)Math.min(2304,Math.min(store.balance(player.getUUID())/denomination.value(),capacity(player,denomination))):requested;
            if(quantity<1)throw new IllegalStateException("No coins can be withdrawn with the available balance and inventory space");
            long amount=Math.multiplyExact((long)quantity,denomination.value());
            if(store.balance(player.getUUID())<amount)throw new IllegalStateException("Insufficient virtual coins for this withdrawal.");
            if(capacity(player,denomination)<quantity)throw new IllegalStateException("Make enough main-inventory space before withdrawing this quantity.");
            delta=-amount;
            text="Withdraw exactly "+quantity+" "+denomination.title()+"s for "+amount+" virtual coins. Physical delivery and account debit use one recoverable inventory receipt. Review expires after 30 seconds.";
        }
        var quote=new Quote(UUID.randomUUID(),store.revision(),selected,quantity,delta,System.nanoTime(),inventory(player),text);QUOTES.put(player,quote);return quote;
    }
    public static String confirm(ServerPlayer player,UUID nonce)throws IOException{
        requireCounter(player);var q=QUOTES.remove(player);var store=CommerceService.store(player.server);
        if(q==null||!q.nonce().equals(nonce)||System.nanoTime()-q.issued()>30_000_000_000L)throw new IllegalStateException("Bank review expired. Review the exact conversion again.");
        if(q.revision()!=store.revision()||!sameInventory(player,q.inventory()))throw new IllegalStateException("Inventory or account changed. Review the conversion again.");
        CommerceService.verifyReceipt(player);
        if(!sameInventory(player,q.inventory()))throw new IllegalStateException("Inventory was reconciled; review the conversion again");
        if(q.selected().startsWith("transfer:")){
            var other=recipient(player,q.selected());store.transferCoins(q.nonce(),player.getUUID(),other.getUUID(),q.quantity());
            LogUtils.getLogger().info("ROYAL_BANK_PAYMENT operation={} sender={} recipient={} coins={}",q.nonce(),player.getUUID(),other.getUUID(),q.quantity());
            return "Transferred "+q.quantity()+" coins to "+other.getGameProfile().getName()+". Both account balances saved.";
        }
        if(q.delta()>0&&depositValue(player)!=q.delta())throw new IllegalStateException("Coin inventory changed.");
        CurrencyItems.Denomination denomination=q.delta()<0?CurrencyItems.denomination(q.selected()):null;
        if(denomination!=null&&(store.balance(player.getUUID()) < -q.delta()||capacity(player,denomination)<q.quantity()))throw new IllegalStateException("Balance or delivery capacity changed.");
        CompoundTag oldPersistent=player.getPersistentData().copy();boolean checkpointStarted=false;
        try{
            CommerceService.checkpoint(player);
            var pending=store.prepareTransfer(q.nonce(),player.getUUID(),q.delta());
            if(q.delta()>0){
                for(int slot=0;slot<36;slot++)if(CurrencyItems.denomination(player.getInventory().getItem(slot))!=null)player.getInventory().setItem(slot,ItemStack.EMPTY);
            }else addCoins(player,denomination,q.quantity());
            CommerceService.putReceipt(player,new EconomyStore.Receipt(store.id(),pending.sequence()));
            checkpointStarted=true;CommerceService.checkpoint(player);
            if(!store.recoverTransfer(CommerceService.receipt(AtomicNbt.read(CommerceService.playerFile(player.server,player.getUUID())))))throw new IOException("Bank inventory receipt did not commit");
            player.getInventory().setChanged();player.inventoryMenu.sendAllDataToRemote();
            LogUtils.getLogger().info("ROYAL_BANK_CONVERSION operation={} player={} delta={} denomination={} quantity={}",q.nonce(),player.getUUID(),q.delta(),q.selected(),q.quantity());
            return q.delta()>0?"Deposited "+q.delta()+" virtual coins.":"Withdrew "+q.quantity()+" "+denomination.title()+"s. Account debit and physical delivery saved.";
        }catch(IOException|RuntimeException failure){
            if(!checkpointStarted){for(int i=0;i<36;i++)player.getInventory().setItem(i,q.inventory().get(i).copy());for(var key:new HashSet<>(player.getPersistentData().getAllKeys()))player.getPersistentData().remove(key);player.getPersistentData().merge(oldPersistent);}
            store.quarantine();player.connection.disconnect(Component.literal("Bank conversion interrupted. Restore/reconcile matching player and economy checkpoints before retrying."));throw failure;
        }
    }
    private static long depositValue(ServerPlayer player){long value=0;for(int i=0;i<36;i++)value=Math.addExact(value,CurrencyItems.value(player.getInventory().getItem(i)));return value;}
    private static int capacity(ServerPlayer player,CurrencyItems.Denomination denomination){
        var coin=new ItemStack(denomination.item());int count=0;
        for(int i=0;i<36;i++){var s=player.getInventory().getItem(i);if(s.isEmpty())count+=coin.getMaxStackSize();else if(ItemStack.isSameItemSameComponents(s,coin))count+=Math.max(0,coin.getMaxStackSize()-s.getCount());}return count;
    }
    private static void addCoins(ServerPlayer player,CurrencyItems.Denomination denomination,int amount){
        var coin=new ItemStack(denomination.item());int remaining=amount;
        for(int i=0;i<36&&remaining>0;i++){
            var s=player.getInventory().getItem(i);if(s.isEmpty())continue;
            if(ItemStack.isSameItemSameComponents(s,coin)){int n=Math.min(remaining,Math.max(0,coin.getMaxStackSize()-s.getCount()));s.grow(n);remaining-=n;}
        }
        for(int i=0;i<36&&remaining>0;i++)if(player.getInventory().getItem(i).isEmpty()){
            int n=Math.min(remaining,coin.getMaxStackSize());player.getInventory().setItem(i,new ItemStack(denomination.item(),n));remaining-=n;
        }
        if(remaining!=0)throw new IllegalStateException("Coin delivery capacity changed during conversion.");
    }
    private static List<ItemStack> inventory(ServerPlayer p){List<ItemStack> out=new ArrayList<>();for(int i=0;i<36;i++)out.add(p.getInventory().getItem(i).copy());return List.copyOf(out);}
    private static boolean sameInventory(ServerPlayer p,List<ItemStack> before){for(int i=0;i<36;i++)if(!ItemStack.matches(p.getInventory().getItem(i),before.get(i)))return false;return true;}
}
