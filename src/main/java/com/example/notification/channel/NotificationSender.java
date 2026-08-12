package com.example.notification.channel;

import com.example.notification.common.NotificationChannel;
import java.util.Map;

public interface NotificationSender {

    NotificationChannel channel();

    SendResult send(SendRequest request);

    record SendRequest(
            String notificationId,
            String clientId,
            String recipient,
            String subject,
            String body,
            Map<String, Object> data) {
    }

    record SendResult(boolean success, String providerResponse, String errorMessage) {
        public static SendResult ok(String providerResponse) {
            return new SendResult(true, providerResponse, null);
        }

        public static SendResult failed(String errorMessage) {
            return new SendResult(false, null, errorMessage);
        }
    }
}
