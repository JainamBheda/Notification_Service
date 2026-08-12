package com.example.notification.channel.push;

import com.example.notification.channel.NotificationSender;
import com.example.notification.common.NotificationChannel;
import com.example.notification.config.NotificationProperties;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushNotificationSender implements NotificationSender {

    private final NotificationProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public SendResult send(SendRequest request) {
        if ("logging".equalsIgnoreCase(properties.getProviders().getPush())) {
            log.info("PUSH (logging) to={} title={} body={} notificationId={}",
                    request.recipient(), request.subject(), request.body(), request.notificationId());
            return SendResult.ok("logged");
        }
        try {
            String serverKey = properties.getFcm().getServerKey();
            if (serverKey == null || serverKey.isBlank()) {
                return SendResult.failed("FCM server key is not configured");
            }
            Map<String, Object> notification = new HashMap<>();
            notification.put("title", request.subject() == null ? "Notification" : request.subject());
            notification.put("body", request.body());

            Map<String, Object> payload = new HashMap<>();
            payload.put("to", request.recipient());
            payload.put("notification", notification);
            if (request.data() != null) {
                payload.put("data", request.data());
            }

            RequestEntity<Map<String, Object>> entity = RequestEntity
                    .post(URI.create("https://fcm.googleapis.com/fcm/send"))
                    .header(HttpHeaders.AUTHORIZATION, "key=" + serverKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload);
            ResponseEntity<String> response = restTemplate.exchange(entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return SendResult.ok("fcm-accepted");
            }
            return SendResult.failed("FCM response: " + response.getStatusCode());
        } catch (Exception e) {
            log.error("Push send failed for notificationId={}", request.notificationId(), e);
            return SendResult.failed(e.getMessage());
        }
    }
}
