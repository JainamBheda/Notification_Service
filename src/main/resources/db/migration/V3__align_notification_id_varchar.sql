ALTER TABLE notification_delivery_attempts
    DROP FOREIGN KEY fk_attempts_notification;

ALTER TABLE notifications
    MODIFY COLUMN notification_id VARCHAR(36) NOT NULL;

ALTER TABLE notification_delivery_attempts
    MODIFY COLUMN notification_id VARCHAR(36) NOT NULL;

ALTER TABLE notification_delivery_attempts
    ADD CONSTRAINT fk_attempts_notification
        FOREIGN KEY (notification_id) REFERENCES notifications (notification_id);
