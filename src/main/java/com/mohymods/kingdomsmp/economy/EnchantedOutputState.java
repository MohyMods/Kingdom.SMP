package com.mohymods.kingdomsmp.economy;

import java.util.*;
import com.mohymods.kingdomsmp.profession.*;
import net.minecraft.nbt.CompoundTag;

/** Derived exclusively from canonical operations, including archives; output use and XP commit together. */
final class EnchantedOutputState {
 static final int LIMIT=4096;
 record Output(UUID smith,String quest,CompoundTag item,NativeBookProof attributes,boolean spent){Output{item=item.copy();} @Override public CompoundTag item(){return item.copy();}}
 final Map<UUID,Output> outputs=new HashMap<>();
 EnchantedOutputState copy(){var next=new EnchantedOutputState();next.outputs.putAll(outputs);return next;}
 void issue(UUID id,UUID smith,String quest,CompoundTag item,NativeBookProof attributes){if(outputs.size()>=LIMIT||outputs.containsKey(id))throw new IllegalStateException("Enchanted output identity already exists or capacity reached");outputs.put(id,new Output(smith,quest,item,attributes,false));}
 boolean matches(UUID id,UUID smith,String quest,CompoundTag item){var o=outputs.get(id);return o!=null&&!o.spent()&&o.smith().equals(smith)&&o.quest().equals(quest)&&o.item().equals(item);}
 void spend(UUID smith,String quest,Map<UUID,CompoundTag> items,int required){if(items.size()!=required||required<1||required>128)throw new IllegalArgumentException("Distinct enchanted output count differs");for(var e:items.entrySet())if(!matches(e.getKey(),smith,quest,e.getValue()))throw new IllegalStateException("Unknown, altered, foreign or already consumed enchanted output");if(quest.equals(NativeOutputQuests.LIBRARY_FAMILIES)&&NativeBookProof.selectFamilies(items.keySet().stream().map(id->outputs.get(id).attributes().families()).toList(),required).size()!=required)throw new IllegalStateException("Deliver distinct books covering every native family");if(quest.equals(NativeOutputQuests.LIBRARY_MAXIMUM)&&items.keySet().stream().anyMatch(id->!outputs.get(id).attributes().maximum()))throw new IllegalStateException("Actual newly achieved native maxima required");items.forEach((id,item)->outputs.put(id,new Output(smith,quest,item,outputs.get(id).attributes(),true)));}
}
