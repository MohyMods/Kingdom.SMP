package com.mohymods.kingdomsmp.profession;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
/** Exact already-runtime-verified public native APIs; labels and client custom data carry no rarity authority. */
public final class NativeLibraryScroll {
 private NativeLibraryScroll(){}
 public static boolean supported(){return ModList.get().getModContainerById("irons_spellbooks").map(v->v.getModInfo().getVersion().toString().equals("1.21.1-3.16.3")).orElse(false);}
 public static LibrarySpellCatalogue.Fact inspect(ServerPlayer p,ItemStack item){if(!supported()||item.isEmpty()||!net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()).toString().equals("irons_spellbooks:scroll"))return null;try{
  if(!Class.forName("io.redspace.ironsspellbooks.item.Scroll").isInstance(item.getItem()))return null;var containers=Class.forName("io.redspace.ironsspellbooks.api.spells.ISpellContainer");var c=containers.getMethod("get",ItemStack.class).invoke(null,item);if((Integer)containers.getMethod("getMaxSpellCount").invoke(c)!=1||(Integer)containers.getMethod("getActiveSpellCount").invoke(c)!=1)return null;var data=containers.getMethod("getSpellAtIndex",int.class).invoke(c,0);var dataType=Class.forName("io.redspace.ironsspellbooks.api.spells.SpellData");var rarity=Class.forName("io.redspace.ironsspellbooks.api.spells.SpellRarity");if(!dataType.getMethod("getRarity").invoke(data).equals(rarity.getField("RARE").get(null)))return null;var spell=dataType.getMethod("getSpell").invoke(data);var type=Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell");int level=(Integer)dataType.getMethod("getLevel").invoke(data);if(!(Boolean)type.getMethod("isEnabled").invoke(spell)||!(Boolean)type.getMethod("allowCrafting").invoke(spell)||!(Boolean)type.getMethod("canBeCraftedBy",net.minecraft.world.entity.player.Player.class).invoke(spell,p)||level<(Integer)type.getMethod("getMinLevel").invoke(spell)||level>(Integer)type.getMethod("getMaxLevel").invoke(spell))return null;return new LibrarySpellCatalogue.Fact(type.getMethod("getSpellResource").invoke(spell).toString(),level,"RARE");
 }catch(ReflectiveOperationException|RuntimeException unavailable){return null;}}
}
