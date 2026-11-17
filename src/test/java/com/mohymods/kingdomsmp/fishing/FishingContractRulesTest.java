package com.mohymods.kingdomsmp.fishing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
class FishingContractRulesTest {
 @TempDir Path dir;
 @Test void finiteDefaultPoolFundsAllOrdersAndReloadsExactly()throws Exception{var path=dir.resolve("contracts.json");var rules=FishingContractRules.load(path);assertEquals(3,rules.orders().size());assertTrue(rules.orders().stream().mapToLong(t->(long)t.goal()*t.unitReward()).sum()<=1600);assertEquals(rules,FishingContractRules.load(path));assertThrows(UnsupportedOperationException.class,()->rules.orders().clear());}
 @Test void unaffordableUnknownOrFractionalTermsRemainDisabledAndUnchanged()throws Exception{var path=dir.resolve("contracts.json");FishingContractRules.load(path);var original=Files.readString(path);for(var invalid:new String[]{original.replace("1600","1"),original.replace("\"bluegill\"","\"cod\""),original.replace("1600","1.5")}){Files.writeString(path,invalid);assertThrows(IOException.class,()->FishingContractRules.load(path));assertEquals(invalid,Files.readString(path));}}
}
