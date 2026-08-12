package com.example.notification.repository;

import com.example.notification.entity.NotificationDeliveryAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryAttemptRepository
        extends JpaRepository<NotificationDeliveryAttemptEntity, Long> {
}
