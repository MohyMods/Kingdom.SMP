package com.mohymods.kingdomsmp.integration.aircraft;

import static org.junit.jupiter.api.Assertions.*;

import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlightBookTest {
  @TempDir Path directory;
  final UUID economy = UUID.randomUUID(), pilot = UUID.randomUUID();

  FlightBook.Mission fresh() {
    return new FlightBook.Mission(
        FlightBook.missionId(economy, pilot, 100),
        pilot,
        100,
        UUID.randomUUID(),
        UUID.randomUUID(),
        128,
        500,
        5000,
        null,
        null,
        -1,
        0,
        0,
        false,
        false,
        false);
  }

  @Test
  void realEvidenceAndEachSettlementPhaseSurviveReload() throws Exception {
    var b = FlightBook.open(directory, economy);
    var m = fresh();
    b.put(m);
    b.checkpoint();
    assertEquals(m, FlightBook.open(directory, economy).get(pilot).orElseThrow());
    UUID registration = UUID.randomUUID(), entity = UUID.randomUUID();
    m = m.assign(registration, entity, 2);
    for (int i = 0; i < 8; i++) m = m.fly(registration, entity, 2, 16);
    m = m.arrive();
    b.put(m);
    b.checkpoint();
    assertEquals(m, FlightBook.open(directory, economy).get(pilot).orElseThrow());
    m = m.verify();
    b.put(m);
    b.checkpoint();
    assertEquals(m, FlightBook.open(directory, economy).get(pilot).orElseThrow());
    m = m.posted();
    b.put(m);
    b.checkpoint();
    assertEquals(m, FlightBook.open(directory, economy).get(pilot).orElseThrow());
  }

  @Test
  void missingOrForeignIdentityNeverSilentlyResetsEvidence() throws Exception {
    FlightBook.open(directory, economy);
    assertThrows(java.io.IOException.class, () -> FlightBook.open(directory, UUID.randomUUID()));
    Files.delete(directory.resolve("kingdomsmp_air_dispatch.dat"));
    assertThrows(java.io.IOException.class, () -> FlightBook.open(directory, economy));
  }

  @Test
  void impossibleTravelWrongAircraftAndPrematureReportsAreRejected() {
    var m = fresh().assign(UUID.randomUUID(), UUID.randomUUID(), 0);
    assertThrows(IllegalArgumentException.class, m::arrive);
    assertThrows(IllegalArgumentException.class, m::verify);
    assertThrows(IllegalArgumentException.class, m::posted);
    for (double step : new double[] {Double.NaN, Double.POSITIVE_INFINITY, -1, 32.01})
      assertThrows(
          IllegalArgumentException.class, () -> m.fly(m.registration(), m.entity(), 0, step));
    assertThrows(IllegalStateException.class, () -> m.fly(UUID.randomUUID(), m.entity(), 0, 16));
    assertThrows(IllegalStateException.class, () -> m.fly(m.registration(), m.entity(), 1, 16));
    assertEquals(fresh().pilot(), m.reset().pilot());
    assertEquals(0, m.reset().meters());
    assertNull(m.reset().registration());
  }

  @Test
  void unknownSavedFieldsAndCounterTypesFailClosed() throws Exception {
    var b = FlightBook.open(directory, economy);
    b.put(fresh());
    b.checkpoint();
    var path = directory.resolve("kingdomsmp_air_dispatch.dat");
    var data = AtomicNbt.read(path);
    var row = data.getList("missions", 10).getCompound(0);
    row.putString("meters", "128");
    AtomicNbt.write(path, data);
    assertThrows(java.io.IOException.class, () -> FlightBook.open(directory, economy));
  }

  @Test
  void configurableTermsAreExactBoundedAndNotOverwrittenOnFailure() throws Exception {
    var path = directory.resolve("dispatch.json");
    assertEquals(new FlightRules(128, 500, 5000), FlightRules.load(path));
    String text = Files.readString(path);
    for (String bad :
        new String[] {
          text.replace("128", "31"),
          text.replace("5000", "100"),
          text.replace("5000", "5000.5"),
          "{}"
        }) {
      Files.writeString(path, bad);
      assertThrows(java.io.IOException.class, () -> FlightRules.load(path));
      assertEquals(bad, Files.readString(path));
    }
  }
}
