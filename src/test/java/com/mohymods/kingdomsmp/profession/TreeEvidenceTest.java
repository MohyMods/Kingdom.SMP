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
}
