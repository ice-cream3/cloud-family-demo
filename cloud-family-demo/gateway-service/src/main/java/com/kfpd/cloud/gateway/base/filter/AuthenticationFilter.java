package com.kfpd.cloud.gateway.base.filter;

import java.nio.charset.StandardCharsets;
import java.util.List;

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
    private final List<String> publicPaths = List.of("/auth/api/login", "/auth/manager/login", "/actuator/health");

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
                    if (path.startsWith("/api/manager/") && !roles.contains("MANAGER")) {
                        return forbidden(exchange, "Manager role required");
                    }
                    ServerHttpRequest authenticatedRequest = request.mutate()
                            .header(GatewayHeaders.USER_NAME, claimOrSubject(jwt, "username"))
                            .header(GatewayHeaders.USER_TYPE, jwt.getClaimAsString("user_type"))
                            .header(GatewayHeaders.USER_ROLES, String.join(",", roles))
                            .header(GatewayHeaders.USER_PERMISSIONS, String.join(",", permissions))
                            .build();
                    return chain.filter(exchange.mutate().request(authenticatedRequest).build());
                })
                .switchIfEmpty(Mono.defer(() -> unauthorized(exchange, "Missing authenticated JWT")));
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        byte[] body = ("{\"code\":401,\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        byte[] body = ("{\"code\":403,\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    private List<String> claimAsStringList(Jwt jwt, String claimName) {
        List<String> values = jwt.getClaimAsStringList(claimName);
        return values == null ? List.of() : values;
    }

    private String claimOrSubject(Jwt jwt, String claimName) {
        String value = jwt.getClaimAsString(claimName);
        return value == null ? jwt.getSubject() : value;
    }
}
