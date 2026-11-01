package com.mohymods.kingdomsmp.profession;

import net.minecraft.nbt.*;
import java.util.*;

/** Immutable canonical-operation payload; this class performs no persistence, payment or client authorization. */
public final class ProfessionRoster {
    public static final int MAX_ACTIVE = 3;
    public static final long SWITCH_FEE = 15_000, MAX_XP = 1_000_000_000L;
    public record Track(long xp, Set<String> completedQuestIds) {
        public Track {
            if (xp < 0 || xp > MAX_XP) throw new IllegalArgumentException("Profession XP outside bounded state");
            completedQuestIds = Set.copyOf(completedQuestIds);
            if (completedQuestIds.size() > 24) throw new IllegalArgumentException("Too many completed profession quests");
        }
    }
    public record Transition(ProfessionRoster roster, long fee, boolean changed) {
        public Transition {
            Objects.requireNonNull(roster);
            if (fee != 0 && fee != SWITCH_FEE || !changed && fee != 0) throw new IllegalArgumentException("Invalid profession transition charge");
        }
    }
    private final Map<Profession, Track> tracks;
    private final Set<Profession> active;
    private ProfessionRoster(Map<Profession, Track> tracks, Set<Profession> active) {
        this.tracks = Map.copyOf(tracks);this.active = Set.copyOf(active);
        if (tracks.size() > 12 || active.size() > MAX_ACTIVE || !tracks.keySet().containsAll(active)) throw new IllegalArgumentException("Invalid profession roster");
        for (var entry : tracks.entrySet()) for (var quest : entry.getValue().completedQuestIds()) {
            if (!entry.getKey().ownsQuestId(quest)) throw new IllegalArgumentException("Unknown or foreign profession quest identity");
        }
    }
    public static ProfessionRoster empty() { return new ProfessionRoster(Map.of(), Set.of()); }
    public Map<Profession, Track> tracks() { return tracks; }
    public Set<Profession> active() { return active; }
    public Track track(Profession profession) { return tracks.getOrDefault(Objects.requireNonNull(profession), new Track(0, Set.of())); }
    public boolean isActive(Profession profession) { return active.contains(Objects.requireNonNull(profession)); }
    public ProfessionRank rank(Profession profession, ProfessionQuestCatalog catalog) { return catalog.rank(profession, track(profession).completedQuestIds()); }
    public boolean perksEligible(Profession profession, ProfessionRank minimum, ProfessionQuestCatalog catalog) {
        return isActive(profession) && rank(profession, catalog).ordinal() >= Objects.requireNonNull(minimum).ordinal();
    }
    public Transition enroll(Profession profession) {
        Objects.requireNonNull(profession);
        if (isActive(profession)) return new Transition(this, 0, false);
        if (active.size() >= MAX_ACTIVE) throw new IllegalStateException("Three professions are already active; review a paid switch");
        long fee = tracks.containsKey(profession) || tracks.size() >= MAX_ACTIVE ? SWITCH_FEE : 0;
        var nextTracks = new HashMap<>(tracks);nextTracks.putIfAbsent(profession, new Track(0, Set.of()));
        var nextActive = new HashSet<>(active);nextActive.add(profession);
        return new Transition(new ProfessionRoster(nextTracks, nextActive), fee, true);
    }
    public Transition switchProfession(Profession from, Profession to) {
        Objects.requireNonNull(from);Objects.requireNonNull(to);
        if (!isActive(from)) throw new IllegalStateException("The replaced profession is not active");
        if (from == to) return new Transition(this, 0, false);
        if (isActive(to)) throw new IllegalStateException("The requested profession is already active");
        var nextTracks = new HashMap<>(tracks);nextTracks.putIfAbsent(to, new Track(0, Set.of()));
        var nextActive = new HashSet<>(active);nextActive.remove(from);nextActive.add(to);
        return new Transition(new ProfessionRoster(nextTracks, nextActive), SWITCH_FEE, true);
    }
    /** Administrative transition only. Ordinary UI must use reviewed switching; reactivation still costs the switch fee. */
    public Transition deactivate(Profession profession) {
        Objects.requireNonNull(profession);
        if (!isActive(profession)) return new Transition(this, 0, false);
        var nextActive = new HashSet<>(active);nextActive.remove(profession);
        return new Transition(new ProfessionRoster(tracks, nextActive), 0, true);
    }
    /** Caller verifies a real server objective and canonically couples this result to any economic reward. */
    public ProfessionRoster completeQuest(Profession profession, String id, long xpReward, ProfessionQuestCatalog catalog) {
        Objects.requireNonNull(profession);Objects.requireNonNull(catalog);
        if (xpReward < 0 || xpReward > MAX_XP) throw new IllegalArgumentException("Invalid profession quest XP award");
        if (!profession.ownsQuestId(id)) throw new IllegalArgumentException("Unknown or foreign profession quest identity");
        if (!isActive(profession)) throw new IllegalStateException("Inactive professions cannot receive quest credit");
        var track = track(profession);
        if (track.completedQuestIds().contains(id)) return this;
        if (!catalog.mayAttempt(profession, id, track.completedQuestIds())) throw new IllegalStateException("Quest is not approved, available or prerequisite-complete");
        long xp = Math.addExact(track.xp(), xpReward);
        var completed = new HashSet<>(track.completedQuestIds());completed.add(id);
        var next = new HashMap<>(tracks);next.put(profession, new Track(xp, completed));
        return new ProfessionRoster(next, active);
    }
    public CompoundTag save() {
        var data = new CompoundTag();data.putInt("schema", 1);var entries = new ListTag();
        for (var profession : Profession.values()) {
            var track = tracks.get(profession);if (track == null) continue;
            var row = new CompoundTag();row.putString("id", profession.id());row.putBoolean("active", active.contains(profession));row.putLong("xp", track.xp());
            var quests = new ListTag();track.completedQuestIds().stream().sorted().forEach(id -> quests.add(StringTag.valueOf(id)));
            row.put("quests", quests);entries.add(row);
        }
        data.put("tracks", entries);return data;
    }
    public static ProfessionRoster load(CompoundTag data) {
        Objects.requireNonNull(data);
        if (!data.getAllKeys().equals(Set.of("schema", "tracks")) || !data.contains("schema", Tag.TAG_INT) || data.getInt("schema") != 1 || !data.contains("tracks", Tag.TAG_LIST)) throw new IllegalArgumentException("Invalid profession state schema");
        var rows = data.getList("tracks", Tag.TAG_COMPOUND);
        if (rows.size() != ((ListTag) data.get("tracks")).size() || rows.size() > 12) throw new IllegalArgumentException("Invalid profession track list");
        Map<Profession, Track> tracks = new HashMap<>();Set<Profession> active = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.getCompound(i);
            if (!row.getAllKeys().equals(Set.of("id", "active", "xp", "quests")) || !row.contains("id", Tag.TAG_STRING) || !row.contains("active", Tag.TAG_BYTE) || row.getByte("active") < 0 || row.getByte("active") > 1 || !row.contains("xp", Tag.TAG_LONG) || !row.contains("quests", Tag.TAG_LIST)) throw new IllegalArgumentException("Malformed profession track");
            var profession = Profession.byId(row.getString("id"));var quests = row.getList("quests", Tag.TAG_STRING);
            if (quests.size() > 24 || quests.size() != ((ListTag) row.get("quests")).size()) throw new IllegalArgumentException("Invalid profession quest list");
            Set<String> completed = new HashSet<>();for (int j = 0; j < quests.size(); j++) if (!completed.add(quests.getString(j))) throw new IllegalArgumentException("Duplicate completed profession quest");
            if (tracks.put(profession, new Track(row.getLong("xp"), completed)) != null) throw new IllegalArgumentException("Duplicate profession track");
            if (row.getBoolean("active")) active.add(profession);
        }
        return new ProfessionRoster(tracks, active);
    }
    @Override public boolean equals(Object other) { return other instanceof ProfessionRoster roster && tracks.equals(roster.tracks) && active.equals(roster.active); }
    @Override public int hashCode() { return Objects.hash(tracks, active); }
}
