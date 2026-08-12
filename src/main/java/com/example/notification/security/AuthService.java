package com.example.notification.security;

import com.example.notification.dto.AuthTokenRequest;
import com.example.notification.dto.AuthTokenResponse;
import com.example.notification.entity.ClientEntity;
import com.example.notification.exception.ApiException;
import com.example.notification.repository.ClientRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthTokenResponse authenticate(AuthTokenRequest request) {
        ClientEntity client = clientRepository
                .findByClientIdAndStatus(request.getClientId(), "ACTIVE")
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "Invalid client credentials"));

        if (!passwordEncoder.matches(request.getClientSecret(), client.getClientSecretHash())) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "Invalid client credentials");
        }

        String token = jwtService.generateToken(client.getClientId(), List.of("CLIENT"));
        return AuthTokenResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs() / 1000)
                .build();
    }
}
