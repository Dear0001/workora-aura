package com.workora.work.service;

import com.workora.common.storage.StorageObjectService;
import com.workora.work.dto.TaskAttachmentRequest;
import com.workora.work.dto.TaskAttachmentUploadRequest;
import com.workora.work.entity.Task;
import com.workora.work.entity.TaskAttachment;
import com.workora.work.exception.AppException;
import com.workora.work.exception.ForbiddenException;
import com.workora.work.mapper.WorkMapper;
import com.workora.work.repository.TaskAttachmentRepository;
import com.workora.work.repository.TaskRepository;
import com.workora.work.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskAttachmentServiceTest {

    @Mock
    private TaskAttachmentRepository attachmentRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private WorkAuthorization authorization;
    @Mock
    private StorageObjectService storage;

    private TaskAttachmentService service;
    private final UUID taskId = UUID.randomUUID();
    private final CurrentUser caller = new CurrentUser(UUID.randomUUID(), "dev@workora.test");

    @BeforeEach
    void setUp() {
        service = new TaskAttachmentService(
                attachmentRepository, taskRepository, authorization, new WorkMapper(), storage);
    }

    @Test
    void add_rejectsUnsafeStorageKey() {
        Task task = new Task(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Draft", UUID.randomUUID());
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.add(taskId, caller, new TaskAttachmentRequest("../insecure.txt", "file.txt", "text/plain", "n/a")))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Storage key");
    }

    @Test
    void addRejectsFilesOutsideTaskNamespace() {
        Task task = new Task(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Draft", UUID.randomUUID());
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.add(
                taskId, caller, new TaskAttachmentRequest("companies/logo.png", "logo.png", "image/png", null)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("tasks storage namespace");
    }

    @Test
    void createUploadRejectsUnsupportedFileType() {
        Task task = new Task(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Draft", UUID.randomUUID());
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.createUpload(
                taskId, caller, new TaskAttachmentUploadRequest("malware.exe", "application/octet-stream")))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Unsupported task attachment file type");
        verifyNoInteractions(storage);
    }

    @Test
    void addRejectsStoredObjectsOver25Mb() {
        Task task = new Task(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Draft", UUID.randomUUID());
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(storage.inspect("tasks/large.pdf")).thenReturn(
                new StorageObjectService.StoredObjectMetadata(25L * 1024 * 1024 + 1, "application/pdf"));

        assertThatThrownBy(() -> service.add(
                taskId, caller, new TaskAttachmentRequest("tasks/large.pdf", "large.pdf", "application/pdf", null)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("25 MB or smaller");
        verifyNoInteractions(attachmentRepository);
    }

    @Test
    void delete_rejectsNonUploaderAndNonManager() {
        Task task = new Task(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Draft", UUID.randomUUID());
        UUID attachmentId = UUID.randomUUID();
        TaskAttachment attachment = new TaskAttachment(taskId, "team/notes.txt", "notes.txt", "text/plain", "draft", UUID.randomUUID());
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
        when(authorization.canManage(task, caller)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(taskId, attachmentId, caller))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("delete this attachment");
        verifyNoInteractions(storage);
    }

    @Test
    void downloadUrlRequiresTaskViewAuthorization() {
        Task task = new Task(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Draft", UUID.randomUUID());
        UUID attachmentId = UUID.randomUUID();
        TaskAttachment attachment = new TaskAttachment(taskId, "tasks/notes.txt", "notes.txt", "text/plain", null, caller.userId());
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
        when(storage.createDownload("tasks/notes.txt")).thenReturn("https://storage.test/download");

        assertThat(service.downloadUrl(taskId, attachmentId, caller))
                .isEqualTo("https://storage.test/download");
        verify(authorization).requireCanViewTask(task, caller);
    }

    @Test
    void downloadUrlDoesNotIssueUrlWhenTaskViewIsDenied() {
        Task task = new Task(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Draft", UUID.randomUUID());
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doThrow(new ForbiddenException("Task is not visible"))
                .when(authorization).requireCanViewTask(task, caller);

        assertThatThrownBy(() -> service.downloadUrl(taskId, UUID.randomUUID(), caller))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void createUploadRequiresTaskViewAuthorization() {
        Task task = new Task(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Draft", UUID.randomUUID());
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(storage.createUpload("tasks", "notes.txt", "text/plain"))
                .thenReturn(new StorageObjectService.PresignedFileUpload(
                        "tasks/generated.txt", "https://storage.test/upload", java.time.Instant.now(), "text/plain"));

        var result = service.createUpload(
                taskId, caller, new TaskAttachmentUploadRequest("notes.txt", "text/plain"));

        assertThat(result.key()).isEqualTo("tasks/generated.txt");
        verify(authorization).requireCanViewTask(task, caller);
    }

    @Test
    void createUploadDoesNotIssueUrlWhenTaskViewIsDenied() {
        Task task = new Task(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Draft", UUID.randomUUID());
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doThrow(new ForbiddenException("Task is not visible"))
                .when(authorization).requireCanViewTask(task, caller);

        assertThatThrownBy(() -> service.createUpload(
                taskId, caller, new TaskAttachmentUploadRequest("notes.txt", "text/plain")))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void deleteRemovesStoredObjectOnlyAfterManagerAuthorization() {
        Task task = new Task(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Draft", UUID.randomUUID());
        UUID attachmentId = UUID.randomUUID();
        TaskAttachment attachment = new TaskAttachment(taskId, "tasks/notes.txt", "notes.txt", "text/plain", null, UUID.randomUUID());
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
        when(authorization.canManage(task, caller)).thenReturn(true);

        service.delete(taskId, attachmentId, caller);

        verify(storage).delete("tasks/notes.txt");
        verify(attachmentRepository).delete(attachment);
    }
}
