package com.example.notification.ratelimit;

import com.example.notification.config.NotificationProperties;
import com.example.notification.exception.RateLimitExceededException;
import com.example.notification.observability.NotificationMetrics;
import com.example.notification.security.ClientPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private final NotificationProperties properties;
    private final NotificationMetrics metrics;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!properties.getRateLimit().isEnabled()) {
            return true;
        }
        if (!request.getRequestURI().startsWith("/api/v1/notifications")
                || !"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof ClientPrincipal principal)) {
            return true;
        }

        String clientId = principal.getClientId();
        String key = "rate:" + clientId + ":" + (System.currentTimeMillis() / 60000);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofMinutes(2));
        }
        int limit = properties.getRateLimit().getRequestsPerMinute();
        if (count != null && count > limit) {
            metrics.incrementRateLimited();
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.setHeader("Retry-After", "60");
            throw new RateLimitExceededException(clientId);
        }
        if (count != null) {
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - count)));
        }
        return true;
    }
}
