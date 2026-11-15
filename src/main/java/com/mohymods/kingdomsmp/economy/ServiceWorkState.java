package com.mohymods.kingdomsmp.economy;

import com.mohymods.kingdomsmp.profession.*;
import net.minecraft.nbt.*;
import java.util.*;

/** Canonical replay projection only. Current ownership is explicitly granted, never inferred from a wearer. */
public final class ServiceWorkState {
 public static final String SMITH="kingdom_smp:blacksmith/m05",LIBRARY="kingdom_smp:librarian/j06";
 public static boolean supported(String quest){return SMITH.equals(quest)||LIBRARY.equals(quest);}
 public static Profession role(String quest){if(!supported(quest))throw new IllegalArgumentException("Unsupported service purpose");return SMITH.equals(quest)?Profession.BLACKSMITH:Profession.LIBRARIAN;}
 public static long xp(String quest){return SMITH.equals(quest)?300:200;}
 public record Consent(UUID id,UUID owner,UUID provider,String quest,UUID wearStore,UUID wear,CompoundTag input,boolean active,boolean returned){
  public Consent{Objects.requireNonNull(id);Objects.requireNonNull(owner);Objects.requireNonNull(provider);Objects.requireNonNull(wearStore);Objects.requireNonNull(wear);if(owner.equals(provider)||!supported(quest)||!RepairEvidence.eligible(input.getString("id"))||input.getInt("count")!=1||damage(input)<1)throw new IllegalArgumentException("Invalid current-owner service consent");var marker=input.getCompound("components").getCompound("minecraft:custom_data").getCompound(RepairService.KEY);if(!marker.hasUUID("store")||!marker.hasUUID("id")||!marker.getUUID("store").equals(wearStore)||!marker.getUUID("id").equals(wear))throw new IllegalArgumentException("Wear identity differs from the exact owner inventory item");input=input.copy();}
  @Override public CompoundTag input(){return input.copy();}
 }
 public record Output(UUID id,UUID consent,CompoundTag item,boolean spent){public Output{item=item.copy();}@Override public CompoundTag item(){return item.copy();}}
 private record Source(UUID store,UUID item) { }
 private final Map<UUID,Consent> consents=new LinkedHashMap<>();private final Map<UUID,Output> outputs=new LinkedHashMap<>();private final Set<Source> spent=new HashSet<>();
 public ServiceWorkState copy(){var n=new ServiceWorkState();n.consents.putAll(consents);n.outputs.putAll(outputs);n.spent.addAll(spent);return n;}
 public boolean spentSource(UUID store,UUID item){return spent.contains(new Source(store,item));}
 public Optional<Consent> consent(UUID id){return Optional.ofNullable(consents.get(id));}
 public Optional<Output> output(UUID id){return Optional.ofNullable(outputs.get(id));}
 public List<Consent> history(UUID actor,int limit){if(limit<1||limit>16)throw new IllegalArgumentException("Service history limit1..16");return consents.values().stream().filter(c->c.owner().equals(actor)||c.provider().equals(actor)).sorted(Comparator.comparing(Consent::id)).limit(limit).toList();}
 public List<Consent> owned(UUID owner){return consents.values().stream().filter(c->c.owner().equals(owner)&&c.active()&&!c.returned()).toList();}
 public Optional<Consent> forInput(UUID provider,CompoundTag input){return consents.values().stream().filter(c->c.active()&&!c.returned()&&c.provider().equals(provider)&&c.input().equals(input)).findFirst();}
 public void authorize(Consent c){if(consents.size()>=4096||consents.containsKey(c.id())||spent.contains(new Source(c.wearStore(),c.wear()))||consents.values().stream().anyMatch(v->v.active()&&!v.returned()&&v.wearStore().equals(c.wearStore())&&v.wear().equals(c.wear())))throw new IllegalStateException("Source already spent/authorized or service capacity reached");consents.put(c.id(),c);}
 public void revoke(UUID id,UUID owner){var c=consents.get(id);if(c==null||!c.owner().equals(owner)||!c.active()||c.returned())throw new IllegalStateException("No active consent owned by this player");consents.put(id,new Consent(c.id(),c.owner(),c.provider(),c.quest(),c.wearStore(),c.wear(),c.input(),false,false));}
 public void issue(UUID id,UUID consent,UUID provider,CompoundTag item,int materials,int cost){var c=consents.get(consent);if(c==null||!c.active()||c.returned()||!c.provider().equals(provider)||outputs.size()>=4096||outputs.containsKey(id)||materials<1||materials>64||cost<1||cost>=40||item.getInt("count")!=1||!c.input().getString("id").equals(item.getString("id"))||damage(item)<0||!improved(c.quest(),c.input(),item))throw new IllegalStateException("Unverified native service improvement");outputs.put(id,new Output(id,consent,item,false));}
 public Consent returnOutput(UUID id,UUID provider,CompoundTag item){var out=outputs.get(id);var c=out==null?null:consents.get(out.consent());if(out==null||out.spent()||!out.item().equals(item)||c==null||!c.active()||c.returned()||!c.provider().equals(provider)||!spent.add(new Source(c.wearStore(),c.wear())))throw new IllegalStateException("Unknown, altered, revoked or spent service output");outputs.put(id,new Output(id,out.consent(),item,true));consents.put(c.id(),new Consent(c.id(),c.owner(),c.provider(),c.quest(),c.wearStore(),c.wear(),c.input(),false,true));return c;}
 public int jobs(UUID provider,String quest){return (int)consents.values().stream().filter(c->c.returned()&&c.provider().equals(provider)&&c.quest().equals(quest)).count();}
 public int customers(UUID provider,String quest){return (int)consents.values().stream().filter(c->c.returned()&&c.provider().equals(provider)&&c.quest().equals(quest)).map(Consent::owner).distinct().count();}
 static boolean improved(String quest,CompoundTag before,CompoundTag after){
  if(SMITH.equals(quest))return damage(after)<damage(before)&&enchants(before).equals(enchants(after));
  if(damage(after)>damage(before))return false;var old=enchants(before).getCompound("levels");var next=enchants(after).getCompound("levels");if(old.isEmpty()||next.isEmpty()||next.size()>16)return false;
  for(var key:old.getAllKeys())if(next.getInt(key)<old.getInt(key))return false;boolean improvement=false;for(var key:next.getAllKeys()){int level=next.getInt(key);if(level<1||level>255)return false;if(level>old.getInt(key))improvement=true;}return improvement;
 }
 static int damage(CompoundTag item){return item.getCompound("components").getInt("minecraft:damage");}
 static CompoundTag enchants(CompoundTag item){return item.getCompound("components").getCompound("minecraft:enchantments");}
}
