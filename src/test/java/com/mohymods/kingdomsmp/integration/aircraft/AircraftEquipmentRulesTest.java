package com.mohymods.kingdomsmp.integration.aircraft;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AircraftEquipmentRulesTest {
  @TempDir Path directory;

  @Test
  void boundedNativeCatalogueSettingsPersistAndRejectUnknownOrFreeStock() throws Exception {
    var file = directory.resolve("aircraft-equipment.json");
    var first = AircraftEquipment.load(file);
    assertEquals(6, first.prices().size());
    assertEquals(8, first.dailyStock());
    assertEquals(
        2000, first.price(AircraftEquipment.part("immersive_aircraft:enhanced_propeller")));
    String original = Files.readString(file);
    assertEquals(first, AircraftEquipment.load(file));
    assertEquals(original, Files.readString(file));
    for (String bad :
        new String[] {
          original.replace("2000", "-1"),
          original.replace("dailyStock\": 8", "dailyStock\": 0"),
          original.replace("enhanced_propeller", "invented"),
          original.replace("2000", "2.5")
        }) {
      Files.writeString(file, bad);
      assertThrows(IOException.class, () -> AircraftEquipment.load(file));
      assertEquals(bad, Files.readString(file));
    }
  }
}
