package com.mohymods.kingdomsmp.fishing;
import net.minecraft.nbt.*;
import java.util.*;
/** Bounded non-financial personal collection and weekly species-diversity recognition. Never grants items or money. */
public final class FishingCatchBook {
 public static final List<String> SPECIES=List.of("bluegill","smallmouth_bass","brown_trout","carp","catfish","perch","atlantic_halibut","capitaine","arapaima","tuna");
 public static final Set<String> RARE=Set.of("catfish","atlantic_halibut","capitaine","arapaima","tuna");
 public record Week(Set<String> species,long reachedAt){public Week{species=Set.copyOf(species);}}
 public record Player(UUID id,String name,Set<String> collection,Map<Long,Week> weeks){public Player{collection=Set.copyOf(collection);weeks=Map.copyOf(weeks);}}
 public record Standing(UUID player,String name,int species,long reachedAt){}
 private final Map<UUID,Player> players=new HashMap<>();
 private final LinkedHashMap<UUID,Long> casts=new LinkedHashMap<>();
 public Optional<Player> player(UUID id){return Optional.ofNullable(players.get(id));}
 public List<Standing> standings(long week){return players.values().stream().filter(p->p.weeks().containsKey(week)).map(p->{var w=p.weeks().get(week);return new Standing(p.id(),p.name(),w.species().size(),w.reachedAt());}).sorted(Comparator.comparingInt(Standing::species).reversed().thenComparingLong(Standing::reachedAt).thenComparing(s->s.player().toString())).toList();}
 public boolean record(UUID player,String name,UUID cast,Set<String> caught,long week,long at){
  if(player==null||cast==null||name==null||!name.matches("[A-Za-z0-9_]{1,16}")||caught.isEmpty()||!SPECIES.containsAll(caught)||week<0||at<0)throw new IllegalArgumentException("Invalid fishing catch evidence");
  if(casts.containsKey(cast)||!players.containsKey(player)&&players.size()>=128)return false;
  var before=players.getOrDefault(player,new Player(player,name,Set.of(),Map.of()));var collection=new HashSet<>(before.collection());collection.addAll(caught);var weeks=new HashMap<>(before.weeks());long latest=Math.max(week,weeks.keySet().stream().mapToLong(Long::longValue).max().orElse(week));if(week<latest-7)return false;weeks.keySet().removeIf(w->w<latest-7);
  var old=weeks.getOrDefault(week,new Week(Set.of(),at));var species=new HashSet<>(old.species());species.addAll(caught);weeks.put(week,new Week(species,species.size()>old.species().size()?at:old.reachedAt()));players.put(player,new Player(player,name,collection,weeks));casts.put(cast,week);while(casts.size()>512)casts.remove(casts.keySet().iterator().next());return true;
 }
 public CompoundTag save(){
  var root=new CompoundTag();root.putInt("schema",1);var rows=new ListTag();players.values().stream().sorted(Comparator.comparing(p->p.id().toString())).forEach(p->{var row=new CompoundTag();row.putUUID("id",p.id());row.putString("name",p.name());row.put("collection",strings(p.collection()));var weeks=new ListTag();p.weeks().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->{var w=new CompoundTag();w.putLong("week",e.getKey());w.putLong("reachedAt",e.getValue().reachedAt());w.put("species",strings(e.getValue().species()));weeks.add(w);});row.put("weeks",weeks);rows.add(row);});root.put("players",rows);var recent=new ListTag();casts.forEach((id,week)->{var row=new CompoundTag();row.putUUID("id",id);row.putLong("week",week);recent.add(row);});root.put("casts",recent);return root;
 }
 public static FishingCatchBook load(CompoundTag root){
  exact(root,Set.of("schema","players","casts"));if(!root.contains("schema",Tag.TAG_INT)||root.getInt("schema")!=1)throw new IllegalArgumentException("Unsupported fishing collection schema");var book=new FishingCatchBook();
  for(var value:rows(root,"players",128,Tag.TAG_COMPOUND)){var row=(CompoundTag)value;exact(row,Set.of("id","name","collection","weeks"));if(!row.hasUUID("id")||!row.contains("name",Tag.TAG_STRING)||!row.getString("name").matches("[A-Za-z0-9_]{1,16}"))throw new IllegalArgumentException("Invalid collection player");Set<String> collection=species(row,"collection");Map<Long,Week> weeks=new HashMap<>();for(var v:rows(row,"weeks",8,Tag.TAG_COMPOUND)){var w=(CompoundTag)v;exact(w,Set.of("week","reachedAt","species"));if(!w.contains("week",Tag.TAG_LONG)||w.getLong("week")<0||!w.contains("reachedAt",Tag.TAG_LONG)||w.getLong("reachedAt")<0)throw new IllegalArgumentException("Invalid fishing week");var caught=species(w,"species");if(caught.isEmpty()||!collection.containsAll(caught)||weeks.put(w.getLong("week"),new Week(caught,w.getLong("reachedAt")))!=null)throw new IllegalArgumentException("Invalid fishing weekly proof");}var id=row.getUUID("id");if(book.players.put(id,new Player(id,row.getString("name"),collection,weeks))!=null)throw new IllegalArgumentException("Duplicate collection player");}
  for(var value:rows(root,"casts",512,Tag.TAG_COMPOUND)){var row=(CompoundTag)value;exact(row,Set.of("id","week"));if(!row.hasUUID("id")||!row.contains("week",Tag.TAG_LONG)||row.getLong("week")<0||book.casts.put(row.getUUID("id"),row.getLong("week"))!=null)throw new IllegalArgumentException("Invalid recent fishing cast");}return book;
 }
 private static ListTag strings(Set<String> values){var list=new ListTag();values.stream().sorted().forEach(s->list.add(StringTag.valueOf(s)));return list;}
 private static Set<String> species(CompoundTag tag,String key){Set<String> values=new HashSet<>();for(var value:rows(tag,key,SPECIES.size(),Tag.TAG_STRING)){String id=value.getAsString();if(!SPECIES.contains(id)||!values.add(id))throw new IllegalArgumentException("Invalid collected species");}return Set.copyOf(values);}
 private static void exact(CompoundTag tag,Set<String> keys){if(!tag.getAllKeys().equals(keys))throw new IllegalArgumentException("Invalid fishing evidence fields");}
 private static ListTag rows(CompoundTag tag,String key,int max,int type){if(!(tag.get(key) instanceof ListTag list)||list.size()>max||!list.isEmpty()&&list.getElementType()!=type)throw new IllegalArgumentException("Invalid fishing evidence rows");return list;}
}
