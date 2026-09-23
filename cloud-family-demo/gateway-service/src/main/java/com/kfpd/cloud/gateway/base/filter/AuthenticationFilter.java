package com.kfpd.cloud.gateway.base.filter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.kfpd.cloud.common.exception.ErrorCode;
import com.kfpd.cloud.common.web.ApiResponse;
import com.kfpd.cloud.common.web.GatewayHeaders;
import com.kfpd.cloud.gateway.pojo.TokenValidation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);
    private static final TokenValidation INVALID_TOKEN = new TokenValidation(false, null, null, List.of(), List.of(), Map.of());
    private static final ParameterizedTypeReference<ApiResponse<TokenValidation>> TOKEN_VALIDATION_RESPONSE =
            new ParameterizedTypeReference<>() {
            };

    // Login and actuator health endpoints must be reachable before a token exists.
    private final List<String> publicPaths = List.of("/auth/api/login", "/auth/manager/login", "/auth/refresh", "/actuator/health");
    private final WebClient authWebClient;
    private final String internalToken;

    public AuthenticationFilter(WebClient.Builder webClientBuilder,
                                @Value("${demo.auth-service-url:http://localhost:8081}") String authServiceUrl,
                                @Value("${demo.gateway.internal-token:cloud-family-demo-gateway-internal-token}") String internalToken) {
        this.authWebClient = webClientBuilder.baseUrl(authServiceUrl).build();
        this.internalToken = internalToken;
    }

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
                    String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                    return validateWithAuthService(authorization)
                            .flatMap(validation -> routeAuthenticatedRequest(exchange, chain, request, path, jwt, validation))
                            .thenReturn(Boolean.TRUE);
                })
                .switchIfEmpty(Mono.defer(() -> writeError(exchange, ErrorCode.GATEWAY_MISSING_AUTHENTICATED_JWT).thenReturn(Boolean.TRUE)))
                .then();
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private Mono<Void> writeError(ServerWebExchange exchange, ErrorCode errorCode) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty();
        }
        log.warn("Gateway authentication exception: code={}, status={}, method={}, path={}, message={}",
                errorCode.getCode(),
                errorCode.getHttpStatus(),
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath(),
                errorCode.getMessage());
        byte[] body = errorBody(errorCode);
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(errorCode.getHttpStatus()));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    private byte[] errorBody(ErrorCode errorCode) {
        String body = "{\"code\":" + errorCode.getCode()
                + ",\"message\":\"" + escapeJson(errorCode.getMessage())
                + "\",\"data\":null"
                + ",\"timestamp\":\"" + LocalDateTime.now()
                + "\"}";
        return body.getBytes(StandardCharsets.UTF_8);
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Mono<Void> routeAuthenticatedRequest(ServerWebExchange exchange,
                                                 GatewayFilterChain chain,
                                                 ServerHttpRequest request,
                                                 String path,
                                                 Jwt jwt,
                                                 TokenValidation validation) {
        if (validation == null || !validation.valid()) {
            log.warn("Gateway token rejected by auth-service: method={}, path={}, subject={}",
                    request.getMethod(), path, jwt.getSubject());
            return writeError(exchange, ErrorCode.GATEWAY_INVALID_ACCESS_TOKEN);
        }

        List<String> roles = emptyIfNull(validation.roles());
        List<String> permissions = emptyIfNull(validation.permissions());
        if (path.startsWith("/api/manager/") && !hasManagerAccess(roles)) {
            return writeError(exchange, ErrorCode.GATEWAY_MANAGER_ROLE_REQUIRED);
        }
        ServerHttpRequest authenticatedRequest = request.mutate()
                .headers(headers -> {
                    headers.set(GatewayHeaders.USER_NAME, validation.username() == null ? claimOrSubject(jwt, "username") : validation.username());
                    headers.set(GatewayHeaders.USER_TYPE, validation.userType() == null ? jwt.getClaimAsString("user_type") : validation.userType());
                    headers.set(GatewayHeaders.USER_ROLES, String.join(",", roles));
                    headers.set(GatewayHeaders.USER_PERMISSIONS, String.join(",", permissions));
                    headers.set(GatewayHeaders.INTERNAL_TOKEN, internalToken);
                })
                .build();
        return chain.filter(exchange.mutate().request(authenticatedRequest).build());
    }

    private Mono<TokenValidation> validateWithAuthService(String authorization) {
        return authWebClient.post()
                .uri("/auth/validate")
                .header(HttpHeaders.AUTHORIZATION, authorization == null ? "" : authorization)
                .retrieve()
                .bodyToMono(TOKEN_VALIDATION_RESPONSE)
                .map(ApiResponse::data)
                .defaultIfEmpty(INVALID_TOKEN)
                .onErrorResume(ex -> {
                    log.warn("Gateway auth-service validation failed: message={}", ex.getMessage());
                    return Mono.just(INVALID_TOKEN);
                });
    }

    private List<String> emptyIfNull(List<String> values) {
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
