package com.mohymods.kingdomsmp.economy;
import java.util.*;
import com.mohymods.kingdomsmp.profession.BuilderWorksite;
/** Replayed projection only; no second wallet, persistence file, or client-supplied balance. */
final class ContractState {
    record Slot(UUID player,String profession,String cycle,long period) { }
    record Pool(String cycle,long period) { }
    record QuestPeriod(UUID player,String profession,String band,String cycle,long period) { }
    record WorksiteSlot(UUID player,String quest) { }
    final Map<UUID,BuilderWorksite> workSites=new HashMap<>();
    final Map<WorksiteSlot,UUID> currentSites=new HashMap<>();
    final Map<UUID,HuntState> hunts=new HashMap<>();
    final Map<Long,Long> huntBudgets=new HashMap<>();
    final Map<Long,Long> huntSpent=new HashMap<>();
    final Set<QuestPeriod> professionPeriods=new HashSet<>();
    final Set<Slot> completed=new HashSet<>();final Map<Pool,Long> spent=new HashMap<>();
    ContractState copy(){var next=new ContractState();next.workSites.putAll(workSites);next.currentSites.putAll(currentSites);next.hunts.putAll(hunts);next.huntBudgets.putAll(huntBudgets);next.huntSpent.putAll(huntSpent);next.completed.addAll(completed);next.spent.putAll(spent);next.professionPeriods.addAll(professionPeriods);return next;}
    long spent(String cycle,long period){return spent.getOrDefault(new Pool(cycle,period),0L);}
    boolean completed(UUID player,String profession,String cycle,long period){return completed.contains(new Slot(player,profession,cycle,period));}
}
