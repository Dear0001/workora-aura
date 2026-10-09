package com.workora.work.dto;

import com.workora.work.entity.CompletionRule;
import com.workora.work.entity.TaskStatus;

import java.util.List;
import java.util.UUID;

public record TaskProgressResponse(
        UUID taskId,
        TaskStatus aggregateStatus,
        CompletionRule completionRule,
        List<AssigneeProgress> assignees
) {
}
