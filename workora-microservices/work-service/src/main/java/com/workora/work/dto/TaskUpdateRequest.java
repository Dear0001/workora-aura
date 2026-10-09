package com.workora.work.dto;

import com.workora.work.entity.CompletionRule;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskUpdateRequest(
        @Size(max = 200) String title,
        String description,
        LocalDate dueDate,
        CompletionRule completionRule
) {
}
