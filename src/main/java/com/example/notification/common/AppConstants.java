package com.example.notification.common;

public final class AppConstants {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_NOTIFICATION_ID = "notificationId";
    public static final String MDC_CLIENT_ID = "clientId";
    public static final String MDC_KAFKA_EVENT_ID = "kafkaEventId";

    private AppConstants() {
    }
}
