package com.workora.work.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_attachments", indexes = {
        @Index(name = "idx_task_attachment_task", columnList = "taskId")
})
public class TaskAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID taskId;

    @Column(nullable = false, length = 1000)
    private String fileKey;

    @Column(length = 255)
    private String fileName;

    @Column(length = 255)
    private String mimeType;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private UUID uploadedBy;

    @Column(nullable = false, updatable = false)
    private Instant uploadedAt;

    protected TaskAttachment() {
    }

    public TaskAttachment(UUID taskId, String fileKey, String fileName, String mimeType, String description, UUID uploadedBy) {
        this.taskId = taskId;
        this.fileKey = fileKey;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.description = description;
        this.uploadedBy = uploadedBy;
    }

    @PrePersist
    void onCreate() {
        this.uploadedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public String getFileKey() {
        return fileKey;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getDescription() {
        return description;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
