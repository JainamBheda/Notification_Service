package com.example.notification.channel;

import com.example.notification.common.NotificationChannel;
import com.example.notification.exception.ApiException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class NotificationSenderRegistry {

    private final Map<NotificationChannel, NotificationSender> senders;

    public NotificationSenderRegistry(List<NotificationSender> senderList) {
        this.senders = new EnumMap<>(NotificationChannel.class);
        for (NotificationSender sender : senderList) {
            senders.put(sender.channel(), sender);
        }
    }

    public NotificationSender get(NotificationChannel channel) {
        NotificationSender sender = senders.get(channel);
        if (sender == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_CHANNEL",
                    "No sender registered for channel: " + channel);
        }
        return sender;
    }
}
