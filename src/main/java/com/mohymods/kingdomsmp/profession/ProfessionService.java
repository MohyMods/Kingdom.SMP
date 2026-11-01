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
    private static final ProfessionQuestCatalog QUESTS=ProfessionQuestCatalog.disabled();
    private ProfessionService(){ }
    public static void close(ServerPlayer p){QUOTES.remove(p);}
    public static void initialize(MinecraftServer server){ProfessionRules.clear(server);ContractService.initialize(server);QUOTES.keySet().removeIf(p->p.server==server);}
    public static ProfessionQuestCatalog quests(){return QUESTS;}
    public static Optional<Quote> quote(ServerPlayer p){var q=QUOTES.get(p);if(q!=null&&(q.revision()!=CommerceService.store(p.server).revision()||System.nanoTime()-q.issued()>30_000_000_000L)){close(p);q=null;}return Optional.ofNullable(q);}
    public static String name(Profession p){return p.id().substring(0,1).toUpperCase(Locale.ROOT)+p.id().substring(1);}
    public static boolean available(ServerPlayer player,Profession profession){
        return ConstructionService.store(player.server).projects().stream().anyMatch(p->p.phase()==ConstructionStore.Phase.COMPLETE&&!p.npcDead()&&p.blueprint().toString().equals(profession.buildingId())&&ConstructionService.structureIntact(player.serverLevel(),p));
    }
    private static void requireBuilding(ServerPlayer p,Profession profession){if(!available(p,profession))throw new IllegalStateException("Complete and maintain the "+name(profession)+" profession building first.");}
    public static Quote review(ServerPlayer p,String selected){
        CommerceService.requireResidentCounter(p);var s=CommerceService.store(p.server);var parts=selected.split(":",-1);String action,from,to;
        if(parts.length==2&&parts[0].equals("learn")){action="enroll";from="";to=parts[1];}
        else if(parts.length==3&&parts[0].equals("switch")){action="switch";from=parts[1];to=parts[2];}
        else throw new IllegalArgumentException("Choose an available profession enrollment or replacement.");
        var target=Profession.byId(to);requireBuilding(p,target);var roster=s.professions(p.getUUID());var transition=action.equals("enroll")?roster.enroll(target):roster.switchProfession(Profession.byId(from),target);
        if(!transition.changed())throw new IllegalStateException("Profession already active; no payment required.");
        if(s.balance(p.getUUID())<transition.fee())throw new IllegalStateException("Profession changes require15000coins. Your progress is preserved.");
        String text=(action.equals("switch")?"Replace active "+name(Profession.byId(from))+" with ":"Activate ")+name(target)+" for exactly "+transition.fee()+" coins. Maximum three active professions. Inactive XP and completed quest records are retained, but their perks and buying rights stop. Returning to a learned profession is a paid change. Wallet and roster commit together. Quest/rank progression is unavailable until its approved content is enabled.";
        var q=new Quote(UUID.randomUUID(),s.revision(),action,from,to,transition.fee(),System.nanoTime(),text);QUOTES.put(p,q);return q;
    }
    public static String confirm(ServerPlayer p,UUID nonce)throws IOException{
        CommerceService.requireResidentCounter(p);var q=QUOTES.remove(p);var s=CommerceService.store(p.server);CommerceService.verifyReceipt(p,s);
        if(q==null||!q.nonce().equals(nonce)||q.revision()!=s.revision()||System.nanoTime()-q.issued()>30_000_000_000L)throw new IllegalStateException("Profession review expired or wallet/roster changed. Review again.");
        requireBuilding(p,Profession.byId(q.to()));s.changeProfession(q.nonce(),p.getUUID(),q.action(),q.from(),q.to());return name(Profession.byId(q.to()))+" is active. Your roster and exact fee were saved together.";
    }
    public static List<CommerceView.Entry> entries(ServerPlayer p){
        var s=CommerceService.store(p.server);var roster=s.professions(p.getUUID());List<CommerceView.Entry> result=new ArrayList<>();
        for(var profession:Profession.values()){
            boolean active=roster.isActive(profession),eligible=available(p,profession);long fee=0;
            if(!active&&roster.active().size()<3)fee=roster.enroll(profession).fee();
            result.add(new CommerceView.Entry("learn:"+profession.id(),name(profession),active?"Active "+roster.rank(profession,QUESTS)+". XP "+roster.track(profession).xp()+"; saved quests "+roster.track(profession).completedQuestIds().size()+"/24. Only active professions grant perks. Approved quest content is not enabled yet.":!eligible?"Requires its completed, intact profession building. Later unimplemented premises grant no enrollment.":roster.active().size()>=3?"Three active professions. Select a replacement action below; cost15000coins, inactive progress retained.":"Review enrollment; first three learned slots are free. Later changes/reactivation cost15000coins. Rank quests await approved content.",fee,active?"PROF_ACTIVE":eligible&&roster.active().size()<3?"PROF_ENROLL":"LOCKED"));
        }
        for(var from:Profession.values())if(roster.isActive(from))for(var to:Profession.values())if(!roster.isActive(to)&&available(p,to))result.add(new CommerceView.Entry("switch:"+from.id()+":"+to.id(),name(from)+" → "+name(to),"Replace this active profession for15000coins. Keep inactive XP/quests; disable its perks. Exact wallet and roster change commit once together.",15000,"PROF_SWITCH"));
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
