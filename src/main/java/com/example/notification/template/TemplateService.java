package com.example.notification.template;

import com.example.notification.common.NotificationChannel;
import com.example.notification.config.NotificationProperties;
import com.example.notification.dto.CreateTemplateRequest;
import com.example.notification.dto.TemplateResponse;
import com.example.notification.dto.UpdateTemplateRequest;
import com.example.notification.entity.NotificationTemplateEntity;
import com.example.notification.exception.ApiException;
import com.example.notification.exception.TemplateNotFoundException;
import com.example.notification.mapper.NotificationMapper;
import com.example.notification.repository.NotificationTemplateRepository;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private static final String CACHE_PREFIX = "template:";

    private final NotificationTemplateRepository templateRepository;
    private final NotificationMapper mapper;
    private final StringRedisTemplate redisTemplate;
    private final NotificationProperties properties;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Transactional
    public TemplateResponse create(String clientId, CreateTemplateRequest request) {
        if (templateRepository.existsByClientIdAndTemplateCodeAndChannel(
                clientId, request.getTemplateCode(), request.getChannel())) {
            throw new ApiException(HttpStatus.CONFLICT, "TEMPLATE_EXISTS",
                    "Template already exists for code and channel");
        }
        NotificationTemplateEntity entity = NotificationTemplateEntity.builder()
                .clientId(clientId)
                .templateCode(request.getTemplateCode())
                .channel(request.getChannel())
                .subject(request.getSubject())
                .body(request.getBody())
                .build();
        NotificationTemplateEntity saved = templateRepository.save(entity);
        evictCache(clientId, saved.getTemplateCode(), saved.getChannel());
        return mapper.toTemplateResponse(saved);
    }

    @Transactional(readOnly = true)
    public TemplateResponse get(String clientId, String templateCode, NotificationChannel channel) {
        NotificationTemplateEntity entity = findCachedOrDb(clientId, templateCode, channel);
        return mapper.toTemplateResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> list(String clientId) {
        return templateRepository.findByClientId(clientId).stream()
                .map(mapper::toTemplateResponse)
                .toList();
    }

    @Transactional
    public TemplateResponse update(
            String clientId, String templateCode, NotificationChannel channel, UpdateTemplateRequest request) {
        NotificationTemplateEntity entity = templateRepository
                .findByClientIdAndTemplateCodeAndChannel(clientId, templateCode, channel)
                .orElseThrow(() -> new TemplateNotFoundException(templateCode));
        entity.setSubject(request.getSubject());
        entity.setBody(request.getBody());
        NotificationTemplateEntity saved = templateRepository.save(entity);
        evictCache(clientId, templateCode, channel);
        return mapper.toTemplateResponse(saved);
    }

    @Transactional
    public void delete(String clientId, String templateCode, NotificationChannel channel) {
        if (!templateRepository.existsByClientIdAndTemplateCodeAndChannel(clientId, templateCode, channel)) {
            throw new TemplateNotFoundException(templateCode);
        }
        templateRepository.deleteByClientIdAndTemplateCodeAndChannel(clientId, templateCode, channel);
        evictCache(clientId, templateCode, channel);
    }

    @Transactional(readOnly = true)
    public NotificationTemplateEntity requireTemplate(
            String clientId, String templateCode, NotificationChannel channel) {
        return findCachedOrDb(clientId, templateCode, channel);
    }

    private NotificationTemplateEntity findCachedOrDb(
            String clientId, String templateCode, NotificationChannel channel) {
        String cacheKey = cacheKey(clientId, templateCode, channel);
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, NotificationTemplateEntity.class);
            }
        } catch (Exception e) {
            log.debug("Template cache miss/error for key={}", cacheKey);
        }

        NotificationTemplateEntity entity = templateRepository
                .findByClientIdAndTemplateCodeAndChannel(clientId, templateCode, channel)
                .orElseThrow(() -> new TemplateNotFoundException(templateCode));

        try {
            redisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(entity),
                    Duration.ofSeconds(properties.getTemplateCache().getTtlSeconds()));
        } catch (Exception e) {
            log.debug("Unable to cache template key={}", cacheKey);
        }
        return entity;
    }

    private void evictCache(String clientId, String templateCode, NotificationChannel channel) {
        try {
            redisTemplate.delete(cacheKey(clientId, templateCode, channel));
        } catch (Exception e) {
            log.debug("Unable to evict template cache");
        }
    }

    private String cacheKey(String clientId, String templateCode, NotificationChannel channel) {
        return CACHE_PREFIX + clientId + ":" + templateCode + ":" + channel.name();
    }
}
