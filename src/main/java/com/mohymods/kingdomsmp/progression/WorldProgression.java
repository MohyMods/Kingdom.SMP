package com.mohymods.kingdomsmp.progression;

import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

/** Versioned, pinned world identity. Historical records always win over fresh-world inference. */
public final class WorldProgression {
    public enum Mode { LEGACY_LODGE, ROYAL_HALL }
    private static final Map<MinecraftServer,WorldProgression> CACHE=new WeakHashMap<>();
    private final Path file; private final UUID identity; private final Mode mode;
    private UUID owner;
    private boolean quarantined;
    private final Map<UUID,String> names=new HashMap<>();
    private WorldProgression(Path file,UUID identity,Mode mode,UUID owner){this.file=file;this.identity=identity;this.mode=mode;this.owner=owner;}
    public Mode mode(){healthy();return mode;} public UUID owner(){healthy();return owner;}
    private void healthy(){if(quarantined)throw new IllegalStateException("Progression write was uncertain; restart and inspect the checkpoint.");}
    public static WorldProgression get(MinecraftServer server){
        var p=CACHE.get(server);if(p!=null){p.healthy();return p;}
        try{
            var single=server.getSingleplayerProfile();
            boolean fixture=net.neoforged.fml.ModList.get().isLoaded("kingdom_smp_tests")&&Boolean.getBoolean("kingdom.testLegacyProfile");
            p=open(server.getWorldPath(LevelResource.ROOT).resolve("data"),server.overworld().getGameTime(),single==null?null:single.getId(),fixture);
            CACHE.put(server,p);return p;
        }catch(IOException|RuntimeException failure){throw new IllegalStateException("World progression requires recovery; no profile conversion permitted",failure);}
    }
    public static WorldProgression open(Path dir,long gameTime,UUID soloOwner,boolean legacyFixture)throws IOException{
        Path file=dir.resolve("kingdomsmp_progression.dat"),seal=dir.resolve("kingdomsmp_progression.created");
        if(Files.exists(file)!=Files.exists(seal))throw new IOException("Progression identity/checkpoint mismatch; restore matching world backup");
        if(Files.exists(file)){
            var tag=AtomicNbt.read(file);var marker=AtomicNbt.read(seal);
            if(tag.getInt("schema")!=1||!tag.hasUUID("identity")||!marker.hasUUID("identity")||!tag.getUUID("identity").equals(marker.getUUID("identity"))||!tag.getString("mode").equals(marker.getString("mode"))||!tag.contains("names",Tag.TAG_COMPOUND)||tag.contains("owner")&&!tag.hasUUID("owner"))throw new IOException("Invalid progression identity/schema");
            WorldProgression p;
            try{p=new WorldProgression(file,tag.getUUID("identity"),Mode.valueOf(tag.getString("mode")),tag.hasUUID("owner")?tag.getUUID("owner"):null);}
            catch(IllegalArgumentException e){throw new IOException("Unsupported progression profile",e);}
            var names=tag.getCompound("names");if(names.getAllKeys().size()>128)throw new IOException("Too many resident names");
            for(String key:names.getAllKeys())try{String name=names.getString(key);validateName(name);p.names.put(UUID.fromString(key),name);}catch(IllegalArgumentException e){throw new IOException("Invalid resident record",e);}
            return p;
        }
        boolean historical=false;
        if(Files.isDirectory(dir))try(var paths=Files.list(dir)){historical=paths.anyMatch(path->path.getFileName().toString().startsWith("kingdomsmp_"));}
        Mode mode=historical||gameTime>0||legacyFixture?Mode.LEGACY_LODGE:Mode.ROYAL_HALL;
        var p=new WorldProgression(file,UUID.randomUUID(),mode,mode==Mode.ROYAL_HALL?soloOwner:null);
        var marker=new CompoundTag();marker.putUUID("identity",p.identity);marker.putString("mode",mode.name());AtomicNbt.write(seal,marker);p.save();return p;
    }
    public void assignOwner(UUID player)throws IOException{
        healthy();Objects.requireNonNull(player);
        if(mode!=Mode.ROYAL_HALL)throw new IllegalStateException("Legacy worlds retain their original bootstrap.");
        if(owner!=null&&!owner.equals(player))throw new IllegalStateException("The starting owner is already pinned.");
        if(owner==null){owner=player;try{save();}catch(IOException e){owner=null;throw e;}}
    }
    public String resident(UUID project){return resident(project,"Elara Vale");}
    public String resident(UUID project,String fallback){healthy();return names.getOrDefault(project,fallback);}
    public void name(UUID project,String value)throws IOException{healthy();validateName(value);String previous=names.put(project,value);try{save();}catch(IOException e){if(previous==null)names.remove(project);else names.put(project,previous);throw e;}}
    private static void validateName(String value){if(value.isBlank()||value.length()>48||value.codePoints().anyMatch(c->Character.isISOControl(c)||c==167))throw new IllegalArgumentException("Use a readable resident name of 1–48 characters without formatting codes.");}
    private void save()throws IOException{
        var tag=new CompoundTag();tag.putInt("schema",1);tag.putUUID("identity",identity);tag.putString("mode",mode.name());if(owner!=null)tag.putUUID("owner",owner);
        var residentNames=new CompoundTag();names.forEach((id,name)->residentNames.putString(id.toString(),name));tag.put("names",residentNames);try{AtomicNbt.write(file,tag);}catch(IOException e){quarantined=true;throw e;}
    }
}
