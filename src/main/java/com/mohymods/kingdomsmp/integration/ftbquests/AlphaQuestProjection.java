package com.mohymods.kingdomsmp.integration.ftbquests;

import com.mohymods.kingdomsmp.economy.CommerceService;
import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbquests.events.CustomTaskEvent;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.task.CustomTask;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import java.util.*;

/** Read-only personal source → explicitly shared team guide. No Kingdom award/write entry point. */
final class AlphaQuestProjection {
    private static final Logger LOG=com.mojang.logging.LogUtils.getLogger();
    private static final Map<MinecraftServer,Long> LAST_ERROR=new WeakHashMap<>();
    private record Cached(int tick,Optional<KingdomQuestSnapshot> snapshot) { }
    private static final Map<MinecraftServer,Cached> CACHE=new WeakHashMap<>();
    private AlphaQuestProjection() { }
    static Optional<AlphaQuestCampaign.Goal> binding(CustomTask task){
        if(!task.getQuestFile().isServerSide())return Optional.empty();
        var tags=task.getTags().stream().filter(t->t.startsWith(AlphaQuestCampaign.TAG_PREFIX)).toList();
        if(tags.size()!=1||task.getTags().stream().anyMatch(t->t.startsWith(KingdomQuestGoal.TAG_PREFIX)))return Optional.empty();
        return AlphaQuestCampaign.parse(tags.getFirst()).filter(g->g.taskId()==task.id&&g.questId()==task.getQuest().id&&g.chapterId()==task.getQuestChapter().id&&task.getQuest().getTasks().size()==1&&task.getQuest().getRewards().isEmpty());
    }
    static void register(FtbQuestsIntegration.SnapshotProvider provider){
        CustomTaskEvent.EVENT.register(event->{var task=event.getTask();if(task.getTags().stream().noneMatch(t->t.startsWith(AlphaQuestCampaign.TAG_PREFIX)))return EventResult.pass();task.setEnableButton(false);var goal=binding(task);if(goal.isEmpty()){task.setCheck(null);LOG.warn("Refusing altered or reward-bearing alpha guide task {}",task.getCodeString());return EventResult.pass();}
            task.setMaxProgress(1);task.setCheckTimer(100);task.setCheck((data,player)->{
                if(player==null||player.server==null||!player.server.isSameThread()||!binding(task).equals(goal))return;
                try {var snapshot=snapshot(provider,player.server);var store=CommerceService.initializedStore(player.server).orElse(null);if(snapshot.isEmpty()||store==null||store.quarantined())return;var g=goal.orElseThrow();Set<String> earned=Set.of();if(g.kind()==AlphaQuestCampaign.Kind.PROFESSION){var slot=com.mohymods.kingdomsmp.profession.ProfessionQuestService.catalog().definition(g.fact()).orElseThrow();earned=store.professions(player.getUUID()).track(slot.profession()).completedQuestIds();}if(g.complete(snapshot.orElseThrow(),earned)&&data.getProgress()!=1)data.setProgress(1);
                } catch(RuntimeException failure){long now=System.nanoTime();Long last=LAST_ERROR.get(player.server);if(last==null||now-last>=60_000_000_000L){LAST_ERROR.put(player.server,now);LOG.warn("Alpha guide paused; authoritative records unavailable",failure);}}
            });return EventResult.pass();});
    }
    private static Optional<KingdomQuestSnapshot> snapshot(FtbQuestsIntegration.SnapshotProvider provider,MinecraftServer server){int tick=server.getTickCount();var cached=CACHE.get(server);if(cached!=null&&tick>=cached.tick()&&tick-cached.tick()<20)return cached.snapshot();var value=Objects.requireNonNull(provider.snapshot(server));CACHE.put(server,new Cached(tick,value));return value;}
    static List<String> inspect(MinecraftServer server){var file=ServerQuestFile.getInstance();if(file.isEmpty()||file.orElseThrow().server!=server)return List.of("Alpha guide: FTB file unavailable");int bound=0;for(var g:AlphaQuestCampaign.goals()){var task=file.orElseThrow().getTask(g.taskId());if(task instanceof CustomTask custom&&binding(custom).filter(g::equals).isPresent())bound++;}return List.of("Alpha guide: "+bound+"/"+AlphaQuestCampaign.goals().size()+" bound, including144 earned profession projections; future entries cannot complete. Team guide only; personal Kingdom ranks/rewards remain authoritative.");}
}
