package com.mohymods.kingdomsmp.profession;
import java.util.*;
import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import net.minecraft.core.*;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.event.level.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
/** Verified native budding callback + successful actual loot, never placed mature clusters or stocked shards. */
public final class AmethystGrowth {
 public static final String QUEST="kingdom_smp:miner/j05",SHARD="minecraft:amethyst_shard";
 public record Frame(AmethystEvidence.Growth previous,int before){}
 private record Key(ServerLevel level,long position){}
 private record Harvest(ServerPlayer player,AmethystEvidence.Growth growth,int tick){}
 private static final Map<MinecraftServer,Map<Key,Harvest>> PENDING=new WeakHashMap<>();
 private static final Map<MinecraftServer,ArrayDeque<Runnable>> LATER=new WeakHashMap<>();
 private AmethystGrowth(){}
 public static int stage(BlockState state){return state.is(Blocks.SMALL_AMETHYST_BUD)?1:state.is(Blocks.MEDIUM_AMETHYST_BUD)?2:state.is(Blocks.LARGE_AMETHYST_BUD)?3:state.is(Blocks.AMETHYST_CLUSTER)?4:0;}
 public static Frame before(ServerLevel level,BlockPos pos){try{if(!level.getServer().isSameThread()||level.dimension()!=Level.OVERWORLD)return null;var state=level.getBlockState(pos);if(stage(state)==0&&!BuddingAmethystBlock.canClusterGrowAtState(state))return null;return new Frame(AmethystSavedData.get(level.getServer()).evidence().at(pos.asLong()),stage(state));}catch(RuntimeException failure){TreeLifecycle.failure(level.getServer(),failure);return null;}}
 public static void grown(ServerLevel level,BlockPos pos,BlockState after,Frame frame){try{if(frame==null||!level.getBlockState(pos).equals(after)||stage(after)<1)return;var bud=pos.relative(after.getValue(AmethystClusterBlock.FACING).getOpposite());if(!level.hasChunkAt(bud)||!level.getBlockState(bud).is(Blocks.BUDDING_AMETHYST))return;var data=AmethystSavedData.get(level.getServer());data.evidence().grow(pos.asLong(),bud.asLong(),frame.before(),stage(after),frame.previous());data.setDirty();}catch(RuntimeException failure){TreeLifecycle.failure(level.getServer(),failure);}}
 public static void changed(LevelChunk chunk,BlockPos pos,BlockState before,BlockState after){try{if(before==null||before.is(after.getBlock())||(stage(before)==0&&!before.is(Blocks.BUDDING_AMETHYST))||!(chunk.getLevel() instanceof ServerLevel level)||level.dimension()!=Level.OVERWORLD||!level.getServer().isSameThread()||!AmethystSavedData.present(level.getServer()))return;var data=AmethystSavedData.get(level.getServer());data.evidence().invalidate(pos.asLong());if(before.is(Blocks.BUDDING_AMETHYST))for(var direction:Direction.values())data.evidence().invalidate(pos.relative(direction).asLong());data.setDirty();}catch(RuntimeException failure){if(chunk.getLevel() instanceof ServerLevel level)TreeLifecycle.failure(level.getServer(),failure);}}
 private static boolean eligible(ServerPlayer p){if(!TreeQuestService.real(p)||p.serverLevel().dimension()!=Level.OVERWORLD)return false;var store=CommerceService.initializedStore(p.server).orElse(null);if(store==null||store.quarantined())return false;var r=store.professions(p.getUUID());return r.isActive(Profession.MINER)&&r.progressBand(Profession.MINER)==ProfessionRank.JOURNEYMAN&&ContributionService.kingdom(p.server).attainedTier()>=4&&!r.track(Profession.MINER).completedQuestIds().contains(QUEST);}
 public static void register(IEventBus bus){bus.addListener(EventPriority.LOWEST,false,BlockEvent.BreakEvent.class,AmethystGrowth::breaking);bus.addListener(EventPriority.LOWEST,false,BlockDropsEvent.class,AmethystGrowth::drops);bus.addListener(AmethystGrowth::tick);bus.addListener(net.neoforged.neoforge.event.server.ServerStoppedEvent.class,e->{PENDING.remove(e.getServer());LATER.remove(e.getServer());});}
 private static void breaking(BlockEvent.BreakEvent event){if(!(event.getPlayer() instanceof ServerPlayer p)||!event.getState().is(Blocks.AMETHYST_CLUSTER))return;try{if(!eligible(p)||!TreeLifecycle.consent(p.serverLevel(),p,event.getPos())||!event.getState().canHarvestBlock(p.serverLevel(),event.getPos(),p))return;var data=AmethystSavedData.get(p.server);var growth=data.evidence().at(event.getPos().asLong());if(growth==null||growth.stage()!=4||!p.serverLevel().getBlockState(BlockPos.of(growth.budding())).is(Blocks.BUDDING_AMETHYST))return;var pending=PENDING.computeIfAbsent(p.server,s->new HashMap<>());if(pending.size()<512)pending.put(new Key(p.serverLevel(),event.getPos().asLong()),new Harvest(p,growth,p.server.getTickCount()));}catch(RuntimeException failure){TreeLifecycle.failure(p.server,failure);}}
 private static void drops(BlockDropsEvent event){if(!event.getState().is(Blocks.AMETHYST_CLUSTER))return;var level=event.getLevel();try{var pending=PENDING.get(level.getServer());var harvest=pending==null?null:pending.remove(new Key(level,event.getPos().asLong()));if(harvest==null||harvest.tick()!=level.getServer().getTickCount()||event.getBreaker()!=harvest.player())return;var pos=event.getPos().immutable();var queue=LATER.computeIfAbsent(level.getServer(),s->new ArrayDeque<>());if(queue.size()>=512)return;queue.add(()->{var p=harvest.player();if(event.isCanceled()||!eligible(p)||!level.hasChunkAt(pos)||level.getBlockState(pos).is(Blocks.AMETHYST_CLUSTER)||!TreeLifecycle.consent(level,p,pos)||!level.hasChunkAt(BlockPos.of(harvest.growth().budding()))||!level.getBlockState(BlockPos.of(harvest.growth().budding())).is(Blocks.BUDDING_AMETHYST))return;int amount=event.getDrops().stream().filter(e->e.getItem().is(Items.AMETHYST_SHARD)).mapToInt(e->e.getItem().getCount()).sum();if(amount<1||amount>64)return;var data=AmethystSavedData.get(level.getServer());data.evidence().harvested(p.getUUID(),amount);data.setDirty();});}catch(RuntimeException failure){TreeLifecycle.failure(level.getServer(),failure);}}
 private static void tick(ServerTickEvent.Post event){var server=event.getServer();var queue=LATER.get(server);if(queue!=null)while(!queue.isEmpty())try{queue.removeFirst().run();}catch(RuntimeException failure){TreeLifecycle.failure(server,failure);}var pending=PENDING.get(server);if(pending!=null)pending.clear();}
 public static boolean ready(ServerPlayer p,int quantity){return eligible(p)&&AmethystSavedData.get(p.server).evidence().count(p.getUUID())>=quantity;}
 public static String status(ServerPlayer p,int quantity){return AmethystSavedData.get(p.server).evidence().count(p.getUUID())+" / "+quantity+" shards personally harvested from fully tracked native budding growth during Journeyman trials. Keep the budding support intact; permanently deliver plain shards at Doran. Placed mature buds, old unknown clusters, Silk Touch cluster items and stocked shards grant no growth proof.";}
}
