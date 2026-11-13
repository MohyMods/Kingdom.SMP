package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class NativeOreRulesTest {
 @TempDir Path dir;
 @Test void oldCoalIronConfigRetainsCostsAndAddsCopperDefaultWithoutRewritingFile()throws Exception{var file=dir.resolve("ore.json");NativeOreRules.load(file);var root=JsonParser.parseString(Files.readString(file)).getAsJsonObject();root.remove("copperAreaSeparationChunks");root.getAsJsonObject("goals").remove("kingdom_smp:miner/a03");root.getAsJsonObject("goals").addProperty("kingdom_smp:miner/a01",99);String old=root.toString();Files.writeString(file,old);var rules=NativeOreRules.load(file);assertEquals(99,rules.goal("kingdom_smp:miner/a01"));assertEquals(64,rules.goal("kingdom_smp:miner/a03"));assertEquals(2,rules.separation());assertEquals(old,Files.readString(file));}
 @Test void malformedAreaSeparationRefusesWithoutResettingSavedBalance()throws Exception{var file=dir.resolve("ore.json");NativeOreRules.load(file);var root=JsonParser.parseString(Files.readString(file)).getAsJsonObject();root.addProperty("copperAreaSeparationChunks",0);Files.writeString(file,root.toString());assertThrows(java.io.IOException.class,()->NativeOreRules.load(file));root.addProperty("copperAreaSeparationChunks",2.5);Files.writeString(file,root.toString());assertThrows(java.io.IOException.class,()->NativeOreRules.load(file));}
}
