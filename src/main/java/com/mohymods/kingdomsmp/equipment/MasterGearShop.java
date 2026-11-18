package com.mohymods.kingdomsmp.equipment;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.profession.*;
import com.mohymods.kingdomsmp.network.CommerceView;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import java.io.IOException;
import java.util.*;
public final class MasterGearShop {
 public record Quote(UUID nonce,long revision,MasterGear gear,long base,long price,long issued,String text) { }
 private static final Map<ServerPlayer,Quote> QUOTES=new WeakHashMap<>();
 private MasterGearShop() { }
 public static void close(ServerPlayer p){QUOTES.remove(p);}
 private static void require(ServerPlayer p,MasterGear gear){ContractService.requireCounter(p,gear.role());if(!CommerceService.store(p.server).professions(p.getUUID()).perksEligible(gear.role(),ProfessionRank.MASTER,ProfessionService.quests()))throw new IllegalStateException("Purchase requires the corresponding active Master profession at its maintained staffed building.");}
 public static Quote review(ServerPlayer p,String id)throws IOException{var g=MasterGear.byId(id);require(p,g);var s=CommerceService.store(p.server);CommerceService.verifyReceipt(p,s);long base=MasterGearRules.get(p.server).prices().get(g),price=g.price(base,s.equipmentPurchases(p.getUUID(),id));if(s.balance(p.getUUID())<price)throw new IllegalStateException("Insufficient coins; exact price "+price);var q=new Quote(UUID.randomUUID(),s.revision(),g,base,price,System.nanoTime(),"Buy "+g.title()+" for exactly "+price+" coins into recoverable private item mail. "+g.detail()+" Your next same-item purchase costs 50% more compounded. Trading/use by others is allowed.");QUOTES.put(p,q);return q;}
 public static String confirm(ServerPlayer p,UUID nonce)throws IOException{var q=QUOTES.remove(p);var s=CommerceService.store(p.server);CommerceService.verifyReceipt(p,s);if(q==null||!q.nonce().equals(nonce)||q.revision()!=s.revision()||System.nanoTime()-q.issued()>30_000_000_000L)throw new IllegalStateException("Review expired or state changed");require(p,q.gear());if(MasterGearRules.get(p.server).prices().get(q.gear())!=q.base()||q.gear().price(q.base(),s.equipmentPurchases(p.getUUID(),q.gear().id()))!=q.price())throw new IllegalStateException("Exact price changed");s.buyMasterGear(q.nonce(),p.getUUID(),q.gear().id(),q.base(),(CompoundTag)MasterGearItems.stack(p,q.gear()).save(p.registryAccess()));return q.gear().title()+" purchased once; claim it in your private Mailbox.";}
 public static List<CommerceView.Entry> entries(ServerPlayer p){var s=CommerceService.store(p.server);var roster=s.professions(p.getUUID());List<CommerceView.Entry> rows=new ArrayList<>();for(var g:MasterGear.values()){if(!roster.isActive(g.role()))continue;long price;try{price=g.price(MasterGearRules.get(p.server).prices().get(g),s.equipmentPurchases(p.getUUID(),g.id()));}catch(IllegalStateException cap){continue;}boolean allowed=roster.perksEligible(g.role(),ProfessionRank.MASTER,ProfessionService.quests())&&ProfessionService.available(p,g.role());rows.add(new CommerceView.Entry("mastergear:"+g.id(),g.title(),g.detail(),price,allowed?"EQUIPMENT":"LOCKED"));}return List.copyOf(rows);}
}
