package com.mohymods.kingdomsmp.profession;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;

/** Native placed-state provenance. Positions replace rather than increment; full ledgers never evict proofs. */
public final class RetainedPlacementEvidence {
    public static final int MAX_SITES=512,MAX_POSITIONS=32768;
    private final Map<UUID,Map<Long,String>> sites=new HashMap<>();
    private int total;
    public Map<Long,String> positions(UUID site){return Map.copyOf(sites.getOrDefault(site,Map.of()));}
    public boolean record(UUID site,BlockPos position,String state){
        Objects.requireNonNull(site);Objects.requireNonNull(position);
        if(state==null||state.isBlank()||state.length()>256)throw new IllegalArgumentException("Invalid native placed state");
        var rows=sites.get(site);long key=position.asLong();
        if(rows==null){if(sites.size()>=MAX_SITES||total>=MAX_POSITIONS)return false;rows=new HashMap<>();sites.put(site,rows);}
        String old=rows.get(key);if(state.equals(old))return false;
        if(old==null){if(total>=MAX_POSITIONS)return false;total++;}rows.put(key,state);return true;
    }
    public boolean invalidate(UUID site,BlockPos position){var rows=sites.get(site);if(rows==null||rows.remove(position.asLong())==null)return false;total--;return true;}
    public CompoundTag save(){var root=new CompoundTag();root.putInt("schema",1);var rows=new ListTag();sites.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->{var row=new CompoundTag();row.putUUID("site",e.getKey());var cells=new ListTag();e.getValue().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(cell->{var t=new CompoundTag();t.putLong("position",cell.getKey());t.putString("state",cell.getValue());cells.add(t);});row.put("positions",cells);rows.add(row);});root.put("sites",rows);return root;}
    public static RetainedPlacementEvidence load(CompoundTag tag){
        if(!tag.getAllKeys().equals(Set.of("schema","sites"))||!tag.contains("schema",Tag.TAG_INT)||tag.getInt("schema")!=1||!(tag.get("sites") instanceof ListTag rows)||rows.size()>MAX_SITES||!rows.isEmpty()&&rows.getElementType()!=Tag.TAG_COMPOUND)throw new IllegalArgumentException("Invalid retained evidence schema");
        var result=new RetainedPlacementEvidence();
        for(var value:rows){var row=(CompoundTag)value;
            if(!row.getAllKeys().equals(Set.of("site","positions"))||!row.hasUUID("site")||!(row.get("positions") instanceof ListTag cells)||!cells.isEmpty()&&cells.getElementType()!=Tag.TAG_COMPOUND||result.sites.containsKey(row.getUUID("site")))throw new IllegalArgumentException("Invalid duplicate worksite proof");
            var positions=new HashMap<Long,String>();result.sites.put(row.getUUID("site"),positions);
            for(var cell:cells){var t=(CompoundTag)cell;
                if(!t.getAllKeys().equals(Set.of("position","state"))||!t.contains("position",Tag.TAG_LONG)||!t.contains("state",Tag.TAG_STRING)||t.getString("state").isBlank()||t.getString("state").length()>256||positions.put(t.getLong("position"),t.getString("state"))!=null||++result.total>MAX_POSITIONS)throw new IllegalArgumentException("Invalid duplicate retained position or evidence capacity");
            }
        }
        return result;
    }
}
