package com.mohymods.kingdomsmp.gametest;
import java.nio.file.*;
import net.minecraft.nbt.*;
/** Test-only stopped private clone: future chunks get ordinary decorated stone terrain; existing chunks stay untouched. */
public final class NativeOreFixturePrep {
 public static void main(String[] args)throws Exception{
  if(args.length!=1)throw new IllegalArgumentException("One stopped private fixture path required");var root=Path.of("").toAbsolutePath().normalize();var fixture=Path.of(args[0]).toRealPath();if(!fixture.getParent().equals(root.resolve("work"))||!fixture.getFileName().toString().startsWith("claim-server.")||!Files.isRegularFile(fixture.resolve("claims-fixture/smith-repair-runtime-proof.nbt")))throw new IllegalArgumentException("Never change user worlds; use the guarded stopped private clone");
  var world=fixture.resolve("claims-fixture");try(var channel=java.nio.channels.FileChannel.open(world.resolve("session.lock"),StandardOpenOption.READ,StandardOpenOption.WRITE);var lock=channel.tryLock()){
   if(lock==null)throw new IllegalStateException("Fixture is in use");var path=world.resolve("level.dat");var tag=NbtIo.readCompressed(path,NbtAccounter.unlimitedHeap());var generator=tag.getCompound("Data").getCompound("WorldGenSettings").getCompound("dimensions").getCompound("minecraft:overworld").getCompound("generator");if(!generator.getString("type").equals("minecraft:flat"))throw new IllegalStateException("Expected original private flat fixture");var settings=generator.getCompound("settings");settings.putString("biome","minecraft:plains");settings.putBoolean("features",true);settings.putBoolean("lakes",false);var layers=new ListTag();for(var row:new String[][]{{"minecraft:bedrock","1"},{"minecraft:stone","124"},{"minecraft:dirt","3"},{"minecraft:grass_block","1"}}){var t=new CompoundTag();t.putString("block",row[0]);t.putInt("height",Integer.parseInt(row[1]));layers.add(t);}settings.put("layers",layers);generator.put("settings",settings);NbtIo.writeCompressed(tag,path);System.out.println("Private future ore terrain prepared; existing region chunks and all kingdom records unchanged");
  }
 }
}
