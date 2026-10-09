package com.workora.work.service;

import com.workora.work.client.CallerMembership;
import com.workora.work.client.OrganizationClient;
import com.workora.work.client.ProjectClient;
import com.workora.work.client.ProjectContext;
import com.workora.work.dto.TaskRequest;
import com.workora.work.dto.TaskResponse;
import com.workora.work.dto.TaskSummaryResponse;
import com.workora.work.entity.CompletionRule;
import com.workora.work.entity.Submission;
import com.workora.work.entity.Task;
import com.workora.work.entity.TaskAttachment;
import com.workora.work.entity.TaskAssignee;
import com.workora.work.entity.TaskStatus;
import com.workora.work.entity.TaskStatusHistory;
import com.workora.work.exception.AppException;
import com.workora.work.exception.ForbiddenException;
import com.workora.work.exception.NotFoundException;
import com.workora.work.mapper.WorkMapper;
import com.workora.work.repository.SubmissionRepository;
import com.workora.work.repository.TaskAssigneeRepository;
import com.workora.work.repository.TaskAttachmentRepository;
import com.workora.work.repository.TaskRepository;
import com.workora.work.repository.TaskStatusHistoryRepository;
import com.workora.work.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskAssigneeRepository taskAssigneeRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private TaskAttachmentRepository attachmentRepository;
    @Mock
    private TaskStatusHistoryRepository historyRepository;
    @Mock
    private ProjectClient projectClient;
    @Mock
    private OrganizationClient organizationClient;

    private TaskService service;

    private final UUID phaseId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID departmentId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();
    private final UUID managerId = UUID.randomUUID();
    private final CurrentUser caller = new CurrentUser(UUID.randomUUID(), "dev@workora.test");
    private final CurrentUser manager = new CurrentUser(managerId, "pm@workora.test");

    @BeforeEach
    void setUp() {
        WorkAuthorization authorization =
                new WorkAuthorization(organizationClient, projectClient, taskAssigneeRepository);
        service = new TaskService(taskRepository, taskAssigneeRepository, submissionRepository,
                attachmentRepository, historyRepository, projectClient, authorization, new WorkMapper());
    }

    private ProjectContext context() {
        return new ProjectContext(phaseId, projectId, departmentId, companyId, managerId);
    }

    private Task taskManagedBy(UUID manager) {
        return new Task(phaseId, projectId, departmentId, companyId, manager, "Write docs", managerId);
    }

    @Test
    void create_isDeniedForPlainDeveloper() {
        when(projectClient.resolvePhase(phaseId)).thenReturn(context());
        when(organizationClient.myMembership(companyId))
                .thenReturn(new CallerMembership("DEVELOPER", departmentId));

        assertThatThrownBy(() -> service.create(phaseId, caller,
                new TaskRequest("Write docs", null, null, null)))
                .isInstanceOf(ForbiddenException.class)
                .extracting(ex -> ((ForbiddenException) ex).getCode())
                .isEqualTo("TASK_CREATE_DENIED");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void create_isAllowedForProjectManager() {
        when(projectClient.resolvePhase(phaseId)).thenReturn(context());
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse response = service.create(phaseId, manager,
                new TaskRequest("Write docs", "Draft the guide", null, CompletionRule.ANY_ASSIGNEE));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task saved = captor.getValue();
        assertThat(saved.getPhaseId()).isEqualTo(phaseId);
        assertThat(saved.getProjectId()).isEqualTo(projectId);
        assertThat(saved.getDepartmentId()).isEqualTo(departmentId);
        assertThat(saved.getCompanyId()).isEqualTo(companyId);
        assertThat(saved.getProjectManagerId()).isEqualTo(managerId);
        assertThat(saved.getStatus()).isEqualTo(TaskStatus.DRAFT);
        assertThat(saved.getCreatedBy()).isEqualTo(managerId);
        assertThat(response.title()).isEqualTo("Write docs");
        verify(historyRepository).save(any());
    }

    @Test
    void changeStatus_rejectsInvalidTransition() {
        UUID taskId = UUID.randomUUID();
        Task task = taskManagedBy(caller.userId());
        task.setStatus(TaskStatus.DRAFT);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.changeStatus(taskId, caller, TaskStatus.COMPLETED, "ship it"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Invalid status transition");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void summary_returnsCountsAndLatestStatus() {
        UUID taskId = UUID.randomUUID();
        Task task = taskManagedBy(managerId);
        task.setStatus(TaskStatus.IN_PROGRESS);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskAssigneeRepository.findByTaskIdAndRemovedAtIsNull(taskId))
                .thenReturn(List.of(
                        new TaskAssignee(taskId, UUID.randomUUID(), departmentId, managerId),
                        new TaskAssignee(taskId, UUID.randomUUID(), departmentId, managerId)));
        when(submissionRepository.findByTaskIdOrderBySubmittedAtAsc(taskId))
                .thenReturn(List.of(
                        new Submission(taskId, UUID.randomUUID(), 1, "first", List.of()),
                        new Submission(taskId, UUID.randomUUID(), 2, "second", List.of())));
        when(attachmentRepository.findByTaskIdOrderByUploadedAtAsc(taskId))
                .thenReturn(List.of(new TaskAttachment(taskId, "team/notes.txt", "notes.txt", "text/plain", "draft", UUID.randomUUID())));
        when(historyRepository.findFirstByTaskIdOrderByChangedAtDesc(taskId))
                .thenReturn(Optional.of(new TaskStatusHistory(taskId, TaskStatus.DRAFT, TaskStatus.IN_PROGRESS, managerId, "Started")));

        TaskSummaryResponse response = service.summary(taskId, manager);

        assertThat(response.id()).isEqualTo(taskId);
        assertThat(response.assigneeCount()).isEqualTo(2);
        assertThat(response.submissionCount()).isEqualTo(2);
        assertThat(response.attachmentCount()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.latestStatusReason()).isEqualTo("Started");
    }

    @Test
    void get_throwsNotFoundForNonAssigneeNonManager() {
        UUID taskId = UUID.randomUUID();
        Task task = taskManagedBy(managerId);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(organizationClient.myMembership(companyId))
                .thenReturn(new CallerMembership("DEVELOPER", departmentId));
        when(taskAssigneeRepository.existsByTaskIdAndUserIdAndRemovedAtIsNull(task.getId(), caller.userId()))
                .thenReturn(false);

        assertThatThrownBy(() -> service.get(taskId, caller))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Task not found");
    }
}
