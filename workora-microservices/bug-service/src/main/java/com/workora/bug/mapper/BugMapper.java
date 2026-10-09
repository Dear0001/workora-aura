package com.workora.bug.mapper;

import com.workora.bug.dto.BugReportResponse;
import com.workora.bug.dto.EvidenceResponse;
import com.workora.bug.entity.BugEvidence;
import com.workora.bug.entity.BugReport;
import org.springframework.stereotype.Component;

@Component
public class BugMapper {

    public BugReportResponse toResponse(BugReport report) {
        return new BugReportResponse(
                report.getId(),
                report.getPhaseId(),
                report.getTaskId(),
                report.getProjectId(),
                report.getDepartmentId(),
                report.getCompanyId(),
                report.getReporterId(),
                report.getTitle(),
                report.getDescription(),
                report.getProblem(),
                report.getLocation(),
                report.getCategory(),
                report.getSeverity(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }

    public EvidenceResponse toResponse(BugEvidence evidence) {
        return new EvidenceResponse(
                evidence.getId(),
                evidence.getReportId(),
                evidence.getFileKey(),
                evidence.getMimeType(),
                evidence.getDescription(),
                evidence.getUploadedBy(),
                evidence.getUploadedAt()
        );
    }
}
