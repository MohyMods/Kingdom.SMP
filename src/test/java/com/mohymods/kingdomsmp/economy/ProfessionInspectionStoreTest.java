package com.mohymods.kingdomsmp.economy;

import static org.junit.jupiter.api.Assertions.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import com.mohymods.kingdomsmp.profession.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProfessionInspectionStoreTest {
    @TempDir Path directory;final UUID player=UUID.randomUUID(),admin=UUID.randomUUID();
    static final String QUEST="kingdom_smp:builder/a04";
    @Test void inspectionAuditAndXpRemainAtomicOnceOnlyAcrossArchiveAndReplay()throws Exception{
        var s=EconomyStore.open(directory);s.changeProfession(UUID.randomUUID(),player,"enroll","","builder");
        var operation=UUID.randomUUID();s.approveProfessionInspection(operation,admin,player,QUEST,"minecraft:overworld@12,64,20;clear entry; owner authorship and consent inspected");
        long revision=s.revision();s.approveProfessionInspection(operation,admin,player,QUEST,"minecraft:overworld@12,64,20;clear entry; owner authorship and consent inspected");
        assertEquals(revision,s.revision());assertEquals(100,s.professions(player).track(Profession.BUILDER).xp());
        assertThrows(IllegalStateException.class,()->s.approveProfessionInspection(UUID.randomUUID(),admin,player,QUEST,"duplicate review"));
        s.archive(admin,"Private inspection audit archive");var reopened=EconomyStore.open(directory);
        assertEquals(100,reopened.professions(player).track(Profession.BUILDER).xp());assertEquals(0,reopened.balance(player));assertTrue(reopened.mailbox(player).isEmpty());
    }
    @Test void unimplementedWrongAuthorityKindMissingEvidenceAndInactiveRoleCannotAdvance()throws Exception{
        var s=EconomyStore.open(directory);s.changeProfession(UUID.randomUUID(),player,"enroll","","builder");
        assertThrows(IllegalArgumentException.class,()->s.completeCommissionQuest(UUID.randomUUID(),player,QUEST,1));
        assertThrows(IllegalArgumentException.class,()->s.completeFarmerQuest(UUID.randomUUID(),player,QUEST,"forged event"));
        assertThrows(IllegalArgumentException.class,()->s.approveProfessionInspection(UUID.randomUUID(),admin,player,"kingdom_smp:blacksmith/m08","no validated adapter"));
        assertThrows(IllegalArgumentException.class,()->s.approveProfessionInspection(UUID.randomUUID(),admin,player,QUEST,""));
        assertThrows(IllegalStateException.class,()->s.approveProfessionInspection(UUID.randomUUID(),admin,player,"kingdom_smp:lumberjack/a06","inactive profession"));
        assertEquals(0,s.professions(player).track(Profession.BUILDER).xp());
    }
    @Test void interruptedAfterRenameRestoresOneInspectionAndItsEarnedXp()throws Exception{
        var s=EconomyStore.open(directory);s.changeProfession(UUID.randomUUID(),player,"enroll","","builder");
        var failing=EconomyStore.open(directory,(p,d)->{AtomicNbt.write(p,d);throw new IOException("Private after-rename inspection fault");});
        var operation=UUID.randomUUID();assertThrows(IOException.class,()->failing.approveProfessionInspection(operation,admin,player,QUEST,"Site and working access personally inspected"));
        var reopened=EconomyStore.open(directory);reopened.approveProfessionInspection(operation,admin,player,QUEST,"Site and working access personally inspected");
        assertEquals(100,reopened.professions(player).track(Profession.BUILDER).xp());
    }
}
