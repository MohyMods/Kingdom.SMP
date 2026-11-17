package com.mohymods.kingdomsmp.bounty;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class HuntRulesTest {
    @TempDir Path dir;
    @Test void existingNumericalConfigIsRetainedAndMustFundTenPlayers() throws Exception {
        var file=dir.resolve("hunts.json");
        String configured="{\"schema\":1,\"kills\":12,\"reward\":250,\"dailyPool\":3000}";
        Files.writeString(file,configured);assertEquals(new HuntRules(12,250,3000),HuntRules.load(file));assertEquals(configured,Files.readString(file));
        Files.writeString(file,configured.replace("3000","2000"));assertThrows(IOException.class,()->HuntRules.load(file));
    }
    @Test void fractionalOrExtraConfigurationCannotChangeMoneyOrSecurityRules() throws Exception {
        var file=dir.resolve("hunts.json");HuntRules.load(file);String defaults=Files.readString(file);
        Files.writeString(file,defaults.replace("\"kills\": 8","\"kills\": 8.5"));assertThrows(IOException.class,()->HuntRules.load(file));
        Files.writeString(file,defaults.replace("\"schema\": 1","\"schema\": 1, \"allowSpawners\": true"));assertThrows(IOException.class,()->HuntRules.load(file));
    }
}
