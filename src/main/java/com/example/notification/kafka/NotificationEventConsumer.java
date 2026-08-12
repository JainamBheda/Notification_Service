package com.example.notification.kafka;

import com.example.notification.kafka.event.NotificationEvent;
import com.example.notification.service.NotificationProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationProcessor processor;

    @KafkaListener(
            topics = "${notification.kafka.topics.requested}",
            groupId = "notification-processor",
            containerFactory = "kafkaListenerContainerFactory")
    public void onRequested(NotificationEvent event) {
        log.debug("Received requested event notificationId={}", event.getNotificationId());
        processor.process(event);
    }

    @KafkaListener(
            topics = "${notification.kafka.topics.requested-priority}",
            groupId = "notification-processor",
            containerFactory = "priorityKafkaListenerContainerFactory")
    public void onPriorityRequested(NotificationEvent event) {
        log.debug("Received priority event notificationId={}", event.getNotificationId());
        processor.process(event);
    }

    @KafkaListener(
            topics = "${notification.kafka.topics.retry}",
            groupId = "notification-retry-processor",
            containerFactory = "kafkaListenerContainerFactory")
    public void onRetry(NotificationEvent event) {
        log.debug("Received retry event notificationId={}", event.getNotificationId());
        processor.process(event);
    }
}
