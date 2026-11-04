package com.mohymods.kingdomsmp.building;
import static org.junit.jupiter.api.Assertions.*;
import com.mohymods.kingdomsmp.profession.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import org.junit.jupiter.api.*;
class ProfessionPremisesTest {
 @BeforeAll static void bootstrap(){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();}
 @Test void allNineOriginalPremisesRespectApprovedGatesAndStableProfessionIdentity(){
  var gates=Map.of(Profession.BLACKSMITH,3,Profession.MINER,3,Profession.MERCHANT,3,Profession.ADVENTURER,3,Profession.ENGINEER,4,Profession.MAGE,4,Profession.LIBRARIAN,4,Profession.ALCHEMIST,5,Profession.MECHANIC,6);assertEquals(9,ProfessionPremises.definitions().size());Set<String> names=new HashSet<>();
  for(var d:ProfessionPremises.definitions()){assertEquals(d.profession().buildingId(),d.id().toString());assertEquals(gates.get(d.profession()),BlueprintRegistry.requiredTier(d.id()));assertEquals(d.title(),BlueprintRegistry.title(d.id()));assertTrue(names.add(d.resident()));assertEquals(ApprovedProfessionContent.get(d.profession().questId(ProfessionRank.APPRENTICE,1)).minimumTier(),d.tier());assertEquals(4,d.stations().size());}
 }
 @Test void rotatedBuildingsHaveCompleteFoundationsRoofUsableDoorAisleResidentAndFourWorkstations(){
  for(var d:ProfessionPremises.definitions()){var b=BlueprintRegistry.get(d.id(),1);for(var r:Rotation.values()){Map<BlockPos,net.minecraft.world.level.block.state.BlockState> at=new HashMap<>();for(var cell:b.blocks(r)){assertNull(at.put(cell.offset(),cell.state()));assertTrue(cell.offset().getX()>=0&&cell.offset().getX()<b.width(r));assertTrue(cell.offset().getZ()>=0&&cell.offset().getZ()<b.depth(r));assertTrue(cell.offset().getY()>=0&&cell.offset().getY()<b.height());}
   for(int x=0;x<b.width(r);x++)for(int z=0;z<b.depth(r);z++)assertNotNull(at.get(new BlockPos(x,0,z)));
   assertTrue(at.get(b.rotate(new BlockPos(7,1,0),r)).getBlock() instanceof DoorBlock);assertNull(at.get(b.rotate(b.home(),r)));assertNull(at.get(b.rotate(b.home().above(),r)));assertTrue(at.get(b.rotate(b.board(),r)).is(Blocks.LECTERN));
   for(int z=1;z<12;z++)for(int y=1;y<=2;y++)assertNull(at.get(b.rotate(new BlockPos(7,y,z),r)),"Clear central route to resident");
   for(var station:d.stations())assertTrue(at.values().stream().anyMatch(s->s.is(station)));for(var block:List.of(Blocks.BARREL,Blocks.LANTERN,Blocks.POTTED_FERN))assertTrue(at.values().stream().anyMatch(s->s.is(block)));
  }}
 }
 @Test void realWorkstationUseDoesNotDisableTheProfessionButRemovalOrRotationDoes(){
  assertTrue(ConstructionBlueprint.matches(Blocks.CHIPPED_ANVIL.defaultBlockState(),Blocks.ANVIL.defaultBlockState()));assertTrue(ConstructionBlueprint.matches(Blocks.DAMAGED_ANVIL.defaultBlockState(),Blocks.ANVIL.defaultBlockState()));assertFalse(ConstructionBlueprint.matches(Blocks.DAMAGED_ANVIL.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,Direction.EAST),Blocks.ANVIL.defaultBlockState()));
  assertTrue(ConstructionBlueprint.matches(Blocks.WATER_CAULDRON.defaultBlockState(),Blocks.CAULDRON.defaultBlockState()));assertTrue(ConstructionBlueprint.matches(Blocks.BREWING_STAND.defaultBlockState().setValue(BrewingStandBlock.HAS_BOTTLE[0],true),Blocks.BREWING_STAND.defaultBlockState()));assertTrue(ConstructionBlueprint.matches(Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.HAS_BOOK,true),Blocks.LECTERN.defaultBlockState()));assertTrue(ConstructionBlueprint.matches(Blocks.COMPOSTER.defaultBlockState().setValue(ComposterBlock.LEVEL,8),Blocks.COMPOSTER.defaultBlockState()));assertFalse(ConstructionBlueprint.matches(Blocks.AIR.defaultBlockState(),Blocks.ANVIL.defaultBlockState()));assertFalse(ConstructionBlueprint.matches(Blocks.IRON_BLOCK.defaultBlockState(),Blocks.ANVIL.defaultBlockState()));assertFalse(ConstructionBlueprint.matches(Blocks.WATER.defaultBlockState(),Blocks.CAULDRON.defaultBlockState()));
 }
}
