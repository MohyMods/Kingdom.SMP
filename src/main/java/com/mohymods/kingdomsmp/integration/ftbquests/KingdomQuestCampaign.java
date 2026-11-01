package com.mohymods.kingdomsmp.integration.ftbquests;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/** Publishes only our missing chapter, never overwriting an existing campaign or player progress. */
public final class KingdomQuestCampaign {
    public static final String FILENAME = "kingdom_smp_settlement_v1.snbt";
    private static final String RESOURCE = "/data/kingdom_smp/ftbquests/" + FILENAME;
    public enum Result { CREATED, CURRENT, EXISTING_DIFFERENT }
    private KingdomQuestCampaign() { }

    public static Result install(Path questDirectory) throws IOException {
        try (InputStream input = KingdomQuestCampaign.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IOException("Missing bundled Kingdom quest chapter");
            byte[] content = input.readAllBytes();
            if (content.length < 128 || content.length > 128 * 1024) throw new IOException("Invalid bundled campaign size");
            return publish(questDirectory, content);
        }
    }

    static Result publish(Path questDirectory, byte[] content) throws IOException {
        Path chapters = questDirectory.resolve("chapters");
        Files.createDirectories(chapters);
        Path target = chapters.resolve(FILENAME);
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) return existing(target, content);
        // The temporary suffix is deliberately not .snbt, so FTB never reads a partial chapter.
        Path temporary = Files.createTempFile(chapters, ".kingdom-smp-chapter-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(content);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                // Same-directory hard-link creation is atomic and refuses an existing target.
                // ATOMIC_MOVE can replace targets on some providers even without REPLACE_EXISTING.
                Files.createLink(target, temporary);
            } catch (FileAlreadyExistsException raced) { return existing(target, content); }
            try (FileChannel directory = FileChannel.open(chapters, StandardOpenOption.READ)) { directory.force(true); }
            return Result.CREATED;
        } finally { Files.deleteIfExists(temporary); }
    }

    private static Result existing(Path target, byte[] content) throws IOException {
        if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS) || Files.size(target) > 128 * 1024)
            throw new IOException("Existing Kingdom chapter is not a bounded regular file");
        return Arrays.equals(content, Files.readAllBytes(target)) ? Result.CURRENT : Result.EXISTING_DIFFERENT;
    }
}
