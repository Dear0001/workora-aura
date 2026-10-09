package com.workora.identity.dto;

import com.workora.identity.entity.AccountStatus;

import java.time.LocalDate;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String gender,
        LocalDate dateOfBirth,
        String avatarUrl,
        String bio,
        String experience,
        String role,
        boolean emailVerified,
        AccountStatus accountStatus
) {
}
