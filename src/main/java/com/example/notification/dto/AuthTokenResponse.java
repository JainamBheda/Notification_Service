package com.example.notification.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthTokenResponse {
    String accessToken;
    String tokenType;
    long expiresIn;
}
