package com.mohymods.kingdomsmp.integration.ftbquests;

import com.mojang.logging.LogUtils;
import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbquests.events.CustomTaskEvent;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.task.CustomTask;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** Links only the exact, rewardless Kingdom custom tasks. Never writes Kingdom records or grants items. */
final class FtbQuestsProjection {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<MinecraftServer, Cached> CACHE = new WeakHashMap<>();
    private static final Map<MinecraftServer, Long> LAST_ERROR = new WeakHashMap<>();
    private static FtbQuestsIntegration.SnapshotProvider provider;
    private record Cached(int tick, Optional<KingdomQuestSnapshot> snapshot) { }
    private FtbQuestsProjection() { }

    static void register(FtbQuestsIntegration.SnapshotProvider snapshots) {
        provider = snapshots;
        CustomTaskEvent.EVENT.register(event -> {
            bind(event.getTask());
            return EventResult.pass();
        });
    }

    private static Optional<KingdomQuestGoal> binding(CustomTask task) {
        if (!task.getQuestFile().isServerSide()) return Optional.empty();
        List<String> projected = task.getTags().stream().filter(t -> t.startsWith(KingdomQuestGoal.TAG_PREFIX)).toList();
        if (projected.size() != 1) return Optional.empty();
        return KingdomQuestGoal.parseTag(projected.getFirst()).filter(goal -> goal.taskId() == task.id
                && goal.questId() == task.getQuest().id
                && task.getQuestChapter().id == KingdomQuestGoal.CHAPTER_ID
                && task.getQuest().getRewards().isEmpty()
                && task.getQuest().getTasks().size() == 1);
    }

    private static void bind(CustomTask task) {
        boolean ours = task.getTags().stream().anyMatch(t -> t.startsWith(KingdomQuestGoal.TAG_PREFIX));
        if (!ours) return;
        var goal = binding(task);
        task.setEnableButton(false);
        if (goal.isEmpty()) {
            task.setCheck(null);
            LOGGER.warn("Refusing unknown, altered or reward-bearing Kingdom FTB projection task {}", task.getCodeString());
            return;
        }
        task.setMaxProgress(1);
        task.setCheckTimer(100);
        task.setCheck((data, player) -> {
            if (player == null || player.server == null || !player.server.isSameThread()) return;
            // Check binding again: edits/reload must not turn this callback into a reward path.
            if (!binding(task).equals(goal)) return;
            snapshot(player.server).ifPresent(current -> {
                if (goal.orElseThrow().complete(current) && data.getProgress() != 1) data.setProgress(1);
            });
        });
        LOGGER.debug("Bound Kingdom FTB projection {} to {}", task.getCodeString(), goal.orElseThrow());
    }

    private static Optional<KingdomQuestSnapshot> snapshot(MinecraftServer server) {
        int tick = server.getTickCount();
        var cached = CACHE.get(server);
        if (cached != null && tick >= cached.tick() && tick - cached.tick() < 20) return cached.snapshot();
        try {
            Optional<KingdomQuestSnapshot> value = provider.snapshot(server);
            if (value == null) throw new IllegalStateException("Null Kingdom projection snapshot");
            CACHE.put(server, new Cached(tick, value)); return value;
        } catch (RuntimeException failure) {
            long now = System.nanoTime(); Long logged = LAST_ERROR.get(server);
            if (logged == null || now - logged >= 60_000_000_000L) {
                LAST_ERROR.put(server, now); LOGGER.warn("Kingdom FTB projection paused; authoritative snapshot unavailable", failure);
            }
            CACHE.put(server, new Cached(tick, Optional.empty())); return Optional.empty();
        }
    }

    static List<String> inspect(MinecraftServer server, String status) {
        var result = new ArrayList<String>(); result.add(status);
        var file = ServerQuestFile.getInstance();
        if (file.isEmpty() || file.orElseThrow().server != server) {
            result.add("FTB server quest file is not ready for this world"); return List.copyOf(result);
        }
        var state = snapshot(server);
        for (var goal : KingdomQuestGoal.values()) {
            var task = file.orElseThrow().getTask(goal.taskId());
            boolean bound = task instanceof CustomTask custom && binding(custom).filter(goal::equals).isPresent();
            result.add(goal.name() + " | task " + goal.code() + " | " + (bound ? "bound" : "missing/altered")
                    + " | authoritative " + state.map(goal::complete).map(String::valueOf).orElse("unavailable"));
        }
        result.add("Shared guide only: no Kingdom rewards, inventory, currencies, profession ranks or tier changes");
        return List.copyOf(result);
    }
}
