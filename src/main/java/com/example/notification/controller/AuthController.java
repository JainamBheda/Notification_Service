package com.example.notification.controller;

import com.example.notification.dto.AuthTokenRequest;
import com.example.notification.dto.AuthTokenResponse;
import com.example.notification.security.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/token")
    @Operation(summary = "Exchange client credentials for a JWT")
    public AuthTokenResponse token(@Valid @RequestBody AuthTokenRequest request) {
        return authService.authenticate(request);
    }
}
