package com.workora.project.dto;

import com.workora.project.entity.PhaseStatus;
import com.workora.project.entity.PhaseVisibility;
import jakarta.validation.constraints.Size;

public record PhaseUpdateRequest(
        @Size(max = 200, message = "Phase name must be at most 200 characters")
        String name,

        @Size(max = 4000, message = "Description must be at most 4000 characters")
        String description,

        PhaseVisibility visibility,

        PhaseStatus status
) {
}
