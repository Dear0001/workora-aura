package com.workora.work.mapper;

import com.workora.work.dto.AssigneeResponse;
import com.workora.work.dto.SubmissionResponse;
import com.workora.work.dto.TaskAttachmentResponse;
import com.workora.work.dto.TaskResponse;
import com.workora.work.entity.Submission;
import com.workora.work.entity.Task;
import com.workora.work.entity.TaskAssignee;
import com.workora.work.entity.TaskAttachment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorkMapper {

    public TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getPhaseId(),
                task.getProjectId(),
                task.getDepartmentId(),
                task.getCompanyId(),
                task.getProjectManagerId(),
                task.getTitle(),
                task.getDescription(),
                task.getDueDate(),
                task.getStatus(),
                task.getCompletionRule(),
                task.getCreatedBy(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    public AssigneeResponse toAssigneeResponse(TaskAssignee assignee) {
        return new AssigneeResponse(
                assignee.getId(),
                assignee.getTaskId(),
                assignee.getUserId(),
                assignee.getDepartmentId(),
                assignee.getStatus(),
                assignee.getAssignedBy(),
                assignee.getAssignedAt(),
                assignee.getRemovedAt()
        );
    }

    public SubmissionResponse toSubmissionResponse(Submission submission) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getTaskId(),
                submission.getAuthorId(),
                submission.getVersion(),
                submission.getNote(),
                List.copyOf(submission.getAttachmentKeys()),
                submission.getSubmittedAt()
        );
    }

    public TaskAttachmentResponse toAttachmentResponse(TaskAttachment attachment) {
        return new TaskAttachmentResponse(
                attachment.getId(),
                attachment.getTaskId(),
                attachment.getFileKey(),
                attachment.getFileName(),
                attachment.getMimeType(),
                attachment.getDescription(),
                attachment.getUploadedBy(),
                attachment.getUploadedAt()
        );
    }
}
