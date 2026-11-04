package com.mohymods.kingdomsmp.integration.waystones;

import com.mohymods.kingdomsmp.kingdom.KingdomTravelRules;

/** Waystone use never substitutes for the destination's own approved realm gate. */
public final class WaystoneRules {
    public static final int UNLOCK_TIER=9;
    private WaystoneRules() { }
    public static int requiredTier(String destination){return Math.max(UNLOCK_TIER,KingdomTravelRules.requiredTier(destination));}
    public static boolean allowed(int tier,String destination){return KingdomTravelRules.allowed(tier,destination)&&tier>=UNLOCK_TIER;}
}
