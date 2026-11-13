package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
/** Producer-computed facts, stored in the canonical journal, never read from client item tags. */
public record NativeBookProof(int families,boolean maximum) {
 public static final NativeBookProof EMPTY=new NativeBookProof(0,false);
 public NativeBookProof{if(families<0||families>15)throw new IllegalArgumentException("Invalid native family mask");}
 public CompoundTag save(){var tag=new CompoundTag();tag.putInt("families",families);tag.putBoolean("maximum",maximum);return tag;}
 public static NativeBookProof load(CompoundTag tag){if(!tag.getAllKeys().equals(Set.of("families","maximum"))||!tag.contains("families",Tag.TAG_INT)||!tag.contains("maximum",Tag.TAG_BYTE)||tag.getByte("maximum")<0||tag.getByte("maximum")>1)throw new IllegalArgumentException("Malformed native book attributes");return new NativeBookProof(tag.getInt("families"),tag.getBoolean("maximum"));}
 public static NativeBookProof combined(ItemStack before,ItemStack after){var old=EnchantmentHelper.getEnchantmentsForCrafting(before);var out=EnchantmentHelper.getEnchantmentsForCrafting(after);int mask=0;boolean max=false;for(var e:out.entrySet()){var key=e.getKey().unwrapKey().orElse(null);if(key==null||!key.location().getNamespace().equals("minecraft"))return EMPTY;mask|=family(key.location().getPath());if(e.getIntValue()>old.getLevel(e.getKey())&&e.getIntValue()==e.getKey().value().getMaxLevel())max=true;}return new NativeBookProof(mask,max);}
 public static int family(String id){return switch(id){case "protection","fire_protection","blast_protection","projectile_protection","feather_falling"->1;case "efficiency","fortune","silk_touch"->2;case "sharpness","smite","bane_of_arthropods","power","impaling","density","breach"->4;case "unbreaking","mending"->8;default->0;};}
 /** Bounded bipartite matching: one physical identity can satisfy only one family slot. */
 public static List<Integer> selectFamilies(List<Integer> masks,int required){if(required<4||required>36||required%4!=0||masks.size()>36||masks.stream().anyMatch(m->m<0||m>15))throw new IllegalArgumentException("Four bounded family quotas required");int[] assigned=new int[masks.size()];Arrays.fill(assigned,-1);for(int slot=0;slot<required;slot++)if(!augment(slot,masks,assigned,new boolean[masks.size()]))return List.of();List<Integer> result=new ArrayList<>();for(int i=0;i<assigned.length;i++)if(assigned[i]>=0)result.add(i);return List.copyOf(result);}
 private static boolean augment(int slot,List<Integer> masks,int[] assigned,boolean[] seen){int bit=1<<(slot%4);for(int i=0;i<masks.size();i++)if(!seen[i]&&(masks.get(i)&bit)!=0){seen[i]=true;if(assigned[i]<0||augment(assigned[i],masks,assigned,seen)){assigned[i]=slot;return true;}}return false;}
}
