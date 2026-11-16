package com.mohymods.kingdomsmp.profession;
import java.nio.file.*;
import java.util.*;
import java.io.IOException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
/** Separate additive alpha rules never rewrite old Library settings or earned receipts. */
public final class LibraryMasterRules {
 private static final Map<MinecraftServer,Integer> CACHE=new WeakHashMap<>();
 private LibraryMasterRules(){}
 public static int books(MinecraftServer server){return CACHE.computeIfAbsent(server,s->{try{var path=s.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-library-master-quests.json");if(!Files.exists(path)){Files.createDirectories(path.getParent());Files.writeString(path,"{\"schema\":1,\"books\":6}\n",StandardOpenOption.CREATE_NEW);}var json=com.google.gson.JsonParser.parseString(Files.readString(path)).getAsJsonObject();if(!json.keySet().equals(Set.of("schema","books"))||!json.get("schema").isJsonPrimitive()||!json.getAsJsonPrimitive("schema").isNumber()||!json.get("books").isJsonPrimitive()||!json.getAsJsonPrimitive("books").isNumber()||json.get("schema").getAsBigDecimal().intValueExact()!=1)throw new IllegalArgumentException("Invalid Master Library rules");int count=json.get("books").getAsBigDecimal().intValueExact();if(count<6||count>36)throw new IllegalArgumentException("Master compendium requires6..36books across at least3native families");return count;}catch(IOException|RuntimeException e){throw new IllegalStateException("Master Library rules invalid; nothing consumed",e);}});}
}
