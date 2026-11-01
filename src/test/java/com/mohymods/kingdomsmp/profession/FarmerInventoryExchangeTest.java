package com.mohymods.kingdomsmp.profession;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class FarmerInventoryExchangeTest {
 @BeforeAll static void bootstrap(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}
 List<ItemStack> empty(){var list=new ArrayList<ItemStack>();for(int i=0;i<36;i++)list.add(ItemStack.EMPTY);return list;}
 int count(List<ItemStack> items,Item item){return items.stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum();}
 @Test void eggsMilkReturnOnlyExactBucketsWithoutMutatingOriginal(){var before=empty();before.set(0,new ItemStack(Items.EGG,16));before.set(1,new ItemStack(Items.EGG,16));before.set(2,new ItemStack(Items.EGG,16));before.set(3,new ItemStack(Items.EGG,16));for(int n=4;n<36;n++)before.set(n,new ItemStack(Items.MILK_BUCKET));var after=FarmerInventoryExchange.plan(before,Map.of(Items.EGG,64,Items.MILK_BUCKET,32),Map.of(Items.BUCKET,32));assertEquals(0,count(after,Items.EGG));assertEquals(0,count(after,Items.MILK_BUCKET));assertEquals(32,count(after,Items.BUCKET));assertEquals(32,count(before,Items.MILK_BUCKET));}
 @Test void soupReturnUsesFreedSlotsAndDoesNotCreateExtraBowls(){var before=empty();before.set(0,new ItemStack(Items.BEETROOT_SOUP));before.set(1,new ItemStack(Items.BOWL,63));for(int n=2;n<36;n++)before.set(n,new ItemStack(Items.STONE,64));var after=FarmerInventoryExchange.plan(before,Map.of(Items.BEETROOT_SOUP,1),Map.of(Items.BOWL,1));assertEquals(64,count(after,Items.BOWL));assertEquals(0,count(after,Items.BEETROOT_SOUP));assertEquals(1,count(before,Items.BEETROOT_SOUP));}
 @Test void incompleteFoodOrInsufficientReturnSpaceLeavesOriginalUntouched(){var before=empty();for(int n=0;n<36;n++)before.set(n,new ItemStack(Items.STONE,64));before.set(0,new ItemStack(Items.BREAD,64));assertThrows(IllegalStateException.class,()->FarmerInventoryExchange.plan(before,Map.of(Items.BREAD,1),Map.of(Items.BOWL,64)));assertEquals(64,count(before,Items.BREAD));assertThrows(IllegalStateException.class,()->FarmerInventoryExchange.plan(before,Map.of(Items.CAKE,1),Map.of()));}
 @Test void approvedFamiliesAndContainerRulesRejectForeignPayload(){var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("minecraft:bread",32);FarmerDeliveries.validate("kingdom_smp:farmer/a07",tag);tag.putInt("minecraft:diamond",1);assertThrows(IllegalArgumentException.class,()->FarmerDeliveries.validate("kingdom_smp:farmer/a07",tag));assertEquals(Map.of("minecraft:bucket",32,"minecraft:bowl",16),FarmerDeliveries.containers(Map.of("minecraft:milk_bucket",32,"minecraft:beetroot_soup",16)));}
}
