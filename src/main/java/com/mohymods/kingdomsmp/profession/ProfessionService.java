package com.mohymods.kingdomsmp.profession;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.network.*;
import com.mohymods.kingdomsmp.persistence.ConstructionStore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.io.IOException;
import java.util.*;

/** Actual server roster enrollment/switches; no rank, XP or completed quest is accepted from a packet. */
public final class ProfessionService {
    public record Quote(UUID nonce,long revision,String action,String from,String to,long fee,long issued,String text){ }
    private static final Map<ServerPlayer,Quote> QUOTES=new WeakHashMap<>();
    private static final ProfessionQuestCatalog QUESTS=ProfessionQuestService.catalog();
    private ProfessionService(){ }
    public static void close(ServerPlayer p){QUOTES.remove(p);EnchantedToolService.close(p);FarmerDeliveryService.close(p);CraftDeliveryService.close(p);com.mohymods.kingdomsmp.equipment.FarmerGearService.close(p);}
    public static void initialize(MinecraftServer server){ProfessionRules.clear(server);RepairQuestRules.clear(server);BuilderInspectionRules.clear(server);ContractService.initialize(server);CommissionQuestRules.clear(server);CraftDeliveryRules.clear(server);QUOTES.keySet().removeIf(p->p.server==server);}
    public static ProfessionQuestCatalog quests(){return QUESTS;}
    public static Optional<Quote> quote(ServerPlayer p){var q=QUOTES.get(p);if(q!=null&&(q.revision()!=CommerceService.store(p.server).revision()||System.nanoTime()-q.issued()>30_000_000_000L)){close(p);q=null;}return Optional.ofNullable(q);}
    public static String rankName(ProfessionRank rank){return rank.id().substring(0,1).toUpperCase(Locale.ROOT)+rank.id().substring(1);}
    public static String name(Profession p){return p.id().substring(0,1).toUpperCase(Locale.ROOT)+p.id().substring(1);}
    public static boolean available(ServerPlayer player,Profession profession){
        return ConstructionService.store(player.server).projects().stream().anyMatch(p->p.phase()==ConstructionStore.Phase.COMPLETE&&!p.npcDead()&&p.blueprint().toString().equals(profession.buildingId())&&ConstructionService.structureIntact(player.serverLevel(),p));
    }
    private static void requireBuilding(ServerPlayer p,Profession profession){if(!available(p,profession))throw new IllegalStateException("Complete and maintain the "+name(profession)+" profession building first.");}
    public static Quote review(ServerPlayer p,String selected)throws IOException{
        CommerceService.requireResidentCounter(p);var s=CommerceService.store(p.server);
        if(selected.startsWith("gear:")){
            var gear=com.mohymods.kingdomsmp.equipment.FarmerGearService.review(p,selected.substring(5));var q=new Quote(gear.nonce(),gear.revision(),"gear","",gear.gear().id(),gear.price(),gear.issued(),gear.text());QUOTES.put(p,q);return q;
        }
        if(selected.equals("enchanted:"+EnchantedToolService.QUEST)){var delivery=EnchantedToolService.review(p);var q=new Quote(delivery.nonce(),delivery.revision(),"enchanted","",EnchantedToolService.QUEST,0,delivery.issued(),delivery.text());QUOTES.put(p,q);return q;}
        if(selected.startsWith("materials:")){
            var delivery=CraftDeliveryService.review(p,selected.substring(10));var q=new Quote(delivery.nonce(),delivery.revision(),"materials","",delivery.quest(),0,delivery.issued(),delivery.text());QUOTES.put(p,q);return q;
        }
        if(selected.startsWith("food:")){
            var delivery=FarmerDeliveryService.review(p,selected.substring(5));var q=new Quote(delivery.nonce(),delivery.revision(),"food","",delivery.quest(),0,delivery.issued(),delivery.text());QUOTES.put(p,q);return q;
        }
        if(selected.startsWith("quest:")){
            String id=selected.substring(6);if(!ProfessionQuestService.ready(p,id))throw new IllegalStateException(ProfessionQuestService.status(p,id));
            String text="Record "+ProfessionQuestService.title(id)+" and "+ProfessionQuestService.xp(id)+" XP once. "+ProfessionQuestService.status(p,id)+" No inventory or coins are consumed by this event objective.";
            var q=new Quote(UUID.randomUUID(),s.revision(),"quest","",id,0,System.nanoTime(),text);QUOTES.put(p,q);return q;
        }
        var parts=selected.split(":",-1);String action,from,to;
        if(parts.length==2&&parts[0].equals("learn")){action="enroll";from="";to=parts[1];}
        else if(parts.length==3&&parts[0].equals("switch")){action="switch";from=parts[1];to=parts[2];}
        else throw new IllegalArgumentException("Choose an available profession enrollment or replacement.");
        var target=Profession.byId(to);requireBuilding(p,target);var roster=s.professions(p.getUUID());var transition=action.equals("enroll")?roster.enroll(target):roster.switchProfession(Profession.byId(from),target);
        if(!transition.changed())throw new IllegalStateException("Profession already active; no payment required.");
        if(s.balance(p.getUUID())<transition.fee())throw new IllegalStateException("Profession changes cost 15,000 coins. Your progress is preserved.");
        String text=(action.equals("switch")?"Replace active "+name(Profession.byId(from))+" with ":"Activate ")+name(target)+" for exactly "+String.format(Locale.ROOT,"%,d",transition.fee())+" coins. You may have up to three active professions. Inactive XP and completed quests are kept, but inactive professions grant no bonuses or shopping rights. Switching or returning to a learned profession costs 15,000 coins.";
        var q=new Quote(UUID.randomUUID(),s.revision(),action,from,to,transition.fee(),System.nanoTime(),text);QUOTES.put(p,q);return q;
    }
    public static String confirm(ServerPlayer p,UUID nonce)throws IOException{
        CommerceService.requireResidentCounter(p);var q=QUOTES.remove(p);var s=CommerceService.store(p.server);CommerceService.verifyReceipt(p,s);
        if(q==null||!q.nonce().equals(nonce)||q.revision()!=s.revision()||System.nanoTime()-q.issued()>30_000_000_000L)throw new IllegalStateException("Profession review expired or wallet/roster changed. Review again.");
        if(q.action().equals("gear"))return com.mohymods.kingdomsmp.equipment.FarmerGearService.confirm(p,q.nonce());
        if(q.action().equals("enchanted"))return EnchantedToolService.confirm(p,q.nonce());
        if(q.action().equals("materials"))return CraftDeliveryService.confirm(p,q.nonce());
        if(q.action().equals("food"))return FarmerDeliveryService.confirm(p,q.nonce());
        if(q.action().equals("quest")){ProfessionQuestService.complete(p,q.nonce(),q.to());return ProfessionQuestService.title(q.to())+" recorded once; earned XP saved.";}
        requireBuilding(p,Profession.byId(q.to()));s.changeProfession(q.nonce(),p.getUUID(),q.action(),q.from(),q.to());return name(Profession.byId(q.to()))+" is active. Your progress is saved.";
    }
    public static List<CommerceView.Entry> entries(ServerPlayer p){
        var s=CommerceService.store(p.server);var roster=s.professions(p.getUUID());List<CommerceView.Entry> result=new ArrayList<>();
        for(var profession:Profession.values()){
            boolean active=roster.isActive(profession),eligible=available(p,profession);long fee=0;
            if(!active&&roster.active().size()<3)fee=roster.enroll(profession).fee();
            result.add(new CommerceView.Entry("learn:"+profession.id(),name(profession),active?"Active "+rankName(roster.rank(profession,QUESTS))+". XP "+String.format(Locale.ROOT,"%,d",roster.track(profession).xp())+"; quests "+roster.track(profession).completedQuestIds().size()+"/24. Only active professions grant bonuses. "+(QUESTS.definitions().values().stream().anyMatch(d->d.profession()==profession)?"Select an objective below to see your progress.":"Quest progression for this profession is not yet available."):!eligible?"Requires its completed, intact profession building.":roster.active().size()>=3?"Three professions are active. Select a replacement below for 15,000 coins. Inactive progress is kept.":"The first three professions you learn are free. Switching or reactivating costs 15,000 coins. You may have up to three active professions.",fee,active?"PROF_ACTIVE":eligible&&roster.active().size()<3?"PROF_ENROLL":"LOCKED"));
        }
        for(var from:Profession.values())if(roster.isActive(from))for(var to:Profession.values())if(!roster.isActive(to)&&available(p,to))result.add(new CommerceView.Entry("switch:"+from.id()+":"+to.id(),name(from)+" → "+name(to),"Replace this active profession for 15,000 coins. Its XP and completed quests are kept, but its bonuses stop.",15000,"PROF_SWITCH"));
        for(var definition:QUESTS.visibleFor(roster.active()).stream().sorted(Comparator.comparing(ProfessionQuestCatalog.Definition::id)).toList()){
            var id=definition.id();var role=definition.profession();boolean done=roster.track(role).completedQuestIds().contains(id);
            boolean enchanted=EnchantedToolService.QUEST.equals(id)&&EnchantedToolService.delivery(p);
            boolean materials=CraftDeliveryService.eligible(p,id);
            boolean delivery=enchanted||materials||role==Profession.FARMER&&FarmerDeliveryService.eligible(p,id);
            boolean ongoing=roster.isActive(role)&&QUESTS.mayAttempt(role,id,roster.track(role).completedQuestIds());
            result.add(new CommerceView.Entry((enchanted?"enchanted:":materials?"materials:":delivery?"food:":"quest:")+id,name(role)+" · "+ProfessionQuestService.title(id),rankName(definition.rank())+" objective "+definition.ordinal()+". "+ProfessionQuestService.status(p,id),0,done?"COMPLETE":delivery?"PROF_DELIVERY":ProfessionQuestService.ready(p,id)?"PROF_QUEST":ongoing?"IN_PROGRESS":"LOCKED"));
        }
        result.addAll(com.mohymods.kingdomsmp.equipment.FarmerGearService.entries(p));
        return List.copyOf(result);
    }
    /** Bonuses increase the personal selling allowance, never per-item payouts or shared resource quotas. */
    public static long allowance(ServerPlayer p,String resource,long base,long sharedBudget){
        Profession profession=switch(resource){case "wheat"->Profession.FARMER;case "logs"->Profession.LUMBERJACK;case "stone"->Profession.BUILDER;case "iron"->Profession.MINER;default->null;};
        if(profession==null)return base;var roster=CommerceService.store(p.server).professions(p.getUUID());if(!roster.isActive(profession))return base;
        var rank=roster.rank(profession,QUESTS);long bonus=ProfessionRules.get(p.server).bonus(rank);
        return Math.min(sharedBudget,Math.addExact(base,bonus));
    }
    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> commands(){return net.minecraft.commands.Commands.literal("profession").then(net.minecraft.commands.Commands.literal("open").executes(c->{CommerceNetworking.open(c.getSource().getPlayerOrException(),7);return 1;})).then(net.minecraft.commands.Commands.literal("inspect").executes(c->{var p=c.getSource().getPlayerOrException();var r=CommerceService.store(p.server).professions(p.getUUID());c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("Active "+r.active()+" | learned "+r.tracks().keySet()+" | approved progression catalogue "+QUESTS.enabled()),false);return 1;}));}
}
