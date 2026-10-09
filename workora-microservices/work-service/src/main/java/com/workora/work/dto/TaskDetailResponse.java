package com.workora.work.dto;

import java.util.List;

public record TaskDetailResponse(
        TaskResponse task,
        List<AssigneeResponse> assignees,
        TaskProgressResponse progress
) {
}
