package com.workora.work.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_status_history", indexes = {
        @Index(name = "idx_status_history_task", columnList = "taskId")
})
public class TaskStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID taskId;

    @Enumerated(EnumType.STRING)
    private TaskStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus toStatus;

    @Column(nullable = false)
    private UUID actorId;

    @Column(length = 1000)
    private String reason;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    protected TaskStatusHistory() {
    }

    public TaskStatusHistory(UUID taskId, TaskStatus fromStatus, TaskStatus toStatus, UUID actorId, String reason) {
        this.taskId = taskId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actorId = actorId;
        this.reason = reason;
    }

    @PrePersist
    void onCreate() {
        this.changedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public TaskStatus getFromStatus() {
        return fromStatus;
    }

    public TaskStatus getToStatus() {
        return toStatus;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getReason() {
        return reason;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
