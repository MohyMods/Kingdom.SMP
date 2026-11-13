package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class MasterTimberDeliveryTest {
 @Test void bothNativeHarvestAndPhysicalStockMustMeetFourSpeciesQuotas(){var counts=Map.of("oak",80,"birch",80,"spruce",80,"jungle",80);assertEquals(256,MasterTimberDelivery.select(counts,counts,256).values().stream().mapToInt(Integer::intValue).sum());assertThrows(IllegalStateException.class,()->MasterTimberDelivery.select(counts,Map.of("oak",2000),256));assertThrows(IllegalStateException.class,()->MasterTimberDelivery.select(Map.of("oak",256),counts,256));assertThrows(IllegalStateException.class,()->MasterTimberDelivery.select(counts,Map.of("oak",80,"birch",80,"spruce",80,"jungle",31),256));}
 @Test void rawAndStrippedShareOneSpeciesAndCanonicalInputsCannotFakeVariety(){assertEquals("oak",MasterTimberDelivery.species("minecraft:stripped_oak_log"));assertNull(MasterTimberDelivery.species("other:oak_log"));assertNull(MasterTimberDelivery.species("minecraft:oak_wood"));assertThrows(IllegalArgumentException.class,()->MasterTimberDelivery.validate(Map.of("minecraft:oak_log",128,"minecraft:stripped_oak_log",128)));assertEquals(256,MasterTimberDelivery.validate(Map.of("minecraft:oak_log",160,"minecraft:birch_log",32,"minecraft:spruce_log",32,"minecraft:jungle_log",32)));assertThrows(IllegalArgumentException.class,()->MasterTimberDelivery.goal(255));}
}
