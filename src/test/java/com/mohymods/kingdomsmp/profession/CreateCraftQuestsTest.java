package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class CreateCraftQuestsTest {
 @TempDir Path dir;
 @Test void refinedCraftsRejectDecompressionAndWrongCounts(){
  assertTrue(CreateCraftQuests.primary("create:andesite_alloy",1,Map.of("minecraft:andesite",2,"minecraft:iron_nugget",2)));
  assertTrue(CreateCraftQuests.primary("create:andesite_alloy",1,Map.of("minecraft:andesite",2,"create:zinc_nugget",2)));
  assertFalse(CreateCraftQuests.primary("create:andesite_alloy",9,Map.of("create:andesite_alloy_block",1)));
  assertTrue(CreateCraftQuests.primary("create:shaft",8,Map.of("create:andesite_alloy",2)));
  assertFalse(CreateCraftQuests.primary("create:shaft",64,Map.of("create:andesite_alloy",2)));
  assertTrue(CreateCraftQuests.primary("create:large_cogwheel",1,Map.of("create:cogwheel",1,"planks",1)));
  assertFalse(CreateCraftQuests.primary("create:cogwheel",1,Map.of("create:large_cogwheel",1)));
 }
 @Test void defaultsAreAdditiveWithOriginalWorldConfigAndApprovedDependency()throws Exception{
  var file=dir.resolve("quests.json");Files.writeString(file,"{\"schema\":1,\"deliveries\":{\"kingdom_smp:builder/a05\":{\"minecraft:stone_brick_stairs\":32,\"minecraft:stone_brick_slab\":32}}}");var original=Files.readString(file);var rules=CraftDeliveryRules.load(file);
  assertEquals(Map.of("create:andesite_alloy",32),rules.inputs(CreateCraftQuests.ALLOY));assertEquals(Map.of("create:shaft",32,"create:cogwheel",16,"create:large_cogwheel",8),rules.inputs(CreateCraftQuests.COMPONENTS));assertEquals(original,Files.readString(file));
  assertEquals(Set.of(CreateCraftQuests.ALLOY),CraftDeliveryQuests.get(CreateCraftQuests.COMPONENTS).prerequisites());
 }
}
