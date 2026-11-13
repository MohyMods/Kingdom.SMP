package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class BrewQuestRulesTest {
 @TempDir Path directory;
 @Test void goalsAreBoundedNativeInventoryQuantitiesAndNeverRewriteExistingFiles()throws Exception{var p=directory.resolve("brew.json");assertEquals(12,BrewQuestRules.load(p).get("kingdom_smp:alchemist/a02"));var text=Files.readString(p);for(var bad:new String[]{"0","37","1.5","\"12\""}){Files.writeString(p,text.replace(": 12",": "+bad));assertThrows(IOException.class,()->BrewQuestRules.load(p));}Files.writeString(p,text.replace(": 12",": 6"));assertEquals(6,BrewQuestRules.load(p).get("kingdom_smp:alchemist/a03"));assertEquals(text.replace(": 12",": 6"),Files.readString(p));}
 @Test void declaredRoutesBindActualOrdinaryNativeInputReagentAndOutput(){assertEquals(9,BrewQuests.ids().size());assertEquals("kingdom_smp:alchemist/a02",BrewQuests.route("minecraft:water","minecraft:nether_wart","minecraft:awkward").orElseThrow().quest());assertTrue(BrewQuests.route("minecraft:awkward","minecraft:sugar","minecraft:strength").isEmpty());assertTrue(BrewQuests.route("other:water","minecraft:nether_wart","minecraft:awkward").isEmpty());}
}
