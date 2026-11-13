package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class TrimCopiesTest {
 @Test void twoCopiesMustShareOneSupportedTemplateKind(){assertEquals(Map.of("copies",2),TrimCopies.totals(Map.of("minecraft:coast_armor_trim_smithing_template",2)));assertThrows(IllegalArgumentException.class,()->TrimCopies.totals(Map.of("minecraft:coast_armor_trim_smithing_template",1,"minecraft:dune_armor_trim_smithing_template",1)));}
 @Test void unrelatedNetherEndAndUpgradeTemplatesCannotMasqueradeAsOverworldCopies(){for(var id:List.of("minecraft:spire_armor_trim_smithing_template","minecraft:snout_armor_trim_smithing_template","minecraft:rib_armor_trim_smithing_template","minecraft:netherite_upgrade_smithing_template","client:coast"))assertThrows(IllegalArgumentException.class,()->TrimCopies.totals(Map.of(id,2)));assertTrue(TrimCopies.contains("minecraft:eye_armor_trim_smithing_template"));assertEquals("trim_coast",TrimCopies.metrics().get("minecraft:coast_armor_trim_smithing_template"));}
}
