package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
class ReagentCropProofTest {
 UUID owner=UUID.randomUUID();ReagentCropProof.Cell cell=new ReagentCropProof.Cell("minecraft:overworld",42);
 @Test void actualAgeChainAndPlanterAreRetainedAcrossNativeSaveAndHarvestCanTakeOnlyOnce(){var d=new ReagentCropProof();d.plant(cell,owner);d.grow(cell,0,3);var restored=ReagentCropProof.load(d.save());restored.grow(cell,3,7);var plant=restored.take(cell).orElseThrow();assertEquals(owner,plant.owner());assertEquals(7,plant.age());restored.harvest(plant.owner(),2);assertTrue(restored.take(cell).isEmpty());assertEquals(2,ReagentCropProof.load(restored.save()).count(owner));}
 @Test void unknownMaturePlacedBlocksAgeRewindAndOtherReplacementCannotInferGrowth(){var d=new ReagentCropProof();d.grow(cell,0,7);assertTrue(d.take(cell).isEmpty());d.plant(cell,owner);d.grow(cell,6,7);assertTrue(d.take(cell).isEmpty());d.plant(cell,owner);d.grow(cell,0,2);d.grow(cell,2,0);assertTrue(d.take(cell).isEmpty());d.plant(cell,owner);d.invalidate(cell);assertTrue(d.take(cell).isEmpty());assertEquals(0,d.count(owner));}
 @Test void malformedOrDuplicateSavedRecordsNeverResetEvidenceToFresh(){var d=new ReagentCropProof();d.plant(cell,owner);var tag=d.save();var list=tag.getList("plants",Tag.TAG_COMPOUND);list.add(list.getCompound(0).copy());assertThrows(IllegalArgumentException.class,()->ReagentCropProof.load(tag));var bad=d.save();bad.getList("plants",Tag.TAG_COMPOUND).getCompound(0).putInt("age",8);assertThrows(IllegalArgumentException.class,()->ReagentCropProof.load(bad));var counts=d.save();counts.getCompound("counts").putInt(owner.toString(),-1);assertThrows(IllegalArgumentException.class,()->ReagentCropProof.load(counts));}
}
