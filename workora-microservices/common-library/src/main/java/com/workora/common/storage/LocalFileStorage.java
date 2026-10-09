package com.workora.common.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

public final class LocalFileStorage {
    private final Path root;
    public LocalFileStorage(Path root) { this.root = root.toAbsolutePath().normalize(); }
    public String put(String key, InputStream input) throws IOException {
        String safeKey = StorageKeyValidator.requireSafe(key);
        Path target = root.resolve(safeKey).normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("Storage key escapes storage root");
        Files.createDirectories(target.getParent());
        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        return safeKey;
    }
    public Path resolve(String key) {
        String safeKey = StorageKeyValidator.requireSafe(key);
        Path target = root.resolve(safeKey).normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("Storage key escapes storage root");
        return target;
    }
}
