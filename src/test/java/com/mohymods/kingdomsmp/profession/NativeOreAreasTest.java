package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;
class NativeOreAreasTest {
 @Test void copperCanBeMinedFirstButRequiresAnotherAuthenticWorkingArea(){var p=UUID.randomUUID();var a=new NativeOreAreas();long copper=ChunkPos.asLong(0,0);a.record(p,ProfessionRank.APPRENTICE,copper,"copper");assertEquals(0,a.copper(p,ProfessionRank.APPRENTICE,2));a.record(p,ProfessionRank.APPRENTICE,ChunkPos.asLong(3,0),"coal");assertEquals(1,a.copper(p,ProfessionRank.APPRENTICE,2));a.record(p,ProfessionRank.APPRENTICE,ChunkPos.asLong(1,0),"iron");assertEquals(0,a.copper(p,ProfessionRank.APPRENTICE,2));assertEquals(1,a.copper(p,ProfessionRank.APPRENTICE,1));}
 @Test void otherPlayersBandsAndDistantDimensionsCannotCreatePersonalAreaAuthority(){var p=UUID.randomUUID();var a=new NativeOreAreas();a.record(p,ProfessionRank.APPRENTICE,ChunkPos.asLong(-4,-4),"copper");a.record(UUID.randomUUID(),ProfessionRank.APPRENTICE,0,"coal");a.record(p,ProfessionRank.JOURNEYMAN,0,"iron");assertEquals(0,a.copper(p,ProfessionRank.APPRENTICE,2));a.record(p,ProfessionRank.APPRENTICE,0,"coal");assertEquals(1,a.copper(p,ProfessionRank.APPRENTICE,2));assertThrows(IllegalArgumentException.class,()->a.copper(p,ProfessionRank.APPRENTICE,0));}
 @Test void restoredAreasCannotExceedMatchingNativeCounts(){var p=UUID.randomUUID();var a=new NativeOreAreas();a.record(p,ProfessionRank.APPRENTICE,0,"coal");var evidence=new ProfessionEvidence(Profession.MINER,Set.of("coal","iron","copper"));assertThrows(IllegalArgumentException.class,()->NativeOreAreas.load(a.save(),evidence));evidence.record(p,ProfessionRank.APPRENTICE,"coal",1,null);var r=NativeOreAreas.load(a.save(),evidence);assertEquals(a.save(),r.save());}
}
