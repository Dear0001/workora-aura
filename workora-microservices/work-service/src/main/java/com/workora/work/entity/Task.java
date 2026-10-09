package com.workora.work.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_task_phase", columnList = "phaseId"),
        @Index(name = "idx_task_project", columnList = "projectId"),
        @Index(name = "idx_task_company", columnList = "companyId")
})
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID phaseId;

    @Column(nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private UUID departmentId;

    @Column(nullable = false)
    private UUID companyId;

    // Denormalized from the project at creation so per-request authorization
    // does not need a cross-service call to the Project service.
    private UUID projectManagerId;

    @Column(nullable = false)
    private String title;

    @Column(length = 4000)
    private String description;

    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompletionRule completionRule = CompletionRule.ALL_ASSIGNEES;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Task() {
    }

    public Task(UUID phaseId,
                UUID projectId,
                UUID departmentId,
                UUID companyId,
                UUID projectManagerId,
                String title,
                UUID createdBy) {
        this.phaseId = phaseId;
        this.projectId = projectId;
        this.departmentId = departmentId;
        this.companyId = companyId;
        this.projectManagerId = projectManagerId;
        this.title = title;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getPhaseId() {
        return phaseId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public UUID getProjectManagerId() {
        return projectManagerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public CompletionRule getCompletionRule() {
        return completionRule;
    }

    public void setCompletionRule(CompletionRule completionRule) {
        this.completionRule = completionRule;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
