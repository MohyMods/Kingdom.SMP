package com.mohymods.kingdomsmp.fishing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
class FishingMarketRulesTest {
 @TempDir Path dir;
 @Test void defaultsReloadExactlyAndRetainOperatorBalance()throws Exception{
  var file=dir.resolve("fishing.json");var first=FishingMarketRules.load(file);assertEquals(6,first.orders().size());assertEquals(first,FishingMarketRules.load(file));
  Files.writeString(file,Files.readString(file).replace("1500","1800"));assertEquals(1800,FishingMarketRules.load(file).rodPrice());assertThrows(UnsupportedOperationException.class,()->first.orders().clear());
 }
 @Test void invalidOrPartialEconomyConfigurationFailsClosedWithoutReplacement()throws Exception{
  var file=dir.resolve("fishing.json");FishingMarketRules.load(file);var original=Files.readString(file);
  for(var broken:new String[]{original.replace("1500","1.5"),original.replace("1500","0"),original.replace("\"bluegill\"","\"unregistered_fish\""),original.replace("\"maximumBuy\": 4","\"maximumBuy\": 0")}){Files.writeString(file,broken);assertThrows(IOException.class,()->FishingMarketRules.load(file));assertEquals(broken,Files.readString(file));}
 }
}
