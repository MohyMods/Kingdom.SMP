package com.mohymods.kingdomsmp.kingdom;

/** Approved vanilla realm-entry gates, independent of unapproved later milestone values. */
public final class KingdomTravelRules {
    private KingdomTravelRules(){ }
    public static int requiredTier(String destination){
        return switch(destination){case "minecraft:the_nether"->5;case "minecraft:the_end"->12;default->1;};
    }
    public static boolean allowed(int attainedTier,String destination){
        if(attainedTier<1||attainedTier>12||destination==null)throw new IllegalArgumentException("Invalid kingdom travel state");
        return attainedTier>=requiredTier(destination);
    }
}
