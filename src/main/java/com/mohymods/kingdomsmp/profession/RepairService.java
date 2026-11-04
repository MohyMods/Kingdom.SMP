package com.mohymods.kingdomsmp.profession;
import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.mixin.*;
import com.mohymods.kingdomsmp.placement.PlacementProtection;
import java.io.IOException;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
/** Exception-isolated evidence around native transactions. No inventory, XP or anvil operation is replaced. */
public final class RepairService {
 public static final String QUEST="kingdom_smp:blacksmith/a06",KEY="kingdom_smp_native_wear";
 private static final ProfessionQuestCatalog CATALOG=ProfessionQuestCatalog.approved(List.of(ApprovedProfessionContent.get(QUEST).slot(),ApprovedProfessionContent.get(CraftDeliveryQuests.TOOLS).slot(),ApprovedProfessionContent.get(CraftDeliveryQuests.IRON).slot()));
 public record Proof(ServerPlayer player,AnvilMenu menu,BlockPos site,UUID item,String kind,int before,int after,int materials,int stock,int levels,int cost,ProfessionRank band){}
 private RepairService(){}public static boolean contains(String id){return QUEST.equals(id);}public static ProfessionQuestCatalog catalog(){return CATALOG;}
 private static UUID identity(ItemStack stack,RepairEvidence evidence){var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound(KEY);return tag.hasUUID("store")&&tag.hasUUID("id")&&tag.getUUID("store").equals(evidence.identity())?tag.getUUID("id"):null;}
 public static void worn(ServerPlayer p,ItemStack stack,int before){try{
  if(!TreeQuestService.real(p)||stack.isEmpty()||stack.getCount()!=1||stack.getDamageValue()<=before)return;String kind=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();if(!RepairEvidence.eligible(kind))return;
  var data=RepairSavedData.get(p.server);var known=identity(stack,data.evidence());var previous=data.evidence().get(known);if(previous!=null&&previous.credited()!=null)return;var id=data.evidence().worn(known,p.getUUID(),kind,stack.getDamageValue());if(id==null)return;var all=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();var marker=new CompoundTag();marker.putUUID("store",data.evidence().identity());marker.putUUID("id",id);all.put(KEY,marker);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(all));data.setDirty();
 }catch(RuntimeException failure){TreeLifecycle.failure(p.server,failure);}}
 private static boolean eligible(ServerPlayer p){var store=CommerceService.initializedStore(p.server).orElse(null);if(store==null||store.quarantined()||!TreeQuestService.real(p))return false;var roster=store.professions(p.getUUID());return roster.isActive(Profession.BLACKSMITH)&&roster.progressBand(Profession.BLACKSMITH)==ProfessionRank.APPRENTICE&&CATALOG.mayAttempt(Profession.BLACKSMITH,QUEST,roster.track(Profession.BLACKSMITH).completedQuestIds())&&com.mohymods.kingdomsmp.contribution.ContributionService.kingdom(p.server).attainedTier()>=3;}
 public static Proof capture(Player player,AnvilMenu menu,ItemStack output,int materials){
  if(!(player instanceof ServerPlayer p))return null;try{
   if(p.containerMenu!=menu||!eligible(p)||!ProfessionService.available(p,Profession.BLACKSMITH)||materials<1||menu.getCost()<1||menu.getCost()>p.experienceLevel)return null;
   var access=((NativeCombinerAccess)menu).kingdom$access();record Site(net.minecraft.world.level.Level level,BlockPos at){}var site=access.evaluate((l,pos)->new Site(l,pos.immutable())).orElse(null);if(site==null||site.level()!=p.serverLevel()||!p.serverLevel().hasChunkAt(site.at())||!p.serverLevel().getBlockState(site.at()).is(BlockTags.ANVIL)||p.distanceToSqr(site.at().getX()+.5,site.at().getY()+.5,site.at().getZ()+.5)>64||PlacementProtection.check(p.serverLevel(),p,site.at(),site.at())!=null)return null;
   var left=menu.getSlot(0).getItem();var right=menu.getSlot(1).getItem();String kind=BuiltInRegistries.ITEM.getKey(left.getItem()).toString();if(!RepairEvidence.eligible(kind)||left.getCount()!=1||left.getDamageValue()<1||!ItemStack.isSameItemSameComponents(right,new ItemStack(Items.IRON_INGOT))||right.getCount()<materials)return null;
   var data=RepairSavedData.get(p.server);var id=identity(left,data.evidence());var record=data.evidence().get(id);if(record==null||record.credited()!=null||record.damage()!=left.getDamageValue()||!record.item().equals(kind))return null;
   var nativeStack=(NativeItemIdentityAccessor)(Object)output;var components=nativeStack.kingdom$nativeComponents();var marker=components.get(DataComponents.CUSTOM_DATA);var tag=marker==null?new CompoundTag():marker.copyTag().getCompound(KEY);Integer damage=components.get(DataComponents.DAMAGE);
   if(nativeStack.kingdom$nativeItem()!=left.getItem()||damage==null||damage<0||damage>=left.getDamageValue()||!tag.hasUUID("store")||!tag.hasUUID("id")||!tag.getUUID("store").equals(data.evidence().identity())||!tag.getUUID("id").equals(id)||!Objects.equals(components.get(DataComponents.ENCHANTMENTS),left.get(DataComponents.ENCHANTMENTS)))return null;
   return new Proof(p,menu,site.at(),id,kind,left.getDamageValue(),damage,materials,right.getCount(),p.experienceLevel,menu.getCost(),ProfessionRank.APPRENTICE);
  }catch(RuntimeException failure){TreeLifecycle.failure(p.server,failure);return null;}
 }
 public static void commit(Proof proof){if(proof==null)return;var p=proof.player();try{
  if(!eligible(p)||p.containerMenu!=proof.menu()||!proof.menu().getSlot(0).getItem().isEmpty()||proof.menu().getSlot(1).getItem().getCount()!=proof.stock()-proof.materials()||p.experienceLevel!=proof.levels()-proof.cost()||PlacementProtection.check(p.serverLevel(),p,proof.site(),proof.site())!=null)return;
  var data=RepairSavedData.get(p.server);if(data.evidence().repair(proof.item(),p.getUUID(),proof.band(),proof.kind(),proof.before(),proof.after(),proof.materials(),proof.cost()))data.setDirty();
 }catch(RuntimeException failure){TreeLifecycle.failure(p.server,failure);}}
 public static int count(ServerPlayer p){return RepairSavedData.get(p.server).evidence().count(p.getUUID(),ProfessionRank.APPRENTICE);}
 public static boolean ready(ServerPlayer p){try{return eligible(p)&&!CommerceService.store(p.server).professions(p.getUUID()).track(Profession.BLACKSMITH).completedQuestIds().contains(QUEST)&&ProfessionService.available(p,Profession.BLACKSMITH)&&count(p)>=RepairQuestRules.goal(p.server);}catch(RuntimeException unavailable){return false;}}
 public static String status(ServerPlayer p){try{return count(p)+" / "+RepairQuestRules.goal(p.server)+" distinct native-worn iron items repaired with real iron and XP. Use tracked tools normally first; copied or repeatedly repaired identities grant no extra credit. Review at your Blacksmith.";}catch(RuntimeException unavailable){return "Native repair evidence unavailable; an operator must restore the matching checkpoint. Existing progress is preserved.";}}
 public static void complete(ServerPlayer p,UUID operation)throws IOException{ContractService.requireCounter(p,Profession.BLACKSMITH);if(!ready(p))throw new IllegalStateException(status(p));CommerceService.store(p.server).completeRepairQuest(operation,p.getUUID(),RepairQuestRules.goal(p.server),count(p));}
}
