package com.mohymods.kingdomsmp.profession;

import java.util.*;
import net.minecraft.nbt.*;

/** Approved craft-and-consume families with verified vanilla output identities. */
public final class CraftDeliveryQuests {
    public static final String MASONRY="kingdom_smp:builder/a05",TIMBER="kingdom_smp:lumberjack/a05";
    private static final Map<String,Integer> TIMBER_GOALS=Map.of("planks",128,"stripped_logs",32);
    private static final Map<String,String> TIMBER_METRICS;
    private static final Map<String,Integer> MASONRY_INPUTS=Map.of("minecraft:stone_brick_stairs",32,"minecraft:stone_brick_slab",32);
    private static final Map<String,String> METRICS=Map.of("minecraft:stone_brick_stairs","stone_stairs","minecraft:stone_brick_slab","stone_slabs");
    private static final ProfessionQuestCatalog CATALOG;
    static {
        Map<String,String> timber=new HashMap<>();for(var wood:TreeEvidence.SPECIES){timber.put("minecraft:"+wood+"_planks","planks");timber.put("minecraft:stripped_"+wood+"_log","stripped_logs");}TIMBER_METRICS=Map.copyOf(timber);
        var definitions=new ArrayList<ProfessionQuestCatalog.Definition>();for(var id:List.of(MASONRY,TIMBER)){var d=ApprovedProfessionContent.get(id);if(!d.validation().equals(Set.of(ApprovedProfessionContent.Validation.MAKE,ApprovedProfessionContent.Validation.DEL))||d.minimumTier()!=2)throw new IllegalStateException("Craft family differs from approved baseline");definitions.add(d.slot());}CATALOG=ProfessionQuestCatalog.approved(definitions);
    }
    private CraftDeliveryQuests() { }
    public static boolean contains(String id){return MASONRY.equals(id)||TIMBER.equals(id);}
    public static ApprovedProfessionContent.Definition get(String id){if(!contains(id))throw new IllegalArgumentException("No validated craft/delivery adapter for this objective");return ApprovedProfessionContent.get(id);}
    public static ProfessionQuestCatalog catalog(){return CATALOG;}
    public static Map<String,Integer> defaults(String id){get(id);return TIMBER.equals(id)?TIMBER_GOALS:MASONRY_INPUTS;}
    public static Map<String,String> metrics(Profession role){return role==Profession.BUILDER?METRICS:role==Profession.LUMBERJACK?TIMBER_METRICS:Map.of();}
    public static void validate(String id,CompoundTag inputs){
        if(TIMBER.equals(id)){var totals=totals(id,compound(inputs));if(totals.get("planks")<1||totals.get("stripped_logs")<1)throw new IllegalArgumentException("Both approved timber categories must be consumed");return;}
        var expected=defaults(id);if(!inputs.getAllKeys().equals(expected.keySet()))throw new IllegalArgumentException("Wrong profession delivery materials");
        int total=0;for(var key:expected.keySet()){
            if(!inputs.contains(key,Tag.TAG_INT)||inputs.getInt(key)<1||inputs.getInt(key)>2304)throw new IllegalArgumentException("Invalid bounded delivery quantity");
            total=Math.addExact(total,inputs.getInt(key));
        }
        if(total>2304)throw new IllegalArgumentException("Delivery exceeds the native inventory capacity");
    }
    public static Map<String,Integer> compound(CompoundTag tag){Map<String,Integer> counts=new HashMap<>();for(var key:tag.getAllKeys()){if(!tag.contains(key,Tag.TAG_INT))throw new IllegalArgumentException("Integer material count required");counts.put(key,tag.getInt(key));}return Map.copyOf(counts);}
    public static Map<String,Integer> totals(String id,Map<String,Integer> actual){
        if(!TIMBER.equals(id))return Map.copyOf(actual);
        int planks=0,stripped=0;for(var e:actual.entrySet()){
            if(e.getValue()<1||e.getValue()>2304)throw new IllegalArgumentException("Invalid timber quantity");String category=TIMBER_METRICS.get(e.getKey());if(category==null)throw new IllegalArgumentException("Unverified timber item");if(category.equals("planks"))planks=Math.addExact(planks,e.getValue());else stripped=Math.addExact(stripped,e.getValue());
        }
        if(planks+stripped>2304)throw new IllegalArgumentException("Timber exceeds inventory capacity");return Map.of("planks",planks,"stripped_logs",stripped);
    }
    public static void validateGoals(String id,CompoundTag goals){
        if(!TIMBER.equals(id)){validate(id,goals);return;}
        if(!goals.getAllKeys().equals(TIMBER_GOALS.keySet()))throw new IllegalArgumentException("Unknown timber goal");int total=0;for(var key:TIMBER_GOALS.keySet()){if(!goals.contains(key,Tag.TAG_INT)||goals.getInt(key)<1||goals.getInt(key)>2304)throw new IllegalArgumentException("Invalid timber goal");total+=goals.getInt(key);}if(total>2304)throw new IllegalArgumentException("Timber delivery cannot fit inventory");
    }

}
