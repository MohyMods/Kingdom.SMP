package com.mohymods.kingdomsmp.kingdom;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KingdomTravelRulesTest {
    @Test void approvedGateBoundariesHoldAtEveryKingdomTier(){
        for(int tier=1;tier<=12;tier++){
            assertEquals(tier>=5,KingdomTravelRules.allowed(tier,"minecraft:the_nether"));
            assertEquals(tier>=12,KingdomTravelRules.allowed(tier,"minecraft:the_end"));
            assertTrue(KingdomTravelRules.allowed(tier,"minecraft:overworld"));
        }
    }
    @Test void noUnapprovedCustomRealmGateIsInvented(){assertTrue(KingdomTravelRules.allowed(1,"example:realm"));}
    @Test void invalidProgressCannotAuthorizeTravel(){
        assertThrows(IllegalArgumentException.class,()->KingdomTravelRules.allowed(0,"minecraft:the_nether"));
        assertThrows(IllegalArgumentException.class,()->KingdomTravelRules.allowed(13,"minecraft:the_end"));
    }
}
