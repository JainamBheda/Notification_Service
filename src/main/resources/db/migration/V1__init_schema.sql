CREATE TABLE clients (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id       VARCHAR(100)  NOT NULL,
    client_secret_hash VARCHAR(255) NOT NULL,
    name            VARCHAR(255)  NOT NULL,
    status          VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_clients_client_id UNIQUE (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notification_templates (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id       VARCHAR(100)  NOT NULL,
    template_code   VARCHAR(100)  NOT NULL,
    channel         VARCHAR(32)   NOT NULL,
    subject         VARCHAR(500)  NULL,
    body            TEXT          NOT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_templates_client_code_channel UNIQUE (client_id, template_code, channel),
    INDEX idx_templates_client (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notifications (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id     CHAR(36)      NOT NULL,
    client_id           VARCHAR(100)  NOT NULL,
    idempotency_key     VARCHAR(255)  NULL,
    recipient           VARCHAR(500)  NOT NULL,
    channel             VARCHAR(32)   NOT NULL,
    template_code       VARCHAR(100)  NOT NULL,
    payload             JSON          NULL,
    priority            VARCHAR(32)   NOT NULL DEFAULT 'NORMAL',
    status              VARCHAR(32)   NOT NULL,
    retry_count         INT           NOT NULL DEFAULT 0,
    failure_reason      VARCHAR(2000) NULL,
    correlation_id      VARCHAR(64)   NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    sent_at             TIMESTAMP     NULL,
    CONSTRAINT uk_notifications_notification_id UNIQUE (notification_id),
    CONSTRAINT uk_notifications_client_idempotency UNIQUE (client_id, idempotency_key),
    INDEX idx_notifications_client_created (client_id, created_at),
    INDEX idx_notifications_status (status),
    INDEX idx_notifications_client_status (client_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notification_delivery_attempts (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id     CHAR(36)      NOT NULL,
    attempt_no          INT           NOT NULL,
    status              VARCHAR(32)   NOT NULL,
    error               VARCHAR(2000) NULL,
    provider_response   VARCHAR(2000) NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_attempts_notification (notification_id),
    CONSTRAINT fk_attempts_notification
        FOREIGN KEY (notification_id) REFERENCES notifications (notification_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
