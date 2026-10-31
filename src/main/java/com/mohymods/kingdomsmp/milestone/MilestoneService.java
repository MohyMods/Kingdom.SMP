package com.mohymods.kingdomsmp.milestone;

import com.mohymods.kingdomsmp.building.ConstructionService;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.kingdom.TierOneProgress;
import com.mohymods.kingdomsmp.persistence.ConstructionStore;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import java.io.*;
import java.util.*;
import static com.mohymods.kingdomsmp.milestone.MilestoneCatalog.*;

/** Main-thread catalogue publication. Draft data can be inspected but never consumed or awarded. */
public final class MilestoneService {
    public static final ResourceLocation FILE=ResourceLocation.fromNamespaceAndPath("kingdom_smp","kingdom/milestones.json");
    private static final Map<MinecraftServer,Snapshot> CATALOGS=new WeakHashMap<>();
    public record Snapshot(MilestoneCatalog catalog,MilestoneValidator.Report report,String error){}
    private MilestoneService(){}
    public static Snapshot current(MinecraftServer server){if(!CATALOGS.containsKey(server))reload(server);return CATALOGS.get(server);}
    public static void sync(OnDatapackSyncEvent event){if(event.getPlayer()==null)reload(event.getPlayerList().getServer());}
    public static void reload(MinecraftServer server){
        Snapshot snapshot;
        try(Reader reader=server.getResourceManager().getResource(FILE).orElseThrow(()->new IOException("Missing "+FILE)).openAsReader()){
            MilestoneCatalog catalog=MilestoneJson.read(reader);
            snapshot=new Snapshot(catalog,MilestoneValidator.validate(catalog,MilestoneService::resolve),"");
            LogUtils.getLogger().info("Milestone catalogue loaded: 12 definitions; later proposals remain disabled");
        }catch(IOException|RuntimeException e){snapshot=new Snapshot(null,null,"Milestone data invalid: "+String.valueOf(e.getMessage()));LogUtils.getLogger().error("Milestone data rejected; new donations paused, saved history preserved",e);}
        CATALOGS.put(server,snapshot);ContributionService.catalogueReloaded(server);
    }
    public static String consumptionDenial(MinecraftServer server){
        Snapshot s=current(server);if(s.catalog()==null)return "Milestone data is invalid. An administrator must correct the server datapack and reload; no resources were consumed.";
        if(!legacyCompatible(s.catalog().tier(1)))return "First Hearth data differs from its pinned historical contract. Migration is required; saved scores/rewards were preserved.";
        if(!s.report().catalogIssues().isEmpty())return "Milestone dependency graph is invalid. An administrator must inspect /kingdom milestones validate; no resources were consumed.";
        if(!s.report().issues(1).isEmpty())return "First Hearth prerequisites/items failed validation. No resources were consumed; inspect /kingdom milestones validate.";
        return "";
    }
    /** Fixed awarded Tier 1 semantics cannot be rewritten by a pack reload. */
    public static boolean legacyCompatible(Milestone m){
        if(!m.id().equals("kingdom_smp:first_hearth")||m.tier()!=1||!m.approval().equals("approved")||!m.scoringPolicy().equals("legacy_equal_four")||!m.reward().equals("kingdom_smp:founders_lodge")||!m.objectives().isEmpty()||m.requirements().size()!=4)return false;
        Map<String,Long> expected=Map.of("#minecraft:logs",1024L,"minecraft:cobblestone",2048L,"minecraft:wheat",512L,"minecraft:iron_ingot",128L);
        for(Requirement r:m.requirements())if(!Objects.equals(expected.get(r.ingredient()),r.quantity())||!r.id().equals(resourceId(r.ingredient())))return false;
        return true;
    }
    private static String resourceId(String ingredient){return "kingdom_smp:"+switch(ingredient){case "#minecraft:logs"->"logs";case "minecraft:cobblestone"->"cobblestone";case "minecraft:wheat"->"wheat";case "minecraft:iron_ingot"->"iron";default->"unknown";};}
    public static Set<String> resolve(String ingredient){
        boolean tag=ingredient.startsWith("#");ResourceLocation id=ResourceLocation.parse(tag?ingredient.substring(1):ingredient);
        Set<String> items=new HashSet<>();
        if(tag)BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM,id)).ifPresent(holders->holders.forEach(h->{if(h.value()!=Items.AIR)items.add(BuiltInRegistries.ITEM.getKey(h.value()).toString());}));
        else BuiltInRegistries.ITEM.getOptional(id).filter(item->item!=Items.AIR).ifPresent(item->items.add(id.toString()));
        return Set.copyOf(items);
    }
    /** Only matches IDs/tags already validated against the running server registry. */
    public static boolean matches(ItemStack stack,Requirement r){
        if(stack.isEmpty())return false;
        if(r.ingredient().startsWith("#"))return stack.is(TagKey.create(Registries.ITEM,ResourceLocation.parse(r.ingredient().substring(1))));
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(r.ingredient())).filter(item->item!=Items.AIR).map(stack::is).orElse(false);
    }
    public static List<String> descriptions(MinecraftServer server){
        Snapshot s=current(server);List<String> result=new ArrayList<>();
        for(int tier=1;tier<=12;tier++)result.add(limit(describe(server,s,tier),4096));return List.copyOf(result);
    }
    private static String describe(MinecraftServer server,Snapshot s,int tier){
        if(s.catalog()==null)return "Milestone requirements need administrator review. Saved history and rewards are unchanged; new donations are paused.";
        Milestone m=s.catalog().tier(tier);StringBuilder out=new StringBuilder();
        out.append(tier==1?"APPROVED · FIRST HEARTH":"PROPOSAL — NOT ENABLED").append("\n").append(m.title()).append(" · ").append(tier==12?"Tier 12 finale":"Tier "+tier+" → "+m.nextTier()).append('\n');
        var progress=ContributionService.ledger(server).progress();
        for(Requirement r:m.requirements()){
            String name=r.displayName().isBlank()?r.ingredient():r.displayName();
            out.append(name).append(": ");
            if(tier==1&&legacyCompatible(m)){var resource=switch(r.ingredient()){case "#minecraft:logs"->TierOneProgress.Resource.LOGS;case "minecraft:cobblestone"->TierOneProgress.Resource.COBBLESTONE;case "minecraft:wheat"->TierOneProgress.Resource.WHEAT;default->TierOneProgress.Resource.IRON_INGOTS;};out.append(progress.total(resource)).append(" / ");}
            out.append(r.quantity()).append(" · ").append(r.category().replace("kingdom_smp:","")).append('\n');
        }
        for(Objective o:m.objectives()){
            out.append(o.description().isBlank()?o.id():o.description());
            if(o.kind().equals("building")&&o.target().equals("kingdom_smp:founders_lodge")){
                var lodge=ConstructionService.lodge(server);out.append(lodge!=null&&lodge.phase()==ConstructionStore.Phase.COMPLETE?" — completed in this world":" — not completed");
            }else out.append(" — awaiting approved verification rules");out.append('\n');
        }
        out.append("Reward: ").append(switch(m.reward()){
            case "kingdom_smp:founders_lodge"->"Founders' Lodge Blueprint";
            case "kingdom_smp:royal_storehouse"->"Royal Storehouse Blueprint";
            case "kingdom_smp:town_hall"->"Ultimate Town Hall Blueprint (Tier 12; award policy pending)";
            default->"Awaiting approval";
        }).append('\n');
        if(tier>1)out.append("No donation, advancement, scoring or blueprint award is enabled for this proposal. Future scoring fairness awaits approval.\n");
        else if(!consumptionDenial(server).isEmpty())out.append(consumptionDenial(server)).append('\n');
        List<String> issues=s.report().issues(tier);if(!issues.isEmpty()){out.append("Validation notes:\n");for(String issue:issues.stream().limit(6).toList())out.append("• ").append(issue).append('\n');}
        else out.append("Registered materials and known vanilla prerequisites validated.\n");
        for(String issue:s.report().catalogIssues().stream().limit(2).toList())out.append(issue).append('\n');
        return out.toString();
    }
    private static String limit(String s,int n){return s.substring(0,Math.min(s.length(),n));}
    public static LiteralArgumentBuilder<CommandSourceStack> commands(){
        return Commands.literal("milestones")
                .then(Commands.literal("inspect").then(Commands.argument("tier",IntegerArgumentType.integer(1,12)).executes(c->{int tier=IntegerArgumentType.getInteger(c,"tier");c.getSource().sendSuccess(()->Component.literal(descriptions(c.getSource().getServer()).get(tier-1)),false);return 1;})))
                .then(Commands.literal("validate").requires(s->s.hasPermission(2)).executes(c->{
                    Snapshot s=current(c.getSource().getServer());if(s.catalog()==null){c.getSource().sendFailure(Component.literal(limit(s.error(),1000)));return 0;}
                    String active=consumptionDenial(c.getSource().getServer());c.getSource().sendSuccess(()->Component.literal(active.isEmpty()?"First Hearth contract validated. All later costs remain disabled.":active),false);
                    for(String issue:s.report().catalogIssues().stream().limit(16).toList())c.getSource().sendFailure(Component.literal(limit(issue,1000)));
                    for(int tier=1;tier<=12;tier++){String label="Tier "+tier+": ";List<String> issues=s.report().issues(tier);String line=label+(issues.isEmpty()?"known prerequisites/registry validated":String.join(" | ",issues));c.getSource().sendSuccess(()->Component.literal(limit(line,4000)),false);}
                    return 1;
                }));
    }
}
