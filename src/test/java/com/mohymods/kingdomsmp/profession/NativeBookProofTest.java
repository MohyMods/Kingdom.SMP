package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class NativeBookProofTest {
 @Test void oneIdentityCannotFillSeveralFamiliesAndMatchingFindsLaterUsefulBooks(){assertEquals(List.of(),NativeBookProof.selectFamilies(List.of(15),4));assertEquals(List.of(),NativeBookProof.selectFamilies(List.of(2,2,2,2),4));assertEquals(4,NativeBookProof.selectFamilies(List.of(2,2,3,4,8),4).size());assertEquals(4,NativeBookProof.selectFamilies(List.of(3,1,4,8),4).size());assertEquals(8,NativeBookProof.selectFamilies(List.of(15,15,15,15,15,15,15,15),8).size());assertThrows(IllegalArgumentException.class,()->NativeBookProof.selectFamilies(List.of(1),5));}
 @Test void canonicalFactsRejectMalformedOrUnknownMasksAndNeverComeFromItemTags(){var facts=new NativeBookProof(9,true);assertEquals(facts,NativeBookProof.load(facts.save()));var bad=facts.save();bad.putInt("families",16);assertThrows(IllegalArgumentException.class,()->NativeBookProof.load(bad));var malformed=facts.save();malformed.putByte("maximum",(byte)2);assertThrows(IllegalArgumentException.class,()->NativeBookProof.load(malformed));assertEquals(0,NativeBookProof.family("foreign:protection"));assertEquals(1,NativeBookProof.family("protection"));assertEquals(2,NativeBookProof.family("efficiency"));assertEquals(4,NativeBookProof.family("sharpness"));assertEquals(8,NativeBookProof.family("unbreaking"));}
}
