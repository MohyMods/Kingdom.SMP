package com.mohymods.kingdomsmp.equipment;
import com.mohymods.kingdomsmp.profession.Profession;
import java.util.*;
/** Owner-approved alpha signatures. Numeric defaults are server configurable. */
public enum MasterGear {
 EXTRACTION("extraction_anvil","Extraction Anvil",Profession.BLACKSMITH,50000,"Extract one selected non-curse enchantment into a book; retain equipment, durability and other components. Real book, lapis and XP costs."),
 WAND("architects_wand","Architect's Wand",Profession.BUILDER,50000,"Inventory-funded lines, walls and floors, or matching-block replacement. Two corners, server preview and confirmation. Sneak-use cycles mode."),
 AXE("worldtree_axe","Worldtree Axe",Profession.LUMBERJACK,50000,"Unbreakable Efficiency VI. Fell a tracked native tree with real drops and seed-funded replanting. Sneak for one block."),
 PICKAXE("prospectors_pickaxe","Prospector's Pickaxe",Profession.MINER,50000,"Unbreakable Fortune VI / Efficiency VI. Bounded native ore veins; right-click to survey nearby permitted loaded ores. Sneak mines one block."),
 CODEX("enchanters_codex","Enchanter's Codex",Profession.LIBRARIAN,50000,"Compatible book enchanting at half native XP cost, with configured advanced limits. Real books/materials/XP; existing item components retained.");
 private final String path,title,detail;private final Profession role;private final long base;
 MasterGear(String path,String title,Profession role,long base,String detail){this.path=path;this.title=title;this.role=role;this.base=base;this.detail=detail;}
 public String id(){return "kingdom_smp:"+path;}public String path(){return path;}public String title(){return title;}public Profession role(){return role;}public long base(){return base;}public String detail(){return detail;}
 public long price(long base,int previous){return FarmerGear.FORTUNE_X_HOE.price(base,previous);}
 public static Optional<MasterGear> find(String id){return Arrays.stream(values()).filter(g->g.id().equals(id)).findFirst();}
 public static MasterGear byId(String id){return find(id).orElseThrow(()->new IllegalArgumentException("Unknown Master reward"));}
}
