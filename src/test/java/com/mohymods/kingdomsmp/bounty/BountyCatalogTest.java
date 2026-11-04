package com.mohymods.kingdomsmp.bounty;
import com.mohymods.kingdomsmp.commerce.CommerceCatalog;
import com.mohymods.kingdomsmp.building.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class BountyCatalogTest {
 @TempDir Path dir;
 @BeforeAll static void bootstrap(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}
 @Test void existingFourBuildingConfigIsPreservedAndBountyUsesAuthorizedNumericalDefault()throws Exception{var file=dir.resolve("commerce.json");CommerceCatalog.load(file);var object=com.google.gson.JsonParser.parseString(Files.readString(file)).getAsJsonObject();object.getAsJsonObject("blueprintPrices").remove("royal_bounty_hall");object.getAsJsonObject("blueprintPrices").addProperty("farmers_house",1800);String original=new com.google.gson.Gson().toJson(object);Files.writeString(file,original);var loaded=CommerceCatalog.load(file);assertEquals(original,Files.readString(file));assertEquals(1800,loaded.building("kingdom_smp:farmers_house").price());assertEquals(1500,loaded.building(RoyalBountyHallBlueprint.ID.toString()).price());assertEquals(3,loaded.building(RoyalBountyHallBlueprint.ID.toString()).tier());assertTrue(CommerceCatalog.marketBuilding(RoyalBountyHallBlueprint.ID));}
 @Test void originalHallHasFullRoofDistinctPositionsAndClearResidentEntranceInEveryRotation(){var b=RoyalBountyHallBlueprint.create();assertEquals(17,b.width());assertEquals(15,b.depth());assertEquals(14,b.height());for(var rotation:net.minecraft.world.level.block.Rotation.values()){var blocks=b.blocks(rotation);assertEquals(blocks.size(),blocks.stream().map(BuilderHutBlueprint.Placement::offset).distinct().count());var home=b.rotate(b.home(),rotation);assertTrue(blocks.stream().noneMatch(p->(p.offset().equals(home)||p.offset().equals(home.above()))&&!p.state().isAir()),"Registered NPC home needs two completely air blocks, including no carpet");assertTrue(blocks.stream().anyMatch(p->p.offset().getY()==13));}assertEquals("Garrick Thornfield",BlueprintRegistry.defaultResident(b.id()));assertEquals("Bounty Master",BlueprintRegistry.occupation(b.id()));}
}
