package com.workora.project.dto;

import jakarta.validation.constraints.Size;

public record ConfirmReceiptRequest(
        @Size(max = 1000, message = "Note must be at most 1000 characters")
        String note
) {
}
