package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
class ApiaryEvidenceTest {
 @Test void observationsPreserveDistinctSpeciesAndReplacementInvalidatesSite(){var e=new ApiaryEvidence();var site=new ApiaryEvidence.Site("minecraft:overworld",42);var iron=new ApiaryEvidence.Observation("productivebees:iron",UUID.randomUUID(),100,2);e.record(site,iron);e.record(site,new ApiaryEvidence.Observation("productivebees:copper",UUID.randomUUID(),101,1));var restored=ApiaryEvidence.load(e.save());assertEquals(2,restored.at(site).size());assertEquals(iron,restored.at(site).get(iron.species()));restored.remove(site);assertTrue(restored.at(site).isEmpty());assertEquals(2,e.at(site).size());}
 @Test void corruptedOrDuplicateAuthorityFailsClosedWithoutReconstruction(){var e=new ApiaryEvidence();var site=new ApiaryEvidence.Site("minecraft:overworld",42);e.record(site,new ApiaryEvidence.Observation("productivebees:iron",UUID.randomUUID(),100,1));var tag=e.save();var sites=tag.getList("sites",Tag.TAG_COMPOUND);sites.add(sites.getCompound(0).copy());assertThrows(IllegalArgumentException.class,()->ApiaryEvidence.load(tag));assertThrows(IllegalArgumentException.class,()->new ApiaryEvidence.Observation("productivebees:draconic",UUID.randomUUID(),1,1));assertThrows(IllegalArgumentException.class,()->new ApiaryEvidence.Site("minecraft:the_end",42));var wrong=e.save();wrong.putString("schema","1");assertThrows(IllegalArgumentException.class,()->ApiaryEvidence.load(wrong));}
}
