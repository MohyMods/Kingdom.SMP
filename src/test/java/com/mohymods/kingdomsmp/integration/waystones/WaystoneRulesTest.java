package com.mohymods.kingdomsmp.integration.waystones;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WaystoneRulesTest {
 @Test void useRequiresNineEvenWithinOverworldAndNeverBypassesEnd(){for(int tier=1;tier<=12;tier++){
  assertEquals(tier>=9,WaystoneRules.allowed(tier,"minecraft:overworld"));assertEquals(tier>=9,WaystoneRules.allowed(tier,"aether:the_aether"));assertEquals(tier>=9,WaystoneRules.allowed(tier,"minecraft:the_nether"));
  assertEquals(tier>=9,WaystoneRules.allowed(tier,"twilightforest:twilight_forest"));assertEquals(tier>=9,WaystoneRules.allowed(tier,"deeperdarker:otherside"));assertEquals(tier>=12,WaystoneRules.allowed(tier,"minecraft:the_end"));
 }}
 @Test void invalidStateNeverAuthorizes(){assertThrows(IllegalArgumentException.class,()->WaystoneRules.allowed(0,"minecraft:overworld"));assertThrows(IllegalArgumentException.class,()->WaystoneRules.allowed(13,"minecraft:overworld"));assertThrows(IllegalArgumentException.class,()->WaystoneRules.allowed(9,null));}
}
