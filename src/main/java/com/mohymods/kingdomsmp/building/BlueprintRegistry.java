package com.mohymods.kingdomsmp.building;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
public final class BlueprintRegistry {
    public static final ResourceLocation HUT=ResourceLocation.fromNamespaceAndPath("kingdom_smp","builders_hut");
    private static final Map<ResourceLocation,ConstructionBlueprint> DEFINITIONS=new HashMap<>();
    private BlueprintRegistry(){}
    public static ConstructionBlueprint get(ResourceLocation id,int version){
        if(version!=1)throw new IllegalArgumentException("Unsupported blueprint version");
        return DEFINITIONS.computeIfAbsent(id,key->{
            if(key.equals(FoundersLodgeBlueprint.ID))return FoundersLodgeBlueprint.create();
            if(key.equals(TownHallBlueprint.ID))return TownHallBlueprint.create();
            if(key.equals(HUT)){var h=BuilderHutBlueprint.create();return new ConstructionBlueprint(HUT,1,h.width(net.minecraft.world.level.block.Rotation.NONE),h.depth(net.minecraft.world.level.block.Rotation.NONE),BuilderHutBlueprint.HEIGHT,h.placements(),new net.minecraft.core.BlockPos(5,1,5),new net.minecraft.core.BlockPos(5,1,5),new net.minecraft.core.BlockPos(5,1,3));}
            throw new IllegalArgumentException("Unsupported blueprint definition");
        });
    }
    public static ConstructionBlueprint lodge(){return get(FoundersLodgeBlueprint.ID,1);}
    public static String title(ResourceLocation id){return id.equals(HUT)?"Builder's Hut":id.equals(TownHallBlueprint.ID)?"Royal Town Hall":"Founders' Lodge";}
}
