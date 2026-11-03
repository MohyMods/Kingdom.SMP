package com.mohymods.kingdomsmp.title;

import static org.junit.jupiter.api.Assertions.*;

import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TitleBookTest {
  @TempDir Path directory;
  final UUID world = UUID.randomUUID(), actor = UUID.randomUUID();
  final String founder = "kingdom_smp:founder/royal_bank";

  @Test
  void unauthorizedSelectionNeverWritesAndAuthorizedSelectionIsSelfOnlyPersistentAndIdempotent()
      throws Exception {
    var b = TitleBook.open(directory, world);
    assertThrows(IllegalStateException.class, () -> b.select(actor, founder, Set.of()));
    assertEquals(0, b.revision());
    assertTrue(b.select(actor, founder, Set.of(founder)));
    assertFalse(b.select(actor, founder, Set.of(founder)));
    assertEquals(1, b.revision());
    assertEquals(TitleBook.NONE, b.selected(UUID.randomUUID()));
    var reopened = TitleBook.open(directory, world);
    assertEquals(founder, reopened.selected(actor));
    assertTrue(reopened.select(actor, TitleBook.NONE, Set.of()));
    assertEquals(TitleBook.NONE, TitleBook.open(directory, world).selected(actor));
  }

  @Test
  void foreignIdentityMissingCompanionAndTypedRevisionRequireMatchingRecovery() throws Exception {
    var b = TitleBook.open(directory, world);
    b.select(actor, founder, Set.of(founder));
    assertThrows(java.io.IOException.class, () -> TitleBook.open(directory, UUID.randomUUID()));
    var file = directory.resolve("kingdomsmp_titles.dat");
    var d = AtomicNbt.read(file);
    d.putInt("revision", 1);
    AtomicNbt.write(file, d);
    assertThrows(java.io.IOException.class, () -> TitleBook.open(directory, world));
    Files.delete(directory.resolve("kingdomsmp_titles.created"));
    assertThrows(java.io.IOException.class, () -> TitleBook.open(directory, world));
  }

  @Test
  void beforeWriteFailurePausesOnlyPreferencesAndRetainsOldCommittedSelection() throws Exception {
    TitleBook.open(directory, world);
    var b =
        TitleBook.open(
            directory,
            world,
            (p, d) -> {
              throw new java.io.IOException("Private preference write fault");
            });
    assertThrows(java.io.IOException.class, () -> b.select(actor, founder, Set.of(founder)));
    assertTrue(b.paused());
    assertEquals(TitleBook.NONE, b.selected(actor));
    assertThrows(IllegalStateException.class, () -> b.select(actor, founder, Set.of(founder)));
    assertEquals(TitleBook.NONE, TitleBook.open(directory, world).selected(actor));
  }

  @Test
  void uncertainAfterRenameRetainsOnlyAlreadyEarnedCosmeticIntentOnReload() throws Exception {
    TitleBook.open(directory, world);
    var b =
        TitleBook.open(
            directory,
            world,
            (p, d) -> {
              AtomicNbt.write(p, d);
              throw new java.io.IOException("Private after-rename preference fault");
            });
    assertThrows(java.io.IOException.class, () -> b.select(actor, founder, Set.of(founder)));
    assertTrue(b.paused());
    assertEquals(founder, TitleBook.open(directory, world).selected(actor));
  }

  @Test
  void hostileLabelsAndForeignNamespaceCannotBecomePreferences() throws Exception {
    var b = TitleBook.open(directory, world);
    for (String value :
        List.of(
            "minecraft:operator",
            "kingdom_smp:op",
            "kingdom_smp:profession/master\nadmin",
            "kingdom_smp:founder/" + "a".repeat(100)))
      assertThrows(IllegalArgumentException.class, () -> b.select(actor, value, Set.of(value)));
    assertEquals(0, b.revision());
  }
}
