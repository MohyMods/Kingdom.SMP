package com.mohymods.kingdomsmp.economy;
import static org.junit.jupiter.api.Assertions.*;
import com.mohymods.kingdomsmp.profession.*;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
/** Pure canonical-boundary tests; native service and claim tests remain separate. */
class MerchantTransactionStoreTest {
 @TempDir Path directory;UUID merchant=UUID.randomUUID(),peer=UUID.randomUUID();
 EconomyStore open()throws Exception{return EconomyStore.open(directory);}
 EconomyStore enrolled()throws Exception{var s=open();s.changeProfession(UUID.randomUUID(),merchant,"enroll","","merchant");return s;}
 static CompoundTag item(){var n=new CompoundTag();n.putString("id","minecraft:iron_ingot");n.putInt("count",1);return n;}
 void transfer(EconomyStore s,long amount)throws Exception{var p=s.prepareTransfer(UUID.randomUUID(),merchant,amount);assertTrue(s.recoverTransfer(new EconomyStore.Receipt(s.id(),p.sequence())));}
 void list(EconomyStore s,UUID id,UUID seller,long price)throws Exception{var p=s.prepareListing(id,seller,"Peer","Native model item",price,false,100,1000,item());s.recoverItem(new EconomyStore.Receipt(s.id(),p.sequence()));}
 @Test void bankRequiresBothCommittedDirectionsAndActiveEnrollment()throws Exception{
  var s=open();transfer(s,100);assertEquals(0,s.merchantProof(merchant).deposited());s.changeProfession(UUID.randomUUID(),merchant,"enroll","","merchant");
  var pending=s.prepareTransfer(UUID.randomUUID(),merchant,50);assertEquals(0,s.merchantProof(merchant).deposited());s.recoverTransfer(new EconomyStore.Receipt(s.id(),pending.previousReceipt()));assertEquals(0,open().merchantProof(merchant).deposited());transfer(s,49);transfer(s,-50);assertFalse(s.merchantProof(merchant).ready(MerchantTransactionQuests.DEPOSIT));transfer(s,1);assertTrue(s.merchantProof(merchant).ready(MerchantTransactionQuests.DEPOSIT));
  var id=UUID.randomUUID();s.completeMerchantTransactionQuest(id,merchant,MerchantTransactionQuests.DEPOSIT);s.completeMerchantTransactionQuest(id,merchant,MerchantTransactionQuests.DEPOSIT);assertEquals(100,open().professions(merchant).track(Profession.MERCHANT).xp());assertThrows(IllegalStateException.class,()->s.completeMerchantTransactionQuest(UUID.randomUUID(),merchant,MerchantTransactionQuests.DEPOSIT));
 }
 @Test void realFixedSaleAndPurchaseNotListingFundingOrMail()throws Exception{
  var s=enrolled();s.adminSet(UUID.randomUUID(),peer,peer,200,"Canonical funding boundary");s.adminSet(UUID.randomUUID(),peer,merchant,200,"Canonical funding boundary");var first=UUID.randomUUID();list(s,first,merchant,49);assertEquals(0,s.merchantProof(merchant).largestSale());s.buyListing(UUID.randomUUID(),peer,first,200);assertFalse(s.merchantProof(merchant).ready(MerchantTransactionQuests.SELL));var sale=UUID.randomUUID();list(s,sale,merchant,50);var buy=UUID.randomUUID();s.buyListing(buy,peer,sale,200);s.buyListing(buy,peer,sale,200);assertTrue(s.merchantProof(merchant).ready(MerchantTransactionQuests.SELL));var listing=UUID.randomUUID();list(s,listing,peer,50);s.buyListing(UUID.randomUUID(),merchant,listing,200);for(var q:List.of(MerchantTransactionQuests.SELL,MerchantTransactionQuests.BUY))s.completeMerchantTransactionQuest(UUID.randomUUID(),merchant,q);assertEquals(200,open().professions(merchant).track(Profession.MERCHANT).xp());assertEquals(9,s.taxRemoved());
 }
 @Test void supplyNeedsThreeApprovedSettledCategoriesAndSurvivesArchive()throws Exception{
  var s=enrolled();for(var key:List.of("logs","logs","unrelated","wheat")){var p=s.prepareSale(UUID.randomUUID(),merchant,"kingdom_smp:supplies/"+key,1,1,0,1000,1000,1000);assertTrue(s.recoverSale(new EconomyStore.Receipt(s.id(),p.sequence())));}assertFalse(s.merchantProof(merchant).ready(MerchantTransactionQuests.SUPPLY));var p=s.prepareSale(UUID.randomUUID(),merchant,"kingdom_smp:supplies/iron",1,1,0,1000,1000,1000);assertEquals(2,s.merchantProof(merchant).resources().size());s.recoverSale(new EconomyStore.Receipt(s.id(),p.sequence()));s.completeMerchantTransactionQuest(UUID.randomUUID(),merchant,MerchantTransactionQuests.SUPPLY);s.archive(peer,"Merchant derived evidence archive regression");assertEquals(s.merchantProof(merchant),open().merchantProof(merchant));assertEquals(100,open().professions(merchant).track(Profession.MERCHANT).xp());
 }
 @Test void orderRequiresCommittedForeignFillAndNoDuplicateAward()throws Exception{
  var s=enrolled();s.adminSet(UUID.randomUUID(),peer,peer,100,"Canonical funding boundary");s.createOrder(UUID.randomUUID(),peer,"Buyer","Iron",item(),1,50,false,100,1000);var order=s.orders().getFirst();assertFalse(s.merchantProof(merchant).orderFilled());var fill=s.prepareOrderFill(UUID.randomUUID(),merchant,order.id(),1,item(),200);assertFalse(s.merchantProof(merchant).orderFilled());s.recoverItem(new EconomyStore.Receipt(s.id(),fill.sequence()));assertTrue(s.merchantProof(merchant).orderFilled());s.completeMerchantTransactionQuest(UUID.randomUUID(),merchant,MerchantTransactionQuests.ORDER);assertEquals(100,open().professions(merchant).track(Profession.MERCHANT).xp());assertEquals(3,s.taxRemoved());
 }
 @Test void inactiveMerchantEarnsNoNewProofAndCannotSpendSavedProof()throws Exception{
  var s=enrolled();transfer(s,50);transfer(s,-50);s.adminSet(UUID.randomUUID(),peer,merchant,20000,"Switch boundary funding");s.changeProfession(UUID.randomUUID(),merchant,"switch","merchant","farmer");transfer(s,100);assertEquals(50,s.merchantProof(merchant).deposited());assertThrows(IllegalStateException.class,()->s.completeMerchantTransactionQuest(UUID.randomUUID(),merchant,MerchantTransactionQuests.DEPOSIT));assertEquals(0,s.professions(merchant).track(Profession.MERCHANT).xp());
 }
}
