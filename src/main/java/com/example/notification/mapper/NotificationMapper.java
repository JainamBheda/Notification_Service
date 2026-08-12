package com.example.notification.mapper;

import com.example.notification.dto.NotificationResponse;
import com.example.notification.dto.NotificationStatusResponse;
import com.example.notification.dto.TemplateResponse;
import com.example.notification.entity.NotificationEntity;
import com.example.notification.entity.NotificationTemplateEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationMapper {

    private final ObjectMapper objectMapper;

    public TemplateResponse toTemplateResponse(NotificationTemplateEntity entity) {
        return TemplateResponse.builder()
                .templateCode(entity.getTemplateCode())
                .channel(entity.getChannel())
                .subject(entity.getSubject())
                .body(entity.getBody())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public NotificationResponse toNotificationResponse(NotificationEntity entity) {
        return NotificationResponse.builder()
                .notificationId(entity.getNotificationId())
                .recipient(entity.getRecipient())
                .channel(entity.getChannel())
                .templateCode(entity.getTemplateCode())
                .priority(entity.getPriority())
                .status(entity.getStatus())
                .retryCount(entity.getRetryCount())
                .failureReason(entity.getFailureReason())
                .data(parsePayload(entity.getPayload()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .sentAt(entity.getSentAt())
                .build();
    }

    public NotificationStatusResponse toStatusResponse(NotificationEntity entity) {
        return NotificationStatusResponse.builder()
                .notificationId(entity.getNotificationId())
                .status(entity.getStatus())
                .retryCount(entity.getRetryCount())
                .failureReason(entity.getFailureReason())
                .updatedAt(entity.getUpdatedAt())
                .sentAt(entity.getSentAt())
                .build();
    }

    public String toPayloadJson(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to serialize notification data", e);
        }
    }

    public Map<String, Object> parsePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
