package com.workora.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRequest(
        @NotBlank(message = "Company name is required")
        @Size(max = 200, message = "Company name must be at most 200 characters")
        String name,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @Email(message = "Contact email must be valid")
        String contactEmail,

        @Size(max = 50, message = "Contact phone must be at most 50 characters")
        String contactPhone,

        @Size(max = 500, message = "Address must be at most 500 characters")
        String address,

        String profileImageUrl,

        String coverImageUrl
) {
}
