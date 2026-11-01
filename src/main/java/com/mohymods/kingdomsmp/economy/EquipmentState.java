package com.mohymods.kingdomsmp.economy;
import java.util.*;
/** Replayed successful purchase counters; a retry or canceled native delivery never increments again. */
final class EquipmentState {
 record Key(UUID player,String item){ }
 final Map<Key,Integer> counts=new HashMap<>();
 int count(UUID player,String item){return counts.getOrDefault(new Key(player,item),0);}
 EquipmentState copy(){var next=new EquipmentState();next.counts.putAll(counts);return next;}
}
