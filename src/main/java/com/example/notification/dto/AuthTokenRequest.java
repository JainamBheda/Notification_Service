package com.example.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthTokenRequest {

    @NotBlank
    private String clientId;

    @NotBlank
    private String clientSecret;
}
