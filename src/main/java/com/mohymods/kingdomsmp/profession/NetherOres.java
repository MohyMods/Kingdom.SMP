package com.mohymods.kingdomsmp.profession;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.*;
import net.neoforged.neoforge.registries.*;
import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.KingdomSmp;
/** Exact native Nether feature writes and native successful correct-tool removal only. */
public final class NetherOres {
 public static final Map<String,String> BLOCKS=Map.of("minecraft:nether_quartz_ore","quartz","minecraft:nether_gold_ore","nether_gold","minecraft:ancient_debris","debris");
 public static final Map<String,Integer> GOALS=Map.of("kingdom_smp:miner/m01",64,"kingdom_smp:miner/m02",32,"kingdom_smp:miner/m03",4);
 private static final Map<String,String> METRICS=Map.of("kingdom_smp:miner/m01","quartz","kingdom_smp:miner/m02","nether_gold","kingdom_smp:miner/m03","debris");
 private static final DeferredRegister<AttachmentType<?>> TYPES=DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES,KingdomSmp.MOD_ID);
 public static final Supplier<AttachmentType<NetherOreMarks>> MARKS=TYPES.register("native_nether_ore_provenance",()->AttachmentType.builder(NetherOreMarks::new).serialize(new IAttachmentSerializer<CompoundTag,NetherOreMarks>(){public NetherOreMarks read(IAttachmentHolder h,CompoundTag tag,HolderLookup.Provider registry){return NetherOreMarks.load(tag);}public CompoundTag write(NetherOreMarks marks,HolderLookup.Provider registry){return marks.save();}}).build());
 private NetherOres(){}public static void register(IEventBus bus){TYPES.register(bus);}public static boolean contains(String id){return GOALS.containsKey(id);}
 public static void generated(WorldGenLevel world,BlockPos pos,BlockState before,BlockState after){try{if(!(world instanceof WorldGenRegion)||world.getLevel().dimension()!=Level.NETHER||before.is(after.getBlock()))return;var id=BuiltInRegistries.BLOCK.getKey(after.getBlock()).toString();if(!BLOCKS.containsKey(id))return;var chunk=world.getChunk(pos.getX()>>4,pos.getZ()>>4);if(!(chunk instanceof ProtoChunk)||chunk instanceof ImposterProtoChunk)return;synchronized(chunk){if(!chunk.getBlockState(pos).is(after.getBlock()))return;chunk.getData(MARKS).mark(pos.asLong(),id);chunk.setUnsaved(true);}}catch(RuntimeException failure){TreeLifecycle.failure(world.getLevel().getServer(),failure);}}
 public static void changed(LevelChunk chunk,BlockPos pos,BlockState before,BlockState after){try{if(before==null||before.is(after.getBlock())||chunk.getLevel().isClientSide)return;synchronized(chunk){if(chunk.hasData(MARKS)&&chunk.getData(MARKS).invalidate(pos.asLong()))chunk.setUnsaved(true);}}catch(RuntimeException failure){if(chunk.getLevel() instanceof ServerLevel level)TreeLifecycle.failure(level.getServer(),failure);}}
 private static boolean eligible(ServerPlayer p){if(!TreeQuestService.real(p)||p.serverLevel().dimension()!=Level.NETHER||ContributionService.kingdom(p.server).attainedTier()<5)return false;var store=CommerceService.initializedStore(p.server).orElse(null);if(store==null||store.quarantined())return false;var r=store.professions(p.getUUID());return r.isActive(Profession.MINER)&&r.progressBand(Profession.MINER)==ProfessionRank.MASTER;}
 public record Proof(ServerPlayer player,BlockPos pos,String metric){}
 public static Proof capture(ServerPlayer p,BlockPos pos,boolean harvest){try{if(!harvest||!eligible(p)||!p.serverLevel().hasChunkAt(pos)||!TreeLifecycle.consent(p.serverLevel(),p,pos))return null;var chunk=p.serverLevel().getChunkAt(pos);var id=BuiltInRegistries.BLOCK.getKey(chunk.getBlockState(pos).getBlock()).toString();var metric=BLOCKS.get(id);if(metric==null)return null;synchronized(chunk){if(!chunk.hasData(MARKS)||!id.equals(chunk.getData(MARKS).at(pos.asLong())))return null;}return new Proof(p,pos.immutable(),metric);}catch(RuntimeException failure){TreeLifecycle.failure(p.server,failure);return null;}}
 public static void mined(Proof proof,boolean success){if(proof==null||!success)return;var p=proof.player();try{if(!eligible(p)||!p.serverLevel().hasChunkAt(proof.pos())||BLOCKS.containsKey(BuiltInRegistries.BLOCK.getKey(p.serverLevel().getBlockState(proof.pos()).getBlock()).toString())||!TreeLifecycle.consent(p.serverLevel(),p,proof.pos()))return;var data=NetherOreSavedData.get(p.server);if(data.evidence().record(p.getUUID(),"minecraft:the_nether",proof.pos().asLong(),proof.metric()))data.setDirty();}catch(RuntimeException failure){TreeLifecycle.failure(p.server,failure);}}
 public static int count(ServerPlayer p,String quest){return NetherOreSavedData.get(p.server).evidence().count(p.getUUID(),METRICS.get(quest));}
 public static String status(ServerPlayer p,String quest){return count(p,quest)+" / "+NetherOreRules.get(p.server).goal(quest)+" personally mined tracked native Nether "+METRICS.get(quest)+" during Master trials, after Tier5. Correct tools/current claim permission are required. Unknown old and placed ores do not grant retrospective source proof. Return to Doran to review.";}
}
