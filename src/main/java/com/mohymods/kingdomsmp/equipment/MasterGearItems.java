package com.mohymods.kingdomsmp.equipment;
import com.mohymods.kingdomsmp.KingdomSmp;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import java.util.*;
public final class MasterGearItems {
 private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(KingdomSmp.MOD_ID);
 private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(KingdomSmp.MOD_ID);
 private static final Map<MasterGear,DeferredItem<Item>> REGISTERED=new EnumMap<>(MasterGear.class);
 public static final DeferredBlock<Block> EXTRACTION=station(MasterGear.EXTRACTION,Blocks.ANVIL),CODEX=station(MasterGear.CODEX,Blocks.ENCHANTING_TABLE);
 static {
  REGISTERED.put(MasterGear.EXTRACTION,ITEMS.register(MasterGear.EXTRACTION.path(),()->new BlockItem(EXTRACTION.get(),properties(MasterGear.EXTRACTION))));
  REGISTERED.put(MasterGear.CODEX,ITEMS.register(MasterGear.CODEX.path(),()->new BlockItem(CODEX.get(),properties(MasterGear.CODEX))));
  REGISTERED.put(MasterGear.AXE,ITEMS.register(MasterGear.AXE.path(),()->new AxeItem(Tiers.NETHERITE,properties(MasterGear.AXE).attributes(AxeItem.createAttributes(Tiers.NETHERITE,5,-3)))));
  REGISTERED.put(MasterGear.PICKAXE,ITEMS.register(MasterGear.PICKAXE.path(),()->new PickaxeItem(Tiers.NETHERITE,properties(MasterGear.PICKAXE).attributes(PickaxeItem.createAttributes(Tiers.NETHERITE,1,-2.8f))){
   @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){if(player instanceof ServerPlayer p&&hand==InteractionHand.MAIN_HAND)MasterToolService.survey(p);return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);}
  }));
  REGISTERED.put(MasterGear.WAND,ITEMS.register(MasterGear.WAND.path(),()->new Item(properties(MasterGear.WAND)){
   @Override public InteractionResult useOn(UseOnContext context){if(context.getPlayer() instanceof ServerPlayer p&&context.getHand()==InteractionHand.MAIN_HAND)MasterWandService.select(p,context);return InteractionResult.sidedSuccess(context.getLevel().isClientSide);}
   @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){if(player instanceof ServerPlayer p&&hand==InteractionHand.MAIN_HAND)MasterWandService.cycle(p);return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);}
  }));
 }
 private MasterGearItems(){ }
 private static Item.Properties properties(MasterGear gear){return new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).component(DataComponents.LORE,new net.minecraft.world.item.component.ItemLore(lore(gear.detail())));}
 private static List<Component> lore(String text){var lines=new ArrayList<Component>();var line=new StringBuilder();for(var word:text.split(" ")){if(line.length()>0&&line.length()+1+word.length()>48){lines.add(Component.literal(line.toString()));line.setLength(0);}if(line.length()>0)line.append(" ");line.append(word);}if(line.length()>0)lines.add(Component.literal(line.toString()));return List.copyOf(lines);}
 private static DeferredBlock<Block> station(MasterGear gear,Block template){return BLOCKS.register(gear.path(),()->new Block(BlockBehaviour.Properties.ofFullCopy(template)){
  @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){if(player instanceof ServerPlayer p)MasterStationService.open(p,pos,gear);return InteractionResult.sidedSuccess(level.isClientSide);}
 });}
 public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);}
 public static Item item(MasterGear gear){return REGISTERED.get(gear).get();}
 public static boolean known(ItemStack item){return Arrays.stream(MasterGear.values()).anyMatch(g->item.is(item(g)));}
 public static ItemStack stack(ServerPlayer player,MasterGear gear){var item=new ItemStack(item(gear));if(gear==MasterGear.AXE||gear==MasterGear.PICKAXE){item.set(DataComponents.UNBREAKABLE,new Unbreakable(true));var registry=player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);item.enchant(registry.getOrThrow(Enchantments.EFFICIENCY),6);if(gear==MasterGear.PICKAXE)item.enchant(registry.getOrThrow(Enchantments.FORTUNE),6);}return item;}
 public static boolean pristine(ItemStack item){if(!known(item))return false;return item.getCount()==1&&!item.isDamaged();}
}
