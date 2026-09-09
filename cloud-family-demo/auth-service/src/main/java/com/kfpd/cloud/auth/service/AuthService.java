package com.kfpd.cloud.auth.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.kfpd.cloud.auth.base.config.AuthLoginProperties;
import com.kfpd.cloud.auth.pojo.LoginRequest;
import com.kfpd.cloud.auth.pojo.LoginResponse;
import com.kfpd.cloud.auth.pojo.TokenValidation;
import com.kfpd.cloud.common.security.CommonJwtProperties;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final String MANAGER_LOGIN_PERMISSION = "manager:login";

    // Demo storage. Replace these in-memory maps with database/cache backed storage in production.
    private final Map<String, LoginAttempt> apiLoginAttempts = new ConcurrentHashMap<>();
    private final AuthLoginProperties loginProperties;
    private final CommonJwtProperties jwtProperties;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final Map<String, LoginAccount> apiAccounts = Map.of(
            "alice", new LoginAccount("alice", "demo", "API", List.of("USER"), List.of("user:profile:read"))
    );
    private final Map<String, LoginAccount> managerAccounts = Map.of(
            "manager", new LoginAccount(
                    "manager",
                    "demo",
                    "MANAGER",
                    List.of("MANAGER"),
                    List.of(MANAGER_LOGIN_PERMISSION, "manager:dashboard:read", "user:profile:read")
            )
    );

    public AuthService(AuthLoginProperties loginProperties,
                       CommonJwtProperties jwtProperties,
                       JwtEncoder jwtEncoder,
                       JwtDecoder jwtDecoder) {
        this.loginProperties = loginProperties;
        this.jwtProperties = jwtProperties;
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
    }

    public LoginResponse apiLogin(LoginRequest request, String clientIp) {
        // API login protects public clients from brute-force attempts by username and source IP.
        String attemptKey = request.username() + "@" + clientIp;
        assertApiLoginNotLocked(attemptKey);

        LoginAccount account = apiAccounts.get(request.username());
        if (!matches(account, request)) {
            recordApiLoginFailure(attemptKey);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid api username or password");
        }

        apiLoginAttempts.remove(attemptKey);
        return login(account);
    }

    public LoginResponse managerLogin(LoginRequest request, String clientIp) {
        // Manager login is checked by network boundary first, then credentials, then explicit permission.
        if (!isManagerIpAllowed(clientIp)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manager login IP is not allowed");
        }

        LoginAccount account = managerAccounts.get(request.username());
        if (!matches(account, request)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid manager username or password");
        }
        if (!account.permissions().contains(MANAGER_LOGIN_PERMISSION)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manager login permission required");
        }

        return login(account);
    }

    private LoginResponse login(LoginAccount account) {
        Instant issuedAt = Instant.now();
        Instant expiresAtInstant = issuedAt.plusSeconds(jwtProperties.getTtlSeconds());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAtInstant)
                .subject(account.username())
                .claim("username", account.username())
                .claim("user_type", account.userType())
                .claim("roles", account.roles())
                .claim("permissions", account.permissions())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        LocalDateTime expiresAt = LocalDateTime.ofInstant(expiresAtInstant, ZoneId.systemDefault());
        return new LoginResponse("Bearer", token, account.username(), account.userType(), account.roles(), account.permissions(), expiresAt);
    }

    public TokenValidation validate(String authorization) {
        String token = resolveToken(authorization);
        if (token == null) {
            return new TokenValidation(false, null, null, List.of(), List.of(), Map.of());
        }
        try {
            Jwt jwt = jwtDecoder.decode(token);
            String username = jwt.getClaimAsString("username");
            String userType = jwt.getClaimAsString("user_type");
            List<String> roles = jwt.getClaimAsStringList("roles");
            List<String> permissions = jwt.getClaimAsStringList("permissions");
            return new TokenValidation(
                    true,
                    username,
                    userType,
                    roles == null ? List.of() : roles,
                    permissions == null ? List.of() : permissions,
                    Map.of("expiresAt", jwt.getExpiresAt())
            );
        } catch (JwtException ex) {
            return new TokenValidation(false, null, null, List.of(), List.of(), Map.of());
        }
    }

    private String resolveToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length());
    }

    private boolean matches(LoginAccount account, LoginRequest request) {
        return account != null && account.password().equals(request.password());
    }

    private void assertApiLoginNotLocked(String attemptKey) {
        LoginAttempt attempt = apiLoginAttempts.get(attemptKey);
        if (attempt != null && attempt.lockedUntil() != null && attempt.lockedUntil().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many api login attempts");
        }
    }

    private void recordApiLoginFailure(String attemptKey) {
        LoginAttempt current = apiLoginAttempts.getOrDefault(attemptKey, new LoginAttempt(0, null));
        int failedCount = current.failedCount() + 1;
        // Once the configured threshold is reached, future attempts are blocked until lockedUntil.
        LocalDateTime lockedUntil = failedCount >= loginProperties.getApi().getMaxFailedAttempts()
                ? LocalDateTime.now().plusSeconds(loginProperties.getApi().getLockSeconds())
                : null;
        apiLoginAttempts.put(attemptKey, new LoginAttempt(failedCount, lockedUntil));
    }

    private boolean isManagerIpAllowed(String clientIp) {
        // "*" can be used for local/demo environments where manager IP filtering is intentionally disabled.
        Set<String> allowedIps = loginProperties.getManager().getAllowedIps();
        return allowedIps.contains("*") || allowedIps.contains(clientIp);
    }

    private record LoginAccount(
            String username,
            String password,
            String userType,
            List<String> roles,
            List<String> permissions
    ) {
    }

    private record LoginAttempt(int failedCount, LocalDateTime lockedUntil) {
    }
}
