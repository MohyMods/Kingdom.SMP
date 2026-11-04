package com.mohymods.kingdomsmp.profession;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;

/** One saved boundary for native tree provenance, harvest counters and once-only replacement recognition. */
public final class TreeEvidence {
    public static final int MAX_TREES=16384,MAX_LOGS=131072,MAX_PLANTS=8192,MAX_CLEARINGS=8192,MAX_NATURAL_LOGS=98304,MAX_NATURAL_TREES=12288;
    public static final Set<String> SPECIES=Set.of("oak","birch","spruce","jungle","acacia","dark_oak","cherry","mangrove");
    public static final Set<String> METRICS=metrics();
    private static Set<String> metrics(){Set<String> values=new HashSet<>(SPECIES);values.add("grown");for(var species:SPECIES)values.add("mature_"+species);return Set.copyOf(values);}
    public record Cell(String dimension,long position){public Cell{TreeEvidence.dimension(dimension);}}
    public record Plant(UUID id,UUID player,ProfessionRank band,String dimension,long position,String sapling,UUID clearing,int tier){
        public Plant{if(tier<0||tier>12)throw new IllegalArgumentException("Invalid planting tier");Objects.requireNonNull(id);Objects.requireNonNull(player);TreeEvidence.dimension(dimension);if(!sapling.matches("minecraft:(oak|birch|spruce|jungle|acacia|dark_oak|cherry)_sapling|minecraft:mangrove_propagule"))throw new IllegalArgumentException("Unsupported native sapling");}
    }
    public record Clearing(UUID id,UUID player,ProfessionRank band,String dimension,long position){public Clearing{Objects.requireNonNull(id);Objects.requireNonNull(player);Objects.requireNonNull(band);TreeEvidence.dimension(dimension);}}
    public record Growth(UUID id,UUID player,ProfessionRank band,String dimension,long position) { }
    public record Log(String state,String species){public Log{if(state==null||state.isBlank()||state.length()>256||!SPECIES.contains(species))throw new IllegalArgumentException("Invalid native tree log");}}
    private record Harvester(UUID player,ProfessionRank band) { }
    private static final class Cultivation {
        final UUID planter;final ProfessionRank band;final String species;boolean harvested;
        Cultivation(UUID planter,ProfessionRank band,String species,boolean harvested){this.planter=Objects.requireNonNull(planter);this.band=Objects.requireNonNull(band);if(!SPECIES.contains(species))throw new IllegalArgumentException("Unknown cultivated species");this.species=species;this.harvested=harvested;}
    }
    private static final class Tree {
        final UUID id;final String dimension;final long origin;final boolean planted;
        final Map<Long,Log> logs=new HashMap<>();final Set<Harvester> harvesters=new HashSet<>();Cultivation cultivation;
        Tree(UUID id,String dimension,long origin,boolean planted){this.id=id;this.dimension=dimension;this.origin=origin;this.planted=planted;}
    }
    private final Map<UUID,Tree> trees=new HashMap<>();
    private final Map<Cell,UUID> index=new HashMap<>();
    private final Map<Cell,Plant> plants=new HashMap<>();
    private final Map<UUID,Clearing> clearings=new HashMap<>();
    private final Map<UUID,Growth> growth=new HashMap<>();
    public List<Growth> growth(){return List.copyOf(growth.values());}
    public boolean confirmGrowth(UUID id,UUID player){var g=growth.get(id);if(g==null||!g.player().equals(player))return false;evidence.record(player,g.band(),"grown",1,id.toString());growth.remove(id);return true;}
    private int naturalLogs,naturalTrees;
    private ProfessionEvidence evidence=new ProfessionEvidence(Profession.LUMBERJACK,METRICS);
    public ProfessionEvidence evidence(){return evidence;}
    public int logs(){return index.size();}public int trees(){return trees.size();}public int plants(){return plants.size();}
    public Map<Cell,Log> sampleNatural(int limit){if(limit<0||limit>128)throw new IllegalArgumentException("Invalid sample bound");Map<Cell,Log> sample=new HashMap<>();for(var tree:trees.values())if(!tree.planted)for(var entry:tree.logs.entrySet()){if(sample.size()>=limit)return Map.copyOf(sample);sample.put(new Cell(tree.dimension,entry.getKey()),entry.getValue());}return Map.copyOf(sample);}
    public Map<Cell,Log> plantedNear(String dim,BlockPos center){Map<Cell,Log> sample=new HashMap<>();for(var tree:trees.values())if(tree.planted&&tree.dimension.equals(dim)&&Math.abs(BlockPos.of(tree.origin).getX()-center.getX())<=8&&Math.abs(BlockPos.of(tree.origin).getZ()-center.getZ())<=8)for(var entry:tree.logs.entrySet()){if(sample.size()>=128)return Map.copyOf(sample);sample.put(new Cell(dim,entry.getKey()),entry.getValue());}return Map.copyOf(sample);}
    public Map<Cell,Log> cultivated(UUID planter,boolean harvested,int limit){if(limit<0||limit>128)throw new IllegalArgumentException("Invalid cultivation sample bound");Map<Cell,Log> result=new HashMap<>();for(var tree:trees.values())if(tree.cultivation!=null&&tree.cultivation.planter.equals(planter)&&tree.cultivation.harvested==harvested)for(var entry:tree.logs.entrySet()){if(result.size()>=limit)return Map.copyOf(result);result.put(new Cell(tree.dimension,entry.getKey()),entry.getValue());}return Map.copyOf(result);}
    public Optional<Plant> plantAt(String dimension,BlockPos pos){return Optional.ofNullable(plants.get(new Cell(dimension,pos.asLong())));}
    public Optional<Log> logAt(String dimension,BlockPos pos){var id=index.get(new Cell(dimension,pos.asLong()));var tree=trees.get(id);return tree==null?Optional.empty():Optional.ofNullable(tree.logs.get(pos.asLong()));}
    public boolean plant(UUID player,ProfessionRank band,String dimension,BlockPos pos,String sapling){return plant(player,band,dimension,pos,sapling,0);}
    public boolean plant(UUID player,ProfessionRank band,String dimension,BlockPos pos,String sapling,int tier){
        var key=new Cell(dimension,pos.asLong());if(!plants.containsKey(key)&&plants.size()>=MAX_PLANTS)return false;
        plants.remove(key);Set<UUID> reserved=new HashSet<>();for(var value:plants.values())if(value.clearing()!=null)reserved.add(value.clearing());
        UUID clearing=band==null?null:clearings.values().stream().filter(c->c.player().equals(player)&&c.band()==band&&c.dimension().equals(dimension)&&!reserved.contains(c.id())&&Math.abs(BlockPos.of(c.position()).getY()-pos.getY())<=4&&Math.abs(BlockPos.of(c.position()).getX()-pos.getX())<=8&&Math.abs(BlockPos.of(c.position()).getZ()-pos.getZ())<=8).sorted(Comparator.comparing(Clearing::id)).map(Clearing::id).findFirst().orElse(null);
        plants.put(key,new Plant(UUID.randomUUID(),player,band,dimension,pos.asLong(),sapling,clearing,tier));return true;
    }
    public boolean discardPlant(String dimension,BlockPos pos){return plants.remove(new Cell(dimension,pos.asLong()))!=null;}
    public boolean register(UUID id,String dimension,BlockPos origin,Map<Long,Log> logs,Plant planted,boolean creditGrow){
        Objects.requireNonNull(id);TreeEvidence.dimension(dimension);if(logs.isEmpty()||logs.size()>2048||trees.containsKey(id)||trees.size()>=MAX_TREES||index.size()+logs.size()>MAX_LOGS||planted==null&&(naturalLogs+logs.size()>MAX_NATURAL_LOGS||naturalTrees>=MAX_NATURAL_TREES))return false;
        if(planted!=null&&(!planted.dimension().equals(dimension)||Math.abs(BlockPos.of(planted.position()).getX()-origin.getX())>1||Math.abs(BlockPos.of(planted.position()).getZ()-origin.getZ())>1||BlockPos.of(planted.position()).getY()!=origin.getY()||!planted.equals(plants.get(new Cell(planted.dimension(),planted.position())))))return false;
        for(var position:logs.keySet())invalidate(dimension,BlockPos.of(position));
        var tree=new Tree(id,dimension,origin.asLong(),planted!=null);tree.logs.putAll(logs);trees.put(id,tree);if(!tree.planted){naturalLogs+=logs.size();naturalTrees++;}logs.keySet().forEach(pos->index.put(new Cell(dimension,pos),id));
        if(planted!=null){
            if(creditGrow&&planted.band()!=null&&planted.tier()>=ApprovedProfessionContent.get(Profession.LUMBERJACK.questId(planted.band(),1)).minimumTier()){
                var species=logs.values().stream().map(Log::species).distinct().toList();if(species.size()==1)tree.cultivation=new Cultivation(planted.player(),planted.band(),species.getFirst(),false);
            }
            plants.remove(new Cell(planted.dimension(),planted.position()));
            var clearing=clearings.get(planted.clearing());
            if(creditGrow&&planted.band()!=null&&clearing!=null&&clearing.player().equals(planted.player())&&clearing.band()==planted.band()){
                if(growth.size()<MAX_CLEARINGS)growth.putIfAbsent(clearing.id(),new Growth(clearing.id(),planted.player(),planted.band(),dimension,planted.position()));clearings.remove(clearing.id());
            }
        }
        return true;
    }
    private Tree remove(String dimension,BlockPos pos){var id=index.remove(new Cell(dimension,pos.asLong()));var tree=trees.get(id);if(tree!=null){tree.logs.remove(pos.asLong());if(!tree.planted)naturalLogs--;}return tree;}
    private void finished(Tree tree,boolean harvested){
        if(tree==null||!tree.logs.isEmpty())return;
        if(trees.remove(tree.id)!=null&&!tree.planted)naturalTrees--;
        if(harvested)for(var actor:tree.harvesters)if(clearings.size()<MAX_CLEARINGS){var id=UUID.nameUUIDFromBytes((tree.id+"/"+actor.player()+"/"+actor.band().id()).getBytes(java.nio.charset.StandardCharsets.UTF_8));clearings.putIfAbsent(id,new Clearing(id,actor.player(),actor.band(),tree.dimension,tree.origin));}
    }
    /** Other placements, machines and unknown removals invalidate rather than grant harvest credit. */
    public boolean invalidate(String dimension,BlockPos pos){var tree=remove(dimension,pos);finished(tree,false);return tree!=null;}
    public boolean harvest(UUID player,ProfessionRank band,String dimension,BlockPos pos,String state){
        var log=logAt(dimension,pos).orElse(null);if(log==null)return false;
        var tree=remove(dimension,pos);if(!log.state().equals(state)){finished(tree,false);return false;}
        evidence.record(player,band,log.species(),1,null);
        var cultivated=tree.cultivation;if(cultivated!=null&&!cultivated.harvested&&cultivated.planter.equals(player)&&cultivated.band==band&&cultivated.species.equals(log.species())){evidence.record(player,band,"mature_"+log.species(),1,tree.id.toString());cultivated.harvested=true;}
        if(tree.harvesters.size()<128)tree.harvesters.add(new Harvester(player,band));finished(tree,true);return true;
    }
    public boolean transform(String dimension,BlockPos pos,String before,String after){
        var id=index.get(new Cell(dimension,pos.asLong()));var tree=trees.get(id);if(tree==null)return false;var log=tree.logs.get(pos.asLong());
        if(!log.state().equals(before))return invalidate(dimension,pos);tree.logs.put(pos.asLong(),new Log(after,log.species()));return true;
    }
    private static void dimension(String id){if(id==null||id.length()>128||!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))throw new IllegalArgumentException("Invalid tree dimension");}
    public CompoundTag save(){
        var root=new CompoundTag();root.putInt("schema",1);root.put("evidence",evidence.save());var encodedTrees=new ListTag();
        trees.values().stream().sorted(Comparator.comparing(t->t.id)).forEach(tree->{var row=new CompoundTag();row.putUUID("id",tree.id);row.putString("dimension",tree.dimension);row.putLong("origin",tree.origin);row.putBoolean("planted",tree.planted);var logs=new ListTag();tree.logs.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->{var log=new CompoundTag();log.putLong("position",e.getKey());log.putString("state",e.getValue().state());log.putString("species",e.getValue().species());logs.add(log);});row.put("logs",logs);var actors=new ListTag();tree.harvesters.stream().sorted(Comparator.comparing((Harvester h)->h.player()).thenComparing(h->h.band().ordinal())).forEach(h->{var a=new CompoundTag();a.putUUID("player",h.player());a.putString("band",h.band().id());actors.add(a);});row.put("harvesters",actors);if(tree.cultivation!=null){var c=new CompoundTag();c.putUUID("planter",tree.cultivation.planter);c.putString("band",tree.cultivation.band.id());c.putString("species",tree.cultivation.species);c.putBoolean("harvested",tree.cultivation.harvested);row.put("cultivation",c);}encodedTrees.add(row);});root.put("trees",encodedTrees);
        var encodedPlants=new ListTag();plants.values().stream().sorted(Comparator.comparing(Plant::id)).forEach(p->{var t=new CompoundTag();t.putUUID("id",p.id());t.putUUID("player",p.player());t.putString("band",p.band()==null?"":p.band().id());t.putString("dimension",p.dimension());t.putLong("position",p.position());t.putString("sapling",p.sapling());if(p.clearing()!=null)t.putUUID("clearing",p.clearing());if(p.tier()>0)t.putInt("tier",p.tier());encodedPlants.add(t);});root.put("plants",encodedPlants);
        var encodedClearings=new ListTag();clearings.values().stream().sorted(Comparator.comparing(Clearing::id)).forEach(c->{var t=new CompoundTag();t.putUUID("id",c.id());t.putUUID("player",c.player());t.putString("band",c.band().id());t.putString("dimension",c.dimension());t.putLong("position",c.position());encodedClearings.add(t);});root.put("clearings",encodedClearings);var pending=new ListTag();growth.values().stream().sorted(Comparator.comparing(Growth::id)).forEach(g->{var t=new CompoundTag();t.putUUID("id",g.id());t.putUUID("player",g.player());t.putString("band",g.band().id());t.putString("dimension",g.dimension());t.putLong("position",g.position());pending.add(t);});root.put("growth",pending);return root;
    }
    private static ListTag list(CompoundTag t,String key,int cap){if(!(t.get(key) instanceof ListTag rows)||rows.size()>cap||!rows.isEmpty()&&rows.getElementType()!=Tag.TAG_COMPOUND)throw new IllegalArgumentException("Invalid tree "+key);return rows;}
    private static void keys(CompoundTag t,Set<String> expected){if(!t.getAllKeys().equals(expected))throw new IllegalArgumentException("Invalid tree record keys");}
    private static UUID uuid(CompoundTag t,String key){if(!t.hasUUID(key))throw new IllegalArgumentException("Invalid tree UUID");return t.getUUID(key);}
    private static String string(CompoundTag t,String key){if(!t.contains(key,Tag.TAG_STRING))throw new IllegalArgumentException("Invalid tree string");return t.getString(key);}
    private static long position(CompoundTag t,String key){if(!t.contains(key,Tag.TAG_LONG))throw new IllegalArgumentException("Invalid tree position");return t.getLong(key);}
    public static TreeEvidence load(CompoundTag root){
        keys(root,Set.of("schema","evidence","trees","plants","clearings","growth"));if(!root.contains("schema",Tag.TAG_INT)||root.getInt("schema")!=1||!root.contains("evidence",Tag.TAG_COMPOUND))throw new IllegalArgumentException("Invalid tree schema");var data=new TreeEvidence();data.evidence=ProfessionEvidence.load(root.getCompound("evidence"),Profession.LUMBERJACK,METRICS);
        for(var value:list(root,"trees",MAX_TREES)){var t=(CompoundTag)value;keys(t,t.contains("cultivation")?Set.of("id","dimension","origin","planted","logs","harvesters","cultivation"):Set.of("id","dimension","origin","planted","logs","harvesters"));String dim=string(t,"dimension");dimension(dim);if(!t.contains("planted",Tag.TAG_BYTE)||t.getByte("planted")<0||t.getByte("planted")>1)throw new IllegalArgumentException("Invalid tree source");var tree=new Tree(uuid(t,"id"),dim,position(t,"origin"),t.getBoolean("planted"));if(data.trees.put(tree.id,tree)!=null)throw new IllegalArgumentException("Duplicate tree");
            for(var v:list(t,"logs",2048)){var l=(CompoundTag)v;keys(l,Set.of("position","state","species"));long pos=position(l,"position");if(tree.logs.put(pos,new Log(string(l,"state"),string(l,"species")))!=null||data.index.put(new Cell(dim,pos),tree.id)!=null||data.index.size()>MAX_LOGS)throw new IllegalArgumentException("Duplicate/excess tree logs");}
            if(t.contains("cultivation")){if(!tree.planted||!t.contains("cultivation",Tag.TAG_COMPOUND))throw new IllegalArgumentException("Invalid cultivated source");var c=t.getCompound("cultivation");keys(c,Set.of("planter","band","species","harvested"));if(!c.contains("harvested",Tag.TAG_BYTE)||c.getByte("harvested")<0||c.getByte("harvested")>1)throw new IllegalArgumentException("Invalid cultivated harvest marker");tree.cultivation=new Cultivation(uuid(c,"planter"),ProfessionRank.byId(string(c,"band")),string(c,"species"),c.getBoolean("harvested"));if(tree.logs.values().stream().anyMatch(log->!log.species().equals(tree.cultivation.species)))throw new IllegalArgumentException("Cultivated species differs from native logs");}
            if(tree.logs.isEmpty())throw new IllegalArgumentException("Empty live tree");if(!tree.planted){data.naturalLogs+=tree.logs.size();data.naturalTrees++;if(data.naturalLogs>MAX_NATURAL_LOGS||data.naturalTrees>MAX_NATURAL_TREES)throw new IllegalArgumentException("Natural provenance exceeds reserved capacity");}for(var v:list(t,"harvesters",128)){var h=(CompoundTag)v;keys(h,Set.of("player","band"));if(!tree.harvesters.add(new Harvester(uuid(h,"player"),ProfessionRank.byId(string(h,"band")))))throw new IllegalArgumentException("Duplicate harvester");}
        }
        for(var value:list(root,"clearings",MAX_CLEARINGS)){var t=(CompoundTag)value;keys(t,Set.of("id","player","band","dimension","position"));var c=new Clearing(uuid(t,"id"),uuid(t,"player"),ProfessionRank.byId(string(t,"band")),string(t,"dimension"),position(t,"position"));if(data.clearings.put(c.id(),c)!=null)throw new IllegalArgumentException("Duplicate clearing");}
        Set<UUID> used=new HashSet<>();for(var value:list(root,"plants",MAX_PLANTS)){var t=((CompoundTag)value).copy();int plantingTier=0;if(t.contains("tier")){if(!t.contains("tier",Tag.TAG_INT)||t.getInt("tier")<1||t.getInt("tier")>12)throw new IllegalArgumentException("Invalid planting tier");plantingTier=t.getInt("tier");t.remove("tier");}Set<String> expected=new HashSet<>(Set.of("id","player","band","dimension","position","sapling"));if(t.contains("clearing"))expected.add("clearing");keys(t,expected);String band=string(t,"band");var p=new Plant(uuid(t,"id"),uuid(t,"player"),band.isEmpty()?null:ProfessionRank.byId(band),string(t,"dimension"),position(t,"position"),string(t,"sapling"),t.contains("clearing")?uuid(t,"clearing"):null,plantingTier);if(data.plants.put(new Cell(p.dimension(),p.position()),p)!=null)throw new IllegalArgumentException("Duplicate planted position");if(p.clearing()!=null){var c=data.clearings.get(p.clearing());if(c==null||!c.player().equals(p.player())||c.band()!=p.band()||!c.dimension().equals(p.dimension())||!used.add(c.id()))throw new IllegalArgumentException("Invalid repeated replacement reservation");}}
        for(var value:list(root,"growth",MAX_CLEARINGS)){var t=(CompoundTag)value;keys(t,Set.of("id","player","band","dimension","position"));String dim=string(t,"dimension");dimension(dim);var g=new Growth(uuid(t,"id"),uuid(t,"player"),ProfessionRank.byId(string(t,"band")),dim,position(t,"position"));if(data.growth.put(g.id(),g)!=null||data.clearings.containsKey(g.id()))throw new IllegalArgumentException("Duplicate growth receipt");}
        return data;
    }
}
