package com.mohymods.kingdomsmp.profession;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;

class RetainedPlacementEvidenceTest {
    @Test void positionsReplaceNeverIncrementAndRemovalInvalidatesTheOriginalProof(){
        var evidence=new RetainedPlacementEvidence();var site=UUID.randomUUID();var pos=new BlockPos(12,-63,40);
        assertTrue(evidence.record(site,pos,"Block{minecraft:cobblestone}"));assertFalse(evidence.record(site,pos,"Block{minecraft:cobblestone}"));assertEquals(1,evidence.positions(site).size());
        assertTrue(evidence.record(site,pos,"Block{minecraft:stone_bricks}"));assertEquals(1,evidence.positions(site).size());assertEquals("Block{minecraft:stone_bricks}",evidence.positions(site).get(pos.asLong()));
        assertTrue(evidence.invalidate(site,pos));assertFalse(evidence.invalidate(site,pos));assertTrue(evidence.positions(site).isEmpty());
    }
    @Test void differentSitesCannotUseEachOthersProofAndSavedDataRoundTrips(){
        var evidence=new RetainedPlacementEvidence();var site=UUID.randomUUID();evidence.record(site,new BlockPos(1,64,1),"Block{minecraft:oak_log}[axis=y]");
        assertTrue(evidence.positions(UUID.randomUUID()).isEmpty());var saved=evidence.save();var restored=RetainedPlacementEvidence.load(saved);assertEquals(saved,restored.save());
        assertThrows(UnsupportedOperationException.class,()->restored.positions(site).clear());
        assertEquals(saved,RetainedBuildingSavedData.load(saved,null).save(new CompoundTag(),null));
    }
    @Test void duplicatePositionUnknownFieldsAndMalformedStateCannotLoad(){
        var evidence=new RetainedPlacementEvidence();var site=UUID.randomUUID();evidence.record(site,new BlockPos(1,64,1),"Block{minecraft:stone}");
        var saved=evidence.save();var row=saved.getList("sites",Tag.TAG_COMPOUND).getCompound(0);var positions=row.getList("positions",Tag.TAG_COMPOUND);positions.add(positions.getCompound(0).copy());
        assertThrows(IllegalArgumentException.class,()->RetainedPlacementEvidence.load(saved));
        var unknown=evidence.save();unknown.putInt("clientCount",2000);assertThrows(IllegalArgumentException.class,()->RetainedPlacementEvidence.load(unknown));
        var empty=evidence.save();empty.getList("sites",Tag.TAG_COMPOUND).getCompound(0).getList("positions",Tag.TAG_COMPOUND).getCompound(0).putString("state","");assertThrows(IllegalArgumentException.class,()->RetainedPlacementEvidence.load(empty));
    }
    @Test void finiteWorldCapacityDoesNotEvictOrRecountOldPositions(){
        var evidence=new RetainedPlacementEvidence();var site=UUID.randomUUID();
        for(int x=0;x<RetainedPlacementEvidence.MAX_POSITIONS;x++)assertTrue(evidence.record(site,new BlockPos(x,64,0),"stone"));
        assertFalse(evidence.record(UUID.randomUUID(),new BlockPos(0,64,1),"stone"));assertFalse(evidence.record(site,new BlockPos(0,64,0),"stone"));
        assertEquals(RetainedPlacementEvidence.MAX_POSITIONS,evidence.positions(site).size());
    }
}
