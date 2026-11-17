package com.mohymods.kingdomsmp.economy;
import static org.junit.jupiter.api.Assertions.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import com.mohymods.kingdomsmp.profession.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class MineAccessStoreTest{
 @TempDir Path directory;
 @Test void earnedOnceOnlyEventAndAfterRenameRecoveryPreserveCurrencyAndProgress()throws Exception{var p=UUID.randomUUID();var s=EconomyStore.open(directory);s.changeProfession(UUID.randomUUID(),p,"enroll","","miner");var op=UUID.randomUUID();var bad=EconomyStore.open(directory,(path,data)->{AtomicNbt.write(path,data);throw new java.io.IOException("Private after-rename fixture");});assertThrows(java.io.IOException.class,()->bad.completeMineAccess(op,p,"a".repeat(32),16));var reopened=EconomyStore.open(directory);reopened.completeMineAccess(op,p,"a".repeat(32),16);assertEquals(100,reopened.professions(p).track(Profession.MINER).xp());assertEquals(0,reopened.balance(p));assertTrue(reopened.mailbox(p).isEmpty());assertThrows(IllegalStateException.class,()->reopened.completeMineAccess(UUID.randomUUID(),p,"a".repeat(32),16));}
 @Test void missingIdentityBadDigestUnboundedCountAndInactiveRoleCannotAward()throws Exception{var p=UUID.randomUUID();var s=EconomyStore.open(directory);s.changeProfession(UUID.randomUUID(),p,"enroll","","miner");assertThrows(IllegalArgumentException.class,()->s.completeMineAccess(UUID.randomUUID(),p,"client count",16));assertThrows(IllegalArgumentException.class,()->s.completeMineAccess(UUID.randomUUID(),p,"a".repeat(32),513));assertThrows(IllegalStateException.class,()->s.completeMineAccess(UUID.randomUUID(),UUID.randomUUID(),"a".repeat(32),16));assertEquals(0,s.professions(p).track(Profession.MINER).xp());}
}
