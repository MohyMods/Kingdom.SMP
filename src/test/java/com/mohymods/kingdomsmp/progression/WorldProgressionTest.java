package com.mohymods.kingdomsmp.progression;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class WorldProgressionTest {
    @TempDir Path dir;
    @Test void freshOwnerAndResidentPersistWithoutReclassification()throws Exception{
        UUID owner=UUID.randomUUID(),project=UUID.randomUUID();
        var p=WorldProgression.open(dir,0,owner,false);assertEquals(WorldProgression.Mode.ROYAL_HALL,p.mode());assertEquals(owner,p.owner());
        p.name(project,"Elara of the Hearth");assertThrows(IllegalStateException.class,()->p.assignOwner(UUID.randomUUID()));
        var reload=WorldProgression.open(dir,90000,null,true);assertEquals(p.mode(),reload.mode());assertEquals(owner,reload.owner());assertEquals("Elara of the Hearth",reload.resident(project));
    }
    @Test void anyHistoricalKingdomRecordPinsLegacyWithoutEditingIt()throws Exception{
        Path old=dir.resolve("kingdomsmp_construction.created");byte[] bytes={1,2,3};Files.write(old,bytes);
        var p=WorldProgression.open(dir,0,null,false);assertEquals(WorldProgression.Mode.LEGACY_LODGE,p.mode());assertArrayEquals(bytes,Files.readAllBytes(old));
        assertThrows(IllegalStateException.class,()->p.assignOwner(UUID.randomUUID()));
        assertEquals(p.mode(),WorldProgression.open(dir,0,UUID.randomUUID(),false).mode());
    }
    @Test void missingSealOrUnknownSchemaRefusesInsteadOfGrantingNewRewards()throws Exception{
        WorldProgression.open(dir,0,null,false);var tag=AtomicNbt.read(dir.resolve("kingdomsmp_progression.dat"));tag.putInt("schema",99);AtomicNbt.write(dir.resolve("kingdomsmp_progression.dat"),tag);
        assertThrows(java.io.IOException.class,()->WorldProgression.open(dir,0,null,false));
        Files.delete(dir.resolve("kingdomsmp_progression.dat"));assertThrows(java.io.IOException.class,()->WorldProgression.open(dir,0,null,false));
    }
    @Test void dedicatedOwnerIsExplicitOneTimeAndRejectsFormatting()throws Exception{
        var p=WorldProgression.open(dir,0,null,false);assertNull(p.owner());UUID owner=UUID.randomUUID();p.assignOwner(owner);p.assignOwner(owner);
        assertThrows(IllegalStateException.class,()->p.assignOwner(UUID.randomUUID()));
        assertThrows(IllegalArgumentException.class,()->p.name(UUID.randomUUID(),"§kSpoof"));
        assertEquals(owner,WorldProgression.open(dir,1,null,false).owner());
    }
}
