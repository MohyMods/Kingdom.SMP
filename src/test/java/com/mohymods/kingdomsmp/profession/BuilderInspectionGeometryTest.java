package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
class BuilderInspectionGeometryTest {
    @Test void crossingNeedsTheWholeTwelveCellGapAndBothGroundedApproaches(){
        boolean[] decks=new boolean[14],ground=new boolean[14],gap=new boolean[14];Arrays.fill(decks,true);Arrays.fill(gap,true);ground[0]=ground[13]=true;
        assertTrue(BuilderInspectionGeometry.twelveSpan(decks,ground,gap));decks[5]=false;assertFalse(BuilderInspectionGeometry.twelveSpan(decks,ground,gap));decks[5]=true;ground[13]=false;assertFalse(BuilderInspectionGeometry.twelveSpan(decks,ground,gap));ground[13]=true;gap[8]=false;assertFalse(BuilderInspectionGeometry.twelveSpan(decks,ground,gap));
    }
    @Test void sixDifferentZonesCannotAllBeOneClusterAndInspectionSearchIsBounded(){
        var groups=new ArrayList<List<BlockPos>>();for(int i=0;i<6;i++)groups.add(List.of(new BlockPos((i%3)*3,64,(i/3)*3)));
        assertTrue(BuilderInspectionGeometry.zones(groups,0,new ArrayList<>(),new int[]{4096}));assertFalse(BuilderInspectionGeometry.zones(groups,0,new ArrayList<>(),new int[]{0}));
        groups.replaceAll(row->List.of(new BlockPos(0,64,0)));assertFalse(BuilderInspectionGeometry.zones(groups,0,new ArrayList<>(),new int[]{4096}));
    }
}
