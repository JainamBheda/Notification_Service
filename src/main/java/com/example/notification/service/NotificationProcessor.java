package com.example.notification.service;

import com.example.notification.channel.NotificationSender;
import com.example.notification.channel.NotificationSenderRegistry;
import com.example.notification.common.AppConstants;
import com.example.notification.common.NotificationStatus;
import com.example.notification.config.NotificationProperties;
import com.example.notification.entity.NotificationDeliveryAttemptEntity;
import com.example.notification.entity.NotificationEntity;
import com.example.notification.entity.NotificationTemplateEntity;
import com.example.notification.kafka.NotificationEventPublisher;
import com.example.notification.kafka.event.NotificationEvent;
import com.example.notification.kafka.event.NotificationStatusEvent;
import com.example.notification.observability.NotificationMetrics;
import com.example.notification.repository.NotificationDeliveryAttemptRepository;
import com.example.notification.repository.NotificationRepository;
import com.example.notification.template.TemplateResolver;
import com.example.notification.template.TemplateService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProcessor {

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryAttemptRepository attemptRepository;
    private final TemplateService templateService;
    private final TemplateResolver templateResolver;
    private final NotificationSenderRegistry senderRegistry;
    private final NotificationEventPublisher eventPublisher;
    private final NotificationProperties properties;
    private final NotificationMetrics metrics;

    @Transactional
    public void process(NotificationEvent event) {
        MDC.put(AppConstants.MDC_NOTIFICATION_ID, event.getNotificationId());
        MDC.put(AppConstants.MDC_CLIENT_ID, event.getClientId());
        MDC.put(AppConstants.MDC_CORRELATION_ID, event.getCorrelationId());
        MDC.put(AppConstants.MDC_KAFKA_EVENT_ID, event.getEventId());

        if (event.getNextAttemptAt() != null && event.getNextAttemptAt().isAfter(Instant.now())) {
            long waitMs = Math.min(
                    event.getNextAttemptAt().toEpochMilli() - Instant.now().toEpochMilli(),
                    30_000L);
            if (waitMs > 0) {
                log.debug("Waiting {}ms before retry for notificationId={}",
                        waitMs, event.getNotificationId());
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (event.getNextAttemptAt().isAfter(Instant.now())) {
                    eventPublisher.publishRetry(event);
                    return;
                }
            }
        }

        NotificationEntity notification = notificationRepository
                .findByNotificationId(event.getNotificationId())
                .orElse(null);
        if (notification == null) {
            log.warn("Notification missing for event={}", event.getNotificationId());
            return;
        }
        if (notification.getStatus() == NotificationStatus.SENT) {
            log.info("Skipping already sent notificationId={}", notification.getNotificationId());
            return;
        }

        notification.setStatus(NotificationStatus.PROCESSING);
        notificationRepository.save(notification);
        publishStatus(notification, null);

        try {
            NotificationTemplateEntity template = templateService.requireTemplate(
                    event.getClientId(), event.getTemplateCode(), event.getChannel());
            String subject = templateResolver.resolve(template.getSubject(), event.getData());
            String body = templateResolver.resolve(template.getBody(), event.getData());

            NotificationSender sender = senderRegistry.get(event.getChannel());
            NotificationSender.SendResult result = sender.send(new NotificationSender.SendRequest(
                    event.getNotificationId(),
                    event.getClientId(),
                    event.getRecipient(),
                    subject,
                    body,
                    event.getData()));

            int attemptNo = notification.getRetryCount() + 1;
            saveAttempt(event.getNotificationId(), attemptNo, result);

            if (result.success()) {
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(Instant.now());
                notification.setFailureReason(null);
                notificationRepository.save(notification);
                metrics.incrementSent(event.getChannel().name());
                publishStatus(notification, null);
                log.info("Notification sent channel={} notificationId={}",
                        event.getChannel(), event.getNotificationId());
            } else {
                handleFailure(notification, event, result.errorMessage());
            }
        } catch (Exception e) {
            log.error("Processing failed for notificationId={}", event.getNotificationId(), e);
            handleFailure(notification, event, e.getMessage());
        } finally {
            MDC.remove(AppConstants.MDC_NOTIFICATION_ID);
            MDC.remove(AppConstants.MDC_CLIENT_ID);
            MDC.remove(AppConstants.MDC_KAFKA_EVENT_ID);
        }
    }

    private void handleFailure(NotificationEntity notification, NotificationEvent event, String reason) {
        int nextRetry = notification.getRetryCount() + 1;
        notification.setRetryCount(nextRetry);
        notification.setFailureReason(truncate(reason));

        if (nextRetry >= properties.getRetry().getMaxAttempts()) {
            notification.setStatus(NotificationStatus.FAILED);
            notificationRepository.save(notification);
            metrics.incrementFailed(event.getChannel().name());
            eventPublisher.publishDeadLetter(event);
            publishStatus(notification, reason);
            log.warn("Moved to DLQ notificationId={} retries={}",
                    notification.getNotificationId(), nextRetry);
            return;
        }

        notification.setStatus(NotificationStatus.RETRYING);
        notificationRepository.save(notification);
        metrics.incrementRetry(event.getChannel().name());

        long delay = computeBackoffMs(nextRetry);
        NotificationEvent retryEvent = NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .notificationId(event.getNotificationId())
                .clientId(event.getClientId())
                .channel(event.getChannel())
                .templateCode(event.getTemplateCode())
                .recipient(event.getRecipient())
                .priority(event.getPriority())
                .data(event.getData())
                .correlationId(event.getCorrelationId())
                .retryCount(nextRetry)
                .nextAttemptAt(Instant.now().plusMillis(delay))
                .build();
        eventPublisher.publishRetry(retryEvent);
        publishStatus(notification, reason);
        log.info("Scheduled retry notificationId={} attempt={} delayMs={}",
                notification.getNotificationId(), nextRetry, delay);
    }

    long computeBackoffMs(int attempt) {
        double delay = properties.getRetry().getInitialIntervalMs()
                * Math.pow(properties.getRetry().getMultiplier(), Math.max(0, attempt - 1));
        return Math.min((long) delay, properties.getRetry().getMaxIntervalMs());
    }

    private void saveAttempt(String notificationId, int attemptNo, NotificationSender.SendResult result) {
        attemptRepository.save(NotificationDeliveryAttemptEntity.builder()
                .notificationId(notificationId)
                .attemptNo(attemptNo)
                .status(result.success() ? "SUCCESS" : "FAILED")
                .error(truncate(result.errorMessage()))
                .providerResponse(truncate(result.providerResponse()))
                .build());
    }

    private void publishStatus(NotificationEntity notification, String failureReason) {
        eventPublisher.publishStatus(NotificationStatusEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .notificationId(notification.getNotificationId())
                .clientId(notification.getClientId())
                .status(notification.getStatus())
                .failureReason(failureReason)
                .retryCount(notification.getRetryCount())
                .correlationId(notification.getCorrelationId())
                .occurredAt(Instant.now())
                .build());
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 2000 ? value : value.substring(0, 2000);
    }
}
