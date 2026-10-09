package com.workora.bug.dto;

import com.workora.bug.entity.BugCategory;
import com.workora.bug.entity.BugReportStatus;
import com.workora.bug.entity.BugSeverity;

import java.time.Instant;
import java.util.UUID;

/**
 * Public/list projection of a bug report. Deliberately carries NO evidence fields —
 * evidence is private and only exposed via the gated evidence endpoints.
 */
public record BugReportResponse(
        UUID id,
        UUID phaseId,
        UUID taskId,
        UUID projectId,
        UUID departmentId,
        UUID companyId,
        UUID reporterId,
        String title,
        String description,
        String problem,
        String location,
        BugCategory category,
        BugSeverity severity,
        BugReportStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
