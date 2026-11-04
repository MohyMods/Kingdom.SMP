package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.nbt.*;
/** Approved native production and permanent-consumption families, with verified vanilla identities. */
public final class CraftDeliveryQuests {
    public static final String MASONRY="kingdom_smp:builder/a05",TIMBER="kingdom_smp:lumberjack/a05",CHARCOAL="kingdom_smp:lumberjack/j05",FITTINGS="kingdom_smp:lumberjack/j06";
    private static final Map<String,Map<String,Integer>> GOALS=Map.of(MASONRY,Map.of("minecraft:stone_brick_stairs",32,"minecraft:stone_brick_slab",32),TIMBER,Map.of("planks",128,"stripped_logs",32),CHARCOAL,Map.of("minecraft:charcoal",64),FITTINGS,Map.of("fences",32,"wood_stairs",16,"trapdoors",16));
    private static final Map<String,String> TIMBER_METRICS;
    private static final Map<String,String> METRICS=Map.of("minecraft:stone_brick_stairs","stone_stairs","minecraft:stone_brick_slab","stone_slabs");
    private static final ProfessionQuestCatalog CATALOG;
    static {
        Map<String,String> timber=new HashMap<>();for(var wood:TreeEvidence.SPECIES){timber.put("minecraft:"+wood+"_planks","planks");timber.put("minecraft:stripped_"+wood+"_log","stripped_logs");timber.put("minecraft:"+wood+"_fence","fences");timber.put("minecraft:"+wood+"_stairs","wood_stairs");timber.put("minecraft:"+wood+"_trapdoor","trapdoors");}timber.put("minecraft:charcoal","smelt_charcoal");TIMBER_METRICS=Map.copyOf(timber);
        Set<String> closure=new HashSet<>(GOALS.keySet());for(var id:GOALS.keySet()){var d=ApprovedProfessionContent.get(id);if(!d.validation().equals(Set.of(ApprovedProfessionContent.Validation.MAKE,ApprovedProfessionContent.Validation.DEL))||d.minimumTier()!=(d.rank()==ProfessionRank.APPRENTICE?2:3))throw new IllegalStateException("Unvalidated production family");for(var rank:ProfessionRank.values())if(rank.ordinal()<d.rank().ordinal())for(int n=1;n<=8;n++)closure.add(d.profession().questId(rank,n));}
        var queue=new ArrayDeque<>(closure);while(!queue.isEmpty())for(var prior:ApprovedProfessionContent.get(queue.removeFirst()).prerequisites())if(closure.add(prior))queue.addLast(prior);CATALOG=ProfessionQuestCatalog.approved(closure.stream().map(id->ApprovedProfessionContent.get(id).slot()).toList());
    }
    private CraftDeliveryQuests() { }
    public static Set<String> ids(){return GOALS.keySet();}
    public static boolean contains(String id){return GOALS.containsKey(id);}
    public static boolean grouped(String id){return TIMBER.equals(id)||FITTINGS.equals(id);}
    public static ApprovedProfessionContent.Definition get(String id){if(!contains(id))throw new IllegalArgumentException("No validated production adapter");return ApprovedProfessionContent.get(id);}
    public static ProfessionQuestCatalog catalog(){return CATALOG;}
    public static Map<String,Integer> defaults(String id){get(id);return GOALS.get(id);}
    public static Map<String,String> metrics(Profession role){return role==Profession.BUILDER?METRICS:role==Profession.LUMBERJACK?TIMBER_METRICS:Map.of();}
    public static Map<String,Integer> compound(CompoundTag tag){Map<String,Integer> counts=new HashMap<>();for(var key:tag.getAllKeys()){if(!tag.contains(key,Tag.TAG_INT))throw new IllegalArgumentException("Integer material count required");counts.put(key,tag.getInt(key));}return Map.copyOf(counts);}
    public static Map<String,Integer> totals(String id,Map<String,Integer> actual){
        if(!grouped(id))return Map.copyOf(actual);Map<String,Integer> result=new HashMap<>();defaults(id).keySet().forEach(k->result.put(k,0));int total=0;
        for(var e:actual.entrySet()){if(e.getValue()<1||e.getValue()>2304)throw new IllegalArgumentException("Invalid material quantity");String category=TIMBER_METRICS.get(e.getKey());if(category==null||!result.containsKey(category))throw new IllegalArgumentException("Unverified material category");result.merge(category,e.getValue(),Math::addExact);total=Math.addExact(total,e.getValue());}
        if(total>2304)throw new IllegalArgumentException("Delivery exceeds inventory capacity");return Map.copyOf(result);
    }
    public static void validate(String id,CompoundTag input){
        if(grouped(id)){if(totals(id,compound(input)).values().stream().anyMatch(n->n<1))throw new IllegalArgumentException("Every approved category must be consumed");return;}validateGoals(id,input);
    }
    public static void validateGoals(String id,CompoundTag goals){
        if(!goals.getAllKeys().equals(defaults(id).keySet()))throw new IllegalArgumentException("Unknown production requirement");int total=0;for(var key:goals.getAllKeys()){if(!goals.contains(key,Tag.TAG_INT)||goals.getInt(key)<1||goals.getInt(key)>2304)throw new IllegalArgumentException("Invalid bounded production quantity");total=Math.addExact(total,goals.getInt(key));}if(total>2304)throw new IllegalArgumentException("Delivery cannot fit inventory");
    }
}
