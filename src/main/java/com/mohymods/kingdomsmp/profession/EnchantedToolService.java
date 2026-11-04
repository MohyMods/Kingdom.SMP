package com.mohymods.kingdomsmp.profession;

import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.mixin.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import com.mohymods.kingdomsmp.placement.PlacementProtection;
import java.io.IOException;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.*;

/** Preview markers carry no authority. Only successful native extraction issues an exact canonical output. */
public final class EnchantedToolService {
 public static final String QUEST="kingdom_smp:blacksmith/j05",KEY="kingdom_smp_native_anvil";
 private static final Map<AnvilMenu,Candidate> CANDIDATES=new WeakHashMap<>();
 private static final Map<ServerPlayer,Quote> QUOTES=new WeakHashMap<>();
 public record Candidate(UUID id,String quest,ServerPlayer player,AnvilMenu menu,BlockPos site,ItemStack output,int levels,int cost){}
 public record Quote(UUID nonce,long revision,String quest,boolean consume,List<ItemStack> before,List<ItemStack> after,Map<UUID,CompoundTag> outputs,int required,long issued,String text){
  public Quote{before=before.stream().map(ItemStack::copy).toList();after=after.stream().map(ItemStack::copy).toList();var copy=new LinkedHashMap<UUID,CompoundTag>();outputs.forEach((id,item)->copy.put(id,item.copy()));outputs=Collections.unmodifiableMap(copy);}
  @Override public List<ItemStack> before(){return before.stream().map(ItemStack::copy).toList();}
  @Override public List<ItemStack> after(){return after.stream().map(ItemStack::copy).toList();}
  @Override public Map<UUID,CompoundTag> outputs(){var copy=new LinkedHashMap<UUID,CompoundTag>();outputs.forEach((id,item)->copy.put(id,item.copy()));return Map.copyOf(copy);}
 }
 private EnchantedToolService(){}
 public static boolean ordinary(String id){return id!=null&&id.startsWith("minecraft:")&&Set.of("iron_pickaxe","iron_axe","iron_shovel","iron_hoe","iron_sword","diamond_pickaxe","diamond_axe","diamond_shovel","diamond_hoe","diamond_sword").contains(id.substring(10));}
 private static boolean eligible(ServerPlayer p){return eligible(p,true);}
 private static boolean eligible(ServerPlayer p,boolean premises){if(!TreeQuestService.real(p))return false;var store=CommerceService.initializedStore(p.server).orElse(null);if(store==null||store.quarantined())return false;var r=store.professions(p.getUUID());return r.isActive(Profession.BLACKSMITH)&&r.progressBand(Profession.BLACKSMITH)==ProfessionRank.JOURNEYMAN&&!r.track(Profession.BLACKSMITH).completedQuestIds().contains(QUEST)&&RepairService.catalog().mayAttempt(Profession.BLACKSMITH,QUEST,r.track(Profession.BLACKSMITH).completedQuestIds())&&com.mohymods.kingdomsmp.contribution.ContributionService.kingdom(p.server).attainedTier()>=4&&(!premises||ProfessionService.available(p,Profession.BLACKSMITH));}
 private static boolean clean(ItemStack s){var keys=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getAllKeys();return keys.isEmpty()||keys.equals(Set.of(RepairService.KEY));}
 private static boolean compatible(ItemStack left,ItemStack right,ItemStack result){
  var old=EnchantmentHelper.getEnchantmentsForCrafting(left);var donor=EnchantmentHelper.getEnchantmentsForCrafting(right);var out=EnchantmentHelper.getEnchantmentsForCrafting(result);if(old.isEmpty()||donor.isEmpty()||out.isEmpty()||out.size()>16)return false;
  for(var e:old.entrySet())if(out.getLevel(e.getKey())<e.getIntValue())return false;
  boolean added=false;for(var e:out.entrySet()){var holder=e.getKey();var key=holder.unwrapKey().orElse(null);if(key==null||!key.location().getNamespace().equals("minecraft")||e.getIntValue()<1||e.getIntValue()>holder.value().getMaxLevel()||!left.is(Items.ENCHANTED_BOOK)&&!left.supportsEnchantment(holder))return false;int prior=old.getLevel(holder);int incoming=donor.getLevel(holder);int expected=incoming==0?prior:Math.min(holder.value().getMaxLevel(),prior==incoming?prior+1:Math.max(prior,incoming));if(e.getIntValue()!=expected)return false;if(e.getIntValue()>prior)added=true;for(var other:out.keySet())if(!other.equals(holder)&&!Enchantment.areCompatible(holder,other))return false;}
  // No donor enchantment may be silently discarded as incompatible.
  for(var e:donor.entrySet())if(out.getLevel(e.getKey())<e.getIntValue())return false;
  var a=left.copy();var b=result.copy();for(var type:List.of(DataComponents.ENCHANTMENTS,DataComponents.STORED_ENCHANTMENTS,DataComponents.REPAIR_COST,DataComponents.DAMAGE,DataComponents.CUSTOM_NAME)){a.remove(type);b.remove(type);}return added&&ItemStack.isSameItemSameComponents(a,b);
 }
 public static void preview(Player player,AnvilMenu menu){CANDIDATES.remove(menu);if(!(player instanceof ServerPlayer p))return;try{
  String quest=menu.getSlot(0).getItem().is(Items.ENCHANTED_BOOK)?NativeOutputQuests.LIBRARY_BOOKS:QUEST;
  if(p.containerMenu!=menu||!NativeOutputQuests.eligible(p,quest,true)||menu.getCost()<1||menu.getCost()>=40||menu.getCost()>p.experienceLevel)return;
  var left=menu.getSlot(0).getItem();var right=menu.getSlot(1).getItem();var result=menu.getSlot(2).getItem();if(left.getCount()!=1||right.getCount()!=1||result.getCount()!=1||!(quest.equals(NativeOutputQuests.LIBRARY_BOOKS)?left.is(Items.ENCHANTED_BOOK):ordinary(BuiltInRegistries.ITEM.getKey(left.getItem()).toString()))||!clean(left)||!clean(right)||!(right.is(Items.ENCHANTED_BOOK)||right.is(left.getItem()))||!result.is(left.getItem())||!compatible(left,right,result))return;
  record Site(net.minecraft.world.level.Level level,BlockPos pos){}var site=((NativeCombinerAccess)menu).kingdom$access().evaluate((level,pos)->new Site(level,pos.immutable())).orElse(null);if(site==null||site.level()!=p.serverLevel()||!p.serverLevel().hasChunkAt(site.pos())||!p.serverLevel().getBlockState(site.pos()).is(BlockTags.ANVIL)||p.distanceToSqr(site.pos().getX()+.5,site.pos().getY()+.5,site.pos().getZ()+.5)>64||PlacementProtection.check(p.serverLevel(),p,site.pos(),site.pos())!=null)return;
  var id=UUID.randomUUID();var all=result.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();var marker=new CompoundTag();marker.putUUID("store",CommerceService.store(p.server).id());marker.putUUID("id",id);all.put(KEY,marker);result.set(DataComponents.CUSTOM_DATA,CustomData.of(all));CANDIDATES.put(menu,new Candidate(id,quest,p,menu,site.pos(),result.copy(),p.experienceLevel,menu.getCost()));
 }catch(RuntimeException failure){TreeLifecycle.failure(p.server,failure);}}
 public static Candidate capture(Player player,AnvilMenu menu,ItemStack output,int materials){var c=CANDIDATES.remove(menu);if(!(player instanceof ServerPlayer p)||c==null||c.player()!=p)return null;try{var nativeStack=(NativeItemIdentityAccessor)(Object)output;return materials==0&&NativeOutputQuests.eligible(p,c.quest(),true)&&p.containerMenu==menu&&p.experienceLevel==c.levels()&&menu.getCost()==c.cost()&&nativeStack.kingdom$nativeItem()==c.output().getItem()&&nativeStack.kingdom$nativeComponents().equals(((NativeItemIdentityAccessor)(Object)c.output()).kingdom$nativeComponents())?c:null;}catch(RuntimeException failure){TreeLifecycle.failure(p.server,failure);return null;}}
 private static boolean premisesAfterTake(Candidate c){
  var p=c.player();var role=NativeOutputQuests.get(c.quest()).profession();if(ProfessionService.available(p,role))return true;
  // The captured native extraction may itself break its anvil. Require every other saved block/support.
  var level=p.serverLevel();if(!level.getBlockState(c.site()).isAir())return false;
  for(var project:com.mohymods.kingdomsmp.building.ConstructionService.store(p.server).projects()){
   if(project.phase()!=com.mohymods.kingdomsmp.persistence.ConstructionStore.Phase.COMPLETE||project.npcDead()||!project.blueprint().toString().equals(role.buildingId())||project.origin()==null)continue;
   var blocks=com.mohymods.kingdomsmp.building.ConstructionFoundations.blocks(project);if(project.placed()!=blocks.size())continue;boolean skipped=false,valid=true;
   for(var block:blocks){var pos=project.origin().offset(block.offset());if(!level.hasChunkAt(pos)){valid=false;break;}if(pos.equals(c.site())&&block.state().is(BlockTags.ANVIL)){skipped=true;continue;}if(!com.mohymods.kingdomsmp.building.ConstructionBlueprint.matches(level.getBlockState(pos),block.state())){valid=false;break;}}
   if(valid&&skipped&&(project.foundations().isEmpty()||com.mohymods.kingdomsmp.building.ConstructionFoundations.validate(level,project,true)==null))return true;
  }return false;
 }
 public static void commit(Candidate c){if(c==null)return;var p=c.player();try{if(!NativeOutputQuests.eligible(p,c.quest(),false)||!premisesAfterTake(c)||p.containerMenu!=c.menu()||!c.menu().getSlot(0).getItem().isEmpty()||!c.menu().getSlot(1).getItem().isEmpty()||p.experienceLevel!=c.levels()-c.cost()||PlacementProtection.check(p.serverLevel(),p,c.site(),c.site())!=null)return;CommerceService.store(p.server).issueNativeOutput(c.id(),p.getUUID(),c.quest(),(CompoundTag)c.output().save(p.registryAccess()));}catch(IOException|RuntimeException failure){TreeLifecycle.failure(p.server,new IllegalStateException("Native anvil output evidence unavailable; native transfer retained",failure));}}
 private static UUID id(ServerPlayer p,ItemStack s){var marker=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound(KEY);return marker.hasUUID("store")&&marker.hasUUID("id")&&marker.getUUID("store").equals(CommerceService.store(p.server).id())?marker.getUUID("id"):null;}
 private static Map<UUID,CompoundTag> select(ServerPlayer p,String quest,boolean consume,List<ItemStack> before,List<ItemStack> after,int required){var store=CommerceService.store(p.server);Map<UUID,CompoundTag> found=new LinkedHashMap<>();for(int slot=0;slot<36&&found.size()<required;slot++){var s=before.get(slot);if(s.isEmpty()||s.getCount()!=1)continue;var id=id(p,s);if(id==null||found.containsKey(id))continue;var item=(CompoundTag)s.save(p.registryAccess());if(store.matchesNativeOutput(id,p.getUUID(),quest,item)){found.put(id,item);if(consume)after.set(slot,ItemStack.EMPTY);}}return found;}
 public static String status(ServerPlayer p){return "Combine compatible ordinary enchantments at a legitimate anvil, paying native XP and inputs. Deliver "+RepairQuestRules.enchantedTools(p.server)+" distinct authenticated tools at your Blacksmith. Preview, altered and recycled outputs do not count.";}
 public static boolean delivery(ServerPlayer p){return eligible(p);}
 public static Quote review(ServerPlayer p)throws IOException{return review(p,QUEST,true,RepairQuestRules.enchantedTools(p.server));}
 public static Quote reviewLibrary(ServerPlayer p)throws IOException{return reviewLibrary(p,NativeOutputQuests.LIBRARY_ENCHANT);}
 public static Quote reviewLibrary(ServerPlayer p,String quest)throws IOException{if(NativeOutputQuests.get(quest).profession()!=Profession.LIBRARIAN)throw new IllegalArgumentException("Choose a native library output");return review(p,quest,true,NativeOutputQuests.goal(p,quest));}
 public static Quote reviewSmithRegistration(ServerPlayer p)throws IOException{return review(p,SmithingQuestService.QUEST,false,RepairQuestRules.upgrades(p.server));}
 private static boolean eligible(ServerPlayer p,String quest){return NativeOutputQuests.eligible(p,quest,true);}
 private static Quote review(ServerPlayer p,String quest,boolean consume,int required)throws IOException{ContractService.requireCounter(p,NativeOutputQuests.get(quest).profession());if(!eligible(p,quest))throw new IllegalStateException("Authenticated output objective unavailable");var store=CommerceService.store(p.server);CommerceService.verifyReceipt(p,store);List<ItemStack> before=new ArrayList<>();for(int i=0;i<36;i++)before.add(p.getInventory().getItem(i).copy());var after=new ArrayList<>(before);var outputs=select(p,quest,consume,before,after,required);if(outputs.size()!=required)throw new IllegalStateException("Bring "+required+" distinct unchanged authenticated native outputs; review consumes nothing");var q=new Quote(UUID.randomUUID(),store.revision(),quest,consume,before,after,outputs,required,System.nanoTime(),consume?"Permanently deliver "+required+" authenticated native outputs for "+NativeOutputQuests.get(quest).xp()+" "+ProfessionService.name(NativeOutputQuests.get(quest).profession())+" XP once. Exact physical items and their output rights are consumed together.":"Register "+required+" authenticated native smithing upgrade for300Blacksmith XP once. Keep your upgraded equipment. Registration uses a native inventory receipt and permanent output identity.");QUOTES.put(p,q);return q;}
 public static void close(ServerPlayer p){QUOTES.remove(p);}
 public static String confirm(ServerPlayer p,UUID nonce)throws IOException{var q=QUOTES.remove(p);var store=CommerceService.store(p.server);CommerceService.verifyReceipt(p,store);if(q==null)throw new IllegalStateException("Native output review expired");ContractService.requireCounter(p,NativeOutputQuests.get(q.quest()).profession());if(!q.nonce().equals(nonce)||q.revision()!=store.revision()||System.nanoTime()-q.issued()>30_000_000_000L||!eligible(p,q.quest())||q.required()!=NativeOutputQuests.goal(p,q.quest()))throw new IllegalStateException("Enchanted delivery changed or expired; review again");var before=q.before();for(int i=0;i<36;i++)if(!ItemStack.matches(p.getInventory().getItem(i),before.get(i)))throw new IllegalStateException("Inventory changed; review again");var old=p.getPersistentData().copy();boolean checkpointStarted=false;try{CommerceService.checkpoint(p);var pending=q.consume()?store.prepareNativeOutputDelivery(q.nonce(),p.getUUID(),q.quest(),q.outputs(),q.required()):store.prepareNativeSmithRegistration(q.nonce(),p.getUUID(),q.outputs(),q.required());var after=q.after();for(int i=0;i<36;i++)p.getInventory().setItem(i,after.get(i));CommerceService.putReceipt(p,new EconomyStore.Receipt(store.id(),pending.sequence()));checkpointStarted=true;CommerceService.checkpoint(p);if(!store.recoverItem(CommerceService.receipt(AtomicNbt.read(CommerceService.playerFile(p.server,p.getUUID())))))throw new IOException("Enchanted native receipt did not commit");p.getInventory().setChanged();p.inventoryMenu.sendAllDataToRemote();return q.consume()?"Authenticated native outputs consumed; "+NativeOutputQuests.get(q.quest()).xp()+" XP recorded once.":"Authenticated smithing upgrade registered; equipment kept and300XP recorded once.";}catch(IOException|RuntimeException failure){if(!checkpointStarted){for(int i=0;i<36;i++)p.getInventory().setItem(i,before.get(i));for(var key:new HashSet<>(p.getPersistentData().getAllKeys()))p.getPersistentData().remove(key);p.getPersistentData().merge(old);}if(!checkpointStarted&&store.pendingItem().isEmpty()&&!store.quarantined()&&(failure instanceof IllegalArgumentException||failure instanceof IllegalStateException))throw failure;store.quarantine();p.connection.disconnect(net.minecraft.network.chat.Component.literal("Enchanted delivery interrupted; restore matching native and canonical checkpoints"));throw failure;}}
}
