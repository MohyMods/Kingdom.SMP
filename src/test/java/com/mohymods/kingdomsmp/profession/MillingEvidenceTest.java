package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
class MillingEvidenceTest {
 @Test void pendingInputsAndExactRandomNativeOutputCreditPersistTogetherWithoutReplay(){
  var proof=new MillingEvidence();var site=new MillingEvidence.Cell("minecraft:overworld",12L);var source=UUID.randomUUID();var owner=UUID.randomUUID();assertTrue(proof.start(site,source,owner,64));var job=proof.job(site).orElseThrow();assertTrue(proof.advance(site,job,3));assertFalse(proof.advance(site,job,3));assertEquals(3,proof.count(owner));
  var restored=MillingEvidence.load(proof.save());assertEquals(63,restored.job(site).orElseThrow().remaining());assertEquals(3,restored.count(owner));assertFalse(restored.advance(site,job,3));assertEquals(0,restored.count(UUID.randomUUID()));var next=restored.job(site).orElseThrow();assertFalse(restored.advance(site,next,0));assertEquals(62,restored.job(site).orElseThrow().remaining());assertEquals(3,restored.count(owner));
  assertThrows(IllegalArgumentException.class,()->restored.advance(site,restored.job(site).orElseThrow(),4));
 }
 @Test void mutationInvalidatesOnlyPendingInputNeverGrantedHistoricalOutputs(){
  var d=new MillingEvidence();var site=new MillingEvidence.Cell("minecraft:overworld",1L);var owner=UUID.randomUUID();d.start(site,UUID.randomUUID(),owner,2);d.advance(site,d.job(site).orElseThrow(),1);d.invalidate(site);assertTrue(d.job(site).isEmpty());assertEquals(1,MillingEvidence.load(d.save()).count(owner));
 }
 @Test void corruptDuplicateSourcesSitesAndBadRemainingFailClosed(){
  var d=new MillingEvidence();d.start(new MillingEvidence.Cell("minecraft:overworld",1),UUID.randomUUID(),UUID.randomUUID(),64);var tag=d.save();var bad=tag.copy();bad.getList("jobs",Tag.TAG_COMPOUND).getCompound(0).putInt("remaining",65);assertThrows(IllegalArgumentException.class,()->MillingEvidence.load(bad));var duplicate=tag.copy();var rows=duplicate.getList("jobs",Tag.TAG_COMPOUND);var row=rows.getCompound(0).copy();row.putLong("position",2);rows.add(row);assertThrows(IllegalArgumentException.class,()->MillingEvidence.load(duplicate));var malformed=tag.copy();malformed.putInt("schema",2);assertThrows(IllegalArgumentException.class,()->MillingEvidence.load(malformed));
 }
}
