package com.mohymods.kingdomsmp.integration.aircraft;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AircraftFuelRulesTest {
  @TempDir Path directory;

  @Test
  void finiteWholePacksUseExactConfigurableUnitPrices() throws Exception {
    var file = directory.resolve("fuel.json");
    var rules = AircraftFuel.load(file);
    assertEquals(16, rules.packSize());
    assertEquals(128, rules.price());
    assertEquals(128, rules.dailyStock());
    String original = Files.readString(file);
    assertEquals(rules, AircraftFuel.load(file));
    assertEquals(original, Files.readString(file));
    Files.writeString(file, original.replace("\"unitPrice\":8", "\"unitPrice\":10"));
    assertEquals(160, AircraftFuel.load(file).price());
  }

  @Test
  void malformedOrFractionalOrImpossiblePackStocksFailWithoutResettingSettings() throws Exception {
    var file = directory.resolve("fuel.json");
    AircraftFuel.load(file);
    String original = Files.readString(file);
    for (String bad :
        new String[] {
          original.replace("128", "127"),
          original.replace("16", "0"),
          original.replace("\"unitPrice\":8", "\"unitPrice\":8.5"),
          original.replace("packSize", "unknown"),
          "{}"
        }) {
      Files.writeString(file, bad);
      assertThrows(java.io.IOException.class, () -> AircraftFuel.load(file));
      assertEquals(bad, Files.readString(file));
    }
  }
}
