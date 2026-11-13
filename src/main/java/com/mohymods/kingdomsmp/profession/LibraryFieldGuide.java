package com.mohymods.kingdomsmp.profession;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;
/** Readable field notes are references to native saved observations, never travel authority. */
public final class LibraryFieldGuide {
 public static final String QUEST="kingdom_smp:librarian/j03",METRIC="field_visits";
 public static final Set<String> DIMENSIONS=Set.of("aether:the_aether","twilightforest:twilight_forest");
 private static final Pattern ROW=Pattern.compile("dimension=([a-z0-9_.-]+:[a-z0-9_./-]+) biome=([a-z0-9_.-]+:[a-z0-9_./-]+)");
 private LibraryFieldGuide(){}
 public static String identity(String dimension,String biome){return "journeyman/"+dimension+"/"+biome;}
 public static String validate(String text,BiPredicate<String,String> witnessed){
  if(text==null||text.isBlank()||text.length()>32768)throw new IllegalStateException("Write a readable field guide within20pages/32768characters");
  Set<String> rows=new HashSet<>();int lines=0;
  for(var line:text.split("\\R")){if(++lines>256)throw new IllegalStateException("Keep the field guide within256lines");var row=ROW.matcher(line.trim());if(row.matches()){
    var dim=row.group(1);var biome=row.group(2);if(!DIMENSIONS.contains(dim)||identity(dim,biome).length()>160||!rows.add(dim+"|"+biome)||rows.size()>8||!witnessed.test(dim,biome))throw new IllegalStateException("Each field note must match your own recorded Journeyman visit to an available Aether or Twilight biome");
   }else if(line.contains("dimension=")||line.contains("biome="))throw new IllegalStateException("Use dimension=namespace:id biome=namespace:id for a recorded observation");
  }
  if(rows.isEmpty())throw new IllegalStateException("Document at least one personally observed Aether or Twilight biome; an operator inspects your narrative");
  try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
 }
}
