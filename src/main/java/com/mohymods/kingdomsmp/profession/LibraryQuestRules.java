package com.mohymods.kingdomsmp.profession;
import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
/** World-local configurable quantities for approved native library objectives. */
public record LibraryQuestRules(int enchantedItems,int bookshelves,int enchantedBooks,int catalogueEntries) {
 private static final Map<MinecraftServer,LibraryQuestRules> CACHE=new WeakHashMap<>();
 public LibraryQuestRules{if(catalogueEntries<1||catalogueEntries>16)throw new IllegalArgumentException("Catalogue entries must be1..16");if(enchantedBooks<1||enchantedBooks>36)throw new IllegalArgumentException("Enchanted books must fit1..36inventory slots");if(bookshelves<1||bookshelves>128)throw new IllegalArgumentException("Bookshelves must be1..128");if(enchantedItems<1||enchantedItems>36)throw new IllegalArgumentException("Enchanted items must fit1..36inventory slots");}
 public static void clear(MinecraftServer server){CACHE.remove(server);}
 public static LibraryQuestRules get(MinecraftServer server){return CACHE.computeIfAbsent(server,s->{try{return load(s.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-library-quests.json"));}catch(IOException e){throw new IllegalStateException("Library configuration invalid; nothing consumed",e);}});}
 public static LibraryQuestRules load(Path path)throws IOException{if(!Files.exists(path)){Files.createDirectories(path.toAbsolutePath().getParent());Files.writeString(path,"{\"schema\":1,\"enchantedItems\":4,\"bookshelves\":12,\"enchantedBooks\":4,\"catalogueEntries\":6}\n",StandardOpenOption.CREATE_NEW);}try{var root=JsonParser.parseString(Files.readString(path)).getAsJsonObject();if(!root.keySet().containsAll(Set.of("schema","enchantedItems"))||!Set.of("schema","enchantedItems","bookshelves","enchantedBooks","catalogueEntries").containsAll(root.keySet()))throw new IllegalArgumentException("Unknown library rule");for(var value:root.entrySet())if(!value.getValue().isJsonPrimitive()||!value.getValue().getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Integer library quantity required");if(root.get("schema").getAsBigDecimal().intValueExact()!=1)throw new IllegalArgumentException("Invalid library schema");return new LibraryQuestRules(root.get("enchantedItems").getAsBigDecimal().intValueExact(),root.has("bookshelves")?root.get("bookshelves").getAsBigDecimal().intValueExact():12,root.has("enchantedBooks")?root.get("enchantedBooks").getAsBigDecimal().intValueExact():4,root.has("catalogueEntries")?root.get("catalogueEntries").getAsBigDecimal().intValueExact():6);}catch(RuntimeException e){throw new IOException("Invalid native library quantities",e);}}
}
