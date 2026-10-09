package com.workora.bug.dto;

import com.workora.bug.entity.BugStatusHistory;

import java.util.List;

public record BugReportDetailResponse(
        BugReportResponse report,
        List<BugStatusHistory> history,
        boolean canViewEvidence
) {
}
