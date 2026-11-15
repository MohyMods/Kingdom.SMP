package com.mohymods.kingdomsmp.admin;

import com.google.gson.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.profession.FarmerInventoryExchange;
import com.mohymods.kingdomsmp.progression.*;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import java.nio.file.*;
import java.util.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;

/** Explicit opt-in isolated-world tools. No quest/rank/source proofs, currency or unique items are minted. */
public final class SandboxCommands {
    private record Offer(UUID nonce,UUID actor,UUID target,String item,int count,String reason,long issued,long revision,List<ItemStack> before,List<ItemStack> after) { }
    private static final Map<MinecraftServer,Map<UUID,Offer>> OFFERS=new WeakHashMap<>();
    private SandboxCommands() { }
    public static boolean enabled(Path file)throws java.io.IOException {
        if(!Files.exists(file,LinkOption.NOFOLLOW_LINKS))return false;
        if(Files.isSymbolicLink(file)||Files.size(file)>4096)throw new java.io.IOException("Unsafe sandbox configuration");
        try{var root=JsonParser.parseString(Files.readString(file)).getAsJsonObject();if(!root.keySet().equals(Set.of("schema","enabled","scope"))||root.get("schema").getAsBigDecimal().intValueExact()!=1||!root.get("enabled").isJsonPrimitive()||!root.get("enabled").getAsJsonPrimitive().isBoolean()||!root.get("scope").getAsString().equals("isolated_test_world"))throw new IllegalArgumentException("Expected explicit isolated-world schema");return root.get("enabled").getAsBoolean();}catch(RuntimeException e){throw new java.io.IOException("Malformed sandbox configuration; tools disabled",e);}
    }
    public static boolean allowedItem(String id){
        if(Set.of("minecraft:oak_log","minecraft:birch_log","minecraft:spruce_log","minecraft:cobblestone","minecraft:wheat","minecraft:iron_ingot","minecraft:copper_ingot","minecraft:glass","minecraft:brick","minecraft:leather","minecraft:raw_iron","minecraft:raw_copper","minecraft:raw_gold","minecraft:coal","minecraft:sand","minecraft:clay_ball","minecraft:paper","minecraft:stick","minecraft:string","minecraft:flint").contains(id))return true;
        for(int tier=3;tier<=5;tier++)if(EarlyTierRules.defaults(tier).containsKey(id))return true;return false;
    }
    private static void require(CommandSourceStack source)throws Exception{if(!source.hasPermission(4)||!source.getServer().isSameThread()||!enabled(source.getServer().getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-sandbox.json")))throw new IllegalStateException("Sandbox disabled. OP4 and explicit isolated-test-world configuration are required; never enable on a live economy.");}
    private static UUID actor(CommandSourceStack source){return source.getEntity()==null?new UUID(0,0):source.getEntity().getUUID();}
    public static LiteralArgumentBuilder<CommandSourceStack> commands(){return Commands.literal("sandbox").requires(s->s.hasPermission(4))
        .then(Commands.literal("status").executes(c->guard(c.getSource(),()->{require(c.getSource());c.getSource().sendSuccess(()->Component.literal("ISOLATED TEST WORLD sandbox enabled. Supplies do not grant quest, source or rank proofs. Native milestones and construction rules remain enforced."),false);return 1;})))
        .then(Commands.literal("supplies").then(Commands.argument("player",EntityArgument.player()).then(Commands.argument("item",ResourceLocationArgument.id()).then(Commands.argument("count",IntegerArgumentType.integer(1,2304)).then(Commands.argument("reason",StringArgumentType.greedyString()).executes(c->guard(c.getSource(),()->{
            var q=review(c.getSource(),EntityArgument.getPlayer(c,"player"),ResourceLocationArgument.getId(c,"item").toString(),IntegerArgumentType.getInteger(c,"count"),StringArgumentType.getString(c,"reason"));
            c.getSource().sendSuccess(()->Component.literal("ISOLATED SANDBOX: grant "+q.count()+" "+q.item()+" to "+q.target()+". Confirm within30seconds: ").append(Component.literal("[Confirm test supplies]").withStyle(s->s.withColor(net.minecraft.ChatFormatting.GOLD).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/kingdom admin sandbox confirm "+q.nonce())))),false);return 1;
        })))))))
        .then(Commands.literal("confirm").then(Commands.argument("nonce",UuidArgument.uuid()).executes(c->guard(c.getSource(),()->confirm(c.getSource(),UuidArgument.getUuid(c,"nonce"))))));}
    private static Offer review(CommandSourceStack source,ServerPlayer p,String id,int count,String reason)throws Exception{
        require(source);if(!allowedItem(id)||!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id))||count<1||count>2304||reason.length()<12||reason.length()>96||reason.codePoints().anyMatch(c->Character.isISOControl(c)||c=='§'))throw new IllegalArgumentException("Use a plain approved resource, bounded quantity and12–96character test reason");
        var store=CommerceService.store(p.server);if(store.quarantined()||store.pendingSale().isPresent()||store.pendingTransfer().isPresent()||store.pendingItem().isPresent()||p.containerMenu!=p.inventoryMenu||!p.containerMenu.getCarried().isEmpty()||!p.isAlive())throw new IllegalStateException("Close containers and resolve pending inventory operations before sandbox supplies");CommerceService.verifyReceipt(p);
        List<ItemStack> before=new ArrayList<>();for(int i=0;i<36;i++)before.add(p.getInventory().getItem(i).copy());var after=FarmerInventoryExchange.plan(before,Map.of(),Map.of(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)),count));
        var offers=OFFERS.computeIfAbsent(p.server,k->new HashMap<>());offers.values().removeIf(q->System.nanoTime()-q.issued()>30_000_000_000L);if(offers.size()>=32)throw new IllegalStateException("Too many active sandbox reviews");var q=new Offer(UUID.randomUUID(),actor(source),p.getUUID(),id,count,reason,System.nanoTime(),store.revision(),List.copyOf(before),after);offers.put(q.nonce(),q);return q;
    }
    private static int confirm(CommandSourceStack source,UUID nonce)throws Exception {
        require(source);var offers=OFFERS.get(source.getServer());var q=offers==null?null:offers.get(nonce);if(q==null||!q.actor().equals(actor(source))||System.nanoTime()-q.issued()>30_000_000_000L)throw new IllegalStateException("Sandbox review expired or belongs to another administrator");offers.remove(nonce);var p=source.getServer().getPlayerList().getPlayer(q.target());if(p==null)throw new IllegalStateException("Target logged out; review again");var store=CommerceService.store(p.server);CommerceService.verifyReceipt(p);if(store.quarantined()||store.pendingSale().isPresent()||store.pendingTransfer().isPresent()||store.pendingItem().isPresent()||store.revision()!=q.revision()||p.containerMenu!=p.inventoryMenu||!p.containerMenu.getCarried().isEmpty())throw new IllegalStateException("Inventory/economy changed; review again");for(int i=0;i<36;i++)if(!ItemStack.matches(p.getInventory().getItem(i),q.before().get(i)))throw new IllegalStateException("Inventory changed; review again");
        CommerceService.checkpoint(p);LogUtils.getLogger().warn("SANDBOX_SUPPLY_BEGIN operation={} actor={} target={} item={} count={} reason={}",q.nonce(),q.actor(),q.target(),q.item(),q.count(),q.reason());
        try{for(int i=0;i<36;i++)p.getInventory().setItem(i,q.after().get(i).copy());CommerceService.checkpoint(p);p.getInventory().setChanged();p.inventoryMenu.sendAllDataToRemote();LogUtils.getLogger().warn("SANDBOX_SUPPLY_SAVED operation={} target={}",q.nonce(),q.target());source.sendSuccess(()->Component.literal("Sandbox test supplies saved. No quest, source, XP, rank or currency proof was granted."),false);return 1;}catch(Exception e){store.quarantine();p.connection.disconnect(Component.literal("Sandbox save uncertain. Restore matching checkpoints before continuing."));throw e;}
    }
    @FunctionalInterface private interface Action{int run()throws Exception;}
    private static int guard(CommandSourceStack source,Action action){try{return action.run();}catch(Exception e){source.sendFailure(Component.literal(e.getMessage()==null?"Sandbox operation refused":e.getMessage()));return 0;}}
}
