package com.mohymods.kingdomsmp.economy;
import static org.junit.jupiter.api.Assertions.*;
import com.mohymods.kingdomsmp.profession.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import java.nio.file.Path;
import java.util.*;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class TreeQuestStoreTest {
    @TempDir Path dir;final UUID p=UUID.randomUUID();final String id="kingdom_smp:lumberjack/a01";
    @Test void nativeRecognitionIsOnceOnlyAndCannotGrantMoneyOrFakeOtherProfessions()throws Exception{
        var s=EconomyStore.open(dir);s.changeProfession(UUID.randomUUID(),p,"enroll","","lumberjack");var operation=UUID.randomUUID();s.completeTreeQuest(operation,p,id,128,128);long rev=s.revision();s.completeTreeQuest(operation,p,id,128,128);assertEquals(rev,s.revision());assertEquals(100,s.professions(p).track(Profession.LUMBERJACK).xp());assertEquals(0,s.balance(p));assertTrue(s.mailbox(p).isEmpty());assertThrows(IllegalStateException.class,()->s.completeTreeQuest(UUID.randomUUID(),p,id,128,128));assertThrows(IllegalArgumentException.class,()->s.completeTreeQuest(UUID.randomUUID(),p,"kingdom_smp:builder/a01",128,128));
        s.archive(p,"Private native tree recognition archive");assertEquals(100,EconomyStore.open(dir).professions(p).track(Profession.LUMBERJACK).xp());
    }
    @Test void inactiveInsufficientOrInvalidQuantitiesCannotAward()throws Exception{
        var s=EconomyStore.open(dir);assertThrows(IllegalStateException.class,()->s.completeTreeQuest(UUID.randomUUID(),p,id,128,128));s.changeProfession(UUID.randomUUID(),p,"enroll","","lumberjack");assertThrows(IllegalArgumentException.class,()->s.completeTreeQuest(UUID.randomUUID(),p,id,128,127));assertThrows(IllegalArgumentException.class,()->s.completeTreeQuest(UUID.randomUUID(),p,id,0,100));assertEquals(0,s.professions(p).track(Profession.LUMBERJACK).xp());
    }
    @Test void afterRenameFailureRestoresExactlyOneBoundXpAward()throws Exception{
        var s=EconomyStore.open(dir);s.changeProfession(UUID.randomUUID(),p,"enroll","","lumberjack");var operation=UUID.randomUUID();var faulty=EconomyStore.open(dir,(f,t)->{AtomicNbt.write(f,t);throw new IOException("Private tree-XP after-rename fault");});assertThrows(IOException.class,()->faulty.completeTreeQuest(operation,p,id,128,128));var restored=EconomyStore.open(dir);restored.completeTreeQuest(operation,p,id,128,128);assertEquals(100,restored.professions(p).track(Profession.LUMBERJACK).xp());
    }
}
