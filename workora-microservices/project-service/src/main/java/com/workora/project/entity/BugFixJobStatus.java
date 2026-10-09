package com.workora.project.entity;

/**
 * Bug-Fix Job lifecycle. DRAFT is authored by the project manager, published to OPEN,
 * assigned to a selected freelancer, then driven through submission, review, fee
 * approval and the (deferred) payment seam to CLOSED. Terminal-ish branches:
 * CANCELLED and DISPUTED.
 */
public enum BugFixJobStatus {
    DRAFT,
    OPEN,
    ASSIGNED,
    SUBMITTED,
    UNDER_REVIEW,
    CHANGES_REQUESTED,
    TEST_FAILED,
    TEST_PASSED,
    FEE_APPROVED,
    WAITING_FOR_COMPANY_PAYMENT,
    PAYMENT_VERIFIED,
    FEE_RECEIPT_CONFIRMED,
    CLOSED,
    CANCELLED,
    DISPUTED
}
