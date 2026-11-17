package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class LibraryQuestRulesTest {
 @TempDir Path directory;
 @Test void quantitiesRemainBoundedAndOptionalShelfDefaultNeverOverwritesExistingFile()throws Exception{var path=directory.resolve("library.json");var initial=LibraryQuestRules.load(path);assertEquals(4,initial.enchantedItems());assertEquals(12,initial.bookshelves());assertEquals(4,initial.enchantedBooks());assertEquals(6,initial.catalogueEntries());Files.writeString(path,"{\"schema\":1,\"enchantedItems\":6}");var saved=Files.readString(path);assertEquals(6,LibraryQuestRules.load(path).enchantedItems());assertEquals(12,LibraryQuestRules.load(path).bookshelves());assertEquals(saved,Files.readString(path));for(var value:new String[]{"0","37","1.5","\"4\""}){Files.writeString(path,"{\"schema\":1,\"enchantedItems\":"+value+"}");assertThrows(IOException.class,()->LibraryQuestRules.load(path));}}
 @Test void onlyVerifiedOrdinaryOutputsAndKnownNativePurposesAreAccepted(){assertTrue(NativeOutputQuests.ordinaryLibrary("minecraft:iron_pickaxe"));assertTrue(NativeOutputQuests.ordinaryLibrary("minecraft:diamond_chestplate"));assertTrue(NativeOutputQuests.ordinaryLibrary("minecraft:enchanted_book"));assertFalse(NativeOutputQuests.ordinaryLibrary("foreign:iron_pickaxe"));assertFalse(NativeOutputQuests.ordinaryLibrary("minecraft:netherite_pickaxe"));assertFalse(NativeOutputQuests.ordinaryLibrary("minecraft:diamond_block"));assertFalse(NativeOutputQuests.contains(null));assertThrows(IllegalArgumentException.class,()->NativeOutputQuests.get("kingdom_smp:librarian/m08"));}
}
