package com.mohymods.kingdomsmp.profession;

import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.economy.CommerceService;
import java.io.IOException;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Server-owned native tree proofs, with explicit single-award reviewed profession completion. */
public final class TreeQuestService {
    public record Definition(String id,String metric,int goal) { }
    private static final Map<String,Definition> DEFINITIONS=Map.of(
        "kingdom_smp:lumberjack/a01",new Definition("kingdom_smp:lumberjack/a01","oak",128),
        "kingdom_smp:lumberjack/a02",new Definition("kingdom_smp:lumberjack/a02","birch",64),
        "kingdom_smp:lumberjack/a03",new Definition("kingdom_smp:lumberjack/a03","spruce",64),
        "kingdom_smp:lumberjack/a04",new Definition("kingdom_smp:lumberjack/a04","grown",24),
        "kingdom_smp:lumberjack/j01",new Definition("kingdom_smp:lumberjack/j01","hardwood",128),
        "kingdom_smp:lumberjack/j02",new Definition("kingdom_smp:lumberjack/j02","jungle",96));
    private static final ProfessionQuestCatalog CATALOG;
    static {
        Set<String> closure=new HashSet<>(DEFINITIONS.keySet());
        for(var id:DEFINITIONS.keySet()){var d=ApprovedProfessionContent.get(id);if(d.minimumTier()!=(d.rank()==ProfessionRank.APPRENTICE?2:3)||!d.validation().equals(Set.of(id.endsWith("a04")?ApprovedProfessionContent.Validation.GROW:ApprovedProfessionContent.Validation.TREE)))throw new IllegalStateException("Unvalidated native tree content");for(var rank:ProfessionRank.values())if(rank.ordinal()<d.rank().ordinal())for(int n=1;n<=8;n++)closure.add(d.profession().questId(rank,n));}
        var queue=new ArrayDeque<>(closure);while(!queue.isEmpty())for(var prior:ApprovedProfessionContent.get(queue.removeFirst()).prerequisites())if(closure.add(prior))queue.addLast(prior);
        CATALOG=ProfessionQuestCatalog.approved(closure.stream().map(id->ApprovedProfessionContent.get(id).slot()).toList());
    }
    public static Set<String> ids(){return DEFINITIONS.keySet();}
    static int hardwood(int acacia,int darkOak,int goal){if(goal<2)throw new IllegalArgumentException("Both hardwood species required");return Math.min(acacia,goal/2)+Math.min(darkOak,goal-goal/2);}
    private TreeQuestService() { }
    public static ProfessionQuestCatalog catalog(){return CATALOG;}
    public static boolean contains(String id){return DEFINITIONS.containsKey(id);}
    public static Definition get(String id){var d=DEFINITIONS.get(id);if(d==null)throw new IllegalArgumentException("No validated native tree objective");return d;}
    public static boolean real(ServerPlayer p){return !(p instanceof FakePlayer)&&p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&p.server.getPlayerList().getPlayer(p.getUUID())==p;}
    public static int count(ServerPlayer p,String id){var d=get(id);var evidence=TreeSavedData.get(p.server).evidence().evidence();var band=ApprovedProfessionContent.get(id).rank();return d.metric().equals("hardwood")?hardwood(evidence.count(p.getUUID(),band,"acacia"),evidence.count(p.getUUID(),band,"dark_oak"),TreeQuestRules.get(p.server).goal(id)):evidence.count(p.getUUID(),band,d.metric());}
    public static boolean ready(ServerPlayer p,String id){try{var d=ApprovedProfessionContent.get(id);var roster=CommerceService.store(p.server).professions(p.getUUID());return real(p)&&roster.isActive(Profession.LUMBERJACK)&&!roster.track(Profession.LUMBERJACK).completedQuestIds().contains(id)&&CATALOG.mayAttempt(Profession.LUMBERJACK,id,roster.track(Profession.LUMBERJACK).completedQuestIds())&&ContributionService.kingdom(p.server).attainedTier()>=d.minimumTier()&&ProfessionService.available(p,Profession.LUMBERJACK)&&count(p,id)>=TreeQuestRules.get(p.server).goal(id);}catch(IllegalStateException unavailable){return false;}}
    public static String status(ServerPlayer p,String id){
        var roster=CommerceService.store(p.server).professions(p.getUUID());if(roster.track(Profession.LUMBERJACK).completedQuestIds().contains(id))return "Complete. Personal native tree proof and XP recorded.";
        if(!roster.isActive(Profession.LUMBERJACK))return "Activate Lumberjack at its completed premises before earning personal tree progress.";
        var d=get(id);String hint=d.metric().equals("grown")?"Grow recorded replacements near your tracked clearings. Offline growth is retained; visit loaded plots to verify consent.":"Harvest native tracked trees or trees grown from real planted saplings. Untracked old trees still drop items; placed logs grant no credit.";
        try{if(d.metric().equals("hardwood")){int goal=TreeQuestRules.get(p.server).goal(id);var evidence=TreeSavedData.get(p.server).evidence().evidence();return evidence.count(p.getUUID(),ProfessionRank.JOURNEYMAN,"acacia")+" / "+goal/2+" native acacia; "+evidence.count(p.getUUID(),ProfessionRank.JOURNEYMAN,"dark_oak")+" / "+(goal-goal/2)+" native dark oak. Each species is required; excess timber never substitutes.";}return count(p,id)+" / "+TreeQuestRules.get(p.server).goal(id)+". "+hint;}catch(IllegalStateException unavailable){return "Tree evidence unavailable; an operator must restore the matching checkpoint. Existing progress is not reset.";}
    }
    public static void complete(ServerPlayer p,UUID operation,String id)throws IOException{
        ContractService.requireCounter(p,Profession.LUMBERJACK);TreeLifecycle.auditGrowth(p.server);if(!ready(p,id))throw new IllegalStateException(status(p,id));
        CommerceService.store(p.server).completeTreeQuest(operation,p.getUUID(),id,TreeQuestRules.get(p.server).goal(id),count(p,id));
    }
}
