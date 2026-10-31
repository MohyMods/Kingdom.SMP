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
            if(key.equals(RoyalBankBlueprint.ID))return RoyalBankBlueprint.create();
            if(key.equals(ProfessionBlueprints.FARMER)||key.equals(ProfessionBlueprints.BUILDER)||key.equals(ProfessionBlueprints.LUMBERJACK))return ProfessionBlueprints.create(key);
            if(key.equals(HUT)){var h=BuilderHutBlueprint.create();return new ConstructionBlueprint(HUT,1,h.width(net.minecraft.world.level.block.Rotation.NONE),h.depth(net.minecraft.world.level.block.Rotation.NONE),BuilderHutBlueprint.HEIGHT,h.placements(),new net.minecraft.core.BlockPos(5,1,5),new net.minecraft.core.BlockPos(5,1,5),new net.minecraft.core.BlockPos(5,1,3));}
            throw new IllegalArgumentException("Unsupported blueprint definition");
        });
    }
    public static ConstructionBlueprint lodge(){return get(FoundersLodgeBlueprint.ID,1);}
    public static String title(ResourceLocation id){
        if(id.equals(HUT))return "Builder's Hut";
        if(id.equals(TownHallBlueprint.ID))return "Royal Town Hall";
        if(id.equals(FoundersLodgeBlueprint.ID))return "Founders' Lodge";
        if(id.equals(RoyalBankBlueprint.ID))return "Royal Bank";
        if(id.equals(ProfessionBlueprints.FARMER))return "Farmer's House";
        if(id.equals(ProfessionBlueprints.BUILDER))return "Builder's Workshop";
        if(id.equals(ProfessionBlueprints.LUMBERJACK))return "Lumberjack's Lodge";
        throw new IllegalArgumentException("Unsupported blueprint title");
    }
    public static int requiredTier(ResourceLocation id){
        if(id.equals(HUT))return 1;
        if(id.equals(RoyalBankBlueprint.ID))return 3;
        if(id.equals(TownHallBlueprint.ID)||id.equals(FoundersLodgeBlueprint.ID)||id.equals(ProfessionBlueprints.FARMER)||id.equals(ProfessionBlueprints.BUILDER)||id.equals(ProfessionBlueprints.LUMBERJACK))return 2;
        throw new IllegalArgumentException("Unsupported blueprint tier");
    }
    public static String defaultResident(ResourceLocation id){
        if(id.equals(ProfessionBlueprints.FARMER))return "Rowan Fenwick";
        if(id.equals(ProfessionBlueprints.BUILDER))return "Cedric Ashlar";
        if(id.equals(ProfessionBlueprints.LUMBERJACK))return "Brenna Oakfall";
        if(id.equals(RoyalBankBlueprint.ID))return "Oswin Fairmark";
        if(id.equals(HUT))return "Aldric Stonehand";
        return id.equals(TownHallBlueprint.ID)?"Elara Vale":"Mira Ashford";
    }
    public static String occupation(ResourceLocation id){
        if(id.equals(ProfessionBlueprints.FARMER))return "Farmer";
        if(id.equals(ProfessionBlueprints.BUILDER))return "Master Mason";
        if(id.equals(ProfessionBlueprints.LUMBERJACK))return "Forester";
        if(id.equals(RoyalBankBlueprint.ID))return "Royal Banker";
        return id.equals(TownHallBlueprint.ID)?"Royal Steward":"Reeve";
    }
    public static String greeting(ResourceLocation id){
        if(id.equals(ProfessionBlueprints.FARMER))return "No kingdom grows on promises alone. Bring sound harvests, and leave the fields ready for tomorrow.";
        if(id.equals(ProfessionBlueprints.BUILDER))return "A straight wall begins with a measured stone. I keep tools ready and accounts square.";
        if(id.equals(ProfessionBlueprints.LUMBERJACK))return "Plant for the next generation before you fell the last tree. Strong timber builds a strong kingdom.";
        if(id.equals(RoyalBankBlueprint.ID))return "Count twice, sign once. Your money deserves the same care as the walls around it.";
        return id.equals(TownHallBlueprint.ID)?"A kingdom is a promise kept together.":"Every beam has a builder, every gift a name.";
    }

}
