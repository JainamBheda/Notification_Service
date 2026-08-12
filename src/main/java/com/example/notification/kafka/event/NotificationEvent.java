package com.example.notification.kafka.event;

import com.example.notification.common.NotificationChannel;
import com.example.notification.common.NotificationPriority;
import com.example.notification.common.NotificationStatus;
import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String eventId;
    private String notificationId;
    private String clientId;
    private NotificationChannel channel;
    private String templateCode;
    private String recipient;
    private NotificationPriority priority;
    private Map<String, Object> data;
    private String correlationId;
    private int retryCount;
    private Instant nextAttemptAt;
}
