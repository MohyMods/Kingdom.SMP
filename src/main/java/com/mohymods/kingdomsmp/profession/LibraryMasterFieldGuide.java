package com.mohymods.kingdomsmp.profession;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
/** Alpha6expedition replaces the future Otherside objective with a new Master-band Twilight visit. */
public final class LibraryMasterFieldGuide {
 public static final String QUEST="kingdom_smp:librarian/m04",METRIC="master_field_visits",DIMENSION="twilightforest:twilight_forest";
 private LibraryMasterFieldGuide(){}
 public static String identity(String dimension,String biome){return "master/"+dimension+"/"+biome;}
 public static String validate(ServerPlayer p,ItemStack stack){var book=stack.get(DataComponents.WRITTEN_BOOK_CONTENT);if(!stack.is(Items.WRITTEN_BOOK)||stack.getCount()!=1||book==null||!book.author().equals(p.getGameProfile().getName())||book.pages().isEmpty()||book.pages().size()>20)throw new IllegalStateException("Present your own readable Master Twilight field volume");var text=new StringBuilder();for(var page:book.pages()){text.append(page.raw().getString()).append('\n');if(text.length()>32768)throw new IllegalStateException("Keep field notes within20pages/32768characters");}var proof=ProfessionEvidenceSavedData.get(p.server).evidence(Profession.LIBRARIAN);try{return LibraryFieldGuide.validate(text.toString(),(dimension,biome)->dimension.equals(DIMENSION)&&LibraryExplorationService.supported(dimension)&&p.server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.ResourceLocation.parse(dimension)))!=null&&proof.seen(p.getUUID(),METRIC,identity(dimension,biome)));}catch(IllegalStateException invalid){throw new IllegalStateException("Document a genuine new Twilight biome observation from your own Master trials; old Journeyman notes cannot substitute",invalid);}}
}
