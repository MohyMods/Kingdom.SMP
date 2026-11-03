package com.mohymods.kingdomsmp.integration.aircraft;

import static org.junit.jupiter.api.Assertions.*;

import com.mohymods.kingdomsmp.economy.ScoutState;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScoutBookTest {
  @TempDir Path directory;
  final UUID economy = UUID.randomUUID(), pilot = UUID.randomUUID();

  ScoutBook.Survey survey() {
    var m =
        new FlightBook.Mission(
            FlightBook.scoutMissionId(economy, pilot, 100),
            pilot,
            100,
            UUID.randomUUID(),
            UUID.randomUUID(),
            512,
            300,
            3000,
            null,
            null,
            -1,
            0,
            0,
            false,
            false,
            false);
    return new ScoutBook.Survey(m, 2, 256, 0, 0, List.of());
  }

  @Test
  void observationsAndPinnedTermsSurviveRestartAndResetOnInterruptedFlight() throws Exception {
    var book = ScoutBook.open(directory, economy);
    var s = survey();
    s = s.move(s.flight().assign(UUID.randomUUID(), UUID.randomUUID(), 0));
    s = s.observe(new ScoutState.Region(20, 0, "minecraft:plains"));
    book.put(s);
    book.checkpoint();
    assertEquals(s, ScoutBook.open(directory, economy).get(pilot).orElseThrow());
    var reset = s.reset();
    assertTrue(reset.observations().isEmpty());
    assertNull(reset.flight().registration());
    assertEquals(s.required(), reset.required());
  }

  @Test
  void wrongIdentityMissingCompanionAndNearOrDuplicateObservationsFailClosed() throws Exception {
    ScoutBook.open(directory, economy);
    assertThrows(java.io.IOException.class, () -> ScoutBook.open(directory, UUID.randomUUID()));
    var s = survey();
    assertThrows(
        IllegalArgumentException.class,
        () -> s.observe(new ScoutState.Region(0, 0, "minecraft:plains")));
    Files.delete(directory.resolve("kingdomsmp_air_scout.created"));
    assertThrows(java.io.IOException.class, () -> ScoutBook.open(directory, economy));
  }

  @Test
  void testedDefaultsEnableNewPoliciesAndExplicitDisablesOrBadPoliciesDoNotReset()
      throws Exception {
    var file = directory.resolve("scout.json");
    var r = ScoutRules.load(file);
    assertTrue(r.enabled());
    assertEquals(6, r.regions());
    assertEquals(256, r.distance());
    String text = Files.readString(file);
    Files.writeString(file, text.replace("\"enabled\":true", "\"enabled\":false"));
    assertFalse(ScoutRules.load(file).enabled());
    for (String bad :
        new String[] {
          text.replace("256", "0"),
          text.replace("3000", "3000.5"),
          text.replace("\"regions\":6", "\"regions\":33"),
          "{}"
        }) {
      Files.writeString(file, bad);
      assertThrows(java.io.IOException.class, () -> ScoutRules.load(file));
      assertEquals(bad, Files.readString(file));
    }
  }

  @Test
  void exactRegionDeduplicationAndTypedDataRejectMalformedSavedObservations() throws Exception {
    var s = survey();
    s = s.move(s.flight().assign(UUID.randomUUID(), UUID.randomUUID(), 0));
    var r = new ScoutState.Region(20, -2, "minecraft:plains");
    s = s.observe(r);
    assertEquals(s, s.observe(r));
    var encoded = com.mohymods.kingdomsmp.economy.ScoutData.encode(List.of(r));
    assertEquals(List.of(r), com.mohymods.kingdomsmp.economy.ScoutData.decode(encoded));
    encoded.getCompound(0).putLong("x", 20);
    assertThrows(
        IllegalArgumentException.class,
        () -> com.mohymods.kingdomsmp.economy.ScoutData.decode(encoded));
    var duplicate = com.mohymods.kingdomsmp.economy.ScoutData.encode(List.of(r, r));
    assertThrows(
        IllegalArgumentException.class,
        () -> com.mohymods.kingdomsmp.economy.ScoutData.decode(duplicate));
  }
}
