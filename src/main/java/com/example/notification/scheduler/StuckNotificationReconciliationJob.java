package com.example.notification.scheduler;

import com.example.notification.common.NotificationStatus;
import com.example.notification.config.NotificationProperties;
import com.example.notification.entity.NotificationEntity;
import com.example.notification.kafka.NotificationEventPublisher;
import com.example.notification.kafka.event.NotificationEvent;
import com.example.notification.mapper.NotificationMapper;
import com.example.notification.observability.NotificationMetrics;
import com.example.notification.repository.NotificationRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StuckNotificationReconciliationJob {

    private final NotificationRepository notificationRepository;
    private final NotificationEventPublisher eventPublisher;
    private final NotificationMapper mapper;
    private final NotificationProperties properties;
    private final NotificationMetrics metrics;

    @Scheduled(fixedDelayString = "${notification.reconciliation.fixed-delay-ms:60000}")
    @Transactional
    public void reconcile() {
        if (!properties.getReconciliation().isEnabled()) {
            return;
        }
        Instant threshold = Instant.now()
                .minus(properties.getReconciliation().getStuckThresholdMinutes(), ChronoUnit.MINUTES);
        List<NotificationEntity> stuck = notificationRepository
                .findByStatusAndUpdatedAtBefore(NotificationStatus.PENDING, threshold);
        stuck.addAll(notificationRepository
                .findByStatusAndUpdatedAtBefore(NotificationStatus.QUEUED, threshold));

        for (NotificationEntity entity : stuck) {
            try {
                NotificationEvent event = NotificationEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .notificationId(entity.getNotificationId())
                        .clientId(entity.getClientId())
                        .channel(entity.getChannel())
                        .templateCode(entity.getTemplateCode())
                        .recipient(entity.getRecipient())
                        .priority(entity.getPriority())
                        .data(mapper.parsePayload(entity.getPayload()))
                        .correlationId(entity.getCorrelationId())
                        .retryCount(entity.getRetryCount())
                        .build();
                entity.setStatus(NotificationStatus.QUEUED);
                notificationRepository.save(entity);
                eventPublisher.publishRequested(event);
                metrics.incrementReconciled();
                log.info("Reconciled stuck notificationId={} previousStatus handled",
                        entity.getNotificationId());
            } catch (Exception e) {
                log.error("Failed to reconcile notificationId={}", entity.getNotificationId(), e);
            }
        }
    }
}
