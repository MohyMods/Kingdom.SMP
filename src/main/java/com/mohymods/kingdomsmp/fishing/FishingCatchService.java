package com.mohymods.kingdomsmp.fishing;
import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.network.CommerceView;
import com.mohymods.kingdomsmp.persistence.ConstructionStore;
import com.mohymods.kingdomsmp.placement.PlacementProtection;
import com.mohymods.kingdomsmp.profession.ContractRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;
/** Actual uncancelled Aquaculture retrieves, observed after loot processing. No client proof or item data is trusted. */
public final class FishingCatchService {
 private record Pending(ServerPlayer player,FishingHook hook,BlockPos water,long week,List<ItemFishedEvent> events){}
 private static final Map<MinecraftServer,Map<UUID,Pending>> PENDING=new WeakHashMap<>();
 private FishingCatchService(){}
 public static void register(IEventBus bus){bus.addListener(EventPriority.LOWEST,false,ItemFishedEvent.class,FishingCatchService::fished);bus.addListener(FishingCatchService::tick);}
 private static boolean real(ServerPlayer p){return !(p instanceof FakePlayer)&&p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&p.server.getPlayerList().getPlayer(p.getUUID())==p;}
 private static boolean wharf(ServerPlayer p){return ContributionService.kingdom(p.server).attainedTier()>=3&&ConstructionService.store(p.server).projects().stream().anyMatch(v->v.blueprint().equals(FishermansWharfBlueprint.ID)&&v.phase()==ConstructionStore.Phase.COMPLETE&&!v.npcDead()&&(!ConstructionService.loaded(p.serverLevel(),v)||ConstructionService.structureIntact(p.serverLevel(),v)));}
 private static void fished(ItemFishedEvent event){
  if(!(event.getEntity() instanceof ServerPlayer p)||!real(p)||p.serverLevel().dimension()!=Level.OVERWORLD||!AquacultureAccess.available())return;var hook=event.getHookEntity();var level=p.serverLevel();var pos=hook.blockPosition();
  if(!hook.getClass().getName().equals("com.teammetallurgy.aquaculture.entity.AquaFishingBobberEntity")||hook.isRemoved()||hook.getPlayerOwner()!=p||p.fishing!=hook||level.getEntity(hook.getUUID())!=hook||hook.level()!=level||hook.tickCount<20||p.distanceToSqr(hook)>1024||!level.hasChunkAt(pos)||!level.getFluidState(pos).is(FluidTags.WATER)||PlacementProtection.check(level,p,pos,pos)!=null||!wharf(p))return;
  var queue=PENDING.computeIfAbsent(p.server,s->new HashMap<>());if(queue.size()>=128&&!queue.containsKey(hook.getUUID()))return;var pending=queue.computeIfAbsent(hook.getUUID(),id->new Pending(p,hook,pos.immutable(),ContractRules.period("weekly"),new ArrayList<>()));if(pending.events().size()<4)pending.events().add(event);
 }
 private static void tick(ServerTickEvent.Post tick){
  var queue=PENDING.remove(tick.getServer());if(queue==null)return;
  for(var pending:queue.values())try{
   var p=pending.player();var hook=pending.hook();var level=p.serverLevel();var pos=pending.water();
   if(!real(p)||level.dimension()!=Level.OVERWORLD||hook.level()!=level||!hook.isRemoved()||p.distanceToSqr(Vec3.atCenterOf(pos))>1024||pending.week()!=ContractRules.period("weekly")||!level.hasChunkAt(pos)||!level.getFluidState(pos).is(FluidTags.WATER)||PlacementProtection.check(level,p,pos,pos)!=null||!wharf(p))continue;
   Set<String> species=new HashSet<>();for(var event:pending.events())if(!event.isCanceled())for(var stack:event.getDrops()){
    var id=BuiltInRegistries.ITEM.getKey(stack.getItem());if(!id.getNamespace().equals("aquaculture")||!FishingCatchBook.SPECIES.contains(id.getPath())||!stack.getItem().getClass().getName().equals("com.teammetallurgy.aquaculture.item.FishItem")||stack.isEmpty()||!ItemStack.isSameItemSameComponents(stack,new ItemStack(stack.getItem())))continue;species.add(id.getPath());
   }
   if(species.isEmpty())continue;var data=FishingSavedData.get(p.server);if(data.book().record(p.getUUID(),p.getGameProfile().getName(),hook.getUUID(),species,pending.week(),System.currentTimeMillis()))data.setDirty();
  }catch(RuntimeException failure){com.mojang.logging.LogUtils.getLogger().error("Fishing collection evidence requires recovery; no financial reward was issued",failure);}
 }
 public static List<CommerceView.Entry> entries(ServerPlayer p){
  var book=FishingSavedData.get(p.server).book();var caught=book.player(p.getUUID()).map(FishingCatchBook.Player::collection).orElse(Set.of());var standings=book.standings(ContractRules.period("weekly"));List<CommerceView.Entry> rows=new ArrayList<>();
  rows.add(new CommerceView.Entry("catchbook:summary","Your personal catch collection","Verified Aquaculture catches "+caught.size()+"/"+FishingCatchBook.SPECIES.size()+"; rare species "+caught.stream().filter(FishingCatchBook.RARE::contains).count()+"/5. Only your real Overworld rod retrieves count after Wharf completion. Traded, delivered or spawned inventory fish grant no proof.",0,"IN_PROGRESS"));
  for(var species:FishingCatchBook.SPECIES){String title=Arrays.stream(species.split("_")).map(s->Character.toUpperCase(s.charAt(0))+s.substring(1)).collect(java.util.stream.Collectors.joining(" "));String habitat=switch(species){case "atlantic_halibut","tuna"->"Ocean or beach";case "capitaine"->"Dry, non-cold Overworld biome";case "arapaima"->"Jungle";default->"Temperate inland waters; Aquaculture biome conditions apply";};rows.add(new CommerceView.Entry("catchbook:"+species,(FishingCatchBook.RARE.contains(species)?"Rare: ":"Catch: ")+title,habitat+". "+(caught.contains(species)?"Personally caught and permanently recorded.":"Not yet personally recorded. No fish is consumed by the collection."),0,caught.contains(species)?"COMPLETE":"IN_PROGRESS"));}
  rows.add(new CommerceView.Entry("derby:rules","Weekly diversity derby","Friendly recognition for different collection species personally caught this UTC week. Repeated fish add no score. Ties: earliest current score, then player UUID. Monday reset; last 8 weeks retained. No fee, item prize or coin reward.",0,"IN_PROGRESS"));
  int rank=0;for(var row:standings.stream().limit(10).toList())rows.add(new CommerceView.Entry("derby:"+row.player(),"#"+(++rank)+" "+row.name(),row.species()+"/10 different verified Overworld species this week. Personal collection remains permanent after weekly reset.",0,"IN_PROGRESS"));return List.copyOf(rows);
 }
}
