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
        "kingdom_smp:lumberjack/j02",new Definition("kingdom_smp:lumberjack/j02","jungle",96),
        "kingdom_smp:lumberjack/j03",new Definition("kingdom_smp:lumberjack/j03","mature_cherry",16),
        "kingdom_smp:lumberjack/j04",new Definition("kingdom_smp:lumberjack/j04","mature_mangrove",12),
        "kingdom_smp:lumberjack/m01",new Definition("kingdom_smp:lumberjack/m01","mosaic",48),
        "kingdom_smp:lumberjack/m05",new Definition("kingdom_smp:lumberjack/m05","renewal_cycles",24));
    private static final ProfessionQuestCatalog CATALOG;
    static {
        Set<String> closure=new HashSet<>(DEFINITIONS.keySet());
        for(var id:DEFINITIONS.keySet()){var d=ApprovedProfessionContent.get(id);if(d.minimumTier()!=(d.rank()==ProfessionRank.APPRENTICE?2:d.rank()==ProfessionRank.JOURNEYMAN?3:6)||!d.validation().equals(id.endsWith("j03")||id.endsWith("j04")||d.rank()==ProfessionRank.MASTER?Set.of(ApprovedProfessionContent.Validation.GROW,ApprovedProfessionContent.Validation.TREE):Set.of(id.endsWith("a04")?ApprovedProfessionContent.Validation.GROW:ApprovedProfessionContent.Validation.TREE)))throw new IllegalStateException("Unvalidated native tree content");for(var rank:ProfessionRank.values())if(rank.ordinal()<d.rank().ordinal())for(int n=1;n<=8;n++)closure.add(d.profession().questId(rank,n));}
        var queue=new ArrayDeque<>(closure);while(!queue.isEmpty())for(var prior:ApprovedProfessionContent.get(queue.removeFirst()).prerequisites())if(closure.add(prior))queue.addLast(prior);
        CATALOG=ProfessionQuestCatalog.approved(closure.stream().map(id->ApprovedProfessionContent.get(id).slot()).toList());
    }
    public static Set<String> ids(){return DEFINITIONS.keySet();}
    static int hardwood(int acacia,int darkOak,int goal){if(goal<2)throw new IllegalArgumentException("Both hardwood species required");return Math.min(acacia,goal/2)+Math.min(darkOak,goal-goal/2);}
    /** Six species must each satisfy their own quota. Excess oak never replaces a missing species. */
    static int mosaic(Collection<Integer> counts,int goal){if(goal<6||goal>100000||goal%6!=0||counts.size()!=TreeEvidence.SPECIES.size()||counts.stream().anyMatch(n->n<0))throw new IllegalArgumentException("Six equal species quotas required");return counts.stream().sorted(Comparator.reverseOrder()).limit(6).mapToInt(n->Math.min(n,goal/6)).sum();}
    private TreeQuestService() { }
    public static ProfessionQuestCatalog catalog(){return CATALOG;}
    public static boolean contains(String id){return DEFINITIONS.containsKey(id);}
    public static Definition get(String id){var d=DEFINITIONS.get(id);if(d==null)throw new IllegalArgumentException("No validated native tree objective");return d;}
    public static boolean real(ServerPlayer p){return !(p instanceof FakePlayer)&&p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&p.server.getPlayerList().getPlayer(p.getUUID())==p;}
    public static int count(ServerPlayer p,String id){var d=get(id);var evidence=TreeSavedData.get(p.server).evidence().evidence();var band=ApprovedProfessionContent.get(id).rank();if(d.metric().equals("mosaic"))return mosaic(TreeEvidence.SPECIES.stream().map(species->evidence.count(p.getUUID(),band,"mature_"+species)).toList(),TreeQuestRules.get(p.server).goal(id));return d.metric().equals("hardwood")?hardwood(evidence.count(p.getUUID(),band,"acacia"),evidence.count(p.getUUID(),band,"dark_oak"),TreeQuestRules.get(p.server).goal(id)):evidence.count(p.getUUID(),band,d.metric());}
    public static boolean ready(ServerPlayer p,String id){try{var d=ApprovedProfessionContent.get(id);var roster=CommerceService.store(p.server).professions(p.getUUID());return real(p)&&roster.isActive(Profession.LUMBERJACK)&&!roster.track(Profession.LUMBERJACK).completedQuestIds().contains(id)&&CATALOG.mayAttempt(Profession.LUMBERJACK,id,roster.track(Profession.LUMBERJACK).completedQuestIds())&&ContributionService.kingdom(p.server).attainedTier()>=d.minimumTier()&&ProfessionService.available(p,Profession.LUMBERJACK)&&count(p,id)>=TreeQuestRules.get(p.server).goal(id);}catch(IllegalStateException unavailable){return false;}}
    public static String status(ServerPlayer p,String id){
        var roster=CommerceService.store(p.server).professions(p.getUUID());if(roster.track(Profession.LUMBERJACK).completedQuestIds().contains(id))return "Complete. Personal native tree proof and XP recorded.";
        if(!roster.isActive(Profession.LUMBERJACK))return "Activate Lumberjack at its completed premises before earning personal tree progress.";
        var d=get(id);if(d.metric().equals("mosaic")){int goal=TreeQuestRules.get(p.server).goal(id);return count(p,id)+" / "+goal+" mature harvests: grow and personally harvest "+goal/6+" trees in each of6different native species during Master trials. Extra trees of one species cannot replace another.";}if(d.metric().equals("renewal_cycles"))return count(p,id)+" / "+TreeQuestRules.get(p.server).goal(id)+" native replacement cycles: fully harvest a tracked tree, plant in its own recorded clearing, grow and harvest the replacement in this band. Unknown legacy cycles do not count.";String hint=d.metric().startsWith("mature_")?"Grow a native tree from your eligible planted sapling, then personally harvest it once. Unknown legacy ownership cannot provide this proof.":d.metric().equals("grown")?"Grow recorded replacements near your tracked clearings. Offline growth is retained; visit loaded plots to verify consent.":"Harvest native tracked trees or trees grown from real planted saplings. Untracked old trees still drop items; placed logs grant no credit.";
        try{if(d.metric().equals("hardwood")){int goal=TreeQuestRules.get(p.server).goal(id);var evidence=TreeSavedData.get(p.server).evidence().evidence();return evidence.count(p.getUUID(),ProfessionRank.JOURNEYMAN,"acacia")+" / "+goal/2+" native acacia; "+evidence.count(p.getUUID(),ProfessionRank.JOURNEYMAN,"dark_oak")+" / "+(goal-goal/2)+" native dark oak. Each species is required; excess timber never substitutes.";}return count(p,id)+" / "+TreeQuestRules.get(p.server).goal(id)+". "+hint;}catch(IllegalStateException unavailable){return "Tree evidence unavailable; an operator must restore the matching checkpoint. Existing progress is not reset.";}
    }
    public static void complete(ServerPlayer p,UUID operation,String id)throws IOException{
        ContractService.requireCounter(p,Profession.LUMBERJACK);TreeLifecycle.auditGrowth(p.server);if(!ready(p,id))throw new IllegalStateException(status(p,id));
        CommerceService.store(p.server).completeTreeQuest(operation,p.getUUID(),id,TreeQuestRules.get(p.server).goal(id),count(p,id));
    }
}
