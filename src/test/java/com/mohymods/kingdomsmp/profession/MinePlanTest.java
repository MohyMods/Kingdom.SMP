package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class MinePlanTest {
 @Test void readableZoneReferencesRequireThreeDistinctSeparatedLocations(){var plan=MinePlan.parse("Safe access and separate stored ore\naccess=32,16,-32\nore=28,17,-34\nsupport=32,-3,-13");assertEquals(3,plan.size());assertThrows(IllegalStateException.class,()->MinePlan.parse("access=0,0,0\nore=0,0,0\nsupport=1,0,0"));assertThrows(IllegalStateException.class,()->MinePlan.parse("access=0,0,0\naccess=0,0,0\nore=8,0,0\nsupport=16,0,0"));assertThrows(IllegalStateException.class,()->MinePlan.parse("access=0,0,0\nore=8.5,0,0\nsupport=16,0,0"));assertThrows(IllegalStateException.class,()->MinePlan.parse("access=0,0,0"));}
}
