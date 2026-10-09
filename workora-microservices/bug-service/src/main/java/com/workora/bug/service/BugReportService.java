package com.workora.bug.service;

import com.workora.bug.client.OrganizationClient;
import com.workora.bug.client.ProjectClient;
import com.workora.bug.client.ProjectContext;
import com.workora.bug.dto.BugReportDetailResponse;
import com.workora.bug.dto.BugReportRequest;
import com.workora.bug.dto.BugReportResponse;
import com.workora.bug.dto.StatusChangeRequest;
import com.workora.bug.entity.BugReport;
import com.workora.bug.entity.BugReportStatus;
import com.workora.bug.entity.BugStatusHistory;
import com.workora.bug.exception.AppException;
import com.workora.bug.exception.NotFoundException;
import com.workora.bug.mapper.BugMapper;
import com.workora.bug.repository.BugReportRepository;
import com.workora.bug.repository.BugStatusHistoryRepository;
import com.workora.bug.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BugReportService {

    private final BugReportRepository bugReportRepository;
    private final BugStatusHistoryRepository historyRepository;
    private final ProjectClient projectClient;
    private final OrganizationClient organizationClient;
    private final BugAuthorization authorization;
    private final BugMapper mapper;

    public BugReportService(BugReportRepository bugReportRepository,
                            BugStatusHistoryRepository historyRepository,
                            ProjectClient projectClient,
                            OrganizationClient organizationClient,
                            BugAuthorization authorization,
                            BugMapper mapper) {
        this.bugReportRepository = bugReportRepository;
        this.historyRepository = historyRepository;
        this.projectClient = projectClient;
        this.organizationClient = organizationClient;
        this.authorization = authorization;
        this.mapper = mapper;
    }

    /**
     * Submits a bug report against a phase; {@code request.taskId()} optionally
     * narrows it to a task. The phase is always resolved through the Project service,
     * which both authorizes the caller and yields the ownership context denormalized
     * onto the report.
     */
    @Transactional
    public BugReportResponse submit(UUID phaseId, CurrentUser caller, BugReportRequest request) {
        UUID taskId = request.taskId();
        if (phaseId == null && taskId == null) {
            throw new AppException("A bug report must reference a phase or a task");
        }
        ProjectContext context = projectClient.resolvePhase(phaseId);

        BugReport report = new BugReport();
        report.setPhaseId(phaseId);
        report.setTaskId(taskId);
        report.setProjectId(context.projectId());
        report.setDepartmentId(context.departmentId());
        report.setCompanyId(context.companyId());
        report.setProjectManagerId(context.projectManagerId());
        report.setReporterId(caller.userId());
        report.setTitle(request.title().trim());
        report.setDescription(request.description());
        report.setProblem(request.problem());
        report.setLocation(request.location());
        if (request.category() != null) {
            report.setCategory(request.category());
        }
        if (request.severity() != null) {
            report.setSeverity(request.severity());
        }
        report.setStatus(BugReportStatus.SUBMITTED);

        BugReport saved = bugReportRepository.save(report);
        historyRepository.save(new BugStatusHistory(
                saved.getId(), null, BugReportStatus.SUBMITTED, caller.userId(), "Bug report submitted"));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BugReportDetailResponse get(UUID reportId, CurrentUser caller) {
        BugReport report = load(reportId);
        authorization.requireCanViewReport(report, caller);
        return toDetail(report, caller);
    }

    @Transactional(readOnly = true)
    public List<BugReportResponse> listForPhase(UUID phaseId, CurrentUser caller) {
        // Resolving through the Project service authorizes the caller against the phase.
        ProjectContext context = projectClient.resolvePhase(phaseId);
        var membership = organizationClient.myMembership(context.companyId());
        return bugReportRepository.findByPhaseId(phaseId).stream()
                .filter(report -> report.getReporterId().equals(caller.userId())
                        || authorization.isReviewer(report, caller, membership))
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BugReportResponse> listMine(CurrentUser caller) {
        return bugReportRepository.findByReporterId(caller.userId()).stream()
                .filter(report -> authorization.canViewEvidence(report, caller))
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public BugReportDetailResponse changeStatus(UUID reportId, CurrentUser caller, StatusChangeRequest request) {
        BugReport report = load(reportId);
        authorization.requireCanTriage(report, caller);
        BugReportStatus from = report.getStatus();
        BugReportStatus to = request.status();
        if (!isAllowedTransition(from, to)) {
            throw new AppException("Cannot transition a bug report from " + from + " to " + to);
        }
        report.setStatus(to);
        BugReport saved = bugReportRepository.save(report);
        historyRepository.save(new BugStatusHistory(saved.getId(), from, to, caller.userId(), request.reason()));
        return toDetail(saved, caller);
    }

    @Transactional(readOnly = true)
    public BugReport load(UUID reportId) {
        return bugReportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Bug report not found"));
    }

    private BugReportDetailResponse toDetail(BugReport report, CurrentUser caller) {
        List<BugStatusHistory> history =
                historyRepository.findByReportIdOrderByChangedAtAsc(report.getId());
        return new BugReportDetailResponse(
                mapper.toResponse(report),
                history,
                authorization.canViewEvidence(report, caller));
    }

    private boolean isAllowedTransition(BugReportStatus from, BugReportStatus to) {
        return switch (from) {
            case SUBMITTED -> to == BugReportStatus.TRIAGED;
            case TRIAGED -> to == BugReportStatus.DUPLICATE
                    || to == BugReportStatus.ACCEPTED
                    || to == BugReportStatus.REJECTED;
            case ACCEPTED -> to == BugReportStatus.IN_PROGRESS;
            case IN_PROGRESS -> to == BugReportStatus.RESOLVED;
            case RESOLVED -> to == BugReportStatus.CLOSED;
            case DUPLICATE, REJECTED, CLOSED -> false;
        };
    }
}
