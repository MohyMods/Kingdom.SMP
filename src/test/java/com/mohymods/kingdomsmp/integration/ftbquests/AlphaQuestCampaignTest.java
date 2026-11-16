package com.mohymods.kingdomsmp.integration.ftbquests;

import com.mohymods.kingdomsmp.profession.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AlphaQuestCampaignTest {
    @TempDir Path directory;
    @Test void all144AreBackedByValidatedMetadataWithStableUniqueAcyclicIds(){
        var goals=AlphaQuestCampaign.goals();assertEquals(144,goals.stream().filter(g->g.kind()==AlphaQuestCampaign.Kind.PROFESSION).count());
        Set<Long> ids=new HashSet<>();Map<Long,AlphaQuestCampaign.Goal> quests=new HashMap<>();for(var g:goals){assertTrue(ids.add(g.taskId()));assertTrue(ids.add(g.questId()));assertEquals(g,AlphaQuestCampaign.parse(g.tag()).orElseThrow());quests.put(g.questId(),g);if(g.kind()==AlphaQuestCampaign.Kind.PROFESSION){assertTrue(ProfessionQuestService.implemented(g.fact()));assertTrue(g.tier()<=6);assertFalse(g.description().isEmpty());}}
        Set<Long> resolved=new HashSet<>();for(var g:KingdomQuestGoal.values())resolved.add(g.questId());int previous=-1;while(previous!=resolved.size()){previous=resolved.size();for(var g:goals)if(resolved.containsAll(g.prerequisites()))resolved.add(g.questId());}assertTrue(resolved.containsAll(quests.keySet()),"Unknown or circular guide dependency");
        assertTrue(AlphaQuestCampaign.parse(null).isEmpty());assertTrue(AlphaQuestCampaign.parse("kingdom_smp:alpha_guide/v2/farmer/a01").isEmpty());
    }
    @Test void readOnlyPredicatesNeedRealTierAndIndividualSourceAndNeverUnlockFuture(){
        var low=new KingdomQuestSnapshot(1,Set.of("kingdom_smp:royal_aerodrome"),Set.of(),false,false);var six=new KingdomQuestSnapshot(6,low.completedBuildingIds(),Set.of(),true,true);
        var master=AlphaQuestCampaign.goals().stream().filter(g->g.fact().equals("kingdom_smp:librarian/m01")).findFirst().orElseThrow();
        assertFalse(master.complete(six,Set.of("kingdom_smp:librarian/a01")));assertFalse(master.complete(low,Set.of(master.fact())));assertTrue(master.complete(six,Set.of(master.fact())));
        var aero=AlphaQuestCampaign.goals().stream().filter(g->g.key().equals("aerodrome")).findFirst().orElseThrow();assertFalse(aero.complete(low,Set.of()));assertTrue(aero.complete(six,Set.of()));assertFalse(aero.complete(new KingdomQuestSnapshot(6,Set.of(),Set.of("kingdom_smp:royal_aerodrome"),true,true),Set.of()));
        var historical=new KingdomQuestSnapshot(12,Set.of(),Set.of(),true,true);for(var g:AlphaQuestCampaign.goals())if(g.kind()==AlphaQuestCampaign.Kind.FUTURE)assertFalse(g.complete(historical,Set.of(g.fact())));
    }
    @Test void additiveChaptersAreRealRewardlessTasksAndPreserveCustomizedFiles()throws Exception{
        var installed=AlphaQuestCampaign.install(directory);assertEquals(8,installed.size());assertTrue(installed.values().stream().allMatch(v->v==KingdomQuestCampaign.Result.CREATED));
        int total=0;for(var name:installed.keySet()){var file=directory.resolve("chapters").resolve(name);var root=TagParser.parseTag(Files.readString(file));var quests=root.getList("quests",Tag.TAG_COMPOUND);assertFalse(quests.isEmpty());for(var value:quests){var q=(CompoundTag)value;assertTrue(q.getList("rewards",Tag.TAG_COMPOUND).isEmpty());var tasks=q.getList("tasks",Tag.TAG_COMPOUND);assertEquals(1,tasks.size());var task=tasks.getCompound(0);assertEquals("custom",task.getString("type"));var g=AlphaQuestCampaign.parse(task.getList("tags",Tag.TAG_STRING).getString(0)).orElseThrow();assertEquals(String.format("%016X",g.questId()),q.getString("id"));assertEquals(String.format("%016X",g.taskId()),task.getString("id"));total++;}}assertEquals(AlphaQuestCampaign.goals().size(),total);
        var custom=directory.resolve("chapters/kingdom_smp_alpha_v1_2.snbt");Files.writeString(custom,"Owner customized chapter");assertEquals(KingdomQuestCampaign.Result.EXISTING_DIFFERENT,AlphaQuestCampaign.install(directory).get(custom.getFileName().toString()));assertEquals("Owner customized chapter",Files.readString(custom));assertFalse(Files.exists(directory.resolve("chapters").resolve(KingdomQuestCampaign.FILENAME)),"Original settlement installation is independent");
    }
    @Test void libraryAlphaDescriptionsNeverPresentFutureMaterialsAsTierSix(){
        var master=AlphaQuestCampaign.goals().stream().filter(g->g.kind()==AlphaQuestCampaign.Kind.PROFESSION&&g.fact().startsWith("kingdom_smp:librarian/m")).toList();assertEquals(8,master.size());assertTrue(master.stream().allMatch(g->g.tier()==6));assertTrue(master.stream().filter(g->g.fact().endsWith("m04")).findFirst().orElseThrow().description().getFirst().contains("Twilight"));
    }
}
