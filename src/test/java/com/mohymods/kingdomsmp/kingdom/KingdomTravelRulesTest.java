package com.mohymods.kingdomsmp.kingdom;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KingdomTravelRulesTest {
    @Test void approvedGateBoundariesHoldAtEveryKingdomTier(){
        for(int tier=1;tier<=12;tier++){
            assertEquals(tier>=4,KingdomTravelRules.allowed(tier,"aether:the_aether"));
            assertEquals(tier>=6,KingdomTravelRules.allowed(tier,"twilightforest:twilight_forest"));
            assertEquals(tier>=8,KingdomTravelRules.allowed(tier,"deeperdarker:otherside"));
            assertEquals(tier>=5,KingdomTravelRules.allowed(tier,"minecraft:the_nether"));
            assertEquals(tier>=12,KingdomTravelRules.allowed(tier,"minecraft:the_end"));
            assertTrue(KingdomTravelRules.allowed(tier,"minecraft:overworld"));
        }
    }
    @Test void exactSpaceDestinationsRetainApprovedInitialAndAdvancedBoundaries(){
        for(String destination:com.mohymods.kingdomsmp.integration.space.SpaceTravelRules.destinations()){
            assertEquals(com.mohymods.kingdomsmp.integration.space.SpaceTravelRules.requiredTier(destination),KingdomTravelRules.requiredTier(destination));
            assertFalse(KingdomTravelRules.allowed(9,destination));
        }
        assertTrue(KingdomTravelRules.allowed(10,"ad_astra:moon"));assertFalse(KingdomTravelRules.allowed(10,"ad_astra:mars"));assertTrue(KingdomTravelRules.allowed(11,"ad_astra:mars"));
        assertTrue(KingdomTravelRules.allowed(1,"minecraft:overworld"));
        assertTrue(KingdomTravelRules.allowed(1,"ad_astra:unreviewed_addon"));
    }
    @Test void noUnapprovedCustomRealmGateIsInvented(){assertTrue(KingdomTravelRules.allowed(1,"example:realm"));}
    @Test void realmLabelsAndScheduleAreImmutable(){
        assertEquals("The Aether",KingdomTravelRules.name("aether:the_aether"));
        assertEquals("Twilight Forest",KingdomTravelRules.name("twilightforest:twilight_forest"));
        assertEquals("The Otherside",KingdomTravelRules.name("deeperdarker:otherside"));
        assertThrows(UnsupportedOperationException.class,()->KingdomTravelRules.realms().clear());
        assertFalse(KingdomTravelRules.allowed(9,"ad_astra:moon"),"Approved earliest space floor applies without inventing an advanced planetary split");
        assertThrows(IllegalArgumentException.class,()->KingdomTravelRules.requiredTier(null));
    }
    @Test void invalidProgressCannotAuthorizeTravel(){
        assertThrows(IllegalArgumentException.class,()->KingdomTravelRules.allowed(0,"minecraft:the_nether"));
        assertThrows(IllegalArgumentException.class,()->KingdomTravelRules.allowed(13,"minecraft:the_end"));
    }
}
