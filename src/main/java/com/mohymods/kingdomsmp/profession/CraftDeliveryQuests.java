package com.mohymods.kingdomsmp.profession;

import java.util.*;
import net.minecraft.nbt.*;

/** Approved craft-and-consume families with verified vanilla output identities. */
public final class CraftDeliveryQuests {
    public static final String MASONRY="kingdom_smp:builder/a05";
    private static final Map<String,Integer> MASONRY_INPUTS=Map.of("minecraft:stone_brick_stairs",32,"minecraft:stone_brick_slab",32);
    private static final Map<String,String> METRICS=Map.of("minecraft:stone_brick_stairs","stone_stairs","minecraft:stone_brick_slab","stone_slabs");
    private static final ProfessionQuestCatalog CATALOG;
    static {
        var d=ApprovedProfessionContent.get(MASONRY);
        if(!d.validation().equals(Set.of(ApprovedProfessionContent.Validation.MAKE,ApprovedProfessionContent.Validation.DEL))||d.minimumTier()!=2)throw new IllegalStateException("Craft family differs from its approved baseline");
        CATALOG=ProfessionQuestCatalog.approved(List.of(d.slot()));
    }
    private CraftDeliveryQuests() { }
    public static boolean contains(String id){return MASONRY.equals(id);}
    public static ApprovedProfessionContent.Definition get(String id){if(!contains(id))throw new IllegalArgumentException("No validated craft/delivery adapter for this objective");return ApprovedProfessionContent.get(id);}
    public static ProfessionQuestCatalog catalog(){return CATALOG;}
    public static Map<String,Integer> defaults(String id){get(id);return MASONRY_INPUTS;}
    public static Map<String,String> metrics(Profession role){return role==Profession.BUILDER?METRICS:Map.of();}
    public static void validate(String id,CompoundTag inputs){
        var expected=defaults(id);if(!inputs.getAllKeys().equals(expected.keySet()))throw new IllegalArgumentException("Wrong profession delivery materials");
        int total=0;for(var key:expected.keySet()){
            if(!inputs.contains(key,Tag.TAG_INT)||inputs.getInt(key)<1||inputs.getInt(key)>2304)throw new IllegalArgumentException("Invalid bounded delivery quantity");
            total=Math.addExact(total,inputs.getInt(key));
        }
        if(total>2304)throw new IllegalArgumentException("Delivery exceeds the native inventory capacity");
    }
}
