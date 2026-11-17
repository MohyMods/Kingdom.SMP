package com.mohymods.kingdomsmp.economy;
import com.mohymods.kingdomsmp.profession.FarmerInventoryExchange;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import java.util.*;
import java.io.IOException;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
/** Exact plain coins in/out use the same staged native inventory receipt as other physical transactions. */
public final class CoinExchangeService {
 public record Quote(UUID nonce,long revision,CoinExchangeTerms terms,List<ItemStack> before,List<ItemStack> after,long issued,String text){
  public Quote{before=before.stream().map(ItemStack::copy).toList();after=after.stream().map(ItemStack::copy).toList();}
  @Override public List<ItemStack> before(){return before.stream().map(ItemStack::copy).toList();}
  @Override public List<ItemStack> after(){return after.stream().map(ItemStack::copy).toList();}
 }
 private static final Map<ServerPlayer,Quote> QUOTES=new WeakHashMap<>();
 private CoinExchangeService(){}
 public static void close(ServerPlayer p){QUOTES.remove(p);}
 public static Optional<Quote> quote(ServerPlayer p){var q=QUOTES.get(p);if(q!=null&&(System.nanoTime()-q.issued()>30_000_000_000L||q.revision()!=CommerceService.store(p.server).revision())){close(p);q=null;}return Optional.ofNullable(q);}
 public static Quote review(ServerPlayer p,String action,int batches)throws IOException{
  BankService.requireCounter(p);var s=CommerceService.store(p.server);CommerceService.verifyReceipt(p,s);var terms=CoinExchangeTerms.parse(action,Math.min(230,batches));var from=CurrencyItems.denomination(terms.source());var to=CurrencyItems.denomination(terms.target());List<ItemStack> before=new ArrayList<>();for(int i=0;i<36;i++)before.add(p.getInventory().getItem(i).copy());var after=FarmerInventoryExchange.plan(before,Map.of(from.item(),terms.inputs()),Map.of(to.item(),terms.outputs()));var q=new Quote(UUID.randomUUID(),s.revision(),terms,before,after,System.nanoTime(),"Exchange exactly "+terms.inputs()+" "+from.title()+"s for "+terms.outputs()+" "+to.title()+"s at face value. Only plain coins qualify. Your virtual balance does not change; review consumes nothing.");QUOTES.put(p,q);return q;
 }
 public static String confirm(ServerPlayer p,UUID nonce)throws IOException{
  BankService.requireCounter(p);var q=QUOTES.remove(p);var s=CommerceService.store(p.server);CommerceService.verifyReceipt(p,s);if(q==null||!q.nonce().equals(nonce)||q.revision()!=s.revision()||System.nanoTime()-q.issued()>30_000_000_000L)throw new IllegalStateException("Coin exchange expired; review again");var before=q.before();for(int i=0;i<36;i++)if(!ItemStack.matches(p.getInventory().getItem(i),before.get(i)))throw new IllegalStateException("Inventory changed; review the coin exchange again");var old=p.getPersistentData().copy();boolean checkpointStarted=false;
  try{CommerceService.checkpoint(p);var pending=s.prepareCoinExchange(q.nonce(),p.getUUID(),q.terms());var after=q.after();for(int i=0;i<36;i++)p.getInventory().setItem(i,after.get(i).copy());CommerceService.putReceipt(p,new EconomyStore.Receipt(s.id(),pending.sequence()));checkpointStarted=true;CommerceService.checkpoint(p);if(!s.recoverItem(CommerceService.receipt(AtomicNbt.read(CommerceService.playerFile(p.server,p.getUUID())))))throw new IOException("Coin exchange native receipt did not commit");p.getInventory().setChanged();p.inventoryMenu.sendAllDataToRemote();return "Physical coins exchanged at equal value. Native receipt saved; virtual balance unchanged.";}
  catch(IOException|RuntimeException failure){if(!checkpointStarted){for(int i=0;i<36;i++)p.getInventory().setItem(i,before.get(i).copy());for(var key:new HashSet<>(p.getPersistentData().getAllKeys()))p.getPersistentData().remove(key);p.getPersistentData().merge(old);}if(!checkpointStarted&&s.pendingItem().isEmpty()&&!s.quarantined()&&(failure instanceof IllegalArgumentException||failure instanceof IllegalStateException))throw failure;s.quarantine();p.connection.disconnect(net.minecraft.network.chat.Component.literal("Coin exchange interrupted; restore matching native inventory and canonical checkpoint"));throw failure;}
 }
}
