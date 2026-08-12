package com.example.notification.dto;

import com.example.notification.common.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTemplateRequest {

    @NotBlank
    @Size(max = 100)
    private String templateCode;

    @NotNull
    private NotificationChannel channel;

    @Size(max = 500)
    private String subject;

    @NotBlank
    private String body;
}
