package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
class ProfessionInspectionGeometryTest {
    @Test void scatteredRouteCellsDoNotSatisfyConnectedAccess(){
        Set<BlockPos> cells=new HashSet<>();for(int i=0;i<8;i++)cells.add(new BlockPos(i*2,64,0));
        assertEquals(1,ProfessionInspectionService.largestConnected(cells));
        for(int i=0;i<8;i++)cells.add(new BlockPos(i,64,0));
        assertEquals(9,ProfessionInspectionService.largestConnected(cells));
        assertEquals(12,cells.size());
    }
    @Test void safeOneBlockGradesConnectButVerticalStacksDoNot(){
        Set<BlockPos> cells=new HashSet<>();for(int i=0;i<8;i++)cells.add(new BlockPos(i,64+i/2,0));
        cells.add(new BlockPos(0,72,0));assertEquals(8,ProfessionInspectionService.largestConnected(cells));
        assertEquals(0,ProfessionInspectionService.largestConnected(Set.of()));
    }
}
