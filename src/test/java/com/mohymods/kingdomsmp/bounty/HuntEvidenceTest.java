package com.mohymods.kingdomsmp.bounty;

import com.mohymods.kingdomsmp.economy.HuntState;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HuntEvidenceTest {
    final String type=HuntState.target(10),dimension="minecraft:overworld";
    HuntState hunt(UUID player) { return new HuntState(UUID.randomUUID(),player,10,type,2,160,false,false); }
    @Test void uniqueKillsSurviveRestartAndCannotCreditAnotherPlayer() {
        var data=new HuntEvidence();var mob=UUID.randomUUID();var first=hunt(UUID.randomUUID());var second=hunt(UUID.randomUUID());
        var proof=data.spawn(mob,type,dimension,10);assertTrue(data.record(first,mob,type,dimension,proof,10));
        var restored=HuntEvidence.load(data.save());assertEquals(1,restored.count(first.id()));
        assertFalse(restored.record(first,mob,type,dimension,proof,10));assertFalse(restored.record(second,mob,type,dimension,proof,10));
    }
    @Test void copiedUuidTypeDimensionDayAndOtherWorldSignaturesFail() {
        var data=new HuntEvidence();var mob=UUID.randomUUID();var hunt=hunt(UUID.randomUUID());var proof=data.spawn(mob,type,dimension,10);
        assertFalse(data.record(hunt,UUID.randomUUID(),type,dimension,proof,10));
        assertFalse(data.record(hunt,mob,"minecraft:zombie",dimension,proof,10));
        assertFalse(data.record(hunt,mob,type,"minecraft:the_nether",proof,10));
        assertFalse(new HuntEvidence().record(hunt,mob,type,dimension,proof,10));
        var changed=proof.copy();changed.putLong("day",9);assertFalse(data.record(hunt,mob,type,dimension,changed,10));
        changed=proof.copy();changed.putByteArray("signature",new byte[32]);assertFalse(data.record(hunt,mob,type,dimension,changed,10));assertEquals(0,data.count(hunt.id()));
    }
    @Test void noUnsignedFutureStaleExpiredOrClosedProofs() {
        var data=new HuntEvidence();var hunt=hunt(UUID.randomUUID());var mob=UUID.randomUUID();
        assertFalse(data.record(hunt,mob,type,dimension,new CompoundTag(),10));
        for(long day:new long[]{8,11})assertFalse(data.record(hunt,mob,type,dimension,data.spawn(mob,type,dimension,day),10));
        var proof=data.spawn(mob,type,dimension,9);assertFalse(data.record(hunt.close(true),mob,type,dimension,proof,10));
        assertFalse(data.record(hunt,mob,type,dimension,proof,11));assertTrue(data.record(hunt,mob,type,dimension,proof,10));
    }
    @Test void completedGoalCannotAccumulateAdditionalReusableVictims() {
        var data=new HuntEvidence();var hunt=hunt(UUID.randomUUID());
        for(int n=0;n<2;n++){var mob=UUID.randomUUID();assertTrue(data.record(hunt,mob,type,dimension,data.spawn(mob,type,dimension,10),10));}
        var next=UUID.randomUUID();assertFalse(data.record(hunt,next,type,dimension,data.spawn(next,type,dimension,10),10));assertEquals(2,data.count(hunt.id()));
    }
    @Test void corruptSchemaOrSecretMustNotRegenerateAuthority() {
        var data=new HuntEvidence();var bad=data.save();bad.putByteArray("secret",new byte[0]);assertThrows(IllegalArgumentException.class,()->HuntEvidence.load(bad));
        var wrongSchema=data.save();wrongSchema.putString("schema","1");assertThrows(IllegalArgumentException.class,()->HuntEvidence.load(wrongSchema));
    }
    @Test void longLivedKnownNaturalOriginRemainsValidWithoutChangingPaidHuntAgePolicy(){var data=new HuntEvidence();var mob=UUID.randomUUID();var proof=data.spawn(mob,type,dimension,2);assertTrue(data.naturalOrigin(mob,type,dimension,proof,10));assertFalse(data.authentic(mob,type,dimension,proof,10));assertFalse(data.record(hunt(UUID.randomUUID()),mob,type,dimension,proof,10));assertFalse(data.naturalOrigin(UUID.randomUUID(),type,dimension,proof,10));assertFalse(data.naturalOrigin(mob,type,dimension,proof,1));assertEquals(0,data.count(UUID.randomUUID()));}

}
