package com.mohymods.kingdomsmp.persistence;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;

/** Synchronous, bounded native-format checkpoints. No silent non-atomic fallback. */
public final class AtomicNbt {
    private AtomicNbt() { }
    public static CompoundTag read(Path file) throws IOException {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) > 16 * 1024 * 1024) {
            throw new IOException("Missing, linked or oversized checkpoint: " + file);
        }
        return NbtIo.readCompressed(file, NbtAccounter.create(64 * 1024 * 1024));
    }
    public static void write(Path file, CompoundTag data) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), file.getFileName() + ".", ".tmp");
        try {
            NbtIo.writeCompressed(data, temporary);
            try (var channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) { channel.force(true); }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            // Java cannot open directories on Windows. File force + atomic rename
            // still covers process interruption; host/storage-loss is outside the contract.
            if (!System.getProperty("os.name").startsWith("Windows")) {
                try (var channel = FileChannel.open(file.getParent(), StandardOpenOption.READ)) { channel.force(true); }
            }
        } finally { Files.deleteIfExists(temporary); }
    }
}
