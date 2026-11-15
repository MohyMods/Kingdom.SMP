package com.mohymods.kingdomsmp.economy;
import static org.junit.jupiter.api.Assertions.*;
import com.mohymods.kingdomsmp.profession.*;
import java.util.*;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class MineExcavationStoreTest {
 @TempDir Path path;
 @Test void inactiveAndUnfinishedTrialsCannotAuthorizeWorkOrMintAnything()throws Exception{var s=EconomyStore.open(path);var p=UUID.randomUUID();var site=new MineExcavationSite(UUID.randomUUID(),UUID.randomUUID(),p,"minecraft:overworld",net.minecraft.core.BlockPos.ZERO,new net.minecraft.core.BlockPos(3,3,3),"a".repeat(64),"Purpose and owner consent reviewed");assertThrows(IllegalStateException.class,()->s.authorizeMineExcavation(site));s.changeProfession(UUID.randomUUID(),p,"enroll","","miner");assertThrows(IllegalStateException.class,()->s.authorizeMineExcavation(site));assertTrue(s.mineExcavation(p).isEmpty());assertTrue(EconomyStore.open(path).mineExcavation(p).isEmpty());assertEquals(0,s.balance(p));assertEquals(0,s.professions(p).track(Profession.MINER).xp());assertTrue(s.mailbox(p).isEmpty());}
}
