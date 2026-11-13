package com.mohymods.kingdomsmp.profession;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.kingdom.KingdomTravelRules;
import com.mohymods.kingdomsmp.placement.PlacementProtection;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
/** Native server position/biome sampling; no coordinates, biome or counts arrive from a client packet. */
public final class LibraryExplorationService {
 private LibraryExplorationService(){}
 public static void register(IEventBus bus){bus.addListener(LibraryExplorationService::tick);}
 public static boolean supported(String dimension){String mod=dimension.equals("aether:the_aether")?"aether":dimension.equals("twilightforest:twilight_forest")?"twilightforest":null;if(mod==null)return false;String version=mod.equals("aether")?"1.5.10":"4.8.3345";var list=ModList.get();return list!=null&&list.getModContainerById(mod).map(c->c.getModInfo().getVersion().toString().equals(version)).orElse(false);}
 private static void tick(PlayerTickEvent.Post event){if(!(event.getEntity() instanceof ServerPlayer p)||p.tickCount%40!=0)return;observe(p);}
 /** Exposed native sampler also lets operator diagnostics verify the current server location; it accepts no evidence parameters. */
 public static void observe(ServerPlayer p){try{
  var level=p.serverLevel();String dimension=level.dimension().location().toString();if(!supported(dimension)||!TreeQuestService.real(p)||!level.hasChunkAt(p.blockPosition())||!level.getWorldBorder().isWithinBounds(p.blockPosition()))return;
  var store=CommerceService.initializedStore(p.server).orElse(null);if(store==null||store.quarantined())return;var roster=store.professions(p.getUUID());if(!roster.isActive(Profession.LIBRARIAN)||roster.progressBand(Profession.LIBRARIAN)!=ProfessionRank.JOURNEYMAN||ContributionService.kingdom(p.server).attainedTier()<6||!KingdomTravelRules.allowed(ContributionService.kingdom(p.server).attainedTier(),dimension)||!com.mohymods.kingdomsmp.building.ConstructionService.store(p.server).projects().stream().anyMatch(project->project.blueprint().toString().equals(Profession.LIBRARIAN.buildingId())&&project.phase()==com.mohymods.kingdomsmp.persistence.ConstructionStore.Phase.COMPLETE&&!project.npcDead())||PlacementProtection.check(level,p,p.blockPosition(),p.blockPosition())!=null)return;
  var biome=level.getBiome(p.blockPosition()).unwrapKey().orElse(null);if(biome==null)return;var data=ProfessionEvidenceSavedData.get(p.server);var proof=data.evidence(Profession.LIBRARIAN);if(proof.count(p.getUUID(),ProfessionRank.JOURNEYMAN,LibraryFieldGuide.METRIC)>=16)return;
  if(proof.record(p.getUUID(),ProfessionRank.JOURNEYMAN,LibraryFieldGuide.METRIC,1,LibraryFieldGuide.identity(dimension,biome.location().toString())))data.setDirty();
 }catch(RuntimeException failure){TreeLifecycle.failure(p.server,failure);}}
 public static String validate(ServerPlayer p,ItemStack stack){
  var book=stack.get(DataComponents.WRITTEN_BOOK_CONTENT);if(!stack.is(Items.WRITTEN_BOOK)||stack.getCount()!=1||book==null||book.pages().isEmpty()||book.pages().size()>20||!book.author().equals(p.getGameProfile().getName()))throw new IllegalStateException("Present your own readable written field guide on an accessible lectern");
  var text=new StringBuilder();for(var page:book.pages()){text.append(page.raw().getString()).append('\n');if(text.length()>32768)throw new IllegalStateException("Keep field notes within32768characters");}
  var proof=ProfessionEvidenceSavedData.get(p.server).evidence(Profession.LIBRARIAN);
  return LibraryFieldGuide.validate(text.toString(),(dimension,biome)->supported(dimension)&&p.server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,ResourceLocation.parse(dimension)))!=null&&proof.seen(p.getUUID(),LibraryFieldGuide.METRIC,LibraryFieldGuide.identity(dimension,biome)));
 }
}
