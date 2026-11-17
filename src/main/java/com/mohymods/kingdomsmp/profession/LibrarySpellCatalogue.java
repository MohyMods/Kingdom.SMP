package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.nbt.*;
/** Native producer facts pin rarity at inspection; optional mod/config classes are never needed for replay. */
public final class LibrarySpellCatalogue {
 public static final String MASTER="kingdom_smp:librarian/m03";
 public static final String QUEST="kingdom_smp:librarian/j05",ARTIFACT="irons_spellbooks:1.21.1-3.16.3",DETAIL="Inspected native spell catalogue";
 public record Fact(String spell,int level,String rarity){
  public Fact{if(spell==null||!spell.matches("irons_spellbooks:[a-z0-9_/]+")||spell.length()>128||level<1||level>256||!"RARE".equals(rarity))throw new IllegalArgumentException("Invalid verified native RARE spell facts");}
  public CompoundTag save(){var n=new CompoundTag();n.putString("spell",spell);n.putInt("level",level);n.putString("rarity",rarity);return n;}
  public static Fact load(CompoundTag n){if(!n.getAllKeys().equals(Set.of("spell","level","rarity"))||!n.contains("spell",Tag.TAG_STRING)||!n.contains("level",Tag.TAG_INT)||!n.contains("rarity",Tag.TAG_STRING))throw new IllegalArgumentException("Malformed native spell facts");return new Fact(n.getString("spell"),n.getInt("level"),n.getString("rarity"));}
 }
 private LibrarySpellCatalogue(){}
 public static boolean isSpellQuest(String quest){return QUEST.equals(quest)||MASTER.equals(quest);}
 public static void validate(List<CompoundTag> items,List<CompoundTag> facts){validate(QUEST,items,facts);}
 public static void validate(String quest,List<CompoundTag> items,List<CompoundTag> facts){int count=LibraryGlyphCatalogue.count(quest);if(!isSpellQuest(quest)||items.size()!=count||facts.size()!=count)throw new IllegalArgumentException("Four different native RARE spell IDs required");var ids=new HashSet<String>();for(int i=0;i<count;i++){var item=items.get(i);var fact=Fact.load(facts.get(i));var c=item.getCompound("components").getCompound("irons_spellbooks:spell_container");var data=c.getList("data",Tag.TAG_COMPOUND);if(item.getInt("count")!=1||!item.getString("id").equals("irons_spellbooks:scroll")||!c.contains("maxSpells",Tag.TAG_INT)||c.getInt("maxSpells")!=1||c.getBoolean("mustEquip")||c.getBoolean("spellWheel")||data.size()!=1||!ids.add(fact.spell()))throw new IllegalArgumentException("Distinct single-spell native scrolls required");var spell=data.getCompound(0);if(!spell.contains("index",Tag.TAG_INT)||spell.getInt("index")!=0||!spell.getString("id").equals(fact.spell())||spell.getInt("level")!=fact.level()||!spell.contains("locked",Tag.TAG_BYTE)||spell.getByte("locked")<0||spell.getByte("locked")>1)throw new IllegalArgumentException("Native scroll component differs from inspected spell facts");}}
}
