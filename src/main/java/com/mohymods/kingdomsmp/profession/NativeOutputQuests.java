package com.mohymods.kingdomsmp.profession;
import com.mohymods.kingdomsmp.economy.CommerceService;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
/** Explicit validated producer purposes. Metadata alone never authenticates a physical output. */
public final class NativeOutputQuests {
 public static final String LIBRARY_FAMILIES="kingdom_smp:librarian/j01",LIBRARY_MAXIMUM="kingdom_smp:librarian/j02";
 private static final String TARGET="kingdom_smp_library_output_target";
 public static final String LIBRARY_BOOKS="kingdom_smp:librarian/a05";
 public static final String LIBRARY_ENCHANT="kingdom_smp:librarian/a04";
 private static final Set<String> BASE_IDS=Set.of(EnchantedToolService.QUEST,SmithingQuestService.QUEST,LIBRARY_ENCHANT,LIBRARY_BOOKS,LIBRARY_FAMILIES,LIBRARY_MAXIMUM);
 private static final Set<String> IDS=java.util.stream.Stream.concat(BASE_IDS.stream(),BrewQuests.ids().stream()).collect(java.util.stream.Collectors.toUnmodifiableSet());
 private static final ProfessionQuestCatalog CATALOG=ProfessionQuestCatalog.approved(ApprovedProfessionContent.definitions().values().stream().filter(d->d.profession()==Profession.ALCHEMIST||d.profession()==Profession.BLACKSMITH||d.profession()==Profession.LIBRARIAN).map(ApprovedProfessionContent.Definition::slot).toList());
 private NativeOutputQuests(){}
 public static boolean contains(String id){return id!=null&&IDS.contains(id);}
 public static ApprovedProfessionContent.Definition get(String id){if(!contains(id))throw new IllegalArgumentException("No validated native output producer");return ApprovedProfessionContent.get(id);}
 public static ProfessionQuestCatalog catalog(){return CATALOG;}
 public static boolean consume(String id){get(id);return !id.equals(SmithingQuestService.QUEST);}
 public static String issueDetail(String id){get(id);if(BrewQuests.contains(id))return "Native attributable brewing output";return id.equals(SmithingQuestService.QUEST)?"Native smithing upgrade output":(id.equals(LIBRARY_BOOKS)||journeymanBook(id))?"Native compatible book output":id.equals(LIBRARY_ENCHANT)?"Native enchanting output":"Native compatible anvil output";}
 public static String receiptDetail(String id){if(BrewQuests.contains(id))return "Consumed authenticated brewed potions";return consume(id)?(id.equals(LIBRARY_ENCHANT)||id.equals(LIBRARY_BOOKS)||journeymanBook(id))?"Consumed authenticated enchanted items":"Consumed authenticated enchanted tools":"Registered authenticated smithing upgrade";}
 public static int goal(ServerPlayer p,String id){get(id);if(BrewQuests.contains(id))return BrewQuestRules.goal(p.server,id);return id.equals(LIBRARY_FAMILIES)?4*LibraryQuestRules.get(p.server).familyCopies():id.equals(LIBRARY_MAXIMUM)?LibraryQuestRules.get(p.server).maximumBooks():id.equals(SmithingQuestService.QUEST)?RepairQuestRules.upgrades(p.server):id.equals(LIBRARY_BOOKS)?LibraryQuestRules.get(p.server).enchantedBooks():id.equals(LIBRARY_ENCHANT)?LibraryQuestRules.get(p.server).enchantedItems():RepairQuestRules.enchantedTools(p.server);}
 public static boolean ordinaryLibrary(String id){if(id==null)return false;if(Set.of("minecraft:enchanted_book","minecraft:bow","minecraft:crossbow","minecraft:fishing_rod").contains(id))return true;return id.startsWith("minecraft:iron_")&&Set.of("pickaxe","axe","shovel","hoe","sword","helmet","chestplate","leggings","boots").contains(id.substring(15))||id.startsWith("minecraft:diamond_")&&Set.of("pickaxe","axe","shovel","hoe","sword","helmet","chestplate","leggings","boots").contains(id.substring(18));}
 public static boolean output(String quest,String item){get(quest);if(BrewQuests.contains(quest))return BrewQuests.outputItem(quest,item);return quest.equals(SmithingQuestService.QUEST)?SmithingQuestService.upgraded(item):(quest.equals(LIBRARY_BOOKS)||journeymanBook(quest))?item.equals("minecraft:enchanted_book"):quest.equals(LIBRARY_ENCHANT)?ordinaryLibrary(item):EnchantedToolService.ordinary(item);}
 public static boolean journeymanBook(String id){return LIBRARY_FAMILIES.equals(id)||LIBRARY_MAXIMUM.equals(id);}
 public static String bookTarget(ServerPlayer p){var selected=p.getPersistentData().getString(TARGET);if(journeymanBook(selected)&&eligible(p,selected,true))return selected;if(eligible(p,LIBRARY_BOOKS,true))return LIBRARY_BOOKS;if(eligible(p,LIBRARY_FAMILIES,true))return LIBRARY_FAMILIES;if(eligible(p,LIBRARY_MAXIMUM,true))return LIBRARY_MAXIMUM;return LIBRARY_BOOKS;}
 public static void choose(ServerPlayer p,String quest){if(!journeymanBook(quest)||!eligible(p,quest,true))throw new IllegalStateException("This book commission is unavailable");ContractService.requireCounter(p,Profession.LIBRARIAN);p.getPersistentData().putString(TARGET,quest);}
 public static boolean eligible(ServerPlayer p,String quest,boolean premises){if(!contains(quest)||!TreeQuestService.real(p))return false;var store=CommerceService.initializedStore(p.server).orElse(null);if(store==null||store.quarantined())return false;var d=get(quest);var r=store.professions(p.getUUID());return r.isActive(d.profession())&&r.progressBand(d.profession())==d.rank()&&!r.track(d.profession()).completedQuestIds().contains(quest)&&CATALOG.mayAttempt(d.profession(),quest,r.track(d.profession()).completedQuestIds())&&com.mohymods.kingdomsmp.contribution.ContributionService.kingdom(p.server).attainedTier()>=d.minimumTier()&&(!premises||ProfessionService.available(p,d.profession()));}
}
