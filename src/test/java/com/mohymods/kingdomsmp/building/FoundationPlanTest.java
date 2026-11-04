package com.mohymods.kingdomsmp.building;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FoundationPlanTest {
    @Test void flatTerrainRetainsOriginalEmptyPrefix() {
        var plan=FoundationPlan.design(19,15,p->FoundationPlan.Cell.ANCHOR);
        assertTrue(plan.valid());assertTrue(plan.cells().isEmpty());
    }
    @Test void mixedColumnsStopAtOriginalAnchorsAndAreBottomUp() {
        var plan=FoundationPlan.design(3,3,p->-p.getY()<=p.getX()+1?FoundationPlan.Cell.FILL:FoundationPlan.Cell.ANCHOR);
        assertTrue(plan.valid());assertEquals(18,plan.cells().size());
        assertEquals(-3,plan.cells().getFirst().getY());assertEquals(-1,plan.cells().getLast().getY());
        assertTrue(plan.cells().contains(new BlockPos(2,-3,0)));assertFalse(plan.cells().contains(new BlockPos(0,-2,0)));
        assertThrows(UnsupportedOperationException.class,()->plan.cells().clear());
    }
    @Test void fourBlocksAreAllowedButFifthCannotBeSilentlyFilled() {
        var four=FoundationPlan.design(3,3,p->p.getY()==-5?FoundationPlan.Cell.ANCHOR:FoundationPlan.Cell.FILL);
        assertTrue(four.valid());assertEquals(36,four.cells().size());
        var five=FoundationPlan.design(3,3,p->FoundationPlan.Cell.FILL);
        assertFalse(five.valid());assertEquals(new BlockPos(0,-5,0),five.problem());assertTrue(five.cells().isEmpty());
    }
    @Test void propertyUnloadedChunksAndWorldLimitsRefuseAnEntirePlan() {
        for(var failure:List.of(FoundationPlan.Cell.BLOCKED,FoundationPlan.Cell.UNLOADED,FoundationPlan.Cell.OUTSIDE)){
            var plan=FoundationPlan.design(3,3,p->p.getX()==2&&p.getZ()==2?failure:FoundationPlan.Cell.ANCHOR);
            assertFalse(plan.valid());assertEquals(new BlockPos(2,-1,2),plan.problem());assertTrue(plan.cells().isEmpty());
        }
    }
}
