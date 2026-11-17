package com.mohymods.kingdomsmp.milestone;

import java.util.*;
import java.util.regex.Pattern;

/** Immutable server-data definitions. Approval labels never authorize resource consumption. */
public record MilestoneCatalog(List<Capability> capabilities, List<Milestone> milestones) {
    private static final Pattern ID=Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");
    public MilestoneCatalog {
        capabilities=List.copyOf(capabilities);milestones=List.copyOf(milestones);
        if(capabilities.size()>128||milestones.size()!=12)throw new IllegalArgumentException("Expected twelve milestones and at most 128 capabilities");
        unique(capabilities.stream().map(Capability::id).toList());unique(milestones.stream().map(Milestone::id).toList());
        for(int tier=1;tier<=12;tier++){int t=tier;if(milestones.stream().filter(m->m.tier()==t).count()!=1)throw new IllegalArgumentException("Exactly one milestone per tier required");}
    }
    public Milestone tier(int tier){return milestones.stream().filter(m->m.tier()==tier).findFirst().orElseThrow();}
    public record Capability(String id,int availableFromTier,List<String> requires,List<String> afterMilestones){
        public Capability {id=identifier(id);if(availableFromTier<0||availableFromTier>12)throw new IllegalArgumentException("Invalid capability tier");requires=references(requires);afterMilestones=references(afterMilestones);}
    }
    public record Requirement(String id,String ingredient,long quantity,String category,List<String> requires,String displayName,String description){
        public Requirement {
            id=identifier(id);Objects.requireNonNull(ingredient);identifier(ingredient.startsWith("#")?ingredient.substring(1):ingredient);
            if(quantity<1||quantity>1_000_000_000L)throw new IllegalArgumentException("Invalid resource quantity");
            category=identifier(category);requires=references(requires);displayName=display(displayName,96);description=display(description,256);
        }
    }
    public record Objective(String id,String kind,String target,List<String> requires,String description){
        public Objective {id=identifier(id);if(!Set.of("building","admin","exploration","profession").contains(kind))throw new IllegalArgumentException("Unknown objective kind");target=identifier(target);requires=references(requires);description=display(description,256);}
    }
    public record Milestone(String id,int tier,int nextTier,String approval,String title,String scoringPolicy,
                            List<Requirement> requirements,List<Objective> objectives,String reward){
        public Milestone {
            id=identifier(id);if(tier<1||tier>12||nextTier!=Math.min(tier+1,12))throw new IllegalArgumentException("Progression must be sequential; Tier 12 has no Tier 13");
            if(!Set.of("approved","proposal").contains(approval))throw new IllegalArgumentException("Unknown approval label");
            title=display(title,96);if(!Set.of("legacy_equal_four","pending_review").contains(scoringPolicy))throw new IllegalArgumentException("Unknown scoring policy");
            requirements=List.copyOf(requirements);objectives=List.copyOf(objectives);reward=identifier(reward);
            if(requirements.isEmpty()||requirements.size()>16||objectives.size()>8)throw new IllegalArgumentException("Unbounded or empty milestone");
            unique(requirements.stream().map(Requirement::id).toList());unique(objectives.stream().map(Objective::id).toList());
            unique(requirements.stream().map(Requirement::ingredient).toList());
        }
    }
    public static String identifier(String s){if(s==null||s.length()>128||!ID.matcher(s).matches())throw new IllegalArgumentException("Invalid namespaced identifier");return s;}
    private static String display(String s,int max){if(s==null||s.length()>max||s.codePoints().anyMatch(c->Character.isISOControl(c)||c==0x00a7))throw new IllegalArgumentException("Invalid display information");return s;}
    private static List<String> references(List<String> ids){if(ids.size()>32)throw new IllegalArgumentException("Too many dependencies");ids=ids.stream().map(MilestoneCatalog::identifier).toList();unique(ids);return ids;}
    private static void unique(List<String> ids){if(new HashSet<>(ids).size()!=ids.size())throw new IllegalArgumentException("Duplicate identifier or ingredient");}
}
