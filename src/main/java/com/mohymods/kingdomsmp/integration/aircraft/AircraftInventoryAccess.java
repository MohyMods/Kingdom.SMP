package com.mohymods.kingdomsmp.integration.aircraft;

import com.mohymods.kingdomsmp.placement.PlacementProtection;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Exact optional native menus and public appearance synchronization; never changes cargo. */
public final class AircraftInventoryAccess {
    private static Method getVehicle,getDescription,getSlots,slotIndex,slotType;
    private AircraftInventoryAccess() { }
    public static void bind()throws ReflectiveOperationException{
        getVehicle=Class.forName("immersive_aircraft.screen.VehicleScreenHandler").getMethod("getVehicle");
        getDescription=Class.forName("immersive_aircraft.entity.InventoryVehicleEntity").getMethod("getInventoryDescription");
        getSlots=Class.forName("immersive_aircraft.entity.inventory.VehicleInventoryDescription").getMethod("getSlots");
        var slot=Class.forName("immersive_aircraft.entity.inventory.slots.SlotDescription");slotIndex=slot.getMethod("index");slotType=slot.getMethod("type");
    }
    public static boolean current(ServerPlayer p){return p.server.isSameThread()&&!p.hasDisconnected()&&p.server.getPlayerList().getPlayer(p.getUUID())==p;}
    public static boolean menu(Player actor,Object menu){
        if(!(actor instanceof ServerPlayer p))return actor.level().isClientSide;
        try{
            return entity(p,(Entity)getVehicle.invoke(menu));
        }catch(ReflectiveOperationException|RuntimeException failure){return false;}
    }
    public static boolean entity(ServerPlayer p,Entity entity){
        return current(p)&&p.isAlive()&&!p.isSpectator()&&AircraftAccess.aircraft(entity)&&entity.distanceTo(p)<8
                &&AircraftAccess.denial(p.server).isEmpty()&&AircraftSales.denial(p,entity).isEmpty()&&PlacementProtection.entityInteraction(p,entity)==null;
    }
    /** Nearby clients may render equipment even while flight is locked; cargo needs the real menu. */
    public static boolean publicSlot(ServerPlayer p,int entityId,int index){
        if(!current(p)||index<0)return false;
        Entity entity=p.serverLevel().getEntity(entityId);
        if(!AircraftAccess.aircraft(entity)||!entity.isAlive())return false;
        try{
            var slots=(List<?>)getSlots.invoke(getDescription.invoke(entity));
            if(index>=slots.size())return false;
            var description=slots.get(index);
            return (int)slotIndex.invoke(description)==index&&!"inventory".equals(slotType.invoke(description));
        }catch(ReflectiveOperationException|RuntimeException failure){return false;}
    }
}
