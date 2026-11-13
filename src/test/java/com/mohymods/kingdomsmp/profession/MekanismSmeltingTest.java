package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class MekanismSmeltingTest {
 @Test void approvedOsmiumRequiresNativeProcessingAndPermanentDeliveryAtOriginalTier(){var d=CraftDeliveryQuests.get(MekanismSmelting.QUEST);assertEquals(6,d.minimumTier());assertEquals(100,d.xp());assertEquals(Set.of(ApprovedProfessionContent.Validation.MAKE,ApprovedProfessionContent.Validation.DEL),d.validation());assertEquals(Map.of("mekanism:ingot_osmium",32),CraftDeliveryQuests.defaults(d.id()));assertEquals("smelt_osmium",CraftDeliveryQuests.metrics(Profession.MECHANIC).get("mekanism:ingot_osmium"));assertFalse(MekanismSmelting.RECIPES.stream().anyMatch(s->s.contains("dust")||s.contains("nugget")||s.contains("storage")));assertEquals(4,MekanismSmelting.RECIPES.size());}
}
