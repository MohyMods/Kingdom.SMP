package com.mohymods.kingdomsmp.building;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Small one-way markers guard against reissuance when native world/entity saves lag. */
final class HutReservationJournal {
    private HutReservationJournal() { }
    static void create(Path file, String value) {
        try {
            Files.createDirectories(file.getParent());
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.DSYNC)) {
                ByteBuffer bytes = StandardCharsets.UTF_8.encode(value);
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot commit hut reservation/issuance; world mutation stopped: " + file, failure);
        }
    }

    static void verify(Path file, String expected) {
        try {
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) > 512
                    || !Files.readString(file).equals(expected)) throw new IOException("Mismatched hut journal");
        } catch (IOException failure) {
            throw new IllegalStateException("Hut reservation journal mismatch; restore matching world data: " + file, failure);
        }
    }
}
