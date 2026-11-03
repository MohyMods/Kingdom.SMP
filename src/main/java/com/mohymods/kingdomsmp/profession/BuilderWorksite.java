package com.mohymods.kingdomsmp.profession;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/** Immutable operator authorization, projected from the canonical journal; never changes world blocks. */
public record BuilderWorksite(UUID id,UUID admin,UUID player,String quest,String dimension,BlockPos from,BlockPos to,String reason) {
    public BuilderWorksite {
        Objects.requireNonNull(id);Objects.requireNonNull(admin);Objects.requireNonNull(player);
        RetainedBuildingQuests.get(quest);
        if(dimension==null||dimension.length()>128||!ResourceLocation.parse(dimension).toString().equals(dimension))throw new IllegalArgumentException("Invalid worksite dimension");
        from=Objects.requireNonNull(from).immutable();to=Objects.requireNonNull(to).immutable();
        long width=(long)to.getX()-from.getX()+1,height=(long)to.getY()-from.getY()+1,depth=(long)to.getZ()-from.getZ()+1;
        if(width<1||width>64||height<1||height>32||depth<1||depth>64||width*height*depth>32768||Math.abs((long)from.getX())>=30_000_000||Math.abs((long)to.getX())>=30_000_000||Math.abs((long)from.getZ())>=30_000_000||Math.abs((long)to.getZ())>=30_000_000||from.getY()<-2048||to.getY()>2047)throw new IllegalArgumentException("Worksite must have ordered bounds, at most64×32×64 and32768cells within world limits");
        if(reason==null||reason.length()<12||reason.length()>96||reason.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Record12–96characters of project purpose, authorship and consent");
    }
    public boolean contains(BlockPos p){return p.getX()>=from.getX()&&p.getX()<=to.getX()&&p.getY()>=from.getY()&&p.getY()<=to.getY()&&p.getZ()>=from.getZ()&&p.getZ()<=to.getZ();}
    public AABB bounds(){return new AABB(from.getX(),from.getY(),from.getZ(),to.getX()+1,to.getY()+1,to.getZ()+1);}
    public CompoundTag save(){var t=new CompoundTag();t.putUUID("id",id);t.putUUID("admin",admin);t.putUUID("player",player);t.putString("quest",quest);t.putString("dimension",dimension);t.putLong("from",from.asLong());t.putLong("to",to.asLong());t.putString("reason",reason);return t;}
    public static BuilderWorksite load(CompoundTag t){
        if(!t.getAllKeys().equals(Set.of("id","admin","player","quest","dimension","from","to","reason"))||!t.hasUUID("id")||!t.hasUUID("admin")||!t.hasUUID("player")||!t.contains("quest",Tag.TAG_STRING)||!t.contains("dimension",Tag.TAG_STRING)||!t.contains("from",Tag.TAG_LONG)||!t.contains("to",Tag.TAG_LONG)||!t.contains("reason",Tag.TAG_STRING))throw new IllegalArgumentException("Invalid worksite authorization");
        return new BuilderWorksite(t.getUUID("id"),t.getUUID("admin"),t.getUUID("player"),t.getString("quest"),t.getString("dimension"),BlockPos.of(t.getLong("from")),BlockPos.of(t.getLong("to")),t.getString("reason"));
    }
}
