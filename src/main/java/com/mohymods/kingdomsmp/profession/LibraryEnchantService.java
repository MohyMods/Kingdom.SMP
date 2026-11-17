package com.mohymods.kingdomsmp.profession;
import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.mixin.NativeEnchantingAccess;
import com.mohymods.kingdomsmp.placement.PlacementProtection;
import java.io.IOException;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.Blocks;
/** Actual native enchanting payments, followed by purpose-bound output authentication. No preview evidence. */
public final class LibraryEnchantService {
 public static final String METRIC="enchant_sites";
 public static String siteIdentity(String dimension,BlockPos pos){return ProfessionRank.APPRENTICE.id()+"/"+dimension+":"+pos.asLong();}
 private static boolean witnessEligible(ServerPlayer p){if(!TreeQuestService.real(p))return false;var store=CommerceService.initializedStore(p.server).orElse(null);if(store==null||store.quarantined())return false;var r=store.professions(p.getUUID());return r.isActive(Profession.LIBRARIAN)&&r.progressBand(Profession.LIBRARIAN)==ProfessionRank.APPRENTICE&&r.track(Profession.LIBRARIAN).completedQuestIds().contains("kingdom_smp:librarian/a02")&&com.mohymods.kingdomsmp.contribution.ContributionService.kingdom(p.server).attainedTier()>=4&&ProfessionService.available(p,Profession.LIBRARIAN);}
 public record Proof(ServerPlayer player,EnchantmentMenu menu,BlockPos site,ItemStack before,int lapis,int levels,int paid,boolean authenticate){public Proof{before=before.copy();}@Override public ItemStack before(){return before.copy();}}
 private LibraryEnchantService(){}
 private static boolean clean(ItemStack s){var keys=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getAllKeys();return keys.isEmpty()||keys.equals(Set.of(RepairService.KEY));}
 public static Proof capture(Player player,EnchantmentMenu menu,int button){if(!(player instanceof ServerPlayer p))return null;try{
  if(button<0||button>2||p.containerMenu!=menu||!witnessEligible(p)||menu.costs[button]<1||p.experienceLevel<menu.costs[button]||p.experienceLevel<button+1)return null;
  var input=menu.getSlot(0).getItem();var lapis=menu.getSlot(1).getItem();if(input.getCount()!=1||!clean(input)||!(input.is(Items.BOOK)||NativeOutputQuests.ordinaryLibrary(BuiltInRegistries.ITEM.getKey(input.getItem()).toString()))||!EnchantmentHelper.getEnchantmentsForCrafting(input).isEmpty()||!ItemStack.isSameItemSameComponents(lapis,new ItemStack(Items.LAPIS_LAZULI))||lapis.getCount()<button+1)return null;
  record Site(net.minecraft.world.level.Level level,BlockPos pos){}var site=((NativeEnchantingAccess)menu).kingdom$access().evaluate((level,pos)->new Site(level,pos.immutable())).orElse(null);if(site==null||site.level()!=p.serverLevel()||!p.serverLevel().hasChunkAt(site.pos())||!p.serverLevel().getBlockState(site.pos()).is(Blocks.ENCHANTING_TABLE)||p.distanceToSqr(site.pos().getX()+.5,site.pos().getY()+.5,site.pos().getZ()+.5)>64||PlacementProtection.check(p.serverLevel(),p,site.pos(),site.pos())!=null)return null;
  return new Proof(p,menu,site.pos(),input,lapis.getCount(),p.experienceLevel,button+1,NativeOutputQuests.eligible(p,NativeOutputQuests.LIBRARY_ENCHANT,true));
 }catch(RuntimeException failure){TreeLifecycle.failure(p.server,failure);return null;}}
 public static void commit(Proof proof,boolean nativeResult){if(proof==null||!nativeResult)return;var p=proof.player();try{
  if(p.containerMenu!=proof.menu()||!witnessEligible(p)||p.experienceLevel!=proof.levels()-proof.paid()||proof.menu().getSlot(1).getItem().getCount()!=proof.lapis()-proof.paid()||PlacementProtection.check(p.serverLevel(),p,proof.site(),proof.site())!=null)return;
  var output=proof.menu().getSlot(0).getItem();var before=proof.before();if(output.getCount()!=1||!clean(output)||!(before.is(Items.BOOK)?output.is(Items.ENCHANTED_BOOK):output.is(before.getItem()))||!NativeOutputQuests.ordinaryLibrary(BuiltInRegistries.ITEM.getKey(output.getItem()).toString()))return;
  var ench=EnchantmentHelper.getEnchantmentsForCrafting(output);if(ench.isEmpty()||ench.size()>16)return;for(var e:ench.entrySet()){var key=e.getKey().unwrapKey().orElse(null);if(key==null||!key.location().getNamespace().equals("minecraft")||e.getIntValue()<1||e.getIntValue()>e.getKey().value().getMaxLevel()||!output.is(Items.ENCHANTED_BOOK)&&!output.supportsEnchantment(e.getKey()))return;for(var other:ench.keySet())if(!e.getKey().equals(other)&&!Enchantment.areCompatible(e.getKey(),other))return;}
  var data=ProfessionEvidenceSavedData.get(p.server);if(data.evidence(Profession.LIBRARIAN).record(p.getUUID(),ProfessionRank.APPRENTICE,METRIC,1,siteIdentity(p.serverLevel().dimension().location().toString(),proof.site())))data.setDirty();
  if(!proof.authenticate()||!NativeOutputQuests.eligible(p,NativeOutputQuests.LIBRARY_ENCHANT,true))return;
  var id=UUID.randomUUID();var all=output.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();var marker=new CompoundTag();marker.putUUID("store",CommerceService.store(p.server).id());marker.putUUID("id",id);all.put(EnchantedToolService.KEY,marker);output.set(DataComponents.CUSTOM_DATA,CustomData.of(all));CommerceService.store(p.server).issueNativeOutput(id,p.getUUID(),NativeOutputQuests.LIBRARY_ENCHANT,(CompoundTag)output.save(p.registryAccess()));proof.menu().broadcastChanges();
 }catch(IOException|RuntimeException failure){TreeLifecycle.failure(p.server,new IllegalStateException("Native enchanting evidence unavailable; native payments and output retained",failure));}}
 public static String status(ServerPlayer p){return "Enchant and deliver "+LibraryQuestRules.get(p.server).enchantedItems()+" distinct authenticated ordinary items or books. Real native table XP/lapis payments required. Preview, altered, recycled and unvalidated mod outputs do not count.";}
}
