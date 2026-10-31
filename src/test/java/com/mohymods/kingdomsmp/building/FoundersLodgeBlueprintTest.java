package com.mohymods.kingdomsmp.building;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FoundersLodgeBlueprintTest {
    @Test void allRotationsHaveUniqueBoundedFurnishedSupportedDefinitions(){
        var b=FoundersLodgeBlueprint.create();
        for(var r:Rotation.values()){
            var blocks=b.blocks(r);Map<BlockPos,net.minecraft.world.level.block.state.BlockState> at=new HashMap<>();
            for(var p:blocks){assertNull(at.put(p.offset(),p.state()));assertTrue(p.offset().getX()>=0&&p.offset().getX()<b.width(r));assertTrue(p.offset().getZ()>=0&&p.offset().getZ()<b.depth(r));assertTrue(p.offset().getY()>=0&&p.offset().getY()<b.height());}
            for(int x=0;x<b.width(r);x++)for(int z=0;z<b.depth(r);z++)assertNotNull(at.get(new BlockPos(x,0,z)));
            assertNull(at.get(b.rotate(b.home(),r)));assertNull(at.get(b.rotate(b.home().above(),r)));
            assertTrue(at.get(b.rotate(b.board(),r)).is(Blocks.LECTERN));
            for(var required:List.of(Blocks.SPRUCE_DOOR,Blocks.BLUE_BED,Blocks.BARREL,Blocks.CARTOGRAPHY_TABLE,Blocks.LANTERN,Blocks.BOOKSHELF))assertTrue(blocks.stream().anyMatch(p->p.state().is(required)),required.toString());
        }
        assertThrows(UnsupportedOperationException.class,()->b.blocks().clear());
    }
    @Test void unsupportedBlueprintVersionsCannotSilentlyChangeSavedBuildings(){
        assertThrows(IllegalArgumentException.class,()->BlueprintRegistry.get(FoundersLodgeBlueprint.ID,2));
        assertThrows(IllegalArgumentException.class,()->BlueprintRegistry.get(net.minecraft.resources.ResourceLocation.parse("kingdom_smp:town_hall"),1));
    }
}
