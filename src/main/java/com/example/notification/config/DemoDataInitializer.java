package com.example.notification.config;

import com.example.notification.common.NotificationChannel;
import com.example.notification.entity.ClientEntity;
import com.example.notification.entity.NotificationTemplateEntity;
import com.example.notification.repository.ClientRepository;
import com.example.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures a local demo client exists. Secret is taken from DEMO_CLIENT_SECRET (default: demo-secret).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    private final ClientRepository clientRepository;
    private final NotificationTemplateRepository templateRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String clientId = "demo-client";
        String secret = System.getenv().getOrDefault("DEMO_CLIENT_SECRET", "demo-secret");

        ClientEntity client = clientRepository.findByClientIdAndStatus(clientId, "ACTIVE")
                .orElseGet(() -> {
                    ClientEntity created = ClientEntity.builder()
                            .clientId(clientId)
                            .clientSecretHash(passwordEncoder.encode(secret))
                            .name("Demo Product Client")
                            .status("ACTIVE")
                            .build();
                    log.info("Seeded demo client_id={}", clientId);
                    return clientRepository.save(created);
                });

        // Keep hash in sync when using known local secret and existing row from Flyway seed
        if (!passwordEncoder.matches(secret, client.getClientSecretHash())) {
            client.setClientSecretHash(passwordEncoder.encode(secret));
            clientRepository.save(client);
            log.info("Updated demo client secret hash for client_id={}", clientId);
        }

        ensureTemplate(clientId, NotificationChannel.EMAIL, "Welcome {{name}}",
                "Hello {{name}}, welcome to our platform.");
        ensureTemplate(clientId, NotificationChannel.SMS, null,
                "Hello {{name}}, welcome to our platform.");
        ensureTemplate(clientId, NotificationChannel.PUSH, "Welcome {{name}}",
                "Hello {{name}}, welcome to our platform.");
    }

    private void ensureTemplate(String clientId, NotificationChannel channel, String subject, String body) {
        if (!templateRepository.existsByClientIdAndTemplateCodeAndChannel(clientId, "WELCOME_USER", channel)) {
            templateRepository.save(NotificationTemplateEntity.builder()
                    .clientId(clientId)
                    .templateCode("WELCOME_USER")
                    .channel(channel)
                    .subject(subject)
                    .body(body)
                    .build());
        }
    }
}
