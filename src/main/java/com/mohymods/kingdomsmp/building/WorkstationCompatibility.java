package com.mohymods.kingdomsmp.building;
import java.lang.reflect.Method;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
/** Read-only equivalence for an exact verified optional workstation conversion. Never edits blocks/items. */
public final class WorkstationCompatibility {
 private WorkstationCompatibility(){}
 public static BlockState canonicalState(BlockState current,BlockState required){
  if(required.is(Blocks.CAULDRON)&&(current.is(Blocks.WATER_CAULDRON)||current.is(Blocks.LAVA_CAULDRON)||current.is(Blocks.POWDER_SNOW_CAULDRON)))return required;
  if(current.getBlock()==required.getBlock()||!required.is(Blocks.CRAFTING_TABLE))return current;
  var mods=ModList.get();if(mods==null||!mods.isLoaded("visualworkbench")||!mods.getModContainerById("visualworkbench").orElseThrow().getModInfo().getVersion().toString().equals("21.1.2"))return current;
  try{var converted=(BlockState)VisualWorkbench.TO_VANILLA.invoke(null,current);return converted!=null&&converted.is(Blocks.CRAFTING_TABLE)?converted:current;}catch(ReflectiveOperationException|LinkageError unavailable){return current;}
 }
 private static final class VisualWorkbench {
  static final Method TO_VANILLA=method();
  private static Method method(){try{return Class.forName("fuzs.visualworkbench.handler.BlockConversionHandler").getMethod("convertToVanillaBlock",BlockState.class);}catch(ReflectiveOperationException e){throw new ExceptionInInitializerError(e);}}
 }
}
