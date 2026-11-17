package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.Test;
class CreateTransmissionInspectionTest {
 CreateTransmissionInspection.Running controls(long network){return new CreateTransmissionInspection.Running(new BlockPos(0,0,0),new BlockPos(1,0,0),new BlockPos(1,1,1),new BlockPos(2,0,0),new BlockPos(2,1,0),new BlockPos(3,0,0),new BlockPos(4,0,0),network,8,8,-16,8);}
 @Test void onlyReviewedPowerBitsMayChangeWithoutHidingOrientationOrOtherControls(){var r=controls(1);var state=Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.POWERED,true).setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST);assertEquals(state.setValue(BlockStateProperties.POWERED,false),CreateTransmissionInspection.snapshot(r.lever(),state,r));assertEquals(state,CreateTransmissionInspection.snapshot(r.lever().east(),state,r));assertEquals(Direction.EAST,CreateTransmissionInspection.snapshot(r.lever(),state,r).getValue(BlockStateProperties.HORIZONTAL_FACING));assertEquals(state,CreateTransmissionInspection.snapshot(r.lever(),state,null));}
 @Test void nativeFrameDigestAndApprovedRequirementsRemainBound(){assertEquals(32,controls(1).digest().length());assertNotEquals(controls(1).digest(),controls(2).digest());assertEquals(Set.of("kingdom_smp:engineer/a02","kingdom_smp:engineer/a03"),InspectionQuests.get(CreateTransmissionInspection.QUEST).prerequisites());assertEquals(4,InspectionQuests.get(CreateTransmissionInspection.QUEST).minimumTier());}
}
