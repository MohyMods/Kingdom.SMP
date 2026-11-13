package com.mohymods.kingdomsmp.bounty;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class CommunityContractRulesTest {
 @TempDir Path dir;
 @Test void defaultsFundEveryGoalAndBadPoolFailsBeforeItCanDeadlockMasterProgress()throws Exception{var path=dir.resolve("contracts.json");var rules=CommunityContractRules.load(path);assertEquals(3,rules.all().size());assertTrue(rules.all().stream().mapToLong(t->(long)t.goal()*t.unitReward()).sum()<=4000);Files.writeString(path,Files.readString(path).replace("4000","100"));assertThrows(java.io.IOException.class,()->CommunityContractRules.load(path));}
}
