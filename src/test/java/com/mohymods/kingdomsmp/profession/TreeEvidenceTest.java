package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
class TreeEvidenceTest {
    final UUID player=UUID.randomUUID();final BlockPos root=new BlockPos(5,64,5);final String dim="minecraft:overworld";
    Map<Long,TreeEvidence.Log> logs(){return Map.of(root.asLong(),new TreeEvidence.Log("oak","oak"),root.above().asLong(),new TreeEvidence.Log("oak","oak"));}
    void harvest(TreeEvidence d){assertTrue(d.harvest(player,ProfessionRank.APPRENTICE,dim,root,"oak"));assertTrue(d.harvest(player,ProfessionRank.APPRENTICE,dim,root.above(),"oak"));}
    @Test void onlyRegisteredNativeLogsCountOnceAndManualInvalidationCannotCredit(){
        var d=new TreeEvidence();assertFalse(d.harvest(player,ProfessionRank.APPRENTICE,dim,root,"oak"));d.register(UUID.randomUUID(),dim,root,logs(),null,false);
        assertTrue(d.invalidate(dim,root));assertFalse(d.harvest(player,ProfessionRank.APPRENTICE,dim,root,"oak"));assertTrue(d.harvest(player,ProfessionRank.APPRENTICE,dim,root.above(),"oak"));assertFalse(d.harvest(player,ProfessionRank.APPRENTICE,dim,root.above(),"oak"));assertEquals(1,d.evidence().count(player,ProfessionRank.APPRENTICE,"oak"));
    }
    @Test void actualHarvestClearingAndSuccessfulReplacementHaveOneAtomicPendingGrowthReceipt(){
        var d=new TreeEvidence();d.register(UUID.randomUUID(),dim,root,logs(),null,false);harvest(d);assertTrue(d.plant(player,ProfessionRank.APPRENTICE,dim,root,"minecraft:oak_sapling"));var plant=d.plantAt(dim,root).orElseThrow();assertNotNull(plant.clearing());
        d.register(UUID.randomUUID(),dim,root,logs(),plant,true);assertEquals(0,d.evidence().count(player,ProfessionRank.APPRENTICE,"grown"));assertEquals(1,d.growth().size());
        var restored=TreeEvidence.load(d.save());var g=restored.growth().getFirst();assertTrue(restored.confirmGrowth(g.id(),player));assertFalse(restored.confirmGrowth(g.id(),player));assertEquals(1,restored.evidence().count(player,ProfessionRank.APPRENTICE,"grown"));assertEquals(0,restored.evidence().count(player,ProfessionRank.JOURNEYMAN,"grown"));assertTrue(restored.growth().isEmpty());
    }
    @Test void firstPlantCreatesHarvestableMatureTreeButNotUnfoundedReplacementCredit(){
        var d=new TreeEvidence();d.plant(player,ProfessionRank.APPRENTICE,dim,root,"minecraft:oak_sapling");var plant=d.plantAt(dim,root).orElseThrow();assertNull(plant.clearing());assertTrue(d.register(UUID.randomUUID(),dim,root,logs(),plant,true));assertTrue(d.growth().isEmpty());harvest(d);assertEquals(2,d.evidence().count(player,ProfessionRank.APPRENTICE,"oak"));
    }
    @Test void canceledOrReplacedSaplingCannotRedeemStaleGrowthAndOtherActorsCannotClaimReceipt(){
        var d=new TreeEvidence();d.register(UUID.randomUUID(),dim,root,logs(),null,false);harvest(d);d.plant(player,ProfessionRank.APPRENTICE,dim,root,"minecraft:oak_sapling");var old=d.plantAt(dim,root).orElseThrow();d.discardPlant(dim,root);d.plant(player,ProfessionRank.APPRENTICE,dim,root,"minecraft:oak_sapling");assertFalse(d.register(UUID.randomUUID(),dim,root,logs(),old,true));
        d.register(UUID.randomUUID(),dim,root,logs(),d.plantAt(dim,root).orElseThrow(),true);assertFalse(d.confirmGrowth(d.growth().getFirst().id(),UUID.randomUUID()));assertEquals(1,d.growth().size());
    }
    @Test void sourceSpeciesStateAndBandRemainScopedAcrossSaveAndTransform(){
        var d=new TreeEvidence();d.register(UUID.randomUUID(),dim,root,logs(),null,false);assertTrue(d.transform(dim,root,"oak","stripped"));assertTrue(d.harvest(player,ProfessionRank.JOURNEYMAN,dim,root,"stripped"));var restored=TreeEvidence.load(d.save());assertEquals(1,restored.evidence().count(player,ProfessionRank.JOURNEYMAN,"oak"));assertEquals(0,restored.evidence().count(player,ProfessionRank.APPRENTICE,"oak"));assertEquals(0,restored.evidence().count(player,ProfessionRank.JOURNEYMAN,"birch"));assertEquals(d.save(),restored.save());
    }
    @Test void duplicateLogsUnknownFieldsAndInconsistentReservationsRefuseLoading(){
        var d=new TreeEvidence();d.register(UUID.randomUUID(),dim,root,logs(),null,false);var saved=d.save();var rows=saved.getList("trees",Tag.TAG_COMPOUND);rows.add(rows.getCompound(0).copy());assertThrows(IllegalArgumentException.class,()->TreeEvidence.load(saved));
        var unknown=d.save();unknown.putInt("clientCount",999);assertThrows(IllegalArgumentException.class,()->TreeEvidence.load(unknown));assertThrows(IllegalArgumentException.class,()->new TreeEvidence.Log("oak","unverified_mod_wood"));
    }
    Map<Long,TreeEvidence.Log> cherry(){return Map.of(root.asLong(),new TreeEvidence.Log("cherry","cherry"),root.above().asLong(),new TreeEvidence.Log("cherry","cherry"),root.above(2).asLong(),new TreeEvidence.Log("cherry","cherry"));}
    TreeEvidence cultivated(){var d=new TreeEvidence();d.plant(player,ProfessionRank.JOURNEYMAN,dim,root,"minecraft:cherry_sapling",3);d.register(UUID.randomUUID(),dim,root,cherry(),d.plantAt(dim,root).orElseThrow(),true);return d;}
    @Test void onlyOriginalPlanterEarnsOneSpeciesCycleAcrossNativeHarvestsAndSaveReload(){
        var d=cultivated();var other=UUID.randomUUID();assertTrue(d.harvest(other,ProfessionRank.JOURNEYMAN,dim,root,"cherry"));assertEquals(0,d.evidence().count(other,ProfessionRank.JOURNEYMAN,"mature_cherry"));assertEquals(0,d.evidence().count(player,ProfessionRank.JOURNEYMAN,"mature_cherry"));
        assertTrue(d.harvest(player,ProfessionRank.JOURNEYMAN,dim,root.above(),"cherry"));assertEquals(1,d.evidence().count(player,ProfessionRank.JOURNEYMAN,"mature_cherry"));var restored=TreeEvidence.load(d.save());assertEquals(d.save(),restored.save());assertFalse(restored.cultivated(player,true,128).isEmpty());assertTrue(restored.harvest(player,ProfessionRank.JOURNEYMAN,dim,root.above(2),"cherry"));assertEquals(1,restored.evidence().count(player,ProfessionRank.JOURNEYMAN,"mature_cherry"));
    }
    @Test void legacyPlantedTreesAndUntieredSaplingsNeverInventNewCycleEntitlements(){
        var saved=cultivated().save();saved.getList("trees",Tag.TAG_COMPOUND).getCompound(0).remove("cultivation");var old=TreeEvidence.load(saved);old.harvest(player,ProfessionRank.JOURNEYMAN,dim,root,"cherry");assertEquals(0,old.evidence().count(player,ProfessionRank.JOURNEYMAN,"mature_cherry"));
        var d=new TreeEvidence();d.plant(player,ProfessionRank.JOURNEYMAN,dim,root,"minecraft:cherry_sapling");var restored=TreeEvidence.load(d.save());assertEquals(0,restored.plantAt(dim,root).orElseThrow().tier());restored.register(UUID.randomUUID(),dim,root,cherry(),restored.plantAt(dim,root).orElseThrow(),true);restored.harvest(player,ProfessionRank.JOURNEYMAN,dim,root,"cherry");assertEquals(0,restored.evidence().count(player,ProfessionRank.JOURNEYMAN,"mature_cherry"));
    }
    @Test void wrongBandCannotReceiveTheOriginalMaturityAndMalformedNewProofNeverResets(){
        var d=cultivated();d.harvest(player,ProfessionRank.MASTER,dim,root,"cherry");assertEquals(0,d.evidence().count(player,ProfessionRank.MASTER,"mature_cherry"));d.harvest(player,ProfessionRank.JOURNEYMAN,dim,root.above(),"cherry");assertEquals(1,d.evidence().count(player,ProfessionRank.JOURNEYMAN,"mature_cherry"));
        var bad=cultivated().save();bad.getList("trees",Tag.TAG_COMPOUND).getCompound(0).getCompound("cultivation").putString("species","mangrove");assertThrows(IllegalArgumentException.class,()->TreeEvidence.load(bad));
        var plant=new TreeEvidence();plant.plant(player,ProfessionRank.JOURNEYMAN,dim,root,"minecraft:cherry_sapling",3);var tag=plant.save();tag.getList("plants",Tag.TAG_COMPOUND).getCompound(0).putInt("tier",0);assertThrows(IllegalArgumentException.class,()->TreeEvidence.load(tag));
    }

 @Test void replacementMaturityKeepsExactClearingAndBandAcrossRestartWithoutRetrospectiveCredit(){
  var d=new TreeEvidence();d.register(UUID.randomUUID(),dim,root,logs(),null,false);assertTrue(d.harvest(player,ProfessionRank.MASTER,dim,root,"oak"));assertTrue(d.harvest(player,ProfessionRank.MASTER,dim,root.above(),"oak"));assertTrue(d.plant(player,ProfessionRank.MASTER,dim,root,"minecraft:oak_sapling",6));var plant=d.plantAt(dim,root).orElseThrow();assertNotNull(plant.clearing());d.register(UUID.randomUUID(),dim,root,logs(),plant,true);var saved=d.save();var restored=TreeEvidence.load(saved);assertTrue(restored.harvest(player,ProfessionRank.MASTER,dim,root,"oak"));assertTrue(restored.harvest(player,ProfessionRank.MASTER,dim,root.above(),"oak"));assertEquals(1,restored.evidence().count(player,ProfessionRank.MASTER,"renewal_cycles"));assertEquals(0,restored.evidence().count(player,ProfessionRank.JOURNEYMAN,"renewal_cycles"));assertFalse(restored.harvest(player,ProfessionRank.MASTER,dim,root,"oak"));var legacy=saved.copy();for(var row:legacy.getList("trees",net.minecraft.nbt.Tag.TAG_COMPOUND))((net.minecraft.nbt.CompoundTag)row).getCompound("cultivation").remove("renewal");var unknown=TreeEvidence.load(legacy);assertTrue(unknown.harvest(player,ProfessionRank.MASTER,dim,root,"oak"));assertEquals(0,unknown.evidence().count(player,ProfessionRank.MASTER,"renewal_cycles"));
 }
 @Test void sixSpeciesQuotasRejectMonocultureAndUnequalGoals(){assertEquals(8,TreeQuestService.mosaic(java.util.List.of(100,0,0,0,0,0,0,0),48));assertEquals(48,TreeQuestService.mosaic(java.util.List.of(8,8,8,8,8,8,0,0),48));assertEquals(47,TreeQuestService.mosaic(java.util.List.of(80,8,8,8,8,7,0,0),48));assertThrows(IllegalArgumentException.class,()->TreeQuestService.mosaic(java.util.List.of(8,8,8,8,8,8,0,0),47));}

}
