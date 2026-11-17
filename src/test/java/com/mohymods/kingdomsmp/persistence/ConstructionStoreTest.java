package com.mohymods.kingdomsmp.persistence;

import com.mohymods.kingdomsmp.building.FoundersLodgeBlueprint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionStoreTest {
    @TempDir Path directory;
    UUID owner=UUID.randomUUID(),admin=UUID.randomUUID();
    ConstructionStore open() throws IOException{return ConstructionStore.open(directory);}
    ConstructionStore.Project award(ConstructionStore s) throws IOException{return s.award("first_hearth",FoundersLodgeBlueprint.ID,1,owner,"Alice");}
    @Test void exactlyOnePermanentEntitlementAndWinnerSurviveReload() throws Exception{
        var s=open();var p=award(s);assertEquals(p.id(),award(open()).id());assertEquals(1,open().projects().size());
        assertThrows(IOException.class,()->open().award("first_hearth",FoundersLodgeBlueprint.ID,1,admin,"Bob"));
    }
    @Test void recoveredPlanInvalidatesOldGenerationAndPriorApproval() throws Exception{
        var s=open();var p=award(s).claim();s.save(p,owner,"CLAIM");UUID old=p.token();
        p=p.propose(new BlockPos(0,64,0),Rotation.CLOCKWISE_90);s.save(p,owner,"PROPOSE");p=p.approve(admin);s.save(p,admin,"APPROVE");
        p=p.claim();s.save(p,owner,"RECOVER");p=open().get(p.id());
        assertNotEquals(old,p.token());assertNull(p.origin());assertNull(p.approvedBy());assertEquals(ConstructionStore.Phase.CLAIMED,p.phase());
    }
    @Test void redemptionCannotReplayOrRegressAfterRestart() throws Exception{
        var s=open();var p=award(s).claim();s.save(p,owner,"CLAIM");
        p=p.propose(new BlockPos(10,64,10),Rotation.NONE);s.save(p,owner,"PROPOSE");var unapproved=p;
        assertThrows(IllegalStateException.class,unapproved::begin);
        p=p.approve(admin);s.save(p,admin,"APPROVE");var before=p;p=p.begin();s.save(p,owner,"BEGIN");
        var restored=open().get(p.id());assertNull(restored.token());assertThrows(IllegalStateException.class,restored::claim);
        assertThrows(IllegalStateException.class,restored::begin);assertThrows(IllegalArgumentException.class,()->s.save(before,owner,"REPLAY"));
        p=p.advance(5);s.save(p,owner,"PROGRESS");assertEquals(5,open().get(p.id()).placed());
    }
    @Test void npcIssuanceIsReservedExactlyOnceAndDeathSurvivesRestart() throws Exception{
        var s=open();var p=award(s).claim().propose(new BlockPos(0,64,0),Rotation.NONE).approve(admin).begin();s.save(p,owner,"BEGIN");
        p=p.advance(25).issueNpc();s.save(p,owner,"ISSUE");var issued=open().get(p.id());assertThrows(IllegalStateException.class,issued::issueNpc);
        p=p.complete();s.save(p,owner,"COMPLETE");p=p.markDead();s.save(p,owner,"DEATH");assertTrue(open().get(p.id()).npcDead());
    }
    @Test void interruptionAfterDurableRotationQuarantinesAndOldTokenCannotReturn() throws Exception{
        var s=open();var p=award(s).claim();s.save(p,owner,"CLAIM");UUID previous=p.token();var next=p.claim();
        var failing=ConstructionStore.open(directory,(file,tag)->{AtomicNbt.write(file,tag);throw new IOException("after rename");});
        assertThrows(IOException.class,()->failing.save(next,owner,"RECOVER"));assertTrue(failing.quarantined());
        var restored=open().get(p.id());assertEquals(next.token(),restored.token());assertNotEquals(previous,restored.token());
        assertThrows(IOException.class,()->failing.save(next,owner,"RETRY"));
    }
    @Test void missingCorruptAndFutureRecordsNeverReset() throws Exception{
        var s=open();award(s);Path file=directory.resolve("kingdomsmp_construction.dat");
        var tag=AtomicNbt.read(file);tag.putInt("schema",99);AtomicNbt.write(file,tag);assertThrows(IOException.class,this::open);
        Files.delete(file);assertThrows(IOException.class,this::open);Files.writeString(file,"corrupt");assertThrows(IOException.class,this::open);
    }
}
