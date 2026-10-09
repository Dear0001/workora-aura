package com.workora.bug.service;

import com.workora.bug.dto.EvidenceRequest;
import com.workora.bug.dto.EvidenceResponse;
import com.workora.bug.entity.BugEvidence;
import com.workora.bug.entity.BugReport;
import com.workora.bug.exception.ForbiddenException;
import com.workora.bug.exception.NotFoundException;
import com.workora.bug.mapper.BugMapper;
import com.workora.bug.repository.BugEvidenceRepository;
import com.workora.bug.security.CurrentUser;
import com.workora.common.storage.StorageKeyValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Evidence is PRIVATE: every access path goes through
 * {@link BugAuthorization#requireCanViewEvidence} before any key or metadata leaves
 * the service.
 */
@Service
public class BugEvidenceService {

    private final BugEvidenceRepository evidenceRepository;
    private final BugReportService bugReportService;
    private final BugAuthorization authorization;
    private final BugMapper mapper;

    public BugEvidenceService(BugEvidenceRepository evidenceRepository,
                              BugReportService bugReportService,
                              BugAuthorization authorization,
                              BugMapper mapper) {
        this.evidenceRepository = evidenceRepository;
        this.bugReportService = bugReportService;
        this.authorization = authorization;
        this.mapper = mapper;
    }

    @Transactional
    public EvidenceResponse add(UUID reportId, CurrentUser caller, EvidenceRequest request) {
        BugReport report = bugReportService.load(reportId);
        authorization.requireCanViewEvidence(report, caller);
        BugEvidence evidence = new BugEvidence(
                report.getId(),
                StorageKeyValidator.requireSafe(request.fileKey()),
                request.mimeType(),
                request.description(),
                caller.userId());
        return mapper.toResponse(evidenceRepository.save(evidence));
    }

    @Transactional(readOnly = true)
    public List<EvidenceResponse> list(UUID reportId, CurrentUser caller) {
        BugReport report = bugReportService.load(reportId);
        authorization.requireCanViewEvidence(report, caller);
        return evidenceRepository.findByReportId(reportId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public void delete(UUID reportId, UUID evidenceId, CurrentUser caller) {
        BugReport report = bugReportService.load(reportId);
        authorization.requireCanViewEvidence(report, caller);
        BugEvidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new NotFoundException("Evidence not found"));
        if (!evidence.getReportId().equals(reportId)) {
            throw new NotFoundException("Evidence not found");
        }
        if (!evidence.getUploadedBy().equals(caller.userId()) && !authorization.isReviewer(report, caller)) {
            throw new ForbiddenException(
                    "Only the uploader or an authorized company reviewer can delete evidence",
                    "EVIDENCE_DELETE_DENIED");
        }
        evidenceRepository.delete(evidence);
    }
}
