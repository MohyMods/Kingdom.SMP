package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class MerchantQuestRulesTest {
 @TempDir Path directory;
 @Test void configurableNativeThresholdsRemainReadOnlyAndBounded()throws Exception{var p=directory.resolve("merchant.json");var defaults=MerchantQuestRules.load(p);assertEquals(50,defaults.get(MerchantTransactionQuests.DEPOSIT));assertEquals(3,defaults.get(MerchantTransactionQuests.SUPPLY));String bytes=Files.readString(p);Files.writeString(p,bytes.replace(": 50",": 75"));assertEquals(75,MerchantQuestRules.load(p).get(MerchantTransactionQuests.SELL));assertEquals(bytes.replace(": 50",": 75"),Files.readString(p));for(var wrong:new String[]{"0","-1","1.5","1000001","\"50\""}){Files.writeString(p,bytes.replace(": 50",": "+wrong));assertThrows(IOException.class,()->MerchantQuestRules.load(p));}Files.writeString(p,bytes.replace(": 3",": 5"));assertThrows(IOException.class,()->MerchantQuestRules.load(p));}
 @Test void earlierFiveObjectiveConfigurationKeepsBytesAndAddsOnlyDefaultExchange()throws Exception{var p=directory.resolve("older.json");String old="{\"schema\":1,\"goals\":{\"kingdom_smp:merchant/a02\":50,\"kingdom_smp:merchant/a03\":50,\"kingdom_smp:merchant/a04\":50,\"kingdom_smp:merchant/a05\":3,\"kingdom_smp:merchant/a06\":1}}";Files.writeString(p,old);assertEquals(1,MerchantQuestRules.load(p).get(MerchantTransactionQuests.EXCHANGE));assertEquals(old,Files.readString(p));}

}
