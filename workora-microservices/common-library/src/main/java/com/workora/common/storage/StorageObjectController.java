package com.workora.common.storage;

import com.workora.common.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/storage")
public class StorageObjectController {
    private final StorageObjectService storage;

    public StorageObjectController(StorageObjectService storage) {
        this.storage = storage;
    }

    @PostMapping("/uploads")
    public ResponseEntity<ApiResponse<StorageObjectService.PresignedFileUpload>> createUpload(
            @AuthenticationPrincipal Jwt ignored,
            @Valid @RequestBody UploadRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Upload URL created",
                storage.createUpload(request.namespace(), request.fileName(), request.contentType())));
    }

    @GetMapping("/downloads")
    public ResponseEntity<ApiResponse<Map<String, String>>> createDownload(
            @AuthenticationPrincipal Jwt ignored,
            @RequestParam String key) {
        return ResponseEntity.ok(ApiResponse.success("Download URL created",
                Map.of("key", StorageKeyValidator.requireSafe(key), "downloadUrl", storage.createDownload(key))));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Jwt ignored,
            @RequestParam String key) {
        storage.delete(key);
        return ResponseEntity.ok(ApiResponse.success("Object deleted", null));
    }

    public record UploadRequest(
            @NotBlank String namespace,
            @NotBlank String fileName,
            String contentType) {}
}
