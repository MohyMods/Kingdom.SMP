package com.mohymods.kingdomsmp.equipment;
import com.mohymods.kingdomsmp.economy.EconomyStore;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class FarmerGearTest {
 @TempDir Path dir;
 @Test void exactCompoundingUsesOriginalBaseAndCeilingNotRepeatedRounding(){var g=FarmerGear.FORTUNE_X_HOE;assertEquals(25000,g.price(25000,0));assertEquals(37500,g.price(25000,1));assertEquals(56250,g.price(25000,2));assertEquals(152,g.price(101,1));assertEquals(7,g.price(3,2));assertEquals(11,g.price(3,3));assertEquals(5000,FarmerGear.JOURNEYMAN_HOE.price(5000,10));}
 @Test void impossibleFurtherPricesAreBounded(){assertThrows(IllegalStateException.class,()->FarmerGear.FORTUNE_X_HOE.price(EconomyStore.MAX_COINS,1));assertThrows(IllegalArgumentException.class,()->FarmerGear.FORTUNE_X_HOE.price(100,-1));assertThrows(IllegalArgumentException.class,()->FarmerGear.FORTUNE_X_HOE.price(100,129));}
 @Test void equipmentDefaultsAreConfigurableButCannotInventNewGearOrFreePurchases()throws Exception{var path=dir.resolve("gear.json");assertEquals(25000,FarmerGearRules.load(path).base(FarmerGear.FORTUNE_X_HOE));Files.writeString(path,Files.readString(path).replace("25000","0"));assertThrows(java.io.IOException.class,()->FarmerGearRules.load(path));}
}
