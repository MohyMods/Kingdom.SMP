package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
class LayeredInteriorInspectionTest {
 @Test void shelvesStackedVerticallyOrCrowdedNextToEachOtherCannotBeDistinctFunctionalZones(){assertFalse(LayeredInteriorInspection.spaced(BlockPos.ZERO,new BlockPos(2,0,2)));assertTrue(LayeredInteriorInspection.spaced(BlockPos.ZERO,new BlockPos(3,0,0)));assertFalse(LayeredInteriorInspection.levels(1,2));assertTrue(LayeredInteriorInspection.levels(1,5));assertFalse(LayeredInteriorInspection.levels(1,10));}
 @Test void approvedMasterInteriorRetainsTierAndEarnedLowerBandPrerequisites(){var d=InspectionQuests.get(LayeredInteriorInspection.QUEST);assertEquals(4,d.minimumTier());assertEquals(300,d.xp());assertFalse(InspectionQuests.catalog().mayAttempt(Profession.BUILDER,d.id(),java.util.Set.of()));}
}
