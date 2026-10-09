package com.workora.project.dto;

import jakarta.validation.constraints.Size;

public record JobApplicationRequest(
        @Size(max = 2000, message = "Proposal must be at most 2000 characters")
        String proposal
) {
}
