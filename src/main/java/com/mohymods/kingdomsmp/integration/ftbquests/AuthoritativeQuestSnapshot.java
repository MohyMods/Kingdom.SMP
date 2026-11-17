package com.mohymods.kingdomsmp.integration.ftbquests;

import com.mohymods.kingdomsmp.building.ConstructionService;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.persistence.ConstructionStore;
import net.minecraft.server.MinecraftServer;
import java.util.*;

/** No initialization, awards, materialization or FTB state is used to establish Kingdom facts. */
public final class AuthoritativeQuestSnapshot {
    private AuthoritativeQuestSnapshot(){ }
    public static Optional<KingdomQuestSnapshot> read(MinecraftServer server){
        if(!server.isSameThread())return Optional.empty();
        var construction=ConstructionService.initializedStore(server).orElse(null);var contributions=ContributionService.initializedLedger(server).orElse(null);var economy=CommerceService.initializedStore(server).orElse(null);
        if(construction==null||contributions==null||economy==null||construction.quarantined()||contributions.quarantined()||economy.quarantined())return Optional.empty();
        int tier=Math.max(com.mohymods.kingdomsmp.persistence.KingdomSavedData.get(server).snapshot().attainedTier(),contributions.progress().complete()?2:1);
        if(com.mohymods.kingdomsmp.progression.TierTwoService.initializedLedger(server).map(s->s.complete()).orElse(false))tier=Math.max(tier,3);
        Set<String> complete=new HashSet<>(),purchased=new HashSet<>();
        for(var project:construction.projects())if(project.phase()==ConstructionStore.Phase.COMPLETE&&!project.npcDead())complete.add(project.blueprint().toString());
        for(var purchase:economy.purchases())purchased.add(purchase.building());
        return Optional.of(new KingdomQuestSnapshot(tier,complete,purchased,!contributions.progress().contributors().isEmpty(),economy.taxRemoved()>0));
    }
}
