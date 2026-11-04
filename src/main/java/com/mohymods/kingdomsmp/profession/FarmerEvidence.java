package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
/** Compatibility facade: existing Farmer schema1 files and producer API remain unchanged. */
public final class FarmerEvidence {
 public static final int MAX_PLAYERS=ProfessionEvidence.MAX_PLAYERS,MAX_UNIQUE=ProfessionEvidence.MAX_UNIQUE,MAX_COUNTER=ProfessionEvidence.MAX_COUNTER;
 private static final Set<String> METRICS=Set.of("soil","wheat","carrot","potato","beetroot","melon","pumpkin","cow","sheep","chicken","pig","bread","cake","replant");
 private final ProfessionEvidence evidence;
 public FarmerEvidence(){this(new ProfessionEvidence(Profession.FARMER,METRICS));}
 private FarmerEvidence(ProfessionEvidence evidence){this.evidence=evidence;}
 public int count(UUID player,ProfessionRank band,String metric){return evidence.count(player,band,metric);}
 public boolean record(UUID player,ProfessionRank band,String metric,int amount,String identity){return evidence.record(player,band,metric,amount,identity);}
 public CompoundTag save(){return evidence.saveLegacy();}
 public static FarmerEvidence load(CompoundTag data){return new FarmerEvidence(ProfessionEvidence.loadLegacy(data,Profession.FARMER,METRICS));}
}
