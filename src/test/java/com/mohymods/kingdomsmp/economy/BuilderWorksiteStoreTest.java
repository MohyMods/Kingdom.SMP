package com.mohymods.kingdomsmp.economy;

import static org.junit.jupiter.api.Assertions.*;
import com.mohymods.kingdomsmp.profession.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BuilderWorksiteStoreTest {
    @TempDir Path directory;final UUID player=UUID.randomUUID(),admin=UUID.randomUUID();final String quest="kingdom_smp:builder/a01";
    EconomyStore open()throws Exception{var s=EconomyStore.open(directory);if(!s.professions(player).isActive(Profession.BUILDER))s.changeProfession(UUID.randomUUID(),player,"enroll","","builder");return s;}
    BuilderWorksite site(EconomyStore s)throws Exception{return s.authorizeBuilderWorksite(UUID.randomUUID(),admin,player,quest,"minecraft:overworld",new BlockPos(1,64,1),new BlockPos(16,69,8),"Consented personal foundations and frame");}
    @Test void authorizationAndOnceOnlyAwardSurviveArchiveWithoutChangingWalletOrItems()throws Exception{
        var s=open();var site=site(s);long rev=s.revision();
        assertEquals(site,s.builderWorksite(player,quest).orElseThrow());assertEquals(0,s.professions(player).track(Profession.BUILDER).xp());
        var operation=UUID.randomUUID();s.completeRetainedBuilding(operation,player,quest,site.id(),128,128,"a".repeat(64));
        s.completeRetainedBuilding(operation,player,quest,site.id(),128,128,"a".repeat(64));assertEquals(rev+1,s.revision());
        assertEquals(100,s.professions(player).track(Profession.BUILDER).xp());assertEquals(0,s.balance(player));assertTrue(s.mailbox(player).isEmpty());
        assertThrows(IllegalStateException.class,()->s.completeRetainedBuilding(UUID.randomUUID(),player,quest,site.id(),128,128,"a".repeat(64)));
        s.archive(admin,"Private retained authorization archive");var reopened=EconomyStore.open(directory);assertEquals(site,reopened.builderWorksite(player,quest).orElseThrow());assertEquals(100,reopened.professions(player).track(Profession.BUILDER).xp());
    }
    @Test void foreignStaleMissingAndInsufficientProofCannotAward(){assertDoesNotThrow(()->{
        var s=open();var first=site(s);var current=site(s);assertNotEquals(first.id(),current.id());
        assertThrows(IllegalStateException.class,()->s.completeRetainedBuilding(UUID.randomUUID(),player,quest,first.id(),128,128,"a".repeat(64)));
        assertThrows(IllegalStateException.class,()->s.completeRetainedBuilding(UUID.randomUUID(),UUID.randomUUID(),quest,current.id(),128,128,"a".repeat(64)));
        assertThrows(IllegalArgumentException.class,()->s.completeRetainedBuilding(UUID.randomUUID(),player,quest,current.id(),128,127,"a".repeat(64)));
        assertThrows(IllegalArgumentException.class,()->s.completeRetainedBuilding(UUID.randomUUID(),player,quest,current.id(),128,128,"client assertion"));
        assertEquals(0,s.professions(player).track(Profession.BUILDER).xp());
    });}
    @Test void coordinateWrappingUnknownObjectivesAndInactiveRolesFailBeforeWriting()throws Exception{
        var inactive=EconomyStore.open(directory);assertThrows(IllegalStateException.class,()->site(inactive));
        var s=open();var store=s;long revision=s.revision();
        assertThrows(IllegalArgumentException.class,()->store.authorizeBuilderWorksite(UUID.randomUUID(),admin,player,"kingdom_smp:builder/m03","minecraft:overworld",BlockPos.ZERO,new BlockPos(1,1,1),"No validated Master site"));
        assertThrows(IllegalArgumentException.class,()->new BuilderWorksite(UUID.randomUUID(),admin,player,quest,"minecraft:overworld",new BlockPos(0,2048,0),new BlockPos(1,2049,1),"No packed Y coordinate wrapping"));
        assertThrows(IllegalArgumentException.class,()->new BuilderWorksite(UUID.randomUUID(),admin,player,quest,"minecraft:overworld",BlockPos.ZERO,new BlockPos(64,8,64),"No excessive region dimensions"));
        assertEquals(revision,s.revision());
    }
    @Test void afterRenameFaultRestoresExactlyOneBoundAward()throws Exception{
        var s=open();var site=site(s);var operation=UUID.randomUUID();var failing=EconomyStore.open(directory,(p,d)->{AtomicNbt.write(p,d);throw new IOException("Private retained-quest after-rename fault");});
        assertThrows(IOException.class,()->failing.completeRetainedBuilding(operation,player,quest,site.id(),128,128,"a".repeat(64)));
        var restored=EconomyStore.open(directory);restored.completeRetainedBuilding(operation,player,quest,site.id(),128,128,"a".repeat(64));assertEquals(100,restored.professions(player).track(Profession.BUILDER).xp());
    }
    @Test void afterRenameAuthorizationRetriesRetainExactlyOneCurrentSiteAndItsOperator()throws Exception{
        var s=open();var operation=UUID.randomUUID();var failing=EconomyStore.open(directory,(p,d)->{AtomicNbt.write(p,d);throw new IOException("Private site authorization after-rename fault");});
        assertThrows(IOException.class,()->failing.authorizeBuilderWorksite(operation,admin,player,quest,"minecraft:overworld",new BlockPos(1,64,1),new BlockPos(16,69,8),"Consented personal foundation project"));
        var recovered=EconomyStore.open(directory);var site=recovered.authorizeBuilderWorksite(operation,admin,player,quest,"minecraft:overworld",new BlockPos(1,64,1),new BlockPos(16,69,8),"Consented personal foundation project");
        assertEquals(1,recovered.builderWorksites().size());assertEquals(operation,site.id());assertEquals(admin,site.admin());assertEquals(0,recovered.professions(player).track(Profession.BUILDER).xp());
    }
    @Test void finalInspectionCannotReplaceAnyOfTheSevenRequiredPersonalProofs()throws Exception{
        var s=open();site(s);
        assertThrows(IllegalStateException.class,()->s.approveProfessionInspection(UUID.randomUUID(),admin,player,"kingdom_smp:builder/a08","Actual inspection without required earlier earned proofs"));
        assertEquals(0,s.professions(player).track(Profession.BUILDER).xp());assertEquals(ProfessionRank.APPRENTICE,s.professions(player).rank(Profession.BUILDER,ProfessionService.quests()));
    }

    @Test void laterRetainedSitesCannotBeAuthorizedBeforeTheirCompleteEarlierBands()throws Exception{
        var s=open();long revision=s.revision();for(var id:List.of("kingdom_smp:builder/j02","kingdom_smp:builder/m01","kingdom_smp:builder/m02"))assertThrows(IllegalStateException.class,()->s.authorizeBuilderWorksite(UUID.randomUUID(),admin,player,id,"minecraft:overworld",new BlockPos(0,64,0),new BlockPos(15,69,7),"Real future site must retain complete prior band requirements"));assertEquals(revision,s.revision());assertTrue(s.builderWorksites().isEmpty());assertEquals(0,s.professions(player).track(Profession.BUILDER).xp());
    }

    @Test void inspectedBuildingObjectivesCannotUseAutomaticRetainedAwards()throws Exception{
        var s=open();long revision=s.revision();for(var id:List.of("kingdom_smp:builder/j06")){assertTrue(RetainedBuildingQuests.tracked(id));assertFalse(RetainedBuildingQuests.contains(id));assertTrue(InspectionQuests.contains(id));assertThrows(IllegalArgumentException.class,()->s.completeRetainedBuilding(UUID.randomUUID(),player,id,UUID.randomUUID(),16,16,"a".repeat(64)));}assertEquals(revision,s.revision());assertEquals(0,s.professions(player).track(Profession.BUILDER).xp());
    }
    @Test void authorizationHistoryKeepsOriginalScopedRecordsWithoutChangingCurrentSite()throws Exception{
        var s=open();var a=site(s);var b=site(s);assertEquals(List.of(b,a),s.builderAuthorizationHistory());assertEquals(List.of(b),s.builderWorksites());s.archive(admin,"Read only immutable authorization history");var restored=EconomyStore.open(directory);assertEquals(List.of(b,a),restored.builderAuthorizationHistory());assertEquals(b,restored.builderWorksite(player,quest).orElseThrow());
    }

}
