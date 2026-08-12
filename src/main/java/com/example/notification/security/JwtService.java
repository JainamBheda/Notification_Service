package com.example.notification.security;

import com.example.notification.config.NotificationProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    private final NotificationProperties properties;
    private final SecretKey key;

    public JwtService(NotificationProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(
                properties.getSecurity().getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String clientId, List<String> roles) {
        long now = System.currentTimeMillis();
        long expiresIn = properties.getSecurity().getJwtExpirationMs();
        return Jwts.builder()
                .subject(clientId)
                .claim("roles", roles)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiresIn))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationMs() {
        return properties.getSecurity().getJwtExpirationMs();
    }
}
