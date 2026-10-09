package com.workora.organization.dto;

import com.workora.organization.entity.CompanyRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InvitationRequest(
        @NotBlank(message = "Invitee email is required")
        @Email(message = "Invitee email must be valid")
        String email,

        @NotNull(message = "Role is required")
        CompanyRole role,

        UUID departmentId
) {
}
