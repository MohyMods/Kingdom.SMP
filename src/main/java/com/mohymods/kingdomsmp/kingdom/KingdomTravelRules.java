package com.mohymods.kingdomsmp.kingdom;

import java.util.List;

/** Approved destination gates. Unknown realms remain outside this explicit policy. */
public final class KingdomTravelRules {
    public record Realm(String dimension,String name,int tier) { }
    private static final List<Realm> REALMS=List.of(
        new Realm("aether:the_aether","The Aether",4),
        new Realm("minecraft:the_nether","The Nether",5),
        new Realm("twilightforest:twilight_forest","Twilight Forest",6),
        new Realm("deeperdarker:otherside","The Otherside",8),
        new Realm("minecraft:the_end","The End",12));
    private KingdomTravelRules(){ }
    public static List<Realm> realms(){return REALMS;}
    public static int requiredTier(String destination){
        if(destination==null)throw new IllegalArgumentException("Missing destination");
        return REALMS.stream().filter(r->r.dimension().equals(destination)).mapToInt(Realm::tier).findFirst().orElse(1);
    }
    public static String name(String destination){
        return REALMS.stream().filter(r->r.dimension().equals(destination)).map(Realm::name).findFirst().orElse(destination);
    }
    public static boolean allowed(int attainedTier,String destination){
        if(attainedTier<1||attainedTier>12||destination==null)throw new IllegalArgumentException("Invalid kingdom travel state");
        return attainedTier>=requiredTier(destination);
    }
}
