package com.example.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.notification.common.NotificationChannel;
import com.example.notification.common.NotificationPriority;
import com.example.notification.common.NotificationStatus;
import com.example.notification.dto.CreateNotificationRequest;
import com.example.notification.dto.CreateNotificationResponse;
import com.example.notification.entity.NotificationEntity;
import com.example.notification.entity.NotificationTemplateEntity;
import com.example.notification.kafka.NotificationEventPublisher;
import com.example.notification.mapper.NotificationMapper;
import com.example.notification.observability.NotificationMetrics;
import com.example.notification.repository.NotificationRepository;
import com.example.notification.template.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private TemplateService templateService;
    @Mock
    private NotificationEventPublisher eventPublisher;
    @Mock
    private NotificationMetrics metrics;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                templateService,
                new NotificationMapper(new ObjectMapper()),
                eventPublisher,
                metrics);
    }

    @Test
    void createsAndPublishesNotification() {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setRecipient("user@example.com");
        request.setChannel(NotificationChannel.EMAIL);
        request.setTemplateCode("WELCOME_USER");
        request.setPriority(NotificationPriority.HIGH);
        request.setData(Map.of("name", "Jainam"));

        when(templateService.requireTemplate("demo-client", "WELCOME_USER", NotificationChannel.EMAIL))
                .thenReturn(NotificationTemplateEntity.builder()
                        .templateCode("WELCOME_USER")
                        .channel(NotificationChannel.EMAIL)
                        .body("Hello {{name}}")
                        .build());
        when(notificationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateNotificationResponse response =
                notificationService.create("demo-client", request, "key-1");

        assertThat(response.getStatus()).isEqualTo(NotificationStatus.QUEUED);
        assertThat(response.getNotificationId()).isNotBlank();
        verify(eventPublisher).publishRequested(any());
        verify(metrics).incrementQueued("EMAIL");
    }

    @Test
    void returnsExistingOnIdempotentReplay() {
        NotificationEntity existing = NotificationEntity.builder()
                .notificationId("existing-id")
                .status(NotificationStatus.QUEUED)
                .build();
        when(notificationRepository.findByClientIdAndIdempotencyKey("demo-client", "abc123"))
                .thenReturn(Optional.of(existing));

        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setRecipient("user@example.com");
        request.setChannel(NotificationChannel.EMAIL);
        request.setTemplateCode("WELCOME_USER");

        CreateNotificationResponse response =
                notificationService.create("demo-client", request, "abc123");

        assertThat(response.getNotificationId()).isEqualTo("existing-id");
        verify(eventPublisher, never()).publishRequested(any());
    }
}
