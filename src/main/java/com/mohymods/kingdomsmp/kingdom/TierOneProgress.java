package com.mohymods.kingdomsmp.kingdom;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Pure, immutable Tier 1 contribution accounting. It plans accepted quantities;
 * it neither removes inventory items nor grants rewards or advances world tier.
 * A server adapter must atomically reconcile an accepted plan with actual item
 * consumption before installing it as authoritative progress.
 */
public record TierOneProgress(long revision, Map<UUID, Contributor> contributors, Optional<UUID> winner) {
    /** Every resource is worth 2048 score units when its entire requirement is met. */
    public enum Resource {
        LOGS(1024, 2), COBBLESTONE(2048, 1), WHEAT(512, 4), IRON_INGOTS(128, 16);

        private final int required;
        private final int scoreWeight;

        Resource(int required, int scoreWeight) {
            this.required = required;
            this.scoreWeight = scoreWeight;
        }

        public int required() { return required; }
        public int scoreWeight() { return scoreWeight; }
    }

    /** Last positive sequence is when this contributor first reached their current score. */
    public record Contributor(int logs, int cobblestone, int wheat, int ironIngots, long lastPositiveSequence) {
        public Contributor {
            validateCount(logs, Resource.LOGS);
            validateCount(cobblestone, Resource.COBBLESTONE);
            validateCount(wheat, Resource.WHEAT);
            validateCount(ironIngots, Resource.IRON_INGOTS);
            if (logs + cobblestone + wheat + ironIngots == 0 || lastPositiveSequence <= 0) {
                throw new IllegalArgumentException("Contributors require positive accepted items and sequence");
            }
        }

        public int count(Resource resource) {
            return switch (resource) {
                case LOGS -> logs;
                case COBBLESTONE -> cobblestone;
                case WHEAT -> wheat;
                case IRON_INGOTS -> ironIngots;
            };
        }

        public int scoreUnits() {
            return logs * 2 + cobblestone + wheat * 4 + ironIngots * 16;
        }

        private Contributor plus(Resource resource, int accepted, long sequence) {
            return new Contributor(logs + (resource == Resource.LOGS ? accepted : 0),
                    cobblestone + (resource == Resource.COBBLESTONE ? accepted : 0),
                    wheat + (resource == Resource.WHEAT ? accepted : 0),
                    ironIngots + (resource == Resource.IRON_INGOTS ? accepted : 0), sequence);
        }
    }

    public record ContributionResult(int acceptedCount, TierOneProgress progress, boolean completedNow) {
        public ContributionResult {
            Objects.requireNonNull(progress, "progress");
            if (acceptedCount < 0 || (completedNow && !progress.complete())) {
                throw new IllegalArgumentException("Invalid contribution outcome");
            }
        }
    }

    public TierOneProgress {
        if (revision < 0) throw new IllegalArgumentException("Revision must not be negative");
        contributors = Map.copyOf(contributors);
        Objects.requireNonNull(winner, "winner");
        for (Contributor contributor : contributors.values()) {
            if (contributor.lastPositiveSequence() > revision) {
                throw new IllegalArgumentException("Contribution sequence is newer than progress revision");
            }
        }
        for (Resource resource : Resource.values()) {
            if (total(contributors, resource) > resource.required()) {
                throw new IllegalArgumentException("Contributions exceed " + resource + " requirement");
            }
        }
        boolean complete = complete(contributors);
        if (complete != winner.isPresent()) {
            throw new IllegalArgumentException("Winner must be recorded exactly when requirements complete");
        }
        if (complete && !winner.equals(leader(contributors))) {
            throw new IllegalArgumentException("Winner does not match weighted contribution ranking");
        }
    }

    public static TierOneProgress initial() {
        return new TierOneProgress(0, Map.of(), Optional.empty());
    }

    /**
     * Accepts at most the outstanding requirement; excess items must remain with
     * the player. Zero and already-filled requirements do not change state.
     * Negative quantities reject rather than reversing progress. Long.MAX_VALUE
     * is safe because only the bounded outstanding quantity is ever converted.
     */
    public ContributionResult contribute(UUID player, Resource resource, long requested) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(resource, "resource");
        if (requested < 0) throw new IllegalArgumentException("Requested quantity must not be negative");
        int accepted = (int) Math.min(requested, resource.required() - total(resource));
        if (accepted == 0) return new ContributionResult(0, this, false);
        if (revision == Long.MAX_VALUE) throw new IllegalStateException("Progress revision exhausted");
        long nextRevision = revision + 1;
        Map<UUID, Contributor> next = new HashMap<>(contributors);
        Contributor existing = next.get(player);
        if (existing == null) {
            existing = new Contributor(resource == Resource.LOGS ? accepted : 0,
                    resource == Resource.COBBLESTONE ? accepted : 0,
                    resource == Resource.WHEAT ? accepted : 0,
                    resource == Resource.IRON_INGOTS ? accepted : 0, nextRevision);
        } else {
            existing = existing.plus(resource, accepted, nextRevision);
        }
        next.put(player, existing);
        boolean completedNow = complete(next);
        Optional<UUID> nextWinner = completedNow ? leader(next) : Optional.empty();
        TierOneProgress progress = new TierOneProgress(nextRevision, next, nextWinner);
        return new ContributionResult(accepted, progress, completedNow);
    }

    public int total(Resource resource) {
        Objects.requireNonNull(resource, "resource");
        return total(contributors, resource);
    }

    public boolean complete() { return complete(contributors); }

    public Optional<UUID> leader() { return leader(contributors); }

    private static int total(Map<UUID, Contributor> contributors, Resource resource) {
        // Bound the accumulation even when validating hostile deserialized input.
        long sum = 0;
        for (Contributor contributor : contributors.values()) {
            sum += contributor.count(resource);
            if (sum > resource.required()) return resource.required() + 1;
        }
        return (int) sum;
    }

    private static boolean complete(Map<UUID, Contributor> contributors) {
        for (Resource resource : Resource.values()) {
            if (total(contributors, resource) != resource.required()) return false;
        }
        return true;
    }

    private static Optional<UUID> leader(Map<UUID, Contributor> contributors) {
        return contributors.entrySet().stream().min(Comparator
                .<Map.Entry<UUID, Contributor>>comparingInt(entry -> entry.getValue().scoreUnits()).reversed()
                .thenComparingLong(entry -> entry.getValue().lastPositiveSequence())
                .thenComparing(Map.Entry::getKey)).map(Map.Entry::getKey);
    }

    private static void validateCount(int count, Resource resource) {
        if (count < 0 || count > resource.required()) {
            throw new IllegalArgumentException("Invalid contribution count for " + resource);
        }
    }
}
