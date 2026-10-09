package com.workora.work.service;

import com.workora.work.client.ProjectClient;
import com.workora.work.client.ProjectContext;
import com.workora.work.dto.AssigneeProgress;
import com.workora.work.dto.AssigneeRequest;
import com.workora.work.dto.AssigneeResponse;
import com.workora.work.dto.TaskDetailResponse;
import com.workora.work.dto.TaskHistoryResponse;
import com.workora.work.dto.TaskProgressResponse;
import com.workora.work.dto.TaskRequest;
import com.workora.work.dto.TaskResponse;
import com.workora.work.dto.TaskSummaryResponse;
import com.workora.work.dto.TaskUpdateRequest;
import com.workora.work.entity.CompletionRule;
import com.workora.work.entity.Submission;
import com.workora.work.entity.Task;
import com.workora.work.entity.TaskAssignee;
import com.workora.work.entity.TaskStatus;
import com.workora.work.entity.TaskStatusHistory;
import com.workora.work.exception.AppException;
import com.workora.work.exception.ConflictException;
import com.workora.work.exception.NotFoundException;
import com.workora.work.mapper.WorkMapper;
import com.workora.work.repository.SubmissionRepository;
import com.workora.work.repository.TaskAssigneeRepository;
import com.workora.work.repository.TaskAttachmentRepository;
import com.workora.work.repository.TaskRepository;
import com.workora.work.repository.TaskStatusHistoryRepository;
import com.workora.work.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED_TRANSITIONS = Map.of(
            TaskStatus.DRAFT, EnumSet.of(TaskStatus.ASSIGNED),
            TaskStatus.ASSIGNED, EnumSet.of(TaskStatus.NOT_YET, TaskStatus.IN_PROGRESS, TaskStatus.SUBMITTED),
            TaskStatus.NOT_YET, EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.SUBMITTED),
            TaskStatus.IN_PROGRESS, EnumSet.of(TaskStatus.SUBMITTED),
            TaskStatus.SUBMITTED, EnumSet.of(TaskStatus.ACCEPTED, TaskStatus.CHANGES_REQUESTED),
            TaskStatus.ACCEPTED, EnumSet.of(TaskStatus.COMPLETED),
            TaskStatus.CHANGES_REQUESTED, EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.SUBMITTED, TaskStatus.COMPLETED),
            TaskStatus.COMPLETED, EnumSet.noneOf(TaskStatus.class)
    );

    private final TaskRepository taskRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final SubmissionRepository submissionRepository;
    private final TaskAttachmentRepository attachmentRepository;
    private final TaskStatusHistoryRepository historyRepository;
    private final ProjectClient projectClient;
    private final WorkAuthorization authorization;
    private final WorkMapper mapper;

    public TaskService(TaskRepository taskRepository,
                       TaskAssigneeRepository taskAssigneeRepository,
                       SubmissionRepository submissionRepository,
                       TaskAttachmentRepository attachmentRepository,
                       TaskStatusHistoryRepository historyRepository,
                       ProjectClient projectClient,
                       WorkAuthorization authorization,
                       WorkMapper mapper) {
        this.taskRepository = taskRepository;
        this.taskAssigneeRepository = taskAssigneeRepository;
        this.submissionRepository = submissionRepository;
        this.attachmentRepository = attachmentRepository;
        this.historyRepository = historyRepository;
        this.projectClient = projectClient;
        this.authorization = authorization;
        this.mapper = mapper;
    }

    @Transactional
    public TaskResponse create(UUID phaseId, CurrentUser caller, TaskRequest request) {
        ProjectContext context = projectClient.resolvePhase(phaseId);
        authorization.requireCanCreateTask(context, caller);

        Task task = new Task(
                context.phaseId(),
                context.projectId(),
                context.departmentId(),
                context.companyId(),
                context.projectManagerId(),
                request.title().trim(),
                caller.userId());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        task.setCompletionRule(request.completionRule() != null ? request.completionRule() : CompletionRule.ALL_ASSIGNEES);
        task.setStatus(TaskStatus.DRAFT);

        Task saved = taskRepository.save(task);
        historyRepository.save(new TaskStatusHistory(
                saved.getId(), null, TaskStatus.DRAFT, caller.userId(), "Task created"));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TaskDetailResponse get(UUID taskId, CurrentUser caller) {
        Task task = load(taskId);
        authorization.requireCanViewTask(task, caller);
        List<TaskAssignee> assignees = taskAssigneeRepository.findByTaskIdAndRemovedAtIsNull(taskId);
        return new TaskDetailResponse(
                mapper.toResponse(task),
                assignees.stream().map(mapper::toAssigneeResponse).toList(),
                buildProgress(task, assignees));
    }

    @Transactional(readOnly = true)
    public List<TaskHistoryResponse> history(UUID taskId, CurrentUser caller) {
        Task task = load(taskId);
        authorization.requireCanViewTask(task, caller);
        return historyRepository.findByTaskIdOrderByChangedAtAsc(taskId).stream()
                .map(history -> new TaskHistoryResponse(
                        history.getId(),
                        history.getFromStatus(),
                        history.getToStatus(),
                        history.getActorId(),
                        history.getReason(),
                        history.getChangedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskSummaryResponse summary(UUID taskId, CurrentUser caller) {
        Task task = load(taskId);
        authorization.requireCanViewTask(task, caller);

        long assigneeCount = taskAssigneeRepository.findByTaskIdAndRemovedAtIsNull(taskId).size();
        long submissionCount = submissionRepository.findByTaskIdOrderBySubmittedAtAsc(taskId).size();
        long attachmentCount = attachmentRepository.findByTaskIdOrderByUploadedAtAsc(taskId).size();
        TaskStatusHistory latestStatus = historyRepository.findFirstByTaskIdOrderByChangedAtDesc(taskId).orElse(null);

        return new TaskSummaryResponse(
                task.getId(),
                task.getPhaseId(),
                task.getProjectId(),
                task.getTitle(),
                task.getStatus(),
                assigneeCount,
                submissionCount,
                attachmentCount,
                task.getUpdatedAt(),
                latestStatus != null ? latestStatus.getChangedAt() : task.getCreatedAt(),
                latestStatus != null ? latestStatus.getActorId() : task.getCreatedBy(),
                latestStatus != null ? latestStatus.getReason() : null
        );
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listForPhase(UUID phaseId, CurrentUser caller) {
        ProjectContext context = projectClient.resolvePhase(phaseId);
        boolean manager = authorization.canManageInContext(context, caller);
        return taskRepository.findByPhaseId(phaseId).stream()
                .filter(task -> manager
                        || taskAssigneeRepository.existsByTaskIdAndUserIdAndRemovedAtIsNull(task.getId(), caller.userId()))
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listMyAssigned(CurrentUser caller) {
        List<UUID> taskIds = taskAssigneeRepository.findByUserIdAndRemovedAtIsNull(caller.userId()).stream()
                .map(TaskAssignee::getTaskId)
                .distinct()
                .toList();
        return taskRepository.findAllById(taskIds).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public TaskResponse update(UUID taskId, CurrentUser caller, TaskUpdateRequest request) {
        Task task = load(taskId);
        authorization.requireCanManageTask(task, caller);

        if (request.title() != null && !request.title().isBlank()) {
            task.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }
        if (request.completionRule() != null) {
            task.setCompletionRule(request.completionRule());
        }
        return mapper.toResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<AssigneeResponse> listAssignees(UUID taskId, CurrentUser caller) {
        Task task = load(taskId);
        authorization.requireCanViewTask(task, caller);
        return taskAssigneeRepository.findByTaskIdAndRemovedAtIsNull(taskId).stream()
                .map(mapper::toAssigneeResponse)
                .toList();
    }

    @Transactional
    public AssigneeResponse addAssignee(UUID taskId, CurrentUser caller, AssigneeRequest request) {
        Task task = load(taskId);
        authorization.requireCanManageTask(task, caller);

        if (taskAssigneeRepository.existsByTaskIdAndUserIdAndRemovedAtIsNull(taskId, request.userId())) {
            throw new ConflictException("User is already an active assignee of this task");
        }

        TaskAssignee assignee = taskAssigneeRepository.save(
                new TaskAssignee(taskId, request.userId(), task.getDepartmentId(), caller.userId()));

        if (task.getStatus() == TaskStatus.DRAFT) {
            task.setStatus(TaskStatus.ASSIGNED);
            taskRepository.save(task);
            historyRepository.save(new TaskStatusHistory(
                    taskId, TaskStatus.DRAFT, TaskStatus.ASSIGNED, caller.userId(), "Assignee added"));
        }
        return mapper.toAssigneeResponse(assignee);
    }

    @Transactional
    public void removeAssignee(UUID taskId, UUID userId, CurrentUser caller) {
        Task task = load(taskId);
        authorization.requireCanManageTask(task, caller);

        TaskAssignee assignee = taskAssigneeRepository.findByTaskIdAndUserIdAndRemovedAtIsNull(taskId, userId)
                .orElseThrow(() -> new NotFoundException("Assignee not found"));
        assignee.setRemovedAt(Instant.now());
        taskAssigneeRepository.save(assignee);
    }

    @Transactional
    public TaskResponse changeStatus(UUID taskId, CurrentUser caller, TaskStatus target, String reason) {
        Task task = load(taskId);
        authorization.requireCanManageTask(task, caller);

        TaskStatus current = task.getStatus();
        if (target == null || !ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new AppException("Invalid status transition from " + current + " to " + target);
        }
        task.setStatus(target);
        Task saved = taskRepository.save(task);
        historyRepository.save(new TaskStatusHistory(taskId, current, target, caller.userId(), reason));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TaskProgressResponse progress(UUID taskId, CurrentUser caller) {
        Task task = load(taskId);
        authorization.requireCanViewTask(task, caller);
        return buildProgress(task, taskAssigneeRepository.findByTaskIdAndRemovedAtIsNull(taskId));
    }

    @Transactional(readOnly = true)
    public Task load(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
    }

    private TaskProgressResponse buildProgress(Task task, List<TaskAssignee> assignees) {
        Map<UUID, Long> submissionCounts = submissionRepository
                .findByTaskIdOrderBySubmittedAtAsc(task.getId()).stream()
                .collect(Collectors.groupingBy(Submission::getAuthorId, Collectors.counting()));
        List<AssigneeProgress> progress = assignees.stream()
                .map(assignee -> new AssigneeProgress(
                        assignee.getUserId(),
                        assignee.getStatus(),
                        submissionCounts.getOrDefault(assignee.getUserId(), 0L)))
                .toList();
        return new TaskProgressResponse(task.getId(), task.getStatus(), task.getCompletionRule(), progress);
    }
}
