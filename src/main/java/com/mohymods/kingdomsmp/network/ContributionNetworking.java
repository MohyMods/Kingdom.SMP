package com.mohymods.kingdomsmp.network;

import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.kingdom.TierOneProgress;
import com.mohymods.kingdomsmp.kingdom.TierOneProgress.Resource;
import com.mohymods.kingdomsmp.persistence.ContributionLedger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import java.util.*;
import java.util.function.Consumer;

public final class ContributionNetworking {
    private static Consumer<ContributionView> receiver;
    private static final Map<ServerPlayer, Long> VIEWERS = new WeakHashMap<>();
    private static final Map<ServerPlayer, Integer> PAGES = new WeakHashMap<>();
    private ContributionNetworking() { }
    public static void install(Consumer<ContributionView> client) { receiver = Objects.requireNonNull(client); }
    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(ContributionRequest.TYPE, ContributionRequest.STREAM_CODEC, ContributionNetworking::request);
        registrar.playToClient(ContributionView.TYPE, ContributionView.STREAM_CODEC, (p, c) -> { if (receiver != null) receiver.accept(p); });
    }
    private static void request(ContributionRequest request, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        long now = System.nanoTime(); Long last = VIEWERS.put(player, now);
        if (last != null && now - last < 100_000_000L) return;
        if (request.action() == 3) PAGES.put(player, request.amount());
        ContributionService.Offer offer = null;
        String notice = "";
        boolean changed = false;
        try {
            if (request.action() == 1) offer = ContributionService.quote(player, Resource.values()[request.resource()], request.amount());
            if (request.action() == 2) { notice = ContributionService.confirm(player, request.offer()); changed = true; }
        } catch (IllegalArgumentException | IllegalStateException error) {
            notice = error.getMessage() == null ? "Donation unavailable." : error.getMessage();
        }
        context.reply(view(player, request.requestId(), notice, offer));
        if (changed) {
            for (ServerPlayer viewer : List.copyOf(VIEWERS.keySet())) {
                if (viewer != player && viewer.server == player.server && !viewer.hasDisconnected()) PacketDistributor.sendToPlayer(viewer, view(viewer, 0, "Kingdom progress updated.", null));
            }
        }
    }
    public static ContributionView view(ServerPlayer player, long requestId, String notice, ContributionService.Offer offer) {
        return view(player, requestId, notice, offer, PAGES.getOrDefault(player, 0));
    }
    public static ContributionView view(ServerPlayer player, long requestId, String notice, ContributionService.Offer offer, int requestedPage) {
        ContributionLedger ledger = ContributionService.ledger(player.server);
        TierOneProgress progress = ledger.progress();
        var contributors = progress.contributors();
        List<UUID> sorted = contributors.keySet().stream().sorted(Comparator
                .<UUID>comparingInt(id -> contributors.get(id).scoreUnits()).reversed()
                .thenComparingLong(id -> contributors.get(id).lastPositiveSequence()).thenComparing(Comparator.naturalOrder())).toList();
        List<ContributionView.Row> leaders = sorted.stream().limit(10).map(id -> new ContributionView.Row(ContributionService.name(ledger, id),
                contributors.get(id).scoreUnits(), counts(contributors.get(id)))).toList();
        var accepted = ledger.history().stream().filter(d -> d.player().equals(player.getUUID())).toList();
        int pages = Math.max(1, (accepted.size()+7)/8);
        int page = Math.clamp(requestedPage, 0, pages-1);
        List<ContributionView.History> history = new ArrayList<>();
        for (int i = accepted.size()-1-page*8; i >= 0 && history.size() < 8; i--) {
            var d = accepted.get(i); history.add(new ContributionView.History(d.resource().ordinal(),d.amount(),d.time()));
        }
        boolean survival = player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL || player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE;
        if (notice.isBlank() && !survival) notice = "Switch to Survival or Adventure mode to donate.";
        if (ledger.quarantined() || ledger.pending().isPresent()) notice = "Donations paused for storage recovery. Contact the administrator.";
        return new ContributionView(requestId, progress.revision(), ContributionService.kingdom(player.server).attainedTier(),
                Arrays.stream(Resource.values()).map(progress::total).toList(), counts(contributors.get(player.getUUID())),
                Arrays.stream(Resource.values()).map(r -> ContributionService.eligible(player, r)).toList(), leaders,
                sorted.indexOf(player.getUUID()) + 1, history, page, pages, offer == null ? ContributionRequest.NONE : offer.id(), offer == null ? 0 : offer.resource().ordinal(),
                offer == null ? 0 : offer.amount(), notice.length() > 256 ? "Contribution unavailable; contact the administrator." : notice,
                survival && player.isAlive() && !ledger.quarantined() && ledger.pending().isEmpty() && !progress.complete(), progress.complete(),
                progress.winner().map(id -> ContributionService.name(ledger, id)).orElse(""),com.mohymods.kingdomsmp.building.ConstructionService.rewardTitle(player.server));
    }
    private static List<Integer> counts(TierOneProgress.Contributor player) {
        return Arrays.stream(Resource.values()).map(r -> player == null ? 0 : player.count(r)).toList();
    }
}
