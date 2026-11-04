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
}
