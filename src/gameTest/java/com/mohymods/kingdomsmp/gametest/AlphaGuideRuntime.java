package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.integration.ftbquests.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import com.mohymods.kingdomsmp.profession.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.*;

/** Observe actual native FTB timers against already genuinely earned personal records. Never set progress. */
final class AlphaGuideRuntime {
    private static final Set<GameTestHelper> STARTED=Collections.newSetFromMap(new WeakHashMap<>());
    static void run(GameTestHelper h)throws Exception{if(STARTED.add(h))new Walk(h).start();}
    private static final class Walk {
        final GameTestHelper h;final net.minecraft.server.MinecraftServer server;final EconomyStore store;final Path proof;final Class<?> team,taskType;final Object file;
        ServerPlayer library,other;long revision,deadline;boolean reset,restoring;
        Walk(GameTestHelper h)throws Exception{this.h=h;server=h.getLevel().getServer();store=CommerceService.store(server);proof=server.getWorldPath(LevelResource.ROOT).resolve("alpha-guide-native-test.nbt");team=Class.forName("dev.ftb.mods.ftbquests.quest.TeamData");taskType=Class.forName("dev.ftb.mods.ftbquests.quest.task.Task");file=((Optional<?>)Class.forName("dev.ftb.mods.ftbquests.quest.ServerQuestFile").getMethod("getInstance").invoke(null)).orElseThrow();}
        void start()throws Exception{
            var id=UUID.nameUUIDFromBytes("PremisesThirdStudent".getBytes(java.nio.charset.StandardCharsets.UTF_8));var old=server.getPlayerList().getPlayer(id);if(old!=null)server.getPlayerList().remove(old);library=ClaimRuntimeTests.vanillaPlayer(h.getLevel(),id,"ThirdStudent");other=ClaimRuntimeTests.vanillaPlayer(h.getLevel(),UUID.nameUUIDFromBytes("AlphaGuideUntrained".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"GuideUntrained");
            h.assertTrue(store.professions(id).track(Profession.LIBRARIAN).completedQuestIds().size()==24&&store.professions(id).track(Profession.LIBRARIAN).xp()==4800,"Source contains all24genuinely native earned Library proofs");
            com.mohymods.kingdomsmp.contribution.ContributionService.ledger(server);server.tickRateManager().setTickRate(20);var project=LibraryServiceRuntime.project(library);var blueprint=com.mohymods.kingdomsmp.building.BlueprintRegistry.get(project.blueprint(),1);var home=project.origin().offset(blueprint.rotate(new net.minecraft.core.BlockPos(7,1,6),project.rotation()));library.teleportTo(h.getLevel(),home.getX()+.5,home.getY()+1,home.getZ()+.5,0,0);other.teleportTo(h.getLevel(),home.getX()+1.5,home.getY()+1,home.getZ()+.5,0,0);library.setNoGravity(true);other.setNoGravity(true);library.setInvulnerable(true);other.setInvulnerable(true);h.getLevel().getChunkSource().move(library);h.getLevel().getChunkSource().move(other);revision=store.revision();restoring=Files.exists(proof);deadline=h.getTick()+3200;
            int seen=0;for(var goal:AlphaQuestCampaign.goals()){var task=task(goal);h.assertTrue(task!=null,"Native FTB loads every additive task "+goal.key());var tag=new CompoundTag();task.getClass().getMethod("writeData",CompoundTag.class,net.minecraft.core.HolderLookup.Provider.class).invoke(task,tag,server.registryAccess());h.assertTrue(tag.getCompound("icon").getString("id").equals(goal.icon()),"Native compound icon "+goal.key());seen++;}h.assertTrue(seen==166,"144quests +6premises +4kingdom +12future panels load");
            h.assertTrue(FtbQuestsIntegration.inspect(server).stream().anyMatch(s->s.contains("166/166 bound")),"Every exact rewardless ID binds in actual FTB runtime");h.onEachTick(this::tick);
        }
        void tick(){try{
            library.doTick();other.doTick();if(!ready()){if(h.getTick()>deadline)throw new IllegalStateException("Native alpha guide task timers failed: "+pending()+"; shared="+AuthoritativeQuestSnapshot.read(server));return;}
            h.assertTrue(store.revision()==revision&&store.professions(other.getUUID()).tracks().isEmpty(),"FTB cannot mutate authority or grant another reader's rank");CommerceService.verifyReceipt(library);CommerceService.verifyReceipt(other);
            if(!restoring&&!reset){var goal=AlphaQuestCampaign.goals().stream().filter(g->g.fact().equals("kingdom_smp:librarian/m08")).findFirst().orElseThrow();var change=Class.forName("dev.ftb.mods.ftbquests.util.ProgressChange");var request=change.getMethod("createServerSide",long.class,boolean.class,UUID.class,boolean.class).invoke(null,goal.questId(),true,library.getUUID(),false);var data=data(library);var teamId=(UUID)team.getMethod("getTeamId").invoke(data);change.getMethod("maybeForceProgress",UUID.class).invoke(request,teamId);h.assertTrue(progress(library,goal)==0,"Actual FTB reset clears only guide progress");reset=true;deadline=h.getTick()+1800;return;}
            file.getClass().getMethod("saveNow").invoke(file);if(!restoring){var saved=new CompoundTag();saved.putLong("revision",revision);AtomicNbt.write(proof,saved);}else h.assertTrue(AtomicNbt.read(proof).getLong("revision")==revision,"Canonical Root unchanged across FTB guide restart");com.mojang.logging.LogUtils.getLogger().info(restoring?"ALPHA_GUIDE_RESTART_PASSED: actual166native tasks/24earned Library tasks/future locks/independent untrained reader/private rank and Root unchanged across dedicated restart":"ALPHA_GUIDE_NATIVE_PASSED: actual166tasks/all144real profession IDs/native icons/exact binding/24earned Library auto checks/unearned refusal/future locks/reset-reprojection/no Kingdom XP-rank-funds-receipt mutations");h.succeed();
        }catch(Exception failure){com.mojang.logging.LogUtils.getLogger().error("Actual alpha guide failed",failure);h.fail(failure.toString());}}
        String pending()throws Exception{var rows=new ArrayList<String>();for(var g:AlphaQuestCampaign.goals())if(g.fact().startsWith("kingdom_smp:librarian/")&&progress(library,g)!=1)rows.add(g.key()+"="+progress(library,g));return rows.toString();}
        boolean ready()throws Exception{for(var goal:AlphaQuestCampaign.goals()){long mine=progress(library,goal),untrained=progress(other,goal);if(goal.kind()==AlphaQuestCampaign.Kind.PROFESSION&&goal.fact().startsWith("kingdom_smp:librarian/")){if(mine!=1)return false;h.assertTrue(untrained==0,"Separate reader cannot inherit earned Library proof");}if(goal.kind()==AlphaQuestCampaign.Kind.FUTURE)h.assertTrue(mine==0&&untrained==0,"Future information cannot complete");}return true;}
        Object task(AlphaQuestCampaign.Goal g)throws Exception{return file.getClass().getMethod("getTask",long.class).invoke(file,g.taskId());}
        Object data(ServerPlayer p)throws Exception{return team.getMethod("get",net.minecraft.world.entity.player.Player.class).invoke(null,p);}
        long progress(ServerPlayer p,AlphaQuestCampaign.Goal g)throws Exception{return (long)team.getMethod("getProgress",taskType).invoke(data(p),task(g));}
    }
}
