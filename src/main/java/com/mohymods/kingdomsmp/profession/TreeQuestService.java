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
        "kingdom_smp:lumberjack/a04",new Definition("kingdom_smp:lumberjack/a04","grown",24));
    private static final ProfessionQuestCatalog CATALOG=ProfessionQuestCatalog.approved(DEFINITIONS.keySet().stream().map(id->{var d=ApprovedProfessionContent.get(id);if(d.minimumTier()!=2||!d.validation().equals(Set.of(id.endsWith("a04")?ApprovedProfessionContent.Validation.GROW:ApprovedProfessionContent.Validation.TREE)))throw new IllegalStateException("Unvalidated tree content");return d.slot();}).toList());
    private TreeQuestService() { }
    public static ProfessionQuestCatalog catalog(){return CATALOG;}
    public static boolean contains(String id){return DEFINITIONS.containsKey(id);}
    public static Definition get(String id){var d=DEFINITIONS.get(id);if(d==null)throw new IllegalArgumentException("No validated native tree objective");return d;}
    public static boolean real(ServerPlayer p){return !(p instanceof FakePlayer)&&p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&p.server.getPlayerList().getPlayer(p.getUUID())==p;}
    public static int count(ServerPlayer p,String id){var d=get(id);return TreeSavedData.get(p.server).evidence().evidence().count(p.getUUID(),ApprovedProfessionContent.get(id).rank(),d.metric());}
    public static boolean ready(ServerPlayer p,String id){try{var d=ApprovedProfessionContent.get(id);var roster=CommerceService.store(p.server).professions(p.getUUID());return real(p)&&roster.isActive(Profession.LUMBERJACK)&&!roster.track(Profession.LUMBERJACK).completedQuestIds().contains(id)&&CATALOG.mayAttempt(Profession.LUMBERJACK,id,roster.track(Profession.LUMBERJACK).completedQuestIds())&&ContributionService.kingdom(p.server).attainedTier()>=d.minimumTier()&&ProfessionService.available(p,Profession.LUMBERJACK)&&count(p,id)>=TreeQuestRules.get(p.server).goal(id);}catch(IllegalStateException unavailable){return false;}}
    public static String status(ServerPlayer p,String id){
        var roster=CommerceService.store(p.server).professions(p.getUUID());if(roster.track(Profession.LUMBERJACK).completedQuestIds().contains(id))return "Complete. Personal native tree proof and XP recorded.";
        if(!roster.isActive(Profession.LUMBERJACK))return "Activate Lumberjack at its completed premises before earning personal tree progress.";
        var d=get(id);String hint=d.metric().equals("grown")?"Grow recorded replacements near your tracked clearings. Offline growth is retained; visit loaded plots to verify consent.":"Harvest native tracked trees or trees grown from real planted saplings. Untracked old trees still drop items; placed logs grant no credit.";
        try{return count(p,id)+" / "+TreeQuestRules.get(p.server).goal(id)+". "+hint;}catch(IllegalStateException unavailable){return "Tree evidence unavailable; an operator must restore the matching checkpoint. Existing progress is not reset.";}
    }
    public static void complete(ServerPlayer p,UUID operation,String id)throws IOException{
        ContractService.requireCounter(p,Profession.LUMBERJACK);TreeLifecycle.auditGrowth(p.server);if(!ready(p,id))throw new IllegalStateException(status(p,id));
        CommerceService.store(p.server).completeTreeQuest(operation,p.getUUID(),id,TreeQuestRules.get(p.server).goal(id),count(p,id));
    }
}
