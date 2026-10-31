package com.mohymods.kingdomsmp.milestone;

import java.util.*;
import java.util.function.Function;
import static com.mohymods.kingdomsmp.milestone.MilestoneCatalog.*;

/** Declared dependency graph plus independently known vanilla acquisition prerequisites. */
public final class MilestoneValidator {
    private static final String K="kingdom_smp:";
    public static final Map<String,Integer> VERIFIED_CAPABILITIES=Map.ofEntries(
            Map.entry(K+"overworld",1),Map.entry(K+"crafting",1),Map.entry(K+"smelting",1),
            Map.entry(K+"agriculture",1),Map.entry(K+"animal_husbandry",1),Map.entry(K+"mining",1),
            Map.entry(K+"nether",5),Map.entry(K+"end",9),Map.entry(K+"founders_lodge",2));
    private static final Map<String,Set<String>> SOURCES=vanillaSources();
    private MilestoneValidator(){}
    public record Report(Map<Integer,List<String>> issues,List<String> catalogIssues){
        public Report {Map<Integer,List<String>> copy=new TreeMap<>();issues.forEach((k,v)->copy.put(k,List.copyOf(v)));issues=Collections.unmodifiableMap(copy);catalogIssues=List.copyOf(catalogIssues);}
        public List<String> issues(int tier){return issues.getOrDefault(tier,List.of());}
    }
    /** Resolver returns actual registered item IDs; an absent item/empty tag returns an empty set. */
    public static Report validate(MilestoneCatalog catalog,Function<String,Set<String>> resolver){
        Map<String,Capability> caps=new LinkedHashMap<>();catalog.capabilities().forEach(c->caps.put(c.id(),c));
        Map<String,Milestone> milestones=new LinkedHashMap<>();catalog.milestones().forEach(m->milestones.put(m.id(),m));
        Map<String,List<String>> graph=new LinkedHashMap<>();
        for(Capability c:caps.values()){
            List<String> edges=new ArrayList<>();c.requires().forEach(id->edges.add("c:"+id));c.afterMilestones().forEach(id->edges.add("m:"+id));
            if(c.availableFromTier()>1)edges.add("m:"+catalog.tier(c.availableFromTier()-1).id());
            graph.put("c:"+c.id(),edges);
        }
        Map<Integer,List<String>> result=new LinkedHashMap<>();
        for(Milestone m:catalog.milestones()){
            List<String> edges=new ArrayList<>();if(m.tier()>1)edges.add("m:"+catalog.tier(m.tier()-1).id());
            for(Requirement r:m.requirements()){
                Set<String> needs=new LinkedHashSet<>(r.requires());needs.addAll(SOURCES.getOrDefault(r.ingredient(),Set.of()));
                needs.forEach(id->edges.add("c:"+id));
            }
            m.objectives().forEach(o->o.requires().forEach(id->edges.add("c:"+id)));
            graph.put("m:"+m.id(),edges);
        }
        for(Milestone m:catalog.milestones()){
            Set<String> issues=new LinkedHashSet<>();
            Map<String,String> matched=new HashMap<>();
            for(Requirement r:m.requirements()){
                Set<String> items=resolver.apply(r.ingredient());
                if(items.isEmpty())issues.add("Unresolved item or empty tag: "+r.ingredient());
                for(String item:items){String previous=matched.putIfAbsent(item,r.id());if(previous!=null)issues.add("Overlapping ingredients: "+previous+" and "+r.id());}
                if(!SOURCES.containsKey(r.ingredient()))issues.add("Acquisition path not verified: "+r.ingredient());
            }
            walk("m:"+m.id(),m.tier(),graph,caps,new ArrayList<>(),new HashSet<>(),issues);
            if(m.tier()>1)issues.add("Proposed balance and scoring are not approved for activation.");
            result.put(m.tier(),List.copyOf(issues));
        }
        Set<String> structural=new LinkedHashSet<>();
        for(String node:graph.keySet()){
            Set<String> notes=new LinkedHashSet<>();walk(node,12,graph,caps,new ArrayList<>(),new HashSet<>(),notes);
            notes.stream().filter(s->s.startsWith("Circular progression:")||s.startsWith("Unknown prerequisite:")).forEach(structural::add);
        }
        return new Report(result,List.copyOf(structural));
    }
    private static void walk(String node,int tier,Map<String,List<String>> graph,Map<String,Capability> caps,List<String> path,Set<String> done,Set<String> issues){
        if(path.contains(node)){List<String> cycle=new ArrayList<>(path.subList(path.indexOf(node),path.size()));cycle.add(node);issues.add("Circular progression: "+String.join(" -> ",cycle));return;}
        if(!graph.containsKey(node)){issues.add("Unknown prerequisite: "+node);return;}
        if(!done.add(node))return;
        if(node.startsWith("c:")){
            Capability c=caps.get(node.substring(2));Integer verified=VERIFIED_CAPABILITIES.get(c.id());
            if(verified==null)issues.add("Unverified technology/profession capability: "+c.id());
            else if(c.availableFromTier()!=verified)issues.add("Fixed unlock tier changed: "+c.id()+" must be "+verified);
            int earliest=verified==null?c.availableFromTier():verified;
            if(earliest==0)issues.add("Unlock tier undecided: "+c.id());
            else if(earliest>tier)issues.add("Inaccessible while gathering at Tier "+tier+": "+c.id()+" requires Tier "+earliest);
        }
        path.add(node);for(String dependency:graph.get(node))walk(dependency,tier,graph,caps,path,done,issues);path.removeLast();
    }
    /** Explicit conservative profiles, not a claim to infer arbitrary recipes or mod acquisition routes. */
    private static Map<String,Set<String>> vanillaSources(){
        Map<String,Set<String>> m=new LinkedHashMap<>();
        m.put("#minecraft:logs",Set.of(K+"overworld"));
        add(m,"overworld","cobblestone","paper","book","bookshelf","amethyst_shard","amethyst_block","prismarine_crystals","emerald");
        add(m,"mining","iron_ingot","copper_ingot","gold_ingot","diamond","redstone","lapis_lazuli","obsidian","diamond_block","lapis_block","copper_block","iron_block","redstone_block","gold_block","deepslate_bricks","sea_lantern");
        add(m,"smelting","glass","brick","stone_bricks","smooth_stone");add(m,"agriculture","wheat");add(m,"animal_husbandry","leather");
        add(m,"nether","quartz","nether_bricks","blaze_rod","soul_sand","magma_cream","quartz_block","glowstone","blaze_powder","crying_obsidian","netherite_ingot","nether_star","beacon","smooth_quartz","gilded_blackstone");
        add(m,"end","end_stone","purpur_block","shulker_shell","dragon_breath","end_rod","purpur_pillar");
        return Map.copyOf(m);
    }
    private static void add(Map<String,Set<String>> m,String capability,String...items){for(String item:items)m.put("minecraft:"+item,Set.of(K+capability));}
}
