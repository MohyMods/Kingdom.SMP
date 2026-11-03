package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class RetainedBuildingRulesTest {
    @TempDir Path directory;
    @org.junit.jupiter.api.BeforeAll static void bootstrap(){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();}
    @Test void approvedGoalsAndExplicitOverridesArePreserved()throws Exception{
        var file=directory.resolve("goals.json");var rules=RetainedBuildingRules.load(file);assertEquals(128,rules.goal("kingdom_smp:builder/a01"));assertEquals(64,rules.goal("kingdom_smp:builder/a02"));assertEquals(64,rules.goal("kingdom_smp:builder/a03"));assertEquals(48,rules.goal("kingdom_smp:builder/a06"));
        String changed=Files.readString(file).replace(": 128",": 96");Files.writeString(file,changed);assertEquals(96,RetainedBuildingRules.load(file).goal("kingdom_smp:builder/a01"));assertEquals(changed,Files.readString(file));
    }
    @Test void malformedOrUnknownGoalsDoNotSilentlyReset()throws Exception{
        var file=directory.resolve("goals.json");RetainedBuildingRules.load(file);String original=Files.readString(file);
        for(String n:new String[]{"0","-1","2049","1.5","\"128\""}){Files.writeString(file,original.replace(": 128",": "+n));assertThrows(IOException.class,()->RetainedBuildingRules.load(file));}
        Files.writeString(file,original.replace("builder/a01","builder/m01"));assertThrows(IOException.class,()->RetainedBuildingRules.load(file));
    }
    @Test void oldFourGoalConfigurationRemainsUnmodifiedWhileLaterDefaultsAreAdditive()throws Exception{
        var file=directory.resolve("old.json");String legacy="{\"schema\":1,\"goals\":{\"kingdom_smp:builder/a01\":120,\"kingdom_smp:builder/a02\":64,\"kingdom_smp:builder/a03\":64,\"kingdom_smp:builder/a06\":48}}";Files.writeString(file,legacy);var rules=RetainedBuildingRules.load(file);assertEquals(120,rules.goal("kingdom_smp:builder/a01"));assertEquals(80,rules.goal("kingdom_smp:builder/j02"));assertEquals(384,rules.goal("kingdom_smp:builder/m01"));assertEquals(96,rules.goal("kingdom_smp:builder/m02"));assertEquals(legacy,Files.readString(file));
    }
    @Test void categoryProgressRequiresGlassAndAllThreeDecorativeKinds(){
        String facade="kingdom_smp:builder/j02",decor="kingdom_smp:builder/m02";
        assertEquals(64,RetainedBuildingService.progress(facade,80,1000,0,0,0,0,1000));assertEquals(16,RetainedBuildingService.progress(facade,80,0,1000,0,0,0,1000));assertEquals(80,RetainedBuildingService.progress(facade,80,64,16,0,0,0,80));assertEquals(79,RetainedBuildingService.progress(facade,80,63,1000,0,0,0,1000));
        assertEquals(95,RetainedBuildingService.progress(decor,96,0,0,100,100,0,200));assertEquals(96,RetainedBuildingService.progress(decor,96,0,0,32,32,32,96));
        assertFalse(ProfessionQuestService.catalog().definition("kingdom_smp:builder/j08").isPresent());
    }

    @Test void neighbourGeometryIsAllowedWithoutAcceptingReplacementRotationOrWaterChanges(){
        var wall=net.minecraft.world.level.block.Blocks.STONE_BRICK_WALL.defaultBlockState();var changed=wall.setValue(net.minecraft.world.level.block.WallBlock.EAST_WALL,net.minecraft.world.level.block.state.properties.WallSide.TALL);
        assertTrue(RetainedBuildingService.sameProof(changed,wall.toString()));assertFalse(RetainedBuildingService.sameProof(net.minecraft.world.level.block.Blocks.COBBLESTONE_WALL.defaultBlockState(),wall.toString()));assertFalse(RetainedBuildingService.sameProof(wall.setValue(net.minecraft.world.level.block.WallBlock.WATERLOGGED,true),wall.toString()));
        var stair=net.minecraft.world.level.block.Blocks.STONE_BRICK_STAIRS.defaultBlockState();assertTrue(RetainedBuildingService.sameProof(stair.setValue(net.minecraft.world.level.block.StairBlock.SHAPE,net.minecraft.world.level.block.state.properties.StairsShape.INNER_LEFT),stair.toString()));assertFalse(RetainedBuildingService.sameProof(stair.setValue(net.minecraft.world.level.block.StairBlock.FACING,net.minecraft.core.Direction.EAST),stair.toString()));
    }

}
