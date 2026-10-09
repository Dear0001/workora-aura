package com.workora.project.dto;

import jakarta.validation.constraints.Size;

public record JobActionRequest(
        @Size(max = 1000, message = "Reason must be at most 1000 characters")
        String reason
) {
}
