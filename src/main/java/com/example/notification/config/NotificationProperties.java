package com.example.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {

    private final Kafka kafka = new Kafka();
    private final Retry retry = new Retry();
    private final RateLimit rateLimit = new RateLimit();
    private final TemplateCache templateCache = new TemplateCache();
    private final Reconciliation reconciliation = new Reconciliation();
    private final Security security = new Security();
    private final Providers providers = new Providers();
    private final Twilio twilio = new Twilio();
    private final Fcm fcm = new Fcm();

    @Getter
    @Setter
    public static class Kafka {
        private Topics topics = new Topics();
        private Consumer consumer = new Consumer();

        @Getter
        @Setter
        public static class Topics {
            private String requested = "notification.requested";
            private String requestedPriority = "notification.requested.priority";
            private String retry = "notification.retry";
            private String deadLetter = "notification.dead-letter";
            private String status = "notification.status";
        }

        @Getter
        @Setter
        public static class Consumer {
            private int concurrency = 3;
            private int priorityConcurrency = 6;
        }
    }

    @Getter
    @Setter
    public static class Retry {
        private int maxAttempts = 3;
        private long initialIntervalMs = 1000;
        private double multiplier = 2.0;
        private long maxIntervalMs = 60000;
    }

    @Getter
    @Setter
    public static class RateLimit {
        private boolean enabled = true;
        private int requestsPerMinute = 100;
    }

    @Getter
    @Setter
    public static class TemplateCache {
        private long ttlSeconds = 300;
    }

    @Getter
    @Setter
    public static class Reconciliation {
        private boolean enabled = true;
        private long fixedDelayMs = 60000;
        private long stuckThresholdMinutes = 5;
    }

    @Getter
    @Setter
    public static class Security {
        private String jwtSecret;
        private long jwtExpirationMs = 3600000;
    }

    @Getter
    @Setter
    public static class Providers {
        private String email = "logging";
        private String sms = "logging";
        private String push = "logging";
    }

    @Getter
    @Setter
    public static class Twilio {
        private String accountSid;
        private String authToken;
        private String fromNumber;
    }

    @Getter
    @Setter
    public static class Fcm {
        private String serverKey;
    }
}
