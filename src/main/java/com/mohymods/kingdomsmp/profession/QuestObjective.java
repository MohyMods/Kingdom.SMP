package com.mohymods.kingdomsmp.profession;

import java.util.*;
import java.util.function.ToLongFunction;

/**
 * Shared read-only objective evaluation; native evidence and canonical rewards stay authoritative.
 */
public record QuestObjective(Kind kind, List<Criterion> criteria) {
  public enum Kind {
    EVENT,
    CONSUMED_DELIVERY,
    INSPECTION
  }

  public enum Aggregation {
    SUM,
    EACH
  }

  public record Criterion(Aggregation aggregation, List<String> metrics, String goal) {
    public Criterion {
      Objects.requireNonNull(aggregation);
      metrics = List.copyOf(metrics);
      if (metrics.isEmpty()
          || metrics.size() > 32
          || new HashSet<>(metrics).size() != metrics.size())
        throw new IllegalArgumentException("Invalid objective metric set");
      for (var metric : metrics)
        if (metric == null || !metric.matches("[a-z0-9_./:-]{1,96}"))
          throw new IllegalArgumentException("Invalid objective metric");
      if (goal == null || !goal.matches("[a-z0-9_]{1,48}"))
        throw new IllegalArgumentException("Invalid objective goal");
    }

    public boolean ready(ToLongFunction<String> facts, ToLongFunction<String> goals) {
      long target = goals.applyAsLong(goal);
      if (target < 1 || target > 1_000_000_000L)
        throw new IllegalArgumentException("Objective target outside bounded policy");
      long sum = 0;
      boolean each = true;
      for (var metric : metrics) {
        long amount = facts.applyAsLong(metric);
        if (amount < 0)
          throw new IllegalStateException("Negative or unavailable authoritative quest evidence");
        each &= amount >= target;
        sum = Math.min(target, sum + Math.min(target, amount));
      }
      return aggregation == Aggregation.EACH ? each : sum >= target;
    }
  }

  public QuestObjective {
    Objects.requireNonNull(kind);
    criteria = List.copyOf(criteria);
    if (criteria.size() > 16
        || kind == Kind.EVENT && criteria.isEmpty()
        || kind != Kind.EVENT && !criteria.isEmpty())
      throw new IllegalArgumentException(
          "Event objectives need evidence; manual delivery/inspection is never auto-granted");
  }

  public boolean ready(ToLongFunction<String> facts, ToLongFunction<String> goals) {
    Objects.requireNonNull(facts);
    Objects.requireNonNull(goals);
    return kind == Kind.EVENT && criteria.stream().allMatch(c -> c.ready(facts, goals));
  }

  public static Criterion sum(String goal, String... metrics) {
    return new Criterion(Aggregation.SUM, List.of(metrics), goal);
  }

  public static Criterion each(String goal, String... metrics) {
    return new Criterion(Aggregation.EACH, List.of(metrics), goal);
  }

  public static QuestObjective event(Criterion... criteria) {
    return new QuestObjective(Kind.EVENT, List.of(criteria));
  }

  public static QuestObjective manual(Kind kind) {
    return new QuestObjective(kind, List.of());
  }
}
