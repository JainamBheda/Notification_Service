package com.example.notification.exception;

import org.springframework.http.HttpStatus;

public class KafkaPublishException extends ApiException {

    public KafkaPublishException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "KAFKA_PUBLISH_FAILED", message);
        initCause(cause);
    }
}
