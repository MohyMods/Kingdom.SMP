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
        assertEquals(44,ProfessionQuestService.catalog().definitions().size());
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
}
