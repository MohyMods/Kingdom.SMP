package com.mohymods.kingdomsmp.equipment;
import com.mohymods.kingdomsmp.profession.ProfessionRank;
import com.mohymods.kingdomsmp.economy.EconomyStore;
import java.math.BigInteger;
import java.util.*;
/** Stable purchase identities. Escalation counts successful purchases of this item by this player only. */
public enum FarmerGear {
 JOURNEYMAN_HOE("kingdom_smp:journeyman_farmer_hoe","Fieldwarden's Hoe",ProfessionRank.JOURNEYMAN,5000,3,2),
 FORTUNE_X_HOE("kingdom_smp:fortune_x_hoe","Fortune X Hoe",ProfessionRank.MASTER,25000,10,4);
 private final String id,title;private final ProfessionRank rank;private final long base;private final int fortune,radius;
 FarmerGear(String id,String title,ProfessionRank rank,long base,int fortune,int radius){this.id=id;this.title=title;this.rank=rank;this.base=base;this.fortune=fortune;this.radius=radius;}
 public String id(){return id;}public String title(){return title;}public ProfessionRank rank(){return rank;}public long base(){return base;}public int fortune(){return fortune;}public int radius(){return radius;}public boolean master(){return rank==ProfessionRank.MASTER;}
 public static FarmerGear byId(String id){return Arrays.stream(values()).filter(g->g.id.equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown Farmer equipment"));}
 /** Ceiling of the exact compounded rational price; no cumulative rounding or floating-point drift. */
 public long price(long base,int successful){if(base<1||base>EconomyStore.MAX_COINS||successful<0||successful>128)throw new IllegalArgumentException("Invalid equipment price/count");if(!master())return base;var denominator=BigInteger.TWO.pow(successful);var numerator=BigInteger.valueOf(base).multiply(BigInteger.valueOf(3).pow(successful));var result=numerator.add(denominator).subtract(BigInteger.ONE).divide(denominator);if(result.compareTo(BigInteger.valueOf(EconomyStore.MAX_COINS))>0)throw new IllegalStateException("Further purchase exceeds bounded coin balance");return result.longValueExact();}
}
