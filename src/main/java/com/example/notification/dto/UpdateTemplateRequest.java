package com.example.notification.dto;

import com.example.notification.common.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTemplateRequest {

    @Size(max = 500)
    private String subject;

    @NotBlank
    private String body;

    private NotificationChannel channel;
}
