package com.mohymods.kingdomsmp.profession;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommissionQuestRulesTest {
    @TempDir Path directory;
    @Test void approvedDefaultAndConfiguredGoalsPersistWithoutOverwriting()throws Exception{
        var path=directory.resolve("goals.json");var rules=CommissionQuestRules.load(path);
        assertEquals(3,rules.goal("kingdom_smp:builder/a07"));assertEquals(3,rules.goal("kingdom_smp:lumberjack/a07"));
        String custom=Files.readString(path).replace(": 3",": 5");Files.writeString(path,custom);
        assertEquals(5,CommissionQuestRules.load(path).goal("kingdom_smp:builder/a07"));assertEquals(custom,Files.readString(path));
    }
    @Test void missingUnknownFractionalNegativeAndUnboundedGoalsFailClosed()throws Exception{
        var path=directory.resolve("goals.json");CommissionQuestRules.load(path);String defaults=Files.readString(path);
        for(String value:new String[]{"0","366","1.5","-1","\"3\""}){
            Files.writeString(path,defaults.replace(": 3",": "+value));assertThrows(IOException.class,()->CommissionQuestRules.load(path));
        }
        Files.writeString(path,defaults.replace("builder/a07","miner/a07"));assertThrows(IOException.class,()->CommissionQuestRules.load(path));
    }
}
