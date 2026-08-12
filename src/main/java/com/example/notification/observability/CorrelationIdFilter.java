package com.example.notification.observability;

import com.example.notification.common.AppConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = headerOrNew(request, AppConstants.CORRELATION_ID_HEADER);
        String requestId = headerOrNew(request, AppConstants.REQUEST_ID_HEADER);
        MDC.put(AppConstants.MDC_CORRELATION_ID, correlationId);
        MDC.put(AppConstants.MDC_REQUEST_ID, requestId);
        response.setHeader(AppConstants.CORRELATION_ID_HEADER, correlationId);
        response.setHeader(AppConstants.REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String headerOrNew(HttpServletRequest request, String header) {
        String value = request.getHeader(header);
        if (value == null || value.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return value;
    }
}
