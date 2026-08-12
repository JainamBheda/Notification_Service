package com.example.notification.dto;

import com.example.notification.common.NotificationStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CreateNotificationResponse {
    String notificationId;
    NotificationStatus status;
}
