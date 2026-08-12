package com.example.notification.controller;

import com.example.notification.common.AppConstants;
import com.example.notification.dto.CreateNotificationRequest;
import com.example.notification.dto.CreateNotificationResponse;
import com.example.notification.dto.NotificationResponse;
import com.example.notification.dto.NotificationStatusResponse;
import com.example.notification.security.ClientPrincipal;
import com.example.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Queue a notification for asynchronous delivery")
    public CreateNotificationResponse create(
            @AuthenticationPrincipal ClientPrincipal principal,
            @Valid @RequestBody CreateNotificationRequest request,
            @RequestHeader(value = AppConstants.IDEMPOTENCY_KEY_HEADER, required = false)
                    String idempotencyKey) {
        return notificationService.create(principal.getClientId(), request, idempotencyKey);
    }

    @GetMapping("/{notificationId}")
    @Operation(summary = "Get notification details")
    public NotificationResponse get(
            @AuthenticationPrincipal ClientPrincipal principal,
            @PathVariable String notificationId) {
        return notificationService.get(principal.getClientId(), notificationId);
    }

    @GetMapping
    @Operation(summary = "List notifications for the authenticated client")
    public Page<NotificationResponse> list(
            @AuthenticationPrincipal ClientPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return notificationService.list(principal.getClientId(), pageable);
    }

    @GetMapping("/{notificationId}/status")
    @Operation(summary = "Get notification status")
    public NotificationStatusResponse status(
            @AuthenticationPrincipal ClientPrincipal principal,
            @PathVariable String notificationId) {
        return notificationService.getStatus(principal.getClientId(), notificationId);
    }
}
