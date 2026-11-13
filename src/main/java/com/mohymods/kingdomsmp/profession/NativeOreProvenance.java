package com.mohymods.kingdomsmp.profession;
import com.mohymods.kingdomsmp.KingdomSmp;
import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.contribution.ContributionService;
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
import java.util.function.Supplier;
/** Native feature writes attach to ProtoChunks; no asynchronous SavedData/player access or inferred naturalness. */
public final class NativeOreProvenance {
 private static final DeferredRegister<AttachmentType<?>> TYPES=DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES,KingdomSmp.MOD_ID);
 public static final Supplier<AttachmentType<NativeOreMarks>> MARKS=TYPES.register("native_ore_provenance",()->AttachmentType.builder(NativeOreMarks::new).serialize(new IAttachmentSerializer<CompoundTag,NativeOreMarks>(){
  public NativeOreMarks read(IAttachmentHolder h,CompoundTag tag,HolderLookup.Provider p){return NativeOreMarks.load(tag);}
  public CompoundTag write(NativeOreMarks data,HolderLookup.Provider p){return data.save();}
 }).build());
 private NativeOreProvenance(){}public static void register(IEventBus bus){TYPES.register(bus);}
 public static void generated(WorldGenLevel world,BlockPos pos,BlockState before,BlockState after){try{
  if(!(world instanceof WorldGenRegion)||world.getLevel().dimension()!=Level.OVERWORLD||before.is(after.getBlock()))return;String id=BuiltInRegistries.BLOCK.getKey(after.getBlock()).toString();if(!NativeOreQuests.BLOCKS.containsKey(id))return;
  var chunk=world.getChunk(pos.getX()>>4,pos.getZ()>>4);if(!(chunk instanceof ProtoChunk)||chunk instanceof ImposterProtoChunk)return;
  synchronized(chunk){if(!chunk.getBlockState(pos).is(after.getBlock()))return;chunk.getData(MARKS).mark(pos.asLong(),id);chunk.setUnsaved(true);}
 }catch(RuntimeException failure){TreeLifecycle.failure(world.getLevel().getServer(),failure);}}
 public static void changed(LevelChunk chunk,BlockPos pos,BlockState before,BlockState after){try{if(before==null||before.is(after.getBlock())||chunk.getLevel().isClientSide)return;synchronized(chunk){if(chunk.hasData(MARKS)&&chunk.getData(MARKS).invalidate(pos.asLong()))chunk.setUnsaved(true);}}catch(RuntimeException failure){if(chunk.getLevel() instanceof ServerLevel l)TreeLifecycle.failure(l.getServer(),failure);}}
 public record Proof(ServerPlayer player,BlockPos pos,String dimension,String metric,ProfessionRank band){}
 public static Proof capture(ServerPlayer p,BlockPos pos,boolean nativeHarvest){try{if(!nativeHarvest||!TreeQuestService.real(p)||p.serverLevel().dimension()!=Level.OVERWORLD||!p.serverLevel().hasChunkAt(pos))return null;var level=p.serverLevel();var state=level.getBlockState(pos);String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();var metric=NativeOreQuests.BLOCKS.get(id);if(metric==null||!TreeLifecycle.consent(level,p,pos))return null;var store=CommerceService.initializedStore(p.server).orElse(null);if(store==null||store.quarantined())return null;var r=store.professions(p.getUUID());if(!r.isActive(Profession.MINER)||r.progressBand(Profession.MINER)!=ProfessionRank.APPRENTICE||ContributionService.kingdom(p.server).attainedTier()<3)return null;var chunk=level.getChunkAt(pos);synchronized(chunk){if(!chunk.hasData(MARKS)||!id.equals(chunk.getData(MARKS).at(pos.asLong())))return null;}return new Proof(p,pos.immutable(),level.dimension().location().toString(),metric,ProfessionRank.APPRENTICE);}catch(RuntimeException failure){TreeLifecycle.failure(p.server,failure);return null;}}
 public static void mined(Proof proof,boolean success){if(proof==null||!success)return;var p=proof.player();try{if(!TreeQuestService.real(p)||!p.serverLevel().dimension().location().toString().equals(proof.dimension())||!p.serverLevel().hasChunkAt(proof.pos())||NativeOreQuests.BLOCKS.containsKey(BuiltInRegistries.BLOCK.getKey(p.serverLevel().getBlockState(proof.pos()).getBlock()).toString())||!TreeLifecycle.consent(p.serverLevel(),p,proof.pos()))return;var r=CommerceService.store(p.server).professions(p.getUUID());if(!r.isActive(Profession.MINER)||r.progressBand(Profession.MINER)!=proof.band())return;var data=NativeOreSavedData.get(p.server);if(data.evidence().record(p.getUUID(),proof.band(),proof.metric(),proof.dimension(),proof.pos().asLong()))data.setDirty();}catch(RuntimeException failure){TreeLifecycle.failure(p.server,failure);}}
}
