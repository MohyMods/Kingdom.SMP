package com.mohymods.kingdomsmp.kingdom;

import com.mohymods.kingdomsmp.profession.*;
import java.util.Set;

/** Runtime release availability, independent of historical attained tiers and saved profession records. */
public final class AlphaScope {
    public static final int MAX_PLAYABLE_TIER=6;
    private static final Set<Profession> CORE=Set.of(Profession.FARMER,Profession.BLACKSMITH,Profession.BUILDER,Profession.LUMBERJACK,Profession.MINER,Profession.LIBRARIAN);
    private AlphaScope() { }
    public static boolean serviceAvailable(int requiredTier){if(requiredTier<1||requiredTier>12)throw new IllegalArgumentException("Invalid service tier");return requiredTier<=MAX_PLAYABLE_TIER;}
    public static boolean selected(Profession profession){return CORE.contains(profession);}
    /** Explicit alpha variants within Tier6; original approved metadata and earned history stay untouched. */
    public static int questTier(String id){var definition=ApprovedProfessionContent.get(id);return Set.of("kingdom_smp:librarian/m01","kingdom_smp:librarian/m02","kingdom_smp:librarian/m03","kingdom_smp:librarian/m04","kingdom_smp:librarian/m05","kingdom_smp:librarian/m06","kingdom_smp:librarian/m07","kingdom_smp:librarian/m08").contains(id)?6:definition.minimumTier();}
    public static boolean futureQuest(String id){if(id==null)return false;var definition=ApprovedProfessionContent.definitions().get(id);return definition!=null&&questTier(id)>MAX_PLAYABLE_TIER;}
    public static boolean destinationAvailable(int attainedTier,String destination){return KingdomTravelRules.requiredTier(destination)<=MAX_PLAYABLE_TIER&&KingdomTravelRules.allowed(attainedTier,destination);}
    public static String futureMessage(){return "COMING SOON — Future Update. Tier 7–12 content is unavailable in this alpha; historical progress is preserved.";}
}
