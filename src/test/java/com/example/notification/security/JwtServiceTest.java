package com.example.notification.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.notification.config.NotificationProperties;
import io.jsonwebtoken.Claims;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        NotificationProperties properties = new NotificationProperties();
        properties.getSecurity().setJwtSecret("test-secret-key-must-be-at-least-32-chars!!");
        properties.getSecurity().setJwtExpirationMs(3600000);
        jwtService = new JwtService(properties);
    }

    @Test
    void generatesAndParsesToken() {
        String token = jwtService.generateToken("demo-client", List.of("CLIENT"));
        Claims claims = jwtService.parse(token);
        assertThat(claims.getSubject()).isEqualTo("demo-client");
        assertThat(claims.get("roles", List.class)).contains("CLIENT");
    }
}
