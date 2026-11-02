package com.mohymods.kingdomsmp.commerce;
import com.google.gson.*;
import com.mohymods.kingdomsmp.building.FishermansWharfBlueprint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class CommerceCatalogMigrationTest {
 @TempDir Path dir;
 @Test void additiveWharfListingPreservesOldServerSettingsBytesAndFixedTierGate()throws Exception{
  var path=dir.resolve("commerce.json");CommerceCatalog.load(path);var old=JsonParser.parseString(Files.readString(path)).getAsJsonObject();old.getAsJsonObject("blueprintPrices").remove("fishermans_wharf");old.getAsJsonObject("blueprintPrices").addProperty("farmers_house",1100);Files.writeString(path,new Gson().toJson(old));var before=Files.readString(path);var catalog=CommerceCatalog.load(path);assertEquals(before,Files.readString(path));assertEquals(1100,catalog.building("kingdom_smp:farmers_house").price());assertEquals(3,catalog.building(FishermansWharfBlueprint.ID.toString()).tier());assertEquals(1750,catalog.building(FishermansWharfBlueprint.ID.toString()).price());assertTrue(CommerceCatalog.marketBuilding(FishermansWharfBlueprint.ID));
 }
 @Test void additiveAerodromeListingPreservesOldBytesAndCustomPrice()throws Exception{
  var path=dir.resolve("aerodrome.json");CommerceCatalog.load(path);var old=JsonParser.parseString(Files.readString(path)).getAsJsonObject();old.getAsJsonObject("blueprintPrices").remove("royal_aerodrome");Files.writeString(path,new Gson().toJson(old));var before=Files.readString(path);var catalog=CommerceCatalog.load(path);assertEquals(before,Files.readString(path));assertEquals(7500,catalog.building("kingdom_smp:royal_aerodrome").price());assertEquals(6,catalog.building("kingdom_smp:royal_aerodrome").tier());
  old.getAsJsonObject("blueprintPrices").addProperty("royal_aerodrome",8500);Files.writeString(path,new Gson().toJson(old));before=Files.readString(path);assertEquals(8500,CommerceCatalog.load(path).building("kingdom_smp:royal_aerodrome").price());assertEquals(before,Files.readString(path));
 }

}
