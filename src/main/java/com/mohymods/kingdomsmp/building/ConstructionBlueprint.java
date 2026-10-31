package com.mohymods.kingdomsmp.building;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Immutable versioned definition shared by previews, validation and construction. */
public record ConstructionBlueprint(ResourceLocation id, int version, int width, int depth, int height,
        List<BuilderHutBlueprint.Placement> blocks, BlockPos home, BlockPos workstation, BlockPos board) {
    public ConstructionBlueprint {
        Objects.requireNonNull(id);
        if (version < 1 || width < 3 || depth < 3 || height < 3 || width > 64 || depth > 64 || height > 64) throw new IllegalArgumentException("Invalid blueprint size");
        blocks = List.copyOf(blocks); home = home.immutable(); workstation = workstation.immutable(); board = board.immutable();
        if (blocks.isEmpty() || blocks.size() > 32768) throw new IllegalArgumentException("Invalid blueprint block count");
        Set<BlockPos> positions = new HashSet<>();
        for (var b : blocks) {
            BlockPos p = b.offset();
            if (p.getX()<0 || p.getX()>=width || p.getZ()<0 || p.getZ()>=depth || p.getY()<0 || p.getY()>=height
                    || b.state().isAir() || !positions.add(p)) throw new IllegalArgumentException("Invalid blueprint block: " + p);
        }
    }
    public int width(Rotation r) { return r == Rotation.CLOCKWISE_90 || r == Rotation.COUNTERCLOCKWISE_90 ? depth : width; }
    public int depth(Rotation r) { return r == Rotation.CLOCKWISE_90 || r == Rotation.COUNTERCLOCKWISE_90 ? width : depth; }
    public BlockPos rotate(BlockPos p, Rotation r) {
        return switch (r) {
            case NONE -> p.immutable();
            case CLOCKWISE_90 -> new BlockPos(depth-1-p.getZ(),p.getY(),p.getX());
            case CLOCKWISE_180 -> new BlockPos(width-1-p.getX(),p.getY(),depth-1-p.getZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(p.getZ(),p.getY(),width-1-p.getX());
        };
    }
    public List<BuilderHutBlueprint.Placement> blocks(Rotation r) {
        return blocks.stream().map(b -> new BuilderHutBlueprint.Placement(rotate(b.offset(),r),b.state().rotate(r))).toList();
    }
    public BlockPos workPost(BlockPos origin, Rotation r) {
        return origin.offset(rotate(new BlockPos(width/2,0,-1),r));
    }
    public static boolean matches(BlockState current, BlockState expected) {
        return BuilderHutService.matchesRequired(current, expected);
    }
}
