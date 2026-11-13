package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
class NativeOreEvidenceTest {
 @Test void globalSpentPositionsAndCountersRestoreTogetherAcrossPlayersAndBands(){var a=UUID.randomUUID();var b=UUID.randomUUID();var e=new NativeOreEvidence();assertTrue(e.record(a,ProfessionRank.APPRENTICE,"coal","minecraft:overworld",1));assertFalse(e.record(b,ProfessionRank.APPRENTICE,"coal","minecraft:overworld",1));var r=NativeOreEvidence.load(e.save());assertEquals(1,r.count(a,ProfessionRank.APPRENTICE,"coal"));assertFalse(r.record(a,ProfessionRank.JOURNEYMAN,"iron","minecraft:overworld",1));assertTrue(r.record(b,ProfessionRank.APPRENTICE,"iron","minecraft:overworld",2));assertFalse(r.record(b,ProfessionRank.APPRENTICE,"iron","minecraft:the_end",3));}
 @Test void unsupportedAndMalformedSavedProofsFailClosedRatherThanReset(){var e=new NativeOreEvidence();var tag=e.save();tag.getList("spent",Tag.TAG_STRING).add(StringTag.valueOf("minecraft:overworld@not-a-number"));assertThrows(IllegalArgumentException.class,()->NativeOreEvidence.load(tag));assertFalse(e.record(UUID.randomUUID(),ProfessionRank.APPRENTICE,"diamond","minecraft:overworld",2));}
 @Test void orphanCountersCannotBeRecoveredAsLegitimateHistory(){var e=new NativeOreEvidence();e.record(UUID.randomUUID(),ProfessionRank.APPRENTICE,"coal","minecraft:overworld",1);var tag=e.save();tag.put("spent",new ListTag());assertThrows(IllegalArgumentException.class,()->NativeOreEvidence.load(tag));}
 @Test void chunkMarksSurvivePromotionSerializationAndNativeMutationWithoutRetrospectiveProof(){var marks=new NativeOreMarks();assertNull(marks.at(7));marks.mark(7,"minecraft:coal_ore");marks.mark(8,"client:coal_ore");var r=NativeOreMarks.load(marks.save());assertEquals("minecraft:coal_ore",r.at(7));assertNull(r.at(8));assertTrue(r.invalidate(7));assertNull(r.at(7));assertFalse(r.invalidate(7));}
 @Test void corruptChunkAttachmentIsPreservedAndDisabledWithoutBreakingChunkLoading(){var raw=new CompoundTag();raw.putString("unknown","retain me");var r=NativeOreMarks.load(raw);r.mark(7,"minecraft:coal_ore");assertNull(r.at(7));assertEquals(raw,r.save());}
 @Test void saturatedChunkNeverEvictsOldPositionsBackIntoEligibleHistory(){var m=new NativeOreMarks();for(int n=0;n<=NativeOreMarks.LIMIT;n++)m.mark(n,"minecraft:iron_ore");assertTrue(m.positions().isEmpty());m.mark(0,"minecraft:coal_ore");assertNull(m.at(0));assertNull(NativeOreMarks.load(m.save()).at(0));}
}
