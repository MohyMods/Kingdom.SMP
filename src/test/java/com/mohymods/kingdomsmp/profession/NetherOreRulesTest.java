package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class NetherOreRulesTest {
 @TempDir Path dir;
 @Test void approvedSeparateCostsAndExistingCustomBytesAreRetained()throws Exception{var path=dir.resolve("nether.json");var r=NetherOreRules.load(path);assertEquals(64,r.goal("kingdom_smp:miner/m01"));assertEquals(32,r.goal("kingdom_smp:miner/m02"));assertEquals(4,r.goal("kingdom_smp:miner/m03"));var root=JsonParser.parseString(Files.readString(path)).getAsJsonObject();assertFalse(root.has("copperAreaSeparationChunks"));root.getAsJsonObject("goals").addProperty("kingdom_smp:miner/m01",80);String text=root.toString();Files.writeString(path,text);assertEquals(80,NetherOreRules.load(path).goal("kingdom_smp:miner/m01"));assertEquals(text,Files.readString(path));}
 @Test void malformedCostsRefuseWithoutResettingFilesOrReadingOverworldGoals()throws Exception{var path=dir.resolve("nether.json");NetherOreRules.load(path);var root=JsonParser.parseString(Files.readString(path)).getAsJsonObject();root.getAsJsonObject("goals").addProperty("kingdom_smp:miner/m03",0);String bad=root.toString();Files.writeString(path,bad);assertThrows(java.io.IOException.class,()->NetherOreRules.load(path));assertEquals(bad,Files.readString(path));var old=NativeOreRules.load(dir.resolve("over.json"));assertThrows(IllegalArgumentException.class,()->old.goal("kingdom_smp:miner/m01"));}
}
