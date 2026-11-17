package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import com.mohymods.kingdomsmp.economy.HuntState;
import java.util.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
class EncounterEvidenceTest {
 @TempDir Path directory;
 @Test void variedRealVictimIdentitiesPersistWithoutRecreditToAnotherPlayer(){var e=new EncounterEvidence();var p=UUID.randomUUID();var first=UUID.randomUUID();assertTrue(e.record(first,p,HuntState.TARGETS.getFirst()));for(var species:HuntState.TARGETS)for(int n=0;n<(species.equals(HuntState.TARGETS.getFirst())?5:6);n++)assertTrue(e.record(UUID.randomUUID(),p,species));assertTrue(e.ready(p,24,4));var restored=EncounterEvidence.load(e.save());assertEquals(e.counts(p),restored.counts(p));assertFalse(restored.record(first,p,HuntState.TARGETS.getFirst()));assertFalse(restored.record(first,UUID.randomUUID(),HuntState.TARGETS.getLast()));assertFalse(restored.ready(UUID.randomUUID(),24,4));}
 @Test void quantityCannotReplaceMissingSpeciesAndMalformedRecordsNeverReset(){var e=new EncounterEvidence();var p=UUID.randomUUID();for(int n=0;n<24;n++)e.record(UUID.randomUUID(),p,"minecraft:zombie");assertFalse(e.ready(p,24,4));assertFalse(e.record(UUID.randomUUID(),p,"minecraft:cow"));var tag=e.save();var list=tag.getList("victims",Tag.TAG_COMPOUND);list.add(list.getCompound(0).copy());assertThrows(IllegalArgumentException.class,()->EncounterEvidence.load(tag));var wrong=e.save();wrong.putString("schema","1");assertThrows(IllegalArgumentException.class,()->EncounterEvidence.load(wrong));}
 @Test void configuredQuantitiesPersistAndInvalidRulesFailClosed()throws Exception{var path=directory.resolve("rules.json");assertEquals(new EncounterRules(24,4),EncounterRules.load(path));String custom="{\"schema\":1,\"total\":32,\"perSpecies\":6}";Files.writeString(path,custom);assertEquals(new EncounterRules(32,6),EncounterRules.load(path));assertEquals(custom,Files.readString(path));for(var value:List.of("{\"schema\":1,\"total\":24,\"perSpecies\":8}","{\"schema\":1,\"total\":24.5,\"perSpecies\":4}","{\"schema\":1,\"total\":\"24\",\"perSpecies\":4}")){Files.writeString(path,value);assertThrows(java.io.IOException.class,()->EncounterRules.load(path));assertEquals(value,Files.readString(path));}}
 @Test void unnecessarySingleSpeciesFarmingCannotExhaustSharedProofCapacity(){var r=new EncounterRules(24,4);assertEquals(12,r.speciesLimit());assertTrue(4L*new EncounterRules(2048,1).speciesLimit()*10<EncounterEvidence.LIMIT);assertThrows(IllegalArgumentException.class,()->new EncounterRules(2049,1));var e=new EncounterEvidence();var p=UUID.randomUUID();for(var type:com.mohymods.kingdomsmp.economy.HuntState.TARGETS)for(int n=0;n<r.speciesLimit();n++)e.record(UUID.randomUUID(),p,type);assertTrue(e.ready(p,r.total(),r.perSpecies()));}

}
