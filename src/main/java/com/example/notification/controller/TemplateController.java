package com.example.notification.controller;

import com.example.notification.common.NotificationChannel;
import com.example.notification.dto.CreateTemplateRequest;
import com.example.notification.dto.TemplateResponse;
import com.example.notification.dto.UpdateTemplateRequest;
import com.example.notification.security.ClientPrincipal;
import com.example.notification.template.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Tag(name = "Templates")
@SecurityRequirement(name = "bearerAuth")
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a notification template")
    public TemplateResponse create(
            @AuthenticationPrincipal ClientPrincipal principal,
            @Valid @RequestBody CreateTemplateRequest request) {
        return templateService.create(principal.getClientId(), request);
    }

    @GetMapping("/{templateCode}")
    @Operation(summary = "Get a template by code and channel")
    public TemplateResponse get(
            @AuthenticationPrincipal ClientPrincipal principal,
            @PathVariable String templateCode,
            @RequestParam NotificationChannel channel) {
        return templateService.get(principal.getClientId(), templateCode, channel);
    }

    @GetMapping
    @Operation(summary = "List templates for the authenticated client")
    public List<TemplateResponse> list(@AuthenticationPrincipal ClientPrincipal principal) {
        return templateService.list(principal.getClientId());
    }

    @PutMapping("/{templateCode}")
    @Operation(summary = "Update a template")
    public TemplateResponse update(
            @AuthenticationPrincipal ClientPrincipal principal,
            @PathVariable String templateCode,
            @RequestParam NotificationChannel channel,
            @Valid @RequestBody UpdateTemplateRequest request) {
        return templateService.update(principal.getClientId(), templateCode, channel, request);
    }

    @DeleteMapping("/{templateCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a template")
    public void delete(
            @AuthenticationPrincipal ClientPrincipal principal,
            @PathVariable String templateCode,
            @RequestParam NotificationChannel channel) {
        templateService.delete(principal.getClientId(), templateCode, channel);
    }
}
