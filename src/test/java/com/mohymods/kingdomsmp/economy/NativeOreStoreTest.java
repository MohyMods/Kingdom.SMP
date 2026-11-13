package com.mohymods.kingdomsmp.economy;
import static org.junit.jupiter.api.Assertions.*;
import com.mohymods.kingdomsmp.profession.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import java.nio.file.Path;
import java.util.*;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class NativeOreStoreTest {
    @TempDir Path dir;final UUID p=UUID.randomUUID();final String id="kingdom_smp:miner/a01";
    @Test void nativeRecognitionIsOnceOnlyAndCannotGrantMoneyOrFakeOtherProfessions()throws Exception{
        var s=EconomyStore.open(dir);s.changeProfession(UUID.randomUUID(),p,"enroll","","miner");var operation=UUID.randomUUID();s.completeNativeOreQuest(operation,p,id,64,64);long rev=s.revision();s.completeNativeOreQuest(operation,p,id,64,64);assertEquals(rev,s.revision());assertEquals(100,s.professions(p).track(Profession.MINER).xp());assertEquals(0,s.balance(p));assertTrue(s.mailbox(p).isEmpty());assertThrows(IllegalStateException.class,()->s.completeNativeOreQuest(UUID.randomUUID(),p,id,64,64));assertThrows(IllegalArgumentException.class,()->s.completeNativeOreQuest(UUID.randomUUID(),p,"kingdom_smp:builder/a01",64,64));
        s.archive(p,"Private native ore recognition archive");assertEquals(100,EconomyStore.open(dir).professions(p).track(Profession.MINER).xp());
    }
    @Test void inactiveInsufficientOrInvalidQuantitiesCannotAward()throws Exception{
        var s=EconomyStore.open(dir);assertThrows(IllegalStateException.class,()->s.completeNativeOreQuest(UUID.randomUUID(),p,id,64,64));s.changeProfession(UUID.randomUUID(),p,"enroll","","miner");assertThrows(IllegalArgumentException.class,()->s.completeNativeOreQuest(UUID.randomUUID(),p,id,64,63));assertThrows(IllegalArgumentException.class,()->s.completeNativeOreQuest(UUID.randomUUID(),p,id,0,100));assertEquals(0,s.professions(p).track(Profession.MINER).xp());
    }
    @Test void afterRenameFailureRestoresExactlyOneBoundXpAward()throws Exception{
        var s=EconomyStore.open(dir);s.changeProfession(UUID.randomUUID(),p,"enroll","","miner");var operation=UUID.randomUUID();var faulty=EconomyStore.open(dir,(f,t)->{AtomicNbt.write(f,t);throw new IOException("Private ore-XP after-rename fault");});assertThrows(IOException.class,()->faulty.completeNativeOreQuest(operation,p,id,64,64));var restored=EconomyStore.open(dir);restored.completeNativeOreQuest(operation,p,id,64,64);assertEquals(100,restored.professions(p).track(Profession.MINER).xp());
    }
}
