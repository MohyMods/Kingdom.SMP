package com.mohymods.kingdomsmp.economy;

import java.util.*;
import net.minecraft.nbt.*;

/** Bounded typed region observations, never accepted as client travel proof. */
public final class ScoutData {
  private ScoutData() {}

  public static ListTag encode(Collection<ScoutState.Region> regions) {
    var rows = new ListTag();
    for (var region : regions) {
      var row = new CompoundTag();
      row.putInt("x", region.x());
      row.putInt("z", region.z());
      row.putString("biome", region.biome());
      rows.add(row);
    }
    return rows;
  }

  public static List<ScoutState.Region> decode(Tag data) {
    if (!(data instanceof ListTag rows)
        || rows.size() > 128
        || !rows.isEmpty() && rows.getElementType() != Tag.TAG_COMPOUND)
      throw new IllegalArgumentException("Invalid bounded scouting observations");
    var result = new ArrayList<ScoutState.Region>();
    for (var value : rows) {
      var row = (CompoundTag) value;
      if (!row.getAllKeys().equals(Set.of("x", "z", "biome"))
          || !row.contains("x", Tag.TAG_INT)
          || !row.contains("z", Tag.TAG_INT)
          || !row.contains("biome", Tag.TAG_STRING))
        throw new IllegalArgumentException("Invalid surveyed region fields");
      result.add(new ScoutState.Region(row.getInt("x"), row.getInt("z"), row.getString("biome")));
    }
    if (result.stream().map(ScoutState.Region::key).distinct().count() != result.size())
      throw new IllegalArgumentException("Duplicate observed regions");
    return List.copyOf(result);
  }
}
