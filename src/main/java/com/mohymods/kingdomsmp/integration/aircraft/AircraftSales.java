package com.mohymods.kingdomsmp.integration.aircraft;

import com.mohymods.kingdomsmp.building.RoyalAerodromeBlueprint;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.network.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import java.io.IOException;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.HitResult;

/**
 * Actual Aerodrome counter and native placement adapter. Canonical records, never copied NBT, grant
 * rights.
 */
public final class AircraftSales {
  public static final String MARKER = "KingdomAircraftRegistration";

  public record Quote(
      UUID nonce, long revision, String action, long price, long day, long issued, String text) {}

  private static final Map<ServerPlayer, Quote> QUOTES = new WeakHashMap<>();

  private AircraftSales() {}

  public static void close(ServerPlayer p) {
    QUOTES.remove(p);
  }

  public static Optional<Quote> quote(ServerPlayer p) {
    return Optional.ofNullable(QUOTES.get(p));
  }

  public static boolean atCounter(ServerPlayer p) {
    var project = CommerceService.counterProject(p);
    return project != null && project.blueprint().equals(RoyalAerodromeBlueprint.ID);
  }

  private static void require(ServerPlayer p) {
    CommerceService.requireResidentCounter(p);
    AircraftAccess.require();
    String why = AircraftAccess.denial(p.server);
    if (!why.isEmpty()) throw new IllegalStateException(why);
    if (!atCounter(p))
      throw new IllegalStateException("Visit Corin Windlass at the completed Aerodrome.");
  }

  public static void review(ServerPlayer p, String action) throws IOException {
    require(p);
    var store = CommerceService.store(p.server);
    CommerceService.verifyReceipt(p, store);
    long price = 0;
    String text;
    if (action.startsWith("air:upgrade:")) {
      var part = AircraftEquipment.part(action.substring(12));
      var rules = AircraftEquipment.rules(p.server);
      price = rules.price(part);
      AircraftEquipment.stack(part);
      if (store.shopSold(day(), part.resource()) >= rules.dailyStock())
        throw new IllegalStateException("Today's shared stock of this upgrade has sold out.");
      if (store.balance(p.getUUID()) < price)
        throw new IllegalStateException("Insufficient coins for this upgrade.");
      text =
          "Buy one native "
              + part.title()
              + " for exactly "
              + price
              + " coins into private Mailbox. Shared daily stock is finite and resets at UTC"
              + " midnight. Fit it in a compatible native aircraft inventory slot; it is not"
              + " installed remotely. Native slot compatibility and current registration/claim"
              + " permissions still apply.";
    } else if (action.startsWith("air:buy:")) {
      String type = action.substring(8);
      price = price(p, type);
      if (store.balance(p.getUUID()) < price)
        throw new IllegalStateException("Insufficient coins for this aircraft.");
      text =
          "Purchase "
              + title(type)
              + " for exactly "
              + price
              + " coins. Registration belongs permanently to you, with access you may share at"
              + " Corin's counter. One native aircraft item is delivered to private Mailbox. Fuel"
              + " and upgrades are not included. Deployment still requires a valid site, Tier 6 and"
              + " the Aerodrome.";
    } else if (action.startsWith("air:recover:")) {
      var record = pendingOwned(p, UUID.fromString(action.substring(12)));
      text =
          "Cancel this unfinished "
              + title(record.type())
              + " deployment and return its canonical native airframe to your private Mailbox. No"
              + " further payment. This invalidates its pending world body and all old physical"
              + " copies. Active aircraft cannot be recovered this way; buyer registration and crew"
              + " are retained.";
    } else {
      var parsed = parseCrew(action);
      var record = store.aircraft(parsed.registration()).orElseThrow();
      if (!record.owner().equals(p.getUUID()) || parsed.actor().equals(p.getUUID()))
        throw new IllegalStateException("Only the buyer may manage crew.");
      if (parsed.allow() && p.server.getPlayerList().getPlayer(parsed.actor()) == null)
        throw new IllegalStateException("Invite a player who is currently online.");
      if (record.crew().contains(parsed.actor()) == parsed.allow())
        throw new IllegalStateException("Aircraft access already has this setting.");
      text =
          (parsed.allow() ? "Grant" : "Revoke")
              + " shared deployment, boarding, controls and cargo access for "
              + name(p.server, parsed.actor())
              + " on "
              + title(record.type())
              + ". You retain ownership; claims and kingdom gates still apply. Revocation also"
              + " suspends a current rider safely.";
    }
    QUOTES.put(
        p,
        new Quote(
            UUID.randomUUID(), store.revision(), action, price, day(), System.nanoTime(), text));
  }

  public static String confirm(ServerPlayer p, UUID nonce) throws IOException {
    var q = QUOTES.remove(p);
    require(p);
    var store = CommerceService.store(p.server);
    CommerceService.verifyReceipt(p, store);
    if (q == null
        || !q.nonce().equals(nonce)
        || q.revision() != store.revision()
        || System.nanoTime() - q.issued() > 30_000_000_000L)
      throw new IllegalStateException(
          "Aircraft review expired or registry/funds changed. Review again.");
    if (q.action().startsWith("air:upgrade:")) {
      var part = AircraftEquipment.part(q.action().substring(12));
      var rules = AircraftEquipment.rules(p.server);
      if (day() != q.day() || rules.price(part) != q.price())
        throw new IllegalStateException("Trading day or upgrade price changed; review again.");
      store.shopBuy(
          q.nonce(),
          p.getUUID(),
          part.resource(),
          part.title(),
          (CompoundTag) AircraftEquipment.stack(part).save(p.registryAccess()),
          1,
          q.day(),
          rules.dailyStock(),
          q.price(),
          q.price());
      return part.title()
          + " purchased once. Claim it in private Mailbox, then fit a compatible aircraft through"
          + " its normal inventory.";
    }
    if (q.action().startsWith("air:buy:")) {
      String type = q.action().substring(8);
      if (price(p, type) != q.price()) throw new IllegalStateException("Aircraft price changed.");
      var item = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(type)));
      mark(item, store.id(), q.nonce(), 0);
      store.buyAircraft(
          q.nonce(), p.getUUID(), type, q.price(), (CompoundTag) item.save(p.registryAccess()));
      return "Aircraft registered to you. Claim the native airframe in Mailbox; use Corin's"
          + " Supplies list to share access.";
    }
    if (q.action().startsWith("air:recover:")) {
      var record = pendingOwned(p, UUID.fromString(q.action().substring(12)));
      var item =
          ItemStack.parse(p.registryAccess(), record.item())
              .orElseThrow(
                  () -> new IllegalStateException("Canonical pending airframe is unavailable"));
      mark(item, store.id(), record.id(), Math.addExact(record.generation(), 1));
      store.packAircraft(
          q.nonce(), p.getUUID(), record.id(), (CompoundTag) item.save(p.registryAccess()));
      return "Unfinished deployment canceled. Claim the airframe in private Mailbox; old pending"
          + " body and copies are invalid. No coins charged.";
    }
    var crew = parseCrew(q.action());
    store.shareAircraft(q.nonce(), p.getUUID(), crew.registration(), crew.actor(), crew.allow());
    return "Aircraft shared access saved. Buyer registration is unchanged.";
  }

  private static AircraftState.Registration pendingOwned(ServerPlayer p, UUID id) {
    var record = CommerceService.store(p.server).aircraft(id).orElseThrow();
    if (!record.owner().equals(p.getUUID()) || record.phase() != AircraftState.Phase.STARTING)
      throw new IllegalStateException(
          "Only the buyer can recover an unfinished deployment. Active aircraft are never reissued"
              + " by absence.");
    return record;
  }

  private record Crew(UUID registration, UUID actor, boolean allow) {}

  private static Crew parseCrew(String value) {
    var parts = value.split(":");
    if (parts.length != 4
        || !parts[0].equals("air")
        || !Set.of("grant", "revoke").contains(parts[1]))
      throw new IllegalArgumentException("Choose a valid aircraft service");
    return new Crew(UUID.fromString(parts[2]), UUID.fromString(parts[3]), parts[1].equals("grant"));
  }

  private static long price(ServerPlayer p, String type) {
    Long price = AircraftSalesRules.prices(p.server).get(type);
    var id = ResourceLocation.tryParse(type);
    if (price == null
        || id == null
        || !BuiltInRegistries.ITEM.containsKey(id)
        || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)
        || !AircraftAccess.aircraftItem(new ItemStack(BuiltInRegistries.ITEM.get(id))))
      throw new IllegalStateException("Native aircraft type is unavailable");
    return price;
  }

  public static List<CommerceView.Entry> entries(ServerPlayer p) {
    var rows = new ArrayList<CommerceView.Entry>();
    for (var type : new TreeSet<>(AircraftState.TYPES)) {
      long price = price(p, type);
      rows.add(
          new CommerceView.Entry(
              "air:buy:" + type,
              title(type),
              "Tier 6 · Buyer registration · Shareable access · Private delivery. Includes the"
                  + " native airframe; supply fuel and upgrades separately.",
              price,
              "AIRCRAFT"));
    }
    var equipmentRules = AircraftEquipment.rules(p.server);
    for (var part : AircraftEquipment.PARTS) {
      AircraftEquipment.stack(part);
      long sold = CommerceService.store(p.server).shopSold(day(), part.resource());
      rows.add(
          new CommerceView.Entry(
              "air:upgrade:" + part.id(),
              part.title(),
              "Native nonweapon aircraft upgrade · "
                  + Math.max(0, equipmentRules.dailyStock() - sold)
                  + " left today. Private item delivery; fit a compatible native inventory slot."
                  + " Registration and claims remain required for aircraft use.",
              equipmentRules.price(part),
              sold >= equipmentRules.dailyStock() ? "OUT_OF_STOCK" : "AIRCRAFT_UPGRADE"));
    }
    for (var record : CommerceService.store(p.server).aircraft())
      if (record.owner().equals(p.getUUID())) {
        if (rows.size() >= 120) break;
        rows.add(
            new CommerceView.Entry(
                "air:record:" + record.id(),
                "Your " + title(record.type()) + " · " + record.id().toString().substring(0, 8),
                "Buyer: "
                    + p.getGameProfile().getName()
                    + " · "
                    + record.phase()
                    + " · Generation "
                    + record.generation()
                    + " · Shared crew: "
                    + record.crew().size()
                    + ". Registered airframes cannot be consumed in crafting; fit native equipment"
                    + " through their inventory.",
                0,
                "AIRCRAFT_RECORD"));
        if (record.phase() == AircraftState.Phase.STARTING && rows.size() < 120)
          rows.add(
              new CommerceView.Entry(
                  "air:recover:" + record.id(),
                  "Recover unfinished " + title(record.type()),
                  "Cancel the pending deployment and return the canonical native airframe to"
                      + " private Mailbox. Invalidates old body/copies; no payment. Does not"
                      + " replace an active aircraft.",
                  0,
                  "AIRCRAFT_RECOVER"));
        for (UUID crew : record.crew()) {
          if (rows.size() >= 120) break;
          rows.add(
              new CommerceView.Entry(
                  "air:revoke:" + record.id() + ":" + crew,
                  "Revoke · " + name(p.server, crew),
                  title(record.type())
                      + " · Shared pilot, deployment and cargo rights. Buyer retains registration.",
                  0,
                  "REVOKE"));
        }
        for (var other : p.server.getPlayerList().getPlayers()) {
          if (rows.size() >= 120) break;
          if (!other.getUUID().equals(p.getUUID()) && !record.crew().contains(other.getUUID()))
            rows.add(
                new CommerceView.Entry(
                    "air:grant:" + record.id() + ":" + other.getUUID(),
                    "Share · " + other.getGameProfile().getName(),
                    title(record.type())
                        + " · "
                        + record.phase()
                        + " · Permit this player to deploy, board, pilot and use cargo. Claims"
                        + " remain enforced.",
                    0,
                    "SHARE"));
        }
      }
    return List.copyOf(rows);
  }

  private static long day() {
    return Math.floorDiv(System.currentTimeMillis(), 86_400_000L);
  }

  private static String name(MinecraftServer s, UUID id) {
    var p = s.getPlayerList().getPlayer(id);
    return p == null
        ? s.getProfileCache()
            .get(id)
            .map(com.mojang.authlib.GameProfile::getName)
            .orElse(id.toString().substring(0, 8))
        : p.getGameProfile().getName();
  }

  private static String title(String type) {
    String raw = type.substring(type.indexOf(':') + 1).replace('_', ' ');
    return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
  }

  public static void mark(ItemStack item, UUID store, UUID registration, long generation) {
    var data = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    data.put(MARKER, marker(store, registration, generation));
    item.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    item.set(
        DataComponents.LORE,
        new net.minecraft.world.item.component.ItemLore(
            List.of(
                Component.literal(
                    "Registered airframe · " + registration.toString().substring(0, 8)),
                Component.literal("Share access at Corin Windlass's counter"),
                Component.literal("Fit equipment in its inventory; not a crafting ingredient"))));
  }

  private static CompoundTag marker(UUID store, UUID id, long generation) {
    var data = new CompoundTag();
    data.putUUID("store", store);
    data.putUUID("registration", id);
    data.putLong("generation", generation);
    return data;
  }

  public static boolean registered(ItemStack item) {
    return item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains(MARKER);
  }

  private static AircraftState.Registration resolve(MinecraftServer server, CompoundTag data) {
    var store = CommerceService.store(server);
    if (!data.getAllKeys().equals(Set.of("store", "registration", "generation"))
        || !data.hasUUID("store")
        || !data.hasUUID("registration")
        || !data.contains("generation", Tag.TAG_LONG)
        || !store.id().equals(data.getUUID("store")))
      throw new IllegalStateException("Invalid or foreign aircraft registration");
    var record =
        store
            .aircraft(data.getUUID("registration"))
            .orElseThrow(() -> new IllegalStateException("Unknown aircraft registration"));
    if (record.generation() != data.getLong("generation"))
      throw new IllegalStateException("This aircraft copy is obsolete");
    return record;
  }

  public static String denial(ServerPlayer p, Entity entity) {
    if (!entity.getPersistentData().contains(MARKER)) return "";
    try {
      var record = resolve(p.server, entity.getPersistentData().getCompound(MARKER));
      if (record.phase() != AircraftState.Phase.ACTIVE
          || !record.deployment().getCompound("entity").getUUID("UUID").equals(entity.getUUID()))
        return "Aircraft deployment is pending or obsolete.";
      return record.permits(p.getUUID())
          ? ""
          : "Only the registered buyer and their shared crew may use this aircraft.";
    } catch (RuntimeException e) {
      return "Aircraft registration requires recovery.";
    }
  }

  /**
   * Called only for marked native items; unregistered legacy airframes keep their original path.
   */
  public static InteractionResultHolder<ItemStack> deploy(ServerPlayer p, InteractionHand hand) {
    var held = p.getItemInHand(hand);
    boolean changed = false;
    try {
      CommerceService.verifyReceipt(p);
      if (!AircraftInventoryAccess.current(p)
          || !p.isAlive()
          || p.isSpectator()
          || p.containerMenu != p.inventoryMenu
          || !p.inventoryMenu.getCarried().isEmpty())
        throw new IllegalStateException("Deploy alive with containers closed.");
      var record =
          resolve(
              p.server,
              held.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                  .copyTag()
                  .getCompound(MARKER));
      if (record.phase() != AircraftState.Phase.PACKED
          || !record.permits(p.getUUID())
          || held.getCount() != 1
          || !BuiltInRegistries.ITEM.getKey(held.getItem()).toString().equals(record.type()))
        throw new IllegalStateException(
            "This airframe is already deployed or not shared with you.");
      String why = AircraftAccess.denial(p.server);
      if (!why.isEmpty()) throw new IllegalStateException(why);
      if (AircraftAccess.deploymentDenied(p, held)) return InteractionResultHolder.fail(held);
      var canonical = ItemStack.parse(p.registryAccess(), record.item()).orElseThrow();
      var hit = p.pick(p.blockInteractionRange(), 1, AircraftAccess.onWater(canonical));
      if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(held);
      Entity entity =
          BuiltInRegistries.ENTITY_TYPE
              .get(ResourceLocation.parse(record.type()))
              .create(p.serverLevel());
      if (entity == null) throw new IllegalStateException("Native airframe unavailable");
      entity.getClass().getMethod("readItemTag", ItemStack.class).invoke(entity, canonical);
      entity.setPos(hit.getLocation());
      entity.setYRot(p.getYRot());
      entity
          .getPersistentData()
          .put(
              MARKER,
              marker(CommerceService.store(p.server).id(), record.id(), record.generation()));
      if (!p.serverLevel().noCollision(entity, entity.getBoundingBox()))
        throw new IllegalStateException("Aircraft footprint is obstructed");
      var body = new CompoundTag();
      if (!entity.save(body)) throw new IllegalStateException("Native aircraft cannot be saved");
      var store = CommerceService.store(p.server);
      CommerceService.checkpoint(p);
      var pending =
          store.prepareAircraftDeployment(
              UUID.randomUUID(),
              p.getUUID(),
              record.id(),
              body,
              p.serverLevel().dimension().location().toString());
      held.shrink(1);
      CommerceService.putReceipt(p, new EconomyStore.Receipt(store.id(), pending.sequence()));
      changed = true;
      CommerceService.checkpoint(p);
      if (!store.recoverItem(
          CommerceService.receipt(
              AtomicNbt.read(CommerceService.playerFile(p.server, p.getUUID())))))
        throw new IOException("Aircraft inventory receipt was not committed");
      finish(p.server, store.aircraft(record.id()).orElseThrow());
      if (store.aircraft(record.id()).orElseThrow().phase() == AircraftState.Phase.STARTING)
        p.sendSystemMessage(
            Component.literal(
                "Aircraft deployment is saved but waiting for a valid loaded site. Revisit the"
                    + " footprint or recover the unfinished airframe at Corin's counter; no second"
                    + " purchase is required."));
      p.inventoryMenu.sendAllDataToRemote();
      return InteractionResultHolder.success(p.getItemInHand(hand));
    } catch (IOException | ReflectiveOperationException | RuntimeException failure) {
      if (changed) {
        CommerceService.store(p.server).quarantine();
        p.connection.disconnect(
            Component.literal(
                "Aircraft deployment interrupted. Matching inventory and world recovery is"
                    + " required."));
      } else {
        com.mojang.logging.LogUtils.getLogger()
            .info(
                "AIRCRAFT_DEPLOYMENT_REFUSED actor={} reason={}",
                p.getUUID(),
                failure.getMessage());
        p.sendSystemMessage(
            Component.literal("Aircraft deployment refused: " + failure.getMessage()));
      }
      return InteractionResultHolder.fail(p.getItemInHand(hand));
    }
  }

  /** Recover only STARTING at its saved origin. ACTIVE aircraft are never respawned by absence. */
  public static void finish(MinecraftServer server, AircraftState.Registration record)
      throws IOException {
    if (record.phase() != AircraftState.Phase.STARTING) return;
    var actor = server.getPlayerList().getPlayer(record.deployment().getUUID("actor"));
    if (actor == null
        || actor.hasDisconnected()
        || !record.permits(actor.getUUID())
        || !AircraftAccess.denial(server).isEmpty()) return;
    var level =
        server.getLevel(
            net.minecraft.resources.ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.parse(record.deployment().getString("dimension"))));
    if (level == null || actor.serverLevel() != level) return;
    var body = record.deployment().getCompound("entity");
    var pos = body.getList("Pos", Tag.TAG_DOUBLE);
    var site =
        net.minecraft.core.BlockPos.containing(
            pos.getDouble(0), pos.getDouble(1), pos.getDouble(2));
    if (!level.hasChunkAt(site)) return;
    Entity entity = level.getEntity(body.getUUID("UUID"));
    boolean missing = entity == null;
    if (missing) entity = EntityTypeLoader.load(body, level);
    if (entity == null) return;
    if (!BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals(record.type())
        || !entity
            .getPersistentData()
            .getCompound(MARKER)
            .equals(body.getCompound("NeoForgeData").getCompound(MARKER)))
      throw new IOException("Aircraft deployment UUID collision");
    var bounds = entity.getBoundingBox();
    var from = net.minecraft.core.BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ);
    var to =
        net.minecraft.core.BlockPos.containing(
            Math.nextDown(bounds.maxX), Math.nextDown(bounds.maxY), Math.nextDown(bounds.maxZ));
    if (from.getY() < level.getMinBuildHeight() || to.getY() >= level.getMaxBuildHeight()) return;
    for (var cell : net.minecraft.core.BlockPos.betweenClosed(from, to))
      if (!level.hasChunkAt(cell)
          || com.mohymods.kingdomsmp.building.ConstructionService.protectedAt(level, cell)) return;
    if (!level.getWorldBorder().isWithinBounds(bounds)
        || !level.noCollision(entity, bounds)
        || com.mohymods.kingdomsmp.placement.PlacementProtection.check(level, actor, from, to)
            != null) return;
    if (missing && !level.addFreshEntity(entity)) return;
    // The native body must reach durable world storage before the canonical ACTIVE pointer.
    server.saveEverything(false, true, true);
    CommerceService.store(server).activateAircraft(UUID.randomUUID(), record.owner(), record.id());
  }

  private static final class EntityTypeLoader {
    static Entity load(CompoundTag body, ServerLevel level) {
      return net.minecraft.world.entity.EntityType.loadEntityRecursive(body, level, e -> e);
    }
  }

  public static void recover(MinecraftServer server) {
    var store = CommerceService.initializedStore(server).orElse(null);
    if (store == null || store.quarantined()) return;
    for (var record : store.aircraft())
      if (record.phase() == AircraftState.Phase.STARTING)
        try {
          finish(server, record);
        } catch (IOException | RuntimeException e) {
          store.quarantine();
          com.mojang.logging.LogUtils.getLogger().error("Aircraft deployment recovery paused", e);
          return;
        }
  }

  /**
   * Native destruction/packing has already removed the body and dropped its ordinary cargo. Mail
   * the remaining airframe exactly once.
   */
  public static boolean nativeDrop(Entity entity, ItemStack item) {
    if (entity.level().isClientSide || !entity.getPersistentData().contains(MARKER)) return false;
    var server = entity.getServer();
    try {
      var record = resolve(server, entity.getPersistentData().getCompound(MARKER));
      if (record.phase() != AircraftState.Phase.ACTIVE
          || !record.deployment().getCompound("entity").getUUID("UUID").equals(entity.getUUID()))
        return true;
      mark(
          item,
          CommerceService.store(server).id(),
          record.id(),
          Math.addExact(record.generation(), 1));
      CommerceService.store(server)
          .packAircraft(
              UUID.randomUUID(),
              record.owner(),
              record.id(),
              (CompoundTag) item.save(entity.registryAccess()));
      return true;
    } catch (IOException | RuntimeException e) {
      CommerceService.store(server).quarantine();
      throw new IllegalStateException(
          "Registered aircraft return requires recovery; no unregistered drop is allowed", e);
    }
  }

  public static boolean validateBody(Entity entity) {
    if (entity.level().isClientSide
        || !entity.getPersistentData().contains(MARKER)
        || CommerceService.initializedStore(entity.getServer()).isEmpty()) return false;
    try {
      var data = entity.getPersistentData().getCompound(MARKER);
      var store = CommerceService.store(entity.getServer());
      if (data.hasUUID("store")
          && store.id().equals(data.getUUID("store"))
          && data.hasUUID("registration")
          && data.contains("generation", Tag.TAG_LONG)) {
        var current = store.aircraft(data.getUUID("registration")).orElse(null);
        if (current != null && data.getLong("generation") < current.generation()) {
          entity.discard(); // Canonically superseded body: never drop or reissue its cargo.
          return true;
        }
      }
      var record = resolve(entity.getServer(), data);
      if (record.phase() == AircraftState.Phase.PACKED
          || !record.deployment().getCompound("entity").getUUID("UUID").equals(entity.getUUID())) {
        entity.discard();
        return true;
      }
      return record.phase() != AircraftState.Phase.ACTIVE;
    } catch (RuntimeException e) {
      return true; /* Preserve unknown authority without granting use or movement. */
    }
  }
}
