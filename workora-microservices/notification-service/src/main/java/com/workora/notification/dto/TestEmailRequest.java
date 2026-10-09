package com.workora.notification.dto;

import jakarta.validation.constraints.Size;

public record TestEmailRequest(
        @Size(max = 200, message = "Subject must be at most 200 characters")
        String subject,
        @Size(max = 4000, message = "Body must be at most 4000 characters")
        String body
) {
}
