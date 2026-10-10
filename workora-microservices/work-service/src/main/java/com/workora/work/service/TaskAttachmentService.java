package com.workora.work.service;

import com.workora.common.storage.StorageKeyValidator;
import com.workora.common.storage.StorageObjectService;
import com.workora.work.dto.TaskAttachmentUploadRequest;
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
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class TaskAttachmentService {
    private static final long MAX_FILE_SIZE_BYTES = 25L * 1024 * 1024;
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "svg", "bmp", "tif", "tiff", "avif", "heic",
            "pdf", "txt", "log", "mp4", "mov", "zip");

    private final TaskAttachmentRepository attachmentRepository;
    private final TaskRepository taskRepository;
    private final WorkAuthorization authorization;
    private final WorkMapper mapper;
    private final StorageObjectService storage;

    public TaskAttachmentService(TaskAttachmentRepository attachmentRepository,
                                 TaskRepository taskRepository,
                                 WorkAuthorization authorization,
                                 WorkMapper mapper,
                                 StorageObjectService storage) {
        this.attachmentRepository = attachmentRepository;
        this.taskRepository = taskRepository;
        this.authorization = authorization;
        this.mapper = mapper;
        this.storage = storage;
    }

    @Transactional
    public TaskAttachmentResponse add(UUID taskId, CurrentUser caller, TaskAttachmentRequest request) {
        Task task = loadTask(taskId);
        authorization.requireCanViewTask(task, caller);

        String safeKey = requireTaskFileKey(request.fileKey());
        requireSupportedFileName(request.fileName());
        StorageObjectService.StoredObjectMetadata object = storage.inspect(safeKey);
        if (object.size() > MAX_FILE_SIZE_BYTES) {
            throw new AppException("Task attachments must be 25 MB or smaller");
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
    public StorageObjectService.PresignedFileUpload createUpload(
            UUID taskId, CurrentUser caller, TaskAttachmentUploadRequest request) {
        Task task = loadTask(taskId);
        authorization.requireCanViewTask(task, caller);
        requireSupportedFileName(request.fileName());
        return storage.createUpload("tasks", request.fileName(), request.contentType());
    }

    @Transactional(readOnly = true)
    public List<TaskAttachmentResponse> list(UUID taskId, CurrentUser caller) {
        Task task = loadTask(taskId);
        authorization.requireCanViewTask(task, caller);
        return attachmentRepository.findByTaskIdOrderByUploadedAtAsc(taskId).stream()
                .map(mapper::toAttachmentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public String downloadUrl(UUID taskId, UUID attachmentId, CurrentUser caller) {
        Task task = loadTask(taskId);
        authorization.requireCanViewTask(task, caller);
        TaskAttachment attachment = loadAttachment(taskId, attachmentId);
        return storage.createDownload(requireTaskFileKey(attachment.getFileKey()));
    }

    @Transactional
    public void delete(UUID taskId, UUID attachmentId, CurrentUser caller) {
        Task task = loadTask(taskId);
        authorization.requireCanViewTask(task, caller);

        TaskAttachment attachment = loadAttachment(taskId, attachmentId);
        boolean canDelete = attachment.getUploadedBy().equals(caller.userId()) || authorization.canManage(task, caller);
        if (!canDelete) {
            throw new ForbiddenException(
                    "Only the uploader or task manager may delete this attachment",
                    "TASK_ATTACHMENT_DELETE_DENIED");
        }

        storage.delete(requireTaskFileKey(attachment.getFileKey()));
        attachmentRepository.delete(attachment);
    }

    private TaskAttachment loadAttachment(UUID taskId, UUID attachmentId) {
        TaskAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new NotFoundException("Attachment not found"));
        if (!attachment.getTaskId().equals(taskId)) {
            throw new NotFoundException("Attachment not found");
        }
        return attachment;
    }

    private String requireTaskFileKey(String fileKey) {
        String safeKey;
        try {
            safeKey = StorageKeyValidator.requireSafe(fileKey);
        } catch (IllegalArgumentException ex) {
            throw new AppException(ex.getMessage());
        }
        if (!safeKey.startsWith("tasks/")) {
            throw new AppException("Task attachments must use the tasks storage namespace");
        }
        return safeKey;
    }

    private void requireSupportedFileName(String fileName) {
        String extension = fileName == null || fileName.lastIndexOf('.') < 0
                ? ""
                : fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new AppException("Unsupported task attachment file type");
        }
    }

    private Task loadTask(UUID taskId) {
        return taskRepository.findByIdAndArchivedAtIsNull(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
    }
}
