package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
class NativeSpecimensTest {
 @Test void onlyApprovedPlainOreFamiliesMaySatisfyPermanentGroupedSpecimens(){var id=NativeSpecimens.QUEST;assertTrue(CraftDeliveryQuests.grouped(id));var actual=Map.of("minecraft:deepslate_iron_ore",1,"minecraft:gold_ore",1,"minecraft:deepslate_redstone_ore",1,"minecraft:lapis_ore",1);assertEquals(NativeSpecimens.DEFAULTS,CraftDeliveryQuests.totals(id,actual));var encoded=new CompoundTag();actual.forEach(encoded::putInt);CraftDeliveryQuests.validate(id,encoded);assertThrows(IllegalArgumentException.class,()->CraftDeliveryQuests.totals(id,Map.of("minecraft:nether_gold_ore",1)));assertNull(NativeSpecimens.category("minecraft:diamond_ore"));assertFalse(NativeOreQuests.eligible("iron",ProfessionRank.MASTER,12));assertFalse(NativeOreQuests.contains("kingdom_smp:miner/m07"));}
}
