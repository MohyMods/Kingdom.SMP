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
    @Test void noUnapprovedCustomRealmGateIsInvented(){assertTrue(KingdomTravelRules.allowed(1,"example:realm"));}
    @Test void realmLabelsAndScheduleAreImmutable(){
        assertEquals("The Aether",KingdomTravelRules.name("aether:the_aether"));
        assertEquals("Twilight Forest",KingdomTravelRules.name("twilightforest:twilight_forest"));
        assertEquals("The Otherside",KingdomTravelRules.name("deeperdarker:otherside"));
        assertThrows(UnsupportedOperationException.class,()->KingdomTravelRules.realms().clear());
        assertTrue(KingdomTravelRules.allowed(1,"ad_astra:moon"),"Planet split is not silently invented");
        assertThrows(IllegalArgumentException.class,()->KingdomTravelRules.requiredTier(null));
    }
    @Test void invalidProgressCannotAuthorizeTravel(){
        assertThrows(IllegalArgumentException.class,()->KingdomTravelRules.allowed(0,"minecraft:the_nether"));
        assertThrows(IllegalArgumentException.class,()->KingdomTravelRules.allowed(13,"minecraft:the_end"));
    }
}
