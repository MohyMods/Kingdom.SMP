package com.mohymods.kingdomsmp.integration.aircraft;

import net.minecraft.world.phys.Vec3;

/** Bounds for the verified native wire format, not new aircraft statistics or unlock tiers. */
public final class AircraftPacketRules {
    public static final double MAX_DISMOUNT_SPEED=10; // blocks/tick; ordinary snapshot aircraft are far slower
    public static final float MAX_COLLISION_DAMAGE=1_000_000,MAX_DIRECTION_LENGTH=1.01f;
    private AircraftPacketRules() { }
    public static boolean throttle(float value){return Float.isFinite(value)&&value>=0&&value<=1;}
    /** Native receivePilotInput already performs its own [-1,1] clamping and pilot check. */
    public static boolean pilot(float x,float y,float z){return Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z);}
    public static boolean collision(float damage){return Float.isFinite(damage)&&damage>=0&&damage<=MAX_COLLISION_DAMAGE;}
    public static boolean direction(float x,float y,float z){return pilot(x,y,z)&&Math.hypot(Math.hypot(x,y),z)<=MAX_DIRECTION_LENGTH;}
    /** Always allow a legitimate exit; never install untrusted NaN/infinite/extreme velocity. */
    public static Vec3 dismount(double x,double y,double z){
        x=Double.isFinite(x)?x:0;y=Double.isFinite(y)?y:0;z=Double.isFinite(z)?z:0;
        double length=Math.hypot(Math.hypot(x,y),z);
        if(length>MAX_DISMOUNT_SPEED){double factor=MAX_DISMOUNT_SPEED/length;x*=factor;y*=factor;z*=factor;}
        return new Vec3(x,y,z);
    }
}
