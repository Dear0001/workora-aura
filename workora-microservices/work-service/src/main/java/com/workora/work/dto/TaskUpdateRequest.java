package com.workora.work.dto;

import com.workora.work.entity.CompletionRule;
import com.workora.work.entity.TaskPriority;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskUpdateRequest(
        @Size(max = 200) String title,
        String description,
        LocalDate dueDate,
        CompletionRule completionRule,
        TaskPriority priority
) {
    public TaskUpdateRequest(String title, String description, LocalDate dueDate, CompletionRule completionRule) {
        this(title, description, dueDate, completionRule, null);
    }
}
