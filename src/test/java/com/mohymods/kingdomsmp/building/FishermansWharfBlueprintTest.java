package com.mohymods.kingdomsmp.building;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class FishermansWharfBlueprintTest {
 @Test void rotatedWharfRetainsCompleteQuayClearResidentAndFurnishedHouse(){
  var b=FishermansWharfBlueprint.create();
  for(var rotation:Rotation.values()){
   var blocks=b.blocks(rotation);var at=new HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
   for(var cell:blocks){assertNull(at.put(cell.offset(),cell.state()));assertTrue(cell.offset().getX()>=0&&cell.offset().getX()<b.width(rotation));assertTrue(cell.offset().getZ()>=0&&cell.offset().getZ()<b.depth(rotation));assertTrue(cell.offset().getY()>=0&&cell.offset().getY()<b.height());}
   for(int x=0;x<b.width(rotation);x++)for(int z=0;z<b.depth(rotation);z++)assertNotNull(at.get(new BlockPos(x,0,z)));
   assertNull(at.get(b.rotate(b.home(),rotation)));assertNull(at.get(b.rotate(b.home().above(),rotation)));
   assertTrue(at.get(b.rotate(b.board(),rotation)).is(Blocks.LECTERN));
   for(var required:List.of(Blocks.SPRUCE_DOOR,Blocks.LANTERN,Blocks.BARREL,Blocks.SMOKER,Blocks.CRAFTING_TABLE,Blocks.POTTED_FERN,Blocks.DEEPSLATE_TILE_STAIRS))assertTrue(blocks.stream().anyMatch(p->p.state().is(required)));
  }
 }
}
