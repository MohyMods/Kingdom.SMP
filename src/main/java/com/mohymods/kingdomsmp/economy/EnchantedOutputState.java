package com.mohymods.kingdomsmp.economy;

import java.util.*;
import net.minecraft.nbt.CompoundTag;

/** Derived exclusively from canonical operations, including archives; output use and XP commit together. */
final class EnchantedOutputState {
 static final int LIMIT=4096;
 record Output(UUID smith,String quest,CompoundTag item,boolean spent){Output{item=item.copy();} @Override public CompoundTag item(){return item.copy();}}
 final Map<UUID,Output> outputs=new HashMap<>();
 EnchantedOutputState copy(){var next=new EnchantedOutputState();next.outputs.putAll(outputs);return next;}
 void issue(UUID id,UUID smith,String quest,CompoundTag item){if(outputs.size()>=LIMIT||outputs.containsKey(id))throw new IllegalStateException("Enchanted output identity already exists or capacity reached");outputs.put(id,new Output(smith,quest,item,false));}
 boolean matches(UUID id,UUID smith,String quest,CompoundTag item){var o=outputs.get(id);return o!=null&&!o.spent()&&o.smith().equals(smith)&&o.quest().equals(quest)&&o.item().equals(item);}
 void spend(UUID smith,String quest,Map<UUID,CompoundTag> items,int required){if(items.size()!=required||required<1||required>128)throw new IllegalArgumentException("Distinct enchanted output count differs");for(var e:items.entrySet())if(!matches(e.getKey(),smith,quest,e.getValue()))throw new IllegalStateException("Unknown, altered, foreign or already consumed enchanted output");items.forEach((id,item)->outputs.put(id,new Output(smith,quest,item,true)));}
}
