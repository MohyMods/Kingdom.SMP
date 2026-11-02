package com.mohymods.kingdomsmp.integration.ftbquests;

import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class KingdomQuestCampaignTest {
    @TempDir Path directory;
    @Test void installedGuideKeepsStableRewardlessIdsAndRealCompoundItemIcons()throws Exception{
        assertEquals(KingdomQuestCampaign.Result.CREATED,KingdomQuestCampaign.install(directory));
        var tag=TagParser.parseTag(Files.readString(directory.resolve("chapters").resolve(KingdomQuestCampaign.FILENAME)));
        assertEquals(String.format("%016X",KingdomQuestGoal.CHAPTER_ID),tag.getString("id"));
        assertEquals("kingdom_smp:gold_coin",tag.getCompound("icon").getString("id"));
        var quests=tag.getList("quests",Tag.TAG_COMPOUND);assertEquals(9,quests.size());Set<String> ids=new HashSet<>();
        for(var goal:KingdomQuestGoal.values()){
            var quest=quests.stream().map(t->(CompoundTag)t).filter(q->q.getString("id").equals(String.format("%016X",goal.questId()))).findFirst().orElseThrow();
            assertTrue(ids.add(quest.getString("id")));assertEquals(0,quest.getList("rewards",Tag.TAG_COMPOUND).size());
            assertEquals(goal.iconItemId(),quest.getCompound("icon").getString("id"));
            var tasks=quest.getList("tasks",Tag.TAG_COMPOUND);assertEquals(1,tasks.size());var task=tasks.getCompound(0);
            assertEquals(goal.code(),task.getString("id"));assertEquals(goal.iconItemId(),task.getCompound("icon").getString("id"));assertEquals(goal.tag(),task.getList("tags",Tag.TAG_STRING).getString(0));
        }
        assertEquals(KingdomQuestCampaign.Result.CURRENT,KingdomQuestCampaign.install(directory));
    }
    @Test void olderOrCustomizedChaptersArePreservedByteForByte()throws Exception{
        var chapters=directory.resolve("chapters");Files.createDirectories(chapters);var file=chapters.resolve(KingdomQuestCampaign.FILENAME);
        String custom="{ id: \"4B534D5000000001\", title: \"Owner layout\", icon: \"minecraft:book\", quests: [] }\n";Files.writeString(file,custom);
        assertEquals(KingdomQuestCampaign.Result.EXISTING_DIFFERENT,KingdomQuestCampaign.install(directory));assertEquals(custom,Files.readString(file));
    }
    @Test void linkedChapterCannotRedirectAnInstallToAnotherFile()throws Exception{
        var chapters=directory.resolve("chapters");Files.createDirectories(chapters);var unrelated=directory.resolve("owner.snbt");Files.writeString(unrelated,"Owner content");
        Files.createSymbolicLink(chapters.resolve(KingdomQuestCampaign.FILENAME),unrelated);
        assertThrows(java.io.IOException.class,()->KingdomQuestCampaign.install(directory));assertEquals("Owner content",Files.readString(unrelated));
    }
}
