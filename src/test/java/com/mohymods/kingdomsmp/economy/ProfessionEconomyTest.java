package com.mohymods.kingdomsmp.economy;
import com.mohymods.kingdomsmp.profession.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ProfessionEconomyTest {
 @TempDir Path dir;final UUID player=UUID.randomUUID();
 EconomyStore open()throws IOException{return EconomyStore.open(dir);}
 void fund(EconomyStore s,long amount)throws IOException{s.adminSet(UUID.randomUUID(),player,player,amount,"Profession test funding");}
 void enroll(EconomyStore s,String role)throws IOException{s.changeProfession(UUID.randomUUID(),player,"enroll","",role);}
 @Test void initialSlotsFreePaidSwitchAtomicAndPersistent()throws Exception{var s=open();fund(s,20000);enroll(s,"farmer");enroll(s,"builder");enroll(s,"lumberjack");assertEquals(20000,s.balance(player));s.changeProfession(UUID.randomUUID(),player,"switch","farmer","miner");var r=open();assertEquals(5000,r.balance(player));assertFalse(r.professions(player).isActive(Profession.FARMER));assertTrue(r.professions(player).tracks().containsKey(Profession.FARMER));assertTrue(r.professions(player).isActive(Profession.MINER));assertEquals(3,r.professions(player).active().size());}
 @Test void insufficientFeeDoesNotChangeEitherAuthority()throws Exception{var s=open();enroll(s,"farmer");long before=s.revision();assertThrows(IllegalStateException.class,()->s.changeProfession(UUID.randomUUID(),player,"switch","farmer","builder"));assertEquals(before,s.revision());assertTrue(open().professions(player).isActive(Profession.FARMER));assertEquals(0,s.balance(player));}
 @Test void cannotAddFourthOrUseDeactivationShortcut()throws Exception{var s=open();fund(s,100000);for(var role:List.of("farmer","builder","lumberjack"))enroll(s,role);assertThrows(IllegalStateException.class,()->enroll(s,"miner"));assertThrows(IllegalArgumentException.class,()->s.changeProfession(UUID.randomUUID(),player,"deactivate","farmer",""));assertEquals(100000,s.balance(player));}
 @Test void replayAfterInterveningChangeNeverChargesTwice()throws Exception{var s=open();fund(s,45000);enroll(s,"farmer");UUID id=UUID.randomUUID();s.changeProfession(id,player,"switch","farmer","builder");s.changeProfession(UUID.randomUUID(),player,"switch","builder","farmer");long rev=s.revision();s.changeProfession(id,player,"switch","farmer","builder");assertEquals(15000,s.balance(player));assertEquals(rev,s.revision());assertTrue(s.professions(player).isActive(Profession.FARMER));assertThrows(IllegalArgumentException.class,()->s.changeProfession(id,player,"switch","farmer","miner"));}
 @Test void pendingInventoryOperationBlocksRosterAndWalletTogether()throws Exception{var s=open();fund(s,20000);enroll(s,"farmer");s.prepareSale(UUID.randomUUID(),player,"minecraft:wheat",1,1,1,100,100,100);assertThrows(IllegalStateException.class,()->s.changeProfession(UUID.randomUUID(),player,"switch","farmer","builder"));assertTrue(s.professions(player).isActive(Profession.FARMER));assertEquals(20000,s.balance(player));}
 @Test void failedRenameCannotPersistOnlyOneSide()throws Exception{var s=open();fund(s,20000);enroll(s,"farmer");var failing=EconomyStore.open(dir,(path,nbt)->{AtomicNbt.write(path,nbt);throw new IOException("after rename");});assertThrows(IOException.class,()->failing.changeProfession(UUID.randomUUID(),player,"switch","farmer","builder"));var restored=open();assertEquals(5000,restored.balance(player));assertTrue(restored.professions(player).isActive(Profession.BUILDER));assertFalse(restored.professions(player).isActive(Profession.FARMER));}
}
