package com.workora.bug.service;

import com.workora.bug.client.CallerMembership;
import com.workora.bug.client.OrganizationClient;
import com.workora.bug.client.ProjectClient;
import com.workora.bug.client.ProjectContext;
import com.workora.bug.dto.BugReportRequest;
import com.workora.bug.dto.StatusChangeRequest;
import com.workora.bug.entity.BugCategory;
import com.workora.bug.entity.BugReport;
import com.workora.bug.entity.BugReportStatus;
import com.workora.bug.entity.BugSeverity;
import com.workora.bug.entity.BugStatusHistory;
import com.workora.bug.exception.AppException;
import com.workora.bug.exception.ForbiddenException;
import com.workora.bug.exception.NotFoundException;
import com.workora.bug.mapper.BugMapper;
import com.workora.bug.repository.BugEvidenceRepository;
import com.workora.bug.repository.BugReportRepository;
import com.workora.bug.repository.BugStatusHistoryRepository;
import com.workora.bug.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BugReportServiceTest {

    @Mock
    private BugReportRepository reportRepository;
    @Mock
    private BugStatusHistoryRepository historyRepository;
    @Mock
    private BugEvidenceRepository evidenceRepository;
    @Mock
    private ProjectClient projectClient;
    @Mock
    private OrganizationClient organizationClient;
    @Mock
    private BugMapper mapper;

    private BugReportService service;
    private BugEvidenceService evidenceService;

    private final UUID phaseId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID departmentId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();
    private final UUID projectManagerId = UUID.randomUUID();
    private final UUID reportId = UUID.randomUUID();
    private final CurrentUser caller = new CurrentUser(UUID.randomUUID(), "dev@workora.test");

    @BeforeEach
    void setUp() {
        BugAuthorization authorization = new BugAuthorization(organizationClient);
        service = new BugReportService(reportRepository, historyRepository, projectClient,
                organizationClient, authorization, mapper);
        evidenceService = new BugEvidenceService(evidenceRepository, service, authorization, mapper);
    }

    private BugReport report(UUID reporterId, UUID managerId, BugReportStatus status) {
        BugReport report = new BugReport();
        report.setPhaseId(phaseId);
        report.setProjectId(projectId);
        report.setDepartmentId(departmentId);
        report.setCompanyId(companyId);
        report.setReporterId(reporterId);
        report.setProjectManagerId(managerId);
        report.setTitle("Login crash");
        report.setDescription("App crashes on login");
        report.setStatus(status);
        return report;
    }

    @Test
    void submit_persistsDenormalizedContextAndInitialHistory() {
        when(projectClient.resolvePhase(phaseId)).thenReturn(
                new ProjectContext(phaseId, projectId, departmentId, companyId, projectManagerId));
        when(reportRepository.save(any(BugReport.class))).thenAnswer(inv -> inv.getArgument(0));

        service.submit(phaseId, caller, new BugReportRequest(
                " Login crash ", "App crashes on login", "Cannot log in", "/login",
                BugCategory.FUNCTIONAL, BugSeverity.HIGH, null));

        ArgumentCaptor<BugReport> reportCaptor = ArgumentCaptor.forClass(BugReport.class);
        verify(reportRepository).save(reportCaptor.capture());
        BugReport saved = reportCaptor.getValue();
        assertThat(saved.getPhaseId()).isEqualTo(phaseId);
        assertThat(saved.getProjectId()).isEqualTo(projectId);
        assertThat(saved.getDepartmentId()).isEqualTo(departmentId);
        assertThat(saved.getCompanyId()).isEqualTo(companyId);
        assertThat(saved.getProjectManagerId()).isEqualTo(projectManagerId);
        assertThat(saved.getReporterId()).isEqualTo(caller.userId());
        assertThat(saved.getTitle()).isEqualTo("Login crash");
        assertThat(saved.getStatus()).isEqualTo(BugReportStatus.SUBMITTED);

        ArgumentCaptor<BugStatusHistory> historyCaptor = ArgumentCaptor.forClass(BugStatusHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        BugStatusHistory history = historyCaptor.getValue();
        assertThat(history.getFromStatus()).isNull();
        assertThat(history.getToStatus()).isEqualTo(BugReportStatus.SUBMITTED);
        assertThat(history.getActorId()).isEqualTo(caller.userId());
    }

    @Test
    void changeStatus_rejectsInvalidTransition() {
        // Caller is the denormalized project manager, so triage authorization passes
        // and only the transition rule can reject.
        BugReport report = report(caller.userId(), caller.userId(), BugReportStatus.SUBMITTED);
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> service.changeStatus(reportId, caller,
                new StatusChangeRequest(BugReportStatus.CLOSED, "done")))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Cannot transition");
    }

    @Test
    void get_byUnrelatedCaller_throwsNotFound() {
        BugReport report = report(UUID.randomUUID(), projectManagerId, BugReportStatus.SUBMITTED);
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(organizationClient.myMembership(companyId))
                .thenThrow(new ForbiddenException("You are not a member of this company"));

        assertThatThrownBy(() -> service.get(reportId, caller))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Bug report not found");
    }

    @Test
    void evidenceList_byUnrelatedMember_isDenied() {
        BugReport report = report(UUID.randomUUID(), projectManagerId, BugReportStatus.SUBMITTED);
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(organizationClient.myMembership(companyId))
                .thenReturn(new CallerMembership("DEVELOPER", departmentId));

        assertThatThrownBy(() -> evidenceService.list(reportId, caller))
                .isInstanceOf(ForbiddenException.class)
                .extracting(ex -> ((ForbiddenException) ex).getCode())
                .isEqualTo("EVIDENCE_ACCESS_DENIED");
    }
}
