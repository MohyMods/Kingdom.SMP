package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class TreeQuestRulesTest {
    @TempDir Path dir;
    @Test void excessOfEitherSpeciesCannotSubstituteForTheOther(){assertEquals(64,TreeQuestService.hardwood(1000,0,128));assertEquals(64,TreeQuestService.hardwood(0,1000,128));assertEquals(128,TreeQuestService.hardwood(64,64,128));assertEquals(127,TreeQuestService.hardwood(63,1000,128));assertThrows(IllegalArgumentException.class,()->TreeQuestService.hardwood(10,10,1));}
    @Test void legacyFourEntryGoalsRemainByteForByteAndNewDefaultsAreAdditive()throws Exception{
        var file=dir.resolve("trees.json");String legacy="{\"schema\":1,\"goals\":{\"kingdom_smp:lumberjack/a01\":128,\"kingdom_smp:lumberjack/a02\":64,\"kingdom_smp:lumberjack/a03\":64,\"kingdom_smp:lumberjack/a04\":24}}";Files.writeString(file,legacy);var rules=TreeQuestRules.load(file);assertEquals(128,rules.goal("kingdom_smp:lumberjack/j01"));assertEquals(96,rules.goal("kingdom_smp:lumberjack/j02"));assertEquals(legacy,Files.readString(file));
        Files.writeString(file,legacy.replace("lumberjack/a01","lumberjack/m01"));assertThrows(IOException.class,()->TreeQuestRules.load(file));
    }
}
