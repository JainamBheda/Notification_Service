package com.example.notification.kafka.event;

import com.example.notification.common.NotificationStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatusEvent {
    private String eventId;
    private String notificationId;
    private String clientId;
    private NotificationStatus status;
    private String failureReason;
    private int retryCount;
    private String correlationId;
    private Instant occurredAt;
}
