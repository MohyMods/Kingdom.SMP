package com.mohymods.kingdomsmp.economy;

import static org.junit.jupiter.api.Assertions.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import com.mohymods.kingdomsmp.profession.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CraftDeliveryStoreTest {
    @TempDir Path directory;final UUID player=UUID.randomUUID();
    EconomyStore enrolled()throws Exception{var s=EconomyStore.open(directory);s.changeProfession(UUID.randomUUID(),player,"enroll","","builder");return s;}
    @Test void oldReceiptCancelsAndMatchingReceiptConsumesAndAwardsExactlyOnce()throws Exception{
        var s=enrolled();var input=CraftDeliveryQuests.defaults(CraftDeliveryQuests.MASONRY);
        s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.MASONRY,"Actual crafted outputs",input);
        assertFalse(s.recoverItem(EconomyStore.Receipt.empty()));assertEquals(0,s.professions(player).track(Profession.BUILDER).xp());
        var pending=s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.MASONRY,"Actual crafted outputs",input);
        var receipt=new EconomyStore.Receipt(s.id(),pending.sequence());
        assertTrue(s.recoverItem(receipt));assertFalse(s.recoverItem(receipt));
        assertEquals(receipt.sequence(),s.expectedReceipt(player));assertEquals(100,s.professions(player).track(Profession.BUILDER).xp());
        assertEquals(0,s.balance(player));assertTrue(s.mailbox(player).isEmpty());assertTrue(s.itemMailbox(player).isEmpty());
        s.archive(player,"Private crafted delivery archive");var reopened=EconomyStore.open(directory);
        assertEquals(100,reopened.professions(player).track(Profession.BUILDER).xp());assertEquals(receipt.sequence(),reopened.expectedReceipt(player));
        assertThrows(IllegalStateException.class,()->reopened.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.MASONRY,"repeat",input));
    }
    @Test void wrongMaterialsMissingInputsNegativeQuantitiesAndOtherAuthorityKindsFailBeforePreparing()throws Exception{
        var s=enrolled();long revision=s.revision();
        for(var input:List.of(Map.of("minecraft:diamond",32),Map.of("minecraft:stone_brick_stairs",32),Map.of("minecraft:stone_brick_stairs",-1,"minecraft:stone_brick_slab",32))){
            assertThrows(IllegalArgumentException.class,()->s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.MASONRY,"forged input",input));
        }
        assertThrows(IllegalArgumentException.class,()->s.prepareProfessionDelivery(UUID.randomUUID(),player,"kingdom_smp:farmer/a07","foreign",Map.of("minecraft:bread",32)));
        assertThrows(IllegalArgumentException.class,()->s.completeFarmerQuest(UUID.randomUUID(),player,CraftDeliveryQuests.MASONRY,"not consumed"));
        assertThrows(IllegalArgumentException.class,()->s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.MASONRY,"",CraftDeliveryQuests.defaults(CraftDeliveryQuests.MASONRY)));
        assertEquals(revision,s.revision());assertTrue(s.pendingItem().isEmpty());
    }
    @Test void afterRenameFaultRecoversOneAwardAndOneNativeReceipt()throws Exception{
        var s=enrolled();var pending=s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.MASONRY,"Verified native recipe and consumed outputs",CraftDeliveryQuests.defaults(CraftDeliveryQuests.MASONRY));
        var receipt=new EconomyStore.Receipt(s.id(),pending.sequence());
        var faulty=EconomyStore.open(directory,(p,d)->{AtomicNbt.write(p,d);throw new IOException("Private after-rename craft receipt fault");});
        assertThrows(IOException.class,()->faulty.recoverItem(receipt));var reopened=EconomyStore.open(directory);
        assertFalse(reopened.recoverItem(receipt));assertEquals(100,reopened.professions(player).track(Profession.BUILDER).xp());assertEquals(receipt.sequence(),reopened.expectedReceipt(player));
    }
    @Test void inactiveProfessionCannotPrepareConsumptionOrAwardXp()throws Exception{
        var s=EconomyStore.open(directory);assertThrows(IllegalStateException.class,()->s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.MASONRY,"no active profession",CraftDeliveryQuests.defaults(CraftDeliveryQuests.MASONRY)));
        assertTrue(s.pendingItem().isEmpty());assertEquals(0,s.professions(player).track(Profession.BUILDER).xp());
    }
}
