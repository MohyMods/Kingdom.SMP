package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class RetainedBuildingRulesTest {
    @TempDir Path directory;
    @Test void approvedGoalsAndExplicitOverridesArePreserved()throws Exception{
        var file=directory.resolve("goals.json");var rules=RetainedBuildingRules.load(file);assertEquals(128,rules.goal("kingdom_smp:builder/a01"));assertEquals(64,rules.goal("kingdom_smp:builder/a02"));assertEquals(64,rules.goal("kingdom_smp:builder/a03"));assertEquals(48,rules.goal("kingdom_smp:builder/a06"));
        String changed=Files.readString(file).replace(": 128",": 96");Files.writeString(file,changed);assertEquals(96,RetainedBuildingRules.load(file).goal("kingdom_smp:builder/a01"));assertEquals(changed,Files.readString(file));
    }
    @Test void malformedOrUnknownGoalsDoNotSilentlyReset()throws Exception{
        var file=directory.resolve("goals.json");RetainedBuildingRules.load(file);String original=Files.readString(file);
        for(String n:new String[]{"0","-1","2049","1.5","\"128\""}){Files.writeString(file,original.replace(": 128",": "+n));assertThrows(IOException.class,()->RetainedBuildingRules.load(file));}
        Files.writeString(file,original.replace("builder/a01","builder/m01"));assertThrows(IOException.class,()->RetainedBuildingRules.load(file));
    }
}
