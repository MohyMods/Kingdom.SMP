package com.mohymods.kingdomsmp.economy;
import java.util.*;
/** Replayed projection only; no second wallet, persistence file, or client-supplied balance. */
final class ContractState {
    record Slot(UUID player,String profession,String cycle,long period) { }
    record Pool(String cycle,long period) { }
    final Set<Slot> completed=new HashSet<>();final Map<Pool,Long> spent=new HashMap<>();
    ContractState copy(){var next=new ContractState();next.completed.addAll(completed);next.spent.putAll(spent);return next;}
    long spent(String cycle,long period){return spent.getOrDefault(new Pool(cycle,period),0L);}
    boolean completed(UUID player,String profession,String cycle,long period){return completed.contains(new Slot(player,profession,cycle,period));}
}
