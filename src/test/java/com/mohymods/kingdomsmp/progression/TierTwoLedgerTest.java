package com.mohymods.kingdomsmp.progression;

import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TierTwoLedgerTest {
    @TempDir Path dir;
    private static final Map<String,Integer> COSTS=Map.of("minecraft:wheat",100,"minecraft:glass",2);
    private static final UUID A=UUID.fromString("00000000-0000-0000-0000-000000000001"),B=UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static void donate(TierTwoLedger s,UUID player,String resource,int amount)throws IOException{var p=s.prepare(UUID.randomUUID(),player,player.equals(A)?"Ada":"Bryn",resource,amount);assertTrue(s.recover(new TierTwoLedger.Receipt(s.id(),p.donation().sequence())));}
    @Test void normalizedRareCategoryAndDeterministicTiedWinnerPersist()throws Exception{
        var s=TierTwoLedger.open(dir,COSTS);donate(s,A,"minecraft:wheat",100);donate(s,B,"minecraft:glass",1);
        assertEquals(A,s.standings().getFirst().player());assertFalse(s.complete());donate(s,B,"minecraft:glass",1);
        assertEquals(s.standings().get(0).scoreUnits(),s.standings().get(1).scoreUnits());assertTrue(s.approveRoad(A,"Inspected completed road"));assertTrue(s.complete());assertEquals(A,s.winner().orElseThrow().player());
        var reload=TierTwoLedger.open(dir,COSTS);assertTrue(reload.complete());assertEquals(A,reload.winner().orElseThrow().player());assertEquals(100,reload.personal(A,"minecraft:wheat"));assertEquals(2,reload.personal(B,"minecraft:glass"));
    }
    @Test void resourceCompletionDoesNotManufactureRoadApproval()throws Exception{
        var s=TierTwoLedger.open(dir,COSTS);donate(s,A,"minecraft:wheat",100);donate(s,A,"minecraft:glass",2);assertTrue(s.resourcesComplete());assertFalse(s.complete());assertTrue(s.winner().isEmpty());
        assertTrue(s.approveRoad(B,"Road inspected by operator"));assertFalse(s.approveRoad(A,"Replacement must not overwrite prior approval"));assertEquals(B,s.road().orElseThrow().actor());assertEquals("Road inspected by operator",s.road().orElseThrow().reason());
    }
    @Test void prepareDoesNotCreditUntilMatchingNativeReceiptAndCancelsSafely()throws Exception{
        var s=TierTwoLedger.open(dir,COSTS);var p=s.prepare(UUID.randomUUID(),A,"Ada","minecraft:wheat",64);assertEquals(0,s.total("minecraft:wheat"));
        var reload=TierTwoLedger.open(dir,COSTS);assertFalse(reload.recover(TierTwoLedger.Receipt.empty()));assertTrue(reload.pending().isEmpty());assertEquals(0,reload.total("minecraft:wheat"));
        p=reload.prepare(UUID.randomUUID(),A,"Ada","minecraft:wheat",64);var again=TierTwoLedger.open(dir,COSTS);assertTrue(again.recover(new TierTwoLedger.Receipt(again.id(),p.donation().sequence())));assertEquals(64,again.total("minecraft:wheat"));assertFalse(again.recover(new TierTwoLedger.Receipt(again.id(),p.donation().sequence())));
    }
    @Test void pendingWrongStoreOrFutureReceiptFailsWithoutGuessing()throws Exception{
        var s=TierTwoLedger.open(dir,COSTS);var p=s.prepare(UUID.randomUUID(),A,"Ada","minecraft:wheat",64);
        assertThrows(IOException.class,()->s.recover(new TierTwoLedger.Receipt(UUID.randomUUID(),p.donation().sequence())));assertThrows(IOException.class,()->s.recover(new TierTwoLedger.Receipt(s.id(),99)));assertTrue(s.pending().isPresent());assertEquals(0,s.total("minecraft:wheat"));
    }
    @Test void partialContributionsCapsAndAcceptedHistoryArePreserved()throws Exception{
        var s=TierTwoLedger.open(dir,COSTS);donate(s,A,"minecraft:wheat",64);donate(s,B,"minecraft:wheat",36);assertEquals(100,s.total("minecraft:wheat"));assertEquals(64,s.personal(A,"minecraft:wheat"));assertEquals(36,s.personal(B,"minecraft:wheat"));
        assertThrows(IllegalArgumentException.class,()->s.prepare(UUID.randomUUID(),B,"Bryn","minecraft:wheat",1));assertThrows(IllegalArgumentException.class,()->s.prepare(UUID.randomUUID(),A,"Ada","minecraft:diamond",1));assertEquals(2,s.history().size());
    }
    @Test void duplicateAcceptedOperationAndConcurrentPreparedContributionCannotReplay()throws Exception{
        var s=TierTwoLedger.open(dir,COSTS);UUID op=UUID.randomUUID();var p=s.prepare(op,A,"Ada","minecraft:wheat",64);assertThrows(IllegalStateException.class,()->s.prepare(UUID.randomUUID(),B,"Bryn","minecraft:glass",1));assertThrows(IllegalStateException.class,()->s.approveRoad(B,"Road inspected"));
        assertTrue(s.recover(new TierTwoLedger.Receipt(s.id(),p.donation().sequence())));assertThrows(IllegalArgumentException.class,()->s.prepare(op,A,"Ada","minecraft:wheat",1));assertEquals(64,s.total("minecraft:wheat"));
    }
    @Test void requirementsArePinnedAcrossConfigChange()throws Exception{
        var s=TierTwoLedger.open(dir,COSTS);donate(s,A,"minecraft:wheat",10);assertThrows(IOException.class,()->TierTwoLedger.open(dir,Map.of("minecraft:wheat",99,"minecraft:glass",2)));assertEquals(10,TierTwoLedger.open(dir,COSTS).total("minecraft:wheat"));
    }
    @Test void missingSealAndUnsupportedSchemaFailClosed()throws Exception{
        TierTwoLedger.open(dir,COSTS);Path file=dir.resolve("kingdomsmp_tier_two.dat");var tag=AtomicNbt.read(file);tag.putInt("schema",2);AtomicNbt.write(file,tag);assertThrows(IOException.class,()->TierTwoLedger.open(dir,COSTS));Files.delete(dir.resolve("kingdomsmp_tier_two.created"));assertThrows(IOException.class,()->TierTwoLedger.open(dir,COSTS));
    }
    @Test void duplicateCorruptHistoryRejectedOnReload()throws Exception{
        var s=TierTwoLedger.open(dir,COSTS);donate(s,A,"minecraft:wheat",10);Path file=dir.resolve("kingdomsmp_tier_two.dat");var tag=AtomicNbt.read(file);var history=tag.getList("history",net.minecraft.nbt.Tag.TAG_COMPOUND);history.add(history.getCompound(0).copy());AtomicNbt.write(file,tag);assertThrows(IOException.class,()->TierTwoLedger.open(dir,COSTS));
    }
    @Test void writeFailureBeforeRenameQuarantinesAndRetainsOldProgress()throws Exception{
        TierTwoLedger.open(dir,COSTS);var s=TierTwoLedger.open(dir,COSTS,(file,tag)->{throw new IOException("simulated pre-rename failure");});assertThrows(IOException.class,()->s.prepare(UUID.randomUUID(),A,"Ada","minecraft:wheat",10));assertTrue(s.quarantined());assertThrows(IllegalStateException.class,()->s.approveRoad(A,"No mutation after uncertainty"));assertTrue(TierTwoLedger.open(dir,COSTS).pending().isEmpty());
    }
    @Test void writeFailureAfterRenameRetainsRecoverablePreparation()throws Exception{
        TierTwoLedger.open(dir,COSTS);var s=TierTwoLedger.open(dir,COSTS,(file,tag)->{AtomicNbt.write(file,tag);throw new IOException("simulated post-rename failure");});assertThrows(IOException.class,()->s.prepare(UUID.randomUUID(),A,"Ada","minecraft:wheat",10));assertTrue(s.quarantined());var reload=TierTwoLedger.open(dir,COSTS);assertTrue(reload.pending().isPresent());assertFalse(reload.recover(TierTwoLedger.Receipt.empty()));assertEquals(0,reload.total("minecraft:wheat"));
    }
    @Test void committedPlayerReceiptSurvivesPostCommitStorageUncertainty()throws Exception{
        var s=TierTwoLedger.open(dir,COSTS);var pending=s.prepare(UUID.randomUUID(),A,"Ada","minecraft:wheat",10);var receipt=new TierTwoLedger.Receipt(s.id(),pending.donation().sequence());
        var faulty=TierTwoLedger.open(dir,COSTS,(file,tag)->{AtomicNbt.write(file,tag);throw new IOException("simulated credit acknowledgement failure");});assertThrows(IOException.class,()->faulty.recover(receipt));assertTrue(faulty.quarantined());var reload=TierTwoLedger.open(dir,COSTS);assertEquals(10,reload.total("minecraft:wheat"));assertEquals(1,reload.expectedReceipt(A));assertTrue(reload.pending().isEmpty());assertFalse(reload.recover(receipt));
    }
}
