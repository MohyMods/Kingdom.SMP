package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
class NetherOreEvidenceTest {
 @Test void separateNetherHistoryIsBoundedToDimensionAndGlobalSpentSource(){var p=UUID.randomUUID();var e=new NetherOreEvidence();assertFalse(e.record(p,"minecraft:overworld",1,"quartz"));assertTrue(e.record(p,"minecraft:the_nether",1,"quartz"));assertFalse(e.record(UUID.randomUUID(),"minecraft:the_nether",1,"debris"));var r=NetherOreEvidence.load(e.save());assertEquals(1,r.count(p,"quartz"));assertFalse(r.record(p,"minecraft:the_nether",1,"quartz"));assertTrue(r.record(p,"minecraft:the_nether",2,"debris"));assertEquals(1,r.count(p,"debris"));}
 @Test void malformedOrOrphanEvidenceFailsWithoutChangingOriginalOverworldRules(){var p=UUID.randomUUID();var e=new NetherOreEvidence();e.record(p,"minecraft:the_nether",1,"quartz");var malformed=e.save();malformed.put("spent",new ListTag());assertThrows(IllegalArgumentException.class,()->NetherOreEvidence.load(malformed));assertFalse(new NativeOreEvidence().record(p,ProfessionRank.MASTER,"coal","minecraft:the_nether",1));var marks=new NetherOreMarks();marks.mark(1,"minecraft:ancient_debris");marks=NetherOreMarks.load(marks.save());assertEquals("minecraft:ancient_debris",marks.at(1));marks.invalidate(1);assertNull(marks.at(1));assertFalse(NativeOreQuests.eligible("diamond",ProfessionRank.APPRENTICE,12));assertFalse(NativeOreQuests.contains("kingdom_smp:miner/m04"));}
}
