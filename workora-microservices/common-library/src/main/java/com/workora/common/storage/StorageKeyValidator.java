package com.workora.common.storage;

import java.nio.file.Path;

public final class StorageKeyValidator {
    private StorageKeyValidator() {}

    public static String requireSafe(String key) {
        if (key == null || key.isBlank() || key.length() > 1000) {
            throw new IllegalArgumentException("Storage key is required and must be at most 1000 characters");
        }
        String normalized = key.trim().replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains("..")
                || Path.of(normalized).isAbsolute() || normalized.startsWith("~")) {
            throw new IllegalArgumentException("Storage key must be a relative, traversal-safe object key");
        }
        if (!normalized.contains("/")) {
            throw new IllegalArgumentException("Storage key must include a namespace");
        }
        return normalized;
    }
}
