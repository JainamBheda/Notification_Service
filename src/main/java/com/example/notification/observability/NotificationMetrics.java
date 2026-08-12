package com.example.notification.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class NotificationMetrics {

    private final MeterRegistry meterRegistry;

    public NotificationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementQueued(String channel) {
        counter("notifications.queued", channel).increment();
    }

    public void incrementSent(String channel) {
        counter("notifications.sent", channel).increment();
    }

    public void incrementFailed(String channel) {
        counter("notifications.failed", channel).increment();
    }

    public void incrementRetry(String channel) {
        counter("notifications.retry", channel).increment();
    }

    public void incrementRateLimited() {
        meterRegistry.counter("notifications.rate_limited").increment();
    }

    public void incrementReconciled() {
        meterRegistry.counter("notifications.reconciled").increment();
    }

    private Counter counter(String name, String channel) {
        return meterRegistry.counter(name, "channel", channel);
    }
}
