package com.mohymods.kingdomsmp.network;

import com.mohymods.kingdomsmp.KingdomSmp;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Bounded server projection. Ranking and awards are never computed by clients. */
public record ContributionView(long requestId, long revision, int tier, List<Integer> totals, List<Integer> personal,
        List<Integer> eligible, List<Row> leaders, int rank, List<History> history, int historyPage, int historyPages, UUID offer, int offerResource,
        int offerCount, String notice, boolean donationsOpen, boolean complete, String winner, String rewardTitle) implements CustomPacketPayload {
    public record Row(String name, int score, List<Integer> counts) {
        public Row { if (name == null || name.length() > 48 || score < 0 || score > 8192) throw new IllegalArgumentException("Invalid contributor"); counts = boundedCounts(counts); }
    }
    public record History(int resource, int count, long time) {
        public History { if (resource < 0 || resource > 3 || count <= 0 || count > 2048 || time < 0) throw new IllegalArgumentException("Invalid donation history"); }
    }
    public ContributionView(long requestId,long revision,int tier,List<Integer> totals,List<Integer> personal,List<Integer> eligible,List<Row> leaders,int rank,List<History> history,int historyPage,int historyPages,UUID offer,int offerResource,int offerCount,String notice,boolean donationsOpen,boolean complete,String winner){this(requestId,revision,tier,totals,personal,eligible,leaders,rank,history,historyPage,historyPages,offer,offerResource,offerCount,notice,donationsOpen,complete,winner,"Founders' Lodge");}
    public ContributionView {
        if (rewardTitle == null || rewardTitle.length()>64 || requestId < 0 || revision < 0 || tier < 1 || tier > 12 || rank < 0 || rank > 3712 || leaders.size() > 10 || history.size() > 8
                || historyPages < 1 || historyPages > 464 || historyPage < 0 || historyPage >= historyPages
                || offer == null || offerResource < 0 || offerResource > 3 || offerCount < 0 || offerCount > 2048 || notice.length() > 256 || winner.length() > 48)
            throw new IllegalArgumentException("Invalid contribution view");
        totals = boundedCounts(totals); personal = boundedCounts(personal); eligible = List.copyOf(eligible);
        if (eligible.size() != 4 || eligible.stream().anyMatch(n -> n < 0 || n > 1_000_000)) throw new IllegalArgumentException("Invalid inventory view");
        leaders = List.copyOf(leaders); history = List.copyOf(history);
    }
    private static List<Integer> boundedCounts(List<Integer> counts) {
        counts = List.copyOf(counts); int[] max = {1024, 2048, 512, 128};
        if (counts.size() != 4) throw new IllegalArgumentException("Invalid resource view");
        for (int i = 0; i < 4; i++) if (counts.get(i) < 0 || counts.get(i) > max[i]) throw new IllegalArgumentException("Invalid resource counts");
        return counts;
    }
    public static final Type<ContributionView> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(KingdomSmp.MOD_ID, "contribution_view"));
    private static void counts(RegistryFriendlyByteBuf b, List<Integer> values) { values.forEach(b::writeVarInt); }
    private static List<Integer> counts(RegistryFriendlyByteBuf b) { return List.of(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt()); }
    public static final StreamCodec<RegistryFriendlyByteBuf, ContributionView> STREAM_CODEC = StreamCodec.of((b, p) -> {
        b.writeVarLong(p.requestId); b.writeVarLong(p.revision); b.writeVarInt(p.tier);
        counts(b, p.totals); counts(b, p.personal); counts(b, p.eligible);
        b.writeVarInt(p.leaders.size()); for (Row row : p.leaders) { b.writeUtf(row.name, 48); b.writeVarInt(row.score); counts(b, row.counts); }
        b.writeVarInt(p.rank); b.writeVarInt(p.history.size()); for (History row : p.history) { b.writeVarInt(row.resource); b.writeVarInt(row.count); b.writeVarLong(row.time); }
        b.writeVarInt(p.historyPage); b.writeVarInt(p.historyPages);
        b.writeUUID(p.offer); b.writeVarInt(p.offerResource); b.writeVarInt(p.offerCount); b.writeUtf(p.notice, 256);
        b.writeBoolean(p.donationsOpen); b.writeBoolean(p.complete); b.writeUtf(p.winner, 48); b.writeUtf(p.rewardTitle,64);
    }, b -> {
        long request = b.readVarLong(), revision = b.readVarLong(); int tier = b.readVarInt();
        var totals = counts(b); var personal = counts(b); var eligible = counts(b);
        int size = b.readVarInt(); if (size < 0 || size > 10) throw new IllegalArgumentException("Oversized leaderboard");
        List<Row> leaders = new ArrayList<>(); for (int i = 0; i < size; i++) leaders.add(new Row(b.readUtf(48), b.readVarInt(), counts(b)));
        int rank = b.readVarInt(), historySize = b.readVarInt(); if (historySize < 0 || historySize > 8) throw new IllegalArgumentException("Oversized history");
        List<History> history = new ArrayList<>(); for (int i = 0; i < historySize; i++) history.add(new History(b.readVarInt(), b.readVarInt(), b.readVarLong()));
        return new ContributionView(request, revision, tier, totals, personal, eligible, leaders, rank, history, b.readVarInt(), b.readVarInt(),
                b.readUUID(), b.readVarInt(), b.readVarInt(), b.readUtf(256), b.readBoolean(), b.readBoolean(), b.readUtf(48),b.readUtf(64));
    });
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
