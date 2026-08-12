package com.example.notification.dto;

import com.example.notification.common.NotificationStatus;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NotificationStatusResponse {
    String notificationId;
    NotificationStatus status;
    int retryCount;
    String failureReason;
    Instant updatedAt;
    Instant sentAt;
}
