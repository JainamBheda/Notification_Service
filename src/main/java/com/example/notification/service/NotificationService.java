package com.example.notification.service;

import com.example.notification.common.AppConstants;
import com.example.notification.common.NotificationPriority;
import com.example.notification.common.NotificationStatus;
import com.example.notification.dto.CreateNotificationRequest;
import com.example.notification.dto.CreateNotificationResponse;
import com.example.notification.dto.NotificationResponse;
import com.example.notification.dto.NotificationStatusResponse;
import com.example.notification.entity.NotificationEntity;
import com.example.notification.exception.NotificationNotFoundException;
import com.example.notification.kafka.NotificationEventPublisher;
import com.example.notification.kafka.event.NotificationEvent;
import com.example.notification.mapper.NotificationMapper;
import com.example.notification.observability.NotificationMetrics;
import com.example.notification.repository.NotificationRepository;
import com.example.notification.template.TemplateService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final TemplateService templateService;
    private final NotificationMapper mapper;
    private final NotificationEventPublisher eventPublisher;
    private final NotificationMetrics metrics;

    @Transactional
    public CreateNotificationResponse create(
            String clientId, CreateNotificationRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = notificationRepository.findByClientIdAndIdempotencyKey(clientId, idempotencyKey);
            if (existing.isPresent()) {
                NotificationEntity entity = existing.get();
                log.info("Idempotent hit notificationId={}", entity.getNotificationId());
                return CreateNotificationResponse.builder()
                        .notificationId(entity.getNotificationId())
                        .status(entity.getStatus())
                        .build();
            }
        }

        templateService.requireTemplate(clientId, request.getTemplateCode(), request.getChannel());

        NotificationPriority priority = request.getPriority() == null
                ? NotificationPriority.NORMAL
                : request.getPriority();
        String notificationId = UUID.randomUUID().toString();
        String correlationId = MDC.get(AppConstants.MDC_CORRELATION_ID);

        NotificationEntity entity = NotificationEntity.builder()
                .notificationId(notificationId)
                .clientId(clientId)
                .idempotencyKey(blankToNull(idempotencyKey))
                .recipient(request.getRecipient())
                .channel(request.getChannel())
                .templateCode(request.getTemplateCode())
                .payload(mapper.toPayloadJson(request.getData()))
                .priority(priority)
                .status(NotificationStatus.QUEUED)
                .retryCount(0)
                .correlationId(correlationId)
                .build();

        try {
            notificationRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            if (idempotencyKey != null) {
                return notificationRepository.findByClientIdAndIdempotencyKey(clientId, idempotencyKey)
                        .map(found -> CreateNotificationResponse.builder()
                                .notificationId(found.getNotificationId())
                                .status(found.getStatus())
                                .build())
                        .orElseThrow(() -> e);
            }
            throw e;
        }

        NotificationEvent event = NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .notificationId(notificationId)
                .clientId(clientId)
                .channel(request.getChannel())
                .templateCode(request.getTemplateCode())
                .recipient(request.getRecipient())
                .priority(priority)
                .data(request.getData())
                .correlationId(correlationId)
                .retryCount(0)
                .build();

        try {
            eventPublisher.publishRequested(event);
            metrics.incrementQueued(request.getChannel().name());
        } catch (RuntimeException e) {
            entity.setStatus(NotificationStatus.PENDING);
            entity.setFailureReason("Kafka publish failed; awaiting reconciliation");
            notificationRepository.save(entity);
            throw e;
        }

        MDC.put(AppConstants.MDC_NOTIFICATION_ID, notificationId);
        log.info("Notification queued channel={} priority={}", request.getChannel(), priority);

        return CreateNotificationResponse.builder()
                .notificationId(notificationId)
                .status(NotificationStatus.QUEUED)
                .build();
    }

    @Transactional(readOnly = true)
    public NotificationResponse get(String clientId, String notificationId) {
        return mapper.toNotificationResponse(require(clientId, notificationId));
    }

    @Transactional(readOnly = true)
    public NotificationStatusResponse getStatus(String clientId, String notificationId) {
        return mapper.toStatusResponse(require(clientId, notificationId));
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(String clientId, Pageable pageable) {
        return notificationRepository.findByClientId(clientId, pageable).map(mapper::toNotificationResponse);
    }

    private NotificationEntity require(String clientId, String notificationId) {
        return notificationRepository.findByNotificationIdAndClientId(notificationId, clientId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
