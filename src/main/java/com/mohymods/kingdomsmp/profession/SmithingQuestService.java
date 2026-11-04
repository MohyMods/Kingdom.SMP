package com.mohymods.kingdomsmp.profession;

import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.mixin.*;
import com.mohymods.kingdomsmp.placement.PlacementProtection;
import java.io.IOException;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;

/** Authenticated native netherite transforms. Registration retains the upgraded tool and awards XP once. */
public final class SmithingQuestService {
 public static final String QUEST="kingdom_smp:blacksmith/m03";
 private static final Set<String> TYPES=Set.of("pickaxe","axe","shovel","hoe","sword","helmet","chestplate","leggings","boots");
 private static final Map<SmithingMenu,Candidate> CANDIDATES=new WeakHashMap<>();
 public record Candidate(UUID id,ServerPlayer player,SmithingMenu menu,BlockPos site,List<ItemStack> inputs,ItemStack output){}
 private SmithingQuestService(){}
 public static boolean upgraded(String id){return id!=null&&id.startsWith("minecraft:netherite_")&&TYPES.contains(id.substring("minecraft:netherite_".length()));}
 public static boolean pair(String base,String result){return base!=null&&base.startsWith("minecraft:diamond_")&&upgraded(result)&&base.substring("minecraft:diamond_".length()).equals(result.substring("minecraft:netherite_".length()));}
 public static boolean eligible(ServerPlayer p){if(!TreeQuestService.real(p))return false;var store=CommerceService.initializedStore(p.server).orElse(null);if(store==null||store.quarantined())return false;var r=store.professions(p.getUUID());return r.isActive(Profession.BLACKSMITH)&&r.progressBand(Profession.BLACKSMITH)==ProfessionRank.MASTER&&!r.track(Profession.BLACKSMITH).completedQuestIds().contains(QUEST)&&RepairService.catalog().mayAttempt(Profession.BLACKSMITH,QUEST,r.track(Profession.BLACKSMITH).completedQuestIds())&&com.mohymods.kingdomsmp.contribution.ContributionService.kingdom(p.server).attainedTier()>=5&&ProfessionService.available(p,Profession.BLACKSMITH);}
 public static void preview(Player player,SmithingMenu menu){CANDIDATES.remove(menu);if(!(player instanceof ServerPlayer p))return;try{
  if(p.containerMenu!=menu||!eligible(p))return;var template=menu.getSlot(0).getItem();var base=menu.getSlot(1).getItem();var ingot=menu.getSlot(2).getItem();var output=menu.getSlot(3).getItem();if(!ItemStack.isSameItemSameComponents(template,new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE))||!ItemStack.isSameItemSameComponents(ingot,new ItemStack(Items.NETHERITE_INGOT))||base.getCount()!=1||output.getCount()!=1||!pair(BuiltInRegistries.ITEM.getKey(base.getItem()).toString(),BuiltInRegistries.ITEM.getKey(output.getItem()).toString()))return;
  var custom=base.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getAllKeys();if(!custom.isEmpty()&&!custom.equals(Set.of(RepairService.KEY)))return;
  var input=new SmithingRecipeInput(template,base,ingot);var recipe=p.server.getRecipeManager().getRecipeFor(RecipeType.SMITHING,input,p.serverLevel()).orElse(null);if(recipe==null||!recipe.id().getNamespace().equals("minecraft")||!(recipe.value() instanceof SmithingTransformRecipe)||!ItemStack.matches(recipe.value().assemble(input,p.registryAccess()),output))return;
  record Site(net.minecraft.world.level.Level level,BlockPos pos){}var site=((NativeCombinerAccess)menu).kingdom$access().evaluate((level,pos)->new Site(level,pos.immutable())).orElse(null);if(site==null||site.level()!=p.serverLevel()||!p.serverLevel().hasChunkAt(site.pos())||!p.serverLevel().getBlockState(site.pos()).is(Blocks.SMITHING_TABLE)||p.distanceToSqr(site.pos().getX()+.5,site.pos().getY()+.5,site.pos().getZ()+.5)>64||PlacementProtection.check(p.serverLevel(),p,site.pos(),site.pos())!=null)return;
  var id=UUID.randomUUID();var all=output.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();var marker=new CompoundTag();marker.putUUID("store",CommerceService.store(p.server).id());marker.putUUID("id",id);all.put(EnchantedToolService.KEY,marker);output.set(DataComponents.CUSTOM_DATA,CustomData.of(all));CANDIDATES.put(menu,new Candidate(id,p,menu,site.pos(),List.of(template.copy(),base.copy(),ingot.copy()),output.copy()));
 }catch(RuntimeException failure){TreeLifecycle.failure(p.server,failure);}}
 public static Candidate capture(Player player,SmithingMenu menu,ItemStack output){var c=CANDIDATES.remove(menu);if(!(player instanceof ServerPlayer p)||c==null||c.player()!=p)return null;try{if(!eligible(p)||p.containerMenu!=menu)return null;for(int i=0;i<3;i++)if(!ItemStack.matches(menu.getSlot(i).getItem(),c.inputs().get(i)))return null;var raw=(NativeItemIdentityAccessor)(Object)output;return raw.kingdom$nativeItem()==c.output().getItem()&&raw.kingdom$nativeComponents().equals(((NativeItemIdentityAccessor)(Object)c.output()).kingdom$nativeComponents())?c:null;}catch(RuntimeException failure){TreeLifecycle.failure(p.server,failure);return null;}}
 public static void commit(Candidate c){if(c==null)return;var p=c.player();try{if(!eligible(p)||p.containerMenu!=c.menu()||PlacementProtection.check(p.serverLevel(),p,c.site(),c.site())!=null)return;for(int i=0;i<3;i++){var expected=c.inputs().get(i).copy();expected.shrink(1);if(!ItemStack.matches(c.menu().getSlot(i).getItem(),expected))return;}CommerceService.store(p.server).issueNativeOutput(c.id(),p.getUUID(),QUEST,(CompoundTag)c.output().save(p.registryAccess()));}catch(IOException|RuntimeException failure){TreeLifecycle.failure(p.server,new IllegalStateException("Native smithing output evidence unavailable; native transfer retained",failure));}}
 public static String status(ServerPlayer p){return "Apply a real netherite upgrade template and alloy to ordinary diamond equipment at a permitted smithing table. Register "+RepairQuestRules.upgrades(p.server)+" authenticated upgraded item at your Blacksmith; keep the item, earn300XP once.";}
}
