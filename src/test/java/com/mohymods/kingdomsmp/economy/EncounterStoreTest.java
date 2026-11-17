package com.mohymods.kingdomsmp.economy;
import static org.junit.jupiter.api.Assertions.*;
import com.mohymods.kingdomsmp.profession.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import java.util.*;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class EncounterStoreTest {
 @TempDir Path directory;
 Map<String,Integer> counts(){var counts=new HashMap<String,Integer>();HuntState.TARGETS.forEach(s->counts.put(s,6));return counts;}
 @Test void onceOnlyAwardAndAfterRenameRecoveryNeverMintCurrency()throws Exception{var p=UUID.randomUUID();var s=EconomyStore.open(directory);s.changeProfession(UUID.randomUUID(),p,"enroll","","adventurer");var op=UUID.randomUUID();var failing=EconomyStore.open(directory,(path,data)->{AtomicNbt.write(path,data);throw new java.io.IOException("Private after-rename fault");});assertThrows(java.io.IOException.class,()->failing.completeEncounterQuest(op,p,24,4,counts()));s=EconomyStore.open(directory);s.completeEncounterQuest(op,p,24,4,counts());assertEquals(100,s.professions(p).track(Profession.ADVENTURER).xp());assertEquals(0,s.balance(p));assertTrue(s.mailbox(p).isEmpty());var reopened=s;assertThrows(IllegalStateException.class,()->reopened.completeEncounterQuest(UUID.randomUUID(),p,24,4,counts()));}
 @Test void incompleteUnbalancedWrongRoleAndInvalidGoalsCannotAdvance()throws Exception{var p=UUID.randomUUID();var s=EconomyStore.open(directory);s.changeProfession(UUID.randomUUID(),p,"enroll","","adventurer");var c=counts();c.put("minecraft:spider",3);assertThrows(IllegalArgumentException.class,()->s.completeEncounterQuest(UUID.randomUUID(),p,24,4,c));assertThrows(IllegalArgumentException.class,()->s.completeEncounterQuest(UUID.randomUUID(),p,24,7,counts()));assertThrows(IllegalStateException.class,()->s.completeEncounterQuest(UUID.randomUUID(),UUID.randomUUID(),24,4,counts()));assertEquals(0,s.professions(p).track(Profession.ADVENTURER).xp());}
}
