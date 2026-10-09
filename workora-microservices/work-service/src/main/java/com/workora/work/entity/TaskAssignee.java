package com.workora.work.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_assignees", indexes = {
        @Index(name = "idx_assignee_task", columnList = "taskId"),
        @Index(name = "idx_assignee_user", columnList = "userId")
})
public class TaskAssignee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID taskId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID departmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssigneeStatus status = AssigneeStatus.ASSIGNED;

    @Column(nullable = false, updatable = false)
    private UUID assignedBy;

    @Column(nullable = false, updatable = false)
    private Instant assignedAt;

    private Instant removedAt;

    protected TaskAssignee() {
    }

    public TaskAssignee(UUID taskId, UUID userId, UUID departmentId, UUID assignedBy) {
        this.taskId = taskId;
        this.userId = userId;
        this.departmentId = departmentId;
        this.assignedBy = assignedBy;
    }

    @PrePersist
    void onCreate() {
        this.assignedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public AssigneeStatus getStatus() {
        return status;
    }

    public void setStatus(AssigneeStatus status) {
        this.status = status;
    }

    public UUID getAssignedBy() {
        return assignedBy;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public Instant getRemovedAt() {
        return removedAt;
    }

    public void setRemovedAt(Instant removedAt) {
        this.removedAt = removedAt;
    }
}
