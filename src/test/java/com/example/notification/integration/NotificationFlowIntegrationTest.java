package com.example.notification.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.notification.common.NotificationChannel;
import com.example.notification.common.NotificationPriority;
import com.example.notification.common.NotificationStatus;
import com.example.notification.dto.AuthTokenRequest;
import com.example.notification.dto.AuthTokenResponse;
import com.example.notification.dto.CreateNotificationRequest;
import com.example.notification.dto.CreateNotificationResponse;
import com.example.notification.dto.NotificationStatusResponse;
import com.example.notification.repository.NotificationRepository;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class NotificationFlowIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("notification_db")
            .withUsername("notification")
            .withPassword("notification");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("notification.security.jwt-secret",
                () -> "integration-test-secret-key-32chars-min!!");
        registry.add("notification.providers.email", () -> "logging");
        registry.add("notification.providers.sms", () -> "logging");
        registry.add("notification.providers.push", () -> "logging");
        registry.add("notification.reconciliation.enabled", () -> "false");
        registry.add("notification.rate-limit.enabled", () -> "false");
        registry.add("spring.mail.host", () -> "localhost");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    private String token;

    @BeforeEach
    void authenticate() {
        AuthTokenRequest request = new AuthTokenRequest();
        request.setClientId("demo-client");
        request.setClientSecret("demo-secret");
        ResponseEntity<AuthTokenResponse> response =
                restTemplate.postForEntity("/api/v1/auth/token", request, AuthTokenResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        token = response.getBody().getAccessToken();
    }

    @Test
    void createsAndDeliversNotification() {
        CreateNotificationRequest body = new CreateNotificationRequest();
        body.setRecipient("user@example.com");
        body.setChannel(NotificationChannel.EMAIL);
        body.setTemplateCode("WELCOME_USER");
        body.setPriority(NotificationPriority.HIGH);
        body.setData(Map.of("name", "Jainam"));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", "integration-key-1");

        ResponseEntity<CreateNotificationResponse> created = restTemplate.exchange(
                "/api/v1/notifications",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                CreateNotificationResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(created.getBody()).isNotNull();
        String notificationId = created.getBody().getNotificationId();

        await().atMost(Duration.ofSeconds(45)).untilAsserted(() -> {
            ResponseEntity<NotificationStatusResponse> status = restTemplate.exchange(
                    "/api/v1/notifications/" + notificationId + "/status",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    NotificationStatusResponse.class);
            assertThat(status.getBody()).isNotNull();
            assertThat(status.getBody().getStatus()).isEqualTo(NotificationStatus.SENT);
        });

        assertThat(notificationRepository.findByNotificationId(notificationId)).isPresent();
    }
}
