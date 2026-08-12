package com.example.notification.repository;

import com.example.notification.common.NotificationStatus;
import com.example.notification.entity.NotificationEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    Optional<NotificationEntity> findByNotificationId(String notificationId);

    Optional<NotificationEntity> findByNotificationIdAndClientId(String notificationId, String clientId);

    Optional<NotificationEntity> findByClientIdAndIdempotencyKey(String clientId, String idempotencyKey);

    Page<NotificationEntity> findByClientId(String clientId, Pageable pageable);

    List<NotificationEntity> findByStatusAndUpdatedAtBefore(NotificationStatus status, Instant updatedBefore);
}
