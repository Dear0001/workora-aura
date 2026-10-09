package com.workora.work.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Immutable audit record of work handed in for a task. Submissions are never
 * mutated after creation; new work produces a new versioned submission.
 */
@Entity
@Table(name = "submissions", indexes = {
        @Index(name = "idx_submission_task", columnList = "taskId")
})
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID taskId;

    @Column(nullable = false, updatable = false)
    private UUID authorId;

    @Column(nullable = false, updatable = false)
    private int version;

    @Column(length = 4000, updatable = false)
    private String note;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "submission_attachments", joinColumns = @JoinColumn(name = "submissionId"))
    @Column(name = "attachmentKey", length = 1000)
    private List<String> attachmentKeys = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant submittedAt;

    protected Submission() {
    }

    public Submission(UUID taskId, UUID authorId, int version, String note, List<String> attachmentKeys) {
        this.taskId = taskId;
        this.authorId = authorId;
        this.version = version;
        this.note = note;
        if (attachmentKeys != null) {
            this.attachmentKeys = new ArrayList<>(attachmentKeys);
        }
    }

    @PrePersist
    void onCreate() {
        this.submittedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public int getVersion() {
        return version;
    }

    public String getNote() {
        return note;
    }

    public List<String> getAttachmentKeys() {
        return attachmentKeys;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
