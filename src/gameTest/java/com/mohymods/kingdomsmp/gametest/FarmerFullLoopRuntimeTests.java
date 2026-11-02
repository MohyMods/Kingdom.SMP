package com.mohymods.kingdomsmp.gametest;

import com.google.gson.*;
import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.bounty.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.equipment.*;
import com.mohymods.kingdomsmp.persistence.*;
import com.mohymods.kingdomsmp.profession.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.*;
import java.util.*;

/** All24actual objective families. Explicit quantity/growth/AI acceleration and seeded supplies/funds; no injected quest/XP/rank proofs. */
@GameTestHolder("kingdom_smp_farmer_loop_tests") @PrefixGameTestTemplate(false)
public final class FarmerFullLoopRuntimeTests {
 @GameTest(template="empty",timeoutTicks=12000)
 public static void earnedFarmerRanksThroughEveryObjective(GameTestHelper h)throws Exception{fullLoop(h,h::succeed);}
 public static void fullLoop(GameTestHelper h,Runnable completion)throws Exception{
  BountyRuntimeTests.completeBountyHall(h,project->{try{new Walk(h,project,completion).step();}catch(Exception e){h.runAfterDelay(1,()->h.fail(e.toString()));}});
 }
 private static final class Walk {
  final GameTestHelper h;final ServerLevel level;final Runnable completion;
  final ServerPlayer p,reviewer;final ConstructionStore.Project bounty;final EconomyStore store;
  final BlockPos center=new BlockPos(100,-63,18),beds=center.offset(-6,0,-6);int stage;
  List<Block> growing=List.of();boolean deferred;
  Walk(GameTestHelper h,ConstructionStore.Project bounty,Runnable completion)throws Exception{
   this.h=h;this.level=h.getLevel();this.bounty=bounty;this.completion=completion;var server=level.getServer();p=server.getPlayerList().getPlayer(bounty.owner());reviewer=server.getPlayerList().getPlayers().stream().filter(v->v!=p).findFirst().orElseThrow();store=CommerceService.store(server);
   configure();level.setDayTime(6000);estate();server.getPlayerList().op(reviewer.getGameProfile());
   farmer();var enroll=ProfessionService.review(p,"learn:farmer");ProfessionService.confirm(p,enroll.nonce());store.adminSet(UUID.randomUUID(),new UUID(0,0),p.getUUID(),30000,"Explicit full Farmer equipment-test funds; no rank or XP injected");
  }
  void step(){try{
   deferred=false;com.mojang.logging.LogUtils.getLogger().info("FARMER_WALK_PHASE {}",stage);
   switch(stage++){
    case 0->{var soil=center.offset(7,0,7);level.setBlock(soil,Blocks.DIRT.defaultBlockState(),3);level.setBlock(soil.above(),Blocks.AIR.defaultBlockState(),3);move(p,soil.getX()+.5,soil.getY()+1,soil.getZ()+2.5);hold(new ItemStack(Items.IRON_HOE));use(soil);}
    case 1->{turn(ProfessionRank.APPRENTICE,1);plant(List.of(Blocks.WHEAT,Blocks.CARROTS,Blocks.POTATOES));}
    case 2->harvest();
    case 3->{for(int n=2;n<=4;n++)turn(ProfessionRank.APPRENTICE,n);breed(List.of(EntityType.COW));}
    case 4->{turn(ProfessionRank.APPRENTICE,5);craft(Items.BREAD,List.of(Items.WHEAT,Items.WHEAT,Items.WHEAT));food(ProfessionRank.APPRENTICE,7);turn(ProfessionRank.APPRENTICE,6);inspect(ProfessionRank.APPRENTICE,8);h.assertTrue(roster().rank(Profession.FARMER,FarmerQuests.catalog())==ProfessionRank.JOURNEYMAN,"Eight actual Apprentice objectives earn Journeyman");buy(FarmerGear.JOURNEYMAN_HOE);plant(List.of(Blocks.WHEAT,Blocks.CARROTS,Blocks.POTATOES,Blocks.BEETROOTS));}
    case 5->harvest();
    case 6->{turn(ProfessionRank.JOURNEYMAN,1);turn(ProfessionRank.JOURNEYMAN,2);move(p,beds.getX()+8.5,beds.getY()+1,beds.getZ()+2.5);var soil=beds.east(8);level.setBlock(soil,Blocks.FARMLAND.defaultBlockState(),3);level.setBlock(soil.above(),Blocks.AIR.defaultBlockState(),3);hold(new ItemStack(Items.MELON_SEEDS));use(soil);}
    case 7->{var stem=beds.east(8).above();level.setBlock(stem,Blocks.MELON_STEM.defaultBlockState().setValue(StemBlock.AGE,7),3);var random=RandomSource.create(924);for(int n=0;n<500&&level.getBlockState(stem).is(Blocks.MELON_STEM);n++)level.getBlockState(stem).randomTick(level,stem,random);h.assertTrue(level.getBlockState(stem).is(Blocks.ATTACHED_MELON_STEM),"Actual stem growth creates its fruit");var fruit=stem.relative(level.getBlockState(stem).getValue(AttachedStemBlock.FACING));hold(ItemStack.EMPTY);h.assertTrue(p.gameMode.destroyBlock(fruit),"Actual player harvest of generated melon");}
    case 8->{turn(ProfessionRank.JOURNEYMAN,3);breed(List.of(EntityType.COW,EntityType.SHEEP,EntityType.CHICKEN));}
    case 9->{turn(ProfessionRank.JOURNEYMAN,4);food(ProfessionRank.JOURNEYMAN,5);h.assertTrue(count(Items.BUCKET)>=1,"Real egg/milk objective returns emptied buckets");craft(Items.CAKE,List.of(Items.MILK_BUCKET,Items.MILK_BUCKET,Items.MILK_BUCKET,Items.SUGAR,Items.EGG,Items.SUGAR,Items.WHEAT,Items.WHEAT,Items.WHEAT));food(ProfessionRank.JOURNEYMAN,6);contract("daily");turn(ProfessionRank.JOURNEYMAN,7);inspect(ProfessionRank.JOURNEYMAN,8);h.assertTrue(roster().rank(Profession.FARMER,FarmerQuests.catalog())==ProfessionRank.JOURNEYMAN&&FarmerQuests.band(roster())==ProfessionRank.MASTER,"Sixteen earned proofs open Master trials without granting Master rank early");plant(List.of(Blocks.WHEAT,Blocks.CARROTS,Blocks.POTATOES,Blocks.BEETROOTS));}
    case 10->harvest();
    case 11->{move(p,beds.getX()+.5,beds.getY()+1,beds.getZ()+2.5);hold(new ItemStack(Items.WHEAT_SEEDS));use(beds);breed(List.of(EntityType.COW,EntityType.SHEEP,EntityType.CHICKEN,EntityType.PIG));}
    case 12->{turn(ProfessionRank.MASTER,1);turn(ProfessionRank.MASTER,2);craft(Items.BREAD,List.of(Items.WHEAT,Items.WHEAT,Items.WHEAT));craft(Items.CAKE,List.of(Items.MILK_BUCKET,Items.MILK_BUCKET,Items.MILK_BUCKET,Items.SUGAR,Items.EGG,Items.SUGAR,Items.WHEAT,Items.WHEAT,Items.WHEAT));craft(Items.PUMPKIN_PIE,List.of(Items.PUMPKIN,Items.SUGAR,Items.EGG));craft(Items.BEETROOT_SOUP,List.of(Items.BEETROOT,Items.BEETROOT,Items.BEETROOT,Items.BEETROOT,Items.BEETROOT,Items.BEETROOT,Items.BOWL));food(ProfessionRank.MASTER,3);h.assertTrue(count(Items.BOWL)>=1,"Banquet returns legitimate soup bowl");contract("weekly");turn(ProfessionRank.MASTER,4);community();farmer();turn(ProfessionRank.MASTER,5);turn(ProfessionRank.MASTER,6);inspect(ProfessionRank.MASTER,7);inspect(ProfessionRank.MASTER,8);finish();return;}
    default->throw new IllegalStateException("Unknown walkthrough phase");
   }
   if(!deferred)h.runAfterDelay(3,this::step);
  }catch(Exception e){h.runAfterDelay(1,()->h.fail("Farmer phase "+(stage-1)+": "+e));}}
  void configure()throws Exception{
   var server=level.getServer();var root=server.getWorldPath(LevelResource.ROOT);FarmerQuestRules.get(server);var path=root.resolve("serverconfig/kingdomsmp-farmer-quests.json");var json=JsonParser.parseString(Files.readString(path)).getAsJsonObject();var quantities=json.getAsJsonObject("quantities");for(var key:new ArrayList<>(quantities.keySet()))quantities.addProperty(key,1);quantities.addProperty("m01",4);quantities.addProperty("m02",4);quantities.addProperty("m05",3);Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(json));FarmerQuestRules.clear(server);
   CommunityContractRules.get(server);path=root.resolve("serverconfig/kingdomsmp-community-contracts.json");json=JsonParser.parseString(Files.readString(path)).getAsJsonObject();for(var row:json.getAsJsonObject("projects").entrySet())row.getValue().getAsJsonObject().addProperty("goal",10);Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(json));CommunityContractRules.clear(server);
  }
  ProfessionRoster roster(){return store.professions(p.getUUID());}
  void turn(ProfessionRank rank,int n)throws Exception{farmer();String id=FarmerQuests.id(rank,n);h.assertTrue(FarmerQuestService.ready(p,id),"Actual objective is ready: "+id+" | "+FarmerQuestService.status(p,id));var q=ProfessionService.review(p,"quest:"+id);ProfessionService.confirm(p,q.nonce());}
  void food(ProfessionRank rank,int n)throws Exception{farmer();String id=FarmerQuests.id(rank,n);for(var entry:FarmerDeliveryService.inputs(p,id).entrySet())supply(BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getKey())),entry.getValue());var q=ProfessionService.review(p,"food:"+id);ProfessionService.confirm(p,q.nonce());}
  void contract(String cycle)throws Exception{farmer();var terms=ContractRules.get(level.getServer()).get("farmer/"+cycle);supply(terms.item(),terms.quantity());var q=ContractService.review(p,"farmer/"+cycle);ContractService.confirm(p,q.nonce());}
  void community()throws Exception{open(level.getEntity(bounty.npc()));for(var terms:CommunityContractRules.get(level.getServer()).all()){supply(BuiltInRegistries.ITEM.get(ResourceLocation.parse(terms.item())),terms.goal());var q=CommunityContractService.review(p,"bounty:"+terms.id(),64);CommunityContractService.confirm(p,q.nonce());}h.assertTrue(store.communityMasterProofs(p.getUUID())==3,"Three actual completed community projects have meaningful personal Master-band supplies");}
  void inspect(ProfessionRank rank,int n)throws Exception{estate();move(reviewer,center.getX()+6.5,center.getY()+1,center.getZ()+6.5);String id=FarmerQuests.id(rank,n);var dispatcher=level.getServer().getCommands().getDispatcher();String cmd="kingdom admin farmer inspect "+p.getGameProfile().getName()+" "+id+" "+center.getX()+" "+center.getY()+" "+center.getZ()+" Inspected working farm ownership irrigation paths storage and community access";h.assertTrue(dispatcher.execute(cmd,reviewer.createCommandSourceStack())==1,"Actual administrator inspects working site for "+id);var nonce=FarmerInspectionService.currentReview(reviewer).orElseThrow();h.assertTrue(dispatcher.execute("kingdom admin farmer confirm "+nonce,reviewer.createCommandSourceStack())==1,"Exact real site approval is recorded for "+id);}
  void buy(FarmerGear gear)throws Exception{farmer();var q=FarmerGearService.review(p,gear.id());FarmerGearService.confirm(p,q.nonce());var claim=AuctionService.review(p,2,q.nonce().toString(),1,0);AuctionService.confirm(p,claim.nonce());h.assertTrue(count(FarmerGearItems.item(gear))==1,"Earned rank buys exactly one actual "+gear.title());}
  void finish()throws Exception{h.assertTrue(roster().track(Profession.FARMER).completedQuestIds().size()==24&&roster().track(Profession.FARMER).xp()==4800&&roster().rank(Profession.FARMER,FarmerQuests.catalog())==ProfessionRank.MASTER,"All24real objective proofs earn4800XP and Master");buy(FarmerGear.FORTUNE_X_HOE);h.assertTrue(count(FarmerGearItems.FIELD.get())==1&&count(FarmerGearItems.MASTER.get())==1,"Both genuinely earned purchase rights retain their distinct physical items");var reopened=EconomyStore.open(level.getServer().getWorldPath(LevelResource.ROOT).resolve("data"));h.assertTrue(reopened.professions(p.getUUID()).equals(roster())&&reopened.equipmentPurchases(p.getUUID(),FarmerGear.FORTUNE_X_HOE.id())==1,"All personal quest/rank/equipment records reopen without a rank fixture");CommerceService.verifyReceipt(p);var evidence=FarmerSavedData.get(level.getServer());var restored=FarmerSavedData.load(evidence.save(new net.minecraft.nbt.CompoundTag(),level.registryAccess()),level.registryAccess());h.assertTrue(restored.evidence().count(p.getUUID(),ProfessionRank.MASTER,"replant")>=1,"Real sustainable lifecycle evidence persists");com.mojang.logging.LogUtils.getLogger().info("FARMER_FULL_LOOP_ASSERTIONS_PASSED: actual24objective families, real seeds/harvests/births/recipes/food/container/contract/site/community proofs; earned Apprentice→Journeyman→Master, purchased both tools, preserved items and reopen; quantities/growth/AI waiting accelerated, supplies/funds explicitly seeded; no injected quest/XP/rank proof");completion.run();}
  void plant(List<Block> crops){growing=List.copyOf(crops);for(int i=0;i<crops.size();i++){var soil=beds.east(i*2);level.setBlock(soil,Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE,7),3);level.setBlock(soil.above(),Blocks.AIR.defaultBlockState(),3);move(p,soil.getX()+.5,soil.getY()+1,soil.getZ()+2.5);Item seed=crops.get(i)==Blocks.WHEAT?Items.WHEAT_SEEDS:crops.get(i)==Blocks.CARROTS?Items.CARROT:crops.get(i)==Blocks.POTATOES?Items.POTATO:Items.BEETROOT_SEEDS;hold(new ItemStack(seed));use(soil);h.assertTrue(p.getMainHandItem().isEmpty(),"Real planted seed is consumed");}}
  void harvest(){hold(ItemStack.EMPTY);for(int i=0;i<growing.size();i++){var pos=beds.east(i*2).above();var crop=(CropBlock)growing.get(i);level.setBlock(pos,crop.getStateForAge(crop.getMaxAge()),3);move(p,pos.getX()+.5,pos.getY(),pos.getZ()+2.5);h.assertTrue(p.gameMode.destroyBlock(pos),"Actual player mature harvest succeeds");}}
  void breed(List<EntityType<? extends Animal>> types){
   deferred=true;move(p,center.getX()+5.5,center.getY()+1,center.getZ()+6.5);
   // Real clients cannot breed entities before their destination chunk is tracking.
   // Use vanilla player teleport tracking and wait before creating/feeding the fixture parents.
   h.runAfterDelay(5,()->awaitBreedingPlot(types,0));
  }
  void awaitBreedingPlot(List<EntityType<? extends Animal>> types,int attempts){try{if(!level.isPositionEntityTicking(center)){h.assertTrue(attempts<80,"Vanilla breeding destination must become entity ticking");h.runAfterDelay(5,()->awaitBreedingPlot(types,attempts+1));return;}breedNow(types);h.runAfterDelay(3,this::step);}catch(Exception e){h.runAfterDelay(1,()->h.fail("Tracked breeding fixture: "+e));}}
  void breedNow(List<EntityType<? extends Animal>> types){int i=0;for(var type:types){var a=type.create(level);var b=type.create(level);h.assertTrue(a!=null&&b!=null,"Registered eligible farm animals");var point=center.offset(4+(i++%2),1,4);a.moveTo(point.getX()+.5,point.getY(),point.getZ()+.5);b.moveTo(point.getX()+1,point.getY(),point.getZ()+1);a.setNoAi(true);b.setNoAi(true);h.assertTrue(level.addFreshEntity(a)&&level.addFreshEntity(b),"Both fixture breeding parents spawn");h.assertTrue(level.getEntity(a.getUUID())==a&&level.getEntity(b.getUUID())==b,"Breeding parents are tracked before actual player interaction");move(p,point.getX(),point.getY(),point.getZ()+2);hold(new ItemStack(type==EntityType.PIG?Items.CARROT:type==EntityType.CHICKEN?Items.WHEAT_SEEDS:Items.WHEAT,2));a.mobInteract(p,InteractionHand.MAIN_HAND);b.mobInteract(p,InteractionHand.MAIN_HAND);h.assertTrue(p.getMainHandItem().isEmpty()&&a.isInLove()&&b.isInLove(),"Actual player food consumed by both breeding parents");a.spawnChildFromBreeding(level,b);}}
  void craft(Item output,List<Item> ingredients){var table=center.offset(7,1,-7);level.setBlock(table,Blocks.CRAFTING_TABLE.defaultBlockState(),3);move(p,table.getX()+.5,table.getY(),table.getZ()+1.5);var menu=new CraftingMenu(42,p.getInventory(),ContainerLevelAccess.create(level,table));p.containerMenu=menu;h.assertTrue(menu.stillValid(p),"Real crafting table is within reach");for(int i=0;i<ingredients.size();i++)menu.getSlot(i+1).set(new ItemStack(ingredients.get(i)));h.assertTrue(menu.getSlot(0).getItem().is(output),"Actual server recipe resolves "+output);h.assertTrue(menu.quickMoveStack(p,0).is(output),"Real recipe output is taken with ingredient consumption");p.closeContainer();}
  void hold(ItemStack next){p.getInventory().selected=0;var old=p.getInventory().getItem(0);if(!old.isEmpty()){int empty=-1;for(int slot=35;slot>=1;slot--)if(p.getInventory().getItem(slot).isEmpty()){empty=slot;break;}h.assertTrue(empty!=-1,"Fixture preserves held items without dropping or copying");p.getInventory().setItem(empty,old);}p.getInventory().setItem(0,next);}
  int count(Item item){int n=0;for(int slot=0;slot<36;slot++)if(p.getInventory().getItem(slot).is(item))n+=p.getInventory().getItem(slot).getCount();return n;}
  void supply(Item item,int required){int left=Math.max(0,required-count(item));while(left>0){int n=Math.min(left,new ItemStack(item).getMaxStackSize());h.assertTrue(p.getInventory().add(new ItemStack(item,n)),"Explicit fixture supplies fit without deleting equipment");left-=n;}}
  void move(ServerPlayer player,double x,double y,double z){player.teleportTo(level,x,y,z,Set.of(),player.getYRot(),player.getXRot());level.getChunkSource().move(player);}
  void use(BlockPos soil){p.gameMode.useItemOn(p,level,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(soil),Direction.UP,soil,false));}
  void farmer(){var farm=ConstructionService.store(level.getServer()).projects().stream().filter(v->v.blueprint().equals(ProfessionBlueprints.FARMER)).findFirst().orElseThrow();open(level.getEntity(farm.npc()));}
  void open(Entity npc){h.assertTrue(npc instanceof com.mohymods.kingdomsmp.npc.ReeveEntity,"Actual assigned resident remains loaded");for(var pos:BlockPos.betweenClosed(npc.blockPosition().offset(-2,0,-2),npc.blockPosition().offset(2,0,2))){if(!level.getBlockState(pos).isAir()||!level.getBlockState(pos.above()).isAir()||!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP))continue;move(p,pos.getX()+.5,pos.getY(),pos.getZ()+.5);if(p.hasLineOfSight(npc)&&p.distanceToSqr(npc)<25){CommerceService.openResident(p,(com.mohymods.kingdomsmp.npc.ReeveEntity)npc);return;}}throw new IllegalStateException("No clear resident interaction position");}
  void estate(){for(var pos:BlockPos.betweenClosed(center.offset(-8,-1,-8),center.offset(8,-1,8)))level.setBlock(pos,Blocks.STONE.defaultBlockState(),18);for(var pos:BlockPos.betweenClosed(center.offset(-8,0,-8),center.offset(8,0,8)))if(level.getBlockState(pos).isAir())level.setBlock(pos,Blocks.DIRT.defaultBlockState(),3);List<Block> crops=List.of(Blocks.WHEAT,Blocks.CARROTS,Blocks.POTATOES,Blocks.BEETROOTS);for(int x=-4;x<=3;x++)for(int z=-3;z<=2;z++){var pos=center.offset(x,0,z);level.setBlock(pos,Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE,7),3);level.setBlock(pos.above(),((CropBlock)crops.get(Math.floorMod(x,4))).getStateForAge(0),3);}for(int x:new int[]{-5,4})for(int z:new int[]{-4,3})level.setBlock(center.offset(x,0,z),Blocks.WATER.defaultBlockState(),3);for(int x=-4;x<=3;x++)level.setBlock(center.offset(x,0,5),Blocks.DIRT_PATH.defaultBlockState(),3);level.setBlock(center.offset(-6,1,5),Blocks.BARREL.defaultBlockState(),3);level.setBlock(center.offset(-6,1,6),Blocks.BARREL.defaultBlockState(),3);}
 }
}
