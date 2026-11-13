package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class SmeltQuestEvidenceTest {
 @Test void nativeRecipeReceiptCertifiesOneActualOutputBatchEvenWhenCollectedPartially(){assertEquals(64,SmeltQuestEvidence.batch(1,63,64));assertEquals(64,SmeltQuestEvidence.batch(64,0,64));assertEquals(0,SmeltQuestEvidence.batch(63,0,0));}
 @Test void oldRecipeHistoryCannotCertifyMoreThanActualPresentOutput(){assertEquals(32,SmeltQuestEvidence.batch(1,31,500));assertEquals(16,SmeltQuestEvidence.batch(32,0,16));}
 @Test void zeroAndImpossibleNativeOutputBoundsFailClosed(){for(var v:new int[][]{{0,64,64},{65,0,65},{1,64,65},{1,-1,1},{1,0,-1},{-1,1,1}})assertEquals(0,SmeltQuestEvidence.batch(v[0],v[1],v[2]));}
}
