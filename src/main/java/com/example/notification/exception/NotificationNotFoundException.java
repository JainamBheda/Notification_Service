package com.example.notification.exception;

import org.springframework.http.HttpStatus;

public class NotificationNotFoundException extends ApiException {

    public NotificationNotFoundException(String notificationId) {
        super(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND",
                "Notification not found: " + notificationId);
    }
}
