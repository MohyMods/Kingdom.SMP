package com.mohymods.kingdomsmp.equipment;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import java.io.IOException;
import java.util.*;
/** One native player checkpoint commits every output and XP debit together, tied to the canonical receipt. */
public final class MasterWorkTransaction {
 private MasterWorkTransaction(){ }
 public static List<ItemStack> inventory(ServerPlayer p){var rows=new ArrayList<ItemStack>();for(int i=0;i<36;i++)rows.add(p.getInventory().getItem(i).copy());return rows;}
 public static boolean same(List<ItemStack>a,List<ItemStack>b){if(a.size()!=b.size())return false;for(int i=0;i<a.size();i++)if(!ItemStack.matches(a.get(i),b.get(i)))return false;return true;}
 private static ListTag encode(ServerPlayer p,List<ItemStack> items){var rows=new ListTag();for(var item:items)rows.add(item.saveOptional(p.registryAccess()));return rows;}
 public static void commit(ServerPlayer p,UUID operation,MasterGear gear,List<ItemStack> before,List<ItemStack> after,int xp,ListTag world)throws IOException{
  if(!world.isEmpty()&&p.serverLevel().noSave)throw new IllegalStateException("Enable world saving before Wand work");var s=CommerceService.store(p.server);CommerceService.verifyReceipt(p,s);if(!same(inventory(p),before)||p.experienceLevel<xp||before.size()!=36||after.size()!=36||!p.containerMenu.getCarried().isEmpty()||p.containerMenu!=p.inventoryMenu)throw new IllegalStateException("Inventory, XP or container changed; review again");
  var payload=new CompoundTag();payload.put("before",encode(p,before));payload.put("after",encode(p,after));payload.putInt("xp",xp);payload.putString("dimension",p.serverLevel().dimension().location().toString());payload.put("world",world.copy());var xpState=new CompoundTag();xpState.putInt("before",p.experienceLevel);xpState.putInt("after",p.experienceLevel-xp);xpState.putInt("total",p.totalExperience);xpState.putFloat("progress",p.experienceProgress);payload.put("xpState",xpState);var persistent=p.getPersistentData().copy();int levels=p.experienceLevel,total=p.totalExperience;float progress=p.experienceProgress;boolean saving=false;
  try{CommerceService.checkpoint(p);var pending=s.prepareMasterWork(operation,p.getUUID(),gear.id(),payload);MasterWorldWork.apply(p,world);for(int i=0;i<36;i++)p.getInventory().setItem(i,after.get(i).copy());p.giveExperienceLevels(-xp);if(p.experienceLevel!=levels-xp)throw new IOException("Native XP debit changed");CommerceService.putReceipt(p,new EconomyStore.Receipt(s.id(),pending.sequence()));saving=true;CommerceService.checkpoint(p);MasterWorldWork.beforeRecover(p.server,s,CommerceService.loadedReceipt(p));if(!s.recoverItem(CommerceService.receipt(AtomicNbt.read(CommerceService.playerFile(p.server,p.getUUID())))))throw new IOException("Master work receipt not committed");MasterWorldWork.resolved(p.server,s);p.getInventory().setChanged();p.inventoryMenu.sendAllDataToRemote();}
  catch(IOException|RuntimeException failure){if(!saving){for(int i=0;i<36;i++)p.getInventory().setItem(i,before.get(i).copy());p.experienceLevel=levels;p.totalExperience=total;p.experienceProgress=progress;for(var key:new HashSet<>(p.getPersistentData().getAllKeys()))p.getPersistentData().remove(key);p.getPersistentData().merge(persistent);try{MasterWorldWork.beforeRecover(p.server,s,CommerceService.loadedReceipt(p));s.recoverItem(CommerceService.loadedReceipt(p));MasterWorldWork.resolved(p.server,s);}catch(IOException|RuntimeException uncertain){s.quarantine();p.connection.disconnect(Component.literal("Master operation needs matching world/inventory recovery"));throw failure;}}
   if(saving){s.quarantine();p.connection.disconnect(Component.literal("Master operation interrupted; restore matching canonical/world/player checkpoint"));}throw failure;
  }
 }
}
