package com.workora.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.UUID;

@Service
public class StorageObjectService {
    private static final Set<String> NAMESPACES = Set.of(
            "projects", "tasks", "submissions", "bug-reports", "recruitment", "applications",
            "companies", "phases", "avatars");

    private final MinioClient minioClient;
    private final MinioClient presignClient;
    private final String bucket;
    private final Duration expiry;

    public StorageObjectService(
            MinioClient minioClient,
            MinioClient minioPresignClient,
            @Value("${storage.minio.bucket:workora-files}") String bucket,
            @Value("${storage.minio.presign-minutes:15}") long expiryMinutes) {
        this.minioClient = minioClient;
        this.presignClient = minioPresignClient;
        this.bucket = bucket;
        this.expiry = Duration.ofMinutes(Math.max(1, Math.min(expiryMinutes, 60)));
    }

    public PresignedFileUpload createUpload(String namespace, String fileName, String contentType) {
        validateNamespace(namespace);
        String extension = extension(fileName);
        String key = namespace.trim() + "/" + UUID.randomUUID() + extension;
        try {
            String url = presignClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(bucket)
                    .object(key)
                    .expiry((int) expiry.toMinutes(), TimeUnit.MINUTES)
                    .build());
            return new PresignedFileUpload(key, url, Instant.now().plus(expiry),
                    contentType == null || contentType.isBlank()
                            ? "application/octet-stream" : contentType);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create MinIO upload URL", exception);
        }
    }

    public String createDownload(String key) {
        String safeKey = validateKey(key);
        try {
            return presignClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(safeKey)
                    .expiry((int) expiry.toMinutes(), TimeUnit.MINUTES)
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create MinIO download URL", exception);
        }
    }

    public void delete(String key) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket).object(validateKey(key)).build());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not delete MinIO object", exception);
        }
    }

    private String validateKey(String key) {
        String safeKey = StorageKeyValidator.requireSafe(key);
        validateNamespace(safeKey.substring(0, safeKey.indexOf('/')));
        return safeKey;
    }

    private void validateNamespace(String namespace) {
        if (namespace == null || !NAMESPACES.contains(namespace.trim())) {
            throw new IllegalArgumentException("Unsupported storage namespace");
        }
    }

    private String extension(String fileName) {
        if (fileName == null || fileName.isBlank()) return "";
        String clean = fileName.replace('\\', '/');
        int slash = clean.lastIndexOf('/');
        int dot = clean.lastIndexOf('.');
        return dot > slash && dot < clean.length() - 1
                ? clean.substring(dot).replaceAll("[^A-Za-z0-9.]", "") : "";
    }

    public record PresignedFileUpload(String key, String uploadUrl, Instant expiresAt,
                                      String contentType) {}
}
