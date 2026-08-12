package com.example.notification.repository;

import com.example.notification.common.NotificationChannel;
import com.example.notification.entity.NotificationTemplateEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, Long> {

    Optional<NotificationTemplateEntity> findByClientIdAndTemplateCodeAndChannel(
            String clientId, String templateCode, NotificationChannel channel);

    List<NotificationTemplateEntity> findByClientId(String clientId);

    boolean existsByClientIdAndTemplateCodeAndChannel(
            String clientId, String templateCode, NotificationChannel channel);

    void deleteByClientIdAndTemplateCodeAndChannel(
            String clientId, String templateCode, NotificationChannel channel);
}
