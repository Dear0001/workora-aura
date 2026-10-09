package com.workora.project.entity;

/**
 * Phase lifecycle: draft → published (public/private) → closed → archived.
 */
public enum PhaseStatus {
    DRAFT,
    PUBLISHED,
    CLOSED,
    ARCHIVED
}
