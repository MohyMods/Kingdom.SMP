package com.mohymods.kingdomsmp.economy;
import static org.junit.jupiter.api.Assertions.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import com.mohymods.kingdomsmp.profession.*;
import java.nio.file.Path;
import java.util.*;
import java.io.IOException;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
/** Pure replay/receipt boundary tests; native human inspection and inventory debit tested separately. */
class LibraryGlyphCatalogueStoreTest {
 @TempDir Path directory;
 static List<CompoundTag> items(){return List.of("conjure_water","crush","grow","smelt").stream().map(id->{var n=new CompoundTag();n.putString("id","ars_nouveau:glyph_"+id);n.putInt("count",1);return n;}).toList();}
 @Test void inspectionDeliveryIsAtomicOnceOnlyAndAfterRenameRecoveryKeepsLegacyRecords()throws Exception{
  var f=new LibraryJourneymanStoreTest();f.directory=directory;var s=f.earnedApprentice();var op=UUID.randomUUID();var admin=UUID.randomUUID();var pending=s.prepareLibraryCatalogue(op,f.p,admin,items(),"Pure canonical on-site catalogue boundary");assertEquals(800,s.professions(f.p).track(Profession.LIBRARIAN).xp());assertFalse(s.recoverItem(new EconomyStore.Receipt(s.id(),s.expectedReceipt(f.p))));assertEquals(800,s.professions(f.p).track(Profession.LIBRARIAN).xp());pending=s.prepareLibraryCatalogue(op,f.p,admin,items(),"Pure canonical on-site catalogue boundary");var sequence=pending.sequence();var fault=EconomyStore.open(directory,(file,data)->{AtomicNbt.write(file,data);throw new IOException("Catalogue after-rename fault");});assertThrows(IOException.class,()->fault.recoverItem(new EconomyStore.Receipt(s.id(),sequence)));var restored=EconomyStore.open(directory);assertFalse(restored.recoverItem(new EconomyStore.Receipt(s.id(),sequence)));assertEquals(1000,restored.professions(f.p).track(Profession.LIBRARIAN).xp());assertThrows(IllegalStateException.class,()->restored.prepareLibraryCatalogue(UUID.randomUUID(),f.p,admin,items(),"Duplicate inspected glyphs"));restored.archive(admin,"Canonical inspected catalogue archive");assertEquals(restored.professions(f.p),EconomyStore.open(directory).professions(f.p));assertEquals(0,restored.balance(f.p));assertTrue(restored.itemMailbox(f.p).isEmpty());
 }
 @Test void duplicateWrongTierUnknownCountOrMissingBandCannotBecomeAValidCatalogue()throws Exception{
  var f=new LibraryJourneymanStoreTest();f.directory=directory;var s=f.earnedApprentice();var admin=UUID.randomUUID();var bad=new ArrayList<>(items());bad.set(3,bad.getFirst());assertThrows(IllegalArgumentException.class,()->s.prepareLibraryCatalogue(UUID.randomUUID(),f.p,admin,bad,"Four copies are not four glyph definitions"));bad.set(3,items().get(3).copy());bad.get(3).putString("id","ars_nouveau:glyph_break");assertThrows(IllegalArgumentException.class,()->s.prepareLibraryCatalogue(UUID.randomUUID(),f.p,admin,bad,"Tier1 does not replace Tier2"));bad.get(3).putString("id","unknown:glyph_smelt");assertThrows(IllegalArgumentException.class,()->s.prepareLibraryCatalogue(UUID.randomUUID(),f.p,admin,bad,"Unknown namespace refused"));var extra=new ArrayList<>(items());extra.set(0,extra.getFirst().copy());extra.getFirst().putInt("count",2);assertThrows(IllegalArgumentException.class,()->s.prepareLibraryCatalogue(UUID.randomUUID(),f.p,admin,extra,"Bounded exact quantities"));assertThrows(IllegalStateException.class,()->s.prepareLibraryCatalogue(UUID.randomUUID(),UUID.randomUUID(),admin,items(),"No approved Apprentice prerequisites"));assertEquals(800,s.professions(f.p).track(Profession.LIBRARIAN).xp());assertTrue(s.pendingItem().isEmpty());
 }
}
