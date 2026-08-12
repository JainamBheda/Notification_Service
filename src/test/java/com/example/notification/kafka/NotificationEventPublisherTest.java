package com.example.notification.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.notification.common.NotificationChannel;
import com.example.notification.common.NotificationPriority;
import com.example.notification.config.NotificationProperties;
import com.example.notification.kafka.event.NotificationEvent;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class NotificationEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private NotificationEventPublisher publisher;

    @BeforeEach
    void setUp() {
        NotificationProperties properties = new NotificationProperties();
        publisher = new NotificationEventPublisher(kafkaTemplate, properties);
        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void publishesNormalPriorityToRequestedTopic() {
        NotificationEvent event = NotificationEvent.builder()
                .notificationId("n1")
                .priority(NotificationPriority.NORMAL)
                .channel(NotificationChannel.EMAIL)
                .build();
        publisher.publishRequested(event);
        verify(kafkaTemplate).send(eq("notification.requested"), eq("n1"), eq(event));
    }

    @Test
    void publishesHighPriorityToPriorityTopic() {
        NotificationEvent event = NotificationEvent.builder()
                .notificationId("n2")
                .priority(NotificationPriority.CRITICAL)
                .channel(NotificationChannel.SMS)
                .build();
        publisher.publishRequested(event);
        verify(kafkaTemplate).send(eq("notification.requested.priority"), eq("n2"), eq(event));
    }
}
