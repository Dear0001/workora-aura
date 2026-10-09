package com.workora.project.entity;

/**
 * What the freelancer must deliver for the fixed fee, from a report only up to a
 * deployed fix.
 */
public enum FixExpectation {
    REPORT_ONLY,
    REMEDIATION_INSTRUCTIONS,
    PATCH,
    PULL_REQUEST,
    DEPLOYED_FIX
}
