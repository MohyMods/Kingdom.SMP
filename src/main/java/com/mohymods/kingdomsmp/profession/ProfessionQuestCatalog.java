package com.mohymods.kingdomsmp.profession;

import java.util.*;

/** Approval-gated rank proofs, not generated quest content. The live default is disabled. */
public final class ProfessionQuestCatalog {
    public record Definition(String id, Profession profession, ProfessionRank rank, int ordinal, Set<String> prerequisites) {
        public Definition {
            Objects.requireNonNull(profession);Objects.requireNonNull(rank);
            if (!profession.questId(rank, ordinal).equals(id)) throw new IllegalArgumentException("Quest identity differs from its profession/rank slot");
            prerequisites = Set.copyOf(prerequisites);
            if (prerequisites.size() > 24 || prerequisites.contains(id)) throw new IllegalArgumentException("Invalid profession quest prerequisites");
            for (String prerequisite : prerequisites) if (!profession.ownsQuestId(prerequisite)) throw new IllegalArgumentException("Foreign profession prerequisite");
        }
        int position() { return rank.ordinal() * 8 + ordinal; }
    }
    private final Map<String, Definition> definitions;
    private final boolean enabled;
    private ProfessionQuestCatalog(Collection<Definition> source, boolean enabled) {
        if (source.size() > 288) throw new IllegalArgumentException("Too many profession quest definitions");
        Map<String, Definition> next = new HashMap<>();
        for (var definition : source) if (next.put(definition.id(), definition) != null) throw new IllegalArgumentException("Duplicate profession quest definition");
        for (var definition : next.values()) for (var prerequisite : definition.prerequisites()) {
            var prior = next.get(prerequisite);
            if (prior == null || prior.position() >= definition.position()) throw new IllegalArgumentException("Missing, cyclic or later profession quest prerequisite");
        }
        this.definitions = Map.copyOf(next);this.enabled = enabled;
    }
    public static ProfessionQuestCatalog disabled() { return new ProfessionQuestCatalog(List.of(), false); }
    /** Only server code which has separately reviewed/approved real objective definitions may call this. */
    public static ProfessionQuestCatalog approved(Collection<Definition> definitions) { return new ProfessionQuestCatalog(definitions, true); }
    public boolean enabled() { return enabled; }
    public Map<String, Definition> definitions() { return definitions; }
    public Optional<Definition> definition(String id) { return enabled ? Optional.ofNullable(definitions.get(id)) : Optional.empty(); }
    public boolean completedBand(Profession profession, ProfessionRank rank, Set<String> completed) {
        if (!enabled) return false;
        for (int ordinal = 1; ordinal <= 8; ordinal++) {
            var id = profession.questId(rank, ordinal);
            if (!definitions.containsKey(id) || !completed.contains(id)) return false;
        }
        return true;
    }
    public ProfessionRank rank(Profession profession, Set<String> completed) {
        if (!completedBand(profession, ProfessionRank.APPRENTICE, completed)) return ProfessionRank.APPRENTICE;
        return completedBand(profession, ProfessionRank.JOURNEYMAN, completed)
                && completedBand(profession, ProfessionRank.MASTER, completed) ? ProfessionRank.MASTER : ProfessionRank.JOURNEYMAN;
    }
    public boolean mayAttempt(Profession profession, String id, Set<String> completed) {
        var definition = definition(id).orElse(null);
        if (definition == null || definition.profession() != profession || !completed.containsAll(definition.prerequisites())) return false;
        return switch (definition.rank()) {
            case APPRENTICE -> true;
            case JOURNEYMAN -> completedBand(profession, ProfessionRank.APPRENTICE, completed);
            case MASTER -> completedBand(profession, ProfessionRank.APPRENTICE, completed)
                    && completedBand(profession, ProfessionRank.JOURNEYMAN, completed);
        };
    }
}
