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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.redisson.api.RAtomicLongReactive;
import org.redisson.api.RedissonReactiveClient;

import reactor.core.publisher.Mono;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final GatewayRateLimitProperties properties;
    private final RedissonReactiveClient redissonReactiveClient;
    private final Map<String, ArrayDeque<Long>> requestTimestamps = new ConcurrentHashMap<>();

    public RateLimitFilter(GatewayRateLimitProperties properties,
                           ObjectProvider<RedissonReactiveClient> redissonReactiveClientProvider) {
        this.properties = properties;
        this.redissonReactiveClient = redissonReactiveClientProvider.getIfAvailable();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        addSecurityHeaders(exchange);
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        if (redissonReactiveClient != null) {
            return allowByRedisson(request)
                    .flatMap(allowed -> allowed ? chain.filter(exchange) : writeError(exchange, ErrorCode.GATEWAY_TOO_MANY_REQUESTS))
                    .onErrorResume(ex -> {
                        log.warn("Redisson rate limit failed, falling back to local limiter: message={}", ex.getMessage());
                        return allowByLocal(request) ? chain.filter(exchange) : writeError(exchange, ErrorCode.GATEWAY_TOO_MANY_REQUESTS);
                    });
        }

        return allowByLocal(request) ? chain.filter(exchange) : writeError(exchange, ErrorCode.GATEWAY_TOO_MANY_REQUESTS);
    }

    @Override
    public int getOrder() {
        return -200;
    }

    private Mono<Boolean> allowByRedisson(ServerHttpRequest request) {
        long now = System.currentTimeMillis();
        int windowSeconds = Math.max(1, properties.getWindowSeconds());
        int maxRequests = Math.max(1, properties.getMaxRequests());
        long windowId = now / Duration.ofSeconds(windowSeconds).toMillis();
        String key = "gateway:rate-limit:" + windowId + ":" + rateLimitKey(request);
        RAtomicLongReactive counter = redissonReactiveClient.getAtomicLong(key);

        return counter.incrementAndGet()
                .flatMap(count -> {
                    Mono<Boolean> expire = count == 1
                            ? counter.expire(Duration.ofSeconds(windowSeconds + 1))
                            : Mono.just(Boolean.TRUE);
                    if (count > maxRequests) {
                        log.warn("Gateway Redisson rate limit exceeded: key={}, count={}, maxRequests={}, windowSeconds={}",
                                key, count, maxRequests, windowSeconds);
                    }
                    return expire.thenReturn(count <= maxRequests);
                });
    }

    private boolean allowByLocal(ServerHttpRequest request) {
        String key = rateLimitKey(request);
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
                return false;
            }
            timestamps.addLast(now);
            currentCount = timestamps.size();
        }

        if (currentCount == 1) {
            cleanupEmptyBuckets(now - windowMillis);
        }
        return true;
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

    private String rateLimitKey(ServerHttpRequest request) {
        return clientIp(request) + "|" + request.getMethod() + "|" + request.getURI().getPath();
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
