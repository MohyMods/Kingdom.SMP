package com.mohymods.kingdomsmp.integration.aircraft;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AircraftPacketRulesTest {
 @Test void throttleAcceptsNativeEndpointsAndRefusesPoisonAndOverdrive(){
  for(float v:new float[]{0,.25f,1})assertTrue(AircraftPacketRules.throttle(v));
  for(float v:new float[]{-.01f,1.01f,Float.NaN,Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY})assertFalse(AircraftPacketRules.throttle(v));
 }
 @Test void pilotRetainsNativeSensitivityClampingButNeverAcceptsNonfiniteInput(){
  assertTrue(AircraftPacketRules.pilot(-200,0,Float.MAX_VALUE));
  assertFalse(AircraftPacketRules.pilot(Float.NaN,0,0));assertFalse(AircraftPacketRules.pilot(0,Float.POSITIVE_INFINITY,0));
 }
 @Test void damageAndWeaponDirectionsNeverPropagateNonfiniteOrUnboundedValues(){
  assertTrue(AircraftPacketRules.collision(0));assertTrue(AircraftPacketRules.collision(100));
  for(float v:new float[]{-1,Float.NaN,Float.POSITIVE_INFINITY,Float.MAX_VALUE})assertFalse(AircraftPacketRules.collision(v));
  assertTrue(AircraftPacketRules.direction(0,0,0));assertTrue(AircraftPacketRules.direction(-.57735f,.57735f,.57735f));
  assertFalse(AircraftPacketRules.direction(1,1,1));assertFalse(AircraftPacketRules.direction(1000,0,0));
  assertFalse(AircraftPacketRules.direction(0,0,Float.NaN));assertFalse(AircraftPacketRules.direction(Float.MAX_VALUE,0,0));
 }
 @Test void exitRetainsOrdinaryMomentumAndSafelyBoundsEveryMalformedVector(){
  assertEquals(new Vec3(.25,-.1,1),AircraftPacketRules.dismount(.25,-.1,1));
  for(var v:new double[][]{{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY},{Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE},{300,-400,500},{0,0,0}}){
   var bounded=AircraftPacketRules.dismount(v[0],v[1],v[2]);
   assertTrue(Double.isFinite(bounded.x)&&Double.isFinite(bounded.y)&&Double.isFinite(bounded.z));
   assertTrue(bounded.length()<=AircraftPacketRules.MAX_DISMOUNT_SPEED+1e-12);
  }
  var d=AircraftPacketRules.dismount(30,40,0);assertEquals(6,d.x,1e-12);assertEquals(8,d.y,1e-12);
 }
}
