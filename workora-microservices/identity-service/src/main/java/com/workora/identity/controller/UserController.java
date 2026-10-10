package com.workora.identity.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.identity.dto.UserProfileResponse;
import com.workora.identity.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(Authentication authentication) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) authentication;
        return ResponseEntity.ok(ApiResponse.success(authService.getCurrentProfile(
                java.util.UUID.fromString(token.getToken().getSubject()),
                token.getToken().getClaimAsString("email")
        )));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateCurrentUser(
            Authentication authentication,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) authentication;
        AuthService.UpdateProfileCommand command = new AuthService.UpdateProfileCommand(
                request.firstName(),
                request.lastName(),
                request.gender(),
                request.dateOfBirth(),
                request.avatarUrl(),
                request.bio(),
                request.experience()
        );
        return ResponseEntity.ok(ApiResponse.success(authService.updateCurrentProfile(
                java.util.UUID.fromString(token.getToken().getSubject()),
                token.getToken().getClaimAsString("email"),
                command
        )));
    }

    public record UpdateUserRequest(
            @Size(max = 100, message = "First name must be at most 100 characters")
            String firstName,

            @Size(max = 100, message = "Last name must be at most 100 characters")
            String lastName,

            @Size(max = 32, message = "Gender must be at most 32 characters")
            String gender,

            @Past(message = "Date of birth must be in the past")
            LocalDate dateOfBirth,

            @Size(max = 1024, message = "Avatar URL must be at most 1024 characters")
            String avatarUrl,

            @Size(max = 2000, message = "Bio must be at most 2000 characters")
            String bio,

            @Size(max = 4000, message = "Experience must be at most 4000 characters")
            String experience
    ) {
    }
}
