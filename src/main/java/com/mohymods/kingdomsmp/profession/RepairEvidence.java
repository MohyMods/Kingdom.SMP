package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.nbt.*;
/** Bounded immutable native-wear identities and one repair credit per item lifetime. No item grants. */
public final class RepairEvidence {
 public static final int LIMIT=8192;
 public static final Set<String> IRON=Set.of("iron_pickaxe","iron_axe","iron_shovel","iron_hoe","iron_sword","iron_helmet","iron_chestplate","iron_leggings","iron_boots");
 public record Item(UUID id,UUID wearer,String item,int damage,UUID credited,ProfessionRank band){
  public Item{Objects.requireNonNull(id);Objects.requireNonNull(wearer);if(!eligible(item)||damage<1||damage>100000||(credited==null)!=(band==null))throw new IllegalArgumentException("Invalid native wear record");}
 }
 private final UUID identity;private final Map<UUID,Item> items=new HashMap<>();
 public RepairEvidence(){this(UUID.randomUUID());}private RepairEvidence(UUID identity){this.identity=identity;}
 public UUID identity(){return identity;}public Item get(UUID id){return items.get(id);}
 public List<Item> history(UUID player,int limit){if(limit<1||limit>16)throw new IllegalArgumentException("History limit1..16");return items.values().stream().filter(i->player.equals(i.wearer())||player.equals(i.credited())).sorted(Comparator.comparing(Item::id)).limit(limit).toList();}
 public static boolean eligible(String item){return item!=null&&item.startsWith("minecraft:")&&IRON.contains(item.substring(10));}
 public UUID worn(UUID known,UUID owner,String item,int damage){
  if(!eligible(item)||damage<1||damage>100000)return null;var previous=items.get(known);
  if(previous!=null){if(!previous.item().equals(item))return null;if(previous.credited()==null)items.put(known,new Item(known,previous.wearer(),item,damage,null,null));return known;}
  if(items.size()>=LIMIT)return null;var id=UUID.randomUUID();items.put(id,new Item(id,owner,item,damage,null,null));return id;
 }
 public boolean repair(UUID id,UUID smith,ProfessionRank band,String item,int before,int after,int materials,int levels){
  var proof=items.get(id);if(proof==null||proof.credited()!=null||!proof.item().equals(item)||before!=proof.damage()||after<0||after>=before||materials<1||levels<1)return false;
  items.put(id,new Item(id,proof.wearer(),item,before,smith,band));return true;
 }
 public int count(UUID smith,ProfessionRank band){return (int)items.values().stream().filter(v->smith.equals(v.credited())&&v.band()==band).count();}
 public CompoundTag save(){var tag=new CompoundTag();tag.putInt("schema",1);tag.putUUID("identity",identity);var rows=new ListTag();items.values().stream().sorted(Comparator.comparing(Item::id)).forEach(v->{var row=new CompoundTag();row.putUUID("id",v.id());row.putUUID("wearer",v.wearer());row.putString("item",v.item());row.putInt("damage",v.damage());row.putString("band",v.band()==null?"":v.band().id());if(v.credited()!=null)row.putUUID("credited",v.credited());rows.add(row);});tag.put("items",rows);return tag;}
 public static RepairEvidence load(CompoundTag tag){
  if(!tag.getAllKeys().equals(Set.of("schema","identity","items"))||!tag.contains("schema",Tag.TAG_INT)||tag.getInt("schema")!=1||!tag.hasUUID("identity")||!(tag.get("items") instanceof ListTag rows)||rows.size()>LIMIT||!rows.isEmpty()&&rows.getElementType()!=Tag.TAG_COMPOUND)throw new IllegalArgumentException("Invalid repair evidence schema");var data=new RepairEvidence(tag.getUUID("identity"));
  for(var value:rows){var row=(CompoundTag)value;boolean claimed=row.contains("credited");if(!row.getAllKeys().equals(claimed?Set.of("id","wearer","item","damage","band","credited"):Set.of("id","wearer","item","damage","band"))||!row.hasUUID("id")||!row.hasUUID("wearer")||!row.contains("item",Tag.TAG_STRING)||!row.contains("damage",Tag.TAG_INT)||!row.contains("band",Tag.TAG_STRING)||claimed&&!row.hasUUID("credited")||claimed==row.getString("band").isEmpty())throw new IllegalArgumentException("Invalid native repair proof");var item=new Item(row.getUUID("id"),row.getUUID("wearer"),row.getString("item"),row.getInt("damage"),claimed?row.getUUID("credited"):null,claimed?ProfessionRank.byId(row.getString("band")):null);if(data.items.put(item.id(),item)!=null)throw new IllegalArgumentException("Duplicate native item identity");}
  return data;
 }
}
