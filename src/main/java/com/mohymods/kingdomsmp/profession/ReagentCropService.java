package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.*;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.placement.PlacementProtection;
/** Exact verified Mystical Agriculture crop, native age-zero seed placement and real native growth/drop callbacks. */
public final class ReagentCropService {
 public static final String QUEST="kingdom_smp:alchemist/j06",CROP="mysticalagriculture:inferium_crop",ESSENCE="mysticalagriculture:inferium_essence";
 private static final Map<MinecraftServer,ArrayDeque<Runnable>> PENDING=new WeakHashMap<>();
 private ReagentCropService(){}
 public static boolean supported(){var list=ModList.get();return list!=null&&list.getModContainerById("mysticalagriculture").map(c->c.getModInfo().getVersion().toString().equals("8.0.29")).orElse(false)&&list.getModContainerById("cucumber").map(c->c.getModInfo().getVersion().toString().equals("8.0.16")).orElse(false);}
 public static boolean crop(BlockState state){return supported()&&state.getBlock() instanceof CropBlock&&BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(CROP);}
 private static boolean eligible(ServerPlayer p){if(!supported()||!TreeQuestService.real(p))return false;var store=CommerceService.initializedStore(p.server).orElse(null);if(store==null||store.quarantined())return false;var r=store.professions(p.getUUID());return r.isActive(Profession.ALCHEMIST)&&r.progressBand(Profession.ALCHEMIST)==ProfessionRank.JOURNEYMAN&&com.mohymods.kingdomsmp.contribution.ContributionService.kingdom(p.server).attainedTier()>=6;}
 private static boolean permitted(ServerPlayer p,ServerLevel level,BlockPos pos){return p.serverLevel()==level&&level.hasChunkAt(pos)&&level.getWorldBorder().isWithinBounds(pos)&&PlacementProtection.check(level,p,pos,pos)==null;}
 private static ReagentCropProof.Cell cell(ServerLevel level,BlockPos pos){return new ReagentCropProof.Cell(level.dimension().location().toString(),pos.asLong());}
 private static void later(MinecraftServer server,Runnable action){var q=PENDING.computeIfAbsent(server,s->new ArrayDeque<>());if(q.size()<8192)q.add(action);}
 public static void register(IEventBus bus){bus.addListener(EventPriority.LOWEST,false,BlockEvent.EntityPlaceEvent.class,ReagentCropService::plant);bus.addListener(EventPriority.LOWEST,false,CropGrowEvent.Post.class,ReagentCropService::grown);bus.addListener(EventPriority.LOWEST,false,BlockDropsEvent.class,ReagentCropService::harvest);bus.addListener(ReagentCropService::tick);}
 private static void tick(ServerTickEvent.Post event){var q=PENDING.get(event.getServer());if(q==null)return;for(int n=0;n<512&&!q.isEmpty();n++)try{q.removeFirst().run();}catch(RuntimeException failure){TreeLifecycle.failure(event.getServer(),failure);}}
 private static void plant(BlockEvent.EntityPlaceEvent event){if(!(event.getLevel() instanceof ServerLevel level)||!supported())return;var pos=event.getPos().immutable();var placed=event.getPlacedBlock();var player=event.getEntity() instanceof ServerPlayer p?p:null;boolean valid=player!=null&&eligible(player)&&crop(placed)&&((CropBlock)placed.getBlock()).getAge(placed)==0&&permitted(player,level,pos);later(level.getServer(),()->{if(!level.hasChunkAt(pos)||!level.getBlockState(pos).equals(placed))return;var data=ReagentCropSavedData.get(level.getServer());data.proof().invalidate(cell(level,pos));if(valid&&permitted(player,level,pos))data.proof().plant(cell(level,pos),player.getUUID());data.setDirty();});}
 private static void grown(CropGrowEvent.Post event){if(event.getLevel() instanceof ServerLevel level)growth(level,event.getPos(),event.getOriginalState(),event.getState());}
 public static void growth(Level world,BlockPos pos,BlockState before,BlockState after){if(!(world instanceof ServerLevel level)||!crop(before)||!crop(after))return;try{if(!level.hasChunkAt(pos)||!level.getBlockState(pos).equals(after))return;var data=ReagentCropSavedData.get(level.getServer());data.proof().grow(cell(level,pos),((CropBlock)before.getBlock()).getAge(before),((CropBlock)after.getBlock()).getAge(after));data.setDirty();}catch(RuntimeException failure){TreeLifecycle.failure(level.getServer(),failure);}}
 private static void harvest(BlockDropsEvent event){if(!crop(event.getState()))return;var level=event.getLevel();try{var pos=event.getPos().immutable();var data=ReagentCropSavedData.get(level.getServer());var plant=data.proof().take(cell(level,pos)).orElse(null);data.setDirty();var player=event.getBreaker() instanceof ServerPlayer p?p:null;boolean valid=plant!=null&&plant.age()==7&&player!=null&&plant.owner().equals(player.getUUID())&&eligible(player)&&permitted(player,level,pos)&&((CropBlock)event.getState().getBlock()).isMaxAge(event.getState());later(level.getServer(),()->{if(!valid||event.isCanceled()||!level.hasChunkAt(pos)||level.getBlockState(pos).equals(event.getState())||!eligible(player)||!permitted(player,level,pos))return;int amount=event.getDrops().stream().filter(e->BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).toString().equals(ESSENCE)).mapToInt(e->e.getItem().getCount()).sum();if(amount<1||amount>128)return;data.proof().harvest(player.getUUID(),amount);var current=level.getBlockState(pos);if(crop(current)&&((CropBlock)current.getBlock()).getAge(current)==0)data.proof().plant(cell(level,pos),player.getUUID());data.setDirty();});}catch(RuntimeException failure){TreeLifecycle.failure(level.getServer(),failure);}}
 public static boolean ready(ServerPlayer p,int quantity){return eligible(p)&&ReagentCropSavedData.get(p.server).proof().count(p.getUUID())>=quantity;}
 public static String status(ServerPlayer p,int quantity){return supported()?ReagentCropSavedData.get(p.server).proof().count(p.getUUID())+" / "+quantity+" native Inferium essence from your planted, genuinely grown mature crops during Journeyman trials. Deliver matching plain essence permanently at Nessa. Unknown old or placed mature crops give no proof.":"Requires verified Mystical Agriculture8.0.29andCucumber8.0.16. Existing records and resources are preserved.";}
}
