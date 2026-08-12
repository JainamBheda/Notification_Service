package com.example.notification.channel.email;

import com.example.notification.channel.NotificationSender;
import com.example.notification.common.NotificationChannel;
import com.example.notification.config.NotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public SendResult send(SendRequest request) {
        if ("logging".equalsIgnoreCase(properties.getProviders().getEmail())) {
            log.info("EMAIL (logging) to={} subject={} notificationId={}",
                    request.recipient(), request.subject(), request.notificationId());
            return SendResult.ok("logged");
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(request.recipient());
            message.setSubject(request.subject() == null ? "Notification" : request.subject());
            message.setText(request.body());
            mailSender.send(message);
            return SendResult.ok("smtp-accepted");
        } catch (Exception e) {
            log.error("Email send failed for notificationId={}", request.notificationId(), e);
            return SendResult.failed(e.getMessage());
        }
    }
}
