package com.mohymods.kingdomsmp.placement;

import net.neoforged.fml.ModList;

/** No assumed permission when foreign claim ownership cannot yet be resolved. */
public final class PlacementProtection {
    private PlacementProtection(){}
    public static String denial(){return ModList.get().isLoaded("ftbchunks")||ModList.get().isLoaded("openpartiesandclaims")?"Installed claim-system placement permissions need a verified adapter; construction is paused.":null;}
}
