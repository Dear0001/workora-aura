package com.workora.work.service;

import com.workora.common.storage.StorageKeyValidator;
import com.workora.work.dto.TaskAttachmentRequest;
import com.workora.work.dto.TaskAttachmentResponse;
import com.workora.work.entity.Task;
import com.workora.work.entity.TaskAttachment;
import com.workora.work.exception.AppException;
import com.workora.work.exception.ForbiddenException;
import com.workora.work.exception.NotFoundException;
import com.workora.work.mapper.WorkMapper;
import com.workora.work.repository.TaskAttachmentRepository;
import com.workora.work.repository.TaskRepository;
import com.workora.work.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TaskAttachmentService {

    private final TaskAttachmentRepository attachmentRepository;
    private final TaskRepository taskRepository;
    private final WorkAuthorization authorization;
    private final WorkMapper mapper;

    public TaskAttachmentService(TaskAttachmentRepository attachmentRepository,
                                 TaskRepository taskRepository,
                                 WorkAuthorization authorization,
                                 WorkMapper mapper) {
        this.attachmentRepository = attachmentRepository;
        this.taskRepository = taskRepository;
        this.authorization = authorization;
        this.mapper = mapper;
    }

    @Transactional
    public TaskAttachmentResponse add(UUID taskId, CurrentUser caller, TaskAttachmentRequest request) {
        Task task = loadTask(taskId);
        authorization.requireCanViewTask(task, caller);

        String safeKey;
        try {
            safeKey = StorageKeyValidator.requireSafe(request.fileKey());
        } catch (IllegalArgumentException ex) {
            throw new AppException(ex.getMessage());
        }

        TaskAttachment attachment = attachmentRepository.save(new TaskAttachment(
                taskId,
                safeKey,
                request.fileName(),
                request.mimeType(),
                request.description(),
                caller.userId()));
        return mapper.toAttachmentResponse(attachment);
    }

    @Transactional(readOnly = true)
    public List<TaskAttachmentResponse> list(UUID taskId, CurrentUser caller) {
        Task task = loadTask(taskId);
        authorization.requireCanViewTask(task, caller);
        return attachmentRepository.findByTaskIdOrderByUploadedAtAsc(taskId).stream()
                .map(mapper::toAttachmentResponse)
                .toList();
    }

    @Transactional
    public void delete(UUID taskId, UUID attachmentId, CurrentUser caller) {
        Task task = loadTask(taskId);
        authorization.requireCanViewTask(task, caller);

        TaskAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new NotFoundException("Attachment not found"));
        if (!attachment.getTaskId().equals(taskId)) {
            throw new NotFoundException("Attachment not found");
        }

        boolean canDelete = attachment.getUploadedBy().equals(caller.userId()) || authorization.canManage(task, caller);
        if (!canDelete) {
            throw new ForbiddenException(
                    "Only the uploader or task manager may delete this attachment",
                    "TASK_ATTACHMENT_DELETE_DENIED");
        }

        attachmentRepository.delete(attachment);
    }

    private Task loadTask(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
    }
}
