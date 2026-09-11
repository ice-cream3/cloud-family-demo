package com.kfpd.cloud.gateway.base.filter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import com.kfpd.cloud.common.exception.ErrorCode;
import com.kfpd.cloud.common.web.GatewayHeaders;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    // Login and actuator health endpoints must be reachable before a token exists.
    private final List<String> publicPaths = List.of("/auth/api/login", "/auth/manager/login", "/auth/refresh", "/actuator/health");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        if (publicPaths.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        return exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .flatMap(authentication -> {
                    Jwt jwt = authentication.getToken();
                    List<String> roles = claimAsStringList(jwt, "roles");
                    List<String> permissions = claimAsStringList(jwt, "permissions");
                    if (path.startsWith("/api/manager/") && !hasManagerAccess(roles)) {
                        return writeError(exchange, ErrorCode.GATEWAY_MANAGER_ROLE_REQUIRED);
                    }
                    ServerHttpRequest authenticatedRequest = request.mutate()
                            .header(GatewayHeaders.USER_NAME, claimOrSubject(jwt, "username"))
                            .header(GatewayHeaders.USER_TYPE, jwt.getClaimAsString("user_type"))
                            .header(GatewayHeaders.USER_ROLES, String.join(",", roles))
                            .header(GatewayHeaders.USER_PERMISSIONS, String.join(",", permissions))
                            .build();
                    return chain.filter(exchange.mutate().request(authenticatedRequest).build());
                })
                .switchIfEmpty(Mono.defer(() -> writeError(exchange, ErrorCode.GATEWAY_MISSING_AUTHENTICATED_JWT)));
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private Mono<Void> writeError(ServerWebExchange exchange, ErrorCode errorCode) {
        byte[] body = errorBody(exchange, errorCode);
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(errorCode.getHttpStatus()));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    private byte[] errorBody(ServerWebExchange exchange, ErrorCode errorCode) {
        String body = "{\"code\":" + errorCode.getCode()
                + ",\"message\":\"" + escapeJson(errorCode.getMessage())
                + "\",\"path\":\"" + escapeJson(exchange.getRequest().getURI().getPath())
                + "\",\"timestamp\":\"" + LocalDateTime.now()
                + "\"}";
        return body.getBytes(StandardCharsets.UTF_8);
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private List<String> claimAsStringList(Jwt jwt, String claimName) {
        List<String> values = jwt.getClaimAsStringList(claimName);
        return values == null ? List.of() : values;
    }

    private boolean hasManagerAccess(List<String> roles) {
        return roles.contains("MANAGER") || roles.contains("SUPER_ADMIN");
    }

    private String claimOrSubject(Jwt jwt, String claimName) {
        String value = jwt.getClaimAsString(claimName);
        return value == null ? jwt.getSubject() : value;
    }
}
