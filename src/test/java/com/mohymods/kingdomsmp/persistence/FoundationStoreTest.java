package com.mohymods.kingdomsmp.persistence;

import com.mohymods.kingdomsmp.building.FoundersLodgeBlueprint;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FoundationStoreTest {
    @TempDir Path dir;final UUID owner=UUID.randomUUID();
    ConstructionStore open()throws Exception{return ConstructionStore.open(dir);}
    ConstructionStore.Project proposal(ConstructionStore store)throws Exception{return store.award("first_hearth",FoundersLodgeBlueprint.ID,1,owner,"Founder").claim().propose(new BlockPos(5,64,7),Rotation.CLOCKWISE_90);}
    List<BlockPos> cells(){return List.of(new BlockPos(0,-2,0),new BlockPos(0,-1,0));}
    @Test void savedApprovedColumnsAndPartialProgressReopenUnchanged()throws Exception{
        var store=open();var p=proposal(store).withFoundations(cells()).approve(owner).begin().advance(1);store.save(p,owner,"BEGIN_FOUNDATIONS");
        var loaded=open().get(p.id());assertEquals(p,loaded);assertEquals(cells(),loaded.foundations());assertEquals(1,loaded.placed());
        assertThrows(IllegalStateException.class,()->loaded.withFoundations(List.of()));
        var changed=new ConstructionStore.Project(p.id(),p.reward(),p.blueprint(),p.blueprintVersion(),p.owner(),p.ownerName(),p.phase(),p.token(),p.origin(),p.rotation(),p.proposal(),p.approvedBy(),p.placed(),p.npc(),p.npcIssued(),p.npcDead(),List.of());
        assertThrows(IllegalArgumentException.class,()->store.save(changed,owner,"REWRITE_REJECTED"));
    }
    @Test void approvedColumnsCannotChangeUnderTheSameApproval()throws Exception{
        var store=open();var p=proposal(store).withFoundations(cells()).approve(owner);store.save(p,owner,"APPROVE");
        var changed=new ConstructionStore.Project(p.id(),p.reward(),p.blueprint(),p.blueprintVersion(),p.owner(),p.ownerName(),p.phase(),p.token(),p.origin(),p.rotation(),p.proposal(),p.approvedBy(),p.placed(),p.npc(),p.npcIssued(),p.npcDead(),List.of());
        assertThrows(IllegalArgumentException.class,()->store.save(changed,owner,"ALTER_APPROVED"));
        var fresh=p.propose(p.origin(),p.rotation());store.save(fresh,owner,"NEW_SITE_REVIEW");assertNotEquals(p.proposal(),open().get(p.id()).proposal());assertNull(open().get(p.id()).approvedBy());
    }
    @Test void lostUnusedPlanAndNewSiteCannotRetainOldSupports()throws Exception{
        var p=proposal(open()).withFoundations(cells()).approve(owner);
        assertTrue(p.claim().foundations().isEmpty());assertTrue(p.propose(new BlockPos(6,64,7),Rotation.NONE).foundations().isEmpty());
        assertNotEquals(p.proposal(),p.propose(p.origin(),p.rotation()).proposal());
    }
    @Test void oldSchemaKeepsIdentityNpcWinnerAndOriginalPlacedCount()throws Exception{
        var store=open();var p=proposal(store).approve(owner).begin().advance(25).issueNpc().complete();store.save(p,owner,"LEGACY_FIXTURE");
        var path=dir.resolve("kingdomsmp_construction.dat");var tag=AtomicNbt.read(path);tag.putInt("schema",1);for(var value:tag.getList("projects",Tag.TAG_COMPOUND))((CompoundTag)value).remove("foundations");AtomicNbt.write(path,tag);
        var loaded=open().get(p.id());assertEquals(p,loaded);assertEquals(25,loaded.placed());assertTrue(loaded.foundations().isEmpty());
        assertEquals(1,AtomicNbt.read(path).getInt("schema"),"Reading must not eagerly rewrite legacy saves");
    }
    @Test void malformedDepthGapsAndUnversionedPlansFailClosed()throws Exception{
        var p=proposal(open());assertThrows(IllegalArgumentException.class,()->p.withFoundations(List.of(new BlockPos(0,-5,0))));
        assertThrows(IllegalArgumentException.class,()->p.withFoundations(List.of(new BlockPos(0,-2,0))));
        assertThrows(IllegalArgumentException.class,()->p.withFoundations(List.of(new BlockPos(0,-1,0),new BlockPos(0,-1,0))));
        var store=open();store.save(p.withFoundations(cells()),owner,"PROPOSAL");var path=dir.resolve("kingdomsmp_construction.dat");var tag=AtomicNbt.read(path);tag.putInt("schema",1);AtomicNbt.write(path,tag);
        assertThrows(IllegalArgumentException.class,this::open);
    }
}
