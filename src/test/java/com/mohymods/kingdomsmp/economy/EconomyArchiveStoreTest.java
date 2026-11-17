package com.mohymods.kingdomsmp.economy;

import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EconomyArchiveStoreTest {
    @TempDir Path data;
    final UUID player=UUID.randomUUID(),other=UUID.randomUUID(),admin=UUID.randomUUID();
    Path journal(){return data.resolve("kingdomsmp_economy.dat");}
    EconomyStore open()throws IOException{return EconomyStore.open(data);}
    EconomyStore seed()throws Exception{var s=open();s.adminSet(UUID.randomUUID(),admin,player,2000,"Fixture purse");var sale=s.prepareSale(UUID.randomUUID(),player,"minecraft:wheat",64,64,123,20000,1000,2000);assertTrue(s.recoverSale(new EconomyStore.Receipt(s.id(),sale.sequence())));s.purchase(UUID.randomUUID(),player,"Alice","kingdom_smp:farmers_house",1000);return s;}
    @Test void closedHistoryRetainsEntitlementMoneyUsageReceiptAndOnceOnlyMailAcrossReopen()throws Exception{
        var s=seed();var reward=s.queueCoins(UUID.randomUUID(),player,23,"Fixture reward");long sequence=s.expectedReceipt(player),revision=s.revision();var id=s.id();var before=Files.readAllBytes(journal());open();assertArrayEquals(before,Files.readAllBytes(journal()));
        var status=s.archive(admin,"Verified maintenance");assertEquals(revision,status.revision());assertEquals(0,status.activeOperations());assertEquals(1,status.closedSegments());assertEquals(2,AtomicNbt.read(journal()).getInt("schema"));
        var restored=open();assertEquals(id,restored.id());assertEquals(revision,restored.revision());assertEquals(sequence,restored.expectedReceipt(player));assertEquals(1064,restored.balance(player));assertTrue(restored.purchase("kingdom_smp:farmers_house").isPresent());assertEquals(new EconomyStore.Usage(64,64,64),restored.usage(123,"minecraft:wheat",player));
        restored.claimCoins(player,reward.id());assertEquals(1087,restored.balance(player));restored=open();restored.claimCoins(player,reward.id());assertEquals(1087,restored.balance(player));assertEquals(revision+1,restored.revision());
        var next=restored.prepareSale(UUID.randomUUID(),player,"minecraft:wheat",1,1,123,20000,1000,2000);assertEquals(revision+2,next.sequence());assertEquals(sequence,next.previousReceipt());restored.recoverSale(new EconomyStore.Receipt(restored.id(),next.sequence()));assertEquals(next.sequence(),open().expectedReceipt(player));
    }
    @Test void pendingNativeOperationCannotBeArchivedOrDisappear()throws Exception{
        var s=seed();var pending=s.prepareSale(UUID.randomUUID(),player,"minecraft:wheat",1,1,123,20000,1000,2000);byte[] before=Files.readAllBytes(journal());assertThrows(IllegalStateException.class,()->s.archive(admin,"Unsafe pending operation"));assertArrayEquals(before,Files.readAllBytes(journal()));assertFalse(Files.exists(EconomyArchive.directory(data)));var reopened=open();assertEquals(pending,reopened.pendingSale().orElseThrow());assertTrue(reopened.recoverSale(new EconomyStore.Receipt(reopened.id(),pending.sequence())));reopened.archive(admin,"After receipt recovery");assertEquals(1065,open().balance(player));
    }
    @Test void missingOrModifiedReferencedSegmentFailsClosedWithoutRegeneration()throws Exception{
        var s=seed();s.archive(admin,"Preserve audit");var root=AtomicNbt.read(journal());var segment=EconomyArchive.descriptors(root).getFirst();Path file=EconomyArchive.directory(data).resolve(segment.file());byte[] bytes=Files.readAllBytes(file),main=Files.readAllBytes(journal());Files.delete(file);assertThrows(IOException.class,this::open);assertFalse(Files.exists(file));assertArrayEquals(main,Files.readAllBytes(journal()));Files.write(file,bytes);assertEquals(1064,open().balance(player));Files.writeString(file,"changed");assertThrows(IOException.class,this::open);assertArrayEquals(main,Files.readAllBytes(journal()));
    }
    @Test void archiveWriteBeforeMainPublicationLeavesOnlyNonAuthoritativeOrphans()throws Exception{
        var s=seed();byte[] main=Files.readAllBytes(journal());var failing=EconomyStore.open(data,(path,tag)->{throw new IOException("Before main rename");});assertThrows(IOException.class,()->failing.archive(admin,"Interrupted archive"));assertTrue(failing.quarantined());assertArrayEquals(main,Files.readAllBytes(journal()));try(var files=Files.list(EconomyArchive.directory(data))){assertTrue(files.findAny().isPresent());}var restored=open();assertEquals(0,restored.archiveStatus().closedSegments());assertEquals(s.revision(),restored.revision());assertEquals(1064,restored.balance(player));restored.archive(admin,"Retry from matching journal");assertEquals(1064,open().balance(player));
    }
    @Test void uncertainMainRenameReopensExactlyOnceWithoutResettingReceiptOrIds()throws Exception{
        var s=seed();var purchase=s.purchase("kingdom_smp:farmers_house").orElseThrow();long seq=s.expectedReceipt(player);var failing=EconomyStore.open(data,(path,tag)->{AtomicNbt.write(path,tag);throw new IOException("After main rename");});assertThrows(IOException.class,()->failing.archive(admin,"After-rename interruption"));var restored=open();assertEquals(1,restored.archiveStatus().closedSegments());assertEquals(seq,restored.expectedReceipt(player));assertEquals(purchase,restored.purchase(purchase.operation(),player,"Alice",purchase.building(),purchase.price()));assertEquals(1064,restored.balance(player));assertEquals(s.revision(),restored.revision());
    }
    @Test void manifestCannotReorderAClosedChain()throws Exception{
        var s=seed();s.archive(admin,"First prefix");s.adminSet(UUID.randomUUID(),admin,other,5,"Second prefix");s.archive(admin,"Second prefix");var root=AtomicNbt.read(journal());assertEquals(2,root.getList("archives",Tag.TAG_COMPOUND).size());var rows=root.getList("archives",Tag.TAG_COMPOUND);var first=rows.get(0);rows.set(0,rows.get(1));rows.set(1,first);AtomicNbt.write(journal(),root);assertThrows(IOException.class,this::open);
    }
    @Test void aValidForeignWorldArchiveCannotReplaceThisWorldHistory()throws Exception{
        var s=seed();s.archive(admin,"Original world");var foreignData=data.resolve("foreign");var foreign=EconomyStore.open(foreignData);for(int i=0;i<3;i++)foreign.adminSet(UUID.randomUUID(),admin,player,i,"Other world");foreign.archive(admin,"Foreign audit");
        var root=AtomicNbt.read(journal());var otherRoot=AtomicNbt.read(foreignData.resolve("kingdomsmp_economy.dat"));for(var ref:EconomyArchive.descriptors(otherRoot))Files.copy(EconomyArchive.directory(foreignData).resolve(ref.file()),EconomyArchive.directory(data).resolve(ref.file()));root.put("archives",otherRoot.getList("archives",Tag.TAG_COMPOUND).copy());AtomicNbt.write(journal(),root);assertThrows(IOException.class,this::open);
    }
    @Test void automaticRotationKeepsRecentTailAndAllHistoricalIdempotency()throws Exception{
        var s=open();UUID first=UUID.randomUUID();s.adminSet(first,admin,player,0,"Scale fixture");var root=AtomicNbt.read(journal());var template=root.getList("history",Tag.TAG_COMPOUND).getCompound(0);var records=new ListTag();for(int n=0;n<8192;n++){var row=template.copy();row.putLong("sequence",n+1);if(n>0)row.putUUID("id",UUID.randomUUID());row.putLong("amount",n);records.add(row);}root.put("history",records);root.putLong("revision",records.size());AtomicNbt.write(journal(),root);
        s=open();assertEquals(0,s.archiveStatus().closedSegments());s.adminSet(UUID.randomUUID(),admin,player,10000,"After automatic rotation");assertEquals(8193,s.revision());assertEquals(129,s.archiveStatus().activeOperations());assertEquals(2,s.archiveStatus().closedSegments());
        var restored=open();restored.adminSet(first,admin,player,0,"Scale fixture");assertEquals(10000,restored.balance(player));assertEquals(8193,restored.revision());restored.archive(admin,"Close recent tail too");var reward=restored.queueCoins(UUID.randomUUID(),player,23,"After repeated archival");restored.claimCoins(player,reward.id());assertEquals(10023,open().balance(player));assertEquals(8195,open().revision());
    }
    @Test void globalRevisionCanPassTheOldFiftyThousandCeilingWithoutResetOrForgottenIds()throws Exception{
        var s=open();UUID first=UUID.randomUUID();s.adminSet(first,admin,player,0,"Long history fixture");var root=AtomicNbt.read(journal());var template=root.getList("history",Tag.TAG_COMPOUND).getCompound(0);List<EconomyArchive.Segment> refs=new ArrayList<>();String previous="";int sequence=1;
        while(sequence<=50000){var rows=new ListTag();long start=sequence;while(sequence<=50000&&rows.size()<4096){var row=template.copy();row.putLong("sequence",sequence);if(sequence>1)row.putUUID("id",UUID.randomUUID());row.putLong("amount",sequence-1L);rows.add(row);sequence++;}var ref=EconomyArchive.seal(data,s.id(),start,previous,rows,admin,"Explicit long-history fixture");refs.add(ref);previous=ref.sha256();}
        root.putInt("schema",2);root.put("archives",EconomyArchive.descriptors(refs));root.put("history",new ListTag());root.putLong("revision",50000);AtomicNbt.write(journal(),root);var restored=open();assertEquals(49999,restored.balance(player));restored.adminSet(UUID.randomUUID(),admin,player,1234,"Beyond old active file limit");restored.adminSet(first,admin,player,0,"Long history fixture");assertEquals(1234,restored.balance(player));assertEquals(50001,restored.revision());assertEquals(1,restored.archiveStatus().activeOperations());assertEquals(1234,open().balance(player));assertEquals(50001,open().revision());
    }
    @Test void duplicatedTransactionIdentityAcrossAuthenticSegmentsIsNotForgotten()throws Exception{
        var s=open();s.adminSet(UUID.randomUUID(),admin,player,1,"Original identity");var root=AtomicNbt.read(journal());var row=root.getList("history",Tag.TAG_COMPOUND).getCompound(0);var a=new ListTag();a.add(row.copy());var first=EconomyArchive.seal(data,s.id(),1,"",a,admin,"First closed record");var copy=row.copy();copy.putLong("sequence",2);copy.putLong("amount",2);var b=new ListTag();b.add(copy);var second=EconomyArchive.seal(data,s.id(),2,first.sha256(),b,admin,"Invalid duplicate identity fixture");root.putInt("schema",2);root.putLong("revision",2);root.put("history",new ListTag());root.put("archives",EconomyArchive.descriptors(List.of(first,second)));AtomicNbt.write(journal(),root);assertThrows(IOException.class,this::open);
    }
    @Test void validFileHashesCannotSubstituteForTheActualParentChain()throws Exception{
        var s=open();s.adminSet(UUID.randomUUID(),admin,player,1,"Chain fixture");var root=AtomicNbt.read(journal());var row=root.getList("history",Tag.TAG_COMPOUND).getCompound(0);var a=new ListTag();a.add(row.copy());var first=EconomyArchive.seal(data,s.id(),1,"",a,admin,"Original chain");var next=row.copy();next.putUUID("id",UUID.randomUUID());next.putLong("sequence",2);var b=new ListTag();b.add(next);var broken=EconomyArchive.seal(data,s.id(),2,"0".repeat(64),b,admin,"Disconnected parent fixture");root.putInt("schema",2);root.putLong("revision",2);root.put("history",new ListTag());root.put("archives",EconomyArchive.descriptors(List.of(first,broken)));AtomicNbt.write(journal(),root);assertThrows(IOException.class,this::open);
    }
    @Test void losingBothAuthorityFilesCannotRegenerateMoneyWhileClosedHistoryExists()throws Exception{
        var s=seed();s.archive(admin,"Preserve authority identity");Files.delete(journal());Files.delete(data.resolve("kingdomsmp_economy.created"));assertThrows(IOException.class,this::open);assertFalse(Files.exists(journal()));assertFalse(Files.exists(data.resolve("kingdomsmp_economy.created")));try(var files=Files.list(EconomyArchive.directory(data))){assertTrue(files.findAny().isPresent());}
    }
}
