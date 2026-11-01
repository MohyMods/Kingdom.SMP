package com.mohymods.kingdomsmp.economy;
import java.util.*;
/** Canonical journal projection; completion and funded native deliveries cannot diverge. */
public final class CommunityContractState {
 public record Key(String project,long week){ }
 public record Contribution(int total,int master){ }
 public record Project(Key key,String item,int goal,int unitReward,long budget,int delivered,Map<UUID,Contribution> contributors){
  public Project{contributors=Map.copyOf(contributors);}
  public boolean complete(){return delivered==goal;}
  public boolean masterProof(UUID player){var proof=contributors.get(player);return complete()&&proof!=null&&proof.master()>=Math.max(1,(goal+9)/10);}
 }
 final Map<Key,Project> projects=new HashMap<>();final Map<Long,Long> spent=new HashMap<>();
 CommunityContractState copy(){var next=new CommunityContractState();next.projects.putAll(projects);next.spent.putAll(spent);return next;}
 public Optional<Project> project(String project,long week){return Optional.ofNullable(projects.get(new Key(project,week)));}
 public long spent(long week){return spent.getOrDefault(week,0L);}
 public long masterProofs(UUID player){return projects.values().stream().filter(p->p.masterProof(player)).count();}
}
