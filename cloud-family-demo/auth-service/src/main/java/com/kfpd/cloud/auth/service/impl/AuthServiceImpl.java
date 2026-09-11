package com.kfpd.cloud.auth.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.kfpd.cloud.auth.base.config.AuthOAuth2JdbcConfig;
import com.kfpd.cloud.auth.base.config.AuthLoginProperties;
import com.kfpd.cloud.auth.pojo.vo.LoginVO;
import com.kfpd.cloud.auth.pojo.LoginResponse;
import com.kfpd.cloud.auth.pojo.vo.RefreshTokenVO;
import com.kfpd.cloud.auth.pojo.TokenValidation;
import com.kfpd.cloud.auth.dao.AuthLoginAccountDao;
import com.kfpd.cloud.auth.service.AuthLoginAccount;
import com.kfpd.cloud.auth.service.AuthService;
import com.kfpd.cloud.common.exception.BusinessException;
import com.kfpd.cloud.common.exception.ErrorCode;
import com.kfpd.cloud.common.security.CommonJwtProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String MANAGER_LOGIN_PERMISSION = "manager:login";

    // Demo rate-limit storage. Replace with database/cache backed storage in production.
    private final Map<String, LoginAttempt> apiLoginAttempts = new ConcurrentHashMap<>();
    private final AuthLoginProperties loginProperties;
    private final CommonJwtProperties jwtProperties;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2AuthorizationService authorizationService;
    private final AuthLoginAccountDao loginAccountDao;

    public AuthServiceImpl(AuthLoginProperties loginProperties,
                           CommonJwtProperties jwtProperties,
                           JwtEncoder jwtEncoder,
                           JwtDecoder jwtDecoder,
                           RegisteredClientRepository registeredClientRepository,
                           OAuth2AuthorizationService authorizationService,
                           AuthLoginAccountDao loginAccountDao) {
        this.loginProperties = loginProperties;
        this.jwtProperties = jwtProperties;
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.registeredClientRepository = registeredClientRepository;
        this.authorizationService = authorizationService;
        this.loginAccountDao = loginAccountDao;
    }

    @Override
    public LoginResponse apiLogin(LoginVO request, String clientIp) {
        // API login protects public clients from brute-force attempts by username and source IP.
        String attemptKey = request.username() + "@" + clientIp;
        log.info("API login attempt: username={}, clientIp={}", request.username(), clientIp);
        assertApiLoginNotLocked(attemptKey);

        Optional<AuthLoginAccount> account = Optional.ofNullable(loginAccountDao.findApiByUsername(request.username()));
        if (account.isEmpty() || !matches(account.get(), request)) {
            recordApiLoginFailure(attemptKey);
            log.warn("API login failed: username={}, clientIp={}, reason=invalid_credentials",
                    request.username(), clientIp);
            throw new BusinessException(ErrorCode.AUTH_INVALID_API_CREDENTIALS);
        }

        apiLoginAttempts.remove(attemptKey);
        log.info("API login authenticated: username={}, clientIp={}", request.username(), clientIp);
        return login(account.get(), AuthOAuth2JdbcConfig.API_CLIENT_ID, AuthOAuth2JdbcConfig.API_LOGIN_GRANT_TYPE);
    }

    @Override
    public LoginResponse managerLogin(LoginVO request, String clientIp) {
        // Manager login is checked by network boundary first, then credentials, then explicit permission.
        log.info("Manager login attempt: username={}, clientIp={}", request.username(), clientIp);
        if (!isManagerIpAllowed(clientIp)) {
            log.warn("Manager login rejected: username={}, clientIp={}, reason=ip_not_allowed",
                    request.username(), clientIp);
            throw new BusinessException(ErrorCode.AUTH_MANAGER_IP_FORBIDDEN);
        }

        Optional<AuthLoginAccount> account = Optional.ofNullable(loginAccountDao.findManagerByUsername(request.username()));
        if (account.isEmpty() || !matches(account.get(), request)) {
            log.warn("Manager login failed: username={}, clientIp={}, reason=invalid_credentials",
                    request.username(), clientIp);
            throw new BusinessException(ErrorCode.AUTH_INVALID_MANAGER_CREDENTIALS);
        }
        if (!"MANAGER".equalsIgnoreCase(account.get().userType()) || !account.get().permissions().contains(MANAGER_LOGIN_PERMISSION)) {
            log.warn("Manager login rejected: username={}, clientIp={}, userType={}, reason=permission_required",
                    request.username(), clientIp, account.get().userType());
            throw new BusinessException(ErrorCode.AUTH_MANAGER_PERMISSION_REQUIRED);
        }

        log.info("Manager login authenticated: username={}, clientIp={}", request.username(), clientIp);
        return login(account.get(), AuthOAuth2JdbcConfig.MANAGER_CLIENT_ID, AuthOAuth2JdbcConfig.MANAGER_LOGIN_GRANT_TYPE);
    }

    private LoginResponse login(AuthLoginAccount account, String clientId, AuthorizationGrantType grantType) {
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null) {
            log.error("Login failed: username={}, clientId={}, grantType={}, reason=client_not_initialized",
                    account.username(), clientId, grantType.getValue());
            throw new BusinessException(ErrorCode.AUTH_CLIENT_NOT_INITIALIZED);
        }

        Instant issuedAt = Instant.now();
        TokenSettings tokenSettings = registeredClient.getTokenSettings();
        Instant expiresAtInstant = issuedAt.plus(tokenSettings.getAccessTokenTimeToLive());
        Instant refreshTokenExpiresAtInstant = issuedAt.plus(tokenSettings.getRefreshTokenTimeToLive());
        Set<String> scopes = new LinkedHashSet<>(account.permissions());
        Jwt jwt = issueAccessToken(account, registeredClient, issuedAt, expiresAtInstant);
        OAuth2AccessToken accessToken = accessToken(jwt, issuedAt, expiresAtInstant, scopes);
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(randomTokenValue(), issuedAt, refreshTokenExpiresAtInstant);
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName(account.username())
                .authorizationGrantType(grantType)
                .authorizedScopes(scopes)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .attribute("user_type", account.userType())
                .attribute("roles", String.join(",", account.roles()))
                .attribute("permissions", String.join(",", account.permissions()))
                .build();
        authorizationService.save(authorization);
        log.info("Token issued: username={}, userType={}, clientId={}, grantType={}, scopes={}, expiresAt={}, refreshTokenExpiresAt={}",
                account.username(), account.userType(), clientId, grantType.getValue(), scopes, expiresAtInstant, refreshTokenExpiresAtInstant);

        LocalDateTime expiresAt = LocalDateTime.ofInstant(expiresAtInstant, ZoneId.systemDefault());
        LocalDateTime refreshTokenExpiresAt = LocalDateTime.ofInstant(refreshTokenExpiresAtInstant, ZoneId.systemDefault());
        return new LoginResponse(
                "Bearer",
                jwt.getTokenValue(),
                refreshToken.getTokenValue(),
                account.username(),
                account.userType(),
                account.roles(),
                account.permissions(),
                expiresAt,
                refreshTokenExpiresAt
        );
    }

    @Override
    public LoginResponse refreshAccessToken(RefreshTokenVO request) {
        log.info("Refresh token attempt");
        OAuth2Authorization existingAuthorization = authorizationService.findByToken(request.refreshToken(), OAuth2TokenType.REFRESH_TOKEN);
        if (existingAuthorization == null || existingAuthorization.getRefreshToken() == null || !existingAuthorization.getRefreshToken().isActive()) {
            log.warn("Refresh token failed: reason=invalid_refresh_token");
            throw new BusinessException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
        }

        RegisteredClient registeredClient = registeredClientRepository.findById(existingAuthorization.getRegisteredClientId());
        if (registeredClient == null) {
            log.warn("Refresh token failed: registeredClientId={}, reason=registered_client_not_found",
                    existingAuthorization.getRegisteredClientId());
            throw new BusinessException(ErrorCode.AUTH_REGISTERED_CLIENT_NOT_FOUND);
        }

        String username = existingAuthorization.getPrincipalName();
        String userType = existingAuthorization.getAttribute("user_type");
        String roles = existingAuthorization.getAttribute("roles");
        String permissions = existingAuthorization.getAttribute("permissions");
        if (userType == null || roles == null || permissions == null) {
            log.warn("Refresh token failed: username={}, registeredClientId={}, reason=authorization_metadata_incomplete",
                    username, existingAuthorization.getRegisteredClientId());
            throw new BusinessException(ErrorCode.AUTHORIZATION_METADATA_INCOMPLETE);
        }

        Instant issuedAt = Instant.now();
        Instant expiresAtInstant = issuedAt.plus(registeredClient.getTokenSettings().getAccessTokenTimeToLive());
        Set<String> scopes = existingAuthorization.getAuthorizedScopes();
        AuthLoginAccount account = new AuthLoginAccount(username, "", userType, splitCsv(roles), splitCsv(permissions));
        Jwt jwt = issueAccessToken(account, registeredClient, issuedAt, expiresAtInstant);
        OAuth2AccessToken accessToken = accessToken(jwt, issuedAt, expiresAtInstant, scopes);

        OAuth2Authorization refreshedAuthorization = OAuth2Authorization.from(existingAuthorization)
                .accessToken(accessToken)
                .build();
        authorizationService.save(refreshedAuthorization);
        log.info("Access token refreshed: username={}, userType={}, clientId={}, scopes={}, expiresAt={}",
                username, userType, registeredClient.getClientId(), scopes, expiresAtInstant);

        LocalDateTime expiresAt = LocalDateTime.ofInstant(expiresAtInstant, ZoneId.systemDefault());
        LocalDateTime refreshTokenExpiresAt = LocalDateTime.ofInstant(
                existingAuthorization.getRefreshToken().getToken().getExpiresAt(),
                ZoneId.systemDefault()
        );
        return new LoginResponse(
                "Bearer",
                jwt.getTokenValue(),
                request.refreshToken(),
                username,
                userType,
                account.roles(),
                account.permissions(),
                expiresAt,
                refreshTokenExpiresAt
        );
    }

    @Override
    public TokenValidation validate(String authorization) {
        String token = resolveToken(authorization);
        if (token == null) {
            log.debug("Token validation skipped: reason=missing_bearer_token");
            return new TokenValidation(false, null, null, List.of(), List.of(), Map.of());
        }
        try {
            Jwt jwt = jwtDecoder.decode(token);
            OAuth2Authorization savedAuthorization = authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
            if (savedAuthorization == null || savedAuthorization.getAccessToken() == null || !savedAuthorization.getAccessToken().isActive()) {
                log.warn("Token validation failed: subject={}, reason=authorization_not_active", jwt.getSubject());
                return new TokenValidation(false, null, null, List.of(), List.of(), Map.of());
            }
            String username = jwt.getClaimAsString("username");
            String userType = jwt.getClaimAsString("user_type");
            List<String> roles = jwt.getClaimAsStringList("roles");
            List<String> permissions = jwt.getClaimAsStringList("permissions");
            log.debug("Token validation succeeded: username={}, userType={}, expiresAt={}",
                    username, userType, jwt.getExpiresAt());
            return new TokenValidation(
                    true,
                    username,
                    userType,
                    roles == null ? List.of() : roles,
                    permissions == null ? List.of() : permissions,
                    Map.of("expiresAt", jwt.getExpiresAt())
            );
        } catch (JwtException ex) {
            log.warn("Token validation failed: reason=jwt_decode_failed, message={}", ex.getMessage());
            return new TokenValidation(false, null, null, List.of(), List.of(), Map.of());
        }
    }

    private String resolveToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length());
    }

    private boolean matches(AuthLoginAccount account, LoginVO request) {
        return account != null && account.passwordHash() != null && account.passwordHash().equals(request.password());
    }

    private Jwt issueAccessToken(AuthLoginAccount account, RegisteredClient registeredClient, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(account.username())
                .claim("username", account.username())
                .claim("user_type", account.userType())
                .claim("roles", account.roles())
                .claim("permissions", account.permissions())
                .claim("client_id", registeredClient.getClientId())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims));
    }

    private OAuth2AccessToken accessToken(Jwt jwt, Instant issuedAt, Instant expiresAt, Set<String> scopes) {
        return new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                jwt.getTokenValue(),
                issuedAt,
                expiresAt,
                scopes
        );
    }

    private String randomTokenValue() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private void assertApiLoginNotLocked(String attemptKey) {
        LoginAttempt attempt = apiLoginAttempts.get(attemptKey);
        if (attempt != null && attempt.lockedUntil() != null && attempt.lockedUntil().isAfter(LocalDateTime.now())) {
            log.warn("API login rejected: attemptKey={}, failedCount={}, lockedUntil={}, reason=too_many_attempts",
                    attemptKey, attempt.failedCount(), attempt.lockedUntil());
            throw new BusinessException(ErrorCode.AUTH_TOO_MANY_API_LOGIN_ATTEMPTS);
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
        log.warn("API login failure recorded: attemptKey={}, failedCount={}, lockedUntil={}",
                attemptKey, failedCount, lockedUntil);
    }

    private boolean isManagerIpAllowed(String clientIp) {
        // "*" can be used for local/demo environments where manager IP filtering is intentionally disabled.
        Set<String> allowedIps = loginProperties.getManager().getAllowedIps();
        return allowedIps.contains("*") || allowedIps.contains(clientIp);
    }

    private record LoginAttempt(int failedCount, LocalDateTime lockedUntil) {
    }
}
