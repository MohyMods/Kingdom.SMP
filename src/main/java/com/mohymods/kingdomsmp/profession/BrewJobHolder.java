package com.mohymods.kingdomsmp.profession;
import net.minecraft.nbt.CompoundTag;
/** Server-owned per-block native operation provenance, persisted together with the actual stand. */
public interface BrewJobHolder {CompoundTag kingdom$brewJob();void kingdom$brewJob(CompoundTag job);}
