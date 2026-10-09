package com.workora.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminAccessRequestCreate(
        @NotBlank @Email @Size(max = 320) String email,
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName) {
}
