package com.mohymods.kingdomsmp.economy;
import java.util.*;
/** Canonical journal projection; completion and funded native deliveries cannot diverge. */
public final class CommunityContractState {
 public record Key(String project,long week){ }
 public record Contribution(int total,int master){ }
 public record Project(Key key,String item,int goal,int unitReward,long budget,int delivered,Map<UUID,Contribution> contributors){
  public Project{contributors=Map.copyOf(contributors);}
  public boolean complete(){return delivered==goal;}
  public boolean masterProof(UUID player){var proof=contributors.get(player);return key.project().startsWith("kingdom_smp:community/")&&complete()&&proof!=null&&proof.master()>=Math.max(1,(goal+9)/10);}
 }
 final Map<Key,Project> projects=new HashMap<>();
 CommunityContractState copy(){var next=new CommunityContractState();next.projects.putAll(projects);return next;}
 public Optional<Project> project(String project,long week){return Optional.ofNullable(projects.get(new Key(project,week)));}
 public long spent(long week){return spent(week,false);}
 public long fishingSpent(long week){return spent(week,true);}
 private long spent(long week,boolean fishing){long total=0;for(var p:projects.values())if(p.key().week()==week&&p.key().project().startsWith("kingdom_smp:fishing_contract/")==fishing)total=Math.addExact(total,Math.multiplyExact((long)p.delivered(),p.unitReward()));return total;}
 public long masterProofs(UUID player){return projects.values().stream().filter(p->p.masterProof(player)).count();}
}
