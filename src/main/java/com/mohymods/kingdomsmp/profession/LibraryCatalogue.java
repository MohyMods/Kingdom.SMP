package com.mohymods.kingdomsmp.profession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
/** Readable bounded reference text, verified against the running native registry; OP2 attests authorship/access. */
public final class LibraryCatalogue {
 private static final Pattern DECLARATION=Pattern.compile("(minecraft:[a-z0-9_]+) max=([1-9][0-9]?)");
 private static final Pattern CONFLICT=Pattern.compile("(minecraft:[a-z0-9_]+) ! (minecraft:[a-z0-9_]+)");
 private LibraryCatalogue(){}
 public static String validate(ServerPlayer p,ItemStack stack){
  var book=stack.get(DataComponents.WRITTEN_BOOK_CONTENT);if(!stack.is(Items.WRITTEN_BOOK)||stack.getCount()!=1||book==null||book.pages().isEmpty()||book.pages().size()>20)throw new IllegalStateException("Place a readable written catalogue on the lectern");var text=new StringBuilder();for(var page:book.pages()){text.append(page.raw().getString()).append('\n');if(text.length()>32768)throw new IllegalStateException("Keep the catalogue within20pages/32768characters");}
  var registry=p.registryAccess().registryOrThrow(Registries.ENCHANTMENT);Map<String,Holder.Reference<Enchantment>> declarations=new TreeMap<>();Set<String> conflicts=new HashSet<>();int lines=0;
  for(var row:text.toString().split("\\R")){if(++lines>256)throw new IllegalStateException("Catalogue is limited to256readable lines");var line=row.trim();var declaration=DECLARATION.matcher(line);var conflict=CONFLICT.matcher(line);if(declaration.matches()){var id=declaration.group(1);var value=registry.getHolder(ResourceLocation.parse(id)).orElseThrow(()->new IllegalStateException("Catalogue enchantment is not registered: "+id));if(Integer.parseInt(declaration.group(2))!=value.value().getMaxLevel()||declarations.put(id,value)!=null)throw new IllegalStateException("Use each actual native maximum once: "+id);if(declarations.size()>16)throw new IllegalStateException("Document at most16ordinary enchantments");}else if(conflict.matches()){var a=conflict.group(1);var b=conflict.group(2);if(a.equals(b)||!conflicts.add(pair(a,b)))throw new IllegalStateException("Duplicate or self-conflicting reference");}else if(line.contains(" max=")||line.contains(" ! "))throw new IllegalStateException("Use minecraft:id max=N and minecraft:a ! minecraft:b for ordinary registered references");}
  if(declarations.size()<LibraryQuestRules.get(p.server).catalogueEntries())throw new IllegalStateException("Document the configured number of distinct ordinary native enchantments");
  Set<String> expected=new HashSet<>();var rows=new ArrayList<>(declarations.entrySet());for(int i=0;i<rows.size();i++)for(int j=i+1;j<rows.size();j++)if(!Enchantment.areCompatible(rows.get(i).getValue(),rows.get(j).getValue()))expected.add(pair(rows.get(i).getKey(),rows.get(j).getKey()));if(!expected.equals(conflicts))throw new IllegalStateException("List every actual incompatible pair; unlisted declared pairs must be compatible");
  try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((book.title().raw().length()+":"+book.title().raw()+"|"+book.author().length()+":"+book.author()+"|"+book.generation()+"|"+book.resolved()+"|"+text).getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
 }
 private static String pair(String a,String b){return a.compareTo(b)<0?a+"!"+b:b+"!"+a;}
}
