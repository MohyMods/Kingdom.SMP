package com.mohymods.kingdomsmp.profession;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
/** Only confirmed native natural deaths enter this producer; client counts never cross the boundary. */
public final class EncounterQuests {
    public static final String QUEST="kingdom_smp:adventurer/a03";
    private static final ProfessionQuestCatalog CATALOG=ProfessionQuestCatalog.approved(List.of(ApprovedProfessionContent.get(QUEST).slot()));
    private EncounterQuests(){}
    public static ProfessionQuestCatalog catalog(){return CATALOG;}
    public static boolean eligible(ServerPlayer p){var r=CommerceService.store(p.server).professions(p.getUUID());return TreeQuestService.real(p)&&r.isActive(Profession.ADVENTURER)&&!r.track(Profession.ADVENTURER).completedQuestIds().contains(QUEST)&&CATALOG.mayAttempt(Profession.ADVENTURER,QUEST,r.track(Profession.ADVENTURER).completedQuestIds())&&ContributionService.kingdom(p.server).attainedTier()>=3&&ProfessionService.available(p,Profession.ADVENTURER);}
    public static void record(ServerPlayer p,UUID victim,String type){if(!eligible(p))return;var data=EncounterSavedData.get(p.server);var rules=EncounterRules.get(p.server);if(data.evidence().ready(p.getUUID(),rules.total(),rules.perSpecies())||data.evidence().counts(p.getUUID()).getOrDefault(type,0)>=rules.speciesLimit())return;if(data.evidence().record(victim,p.getUUID(),type))data.setDirty();}
    public static boolean ready(ServerPlayer p){var rules=EncounterRules.get(p.server);return eligible(p)&&EncounterSavedData.get(p.server).evidence().ready(p.getUUID(),rules.total(),rules.perSpecies());}
    public static String status(ServerPlayer p){var r=EncounterRules.get(p.server);return EncounterSavedData.get(p.server).evidence().counts(p.getUUID())+". Need "+r.total()+" total and "+r.perSpecies()+" each zombie, skeleton, spider and creeper. Personally defeat authenticated natural Overworld spawns while eligible. Eggs, spawners, unknown historical origins, pets, machines and canceled deaths do not count.";}
    public static void complete(ServerPlayer p,UUID operation)throws java.io.IOException{ContractService.requireCounter(p,Profession.ADVENTURER);if(!ready(p))throw new IllegalStateException(status(p));var rules=EncounterRules.get(p.server);CommerceService.store(p.server).completeEncounterQuest(operation,p.getUUID(),rules.total(),rules.perSpecies(),EncounterSavedData.get(p.server).evidence().counts(p.getUUID()));}
}
