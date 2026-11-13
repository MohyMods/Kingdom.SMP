package com.mohymods.kingdomsmp.profession;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;

class ApprovedProfessionContentTest {
    String bundled()throws Exception{
        try(var input=getClass().getResourceAsStream("/data/kingdom_smp/professions/approved_remaining_quests.json")){
            assertNotNull(input);return new String(input.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    @Test void allElevenApprovedProfessionsHaveExactBandsAndRewardsWithoutActivatingUnvalidatedContent(){
        var rows=ApprovedProfessionContent.definitions();assertEquals(264,rows.size());
        for(var profession:Profession.values())if(profession!=Profession.FARMER){
            long xp=0;
            for(var rank:ProfessionRank.values())for(int n=1;n<=8;n++){
                var d=rows.get(profession.questId(rank,n));assertNotNull(d);
                assertEquals(profession,d.profession());assertEquals(rank,d.rank());
                assertEquals((rank.ordinal()+1)*100L,d.xp());xp+=d.xp();
            }
            assertEquals(4800,xp);
        }
        assertEquals(304,rows.values().stream().mapToInt(d->d.prerequisites().size()).sum());
        assertEquals(24,FarmerQuests.catalog().definitions().size());
        assertEquals(146,ProfessionQuestService.catalog().definitions().size());
        assertTrue(ProfessionQuestService.catalog().definition("kingdom_smp:mechanic/m08").isEmpty());
        assertTrue(ProfessionQuestService.catalog().definition("kingdom_smp:builder/a01").isPresent());
    }
    @Test void changedBaselineMissingSlotAndUnknownValidatorFailClosed()throws Exception{
        String original=bundled();
        assertThrows(IllegalArgumentException.class,()->ApprovedProfessionContent.parse(original.replace(ApprovedProfessionContent.BASELINE,"unapproved")));
        var json=JsonParser.parseString(original).getAsJsonObject();json.getAsJsonArray("quests").remove(0);
        assertThrows(IllegalArgumentException.class,()->ApprovedProfessionContent.parse(json.toString()));
        assertThrows(IllegalArgumentException.class,()->ApprovedProfessionContent.parse(original.replace("\"NAT\"","\"CLIENT_PACKET\"")));
    }
    @Test void duplicateForwardForeignAndMissingDependenciesAreRefused()throws Exception{
        String original=bundled();
        for(String prior:List.of("kingdom_smp:blacksmith/a08","kingdom_smp:builder/a01","kingdom_smp:blacksmith/a09")){
            var json=JsonParser.parseString(original).getAsJsonObject();var array=new JsonArray();array.add(prior);
            json.getAsJsonArray("quests").get(0).getAsJsonObject().add("prerequisites",array);
            assertThrows(IllegalArgumentException.class,()->ApprovedProfessionContent.parse(json.toString()));
        }
        var json=JsonParser.parseString(original).getAsJsonObject();json.getAsJsonArray("quests").set(1,json.getAsJsonArray("quests").get(0).deepCopy());
        assertThrows(IllegalArgumentException.class,()->ApprovedProfessionContent.parse(json.toString()));
    }
 @org.junit.jupiter.api.Test void unavailableProfessionPacketsNeverFallThroughIntoFarmerAuthority(){
  for(String id:java.util.List.of("kingdom_smp:blacksmith/m05","kingdom_smp:librarian/m08","client:invented")){org.junit.jupiter.api.Assertions.assertFalse(ProfessionQuestService.ready(null,id));org.junit.jupiter.api.Assertions.assertTrue(ProfessionQuestService.status(null,id).contains("no validated runtime"));org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,()->ProfessionQuestService.complete(null,java.util.UUID.randomUUID(),id));}
 }
 @org.junit.jupiter.api.Test void fullApproved288MetadataFitsPlayerMenuBudgetForEveryThreeProfessionCombination(){
  var definitions=new java.util.ArrayList<>(FarmerQuests.catalog().definitions().values());definitions.addAll(ApprovedProfessionContent.definitions().values().stream().map(ApprovedProfessionContent.Definition::slot).toList());var all=ProfessionQuestCatalog.approved(definitions);var roles=Profession.values();for(int a=0;a<roles.length;a++)for(int b=a+1;b<roles.length;b++)for(int c=b+1;c<roles.length;c++){var active=java.util.Set.of(roles[a],roles[b],roles[c]);var visible=all.visibleFor(active);org.junit.jupiter.api.Assertions.assertEquals(72,visible.size());org.junit.jupiter.api.Assertions.assertTrue(visible.stream().allMatch(d->active.contains(d.profession())));org.junit.jupiter.api.Assertions.assertTrue(visible.size()+12+27+2<=128,"Objectives, all roster rows, every switch and current gear fit unchanged packet budget");}org.junit.jupiter.api.Assertions.assertEquals(288,all.definitions().size());org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,()->all.visibleFor(java.util.Set.of(roles[0],roles[1],roles[2],roles[3])));
 }
}
