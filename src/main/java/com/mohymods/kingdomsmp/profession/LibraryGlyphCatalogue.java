package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.nbt.*;
/** Immutable replay contract for the exact runtime-verified Ars5.13.3 Tier2 catalogue. */
public final class LibraryGlyphCatalogue {
 public static final String QUEST="kingdom_smp:librarian/j04",ARTIFACT="ars_nouveau:5.13.3",DETAIL="Inspected native glyph catalogue";
 public static final Set<String> GLYPHS=Set.of("accelerate","animate_block","aoe","cold_snap","conjure_water","crush","dampen","decelerate","duration_down","ender_inventory","exchange","explosion","extend_time","extract","firework","flare","fortune","gravity","grow","heal","infuse","invisibility","name","pierce","sense_magic","slowfall","smelt","wind_shear");
 private static final ProfessionQuestCatalog CATALOG=ProfessionQuestCatalog.approved(ApprovedProfessionContent.definitions().values().stream().filter(d->d.profession()==Profession.LIBRARIAN).map(ApprovedProfessionContent.Definition::slot).toList());
 private LibraryGlyphCatalogue(){}
 public static ProfessionQuestCatalog catalog(){return CATALOG;}
 public static boolean glyph(String id){return id!=null&&id.startsWith("ars_nouveau:glyph_")&&GLYPHS.contains(id.substring("ars_nouveau:glyph_".length()));}
 public static void validate(List<CompoundTag> items){if(items.size()!=4)throw new IllegalArgumentException("Four different native Tier2 glyphs required");var ids=new HashSet<String>();for(var item:items)if(item.getInt("count")!=1||!glyph(item.getString("id"))||!ids.add(item.getString("id")))throw new IllegalArgumentException("Catalogue glyphs must be distinct native Tier2 items, one of each");}
}
