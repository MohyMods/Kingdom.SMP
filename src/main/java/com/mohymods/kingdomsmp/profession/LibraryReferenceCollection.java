package com.mohymods.kingdomsmp.profession;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
/** Actual readable collection; operator attests accuracy/usefulness, not a client completion message. */
public final class LibraryReferenceCollection {
 public static final String QUEST="kingdom_smp:librarian/m07",FINAL="kingdom_smp:librarian/m08";
 public static final Set<String> SUBJECTS=Set.of("kingdom","dimensions","construction","economy","professions","magic");
 private LibraryReferenceCollection(){}
 public static boolean candidate(ItemStack item){var book=item.get(DataComponents.WRITTEN_BOOK_CONTENT);return item.is(Items.WRITTEN_BOOK)&&book!=null&&book.pages().stream().anyMatch(p->p.raw().getString().contains("subject="));}
 public static String validate(ServerPlayer player,List<ItemStack> books){if(books.isEmpty()||books.size()>8)throw new IllegalStateException("Place your readable six-subject reference collection on accessible Library lecterns");var text=new StringBuilder();int pages=0;for(var item:books){var book=item.get(DataComponents.WRITTEN_BOOK_CONTENT);if(!item.is(Items.WRITTEN_BOOK)||item.getCount()!=1||book==null||!book.author().equals(player.getGameProfile().getName()))throw new IllegalStateException("Use your own authorized signed reference volumes");pages+=book.pages().size();if(pages>20)throw new IllegalStateException("Keep the collection within20pages");for(var page:book.pages())text.append(page.raw().getString()).append('\n');if(text.length()>32768)throw new IllegalStateException("Keep reference text within32768characters");}return validateText(text.toString());}
 public static String validateText(String text){if(text==null||text.isBlank()||text.length()>32768)throw new IllegalStateException("Readable bounded reference text required");var sections=new TreeMap<String,StringBuilder>();String current=null;int lines=0;for(var raw:text.split("\\R")){if(++lines>256)throw new IllegalStateException("Keep the collection within256lines");var line=raw.trim();if(line.startsWith("subject=")){current=line.substring(8);if(!SUBJECTS.contains(current)||sections.putIfAbsent(current,new StringBuilder())!=null)throw new IllegalStateException("Use each subject once: kingdom, dimensions, construction, economy, professions, magic");}else if(current!=null)sections.get(current).append(line).append('\n');}if(!sections.keySet().equals(SUBJECTS)||sections.values().stream().anyMatch(s->s.toString().trim().length()<40))throw new IllegalStateException("Provide six useful readable sections (at least40characters each); an operator checks current accuracy and public usefulness");try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
}
