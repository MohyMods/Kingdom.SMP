package com.mohymods.kingdomsmp.building;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class RoyalAerodromeBlueprintTest {
 @Test void allRotationsHaveAnOpenApronHangarAndFurnishedAccessibleOffice(){
  var b=RoyalAerodromeBlueprint.create();
  assertEquals(6,BlueprintRegistry.requiredTier(b.id()));
  for(var rotation:Rotation.values()){
   var at=new HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
   for(var c:b.blocks(rotation)){
    assertNull(at.put(c.offset(),c.state()));assertTrue(c.offset().getX()>=0&&c.offset().getX()<b.width(rotation));assertTrue(c.offset().getZ()>=0&&c.offset().getZ()<b.depth(rotation));assertTrue(c.offset().getY()>=0&&c.offset().getY()<b.height());
   }
   for(int x=0;x<b.width(rotation);x++)for(int z=0;z<b.depth(rotation);z++)assertNotNull(at.get(new BlockPos(x,0,z)));
   for(int x=8;x<=18;x++)for(int y=1;y<6;y++)assertNull(at.get(b.rotate(new BlockPos(x,y,12),rotation)));
   for(int y=1;y<=2;y++)assertNull(at.get(b.rotate(new BlockPos(20,y,23),rotation)));
   assertNull(at.get(b.rotate(b.home(),rotation)));assertNull(at.get(b.rotate(b.home().above(),rotation)));
   assertTrue(at.get(b.rotate(b.board(),rotation)).is(Blocks.LECTERN));
   for(var block:List.of(Blocks.CARTOGRAPHY_TABLE,Blocks.SMITHING_TABLE,Blocks.BARREL,Blocks.LANTERN,Blocks.POTTED_FERN,Blocks.DEEPSLATE_TILES))assertTrue(at.values().stream().anyMatch(s->s.is(block)));
  }
 }
}
