package com.mohymods.kingdomsmp.profession;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class ProfessionEvidenceSavedDataTest {
    @Test void independentProfessionCountersAndBandsSurviveExactSavedDataRoundTrip(){
        var data=new ProfessionEvidenceSavedData();var player=UUID.randomUUID();
        data.evidence(Profession.BUILDER).record(player,ProfessionRank.APPRENTICE,"stone_stairs",32,null);
        var saved=data.save(new CompoundTag(),null);var restored=ProfessionEvidenceSavedData.load(saved,null);
        assertEquals(saved,restored.save(new CompoundTag(),null));assertEquals(32,restored.evidence(Profession.BUILDER).count(player,ProfessionRank.APPRENTICE,"stone_stairs"));
        assertEquals(0,restored.evidence(Profession.BUILDER).count(player,ProfessionRank.JOURNEYMAN,"stone_stairs"));
        assertEquals(0,restored.evidence(Profession.BUILDER).count(UUID.randomUUID(),ProfessionRank.APPRENTICE,"stone_stairs"));
    }
    @Test void unrecognizedProducerWrongSchemaAndForeignProfessionScopeAreRefused(){
        var data=new ProfessionEvidenceSavedData();data.evidence(Profession.BUILDER);var saved=data.save(new CompoundTag(),null);
        var wrongSchema=saved.copy();wrongSchema.putInt("schema",2);assertThrows(IllegalArgumentException.class,()->ProfessionEvidenceSavedData.load(wrongSchema,null));
        var wrong=saved.copy();wrong.getCompound("professions").getCompound("builder").putString("profession","farmer");var foreign=wrong;
        assertThrows(IllegalArgumentException.class,()->ProfessionEvidenceSavedData.load(foreign,null));
        assertThrows(IllegalArgumentException.class,()->data.evidence(Profession.FARMER));
    }
}
