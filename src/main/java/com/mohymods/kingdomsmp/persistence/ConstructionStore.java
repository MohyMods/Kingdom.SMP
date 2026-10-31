package com.mohymods.kingdomsmp.persistence;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Durable entitlements, reservations, projects and private administrator audit. */
public final class ConstructionStore {
    public enum Phase { AVAILABLE, CLAIMED, PROPOSED, APPROVED, BUILDING, COMPLETE }
    public record Project(UUID id, String reward, ResourceLocation blueprint, int blueprintVersion,
            UUID owner, String ownerName, Phase phase, UUID token, BlockPos origin, Rotation rotation,
            UUID proposal, UUID approvedBy, int placed, UUID npc, boolean npcIssued, boolean npcDead) {
        public Project {
            Objects.requireNonNull(id); Objects.requireNonNull(owner); Objects.requireNonNull(npc);
            Objects.requireNonNull(blueprint); Objects.requireNonNull(phase); Objects.requireNonNull(rotation);
            if(reward==null||reward.isBlank()||reward.length()>64||ownerName==null||ownerName.isBlank()||ownerName.length()>48
                    ||blueprintVersion<1||placed<0||placed>32768)throw new IllegalArgumentException("Invalid construction record");
            origin=origin==null?null:origin.immutable();
            boolean site=phase.ordinal()>=Phase.PROPOSED.ordinal(), started=phase.ordinal()>=Phase.BUILDING.ordinal();
            if(site!=(origin!=null)||site!=(proposal!=null)|| (phase.ordinal()>=Phase.APPROVED.ordinal())!=(approvedBy!=null)
                    || (phase!=Phase.AVAILABLE&&!started)!=(token!=null) || !started&&(placed!=0||npcIssued||npcDead)
                    ||phase==Phase.COMPLETE&&!npcIssued||npcDead&&!npcIssued)throw new IllegalArgumentException("Inconsistent construction phase");
        }
        public Project claim() {
            if(phase.ordinal()>=Phase.BUILDING.ordinal())throw new IllegalStateException("This entitlement has already been redeemed");
            return copy(Phase.CLAIMED,UUID.randomUUID(),null,Rotation.NONE,null,null,0,false,false);
        }
        public Project propose(BlockPos pos, Rotation r) {
            if(phase==Phase.AVAILABLE||phase.ordinal()>=Phase.BUILDING.ordinal())throw new IllegalStateException("Blueprint is not available for proposal");
            return copy(Phase.PROPOSED,token,pos,r,UUID.randomUUID(),null,0,false,false);
        }
        public Project approve(UUID actor) {
            if(phase!=Phase.PROPOSED)throw new IllegalStateException("Proposal changed or is already approved");
            return copy(Phase.APPROVED,token,origin,rotation,proposal,actor,0,false,false);
        }
        public Project begin() {
            if(phase!=Phase.APPROVED)throw new IllegalStateException("Administrator approval is required");
            return copy(Phase.BUILDING,null,origin,rotation,proposal,approvedBy,0,false,false);
        }
        public Project advance(int next) {
            if(phase!=Phase.BUILDING||next<placed||next>32768)throw new IllegalStateException("Invalid construction progress");
            return copy(phase,null,origin,rotation,proposal,approvedBy,next,npcIssued,npcDead);
        }
        public Project issueNpc() {
            if(phase!=Phase.BUILDING||npcIssued)throw new IllegalStateException("NPC issuance already reserved");
            return copy(phase,null,origin,rotation,proposal,approvedBy,placed,true,false);
        }
        public Project complete() {
            if(phase!=Phase.BUILDING||!npcIssued||npcDead)throw new IllegalStateException("Building is not ready");
            return copy(Phase.COMPLETE,null,origin,rotation,proposal,approvedBy,placed,true,false);
        }
        public Project markDead() { return copy(phase,null,origin,rotation,proposal,approvedBy,placed,true,true); }
        private Project copy(Phase p,UUID t,BlockPos o,Rotation r,UUID q,UUID a,int n,boolean issued,boolean dead) {
            return new Project(id,reward,blueprint,blueprintVersion,owner,ownerName,p,t,o,r,q,a,n,npc,issued,dead);
        }
    }
    public record Audit(long time, UUID actor, UUID project, String action) { }
    private final Path file;
    private final UUID identity;
    @FunctionalInterface interface Writer {void write(Path file,CompoundTag tag) throws IOException;}
    private final Writer writer;
    private Map<UUID,Project> projects;
    private List<Audit> audit;
    private long revision;
    private boolean quarantined;
    private ConstructionStore(Path file,UUID identity,long revision,Map<UUID,Project> projects,List<Audit> audit,Writer writer) {
        this.file=file;this.identity=identity;this.revision=revision;this.projects=Map.copyOf(projects);this.audit=List.copyOf(audit);this.writer=writer;
    }
    public static ConstructionStore open(Path directory) throws IOException {
        return open(directory,AtomicNbt::write);
    }
    static ConstructionStore open(Path directory,Writer writer) throws IOException {
        Path file=directory.resolve("kingdomsmp_construction.dat"),marker=directory.resolve("kingdomsmp_construction.created");
        boolean exists=Files.exists(file,LinkOption.NOFOLLOW_LINKS);
        if(exists!=Files.exists(marker,LinkOption.NOFOLLOW_LINKS))throw new IOException("Construction journal/identity mismatch; restore a matching backup");
        if(!exists){
            UUID id=UUID.randomUUID();var store=new ConstructionStore(file,id,0,Map.of(),List.of(),writer);
            CompoundTag mark=new CompoundTag();mark.putUUID("identity",id);AtomicNbt.write(marker,mark);writer.write(file,store.encode(store.projects,store.audit,0));return store;
        }
        CompoundTag tag=AtomicNbt.read(file),mark=AtomicNbt.read(marker);
        require(tag,"schema",Tag.TAG_INT);require(tag,"revision",Tag.TAG_LONG);require(tag,"projects",Tag.TAG_LIST);require(tag,"audit",Tag.TAG_LIST);
        if(tag.getInt("schema")!=1||!tag.hasUUID("identity")||!mark.hasUUID("identity")
                ||!tag.getUUID("identity").equals(mark.getUUID("identity"))||tag.getLong("revision")<0)throw new IOException("Unsupported or corrupt construction journal");
        ListTag list=tag.getList("projects",Tag.TAG_COMPOUND),entries=tag.getList("audit",Tag.TAG_COMPOUND);
        if(list.size()!=((ListTag)tag.get("projects")).size()||list.size()>128||entries.size()>256
                ||entries.size()!=((ListTag)tag.get("audit")).size())throw new IOException("Invalid construction journal bounds");
        Map<UUID,Project> projects=new LinkedHashMap<>();Set<String> rewards=new HashSet<>();
        for(int i=0;i<list.size();i++){
            var p=decode(list.getCompound(i));if(projects.put(p.id(),p)!=null||!rewards.add(p.reward()))throw new IOException("Duplicate construction entitlement");
        }
        List<Audit> audit=new ArrayList<>();
        for(int i=0;i<entries.size();i++){
            var e=entries.getCompound(i);require(e,"time",Tag.TAG_LONG);require(e,"action",Tag.TAG_STRING);
            if(!e.hasUUID("actor")||!e.hasUUID("project")||e.getString("action").length()>96||e.getLong("time")<0)throw new IOException("Invalid construction audit");
            audit.add(new Audit(e.getLong("time"),e.getUUID("actor"),e.getUUID("project"),e.getString("action")));
        }
        return new ConstructionStore(file,tag.getUUID("identity"),tag.getLong("revision"),projects,audit,writer);
    }
    public long revision(){return revision;}
    public UUID identity(){return identity;}
    public boolean quarantined(){return quarantined;}
    public Collection<Project> projects(){return projects.values();}
    public List<Audit> audit(){return audit;}
    public Optional<Project> reward(String key){return projects.values().stream().filter(p->p.reward().equals(key)).findFirst();}
    public Project get(UUID id){var p=projects.get(id);if(p==null)throw new IllegalArgumentException("Unknown construction project");return p;}
    public Project award(String reward,ResourceLocation blueprint,int version,UUID owner,String name) throws IOException {
        var previous=reward(reward);if(previous.isPresent()){
            var p=previous.get();if(!p.owner().equals(owner)||!p.blueprint().equals(blueprint)||p.blueprintVersion()!=version)throw new IOException("Reward identity differs from permanent winner");return p;
        }
        if(projects.size()>=128)throw new IOException("Construction registry capacity reached");
        var p=new Project(UUID.randomUUID(),reward,blueprint,version,owner,name,Phase.AVAILABLE,null,null,Rotation.NONE,null,null,0,UUID.randomUUID(),false,false);
        save(p,owner,"AWARD");return p;
    }
    /** All live mutations call this on the server thread, before external side effects. */
    public void save(Project project,UUID actor,String action) throws IOException {
        if(quarantined)throw new IOException("Construction storage quarantined; restart/reconcile first");
        if(action.isBlank()||action.length()>96||actor==null)throw new IllegalArgumentException("Invalid audit action");
        Project old=projects.get(project.id());
        if(old==null&&(projects.size()>=128||projects.values().stream().anyMatch(p->p.reward().equals(project.reward()))))throw new IllegalArgumentException("Duplicate or excessive construction entitlement");
        if(old!=null&&(!old.owner().equals(project.owner())||!old.reward().equals(project.reward())
                ||!old.blueprint().equals(project.blueprint())||old.blueprintVersion()!=project.blueprintVersion()||!old.npc().equals(project.npc())
                ||old.phase().ordinal()>=Phase.BUILDING.ordinal()&&(project.phase().ordinal()<old.phase().ordinal()||project.placed()<old.placed())))
            throw new IllegalArgumentException("Construction identity/progress cannot be rewritten");
        Map<UUID,Project> next=new LinkedHashMap<>(projects);next.put(project.id(),project);
        List<Audit> history=new ArrayList<>(audit);
        if(!action.equals("PROGRESS")){history.add(new Audit(System.currentTimeMillis(),actor,project.id(),action));if(history.size()>256)history.removeFirst();}
        long rev=Math.incrementExact(revision);
        try{writer.write(file,encode(next,history,rev));}
        catch(IOException|RuntimeException failure){quarantined=true;throw failure;}
        projects=Map.copyOf(next);audit=List.copyOf(history);revision=rev;
    }
    private CompoundTag encode(Map<UUID,Project> records,List<Audit> history,long rev){
        CompoundTag tag=new CompoundTag();tag.putInt("schema",1);tag.putUUID("identity",identity);tag.putLong("revision",rev);
        ListTag list=new ListTag();records.values().stream().sorted(Comparator.comparing(Project::id)).forEach(p->list.add(encode(p)));tag.put("projects",list);
        ListTag entries=new ListTag();for(var a:history){CompoundTag e=new CompoundTag();e.putLong("time",a.time());e.putUUID("actor",a.actor());e.putUUID("project",a.project());e.putString("action",a.action());entries.add(e);}tag.put("audit",entries);return tag;
    }
    private static CompoundTag encode(Project p){
        CompoundTag t=new CompoundTag();t.putUUID("id",p.id());t.putString("reward",p.reward());t.putString("blueprint",p.blueprint().toString());t.putInt("version",p.blueprintVersion());
        t.putUUID("owner",p.owner());t.putString("ownerName",p.ownerName());t.putString("phase",p.phase().name());t.putString("rotation",p.rotation().name());
        if(p.token()!=null)t.putUUID("token",p.token());if(p.origin()!=null)t.putLong("origin",p.origin().asLong());if(p.proposal()!=null)t.putUUID("proposal",p.proposal());if(p.approvedBy()!=null)t.putUUID("approvedBy",p.approvedBy());
        t.putInt("placed",p.placed());t.putUUID("npc",p.npc());t.putBoolean("npcIssued",p.npcIssued());t.putBoolean("npcDead",p.npcDead());return t;
    }
    private static Project decode(CompoundTag t){
        for(String k:List.of("reward","blueprint","ownerName","phase","rotation"))require(t,k,Tag.TAG_STRING);
        for(String k:List.of("version","placed"))require(t,k,Tag.TAG_INT);
        for(String k:List.of("npcIssued","npcDead"))require(t,k,Tag.TAG_BYTE);
        for(String k:List.of("id","owner","npc"))if(!t.hasUUID(k))throw new IllegalArgumentException("Invalid construction identity");
        for(String k:List.of("token","proposal","approvedBy"))if(t.contains(k)&&!t.hasUUID(k))throw new IllegalArgumentException("Invalid construction identity");
        if(t.contains("origin"))require(t,"origin",Tag.TAG_LONG);
        return new Project(t.getUUID("id"),t.getString("reward"),ResourceLocation.parse(t.getString("blueprint")),t.getInt("version"),t.getUUID("owner"),t.getString("ownerName"),Phase.valueOf(t.getString("phase")),
                t.hasUUID("token")?t.getUUID("token"):null,t.contains("origin")?BlockPos.of(t.getLong("origin")):null,Rotation.valueOf(t.getString("rotation")),
                t.hasUUID("proposal")?t.getUUID("proposal"):null,t.hasUUID("approvedBy")?t.getUUID("approvedBy"):null,t.getInt("placed"),t.getUUID("npc"),t.getBoolean("npcIssued"),t.getBoolean("npcDead"));
    }
    private static void require(CompoundTag t,String k,int type){if(!t.contains(k,type))throw new IllegalArgumentException("Invalid construction field: "+k);}
}
