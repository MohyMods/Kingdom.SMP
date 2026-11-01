package com.mohymods.kingdomsmp.building;

import com.mohymods.kingdomsmp.persistence.ConstructionStore.Project;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import java.util.*;

/** Live world adapter. No terrain clearing, blueprint mutation, dynamic replanning or chunk tickets. */
public final class ConstructionFoundations {
    private ConstructionFoundations() { }
    public static boolean fillable(net.minecraft.world.level.block.state.BlockState state) {
        return state.isAir()||state.is(Blocks.SNOW)||state.is(Blocks.WATER);
    }
    public static Project propose(ServerLevel level,Project proposed) {
        if(proposed.blueprint().toString().equals(BuilderHutBlueprint.ID))return proposed;
        var b=BlueprintRegistry.get(proposed.blueprint(),proposed.blueprintVersion());
        var plan=inspect(level,proposed);
        if(!plan.valid())throw new IllegalStateException(plan.denial()+" at "+proposed.origin().offset(b.rotate(plan.problem(),proposed.rotation())).toShortString()+". Nothing changed.");
        if(plan.cells().size()+b.blocks().size()>32768)throw new IllegalStateException("Combined foundation and building exceed construction limits");
        return proposed.withFoundations(plan.cells());
    }
    public static FoundationPlan.Result inspect(ServerLevel level,Project proposed) {
        var b=BlueprintRegistry.get(proposed.blueprint(),proposed.blueprintVersion());
        return FoundationPlan.design(b.width(),b.depth(),relative->{
            var pos=proposed.origin().offset(b.rotate(relative,proposed.rotation()));
            if(level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos))return FoundationPlan.Cell.OUTSIDE;
            if(!level.hasChunkAt(pos))return FoundationPlan.Cell.UNLOADED;
            var state=level.getBlockState(pos);
            if(fillable(state))return FoundationPlan.Cell.FILL;
            if(!state.hasBlockEntity()&&state.isFaceSturdy(level,pos,Direction.UP))return FoundationPlan.Cell.ANCHOR;
            return FoundationPlan.Cell.BLOCKED;
        });
    }
    public static int depth(Project p) { return p.foundations().stream().mapToInt(pos->-pos.getY()).max().orElse(0); }
    public static List<BuilderHutBlueprint.Placement> blocks(Project p) {
        var b=BlueprintRegistry.get(p.blueprint(),p.blueprintVersion());
        if(p.foundations().isEmpty())return b.blocks(p.rotation());
        List<BuilderHutBlueprint.Placement> blocks=new ArrayList<>(p.foundations().size()+b.blocks().size());
        for(var cell:p.foundations())blocks.add(new BuilderHutBlueprint.Placement(b.rotate(cell,p.rotation()),Blocks.COBBLESTONE.defaultBlockState()));
        blocks.addAll(b.blocks(p.rotation()));return List.copyOf(blocks);
    }
    /** Recheck the saved cells and their original supporting columns, never recalculate a different plan. */
    public static String validate(ServerLevel level,Project p,boolean resuming) {
        var b=BlueprintRegistry.get(p.blueprint(),p.blueprintVersion());var cells=new HashSet<>(p.foundations());
        for(var cell:cells) {
            if(cell.getX()>=b.width()||cell.getZ()>=b.depth())return "Saved foundation does not match its versioned footprint";
            var pos=p.origin().offset(b.rotate(cell,p.rotation()));
            if(level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.hasChunkAt(pos))return "Saved foundation is outside world limits or unloaded";
            var state=level.getBlockState(pos);
            if(!fillable(state)&&!(resuming&&state.equals(Blocks.COBBLESTONE.defaultBlockState())))return "Foundation site changed at "+pos.toShortString()+"; no terrain or property is replaced";
        }
        for(int z=0;z<b.depth();z++)for(int x=0;x<b.width();x++) {
            int down=1;while(cells.contains(new BlockPos(x,-down,z)))down++;
            var anchor=p.origin().offset(b.rotate(new BlockPos(x,-down,z),p.rotation()));
            if(level.isOutsideBuildHeight(anchor)||!level.hasChunkAt(anchor))return "Foundation anchor is outside build height or unloaded";
            var state=level.getBlockState(anchor);
            if(!state.isFaceSturdy(level,anchor,Direction.UP)||!cells.isEmpty()&&state.hasBlockEntity())return "Original terrain support changed at "+anchor.toShortString()+"; saved foundations are not expanded";
        }
        return null;
    }
}
