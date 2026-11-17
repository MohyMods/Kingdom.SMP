package com.mohymods.kingdomsmp.integration.aircraft;

import static org.junit.jupiter.api.Assertions.*;

import com.mohymods.kingdomsmp.economy.AircraftState;
import java.io.IOException;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AircraftSalesRulesTest {
  @TempDir Path directory;

  @Test
  void pricesAreCompletePersistentBoundedDefaultsAndCanBeConfigured() throws Exception {
    var file = directory.resolve("prices.json");
    var rules = AircraftSalesRules.load(file);
    assertEquals(AircraftState.TYPES, rules.keySet());
    assertEquals(20000L, rules.get("immersive_aircraft:biplane"));
    String first = Files.readString(file);
    assertEquals(rules, AircraftSalesRules.load(file));
    assertEquals(first, Files.readString(file));
    Files.writeString(file, first.replace("20000", "25000"));
    assertEquals(25000L, AircraftSalesRules.load(file).get("immersive_aircraft:biplane"));
  }

  @Test
  void invalidOrPartialSettingsDoNotSilentlyEnableCheapAircraft() throws Exception {
    var file = directory.resolve("prices.json");
    AircraftSalesRules.load(file);
    String first = Files.readString(file);
    for (String changed :
        new String[] {
          first.replace("20000", "0"),
          first.replace("20000", "20000.1"),
          first.replace("biplane", "invented"),
          "{}"
        }) {
      Files.writeString(file, changed);
      assertThrows(IOException.class, () -> AircraftSalesRules.load(file));
      assertEquals(changed, Files.readString(file));
    }
  }
}
