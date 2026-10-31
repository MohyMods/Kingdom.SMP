package com.mohymods.kingdomsmp.building;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class HutReservationJournalTest {
    @TempDir Path folder;
    @Test void secondIssuanceCannotOverwriteTheDurableFirstReservation() throws Exception {
        Path file = folder.resolve("data/hut.reserved");
        HutReservationJournal.create(file, "first");
        assertThrows(IllegalStateException.class, () -> HutReservationJournal.create(file, "second"));
        HutReservationJournal.verify(file, "first");
        assertEquals("first", Files.readString(file));
    }
    @Test void missingPartialAndMismatchedJournalsFailClosedWithoutRepairingThem() throws Exception {
        Path file = folder.resolve("hut.reserved");
        assertThrows(IllegalStateException.class, () -> HutReservationJournal.verify(file, "complete"));
        Files.writeString(file, "part");
        assertThrows(IllegalStateException.class, () -> HutReservationJournal.verify(file, "complete"));
        assertEquals("part", Files.readString(file));
        assertThrows(IllegalStateException.class, () -> HutReservationJournal.create(file, "complete"));
    }
    @Test void linkedOrOversizedMarkersCannotAuthorizeReissuance() throws Exception {
        Path file = folder.resolve("real");
        Files.writeString(file, "expected");
        Path link = folder.resolve("linked");
        Files.createSymbolicLink(link, file);
        assertThrows(IllegalStateException.class, () -> HutReservationJournal.verify(link, "expected"));
        Files.writeString(file, "x".repeat(513));
        assertThrows(IllegalStateException.class, () -> HutReservationJournal.verify(file, "x".repeat(513)));
    }
}
