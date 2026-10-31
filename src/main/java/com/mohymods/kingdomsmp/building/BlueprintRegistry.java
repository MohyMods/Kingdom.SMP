package com.mohymods.kingdomsmp.building;

import net.minecraft.resources.ResourceLocation;

/** Approved definitions only; unknown or revised saved content fails closed. */
public final class BlueprintRegistry {
    private static ConstructionBlueprint lodge;
    private BlueprintRegistry() { }
    public static ConstructionBlueprint get(ResourceLocation id,int version) {
        if(!FoundersLodgeBlueprint.ID.equals(id)||version!=1)throw new IllegalArgumentException("Unsupported blueprint definition/version");
        if(lodge==null)lodge=FoundersLodgeBlueprint.create();
        return lodge;
    }
    public static ConstructionBlueprint lodge(){return get(FoundersLodgeBlueprint.ID,1);}
}
