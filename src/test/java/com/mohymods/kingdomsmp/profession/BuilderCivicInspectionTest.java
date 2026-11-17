package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class BuilderCivicInspectionTest {
 @Test void threeServicesGiveTwoConnectionsWithoutTriangleOrDuplicateEndpointInflation(){assertEquals(2,BuilderCivicInspection.connections(List.of(Set.of("a","b","c"))));assertEquals(1,BuilderCivicInspection.connections(List.of(Set.of("a","b"),Set.of("a","b"))));assertEquals(0,BuilderCivicInspection.connections(List.of(Set.of("a"))));assertEquals(2,BuilderCivicInspection.connections(List.of(Set.of("a","b"),Set.of("b","c"))));}
 @Test void allBuilderObjectivesAreImplementedWhileMasterAssessmentRequiresEveryEarnedBand(){var definitions=ProfessionQuestService.catalog().definitions();for(var rank:ProfessionRank.values())for(int n=1;n<=8;n++)assertTrue(definitions.containsKey(Profession.BUILDER.questId(rank,n)));assertFalse(InspectionQuests.catalog().mayAttempt(Profession.BUILDER,BuilderCivicInspection.FINAL,Set.of()));var all=new HashSet<String>();for(var rank:ProfessionRank.values())for(int n=1;n<=8;n++)if(rank!=ProfessionRank.MASTER||n<8)all.add(Profession.BUILDER.questId(rank,n));assertTrue(InspectionQuests.catalog().mayAttempt(Profession.BUILDER,BuilderCivicInspection.FINAL,all));}
}
