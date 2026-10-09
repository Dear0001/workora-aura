package com.workora.work.service;

import com.workora.common.storage.StorageKeyValidator;
import com.workora.work.dto.SubmissionRequest;
import com.workora.work.dto.SubmissionResponse;
import com.workora.work.entity.AssigneeStatus;
import com.workora.work.entity.CompletionRule;
import com.workora.work.entity.Submission;
import com.workora.work.entity.Task;
import com.workora.work.entity.TaskAssignee;
import com.workora.work.entity.TaskStatus;
import com.workora.work.entity.TaskStatusHistory;
import com.workora.work.exception.AppException;
import com.workora.work.exception.NotFoundException;
import com.workora.work.mapper.WorkMapper;
import com.workora.work.repository.SubmissionRepository;
import com.workora.work.repository.TaskAssigneeRepository;
import com.workora.work.repository.TaskRepository;
import com.workora.work.repository.TaskStatusHistoryRepository;
import com.workora.work.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final TaskRepository taskRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final TaskStatusHistoryRepository historyRepository;
    private final WorkAuthorization authorization;
    private final WorkMapper mapper;

    public SubmissionService(SubmissionRepository submissionRepository,
                             TaskRepository taskRepository,
                             TaskAssigneeRepository taskAssigneeRepository,
                             TaskStatusHistoryRepository historyRepository,
                             WorkAuthorization authorization,
                             WorkMapper mapper) {
        this.submissionRepository = submissionRepository;
        this.taskRepository = taskRepository;
        this.taskAssigneeRepository = taskAssigneeRepository;
        this.historyRepository = historyRepository;
        this.authorization = authorization;
        this.mapper = mapper;
    }

    @Transactional
    public SubmissionResponse submit(UUID taskId, CurrentUser caller, SubmissionRequest request) {
        Task task = loadTask(taskId);
        authorization.requireActiveAssignee(task, caller);

        int version = (int) submissionRepository.countByTaskId(taskId) + 1;
        List<String> attachmentKeys = request.attachmentKeys() == null ? List.<String>of()
                : request.attachmentKeys().stream().map(key -> {
                    try {
                        return StorageKeyValidator.requireSafe(key);
                    } catch (IllegalArgumentException ex) {
                        throw new AppException(ex.getMessage());
                    }
                }).toList();
        Submission saved = submissionRepository.save(
                new Submission(taskId, caller.userId(), version, request.note(), attachmentKeys));

        taskAssigneeRepository.findByTaskIdAndUserIdAndRemovedAtIsNull(taskId, caller.userId())
                .ifPresent(assignee -> {
                    assignee.setStatus(AssigneeStatus.SUBMITTED);
                    taskAssigneeRepository.save(assignee);
                });

        if (task.getCompletionRule() == CompletionRule.ANY_ASSIGNEE
                && version == 1
                && task.getStatus() != TaskStatus.SUBMITTED) {
            TaskStatus from = task.getStatus();
            task.setStatus(TaskStatus.SUBMITTED);
            taskRepository.save(task);
            historyRepository.save(new TaskStatusHistory(
                    taskId, from, TaskStatus.SUBMITTED, caller.userId(), "Submission received (any-assignee rule)"));
        }
        return mapper.toSubmissionResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> listForTask(UUID taskId, CurrentUser caller) {
        Task task = loadTask(taskId);
        authorization.requireCanViewTask(task, caller);
        return submissionRepository.findByTaskIdOrderBySubmittedAtAsc(taskId).stream()
                .map(mapper::toSubmissionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countSubmitters(UUID taskId, CurrentUser caller) {
        Task task = loadTask(taskId);
        authorization.requireCanViewTask(task, caller);
        return submissionRepository.findByTaskIdOrderBySubmittedAtAsc(taskId).stream()
                .map(Submission::getAuthorId)
                .distinct()
                .count();
    }

    private Task loadTask(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
    }
}

