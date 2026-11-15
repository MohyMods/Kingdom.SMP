package com.mohymods.kingdomsmp.economy;
import static org.junit.jupiter.api.Assertions.*;
import com.mohymods.kingdomsmp.profession.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class AlchemyTraceTest {
 @Test void missingCanonicalPotionReceiptsCannotBecomeTraceableLabEvidence(@TempDir java.nio.file.Path path)throws Exception{var store=EconomyStore.open(path);var p=UUID.randomUUID();assertThrows(IllegalStateException.class,()->store.alchemyTrace(p));assertEquals(0,store.balance(p));assertTrue(store.mailbox(p).isEmpty());assertTrue(InspectionQuests.contains("kingdom_smp:alchemist/j08"));assertEquals(6,InspectionQuests.get("kingdom_smp:alchemist/j08").minimumTier());assertEquals(5,InspectionQuests.get("kingdom_smp:alchemist/a08").minimumTier());assertFalse(InspectionQuests.contains("kingdom_smp:alchemist/m08"));}
}
