package com.workora.recruitment.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID postId;

    @Column(nullable = false)
    private UUID applicantId;

    @Column(nullable = false)
    private String applicantEmail;

    @Column(length = 4000)
    private String coverNote;

    /** Simple "key=value" strings supplied by the applicant form. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "application_structured_data",
            joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "entry", length = 1000)
    @OrderColumn(name = "entry_index")
    private List<String> structuredData = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    @Column(length = 2000)
    private String managerNote;

    @Column(length = 2000)
    private String ownerNote;

    @Column(nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (this.submittedAt == null) {
            this.submittedAt = now;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPostId() {
        return postId;
    }

    public void setPostId(UUID postId) {
        this.postId = postId;
    }

    public UUID getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(UUID applicantId) {
        this.applicantId = applicantId;
    }

    public String getApplicantEmail() {
        return applicantEmail;
    }

    public void setApplicantEmail(String applicantEmail) {
        this.applicantEmail = applicantEmail;
    }

    public String getCoverNote() {
        return coverNote;
    }

    public void setCoverNote(String coverNote) {
        this.coverNote = coverNote;
    }

    public List<String> getStructuredData() {
        return structuredData;
    }

    public void setStructuredData(List<String> structuredData) {
        this.structuredData = structuredData == null ? new ArrayList<>() : structuredData;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public String getManagerNote() {
        return managerNote;
    }

    public void setManagerNote(String managerNote) {
        this.managerNote = managerNote;
    }

    public String getOwnerNote() {
        return ownerNote;
    }

    public void setOwnerNote(String ownerNote) {
        this.ownerNote = ownerNote;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
