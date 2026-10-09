package com.workora.project.entity;

/**
 * Lifecycle of a freelancer's application to a job. Exactly one is SELECTED; the
 * remaining PENDING applications are REJECTED on selection; an applicant may
 * WITHDRAW while still PENDING.
 */
public enum JobApplicationDecision {
    PENDING,
    SELECTED,
    REJECTED,
    WITHDRAWN
}
