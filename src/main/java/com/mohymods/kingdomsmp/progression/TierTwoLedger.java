package com.mohymods.kingdomsmp.progression;

import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import net.minecraft.nbt.*;
import java.nio.file.*;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

/** Pinned Tier 2 costs, accepted history, one inventory preparation and permanent road approval. */
public final class TierTwoLedger {
    public static final int MAX_OPERATIONS=50_000;
    public record Receipt(UUID store,long sequence){
        public Receipt{if(sequence<0||store==null&&sequence!=0)throw new IllegalArgumentException("Invalid Tier 2 receipt");}
        public static Receipt empty(){return new Receipt(null,0);}
    }
    public record Donation(long sequence,UUID operation,UUID player,String name,String resource,int amount,long time){
        public Donation{Objects.requireNonNull(operation);Objects.requireNonNull(player);validText(name,48);identifier(resource);if(sequence<1||amount<1||amount>2304||time<0)throw new IllegalArgumentException("Invalid Tier 2 contribution");}
    }
    public record Pending(Donation donation,long previousReceipt){public Pending{Objects.requireNonNull(donation);if(previousReceipt<0||previousReceipt>=donation.sequence())throw new IllegalArgumentException("Invalid prepared Tier 2 receipt");}}
    public record Road(UUID actor,String reason,long time){public Road{Objects.requireNonNull(actor);validText(reason,128);if(time<0)throw new IllegalArgumentException("Invalid road approval");}}
    public record Standing(UUID player,String name,BigInteger scoreUnits,long reached){ }
    @FunctionalInterface interface Writer{void write(Path file,CompoundTag tag)throws IOException;}
    private final Path file;private final UUID identity;private final Writer writer;
    private final Map<String,Integer> requirements;private final BigInteger scoreScale;
    private List<Donation> history;private Pending pending;private Road road;private boolean quarantined;
    private TierTwoLedger(Path file,UUID identity,Writer writer,Map<String,Integer> requirements,List<Donation> history,Pending pending,Road road){
        this.file=file;this.identity=identity;this.writer=writer;this.requirements=Map.copyOf(requirements);this.history=List.copyOf(history);this.pending=pending;this.road=road;
        if(requirements.isEmpty()||requirements.size()>16)throw new IllegalArgumentException("Invalid Tier 2 cost count");
        BigInteger scale=BigInteger.ONE;
        for(var e:requirements.entrySet()){identifier(e.getKey());if(e.getValue()<1||e.getValue()>1_000_000)throw new IllegalArgumentException("Invalid Tier 2 cost");var n=BigInteger.valueOf(e.getValue());scale=scale.divide(scale.gcd(n)).multiply(n);}
        scoreScale=scale;
        Set<UUID> operations=new HashSet<>();Map<String,Integer> totals=new HashMap<>();
        for(int i=0;i<history.size();i++){var d=history.get(i);if(d.sequence()!=i+1L||!operations.add(d.operation())||!requirements.containsKey(d.resource()))throw new IllegalArgumentException("Unordered or duplicate Tier 2 history");int total=Math.addExact(totals.getOrDefault(d.resource(),0),d.amount());if(total>requirements.get(d.resource()))throw new IllegalArgumentException("Excessive Tier 2 credit");totals.put(d.resource(),total);}
        if(pending!=null){var d=pending.donation();if(d.sequence()!=history.size()+1L||operations.contains(d.operation())||pending.previousReceipt()!=expectedReceipt(d.player())||!requirements.containsKey(d.resource())||Math.addExact(totals.getOrDefault(d.resource(),0),d.amount())>requirements.get(d.resource()))throw new IllegalArgumentException("Invalid Tier 2 preparation");}
    }
    public static TierTwoLedger open(Path directory,Map<String,Integer> requirements)throws IOException{return open(directory,requirements,AtomicNbt::write);}
    static TierTwoLedger open(Path directory,Map<String,Integer> requirements,Writer writer)throws IOException{
        Path file=directory.resolve("kingdomsmp_tier_two.dat"),seal=directory.resolve("kingdomsmp_tier_two.created");
        if(Files.exists(file,LinkOption.NOFOLLOW_LINKS)!=Files.exists(seal,LinkOption.NOFOLLOW_LINKS))throw new IOException("Tier 2 journal/seal mismatch; restore matching world backup");
        if(!Files.exists(file,LinkOption.NOFOLLOW_LINKS)){var s=new TierTwoLedger(file,UUID.randomUUID(),writer,requirements,List.of(),null,null);var mark=new CompoundTag();mark.putUUID("identity",s.identity);AtomicNbt.write(seal,mark);writer.write(file,s.encode(s.history,null,null));return s;}
        try{
            var tag=AtomicNbt.read(file);var mark=AtomicNbt.read(seal);
            require(tag,"schema",Tag.TAG_INT);require(tag,"requirements",Tag.TAG_COMPOUND);require(tag,"history",Tag.TAG_LIST);
            if(tag.getInt("schema")!=1||!tag.hasUUID("identity")||!mark.hasUUID("identity")||!tag.getUUID("identity").equals(mark.getUUID("identity")))throw new IOException("Unsupported Tier 2 identity/schema");
            var costs=tag.getCompound("requirements");Map<String,Integer> pinned=new HashMap<>();for(String key:costs.getAllKeys()){require(costs,key,Tag.TAG_INT);pinned.put(key,costs.getInt(key));}
            if(!pinned.equals(requirements))throw new IOException("Tier 2 costs differ from pinned journal. Restore the matching configuration; do not reinterpret existing credit.");
            var rows=tag.getList("history",Tag.TAG_COMPOUND);if(rows.size()!=((ListTag)tag.get("history")).size()||rows.size()>MAX_OPERATIONS)throw new IOException("Invalid Tier 2 history bounds");
            List<Donation> history=new ArrayList<>();for(int i=0;i<rows.size();i++)history.add(decode(rows.getCompound(i)));
            Pending pending=null;if(tag.contains("pending")){require(tag,"pending",Tag.TAG_COMPOUND);var p=tag.getCompound("pending");require(p,"previousReceipt",Tag.TAG_LONG);pending=new Pending(decode(p),p.getLong("previousReceipt"));}
            Road road=null;if(tag.contains("road")){require(tag,"road",Tag.TAG_COMPOUND);var r=tag.getCompound("road");if(!r.hasUUID("actor"))throw new IOException("Malformed road actor");require(r,"reason",Tag.TAG_STRING);require(r,"time",Tag.TAG_LONG);road=new Road(r.getUUID("actor"),r.getString("reason"),r.getLong("time"));}
            return new TierTwoLedger(file,tag.getUUID("identity"),writer,pinned,history,pending,road);
        }catch(IllegalArgumentException|ArithmeticException e){throw new IOException("Corrupt Tier 2 checkpoint",e);}
    }
    public UUID id(){return identity;}public long revision(){return history.size()+(road==null?0L:1L);}public boolean quarantined(){return quarantined;}public void quarantine(){quarantined=true;}
    public Map<String,Integer> requirements(){return requirements;}public List<Donation> history(){return history;}public Optional<Pending> pending(){return Optional.ofNullable(pending);}public Optional<Road> road(){return Optional.ofNullable(road);}
    public int total(String resource){return history.stream().filter(d->d.resource().equals(resource)).mapToInt(Donation::amount).sum();}
    public int personal(UUID player,String resource){return history.stream().filter(d->d.player().equals(player)&&d.resource().equals(resource)).mapToInt(Donation::amount).sum();}
    public boolean resourcesComplete(){return requirements.entrySet().stream().allMatch(e->total(e.getKey())==e.getValue());}
    public boolean complete(){return resourcesComplete()&&road!=null;}
    public long expectedReceipt(UUID player){for(int i=history.size()-1;i>=0;i--)if(history.get(i).player().equals(player))return history.get(i).sequence();return 0;}
    public BigInteger scoreScale(){return scoreScale;}
    public List<Standing> standings(){
        Map<UUID,BigInteger> scores=new HashMap<>();Map<UUID,Donation> last=new HashMap<>();
        for(var d:history){scores.merge(d.player(),scoreScale.divide(BigInteger.valueOf(requirements.get(d.resource()))).multiply(BigInteger.valueOf(d.amount())),BigInteger::add);last.put(d.player(),d);}
        return scores.entrySet().stream().map(e->{var d=last.get(e.getKey());return new Standing(e.getKey(),d.name(),e.getValue(),d.sequence());}).sorted(Comparator.comparing(Standing::scoreUnits).reversed().thenComparingLong(Standing::reached).thenComparing(s->s.player().toString())).toList();
    }
    public Optional<Standing> winner(){return complete()?standings().stream().findFirst():Optional.empty();}
    public Pending prepare(UUID operation,UUID player,String name,String resource,int amount)throws IOException{
        healthy();if(pending!=null)throw new IllegalStateException("Tier 2 contribution recovery is pending");
        if(history.size()>=MAX_OPERATIONS)throw new IllegalStateException("Tier 2 journal capacity reached; no history was evicted");
        if(history.stream().anyMatch(d->d.operation().equals(operation)))throw new IllegalArgumentException("Replayed Tier 2 operation");
        if(!requirements.containsKey(resource)||amount<1||amount>2304||Math.addExact(total(resource),amount)>requirements.get(resource))throw new IllegalArgumentException("Invalid Tier 2 accepted amount");
        var next=new Pending(new Donation(history.size()+1L,operation,player,name,resource,amount,System.currentTimeMillis()),expectedReceipt(player));persist(history,next,road);pending=next;return next;
    }
    /** True commits accepted inventory removal; false cancels a pre-removal preparation. */
    public boolean recover(Receipt receipt)throws IOException{
        healthy();if(pending==null)return false;var p=pending;var d=p.donation();boolean same=identity.equals(receipt.store());
        if(same&&receipt.sequence()==d.sequence()){List<Donation> next=new ArrayList<>(history);next.add(d);persist(next,null,road);history=List.copyOf(next);pending=null;return true;}
        if(receipt.sequence()==p.previousReceipt()&&(same||receipt.store()==null&&receipt.sequence()==0)){persist(history,null,road);pending=null;return false;}
        throw new IOException("Native inventory receipt differs from prepared Tier 2 contribution");
    }
    public boolean approveRoad(UUID actor,String reason)throws IOException{
        healthy();if(pending!=null)throw new IllegalStateException("Reconcile the prepared inventory contribution first");if(road!=null)return false;
        var next=new Road(actor,reason,System.currentTimeMillis());persist(history,null,next);road=next;return true;
    }
    private void healthy(){if(quarantined)throw new IllegalStateException("Tier 2 storage quarantined; restart and inspect matching saves");}
    private void persist(List<Donation> h,Pending p,Road r)throws IOException{try{writer.write(file,encode(h,p,r));}catch(IOException|RuntimeException e){quarantined=true;throw e;}}
    private CompoundTag encode(List<Donation> h,Pending p,Road r){var t=new CompoundTag();t.putInt("schema",1);t.putUUID("identity",identity);var costs=new CompoundTag();requirements.forEach(costs::putInt);t.put("requirements",costs);var rows=new ListTag();h.forEach(d->rows.add(encode(d)));t.put("history",rows);if(p!=null){var v=encode(p.donation());v.putLong("previousReceipt",p.previousReceipt());t.put("pending",v);}if(r!=null){var v=new CompoundTag();v.putUUID("actor",r.actor());v.putString("reason",r.reason());v.putLong("time",r.time());t.put("road",v);}return t;}
    private static CompoundTag encode(Donation d){var t=new CompoundTag();t.putLong("sequence",d.sequence());t.putUUID("operation",d.operation());t.putUUID("player",d.player());t.putString("name",d.name());t.putString("resource",d.resource());t.putInt("amount",d.amount());t.putLong("time",d.time());return t;}
    private static Donation decode(CompoundTag t){for(String k:List.of("sequence","time"))require(t,k,Tag.TAG_LONG);for(String k:List.of("resource","name"))require(t,k,Tag.TAG_STRING);require(t,"amount",Tag.TAG_INT);if(!t.hasUUID("player")||!t.hasUUID("operation"))throw new IllegalArgumentException("Malformed Tier 2 donor");return new Donation(t.getLong("sequence"),t.getUUID("operation"),t.getUUID("player"),t.getString("name"),t.getString("resource"),t.getInt("amount"),t.getLong("time"));}
    private static void require(CompoundTag t,String key,int type){if(!t.contains(key,type))throw new IllegalArgumentException("Malformed Tier 2 field: "+key);}
    private static void identifier(String id){if(id==null||!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))throw new IllegalArgumentException("Invalid Tier 2 resource ID");}
    private static void validText(String value,int max){if(value==null||value.isBlank()||value.length()>max||value.codePoints().anyMatch(c->Character.isISOControl(c)||c=='§'))throw new IllegalArgumentException("Invalid Tier 2 text");}
}
