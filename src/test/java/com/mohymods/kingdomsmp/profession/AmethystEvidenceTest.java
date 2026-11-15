package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
class AmethystEvidenceTest {
 @Test void onlyFullNativeGrowthChainCanBecomeMatureAndReplacementBreaksIt(){var e=new AmethystEvidence();long cell=BlockPos.ZERO.above().asLong(),bud=BlockPos.ZERO.asLong();e.grow(cell,bud,3,4,null);assertNull(e.at(cell));for(int stage=1;stage<=4;stage++){var previous=e.at(cell);e.invalidate(cell);e.grow(cell,bud,stage-1,stage,previous);}assertEquals(4,e.at(cell).stage());e.invalidate(cell);e.grow(cell,bud,3,4,null);assertNull(e.at(cell));e.grow(cell,bud,0,2,null);assertNull(e.at(cell));}
 @Test void genuinePartialGrowthAndHarvesterCountersRestoreWithoutUnknownProof(){var e=new AmethystEvidence();long cell=BlockPos.ZERO.above().asLong();e.grow(cell,0,0,1,null);e.grow(cell,0,1,2,e.at(cell));var owner=UUID.randomUUID();e.harvested(owner,16);var r=AmethystEvidence.load(e.save());assertEquals(2,r.at(cell).stage());assertEquals(16,r.count(owner));assertEquals(0,r.count(UUID.randomUUID()));assertEquals(e.save(),r.save());var malformed=e.save();malformed.getList("growth",Tag.TAG_COMPOUND).getCompound(0).putInt("stage",5);assertThrows(IllegalArgumentException.class,()->AmethystEvidence.load(malformed));var duplicate=e.save();duplicate.getList("growth",Tag.TAG_COMPOUND).add(duplicate.getList("growth",Tag.TAG_COMPOUND).getCompound(0).copy());assertThrows(IllegalArgumentException.class,()->AmethystEvidence.load(duplicate));assertThrows(IllegalArgumentException.class,()->r.harvested(owner,0));}
 @Test void saturationDoesNotEvictPreviouslyTrackedSource(){var e=new AmethystEvidence();for(int n=0;n<=AmethystEvidence.LIMIT;n++){long bud=new BlockPos(n,0,0).asLong();e.grow(new BlockPos(n,1,0).asLong(),bud,0,1,null);}assertNotNull(e.at(BlockPos.ZERO.above().asLong()));assertNull(e.at(new BlockPos(AmethystEvidence.LIMIT,1,0).asLong()));assertEquals(e.save(),AmethystEvidence.load(e.save()).save());}
}
