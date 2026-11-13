package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.nio.file.*;
import java.io.IOException;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class BrewMixedQuotaTest {
 @TempDir Path folder;
 static CompoundTag potion(String item,String kind){var t=new CompoundTag();t.putString("id","minecraft:"+item);t.putInt("count",1);var c=new CompoundTag();var p=new CompoundTag();p.putString("potion","minecraft:"+kind);c.put("minecraft:potion_contents",p);t.put("components",c);return t;}
 @Test void strictEqualQuotasFindLaterNightVisionWithoutConsumingMonoculture(){List<CompoundTag> items=new ArrayList<>();for(int n=0;n<18;n++)items.add(potion("potion","water_breathing"));assertTrue(BrewQuests.select(BrewQuests.UNDERWATER,items,16).isEmpty());for(int n=0;n<9;n++)items.add(potion("potion","night_vision"));var picked=BrewQuests.select(BrewQuests.UNDERWATER,items,16);assertEquals(16,picked.size());assertEquals(8,picked.stream().filter(n->BrewQuests.kind(items.get(n)).equals("minecraft:night_vision")).count());assertThrows(IllegalArgumentException.class,()->BrewQuests.goal(BrewQuests.UNDERWATER,15));}
 @Test void beneficialNativeContainerConversionCannotUseLingeringHarmfulOrWrongRecipe(){BrewQuests.validateEncoded(BrewQuests.SPLASH,potion("splash_potion","healing"));assertThrows(IllegalArgumentException.class,()->BrewQuests.validateEncoded(BrewQuests.SPLASH,potion("potion","healing")));assertThrows(IllegalArgumentException.class,()->BrewQuests.validateEncoded(BrewQuests.SPLASH,potion("lingering_potion","healing")));assertThrows(IllegalArgumentException.class,()->BrewQuests.validateEncoded(BrewQuests.SPLASH,potion("splash_potion","poison")));assertTrue(BrewQuests.route("minecraft:healing","minecraft:gunpowder","minecraft:healing","minecraft:potion").isEmpty());assertTrue(BrewQuests.route("minecraft:healing","minecraft:gunpowder","minecraft:healing","minecraft:splash_potion").isPresent());}
 @Test void oldSingleFamilyConfigRemainsUntouchedAndNewUnderwaterDefaultsAreBalanced()throws Exception{var f=folder.resolve("old.json");String old="{\"schema\":1,\"goals\":{\"kingdom_smp:alchemist/a02\":6}}";Files.writeString(f,old);assertEquals(16,BrewQuestRules.load(f).get(BrewQuests.UNDERWATER));assertEquals(6,BrewQuestRules.load(f).get("kingdom_smp:alchemist/a02"));assertEquals(old,Files.readString(f));Files.writeString(f,"{\"schema\":1,\"goals\":{\"kingdom_smp:alchemist/j03\":15}}");assertThrows(IOException.class,()->BrewQuestRules.load(f));}
}
