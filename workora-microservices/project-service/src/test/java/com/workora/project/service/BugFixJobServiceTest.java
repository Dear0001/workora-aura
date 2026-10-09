package com.workora.project.service;

import com.workora.project.client.CallerMembership;
import com.workora.project.client.OrganizationClient;
import com.workora.project.dto.ApproveFeeRequest;
import com.workora.project.dto.BugFixJobRequest;
import com.workora.project.dto.BugFixJobUpdateRequest;
import com.workora.project.entity.BugFixJob;
import com.workora.project.entity.BugFixJobStatus;
import com.workora.project.entity.BugFixJobStatusHistory;
import com.workora.project.entity.FixExpectation;
import com.workora.project.entity.JobCategory;
import com.workora.project.entity.Phase;
import com.workora.project.entity.Project;
import com.workora.project.exception.AppException;
import com.workora.project.exception.ConflictException;
import com.workora.project.exception.ForbiddenException;
import com.workora.project.mapper.ProjectMapper;
import com.workora.project.repository.BugFixJobApplicationRepository;
import com.workora.project.repository.BugFixJobRepository;
import com.workora.project.repository.BugFixJobReviewRepository;
import com.workora.project.repository.BugFixJobStatusHistoryRepository;
import com.workora.project.repository.BugFixJobSubmissionRepository;
import com.workora.project.repository.PhaseRepository;
import com.workora.project.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BugFixJobServiceTest {

    @Mock private BugFixJobRepository jobRepository;
    @Mock private PhaseRepository phaseRepository;
    @Mock private BugFixJobApplicationRepository applicationRepository;
    @Mock private BugFixJobSubmissionRepository submissionRepository;
    @Mock private BugFixJobReviewRepository reviewRepository;
    @Mock private BugFixJobStatusHistoryRepository historyRepository;
    @Mock private ProjectService projectService;
    @Mock private ProjectAuthorization projectAuthorization;
    @Mock private BugFixJobAuthorization jobAuthorization;
    @Mock private ProjectMapper mapper;
    @Mock private OrganizationClient organizationClient;
    @Mock private com.workora.project.event.ProjectEventPublisher projectEventPublisher;

    private BugFixJobService service;

    private final UUID jobId = UUID.randomUUID();
    private final CurrentUser caller = new CurrentUser(UUID.randomUUID(), "pm@workora.test");

    @BeforeEach
    void setUp() {
        service = new BugFixJobService(jobRepository, phaseRepository, applicationRepository,
                submissionRepository, reviewRepository, historyRepository, projectService,
                projectAuthorization, jobAuthorization, mapper, projectEventPublisher);
    }

    private BugFixJobRequest minimalRequest() {
        return new BugFixJobRequest(
                "Fix login bug", null, null, null, null, null, null, null, null, null,
                JobCategory.FUNCTIONAL, null, null, null, FixExpectation.PATCH, null, null, null,
                new BigDecimal("250.00"), "USD", null, null, null, null, null, null, null);
    }

    private BugFixJob jobWithStatus(BugFixJobStatus status) {
        BugFixJob job = new BugFixJob(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "Fix login bug");
        job.setFee(new BigDecimal("250.00"));
        job.setCurrency("USD");
        job.setStatus(status);
        return job;
    }

    @Test
    void create_denormalizesProjectContext_andWritesDraftHistory() {
        UUID phaseId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        Phase phase = new Phase(projectId, "Phase 1");
        Project project = new Project(companyId, departmentId, "Proj", managerId);
        ReflectionTestUtils.setField(project, "id", projectId);

        when(phaseRepository.findById(phaseId)).thenReturn(Optional.of(phase));
        when(projectService.loadActive(projectId)).thenReturn(project);
        when(jobRepository.save(any(BugFixJob.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(phaseId, caller, minimalRequest());

        ArgumentCaptor<BugFixJob> jobCaptor = ArgumentCaptor.forClass(BugFixJob.class);
        verify(jobRepository).save(jobCaptor.capture());
        BugFixJob saved = jobCaptor.getValue();
        assertThat(saved.getProjectId()).isEqualTo(projectId);
        assertThat(saved.getDepartmentId()).isEqualTo(departmentId);
        assertThat(saved.getCompanyId()).isEqualTo(companyId);
        assertThat(saved.getProjectManagerId()).isEqualTo(managerId);
        assertThat(saved.getPhaseId()).isEqualTo(phaseId);
        assertThat(saved.getStatus()).isEqualTo(BugFixJobStatus.DRAFT);
        assertThat(saved.getJobVersion()).isEqualTo(1);

        ArgumentCaptor<BugFixJobStatusHistory> historyCaptor =
                ArgumentCaptor.forClass(BugFixJobStatusHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getFromStatus()).isNull();
        assertThat(historyCaptor.getValue().getToStatus()).isEqualTo(BugFixJobStatus.DRAFT);
    }

    @Test
    void update_rejectsFeeChangeAfterAssignment() {
        BugFixJob job = jobWithStatus(BugFixJobStatus.ASSIGNED);
        job.setSelectedFreelancerId(UUID.randomUUID());
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        BugFixJobUpdateRequest request = new BugFixJobUpdateRequest(
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                new BigDecimal("300.00"), null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.update(jobId, caller, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("cannot change after a freelancer is assigned");
    }

    @Test
    void approveFee_rejectsWhenStatusNotTestPassed() {
        BugFixJob job = jobWithStatus(BugFixJobStatus.SUBMITTED);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        ApproveFeeRequest request = new ApproveFeeRequest(new BigDecimal("250.00"), "USD", null);

        assertThatThrownBy(() -> service.approveFee(jobId, caller, request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Fee can only be approved after testing passed");
    }

    @Test
    void approveFee_rejectsWhenAmountDiffersFromFee() {
        BugFixJob job = jobWithStatus(BugFixJobStatus.TEST_PASSED);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        ApproveFeeRequest request = new ApproveFeeRequest(new BigDecimal("999.00"), "USD", null);

        assertThatThrownBy(() -> service.approveFee(jobId, caller, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("must equal the agreed fixed fee");
    }

    @Test
    void transition_rejectsInvalidMove() {
        BugFixJob job = jobWithStatus(BugFixJobStatus.OPEN);

        assertThatThrownBy(() -> service.transition(job, BugFixJobStatus.CLOSED, caller.userId(), "nope"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Invalid status transition from OPEN to CLOSED");
    }

    @Test
    void requireNotOwnCompany_throwsWhenCallerIsMember() {
        BugFixJobAuthorization realAuth = new BugFixJobAuthorization(organizationClient, applicationRepository);
        UUID companyId = UUID.randomUUID();
        BugFixJob job = new BugFixJob(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                companyId, UUID.randomUUID(), "Fix");
        when(organizationClient.myMembership(companyId)).thenReturn(new CallerMembership("DEVELOPER", null));

        assertThatThrownBy(() -> realAuth.requireNotOwnCompany(job, caller))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(ex -> assertThat(((ForbiddenException) ex).getCode())
                        .isEqualTo("OWN_COMPANY_ACTION_NOT_ALLOWED"));
    }

    @Test
    void requireNotOwnCompany_passesWhenCallerIsNotMember() {
        BugFixJobAuthorization realAuth = new BugFixJobAuthorization(organizationClient, applicationRepository);
        UUID companyId = UUID.randomUUID();
        BugFixJob job = new BugFixJob(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                companyId, UUID.randomUUID(), "Fix");
        when(organizationClient.myMembership(companyId))
                .thenThrow(new ForbiddenException("not a member", "NOT_A_COMPANY_MEMBER"));

        assertThatCode(() -> realAuth.requireNotOwnCompany(job, caller)).doesNotThrowAnyException();
    }
}
