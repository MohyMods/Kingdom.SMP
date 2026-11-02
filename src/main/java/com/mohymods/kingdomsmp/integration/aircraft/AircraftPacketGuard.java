package com.mohymods.kingdomsmp.integration.aircraft;

import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import org.joml.Vector3f;

/** Exact optional native C2S boundary. Native seat/pilot/weapon permissions still run afterward. */
public final class AircraftPacketGuard {
    private record Schema(String kind,Map<String,Field> fields){
        Object get(Object packet,String name)throws IllegalAccessException{return fields.get(name).get(packet);}
        float number(Object packet,String name)throws IllegalAccessException{return fields.get(name).getFloat(packet);}
    }
    private static Map<Class<?>,Schema> schemas=Map.of();
    private static final Map<ServerPlayer,Long> rejected=new WeakHashMap<>();
    private AircraftPacketGuard() { }
    public static void bind()throws ReflectiveOperationException{
        var definitions=Map.of("PilotInputMessage",List.of("x","y","z"),"EnginePowerMessage",List.of("engineTarget"),"SeatChangeMessage",List.<String>of(),"CollisionMessage",List.of("damage"),"FireMessage",List.of("direction"),"CommandMessage",List.of("key"));
        var bound=new HashMap<Class<?>,Schema>();
        for(var entry:definitions.entrySet()){
            Class<?> type=Class.forName("immersive_aircraft.network.c2s."+entry.getKey());
            type.getMethod("receiveServer",ServerPlayer.class);var fields=new HashMap<String,Field>();
            for(String name:entry.getValue()){Field f=type.getDeclaredField(name);if(!f.trySetAccessible())throw new IllegalStateException("Native aircraft packet field unavailable: "+name);fields.put(name,f);}
            bound.put(type,new Schema(entry.getKey(),Map.copyOf(fields)));
        }
        schemas=Map.copyOf(bound);
    }
    public static String status(){return "Aircraft control payload boundary: "+schemas.size()+" verified native types bound; native permissions retained";}
    public static long rejected(ServerPlayer p){return rejected.getOrDefault(p,0L);}
    public static boolean permit(ServerPlayer p,Object packet){
        if(!p.server.isSameThread())return false;
        Schema schema=schemas.get(packet.getClass());if(schema==null)return deny(p);
        if(p.hasDisconnected()||p.server.getPlayerList().getPlayer(p.getUUID())!=p)return deny(p);
        var vehicle=schema.kind().equals("CommandMessage")||schema.kind().equals("CollisionMessage")?p.getRootVehicle():p.getVehicle();
        if(!AircraftAccess.aircraft(vehicle))return deny(p);
        try{
            // Dismount must remain possible even after eligibility changes. Its velocity
            // is bounded by the dedicated primitive-field mixin, without changing exit rights.
            if(schema.kind().equals("CommandMessage")&&((Enum<?>)schema.get(packet,"key")).name().equals("DISMOUNT"))return true;
            if(!p.isAlive()||p.isSpectator()||!AircraftAccess.denial(p.server).isEmpty())return deny(p);
            boolean valid=switch(schema.kind()){
                case "PilotInputMessage"->AircraftPacketRules.pilot(schema.number(packet,"x"),schema.number(packet,"y"),schema.number(packet,"z"));
                case "EnginePowerMessage"->AircraftPacketRules.throttle(schema.number(packet,"engineTarget"));
                case "CollisionMessage"->AircraftPacketRules.collision(schema.number(packet,"damage"));
                case "FireMessage"->{var direction=(Vector3f)schema.get(packet,"direction");yield direction!=null&&AircraftPacketRules.direction(direction.x,direction.y,direction.z);}
                default->true;
            };
            return valid||deny(p);
        }catch(ReflectiveOperationException|RuntimeException failure){com.mojang.logging.LogUtils.getLogger().error("Aircraft control refused: verified native payload could not be inspected",failure);return deny(p);}
    }
    private static boolean deny(ServerPlayer p){rejected.merge(p,1L,(a,b)->a==Long.MAX_VALUE?a:a+b);return false;}
}
