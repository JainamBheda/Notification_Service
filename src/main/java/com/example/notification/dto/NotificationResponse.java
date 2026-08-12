package com.example.notification.dto;

import com.example.notification.common.NotificationChannel;
import com.example.notification.common.NotificationPriority;
import com.example.notification.common.NotificationStatus;
import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NotificationResponse {
    String notificationId;
    String recipient;
    NotificationChannel channel;
    String templateCode;
    NotificationPriority priority;
    NotificationStatus status;
    int retryCount;
    String failureReason;
    Map<String, Object> data;
    Instant createdAt;
    Instant updatedAt;
    Instant sentAt;
}
