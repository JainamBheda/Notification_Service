package com.example.notification.exception;

import org.springframework.http.HttpStatus;

public class TemplateNotFoundException extends ApiException {

    public TemplateNotFoundException(String templateCode) {
        super(HttpStatus.NOT_FOUND, "TEMPLATE_NOT_FOUND",
                "Template not found: " + templateCode);
    }
}
