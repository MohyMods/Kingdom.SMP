package com.mohymods.kingdomsmp.profession;
import com.mohymods.kingdomsmp.economy.CommerceService;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
/** Explicit validated producer purposes. Metadata alone never authenticates a physical output. */
public final class NativeOutputQuests {
 public static final String LIBRARY_BOOKS="kingdom_smp:librarian/a05";
 public static final String LIBRARY_ENCHANT="kingdom_smp:librarian/a04";
 private static final Set<String> IDS=Set.of(EnchantedToolService.QUEST,SmithingQuestService.QUEST,LIBRARY_ENCHANT,LIBRARY_BOOKS);
 private static final ProfessionQuestCatalog CATALOG=ProfessionQuestCatalog.approved(ApprovedProfessionContent.definitions().values().stream().filter(d->d.profession()==Profession.BLACKSMITH||d.profession()==Profession.LIBRARIAN).map(ApprovedProfessionContent.Definition::slot).toList());
 private NativeOutputQuests(){}
 public static boolean contains(String id){return id!=null&&IDS.contains(id);}
 public static ApprovedProfessionContent.Definition get(String id){if(!contains(id))throw new IllegalArgumentException("No validated native output producer");return ApprovedProfessionContent.get(id);}
 public static ProfessionQuestCatalog catalog(){return CATALOG;}
 public static boolean consume(String id){get(id);return !id.equals(SmithingQuestService.QUEST);}
 public static String issueDetail(String id){get(id);return id.equals(SmithingQuestService.QUEST)?"Native smithing upgrade output":id.equals(LIBRARY_BOOKS)?"Native compatible book output":id.equals(LIBRARY_ENCHANT)?"Native enchanting output":"Native compatible anvil output";}
 public static String receiptDetail(String id){return consume(id)?(id.equals(LIBRARY_ENCHANT)||id.equals(LIBRARY_BOOKS))?"Consumed authenticated enchanted items":"Consumed authenticated enchanted tools":"Registered authenticated smithing upgrade";}
 public static int goal(ServerPlayer p,String id){get(id);return id.equals(SmithingQuestService.QUEST)?RepairQuestRules.upgrades(p.server):id.equals(LIBRARY_BOOKS)?LibraryQuestRules.get(p.server).enchantedBooks():id.equals(LIBRARY_ENCHANT)?LibraryQuestRules.get(p.server).enchantedItems():RepairQuestRules.enchantedTools(p.server);}
 public static boolean ordinaryLibrary(String id){if(id==null)return false;if(Set.of("minecraft:enchanted_book","minecraft:bow","minecraft:crossbow","minecraft:fishing_rod").contains(id))return true;return id.startsWith("minecraft:iron_")&&Set.of("pickaxe","axe","shovel","hoe","sword","helmet","chestplate","leggings","boots").contains(id.substring(15))||id.startsWith("minecraft:diamond_")&&Set.of("pickaxe","axe","shovel","hoe","sword","helmet","chestplate","leggings","boots").contains(id.substring(18));}
 public static boolean output(String quest,String item){get(quest);return quest.equals(SmithingQuestService.QUEST)?SmithingQuestService.upgraded(item):quest.equals(LIBRARY_BOOKS)?item.equals("minecraft:enchanted_book"):quest.equals(LIBRARY_ENCHANT)?ordinaryLibrary(item):EnchantedToolService.ordinary(item);}
 public static boolean eligible(ServerPlayer p,String quest,boolean premises){if(!contains(quest)||!TreeQuestService.real(p))return false;var store=CommerceService.initializedStore(p.server).orElse(null);if(store==null||store.quarantined())return false;var d=get(quest);var r=store.professions(p.getUUID());return r.isActive(d.profession())&&r.progressBand(d.profession())==d.rank()&&!r.track(d.profession()).completedQuestIds().contains(quest)&&CATALOG.mayAttempt(d.profession(),quest,r.track(d.profession()).completedQuestIds())&&com.mohymods.kingdomsmp.contribution.ContributionService.kingdom(p.server).attainedTier()>=d.minimumTier()&&(!premises||ProfessionService.available(p,d.profession()));}
}
