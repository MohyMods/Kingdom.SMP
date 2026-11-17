package com.mohymods.kingdomsmp.gametest;
import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.npc.KingdomNpcs;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("kingdom_smp_geometry_tests") @PrefixGameTestTemplate(false)
public final class PurchasedGeometryTests {
 @GameTest(template="empty",timeoutTicks=100)
 public static void rotatedWorkshops(GameTestHelper h){
  var level=h.getLevel();int row=0;
  for(var id:java.util.List.of(ProfessionBlueprints.BUILDER,ProfessionBlueprints.LUMBERJACK)){
   var b=BlueprintRegistry.get(id,1);int col=0;
   for(var rotation:Rotation.values()){
    var origin=h.absolutePos(new BlockPos(col++*20,1,row*20));
    for(var block:b.blocks(rotation))level.setBlock(origin.offset(block.offset()),block.state(),18);
    for(var block:b.blocks(rotation)){var p=origin.offset(block.offset());h.assertTrue(level.getBlockState(p).canSurvive(level,p),id+" "+rotation+" furnishing support "+p);}
    var home=origin.offset(b.rotate(b.home(),rotation));h.assertTrue(level.getBlockState(home).isAir()&&level.getBlockState(home.above()).isAir(),"Clear resident home in every rotation");
    h.assertTrue(level.getBlockState(origin.offset(b.rotate(b.board(),rotation))).is(Blocks.LECTERN),"Usable workshop service board");
    var npc=KingdomNpcs.RESIDENT.get().create(level);npc.moveTo(home.getX()+.5,home.getY(),home.getZ()+.5);
    h.assertTrue(level.getBlockState(home.below()).isFaceSturdy(level,home.below(),net.minecraft.core.Direction.UP),"Resident stands on actual solid floor");npc.setOnGround(true);
    var path=npc.getNavigation().createPath(origin.offset(b.rotate(new BlockPos(b.width()/2,1,2),rotation)),0);
    h.assertTrue(path!=null&&path.canReach(),"Native workshop entrance navigation "+id+" "+rotation);
   }row++;
  }
  h.succeed();com.mojang.logging.LogUtils.getLogger().info("GEOMETRY_TEST_ASSERTIONS_PASSED: Builder and Lumberjack all four rotations, actual furnishing support, resident spaces, service lecterns and entrance navigation");
 }
}
