package com.mohymods.kingdomsmp.admin;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class SandboxCommandsTest {
 @TempDir Path dir;
 @Test void sandboxRequiresExplicitStrictIsolatedWorldOptInAndNeverRewritesInput()throws Exception {
  var file=dir.resolve("sandbox.json");assertFalse(SandboxCommands.enabled(file));assertFalse(Files.exists(file));
  Files.writeString(file,"{\"schema\":1,\"enabled\":true,\"scope\":\"isolated_test_world\"}");assertTrue(SandboxCommands.enabled(file));
  for(String bad:new String[]{"{\"schema\":1,\"enabled\":\"true\",\"scope\":\"isolated_test_world\"}","{\"schema\":1,\"enabled\":true,\"scope\":\"live_world\"}","{\"enabled\":true}"}){Files.writeString(file,bad);assertThrows(java.io.IOException.class,()->SandboxCommands.enabled(file));assertEquals(bad,Files.readString(file));}
 }
 @Test void onlyPlainResourcesNotCurrencyBlueprintsOrEquipmentCanBeRequested(){
  assertTrue(SandboxCommands.allowedItem("minecraft:diamond"));assertTrue(SandboxCommands.allowedItem("minecraft:cobblestone"));
  for(String denied:new String[]{"kingdom_smp:gold_coin","kingdom_smp:town_hall_blueprint","minecraft:netherite_pickaxe","minecraft:enchanted_book","minecraft:command_block"})assertFalse(SandboxCommands.allowedItem(denied));
 }
}
