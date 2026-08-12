package com.example.notification.common;

public enum NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL;

    public boolean isHighPriority() {
        return this == HIGH || this == CRITICAL;
    }
}
