package com.example.notification.exception;

import org.springframework.http.HttpStatus;

public class DuplicateIdempotencyKeyException extends ApiException {

    public DuplicateIdempotencyKeyException(String key) {
        super(HttpStatus.CONFLICT, "DUPLICATE_IDEMPOTENCY_KEY",
                "Duplicate idempotency key: " + key);
    }
}
