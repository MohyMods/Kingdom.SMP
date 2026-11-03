package com.mohymods.kingdomsmp.profession;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CraftDeliveryRulesTest {
    @TempDir Path directory;
    @Test void approvedQuantitiesAndOverridesArePreservedExactly()throws Exception{
        var file=directory.resolve("craft.json");assertEquals(CraftDeliveryQuests.defaults(CraftDeliveryQuests.MASONRY),CraftDeliveryRules.load(file).inputs(CraftDeliveryQuests.MASONRY));
        String configured=Files.readString(file).replace(": 32",": 16");Files.writeString(file,configured);
        assertEquals(16,CraftDeliveryRules.load(file).inputs(CraftDeliveryQuests.MASONRY).get("minecraft:stone_brick_stairs"));assertEquals(configured,Files.readString(file));
    }
    @Test void unknownMaterialsFractionalZeroAndUnreachableInventoryTotalsFailClosed()throws Exception{
        var file=directory.resolve("craft.json");CraftDeliveryRules.load(file);String original=Files.readString(file);
        for(String n:new String[]{"0","-1","1.5","2305","1200","\"32\""}){
            Files.writeString(file,original.replace(": 32",": "+n));assertThrows(IOException.class,()->CraftDeliveryRules.load(file));
        }
        Files.writeString(file,original.replace("minecraft:stone_brick_stairs","minecraft:diamond"));assertThrows(IOException.class,()->CraftDeliveryRules.load(file));
    }
}
