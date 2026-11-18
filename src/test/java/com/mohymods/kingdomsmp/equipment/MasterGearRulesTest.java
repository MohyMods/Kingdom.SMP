package com.mohymods.kingdomsmp.equipment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class MasterGearRulesTest {
 @TempDir Path directory;
 @Test void fiveSeparateAuthoritativeIdentitiesAndExactCompounding(){assertEquals(5,MasterGear.values().length);for(var gear:MasterGear.values()){assertEquals(gear,MasterGear.byId(gear.id()));assertEquals(50000,gear.price(50000,0));assertEquals(75000,gear.price(50000,1));assertEquals(112500,gear.price(50000,2));assertEquals(4,gear.price(1,3));assertThrows(IllegalStateException.class,()->gear.price(1000000000,128));}assertTrue(MasterGear.find("kingdom_smp:fake_codex").isEmpty());}
 @Test void additiveConfigLoadsWithoutRewritingAndRejectsUnboundedControls()throws Exception{var file=directory.resolve("master.json");var rules=MasterGearRules.load(file);String original=Files.readString(file);assertEquals(256,rules.treeLimit());assertEquals(6,rules.surveyRadius());assertEquals(80,rules.codexMaxCost());assertEquals(50000,rules.prices().get(MasterGear.CODEX));assertEquals(rules,MasterGearRules.load(file));assertEquals(original,Files.readString(file));Files.writeString(file,original.replace("\"survey_radius\": 6","\"survey_radius\": 1000"));assertThrows(java.io.IOException.class,()->MasterGearRules.load(file));}
}
