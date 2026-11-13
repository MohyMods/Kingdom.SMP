package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class ForestReserveInspectionTest {
 @Test void liveSpeciesAndPersonalNativeHistoryAreIndependentRequirements(){var grown=Set.of("oak","birch","spruce","acacia","cherry","jungle");var logs=Map.of("1","native oak state","2","native birch state");assertTrue(ForestReserveInspection.summarize(logs,Set.of("oak","birch"),grown,24).startsWith("reserve="));assertThrows(IllegalStateException.class,()->ForestReserveInspection.summarize(logs,Set.of("oak"),grown,24));assertThrows(IllegalStateException.class,()->ForestReserveInspection.summarize(logs,Set.of("oak","birch"),Set.of("oak"),24));assertThrows(IllegalStateException.class,()->ForestReserveInspection.summarize(logs,Set.of("oak","birch"),grown,0));}
 @Test void stableNativeGeometryAndRenewalHistoryInvalidateStagedSnapshots(){var grown=Set.of("oak","birch","spruce","acacia","cherry","jungle");var a=new LinkedHashMap<String,String>();a.put("2","birch");a.put("1","oak");String first=ForestReserveInspection.summarize(a,Set.of("oak","birch"),grown,24);assertEquals(first,ForestReserveInspection.summarize(Map.of("1","oak","2","birch"),Set.of("birch","oak"),grown,24));assertNotEquals(first,ForestReserveInspection.summarize(Map.of("1","oak","2","changed native state"),Set.of("oak","birch"),grown,24));assertNotEquals(first,ForestReserveInspection.summarize(a,Set.of("oak","birch"),grown,25));}
}
