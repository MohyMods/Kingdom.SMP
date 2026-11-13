package com.mohymods.kingdomsmp.profession;
import java.util.*;
import java.lang.ref.WeakReference;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
/** Session-only actual native inventory debits. Unabsorbed old world drops must be picked up/re-dropped; accepted machine batches persist separately. */
public final class NativeDropReceipts {
 public record Receipt(UUID player,UUID entity,Item item,int amount,WeakReference<ItemEntity> instance){}
 public static final class Drop {final ServerPlayer player;final int selected,before,amount;final Item item;ItemEntity entity;Drop(ServerPlayer p,boolean all){player=p;item=p.getMainHandItem().getItem();selected=p.getInventory().selected;before=p.getMainHandItem().getCount();amount=all?before:1;}}
 private static final Map<ItemEntity,Receipt> RECEIPTS=Collections.synchronizedMap(new WeakHashMap<>());
 private static final ThreadLocal<Drop> CURRENT=new ThreadLocal<>();
 private NativeDropReceipts(){}
 public static void register(IEventBus bus){bus.addListener(EventPriority.LOWEST,false,ItemTossEvent.class,NativeDropReceipts::tossed);}
 private static boolean eligible(ServerPlayer p,Item item){return CreateCraftQuests.supported()&&(item==Items.WHEAT?MillingService.canFund(p):item==Items.IRON_INGOT||item==Items.COPPER_INGOT||item==Items.GOLD_INGOT?CreatePressService.canFund(p):false);}
 public static Drop begin(ServerPlayer p,boolean all){try{var stack=p.getMainHandItem();var item=stack.getItem();if(item!=Items.WHEAT&&item!=Items.IRON_INGOT&&item!=Items.COPPER_INGOT&&item!=Items.GOLD_INGOT)return null;if(!p.server.isSameThread()||!ItemStack.isSameItemSameComponents(stack,new ItemStack(item))||!eligible(p,item))return null;return new Drop(p,all);}catch(RuntimeException failure){TreeLifecycle.failure(p.server,failure);return null;}}
 public static Drop enter(Drop drop){var old=CURRENT.get();if(drop==null)CURRENT.remove();else CURRENT.set(drop);return old;}
 private static void tossed(ItemTossEvent event){var drop=CURRENT.get();if(drop==null||event.getPlayer()!=drop.player)return;var entity=event.getEntity();var stack=entity.getItem();if(stack.is(drop.item)&&stack.getCount()==drop.amount&&ItemStack.isSameItemSameComponents(stack,new ItemStack(drop.item)))drop.entity=entity;}
 public static void finish(Drop drop,boolean success){if(drop==null||!success)return;var p=drop.player;try{var entity=drop.entity;var after=p.getMainHandItem();int remaining=after.isEmpty()?0:after.is(drop.item)&&ItemStack.isSameItemSameComponents(after,new ItemStack(drop.item))?after.getCount():-1;if(entity==null||p.getInventory().selected!=drop.selected||drop.before-remaining!=drop.amount||drop.amount<1||drop.amount>64||!entity.isAlive()||entity.getOwner()!=p||p.serverLevel().getEntity(entity.getUUID())!=entity||!eligible(p,drop.item)||!entity.getItem().is(drop.item)||entity.getItem().getCount()!=drop.amount||!ItemStack.isSameItemSameComponents(entity.getItem(),new ItemStack(drop.item))||RECEIPTS.size()>=8192)return;RECEIPTS.put(entity,new Receipt(p.getUUID(),entity.getUUID(),drop.item,drop.amount,new WeakReference<>(entity)));}catch(RuntimeException failure){TreeLifecycle.failure(p.server,failure);}}
 private static Receipt find(ItemEntity entity){if(!(entity.level() instanceof ServerLevel))return null;var r=RECEIPTS.get(entity);return r!=null&&r.instance().get()==entity?r:null;}
 public static boolean funded(ItemEntity entity){var r=find(entity);return r!=null&&r.entity().equals(entity.getUUID())&&entity.getItem().is(r.item())&&entity.getItem().getCount()==r.amount();}
 public static Receipt take(ItemEntity entity){var r=find(entity);if(r!=null)RECEIPTS.remove(entity);return r!=null&&r.entity().equals(entity.getUUID())&&entity.getItem().is(r.item())&&entity.getItem().getCount()==r.amount()?r:null;}
 public static void changed(ItemEntity entity){var r=find(entity);if(r!=null&&(!entity.isAlive()||!r.entity().equals(entity.getUUID())||!entity.getItem().is(r.item())||entity.getItem().getCount()!=r.amount()||!ItemStack.isSameItemSameComponents(entity.getItem(),new ItemStack(r.item()))))RECEIPTS.remove(entity);}
 public static void merged(ItemEntity a,ItemEntity b,int beforeA,int beforeB){if(!(a.level() instanceof ServerLevel))return;if(a.getItem().getCount()!=beforeA||b.getItem().getCount()!=beforeB){if(find(a)!=null)RECEIPTS.remove(a);if(find(b)!=null)RECEIPTS.remove(b);}}
}
