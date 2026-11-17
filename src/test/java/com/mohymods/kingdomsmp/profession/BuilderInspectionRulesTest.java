package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class BuilderInspectionRulesTest {
    @TempDir Path directory;
    @Test void approvedRouteDefaultAndExplicitOverridesNeverOverwriteExistingConfiguration()throws Exception{
        var file=directory.resolve("inspection.json");assertEquals(48,BuilderInspectionRules.load(file));String custom="{\"schema\":1,\"gradedRouteCells\":64}";Files.writeString(file,custom);assertEquals(64,BuilderInspectionRules.load(file));assertEquals(custom,Files.readString(file));
    }
    @Test void invalidOrUnknownRequirementsFailClosed()throws Exception{
        var file=directory.resolve("inspection.json");for(String n:new String[]{"0","-1","513","1.5","\"48\""}){Files.writeString(file,"{\"schema\":1,\"gradedRouteCells\":"+n+"}");assertThrows(IOException.class,()->BuilderInspectionRules.load(file));}Files.writeString(file,"{\"schema\":1,\"lights\":16}");assertThrows(IOException.class,()->BuilderInspectionRules.load(file));
    }
}
