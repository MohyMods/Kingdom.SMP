package com.mohymods.kingdomsmp.milestone;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MilestoneCatalogTest {
    private String source()throws Exception{try(var in=getClass().getResourceAsStream("/data/kingdom_smp/kingdom/milestones.json")){return new String(Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8);}}
    private JsonObject tree()throws Exception{return JsonParser.parseString(source()).getAsJsonObject();}
    private MilestoneCatalog read(JsonObject o)throws Exception{return MilestoneJson.read(new StringReader(o.toString()));}
    private void invalid(JsonObject o){assertThrows(IOException.class,()->read(o));}
    private JsonObject tier(JsonObject o,int tier){return o.getAsJsonArray("milestones").get(tier-1).getAsJsonObject();}
    private JsonObject requirement(JsonObject o,int tier,int index){return tier(o,tier).getAsJsonArray("requirements").get(index).getAsJsonObject();}
    private MilestoneValidator.Report validate(JsonObject o)throws Exception{return MilestoneValidator.validate(read(o),s->Set.of(s));}
    private String notes(MilestoneValidator.Report r,int tier){return String.join("\n",r.issues(tier))+String.join("\n",r.catalogIssues());}
    @Test void bundledDefinitionsKeepExactUserCandidatesAndPinnedHistory()throws Exception{
        var c=read(tree());assertTrue(MilestoneService.legacyCompatible(c.tier(1)));assertEquals(12,c.milestones().size());
        Map<String,Long> costs=new HashMap<>();c.tier(2).requirements().forEach(r->costs.put(r.ingredient(),r.quantity()));
        assertEquals(Map.of("#minecraft:logs",2048L,"minecraft:cobblestone",3072L,"minecraft:wheat",768L,"minecraft:iron_ingot",192L,"minecraft:copper_ingot",384L,"minecraft:glass",256L,"minecraft:brick",384L,"minecraft:leather",64L),costs);
        assertEquals("proposal",c.tier(2).approval());assertEquals("kingdom_smp:royal_storehouse",c.tier(2).reward());
        assertEquals(12,c.tier(12).nextTier());assertEquals("kingdom_smp:town_hall",c.tier(12).reward());
    }
    @Test void strictJsonRejectsUnknownDuplicateTrailingAndUnsupportedData()throws Exception{
        var o=tree();o.addProperty("enabled",true);invalid(o);
        assertThrows(IOException.class,()->MilestoneJson.read(new StringReader(source().replaceFirst("\\\"schema\\\": 1","\"schema\": 1, \"schema\": 1"))));
        assertThrows(IOException.class,()->MilestoneJson.read(new StringReader(source()+"{}")));
        o=tree();o.addProperty("schema",2);var future=o;assertThrows(IOException.class,()->read(future));
    }
    @Test void inputAndNestingAreBounded()throws Exception{
        assertThrows(IOException.class,()->MilestoneJson.read(new StringReader(" ".repeat(MilestoneJson.MAX_CHARACTERS+1))));
        assertThrows(IOException.class,()->MilestoneJson.read(new StringReader("[".repeat(34)+"1"+"]".repeat(34))));
    }
    @Test void quantitiesRejectNegativeFractionalAndOverflow()throws Exception{
        for(Number n:List.of(-1,0,1.5,1_000_000_001L,Long.MAX_VALUE)){
            var o=tree();requirement(o,2,0).addProperty("quantity",n);invalid(o);
        }
    }
    @Test void ingredientHasExactlyOneRegisteredSelectorShape()throws Exception{
        var o=tree();requirement(o,2,0).addProperty("item","minecraft:oak_log");invalid(o);
        o=tree();requirement(o,2,0).remove("tag");var missing=o;assertThrows(IOException.class,()->read(missing));
        o=tree();requirement(o,2,0).addProperty("tag","Bad ID");var malformed=o;assertThrows(IOException.class,()->read(malformed));
    }
    @Test void duplicateRequirementsAndSkippedTiersFail()throws Exception{
        var o=tree();tier(o,2).getAsJsonArray("requirements").add(requirement(o,2,0).deepCopy());invalid(o);
        o=tree();tier(o,2).addProperty("next_tier",4);var skipped=o;assertThrows(IOException.class,()->read(skipped));
        o=tree();tier(o,12).addProperty("next_tier",13);var thirteenth=o;assertThrows(IOException.class,()->read(thirteenth));
    }
    @Test void baselineKnownVanillaSourcesValidateWithoutModAssumptions()throws Exception{
        var r=validate(tree());assertTrue(r.issues(1).isEmpty(),r.issues(1).toString());assertTrue(r.catalogIssues().isEmpty(),r.catalogIssues().toString());
        assertFalse(notes(r,2).contains("Inaccessible"));assertTrue(notes(r,6).contains("Acquisition path not verified: create:brass_ingot"));
    }
    @Test void netherCostToUnlockNetherIsDetectedEvenIfAuthorOmitsPrerequisite()throws Exception{
        var o=tree();requirement(o,4,0).addProperty("item","minecraft:quartz");var notes=notes(validate(o),4);
        assertTrue(notes.contains("requires Tier 5"),notes);assertTrue(notes.contains("Circular progression"),notes);
    }
    @Test void netherCostsAfterUnlockHaveNoDimensionCycle()throws Exception{
        var notes=notes(validate(tree()),5);assertFalse(notes.contains("Inaccessible"),notes);assertFalse(notes.contains("Circular progression"),notes);
    }
    @Test void endCostToUnlockEndIsDetectedAndTierNineSourcesAreAllowed()throws Exception{
        var o=tree();requirement(o,8,0).addProperty("item","minecraft:dragon_breath");var notes=notes(validate(o),8);
        assertTrue(notes.contains("requires Tier 9"),notes);assertTrue(notes.contains("Circular progression"),notes);
        assertFalse(notes(validate(tree()),9).contains("Inaccessible"));
    }
    @Test void fabricatedEarlierUnlockCannotBypassVerifiedRealmGate()throws Exception{
        var o=tree();o.getAsJsonArray("capabilities").forEach(e->{if(e.getAsJsonObject().get("id").getAsString().equals("kingdom_smp:nether"))e.getAsJsonObject().addProperty("available_from_tier",1);});
        requirement(o,4,0).addProperty("item","minecraft:quartz");String notes=notes(validate(o),4);
        assertTrue(notes.contains("Fixed unlock tier changed"));assertTrue(notes.contains("requires Tier 5"));
    }
    @Test void circularTechnologyAndUnknownReferencesAreReported()throws Exception{
        var o=tree();var caps=o.getAsJsonArray("capabilities");
        caps.get(9).getAsJsonObject().getAsJsonArray("requires").add("kingdom_smp:mekanism_processing");
        caps.get(10).getAsJsonObject().getAsJsonArray("requires").add("kingdom_smp:create_processing");
        assertTrue(notes(validate(o),6).contains("Circular progression"));
        o=tree();requirement(o,2,0).getAsJsonArray("requires").add("kingdom_smp:missing");assertTrue(notes(validate(o),2).contains("Unknown prerequisite"));
    }
    @Test void unusedCycleIsNotSilentlyAccepted()throws Exception{
        var o=tree();o.getAsJsonArray("capabilities").get(9).getAsJsonObject().getAsJsonArray("requires").add("kingdom_smp:create_processing");
        assertTrue(validate(o).catalogIssues().stream().anyMatch(s->s.contains("Circular progression")));
    }
    @Test void missingRegistrationAndOverlappingIngredientsFailClosed()throws Exception{
        var c=read(tree());var absent=MilestoneValidator.validate(c,s->Set.of());assertTrue(notes(absent,1).contains("Unresolved item or empty tag"));
        var overlap=MilestoneValidator.validate(c,s->Set.of("minecraft:oak_log"));assertTrue(notes(overlap,1).contains("Overlapping ingredients"));
    }
    @Test void registrationAloneDoesNotVerifyModRecipesOrMasterPrerequisites()throws Exception{
        var notes=notes(validate(tree()),10);assertTrue(notes.contains("Unverified technology/profession capability"));assertTrue(notes.contains("Unlock tier undecided: kingdom_smp:master_production"));
        var o=tree();requirement(o,2,0).remove("tag");requirement(o,2,0).addProperty("item","minecraft:netherite_scrap");assertTrue(notes(validate(o),2).contains("Acquisition path not verified"));
    }
    @Test void approvalTextInPackCannotEnableFutureCostsOrRewriteHistory()throws Exception{
        var o=tree();tier(o,2).addProperty("approval","approved");assertFalse(MilestoneService.legacyCompatible(read(o).tier(2)));assertTrue(notes(validate(o),2).contains("not approved for activation"));
        for(String field:List.of("quantity","scoring_policy","reward")){
            o=tree();if(field.equals("quantity"))requirement(o,1,0).addProperty(field,2048);
            else tier(o,1).addProperty(field,field.equals("reward")?"kingdom_smp:town_hall":"pending_review");
            assertFalse(MilestoneService.legacyCompatible(read(o).tier(1)));
        }
    }
}
