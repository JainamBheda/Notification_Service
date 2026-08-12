package com.example.notification.dto;

import com.example.notification.common.NotificationChannel;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TemplateResponse {
    String templateCode;
    NotificationChannel channel;
    String subject;
    String body;
    Instant createdAt;
    Instant updatedAt;
}
