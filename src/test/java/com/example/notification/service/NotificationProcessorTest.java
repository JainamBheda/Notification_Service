package com.example.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.notification.channel.NotificationSender;
import com.example.notification.channel.NotificationSenderRegistry;
import com.example.notification.common.NotificationChannel;
import com.example.notification.common.NotificationPriority;
import com.example.notification.common.NotificationStatus;
import com.example.notification.config.NotificationProperties;
import com.example.notification.entity.NotificationEntity;
import com.example.notification.entity.NotificationTemplateEntity;
import com.example.notification.kafka.NotificationEventPublisher;
import com.example.notification.kafka.event.NotificationEvent;
import com.example.notification.observability.NotificationMetrics;
import com.example.notification.repository.NotificationDeliveryAttemptRepository;
import com.example.notification.repository.NotificationRepository;
import com.example.notification.template.TemplateResolver;
import com.example.notification.template.TemplateService;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationProcessorTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationDeliveryAttemptRepository attemptRepository;
    @Mock
    private TemplateService templateService;
    @Mock
    private NotificationSenderRegistry senderRegistry;
    @Mock
    private NotificationSender sender;
    @Mock
    private NotificationEventPublisher eventPublisher;
    @Mock
    private NotificationMetrics metrics;

    private NotificationProcessor processor;
    private NotificationProperties properties;

    @BeforeEach
    void setUp() {
        properties = new NotificationProperties();
        properties.getRetry().setMaxAttempts(3);
        properties.getRetry().setInitialIntervalMs(1000);
        properties.getRetry().setMultiplier(2.0);
        properties.getRetry().setMaxIntervalMs(60000);
        processor = new NotificationProcessor(
                notificationRepository,
                attemptRepository,
                templateService,
                new TemplateResolver(),
                senderRegistry,
                eventPublisher,
                properties,
                metrics);
    }

    @Test
    void sendsSuccessfully() {
        String id = UUID.randomUUID().toString();
        NotificationEntity entity = NotificationEntity.builder()
                .notificationId(id)
                .clientId("demo-client")
                .channel(NotificationChannel.EMAIL)
                .templateCode("WELCOME_USER")
                .recipient("user@example.com")
                .priority(NotificationPriority.NORMAL)
                .status(NotificationStatus.QUEUED)
                .retryCount(0)
                .build();
        when(notificationRepository.findByNotificationId(id)).thenReturn(Optional.of(entity));
        when(templateService.requireTemplate("demo-client", "WELCOME_USER", NotificationChannel.EMAIL))
                .thenReturn(NotificationTemplateEntity.builder()
                        .subject("Welcome {{name}}")
                        .body("Hello {{name}}")
                        .build());
        when(senderRegistry.get(NotificationChannel.EMAIL)).thenReturn(sender);
        when(sender.send(any())).thenReturn(NotificationSender.SendResult.ok("logged"));

        processor.process(NotificationEvent.builder()
                .eventId("e1")
                .notificationId(id)
                .clientId("demo-client")
                .channel(NotificationChannel.EMAIL)
                .templateCode("WELCOME_USER")
                .recipient("user@example.com")
                .priority(NotificationPriority.NORMAL)
                .data(Map.of("name", "Jainam"))
                .retryCount(0)
                .build());

        assertThat(entity.getStatus()).isEqualTo(NotificationStatus.SENT);
        verify(metrics).incrementSent("EMAIL");
    }

    @Test
    void schedulesRetryOnFailure() {
        String id = UUID.randomUUID().toString();
        NotificationEntity entity = NotificationEntity.builder()
                .notificationId(id)
                .clientId("demo-client")
                .channel(NotificationChannel.SMS)
                .templateCode("WELCOME_USER")
                .recipient("+10000000000")
                .priority(NotificationPriority.NORMAL)
                .status(NotificationStatus.QUEUED)
                .retryCount(0)
                .build();
        when(notificationRepository.findByNotificationId(id)).thenReturn(Optional.of(entity));
        when(templateService.requireTemplate(any(), any(), any()))
                .thenReturn(NotificationTemplateEntity.builder().body("Hi").build());
        when(senderRegistry.get(NotificationChannel.SMS)).thenReturn(sender);
        when(sender.send(any())).thenReturn(NotificationSender.SendResult.failed("provider down"));

        processor.process(NotificationEvent.builder()
                .eventId("e1")
                .notificationId(id)
                .clientId("demo-client")
                .channel(NotificationChannel.SMS)
                .templateCode("WELCOME_USER")
                .recipient("+10000000000")
                .priority(NotificationPriority.NORMAL)
                .retryCount(0)
                .build());

        assertThat(entity.getStatus()).isEqualTo(NotificationStatus.RETRYING);
        verify(eventPublisher).publishRetry(any());
        verify(metrics).incrementRetry("SMS");
    }

    @Test
    void movesToDlqAfterMaxRetries() {
        String id = UUID.randomUUID().toString();
        NotificationEntity entity = NotificationEntity.builder()
                .notificationId(id)
                .clientId("demo-client")
                .channel(NotificationChannel.PUSH)
                .templateCode("WELCOME_USER")
                .recipient("device-token")
                .priority(NotificationPriority.HIGH)
                .status(NotificationStatus.RETRYING)
                .retryCount(2)
                .build();
        when(notificationRepository.findByNotificationId(id)).thenReturn(Optional.of(entity));
        when(templateService.requireTemplate(any(), any(), any()))
                .thenReturn(NotificationTemplateEntity.builder().body("Hi").subject("Title").build());
        when(senderRegistry.get(NotificationChannel.PUSH)).thenReturn(sender);
        when(sender.send(any())).thenReturn(NotificationSender.SendResult.failed("still failing"));

        processor.process(NotificationEvent.builder()
                .eventId("e1")
                .notificationId(id)
                .clientId("demo-client")
                .channel(NotificationChannel.PUSH)
                .templateCode("WELCOME_USER")
                .recipient("device-token")
                .priority(NotificationPriority.HIGH)
                .retryCount(2)
                .build());

        assertThat(entity.getStatus()).isEqualTo(NotificationStatus.FAILED);
        verify(eventPublisher).publishDeadLetter(any());
        verify(metrics).incrementFailed("PUSH");
    }

    @Test
    void computesExponentialBackoff() {
        assertThat(processor.computeBackoffMs(1)).isEqualTo(1000);
        assertThat(processor.computeBackoffMs(2)).isEqualTo(2000);
        assertThat(processor.computeBackoffMs(3)).isEqualTo(4000);
    }
}
