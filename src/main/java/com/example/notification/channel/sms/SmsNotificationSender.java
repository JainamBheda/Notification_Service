package com.example.notification.channel.sms;

import com.example.notification.channel.NotificationSender;
import com.example.notification.common.NotificationChannel;
import com.example.notification.config.NotificationProperties;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
public class SmsNotificationSender implements NotificationSender {

    private final NotificationProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public SendResult send(SendRequest request) {
        if ("logging".equalsIgnoreCase(properties.getProviders().getSms())) {
            log.info("SMS (logging) to={} body={} notificationId={}",
                    request.recipient(), request.body(), request.notificationId());
            return SendResult.ok("logged");
        }
        try {
            String accountSid = properties.getTwilio().getAccountSid();
            String authToken = properties.getTwilio().getAuthToken();
            String from = properties.getTwilio().getFromNumber();
            if (isBlank(accountSid) || isBlank(authToken) || isBlank(from)) {
                return SendResult.failed("Twilio credentials are not configured");
            }
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";
            String body = "To=" + encode(request.recipient())
                    + "&From=" + encode(from)
                    + "&Body=" + encode(request.body());
            String basic = Base64.getEncoder().encodeToString(
                    (accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
            RequestEntity<String> entity = RequestEntity.post(URI.create(url))
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body);
            ResponseEntity<String> response = restTemplate.exchange(entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return SendResult.ok("twilio-accepted");
            }
            return SendResult.failed("Twilio response: " + response.getStatusCode());
        } catch (Exception e) {
            log.error("SMS send failed for notificationId={}", request.notificationId(), e);
            return SendResult.failed(e.getMessage());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
