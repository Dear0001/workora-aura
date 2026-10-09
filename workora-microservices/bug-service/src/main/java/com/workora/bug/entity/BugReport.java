package com.workora.bug.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bug_reports", indexes = {
        @Index(name = "idx_bug_report_phase", columnList = "phaseId"),
        @Index(name = "idx_bug_report_task", columnList = "taskId"),
        @Index(name = "idx_bug_report_reporter", columnList = "reporterId"),
        @Index(name = "idx_bug_report_company", columnList = "companyId")
})
public class BugReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID phaseId;

    private UUID taskId;

    @Column(nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private UUID departmentId;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private UUID reporterId;

    /** Denormalized so reviewer checks never need a Project-service round trip. */
    private UUID projectManagerId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(length = 4000)
    private String problem;

    @Column(length = 500)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BugCategory category = BugCategory.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BugSeverity severity = BugSeverity.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BugReportStatus status = BugReportStatus.SUBMITTED;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public BugReport() {
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

    public void setPhaseId(UUID phaseId) {
        this.phaseId = phaseId;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(UUID departmentId) {
        this.departmentId = departmentId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public UUID getReporterId() {
        return reporterId;
    }

    public void setReporterId(UUID reporterId) {
        this.reporterId = reporterId;
    }

    public UUID getProjectManagerId() {
        return projectManagerId;
    }

    public void setProjectManagerId(UUID projectManagerId) {
        this.projectManagerId = projectManagerId;
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

    public String getProblem() {
        return problem;
    }

    public void setProblem(String problem) {
        this.problem = problem;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public BugCategory getCategory() {
        return category;
    }

    public void setCategory(BugCategory category) {
        this.category = category;
    }

    public BugSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(BugSeverity severity) {
        this.severity = severity;
    }

    public BugReportStatus getStatus() {
        return status;
    }

    public void setStatus(BugReportStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
