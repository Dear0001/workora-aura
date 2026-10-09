package com.workora.project.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bug_fix_jobs", indexes = {
        @Index(name = "idx_bfj_phase", columnList = "phaseId"),
        @Index(name = "idx_bfj_company", columnList = "companyId"),
        @Index(name = "idx_bfj_status", columnList = "status")
})
public class BugFixJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID phaseId;

    private UUID taskId;

    private UUID projectId;

    private UUID departmentId;

    private UUID companyId;

    private UUID projectManagerId;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String summary;

    @Column(length = 500)
    private String productType;

    @Column(length = 500)
    private String techStack;

    @Column(length = 500)
    private String versionBuild;

    @Column(length = 500)
    private String environments;

    @Column(length = 4000)
    private String scope;

    @Column(length = 2000)
    private String outOfScope;

    @Column(length = 4000)
    private String authorizedTestingRules;

    /** PRIVATE: may reference sandbox/credential policy. Never exposed in the public projection. */
    @Column(length = 2000)
    private String accessMethod;

    @Enumerated(EnumType.STRING)
    private JobCategory category;

    @Column(length = 1000)
    private String severityCriteria;

    @Column(length = 2000)
    private String reproductionRequirements;

    @Column(length = 2000)
    private String evidenceRequirements;

    @Enumerated(EnumType.STRING)
    private FixExpectation fixExpectation;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "bug_fix_job_skills", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "skill", length = 200)
    private List<String> requiredSkills = new ArrayList<>();

    @Column(length = 1000)
    private String eligibilityNotes;

    @Column(nullable = false)
    private boolean requiresNda = false;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal fee;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private boolean splitFeeAllowed = false;

    @Column(length = 500)
    private String duplicateReportRule;

    private Integer maxAcceptedReports;

    @Column(nullable = false)
    private boolean closesAfterOneResolution = true;

    private Instant deadline;

    @Column(length = 2000)
    private String safeHarborStatement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BugFixJobStatus status = BugFixJobStatus.DRAFT;

    private UUID selectedFreelancerId;

    @Column(nullable = false)
    private int jobVersion = 1;

    private UUID paymentId;

    private String paymentReference;

    @Column(precision = 15, scale = 2)
    private BigDecimal verifiedAmount;

    @Column(length = 3)
    private String verifiedCurrency;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant publishedAt;

    private Instant closedAt;

    protected BugFixJob() {
    }

    public BugFixJob(UUID phaseId, UUID projectId, UUID departmentId, UUID companyId,
                     UUID projectManagerId, String title) {
        this.phaseId = phaseId;
        this.projectId = projectId;
        this.departmentId = departmentId;
        this.companyId = companyId;
        this.projectManagerId = projectManagerId;
        this.title = title;
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

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getTechStack() {
        return techStack;
    }

    public void setTechStack(String techStack) {
        this.techStack = techStack;
    }

    public String getVersionBuild() {
        return versionBuild;
    }

    public void setVersionBuild(String versionBuild) {
        this.versionBuild = versionBuild;
    }

    public String getEnvironments() {
        return environments;
    }

    public void setEnvironments(String environments) {
        this.environments = environments;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getOutOfScope() {
        return outOfScope;
    }

    public void setOutOfScope(String outOfScope) {
        this.outOfScope = outOfScope;
    }

    public String getAuthorizedTestingRules() {
        return authorizedTestingRules;
    }

    public void setAuthorizedTestingRules(String authorizedTestingRules) {
        this.authorizedTestingRules = authorizedTestingRules;
    }

    public String getAccessMethod() {
        return accessMethod;
    }

    public void setAccessMethod(String accessMethod) {
        this.accessMethod = accessMethod;
    }

    public JobCategory getCategory() {
        return category;
    }

    public void setCategory(JobCategory category) {
        this.category = category;
    }

    public String getSeverityCriteria() {
        return severityCriteria;
    }

    public void setSeverityCriteria(String severityCriteria) {
        this.severityCriteria = severityCriteria;
    }

    public String getReproductionRequirements() {
        return reproductionRequirements;
    }

    public void setReproductionRequirements(String reproductionRequirements) {
        this.reproductionRequirements = reproductionRequirements;
    }

    public String getEvidenceRequirements() {
        return evidenceRequirements;
    }

    public void setEvidenceRequirements(String evidenceRequirements) {
        this.evidenceRequirements = evidenceRequirements;
    }

    public FixExpectation getFixExpectation() {
        return fixExpectation;
    }

    public void setFixExpectation(FixExpectation fixExpectation) {
        this.fixExpectation = fixExpectation;
    }

    public List<String> getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(List<String> requiredSkills) {
        this.requiredSkills = requiredSkills != null ? requiredSkills : new ArrayList<>();
    }

    public String getEligibilityNotes() {
        return eligibilityNotes;
    }

    public void setEligibilityNotes(String eligibilityNotes) {
        this.eligibilityNotes = eligibilityNotes;
    }

    public boolean isRequiresNda() {
        return requiresNda;
    }

    public void setRequiresNda(boolean requiresNda) {
        this.requiresNda = requiresNda;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isSplitFeeAllowed() {
        return splitFeeAllowed;
    }

    public void setSplitFeeAllowed(boolean splitFeeAllowed) {
        this.splitFeeAllowed = splitFeeAllowed;
    }

    public String getDuplicateReportRule() {
        return duplicateReportRule;
    }

    public void setDuplicateReportRule(String duplicateReportRule) {
        this.duplicateReportRule = duplicateReportRule;
    }

    public Integer getMaxAcceptedReports() {
        return maxAcceptedReports;
    }

    public void setMaxAcceptedReports(Integer maxAcceptedReports) {
        this.maxAcceptedReports = maxAcceptedReports;
    }

    public boolean isClosesAfterOneResolution() {
        return closesAfterOneResolution;
    }

    public void setClosesAfterOneResolution(boolean closesAfterOneResolution) {
        this.closesAfterOneResolution = closesAfterOneResolution;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public void setDeadline(Instant deadline) {
        this.deadline = deadline;
    }

    public String getSafeHarborStatement() {
        return safeHarborStatement;
    }

    public void setSafeHarborStatement(String safeHarborStatement) {
        this.safeHarborStatement = safeHarborStatement;
    }

    public BugFixJobStatus getStatus() {
        return status;
    }

    public void setStatus(BugFixJobStatus status) {
        this.status = status;
    }

    public UUID getSelectedFreelancerId() {
        return selectedFreelancerId;
    }

    public void setSelectedFreelancerId(UUID selectedFreelancerId) {
        this.selectedFreelancerId = selectedFreelancerId;
    }

    public int getJobVersion() {
        return jobVersion;
    }

    public void setJobVersion(int jobVersion) {
        this.jobVersion = jobVersion;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public BigDecimal getVerifiedAmount() {
        return verifiedAmount;
    }

    public void setVerifiedAmount(BigDecimal verifiedAmount) {
        this.verifiedAmount = verifiedAmount;
    }

    public String getVerifiedCurrency() {
        return verifiedCurrency;
    }

    public void setVerifiedCurrency(String verifiedCurrency) {
        this.verifiedCurrency = verifiedCurrency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }
}
