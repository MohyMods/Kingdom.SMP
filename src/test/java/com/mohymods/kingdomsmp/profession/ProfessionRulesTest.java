package com.mohymods.kingdomsmp.profession;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
class ProfessionRulesTest {
 @TempDir Path dir;
 @Test void generatedDefaultsPersistAndCanBeExplicitlyReconfigured()throws Exception{var file=dir.resolve("rules.json");var rules=ProfessionRules.load(file);assertEquals(500,rules.bonus(ProfessionRank.APPRENTICE));assertEquals(3000,rules.bonus(ProfessionRank.MASTER));Files.writeString(file,"{\"schema\":1,\"apprenticeAllowanceBonus\":100,\"journeymanAllowanceBonus\":200,\"masterAllowanceBonus\":300}");assertEquals(300,ProfessionRules.load(file).master());}
 @Test void malformedOrUnsafeBoundsRefuse()throws Exception{var file=dir.resolve("rules.json");for(var value:new String[]{"{}","{\"schema\":1.5}","{\"schema\":1,\"apprenticeAllowanceBonus\":-1,\"journeymanAllowanceBonus\":200,\"masterAllowanceBonus\":300}","{\"schema\":1,\"apprenticeAllowanceBonus\":100,\"journeymanAllowanceBonus\":99,\"masterAllowanceBonus\":300}"}){Files.writeString(file,value);assertThrows(IOException.class,()->ProfessionRules.load(file));}}
}
