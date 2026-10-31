package com.mohymods.kingdomsmp.persistence;

import com.mohymods.kingdomsmp.kingdom.TierOneProgress;
import com.mohymods.kingdomsmp.kingdom.TierOneProgress.Resource;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ContributionLedgerTest {
    @TempDir Path folder;
    private final UUID player = UUID.randomUUID();
    private ContributionLedger open() throws IOException { return ContributionLedger.open(folder, TierOneProgress.initial()); }
    @Test void preparationWithoutDurableRemovalCancelsWithoutCreditAfterRestart() throws Exception {
        var ledger = open(); ledger.prepare(UUID.randomUUID(), player, "Alice", Resource.LOGS, 64);
        ledger = open(); assertTrue(ledger.pending().isPresent());
        assertFalse(ledger.recover(ContributionLedger.Receipt.empty()));
        assertEquals(0, open().progress().total(Resource.LOGS)); assertTrue(open().pending().isEmpty());
    }
    @Test void durableRemovalCommitsExactlyOnceAfterCrashBeforeCredit() throws Exception {
        var ledger = open(); var pending = ledger.prepare(UUID.randomUUID(), player, "Alice", Resource.LOGS, 64);
        var receipt = new ContributionLedger.Receipt(ledger.id(), pending.donation().sequence());
        ledger = open(); assertTrue(ledger.recover(receipt));
        ledger = open(); assertFalse(ledger.recover(receipt));
        assertEquals(64, ledger.progress().total(Resource.LOGS)); assertEquals(1, ledger.history().size());
        assertThrows(IllegalArgumentException.class, () -> open().prepare(pending.donation().operation(), player, "Alice", Resource.LOGS, 64));
    }
    @Test void wrongStoreFutureAndStaleReceiptsCannotAuthorizeCredit() throws Exception {
        var ledger = open(); ledger.prepare(UUID.randomUUID(), player, "Alice", Resource.IRON_INGOTS, 1);
        assertThrows(IOException.class, () -> ledger.recover(new ContributionLedger.Receipt(UUID.randomUUID(), 1)));
        assertThrows(IOException.class, () -> ledger.recover(new ContributionLedger.Receipt(ledger.id(), 2)));
        assertEquals(0, open().progress().total(Resource.IRON_INGOTS));
    }
    @Test void failedPreparationDoesNotExposeConsumableAuthorizationAndQuarantinesWriter() throws Exception {
        open();
        var ledger = ContributionLedger.open(folder, TierOneProgress.initial(), (file, tag) -> { throw new IOException("disk full"); });
        assertThrows(IOException.class, () -> ledger.prepare(UUID.randomUUID(), player, "Alice", Resource.WHEAT, 1));
        assertTrue(ledger.quarantined()); assertTrue(ledger.pending().isEmpty());
        assertEquals(0, open().progress().revision());
    }
    @Test void interruptedPrepareAfterRenameCancelsWhenInventoryWasNeverRemoved() throws Exception {
        open();
        var failed = ContributionLedger.open(folder, TierOneProgress.initial(), (file, tag) -> { AtomicNbt.write(file, tag); throw new IOException("after prepare rename"); });
        assertThrows(IOException.class, () -> failed.prepare(UUID.randomUUID(), player, "Alice", Resource.LOGS, 64));
        var restored = open(); assertTrue(restored.pending().isPresent());
        assertFalse(restored.recover(ContributionLedger.Receipt.empty()));
        assertEquals(0, open().progress().total(Resource.LOGS));
    }
    @Test void failedCreditWriteLeavesPreparedRemovalRecoverable() throws Exception {
        var ledger = open(); var pending = ledger.prepare(UUID.randomUUID(), player, "Alice", Resource.COBBLESTONE, 17);
        var receipt = new ContributionLedger.Receipt(ledger.id(), pending.donation().sequence());
        var failed = ContributionLedger.open(folder, TierOneProgress.initial(), (file, tag) -> { throw new IOException("disk full"); });
        assertThrows(IOException.class, () -> failed.recover(receipt)); assertTrue(failed.quarantined());
        ledger = open(); assertTrue(ledger.recover(receipt)); assertEquals(17, open().progress().total(Resource.COBBLESTONE));
    }
    @Test void exceptionAfterAtomicCreditReplacementCannotDuplicateOnRestart() throws Exception {
        var ledger = open(); var pending = ledger.prepare(UUID.randomUUID(), player, "Alice", Resource.LOGS, 2);
        var receipt = new ContributionLedger.Receipt(ledger.id(), pending.donation().sequence());
        var uncertain = ContributionLedger.open(folder, TierOneProgress.initial(), (file, tag) -> { AtomicNbt.write(file, tag); throw new IOException("interrupted after rename"); });
        assertThrows(IOException.class, () -> uncertain.recover(receipt));
        ledger = open(); assertFalse(ledger.recover(receipt)); assertEquals(2, ledger.progress().total(Resource.LOGS));
        assertEquals(1, ledger.history().size());
    }
    @Test void missingCorruptAndFutureLedgerNeverSilentlyReset() throws Exception {
        open(); Path file = folder.resolve("kingdomsmp_contributions.dat");
        var valid = AtomicNbt.read(file); valid.putInt("schema", 99); AtomicNbt.write(file, valid);
        assertThrows(IOException.class, this::open);
        Files.delete(file); assertThrows(IOException.class, this::open);
        Files.writeString(file, "corrupt"); assertThrows(IOException.class, this::open);
    }
    @Test void completionAndPermanentWinnerRebuildFromAcceptedHistory() throws Exception {
        var ledger = open();
        for (Resource resource : Resource.values()) {
            var pending = ledger.prepare(UUID.randomUUID(), player, "Alice", resource, resource.required());
            assertTrue(ledger.recover(new ContributionLedger.Receipt(ledger.id(), pending.donation().sequence())));
        }
        ledger = open(); assertTrue(ledger.progress().complete()); assertEquals(player, ledger.progress().winner().orElseThrow());
        assertEquals(4, ledger.history().size()); assertEquals(8192, ledger.progress().contributors().get(player).scoreUnits());
        assertThrows(IllegalArgumentException.class, () -> open().prepare(UUID.randomUUID(), player, "Alice", Resource.LOGS, 1));
    }
}
