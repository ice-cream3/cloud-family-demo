package com.kfpd.cloud.gateway.base.filter;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.kfpd.cloud.common.exception.ErrorCode;
import com.kfpd.cloud.gateway.base.config.GatewayRateLimitProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final GatewayRateLimitProperties properties;
    private final Map<String, ArrayDeque<Long>> requestTimestamps = new ConcurrentHashMap<>();

    public RateLimitFilter(GatewayRateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        addSecurityHeaders(exchange);
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String key = clientIp(request) + "|" + request.getMethod() + "|" + request.getURI().getPath();
        long now = System.currentTimeMillis();
        long windowMillis = Duration.ofSeconds(Math.max(1, properties.getWindowSeconds())).toMillis();
        int maxRequests = Math.max(1, properties.getMaxRequests());

        ArrayDeque<Long> timestamps = requestTimestamps.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        int currentCount;
        synchronized (timestamps) {
            purgeExpired(timestamps, now - windowMillis);
            if (timestamps.size() >= maxRequests) {
                currentCount = timestamps.size();
                log.warn("Gateway rate limit exceeded: key={}, count={}, maxRequests={}, windowSeconds={}",
                        key, currentCount, maxRequests, properties.getWindowSeconds());
                return writeError(exchange, ErrorCode.GATEWAY_TOO_MANY_REQUESTS);
            }
            timestamps.addLast(now);
            currentCount = timestamps.size();
        }

        if (currentCount == 1) {
            cleanupEmptyBuckets(now - windowMillis);
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -200;
    }

    private void purgeExpired(ArrayDeque<Long> timestamps, long threshold) {
        while (!timestamps.isEmpty() && timestamps.peekFirst() < threshold) {
            timestamps.removeFirst();
        }
    }

    private void cleanupEmptyBuckets(long threshold) {
        Iterator<Map.Entry<String, ArrayDeque<Long>>> iterator = requestTimestamps.entrySet().iterator();
        while (iterator.hasNext()) {
            ArrayDeque<Long> timestamps = iterator.next().getValue();
            synchronized (timestamps) {
                purgeExpired(timestamps, threshold);
                if (timestamps.isEmpty()) {
                    iterator.remove();
                }
            }
        }
    }

    private String clientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddress() == null ? "unknown" : request.getRemoteAddress().getAddress().getHostAddress();
    }

    private void addSecurityHeaders(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add("X-Frame-Options", "DENY");
        headers.add("X-XSS-Protection", "0");
        headers.add("Referrer-Policy", "no-referrer");
        headers.add("Cache-Control", "no-store");
    }

    private Mono<Void> writeError(ServerWebExchange exchange, ErrorCode errorCode) {
        byte[] body = errorBody(errorCode);
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(errorCode.getHttpStatus()));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    private byte[] errorBody(ErrorCode errorCode) {
        String body = "{\"code\":" + errorCode.getCode()
                + ",\"message\":\"" + errorCode.getMessage()
                + "\",\"data\":null"
                + ",\"timestamp\":\"" + LocalDateTime.now()
                + "\"}";
        return body.getBytes(StandardCharsets.UTF_8);
    }
}
