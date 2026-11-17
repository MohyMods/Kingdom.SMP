package com.mohymods.kingdomsmp.profession;
import com.mohymods.kingdomsmp.economy.ServiceWorkState;
/** Exact approved cooperative-job prerequisite closure; other services are not activated by metadata. */
public final class ServiceWorkQuests {
 private static final ProfessionQuestCatalog CATALOG=ProfessionQuestCatalog.approved(ApprovedProfessionContent.definitions().values().stream().filter(d->d.profession()==Profession.BLACKSMITH||d.profession()==Profession.LIBRARIAN).map(ApprovedProfessionContent.Definition::slot).toList());
 private ServiceWorkQuests() { }
 public static ProfessionQuestCatalog catalog(){return CATALOG;}
 public static boolean contains(String id){return ServiceWorkState.supported(id);}
}
