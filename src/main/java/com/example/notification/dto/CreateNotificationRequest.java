package com.example.notification.dto;

import com.example.notification.common.NotificationChannel;
import com.example.notification.common.NotificationPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import lombok.Data;

@Data
public class CreateNotificationRequest {

    @NotBlank
    @Size(max = 500)
    private String recipient;

    @NotNull
    private NotificationChannel channel;

    @NotBlank
    @Size(max = 100)
    private String templateCode;

    private NotificationPriority priority = NotificationPriority.NORMAL;

    private Map<String, Object> data;
}
