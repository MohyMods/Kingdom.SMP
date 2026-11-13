package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class SupplementariesInspectionTest {
 @Test void variantsDoNotReplaceFourDistinctWorkingPurposes(){assertEquals("sconce",SupplementariesInspection.kind("supplementaries:sconce_wall"));assertEquals("sconce",SupplementariesInspection.kind("supplementaries:sconce"));assertNull(SupplementariesInspection.kind("minecraft:torch"));assertNull(SupplementariesInspection.kind("client:sack"));assertEquals(4,SupplementariesInspection.KINDS.size());}
 @Test void unreadableAndUnboundedNoticesCannotClaimTheMessageArea(){assertFalse(SupplementariesInspection.readable("  \n "));assertFalse(SupplementariesInspection.readable("x".repeat(8193)));assertTrue(SupplementariesInspection.readable("Keep the stair route clear for every kingdom resident."));}
 @Test void anAuthorNameDoesNotMakeAnEmptyDisplayedBookReadable(){var book=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITTEN_BOOK);book.set(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT,new net.minecraft.world.item.component.WrittenBookContent(net.minecraft.server.network.Filterable.passThrough("Notice"),"LongAuthorName",0,java.util.List.of(net.minecraft.server.network.Filterable.<net.minecraft.network.chat.Component>passThrough(net.minecraft.network.chat.Component.empty())),true));assertNull(SupplementariesInspection.notice(book));}
 @Test void nativeFurnishingsNeverBypassEarnedMasterBandOrTierFour(){var d=InspectionQuests.get(SupplementariesInspection.QUEST);assertEquals(4,d.minimumTier());assertEquals(300,d.xp());assertFalse(InspectionQuests.catalog().mayAttempt(Profession.BUILDER,d.id(),java.util.Set.of()));}
}
