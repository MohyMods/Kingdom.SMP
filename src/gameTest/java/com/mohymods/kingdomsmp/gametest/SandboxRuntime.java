package com.mohymods.kingdomsmp.gametest;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import net.minecraft.commands.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.*;
final class SandboxRuntime {
 static void run(GameTestHelper h)throws Exception {
  var server=h.getLevel().getServer();var root=server.getWorldPath(LevelResource.ROOT);var proof=root.resolve("sandbox-test-player.nbt");
  if(Files.exists(proof)){var data=AtomicNbt.read(proof);var p=ClaimRuntimeTests.player(h.getLevel(),data.getUUID("player"),"SandboxTester");CommerceService.verifyReceipt(p);h.assertTrue(p.getInventory().countItem(Items.DIAMOND)==4&&CommerceService.store(server).balance(p.getUUID())==0,"Actual native supplies restored without virtual currency or reward proof");com.mojang.logging.LogUtils.getLogger().info("SANDBOX_RESTART_PASSED: native inventory grant retained, no currency/rank/quest/source/blueprint mint");h.succeed();return;}
  var p=ClaimRuntimeTests.player(h.getLevel(),"SandboxTester");var dispatcher=server.getCommands().getDispatcher();var messages=new ArrayList<Component>();
  var capture=new CommandSource(){public void sendSystemMessage(Component text){messages.add(text);}public boolean acceptsSuccess(){return true;}public boolean acceptsFailure(){return true;}public boolean shouldInformAdmins(){return false;}};
  var source=server.createCommandSourceStack().withSource(capture);String prefix="kingdom admin sandbox ";
  h.assertTrue(dispatcher.execute(prefix+"status",source)==0,"Sandbox disabled by default even for console");
  boolean denied=false;try{dispatcher.execute(prefix+"status",p.createCommandSourceStack());}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){denied=true;}h.assertTrue(denied,"Ordinary player cannot access sandbox dispatcher");
  var config=root.resolve("serverconfig/kingdomsmp-sandbox.json");Files.createDirectories(config.getParent());Files.writeString(config,"{\"schema\":1,\"enabled\":true,\"scope\":\"isolated_test_world\"}");
  h.assertTrue(dispatcher.execute(prefix+"supplies SandboxTester minecraft:netherite_pickaxe 1 Forbidden unique equipment fixture",source)==0,"No equipment or forged provenance can be supplied");
  String command=prefix+"supplies SandboxTester minecraft:diamond 4 Explicit private resource save fixture";h.assertTrue(dispatcher.execute(command,source)==1,"Real reviewed resource command");String stale=confirmation(messages.getLast());p.getInventory().setItem(35,new ItemStack(Items.COBBLESTONE));h.assertTrue(dispatcher.execute(stale,source)==0&&p.getInventory().countItem(Items.DIAMOND)==0,"Changed inventory refuses before grant");p.getInventory().setItem(35,ItemStack.EMPTY);
  long revision=CommerceService.store(server).revision();h.assertTrue(dispatcher.execute(command,source)==1,"Fresh reviewed resource command");String confirm=confirmation(messages.getLast());
  h.assertTrue(dispatcher.execute(confirm,p.createCommandSourceStack().withPermission(4))==0,"Another administrator cannot redeem console-bound review");h.assertTrue(dispatcher.execute(confirm,source)==1&&p.getInventory().countItem(Items.DIAMOND)==4,"Original console confirms actual physical supplies once");h.assertTrue(dispatcher.execute(confirm,source)==0&&p.getInventory().countItem(Items.DIAMOND)==4,"Nonce replay cannot duplicate resources");
  h.assertTrue(CommerceService.store(server).revision()==revision&&CommerceService.store(server).balance(p.getUUID())==0&&CommerceService.store(server).professions(p.getUUID()).tracks().isEmpty(),"No financial/quest/XP/rank/source state fabricated");
  var data=new CompoundTag();data.putUUID("player",p.getUUID());AtomicNbt.write(proof,data);com.mojang.logging.LogUtils.getLogger().info("SANDBOX_NATIVE_PASSED: OP4/private opt-in/console confirmation/current inventory/actor-bound nonce/replay refusal/actual native save/no currency or progression proof mint");h.succeed();
 }
 static String confirmation(Component component){var click=component.getStyle().getClickEvent();if(click!=null)return click.getValue().replaceFirst("^/","");for(var child:component.getSiblings()){String found=confirmation(child);if(found!=null)return found;}return null;}
}
