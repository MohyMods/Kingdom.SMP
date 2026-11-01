package com.mohymods.kingdomsmp.profession;
import net.minecraft.world.item.*;
import java.util.*;
/** Compute the complete native exchange before preparing a receipt. Never drops a return container. */
public final class FarmerInventoryExchange {
 private FarmerInventoryExchange(){ }
 public static List<ItemStack> plan(List<ItemStack> before,Map<Item,Integer> inputs,Map<Item,Integer> returns){
  if(before.size()!=36)throw new IllegalArgumentException("Expected36main inventory slots");var next=new ArrayList<>(before.stream().map(ItemStack::copy).toList());
  for(var entry:inputs.entrySet()){int left=entry.getValue();if(left<1||left>2304)throw new IllegalArgumentException("Invalid delivery quantity");var plain=new ItemStack(entry.getKey());for(var stack:next)if(ItemStack.isSameItemSameComponents(stack,plain)){int n=Math.min(left,stack.getCount());stack.shrink(n);left-=n;}if(left!=0)throw new IllegalStateException("Bring the complete plain food delivery; nothing has been consumed.");}
  for(var entry:returns.entrySet()){int left=entry.getValue();if(left<1||left>2304)throw new IllegalArgumentException("Invalid returned container quantity");var item=new ItemStack(entry.getKey());for(var stack:next)if(!stack.isEmpty()&&ItemStack.isSameItemSameComponents(stack,item)){int n=Math.min(left,Math.max(0,stack.getMaxStackSize()-stack.getCount()));stack.grow(n);left-=n;}for(int slot=0;slot<36&&left>0;slot++)if(next.get(slot).isEmpty()){int n=Math.min(left,item.getMaxStackSize());next.set(slot,new ItemStack(entry.getKey(),n));left-=n;}if(left!=0)throw new IllegalStateException("Make inventory space for all emptied containers; nothing has been consumed.");}
  return next.stream().map(ItemStack::copy).toList();
 }
}
