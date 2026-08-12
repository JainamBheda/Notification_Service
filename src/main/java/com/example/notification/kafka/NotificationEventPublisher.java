package com.example.notification.kafka;

import com.example.notification.common.NotificationPriority;
import com.example.notification.config.NotificationProperties;
import com.example.notification.exception.KafkaPublishException;
import com.example.notification.kafka.event.NotificationEvent;
import com.example.notification.kafka.event.NotificationStatusEvent;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final NotificationProperties properties;

    public void publishRequested(NotificationEvent event) {
        String topic = resolveRequestedTopic(event.getPriority());
        send(topic, event.getNotificationId(), event);
    }

    public void publishRetry(NotificationEvent event) {
        send(properties.getKafka().getTopics().getRetry(), event.getNotificationId(), event);
    }

    public void publishDeadLetter(NotificationEvent event) {
        send(properties.getKafka().getTopics().getDeadLetter(), event.getNotificationId(), event);
    }

    public void publishStatus(NotificationStatusEvent event) {
        send(properties.getKafka().getTopics().getStatus(), event.getNotificationId(), event);
    }

    private String resolveRequestedTopic(NotificationPriority priority) {
        if (priority != null && priority.isHighPriority()) {
            return properties.getKafka().getTopics().getRequestedPriority();
        }
        return properties.getKafka().getTopics().getRequested();
    }

    private void send(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, payload).get(10, TimeUnit.SECONDS);
            log.info("Published event to topic={} key={}", topic, key);
        } catch (Exception e) {
            throw new KafkaPublishException("Failed to publish to topic " + topic, e);
        }
    }
}
