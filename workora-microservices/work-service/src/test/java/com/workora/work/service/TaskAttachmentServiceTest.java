package com.workora.work.service;

import com.workora.work.dto.TaskAttachmentRequest;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskAttachmentServiceTest {

    @Mock
    private TaskAttachmentRepository attachmentRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private WorkAuthorization authorization;

    private TaskAttachmentService service;
    private final UUID taskId = UUID.randomUUID();
    private final CurrentUser caller = new CurrentUser(UUID.randomUUID(), "dev@workora.test");

    @BeforeEach
    void setUp() {
        service = new TaskAttachmentService(attachmentRepository, taskRepository, authorization, new WorkMapper());
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
    }
}
