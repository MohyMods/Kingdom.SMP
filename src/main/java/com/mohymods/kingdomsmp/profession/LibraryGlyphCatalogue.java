package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.nbt.*;
/** Immutable replay contract for the exact runtime-verified Ars5.13.3 Tier2 catalogue. */
public final class LibraryGlyphCatalogue {
 public static final String MASTER="kingdom_smp:librarian/m02";
 public static final String QUEST="kingdom_smp:librarian/j04",ARTIFACT="ars_nouveau:5.13.3",DETAIL="Inspected native glyph catalogue";
 public static final Set<String> GLYPHS=Set.of("accelerate","animate_block","aoe","cold_snap","conjure_water","crush","dampen","decelerate","duration_down","ender_inventory","exchange","explosion","extend_time","extract","firework","flare","fortune","gravity","grow","heal","infuse","invisibility","name","pierce","sense_magic","slowfall","smelt","wind_shear");
 private static final ProfessionQuestCatalog CATALOG=ProfessionQuestCatalog.approved(ApprovedProfessionContent.definitions().values().stream().filter(d->d.profession()==Profession.LIBRARIAN).map(ApprovedProfessionContent.Definition::slot).toList());
 private LibraryGlyphCatalogue(){}
 public static ProfessionQuestCatalog catalog(){return CATALOG;}
 public static boolean glyph(String id){return id!=null&&id.startsWith("ars_nouveau:glyph_")&&GLYPHS.contains(id.substring("ars_nouveau:glyph_".length()));}
 public static boolean isGlyphQuest(String quest){return QUEST.equals(quest)||MASTER.equals(quest);}
 public static int count(String quest){if(!isGlyphQuest(quest)&&!LibrarySpellCatalogue.isSpellQuest(quest))throw new IllegalArgumentException("Unknown catalogue purpose");return quest.equals(MASTER)||quest.equals(LibrarySpellCatalogue.MASTER)?6:4;}
 public static long xp(String quest){return count(quest)==6?300:200;}
 public static void validate(List<CompoundTag> items){validate(QUEST,items);}
 public static void validate(String quest,List<CompoundTag> items){if(!isGlyphQuest(quest)||items.size()!=count(quest))throw new IllegalArgumentException("Four different native Tier2 glyphs required");var ids=new HashSet<String>();for(var item:items)if(item.getInt("count")!=1||!glyph(item.getString("id"))||!ids.add(item.getString("id")))throw new IllegalArgumentException("Catalogue glyphs must be distinct native Tier2 items, one of each");}
}
