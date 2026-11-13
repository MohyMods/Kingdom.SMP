package com.mohymods.kingdomsmp.bounty;

import com.mohymods.kingdomsmp.economy.HuntState;
import net.minecraft.nbt.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Bounded non-monetary evidence. UUID-bound signed spawn provenance survives entity storage/restarts. */
public final class HuntEvidence {
    private record Progress(long day,int count) { }
    private final byte[] secret;
    private final Map<UUID,Long> victims = new HashMap<>();
    private final Map<UUID,Progress> progress = new HashMap<>();
    public HuntEvidence() { secret=new byte[32];new SecureRandom().nextBytes(secret); }
    private HuntEvidence(byte[] secret) { this.secret=secret.clone(); }
    public CompoundTag spawn(UUID mob,String type,String dimension,long day) {
        if (!HuntState.TARGETS.contains(type) || !dimension.equals("minecraft:overworld") || day<0) throw new IllegalArgumentException("Invalid hunt origin");
        var tag=new CompoundTag();tag.putLong("day",day);tag.putByteArray("signature",sign(mob,type,dimension,day));return tag;
    }
    private byte[] sign(UUID mob,String type,String dimension,long day) {
        try {
            var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret,"HmacSHA256"));
            return mac.doFinal((mob+"|"+type+"|"+dimension+"|"+day).getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) { throw new IllegalStateException("Hunt provenance unavailable",e); }
    }
    public int count(UUID accepted) { var p=progress.get(accepted);return p==null?0:p.count(); }
    public void prune(long today) { victims.values().removeIf(day->day<today-2);progress.values().removeIf(p->p.day()<today); }
    public boolean record(HuntState hunt,UUID mob,String type,String dimension,CompoundTag proof,long today) {
        prune(today);
        if (hunt.closed() || hunt.day()!=today || !hunt.target().equals(type) || !dimension.equals("minecraft:overworld") ||
            !proof.getAllKeys().equals(Set.of("day","signature")) || !proof.contains("day",Tag.TAG_LONG) || !proof.contains("signature",Tag.TAG_BYTE_ARRAY)) return false;
        long born=proof.getLong("day");
        if (born>today || born<today-1 || !MessageDigest.isEqual(proof.getByteArray("signature"),sign(mob,type,dimension,born)) ||
            victims.containsKey(mob) || victims.size()>=16384 || progress.size()>=1024&&!progress.containsKey(hunt.id()) || count(hunt.id())>=hunt.goal()) return false;
        victims.put(mob,today);progress.put(hunt.id(),new Progress(today,count(hunt.id())+1));return true;
    }
    public CompoundTag save() {
        var tag=new CompoundTag();tag.putInt("schema",1);tag.putByteArray("secret",secret);
        var dead=new ListTag();victims.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->{var row=new CompoundTag();row.putUUID("mob",e.getKey());row.putLong("day",e.getValue());dead.add(row);});tag.put("victims",dead);
        var counts=new ListTag();progress.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->{var row=new CompoundTag();row.putUUID("accepted",e.getKey());row.putLong("day",e.getValue().day());row.putInt("count",e.getValue().count());counts.add(row);});tag.put("progress",counts);return tag;
    }
    public static HuntEvidence load(CompoundTag tag) {
        if (!tag.getAllKeys().equals(Set.of("schema","secret","victims","progress")) || !tag.contains("schema",Tag.TAG_INT) || tag.getInt("schema")!=1 || !tag.contains("secret",Tag.TAG_BYTE_ARRAY) || tag.getByteArray("secret").length!=32) throw new IllegalArgumentException("Invalid hunt evidence");
        var result=new HuntEvidence(tag.getByteArray("secret"));
        var dead=rows(tag,"victims",16384);var counts=rows(tag,"progress",1024);
        for (var value:dead) { var row=(CompoundTag)value;if(!row.getAllKeys().equals(Set.of("mob","day"))||!row.hasUUID("mob")||!row.contains("day",Tag.TAG_LONG)||row.getLong("day")<0||result.victims.put(row.getUUID("mob"),row.getLong("day"))!=null) throw new IllegalArgumentException("Invalid hunt victim"); }
        for (var value:counts) { var row=(CompoundTag)value;if(!row.getAllKeys().equals(Set.of("accepted","day","count"))||!row.hasUUID("accepted")||!row.contains("day",Tag.TAG_LONG)||row.getLong("day")<0||!row.contains("count",Tag.TAG_INT)||row.getInt("count")<1||row.getInt("count")>256||result.progress.put(row.getUUID("accepted"),new Progress(row.getLong("day"),row.getInt("count")))!=null) throw new IllegalArgumentException("Invalid hunt progress"); }
        return result;
    }
    private static ListTag rows(CompoundTag tag,String key,int max) { if(!(tag.get(key) instanceof ListTag list)||list.size()>max||!list.isEmpty()&&list.getElementType()!=Tag.TAG_COMPOUND)throw new IllegalArgumentException("Invalid hunt rows");return list; }
}
