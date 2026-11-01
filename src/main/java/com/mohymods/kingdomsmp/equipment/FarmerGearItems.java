package com.mohymods.kingdomsmp.equipment;
import com.mohymods.kingdomsmp.KingdomSmp;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
/** No recipes and no client-supplied authority. Legitimate purchased items can be traded and used by any player. */
public final class FarmerGearItems {
 private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(KingdomSmp.MOD_ID);
 public static final DeferredItem<Item> FIELD=register(FarmerGear.JOURNEYMAN_HOE,Tiers.DIAMOND);
 public static final DeferredItem<Item> MASTER=register(FarmerGear.FORTUNE_X_HOE,Tiers.NETHERITE);
 private FarmerGearItems(){ }
 private static DeferredItem<Item> register(FarmerGear gear,Tier tier){return ITEMS.register(gear.id().substring(gear.id().indexOf(':')+1),()->new HoeItem(tier,new Item.Properties().stacksTo(1).rarity(gear.master()?Rarity.EPIC:Rarity.RARE).attributes(HoeItem.createAttributes(tier,-3,0))){
  @Override public InteractionResult useOn(UseOnContext context){if(context.getPlayer() instanceof ServerPlayer player&&FarmerHarvestService.start(player,context,gear))return InteractionResult.CONSUME;return super.useOn(context);}
 });}
 public static void register(IEventBus bus){ITEMS.register(bus);}
 public static Item item(FarmerGear gear){return gear==FarmerGear.FORTUNE_X_HOE?MASTER.get():FIELD.get();}
 public static ItemStack stack(ServerPlayer player,FarmerGear gear){var stack=new ItemStack(item(gear));stack.enchant(player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),gear.fortune());return stack;}
 public static boolean pristine(ItemStack stack){
  if(!known(stack))return false;var gear=stack.is(MASTER.get())?FarmerGear.FORTUNE_X_HOE:FarmerGear.JOURNEYMAN_HOE;var enchants=stack.get(net.minecraft.core.component.DataComponents.ENCHANTMENTS);if(enchants==null||enchants.entrySet().size()!=1||enchants.entrySet().stream().noneMatch(e->e.getKey().is(Enchantments.FORTUNE)&&e.getIntValue()==gear.fortune()))return false;var plain=stack.copyWithCount(1);plain.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS,net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);return ItemStack.isSameItemSameComponents(plain,new ItemStack(stack.getItem()));
 }
 public static boolean known(ItemStack stack){return stack.is(FIELD.get())||stack.is(MASTER.get());}
 public static boolean master(ItemStack stack){return stack.is(MASTER.get());}
}
