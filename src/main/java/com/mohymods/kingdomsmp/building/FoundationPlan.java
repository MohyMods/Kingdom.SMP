package com.mohymods.kingdomsmp.building;

import net.minecraft.core.BlockPos;
import java.util.*;

/** Site-specific immutable cells, ordered bottom-up before the original versioned building. */
public final class FoundationPlan {
    public enum Cell { ANCHOR, FILL, BLOCKED, UNLOADED, OUTSIDE }
    @FunctionalInterface public interface Probe { Cell at(BlockPos relative); }
    public record Result(List<BlockPos> cells,String denial,BlockPos problem) {
        public Result { cells=List.copyOf(cells); }
        public boolean valid() { return denial.isEmpty(); }
    }
    private FoundationPlan() { }
    public static Result design(int width,int depth,Probe probe) {
        if(width<3||depth<3||width>64||depth>64)throw new IllegalArgumentException("Invalid foundation footprint");
        List<BlockPos> cells=new ArrayList<>();
        for(int z=0;z<depth;z++)for(int x=0;x<width;x++)for(int down=1;down<=5;down++) {
            var pos=new BlockPos(x,-down,z);var state=Objects.requireNonNull(probe.at(pos));
            if(state==Cell.ANCHOR)break;
            String denied=switch(state){case OUTSIDE->"Foundation exceeds the world border or build height";case UNLOADED->"Foundation chunks must already be loaded";case BLOCKED->"Foundation would replace terrain or property";default->down==5?"Terrain support is deeper than four foundation blocks":"";};
            if(!denied.isEmpty())return new Result(List.of(),denied,pos);
            cells.add(pos);
        }
        cells.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY).thenComparingInt(BlockPos::getZ).thenComparingInt(BlockPos::getX));
        return new Result(cells,"",null);
    }
}
