package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
class MineAccessEvidenceTest {
 @TempDir Path directory;
 static long at(int n){return new BlockPos(n,8-n,0).asLong();}
 @Test void continuousRoundTripRequiresDepthReturnAndDistinctRetainedSupports(){var data=new MineAccessEvidence();var p=UUID.randomUUID();var walk=new MineAccessEvidence.Walk(at(0));for(int n=0;n<16;n++){data.record(new MineAccessEvidence.Cell("minecraft:overworld",at(n)),new MineAccessEvidence.Mark(p,"minecraft:stone_brick_stairs"));assertTrue(walk.step(at(n)));}assertFalse(walk.returned(16));assertFalse(data.complete(p,"minecraft:overworld",walk,16));for(int n=14;n>=0;n--)assertTrue(walk.step(at(n)));assertTrue(walk.returned(16));assertTrue(data.complete(p,"minecraft:overworld",walk,16));var restored=MineAccessEvidence.load(data.save());assertEquals(data.route(p),restored.route(p));assertEquals(16,restored.route(p).orElseThrow().cells().size());assertTrue(restored.remove(new MineAccessEvidence.Cell("minecraft:overworld",at(5))));assertTrue(restored.mark(new MineAccessEvidence.Cell("minecraft:overworld",at(5))).isEmpty());assertTrue(restored.route(p).isPresent(),"Historical route survives a missing retained block without inventing replacement ownership");}
 @Test void jumpsRepetitionForeignOwnershipAndMalformedSavedProofCannotQualify(){var p=UUID.randomUUID();var e=new MineAccessEvidence();var walk=new MineAccessEvidence.Walk(at(0));assertFalse(walk.step(at(3)));assertFalse(walk.returned(2));assertThrows(IllegalArgumentException.class,()->new MineAccessEvidence.Walk(at(10)));assertThrows(IllegalArgumentException.class,()->new MineAccessEvidence.Route(UUID.randomUUID(),"minecraft:overworld",List.of(at(0),at(10))));for(int n=0;n<16;n++){e.record(new MineAccessEvidence.Cell("minecraft:overworld",at(n)),new MineAccessEvidence.Mark(n==10?UUID.randomUUID():p,"minecraft:cobblestone"));walk.step(at(n));}for(int n=14;n>=0;n--)walk.step(at(n));assertFalse(e.complete(p,"minecraft:overworld",walk,16));var tag=e.save();var rows=tag.getList("marks",Tag.TAG_COMPOUND);rows.add(rows.getCompound(0).copy());assertThrows(IllegalArgumentException.class,()->MineAccessEvidence.load(tag));assertFalse(MineAccessEvidence.material("kingdom_smp:free_stairs"));}
 @Test void configurableDefaultsPersistAndInvalidInputsNeverOverwrite()throws Exception{var path=directory.resolve("rules.json");assertEquals(new MineAccessRules(16,16),MineAccessRules.load(path));var custom="{\"schema\":1,\"routeCells\":24,\"lights\":20}";Files.writeString(path,custom);assertEquals(new MineAccessRules(24,20),MineAccessRules.load(path));assertEquals(custom,Files.readString(path));Files.writeString(path,custom.replace("24","24.5"));assertThrows(java.io.IOException.class,()->MineAccessRules.load(path));}
}
