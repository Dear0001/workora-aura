package com.workora.work.dto;

import com.workora.work.entity.CompletionRule;
import com.workora.work.entity.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskRequest(
        @NotBlank @Size(max = 200) String title,
        String description,
        LocalDate dueDate,
        CompletionRule completionRule,
        TaskPriority priority
) {
    public TaskRequest(String title, String description, LocalDate dueDate, CompletionRule completionRule) {
        this(title, description, dueDate, completionRule, null);
    }
}
