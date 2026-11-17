package com.mohymods.kingdomsmp.integration.aircraft;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CargoRulesTest {
  @TempDir Path directory;

  @Test
  void validatedDefaultsAreEnabledAndTermsAreBoundedExactAndPersistent() throws Exception {
    var file = directory.resolve("cargo.json");
    var rules = CargoRules.load(file);
    assertTrue(rules.enabled());
    assertEquals("minecraft:wheat", rules.item());
    assertEquals(64, rules.quantity());
    assertEquals(128, rules.minimum());
    String text = Files.readString(file);
    assertEquals(rules, CargoRules.load(file));
    for (String bad :
        new String[] {
          text.replace("64", "0"),
          text.replace("5000", "5000.5"),
          text.replace("true", "\"true\""),
          text.replace("5000", "499"),
          "{}"
        }) {
      Files.writeString(file, bad);
      assertThrows(java.io.IOException.class, () -> CargoRules.load(file));
      assertEquals(bad, Files.readString(file));
    }
  }
 @Test void existingAdministratorDisableIsNeverSilentlyOverwritten()throws Exception{var file=directory.resolve("cargo.json");CargoRules.load(file);String disabled=Files.readString(file).replace("true","false");Files.writeString(file,disabled);assertFalse(CargoRules.load(file).enabled());assertEquals(disabled,Files.readString(file));}
}
