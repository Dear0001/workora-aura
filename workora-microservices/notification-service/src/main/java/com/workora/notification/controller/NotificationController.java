package com.workora.notification.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.common.dto.PageResponse;
import com.workora.notification.dto.NotificationCreateRequest;
import com.workora.notification.dto.NotificationResponse;
import com.workora.notification.dto.UnreadCountResponse;
import com.workora.notification.dto.TestEmailRequest;
import com.workora.notification.security.CurrentUser;
import com.workora.notification.service.NotificationService;
import com.workora.notification.service.EmailDeliveryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;
    private final EmailDeliveryService emailDeliveryService;

    public NotificationController(NotificationService notificationService, EmailDeliveryService emailDeliveryService) {
        this.notificationService = notificationService;
        this.emailDeliveryService = emailDeliveryService;
    }

    @PostMapping("/notifications/test-email")
    public ResponseEntity<ApiResponse<Void>> testEmail(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody(required = false) TestEmailRequest request) {
        CurrentUser user = CurrentUser.from(jwt);
        TestEmailRequest value = request == null ? new TestEmailRequest(null, null) : request;
        emailDeliveryService.sendTestEmail(user.email(), value.subject(), value.body());
        return ResponseEntity.ok(ApiResponse.success("Test email sent", null));
    }

    @PostMapping("/notifications")
    public ResponseEntity<ApiResponse<NotificationResponse>> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Internal-Service-Token", required = false) String internalServiceToken,
            @Valid @RequestBody NotificationCreateRequest request) {
        NotificationResponse created = notificationService.create(CurrentUser.from(jwt), request, internalServiceToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Notification created", created));
    }

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> inbox(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.inbox(CurrentUser.from(jwt), unreadOnly, pageable)));
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.unreadCount(CurrentUser.from(jwt))));
    }

    @GetMapping("/notifications/{notificationId}")
    public ResponseEntity<ApiResponse<NotificationResponse>> get(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.get(notificationId, CurrentUser.from(jwt))));
    }

    @PatchMapping("/notifications/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read",
                notificationService.markRead(notificationId, CurrentUser.from(jwt))));
    }

    @PatchMapping("/notifications/{notificationId}/archive")
    public ResponseEntity<ApiResponse<NotificationResponse>> archive(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Notification archived",
                notificationService.archive(notificationId, CurrentUser.from(jwt))));
    }

    @DeleteMapping("/notifications/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal Jwt jwt) {
        notificationService.delete(notificationId, CurrentUser.from(jwt));
        return ResponseEntity.ok(ApiResponse.success("Notification deleted", null));
    }
}
