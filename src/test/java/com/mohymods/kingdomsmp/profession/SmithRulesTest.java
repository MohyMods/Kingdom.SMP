package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
class SmithRulesTest {
 @TempDir Path dir;
 @Test void oldSixContractFileRemainsExactAndNewSmithDefaultsAreAdditive()throws Exception{var p=dir.resolve("contracts.json");ContractRules.load(p);var json=com.google.gson.JsonParser.parseString(Files.readString(p)).getAsJsonObject();json.getAsJsonObject("deliveries").remove("blacksmith/daily");json.getAsJsonObject("deliveries").remove("blacksmith/weekly");Files.writeString(p,json.toString());String before=Files.readString(p);var rules=ContractRules.load(p);assertEquals(8,rules.all().size());assertEquals(16,rules.get("blacksmith/daily").quantity());assertEquals(160,rules.get("blacksmith/daily").reward());assertEquals(before,Files.readString(p));json.getAsJsonObject("deliveries").remove("farmer/daily");Files.writeString(p,json.toString());assertThrows(IOException.class,()->ContractRules.load(p));}
 @Test void repairGoalsHaveBoundedExplicitDefaultsAndNeverRewriteCustomRules()throws Exception{var p=dir.resolve("repairs.json");assertEquals(4,RepairQuestRules.load(p));Files.writeString(p,"{\"schema\":1,\"distinctIronItems\":6}");String before=Files.readString(p);assertEquals(6,RepairQuestRules.load(p));assertEquals(before,Files.readString(p));for(String n:new String[]{"0","129","1.5","\"4\""}){Files.writeString(p,"{\"schema\":1,\"distinctIronItems\":"+n+"}");assertThrows(IOException.class,()->RepairQuestRules.load(p));}}
 @Test void oldRepairRulesNeverInventEnchantedHistoryOrRewriteBytesAndNewCountsAreBounded()throws Exception{var p=dir.resolve("repair-bands.json");Files.writeString(p,"{\"schema\":1,\"distinctIronItems\":7}");var bytes=Files.readString(p);var g=RepairQuestRules.loadGoals(p);assertEquals(7,g.ordinary());assertEquals(4,g.enchanted());assertEquals(4,g.tools());assertEquals(bytes,Files.readString(p));for(String n:new String[]{"0","129","1.5","\"4\""}){Files.writeString(p,"{\"schema\":1,\"distinctIronItems\":4,\"enchantedIronItems\":4,\"enchantedTools\":"+n+"}");assertThrows(IOException.class,()->RepairQuestRules.loadGoals(p));}}

}
