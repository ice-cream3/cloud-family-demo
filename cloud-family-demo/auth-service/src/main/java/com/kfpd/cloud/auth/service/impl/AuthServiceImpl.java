package com.kfpd.cloud.auth.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfpd.cloud.auth.base.config.AuthOAuth2JdbcConfig;
import com.kfpd.cloud.auth.base.config.AuthLoginProperties;
import com.kfpd.cloud.auth.base.config.AuthSessionProperties;
import com.kfpd.cloud.auth.dao.cloud.OAuth2AuthorizationDao;
import com.kfpd.cloud.auth.dao.model.ManagerLoginLogDao;
import com.kfpd.cloud.auth.dao.model.PartnerLoginLogDao;
import com.kfpd.cloud.auth.pojo.dto.LoginRequestContext;
import com.kfpd.cloud.auth.pojo.dto.SessionStatusVO;
import com.kfpd.cloud.auth.pojo.entity.ManagerLoginLog;
import com.kfpd.cloud.auth.pojo.entity.PartnerLoginLog;
import com.kfpd.cloud.auth.pojo.vo.LoginVO;
import com.kfpd.cloud.auth.pojo.dto.LoginResponse;
import com.kfpd.cloud.auth.pojo.dto.RedisLoginSession;
import com.kfpd.cloud.auth.pojo.vo.AntiAbuseChallengeVO;
import com.kfpd.cloud.auth.pojo.vo.AntiAbuseChallengeVO.ChallengeOptionVO;
import com.kfpd.cloud.auth.pojo.vo.AntiAbuseChallengeVO.ChallengeShapeVO;
import com.kfpd.cloud.auth.pojo.vo.KickOutVO;
import com.kfpd.cloud.auth.pojo.vo.RefreshTokenVO;
import com.kfpd.cloud.auth.pojo.vo.RegisterVO;
import com.kfpd.cloud.auth.pojo.dto.TokenValidation;
import com.kfpd.cloud.auth.dao.cloud.AuthLoginAccountDao;
import com.kfpd.cloud.auth.pojo.dto.AuthLoginAccount;
import com.kfpd.cloud.auth.service.AuthService;
import com.kfpd.cloud.auth.util.GraphicChallengeUtil;
import com.kfpd.cloud.common.config.datasource.MultiDataSourceNames;
import com.kfpd.cloud.common.exception.BusinessException;
import com.kfpd.cloud.common.exception.ErrorCode;
import com.kfpd.cloud.common.config.security.CommonJwtProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.redisson.api.RBucket;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
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
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String MANAGER_LOGIN_PERMISSION = "manager:login";
    private static final String LOGIN_RESULT_SUCCESS = "SUCCESS";
    private static final String LOGIN_RESULT_FAILURE = "FAILURE";
    private static final String CHALLENGE_PURPOSE_API_LOGIN = "api_login";
    private static final String CHALLENGE_PURPOSE_API_REGISTER = "api_register";
    private static final List<ChallengeScheme> CHALLENGE_SCHEMES = List.of(
            new ChallengeScheme("graphic_set", 3, false, "请选择:"),
            new ChallengeScheme("graphic_sequence", 3, true, "按顺序选择:"),
            new ChallengeScheme("graphic_pair", 2, false, "请选择:"),
            new ChallengeScheme("image_reassemble", 4, true, "按图重组:")
    );
    private static final Duration LOGIN_CHALLENGE_TTL = Duration.ofMinutes(3);
    private static final Map<String, LoginChallenge> LOGIN_CHALLENGES = new ConcurrentHashMap<>();
    private static final List<PatternColor> LOGIN_PATTERN_COLORS = List.of(
            new PatternColor("blue", "蓝色", "#2563eb"),
            new PatternColor("green", "绿色", "#059669"),
            new PatternColor("orange", "橙色", "#ea580c"),
            new PatternColor("purple", "紫色", "#7c3aed"),
            new PatternColor("red", "红色", "#dc2626")
    );

    // Demo rate-limit storage. Replace with database/cache backed storage in production.
    private final Map<String, LoginAttempt> apiLoginAttempts = new ConcurrentHashMap<>();
    private final AuthLoginProperties loginProperties;
    private final CommonJwtProperties jwtProperties;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2AuthorizationService authorizationService;
    private final AuthLoginAccountDao loginAccountDao;
    private final PartnerLoginLogDao partnerLoginLogDao;
    private final ManagerLoginLogDao managerLoginLogDao;
    private final OAuth2AuthorizationDao oAuth2AuthorizationDao;
    private final AuthSessionProperties sessionProperties;
    private final ObjectProvider<RedissonClient> redissonClientProvider;
    private final ObjectMapper objectMapper;

    public AuthServiceImpl(AuthLoginProperties loginProperties,
                           CommonJwtProperties jwtProperties,
                           JwtEncoder jwtEncoder,
                           JwtDecoder jwtDecoder,
                           RegisteredClientRepository registeredClientRepository,
                           OAuth2AuthorizationService authorizationService,
                           AuthLoginAccountDao loginAccountDao,
                           PartnerLoginLogDao partnerLoginLogDao,
                           ManagerLoginLogDao managerLoginLogDao,
                           OAuth2AuthorizationDao oAuth2AuthorizationDao,
                           AuthSessionProperties sessionProperties,
                           ObjectProvider<RedissonClient> redissonClientProvider,
                           ObjectMapper objectMapper) {
        this.loginProperties = loginProperties;
        this.jwtProperties = jwtProperties;
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.registeredClientRepository = registeredClientRepository;
        this.authorizationService = authorizationService;
        this.loginAccountDao = loginAccountDao;
        this.partnerLoginLogDao = partnerLoginLogDao;
        this.managerLoginLogDao = managerLoginLogDao;
        this.oAuth2AuthorizationDao = oAuth2AuthorizationDao;
        this.sessionProperties = sessionProperties;
        this.redissonClientProvider = redissonClientProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public LoginResponse apiLogin(LoginVO request, LoginRequestContext context) {
        // API login protects public clients from brute-force attempts by username and source IP.
        String clientIp = context.clientIp();
        String attemptKey = request.username() + "@" + clientIp;
        log.info("API login attempt: username={}, clientIp={}", request.username(), clientIp);
        verifyLoginChallenge(request.antiAbuseChallengeId(), request.antiAbuseAnswer(), CHALLENGE_PURPOSE_API_LOGIN, context);
        assertApiLoginNotLocked(attemptKey);

        Optional<AuthLoginAccount> account = Optional.ofNullable(loginAccountDao.findApiByUsername(request.username()));
        if (account.isEmpty() || !matches(account.get(), request)) {
            recordApiLoginFailure(attemptKey);
            recordLoginFailure(request.username(), "API", AuthOAuth2JdbcConfig.API_CLIENT_ID,
                    AuthOAuth2JdbcConfig.API_LOGIN_GRANT_TYPE, context, "INVALID_CREDENTIALS");
            log.warn("API login failed: username={}, clientIp={}, reason=invalid_credentials",
                    request.username(), clientIp);
            throw new BusinessException(ErrorCode.AUTH_INVALID_API_CREDENTIALS);
        }

        apiLoginAttempts.remove(attemptKey);
        log.info("API login authenticated: username={}, clientIp={}", request.username(), clientIp);
        return login(account.get(), AuthOAuth2JdbcConfig.API_CLIENT_ID, AuthOAuth2JdbcConfig.API_LOGIN_GRANT_TYPE, context);
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public LoginResponse apiRegister(RegisterVO request, LoginRequestContext context) {
        requireRegisterText(request.username(), "username is required");
        requireRegisterText(request.password(), "password is required");
        requireRegisterText(request.displayName(), "displayName is required");
        verifyLoginChallenge(request.antiAbuseChallengeId(), request.antiAbuseAnswer(), CHALLENGE_PURPOSE_API_REGISTER, context);
        String username = request.username().trim();
        if (loginAccountDao.countApiByUsername(username) > 0) {
            log.warn("API register rejected: username={}, reason=duplicate_username", username);
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "username already exists");
        }
        String email = blankToNull(request.email());
        String phone = normalizePhone(request.phone());
        String phoneCountryCode = phone == null ? null : normalizePhoneCountryCode(request.phoneCountryCode());
        validateOptionalEmail(email);
        validateOptionalPhone(phoneCountryCode, phone);
        loginAccountDao.insertApiUser(
                username,
                request.password(),
                request.displayName().trim(),
                email,
                phoneCountryCode,
                phone
        );
        log.info("API user registered: username={}, clientIp={}", username, context.clientIp());
        AuthLoginAccount account = loginAccountDao.findApiByUsername(username);
        if (account == null) {
            log.error("API register login failed: username={}, reason=created_account_not_found", username);
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "created account not found");
        }
        return login(account, AuthOAuth2JdbcConfig.API_CLIENT_ID, AuthOAuth2JdbcConfig.API_LOGIN_GRANT_TYPE, context);
    }

    @Override
    public AntiAbuseChallengeVO createLoginChallenge(String purpose, LoginRequestContext context) {
        String normalizedPurpose = normalizeChallengePurpose(purpose);
        pruneExpiredLoginChallenges();
        ChallengeScheme scheme = randomChallengeScheme();
        if ("image_reassemble".equals(scheme.value())) {
            return createImageReassembleChallenge(normalizedPurpose, scheme, context);
        }
        List<LoginChallengeOption> options = buildChallengeOptions();
        List<LoginChallengeOption> targets = new ArrayList<>(options);
        Collections.shuffle(targets);
        targets = targets.subList(0, scheme.requiredSelectionCount());
        String question = scheme.prompt();
        String answer = targets.stream()
                .map(LoginChallengeOption::value)
                .reduce((first, second) -> first + "," + second)
                .orElse("");
        String challengeId = UUID.randomUUID().toString();
        LOGIN_CHALLENGES.put(challengeId, new LoginChallenge(
                answer,
                normalizedPurpose,
                contextFingerprint(context),
                scheme.ordered(),
                Instant.now().plus(LOGIN_CHALLENGE_TTL)
        ));
        List<ChallengeOptionVO> challengeOptions = options.stream()
                .map(option -> new ChallengeOptionVO(
                        option.value(),
                        new ChallengeShapeVO(option.shapeValue(), option.shapeClipPath()),
                        option.color(),
                        option.fillMode(),
                        null
                ))
                .toList();
        return new AntiAbuseChallengeVO(
                challengeId,
                question,
                GraphicChallengeUtil.buildQuestionImage(targets, scheme.prompt(), scheme.ordered()),
                (int) LOGIN_CHALLENGE_TTL.toSeconds(),
                scheme.value(),
                scheme.requiredSelectionCount(),
                challengeOptions
        );
    }

    private AntiAbuseChallengeVO createImageReassembleChallenge(String normalizedPurpose,
                                                                ChallengeScheme scheme,
                                                                LoginRequestContext context) {
        List<String> orderedValues = List.of(
                randomChallengeOptionValue(),
                randomChallengeOptionValue(),
                randomChallengeOptionValue(),
                randomChallengeOptionValue()
        );
        GraphicChallengeUtil.PuzzleChallenge puzzle = GraphicChallengeUtil.buildPuzzleChallenge(orderedValues, scheme.prompt());
        String answer = orderedValues.stream()
                .reduce((first, second) -> first + "," + second)
                .orElse("");
        String challengeId = UUID.randomUUID().toString();
        LOGIN_CHALLENGES.put(challengeId, new LoginChallenge(
                answer,
                normalizedPurpose,
                contextFingerprint(context),
                scheme.ordered(),
                Instant.now().plus(LOGIN_CHALLENGE_TTL)
        ));
        List<ChallengeOptionVO> challengeOptions = puzzle.tiles().stream()
                .map(tile -> new ChallengeOptionVO(tile.value(), null, null, null, tile.image()))
                .toList();
        return new AntiAbuseChallengeVO(
                challengeId,
                scheme.prompt(),
                puzzle.questionImage(),
                (int) LOGIN_CHALLENGE_TTL.toSeconds(),
                scheme.value(),
                scheme.requiredSelectionCount(),
                challengeOptions
        );
    }

    @Override
    public LoginResponse managerLogin(LoginVO request, LoginRequestContext context) {
        // Manager login is checked by network boundary first, then credentials, then explicit permission.
        String clientIp = context.clientIp();
        log.info("Manager login attempt: username={}, clientIp={}", request.username(), clientIp);
        if (!isManagerIpAllowed(clientIp)) {
            recordLoginFailure(request.username(), "MANAGER", AuthOAuth2JdbcConfig.MANAGER_CLIENT_ID,
                    AuthOAuth2JdbcConfig.MANAGER_LOGIN_GRANT_TYPE, context, "IP_NOT_ALLOWED");
            log.warn("Manager login rejected: username={}, clientIp={}, reason=ip_not_allowed",
                    request.username(), clientIp);
            throw new BusinessException(ErrorCode.AUTH_MANAGER_IP_FORBIDDEN);
        }

        Optional<AuthLoginAccount> account = Optional.ofNullable(loginAccountDao.findManagerByUsername(request.username()));
        if (account.isEmpty() || !matches(account.get(), request)) {
            recordLoginFailure(request.username(), "MANAGER", AuthOAuth2JdbcConfig.MANAGER_CLIENT_ID,
                    AuthOAuth2JdbcConfig.MANAGER_LOGIN_GRANT_TYPE, context, "INVALID_CREDENTIALS");
            log.warn("Manager login failed: username={}, clientIp={}, reason=invalid_credentials",
                    request.username(), clientIp);
            throw new BusinessException(ErrorCode.AUTH_INVALID_MANAGER_CREDENTIALS);
        }
        if (!"MANAGER".equalsIgnoreCase(account.get().userType()) || !account.get().permissions().contains(MANAGER_LOGIN_PERMISSION)) {
            recordLoginFailure(request.username(), "MANAGER", AuthOAuth2JdbcConfig.MANAGER_CLIENT_ID,
                    AuthOAuth2JdbcConfig.MANAGER_LOGIN_GRANT_TYPE, context, "PERMISSION_REQUIRED");
            log.warn("Manager login rejected: username={}, clientIp={}, userType={}, reason=permission_required",
                    request.username(), clientIp, account.get().userType());
            throw new BusinessException(ErrorCode.AUTH_MANAGER_PERMISSION_REQUIRED);
        }

        log.info("Manager login authenticated: username={}, clientIp={}", request.username(), clientIp);
        return login(account.get(), AuthOAuth2JdbcConfig.MANAGER_CLIENT_ID, AuthOAuth2JdbcConfig.MANAGER_LOGIN_GRANT_TYPE, context);
    }

    private LoginResponse login(AuthLoginAccount account,
                                String clientId,
                                AuthorizationGrantType grantType,
                                LoginRequestContext context) {
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null) {
            log.error("Login failed: username={}, clientId={}, grantType={}, reason=client_not_initialized",
                    account.username(), clientId, grantType.getValue());
            throw new BusinessException(ErrorCode.AUTH_CLIENT_NOT_INITIALIZED);
        }
        if (sessionProperties.isRedis()) {
            return loginWithRedis(account, registeredClient, grantType, context);
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
        revokeExistingJdbcSessions(account.username(), registeredClient.getId());
        authorizationService.save(authorization);
        recordLoginLog(account, registeredClient.getClientId(), grantType, context, issuedAt);
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
        if (sessionProperties.isRedis()) {
            return refreshAccessTokenWithRedis(request);
        }

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
            if (sessionProperties.isRedis()) {
                return validateRedisAccessToken(token, jwt);
            }

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

    @Override
    public void logout(String authorization) {
        String token = resolveToken(authorization);
        if (token == null) {
            log.warn("Logout failed: reason=missing_bearer_token");
            throw new BusinessException(ErrorCode.AUTH_INVALID_ACCESS_TOKEN);
        }
        if (sessionProperties.isRedis()) {
            logoutWithRedis(token);
            return;
        }

        OAuth2Authorization savedAuthorization = authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
        if (savedAuthorization == null) {
            log.warn("Logout skipped: reason=authorization_not_found");
            throw new BusinessException(ErrorCode.AUTH_INVALID_ACCESS_TOKEN);
        }

        authorizationService.remove(savedAuthorization);
        log.info("Logout succeeded: username={}, authorizationId={}",
                savedAuthorization.getPrincipalName(), savedAuthorization.getId());
    }

    @Override
    public int kickOut(KickOutVO request, String operatorAuthorization) {
        TokenValidation operator = requireManagerOperator(operatorAuthorization, request.username(), "Kick-out");
        if (sessionProperties.isRedis()) {
            int revokedCount = kickOutWithRedis(request.username());
            log.info("Redis kick-out succeeded: operator={}, target={}, revokedCount={}",
                    operator.username(), request.username(), revokedCount);
            return revokedCount;
        }

        List<String> authorizationIds = findAuthorizationIdsByPrincipalName(request.username());
        int revokedCount = 0;
        for (String authorizationId : authorizationIds) {
            OAuth2Authorization authorization = authorizationService.findById(authorizationId);
            if (authorization != null) {
                authorizationService.remove(authorization);
                revokedCount++;
            }
        }

        log.info("Kick-out succeeded: operator={}, target={}, revokedCount={}",
                operator.username(), request.username(), revokedCount);
        return revokedCount;
    }

    @Override
    public List<SessionStatusVO> apiSessionStatuses(List<String> usernames, String operatorAuthorization) {
        TokenValidation operator = requireManagerOperator(operatorAuthorization, String.join(",", normalizeUsernames(usernames)), "API session status");
        List<String> normalizedUsernames = normalizeUsernames(usernames);
        List<SessionStatusVO> statuses = sessionProperties.isRedis()
                ? redisApiSessionStatuses(normalizedUsernames)
                : jdbcApiSessionStatuses(normalizedUsernames);
        log.debug("API session status queried: operator={}, userCount={}", operator.username(), normalizedUsernames.size());
        return statuses;
    }

    @Override
    public int kickOutApiUser(KickOutVO request, String operatorAuthorization) {
        TokenValidation operator = requireManagerOperator(operatorAuthorization, request.username(), "API kick-out");
        int revokedCount;
        if (sessionProperties.isRedis()) {
            revokedCount = revokeExistingRedisSessions(request.username(), AuthOAuth2JdbcConfig.API_CLIENT_ID);
            log.info("Redis API kick-out succeeded: operator={}, target={}, revokedCount={}",
                    operator.username(), request.username(), revokedCount);
            return revokedCount;
        }

        RegisteredClient apiClient = registeredClientRepository.findByClientId(AuthOAuth2JdbcConfig.API_CLIENT_ID);
        if (apiClient == null) {
            throw new BusinessException(ErrorCode.AUTH_CLIENT_NOT_INITIALIZED);
        }
        revokedCount = revokeExistingJdbcSessions(request.username(), apiClient.getId());
        log.info("JDBC API kick-out succeeded: operator={}, target={}, revokedCount={}",
                operator.username(), request.username(), revokedCount);
        return revokedCount;
    }

    private LoginResponse loginWithRedis(AuthLoginAccount account,
                                         RegisteredClient registeredClient,
                                         AuthorizationGrantType grantType,
                                         LoginRequestContext context) {
        Instant issuedAt = Instant.now();
        TokenSettings tokenSettings = registeredClient.getTokenSettings();
        Instant expiresAtInstant = issuedAt.plus(tokenSettings.getAccessTokenTimeToLive());
        Instant refreshTokenExpiresAtInstant = issuedAt.plus(tokenSettings.getRefreshTokenTimeToLive());
        Jwt jwt = issueAccessToken(account, registeredClient, issuedAt, expiresAtInstant);
        String refreshToken = randomTokenValue();
        RedisLoginSession session = new RedisLoginSession(
                jwt.getTokenValue(),
                refreshToken,
                account.username(),
                account.userType(),
                account.roles(),
                account.permissions(),
                registeredClient.getClientId(),
                grantType.getValue(),
                issuedAt,
                expiresAtInstant,
                issuedAt,
                refreshTokenExpiresAtInstant
        );
        revokeExistingRedisSessions(account.username(), registeredClient.getClientId());
        saveRedisSession(session);
        recordLoginLog(account, registeredClient.getClientId(), grantType, context, issuedAt);
        log.info("Redis token issued: username={}, userType={}, clientId={}, grantType={}, expiresAt={}, refreshTokenExpiresAt={}",
                account.username(), account.userType(), registeredClient.getClientId(), grantType.getValue(),
                expiresAtInstant, refreshTokenExpiresAtInstant);

        return loginResponse(session);
    }

    private LoginResponse refreshAccessTokenWithRedis(RefreshTokenVO request) {
        RedisLoginSession existingSession = readRedisSession(redisRefreshTokenKey(request.refreshToken()));
        if (existingSession == null || existingSession.refreshTokenExpiresAt().isBefore(Instant.now())) {
            log.warn("Redis refresh token failed: reason=invalid_refresh_token");
            throw new BusinessException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
        }

        RegisteredClient registeredClient = registeredClientRepository.findByClientId(existingSession.clientId());
        if (registeredClient == null) {
            log.warn("Redis refresh token failed: clientId={}, reason=registered_client_not_found",
                    existingSession.clientId());
            throw new BusinessException(ErrorCode.AUTH_REGISTERED_CLIENT_NOT_FOUND);
        }

        AuthLoginAccount account = new AuthLoginAccount(
                existingSession.username(),
                "",
                existingSession.userType(),
                existingSession.roles(),
                existingSession.permissions()
        );
        Instant issuedAt = Instant.now();
        Instant expiresAtInstant = issuedAt.plus(registeredClient.getTokenSettings().getAccessTokenTimeToLive());
        Jwt jwt = issueAccessToken(account, registeredClient, issuedAt, expiresAtInstant);
        deleteRedisAccessToken(existingSession.accessToken());

        RedisLoginSession refreshedSession = new RedisLoginSession(
                jwt.getTokenValue(),
                existingSession.refreshToken(),
                existingSession.username(),
                existingSession.userType(),
                existingSession.roles(),
                existingSession.permissions(),
                existingSession.clientId(),
                existingSession.grantType(),
                issuedAt,
                expiresAtInstant,
                existingSession.refreshTokenIssuedAt(),
                existingSession.refreshTokenExpiresAt()
        );
        saveRedisSession(refreshedSession);
        log.info("Redis access token refreshed: username={}, userType={}, clientId={}, expiresAt={}",
                refreshedSession.username(), refreshedSession.userType(), refreshedSession.clientId(), expiresAtInstant);
        return loginResponse(refreshedSession);
    }

    private TokenValidation validateRedisAccessToken(String token, Jwt jwt) {
        RedisLoginSession session = readRedisSession(redisAccessTokenKey(token));
        if (session == null || session.accessTokenExpiresAt().isBefore(Instant.now())) {
            log.warn("Redis token validation failed: subject={}, reason=session_not_active", jwt.getSubject());
            return new TokenValidation(false, null, null, List.of(), List.of(), Map.of());
        }
        if (!session.username().equals(jwt.getSubject())) {
            log.warn("Redis token validation failed: subject={}, sessionUser={}, reason=subject_mismatch",
                    jwt.getSubject(), session.username());
            return new TokenValidation(false, null, null, List.of(), List.of(), Map.of());
        }

        log.debug("Redis token validation succeeded: username={}, userType={}, expiresAt={}",
                session.username(), session.userType(), session.accessTokenExpiresAt());
        return new TokenValidation(
                true,
                session.username(),
                session.userType(),
                session.roles(),
                session.permissions(),
                Map.of("expiresAt", session.accessTokenExpiresAt())
        );
    }

    private void logoutWithRedis(String accessToken) {
        RedisLoginSession session = readRedisSession(redisAccessTokenKey(accessToken));
        if (session == null) {
            log.warn("Redis logout skipped: reason=session_not_found");
            throw new BusinessException(ErrorCode.AUTH_INVALID_ACCESS_TOKEN);
        }
        revokeRedisSession(session);
        log.info("Redis logout succeeded: username={}", session.username());
    }

    private int kickOutWithRedis(String username) {
        RSet<String> refreshTokens = redissonClient().getSet(redisUserSessionsKey(username));
        Set<String> tokenSnapshot = new LinkedHashSet<>(refreshTokens.readAll());
        int revokedCount = 0;
        for (String refreshToken : tokenSnapshot) {
            RedisLoginSession session = readRedisSession(redisRefreshTokenKey(refreshToken));
            if (session == null) {
                refreshTokens.remove(refreshToken);
                continue;
            }
            revokeRedisSession(session);
            revokedCount++;
        }
        refreshTokens.delete();
        return revokedCount;
    }

    private List<SessionStatusVO> jdbcApiSessionStatuses(List<String> usernames) {
        RegisteredClient apiClient = registeredClientRepository.findByClientId(AuthOAuth2JdbcConfig.API_CLIENT_ID);
        if (apiClient == null) {
            throw new BusinessException(ErrorCode.AUTH_CLIENT_NOT_INITIALIZED);
        }

        return usernames.stream()
                .map(username -> jdbcSessionStatus(username, apiClient.getId()))
                .toList();
    }

    private SessionStatusVO jdbcSessionStatus(String username, String registeredClientId) {
        int sessionCount = 0;
        Instant latestAccessTokenExpiresAt = null;
        Instant latestRefreshTokenExpiresAt = null;
        for (String authorizationId : findAuthorizationIdsByPrincipalNameAndRegisteredClientId(username, registeredClientId)) {
            OAuth2Authorization authorization = authorizationService.findById(authorizationId);
            if (authorization == null) {
                continue;
            }
            boolean active = false;
            if (authorization.getAccessToken() != null && authorization.getAccessToken().isActive()) {
                active = true;
                latestAccessTokenExpiresAt = latest(latestAccessTokenExpiresAt, authorization.getAccessToken().getToken().getExpiresAt());
            }
            if (authorization.getRefreshToken() != null && authorization.getRefreshToken().isActive()) {
                active = true;
                latestRefreshTokenExpiresAt = latest(latestRefreshTokenExpiresAt, authorization.getRefreshToken().getToken().getExpiresAt());
            }
            if (active) {
                sessionCount++;
            }
        }
        return new SessionStatusVO(username, sessionCount > 0, sessionCount, latestAccessTokenExpiresAt, latestRefreshTokenExpiresAt);
    }

    private List<SessionStatusVO> redisApiSessionStatuses(List<String> usernames) {
        return usernames.stream()
                .map(this::redisApiSessionStatus)
                .toList();
    }

    private SessionStatusVO redisApiSessionStatus(String username) {
        RSet<String> refreshTokens = redissonClient().getSet(redisUserSessionsKey(username));
        Set<String> tokenSnapshot = new LinkedHashSet<>(refreshTokens.readAll());
        int sessionCount = 0;
        Instant latestAccessTokenExpiresAt = null;
        Instant latestRefreshTokenExpiresAt = null;
        for (String refreshToken : tokenSnapshot) {
            RedisLoginSession session = readRedisSession(redisRefreshTokenKey(refreshToken));
            if (session == null) {
                refreshTokens.remove(refreshToken);
                continue;
            }
            if (!AuthOAuth2JdbcConfig.API_CLIENT_ID.equals(session.clientId()) || session.refreshTokenExpiresAt().isBefore(Instant.now())) {
                continue;
            }
            sessionCount++;
            latestAccessTokenExpiresAt = latest(latestAccessTokenExpiresAt, session.accessTokenExpiresAt());
            latestRefreshTokenExpiresAt = latest(latestRefreshTokenExpiresAt, session.refreshTokenExpiresAt());
        }
        return new SessionStatusVO(username, sessionCount > 0, sessionCount, latestAccessTokenExpiresAt, latestRefreshTokenExpiresAt);
    }

    private int revokeExistingJdbcSessions(String username, String registeredClientId) {
        List<String> authorizationIds = findAuthorizationIdsByPrincipalNameAndRegisteredClientId(username, registeredClientId);
        int revokedCount = 0;
        for (String authorizationId : authorizationIds) {
            OAuth2Authorization authorization = authorizationService.findById(authorizationId);
            if (authorization != null) {
                authorizationService.remove(authorization);
                revokedCount++;
            }
        }
        if (revokedCount > 0) {
            log.info("Existing JDBC sessions revoked before login: username={}, registeredClientId={}, revokedCount={}",
                    username, registeredClientId, revokedCount);
        }
        return revokedCount;
    }

    private int revokeExistingRedisSessions(String username, String clientId) {
        RSet<String> refreshTokens = redissonClient().getSet(redisUserSessionsKey(username));
        Set<String> tokenSnapshot = new LinkedHashSet<>(refreshTokens.readAll());
        int revokedCount = 0;
        for (String refreshToken : tokenSnapshot) {
            RedisLoginSession session = readRedisSession(redisRefreshTokenKey(refreshToken));
            if (session == null) {
                refreshTokens.remove(refreshToken);
                continue;
            }
            if (clientId.equals(session.clientId())) {
                revokeRedisSession(session);
                revokedCount++;
            }
        }
        if (revokedCount > 0) {
            log.info("Existing Redis sessions revoked before login: username={}, clientId={}, revokedCount={}",
                    username, clientId, revokedCount);
        }
        return revokedCount;
    }

    private TokenValidation requireManagerOperator(String operatorAuthorization, String target, String action) {
        TokenValidation operator = validate(operatorAuthorization);
        if (!operator.valid() || !hasManagerAccess(operator.roles())) {
            log.warn("{} rejected: operator={}, target={}, reason=manager_role_required",
                    action, operator.username(), target);
            throw new BusinessException(ErrorCode.AUTH_MANAGER_PERMISSION_REQUIRED);
        }
        return operator;
    }

    private List<String> normalizeUsernames(List<String> usernames) {
        if (usernames == null) {
            return List.of();
        }
        return usernames.stream()
                .filter(username -> username != null && !username.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private Instant latest(Instant current, Instant candidate) {
        if (candidate == null) {
            return current;
        }
        return current == null || candidate.isAfter(current) ? candidate : current;
    }

    private void saveRedisSession(RedisLoginSession session) {
        writeRedisSession(redisAccessTokenKey(session.accessToken()), session, session.accessTokenExpiresAt());
        writeRedisSession(redisRefreshTokenKey(session.refreshToken()), session, session.refreshTokenExpiresAt());
        redissonClient().getSet(redisUserSessionsKey(session.username())).add(session.refreshToken());
    }

    private void revokeRedisSession(RedisLoginSession session) {
        deleteRedisAccessToken(session.accessToken());
        redissonClient().getBucket(redisRefreshTokenKey(session.refreshToken())).delete();
        redissonClient().getSet(redisUserSessionsKey(session.username())).remove(session.refreshToken());
    }

    private void deleteRedisAccessToken(String accessToken) {
        redissonClient().getBucket(redisAccessTokenKey(accessToken)).delete();
    }

    private RedisLoginSession readRedisSession(String key) {
        RBucket<String> bucket = redissonClient().getBucket(key);
        String payload = bucket.get();
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, RedisLoginSession.class);
        } catch (JsonProcessingException ex) {
            log.warn("Redis session payload is invalid: key={}, message={}", key, ex.getMessage());
            bucket.delete();
            return null;
        }
    }

    private void writeRedisSession(String key, RedisLoginSession session, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        try {
            redissonClient().getBucket(key).set(objectMapper.writeValueAsString(session), ttl);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize Redis login session: username={}, key={}, message={}",
                    session.username(), key, ex.getOriginalMessage(), ex);
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR,
                    "Failed to serialize Redis login session: " + ex.getOriginalMessage());
        }
    }

    private LoginResponse loginResponse(RedisLoginSession session) {
        return new LoginResponse(
                "Bearer",
                session.accessToken(),
                session.refreshToken(),
                session.username(),
                session.userType(),
                session.roles(),
                session.permissions(),
                LocalDateTime.ofInstant(session.accessTokenExpiresAt(), ZoneId.systemDefault()),
                LocalDateTime.ofInstant(session.refreshTokenExpiresAt(), ZoneId.systemDefault())
        );
    }

    private RedissonClient redissonClient() {
        RedissonClient redissonClient = redissonClientProvider.getIfAvailable();
        if (redissonClient == null) {
            throw new BusinessException(ErrorCode.AUTH_REDIS_SESSION_STORE_UNAVAILABLE);
        }
        return redissonClient;
    }

    private String redisAccessTokenKey(String accessToken) {
        return redisKey("access", accessToken);
    }

    private String redisRefreshTokenKey(String refreshToken) {
        return redisKey("refresh", refreshToken);
    }

    private String redisUserSessionsKey(String username) {
        return redisKey("user", username);
    }

    private String redisKey(String namespace, String value) {
        return sessionProperties.getRedisKeyPrefix() + ":" + namespace + ":" + value;
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

    private boolean hasManagerAccess(List<String> roles) {
        return roles.contains("MANAGER") || roles.contains("SUPER_ADMIN");
    }

    private void recordLoginLog(AuthLoginAccount account,
                                String clientId,
                                AuthorizationGrantType grantType,
                                LoginRequestContext context,
                                Instant loginAt) {
        try {
            insertLoginLog(
                    account.userType(),
                    account.username(),
                    clientId,
                    grantType.getValue(),
                    context,
                    LocalDateTime.ofInstant(loginAt, ZoneId.systemDefault()),
                    LOGIN_RESULT_SUCCESS,
                    null
            );
        } catch (Exception ex) {
            log.error("Login log write failed: username={}, userType={}, clientId={}, message={}",
                    account.username(), account.userType(), clientId, ex.getMessage(), ex);
        }
    }

    private void recordLoginFailure(String username,
                                    String userType,
                                    String clientId,
                                    AuthorizationGrantType grantType,
                                    LoginRequestContext context,
                                    String failureReason) {
        try {
            insertLoginLog(
                    userType,
                    username,
                    clientId,
                    grantType.getValue(),
                    context,
                    LocalDateTime.now(),
                    LOGIN_RESULT_FAILURE,
                    failureReason
            );
        } catch (Exception ex) {
            log.error("Login failure log write failed: username={}, userType={}, clientId={}, failureReason={}, message={}",
                    username, userType, clientId, failureReason, ex.getMessage(), ex);
        }
    }

    private void requireRegisterText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, message);
        }
    }

    private void verifyLoginChallenge(String challengeId, String rawAnswer, String purpose, LoginRequestContext context) {
        String normalizedChallengeId = blankToNull(challengeId);
        String answer = blankToNull(rawAnswer);
        if (answer != null) {
            answer = answer.replaceAll("\\s+", "");
        }
        if (normalizedChallengeId == null || answer == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "anti abuse verification is required");
        }
        LoginChallenge challenge = LOGIN_CHALLENGES.get(normalizedChallengeId);
        if (challenge == null || challenge.expiresAt().isBefore(Instant.now())) {
            LOGIN_CHALLENGES.remove(normalizedChallengeId);
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "anti abuse verification expired");
        }
        if (!challenge.purpose().equals(purpose) || !challenge.contextFingerprint().equals(contextFingerprint(context))) {
            LOGIN_CHALLENGES.remove(normalizedChallengeId);
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "anti abuse verification is invalid");
        }
        if (!matchesLoginChallengeAnswer(challenge.answer(), answer, challenge.ordered())) {
            LOGIN_CHALLENGES.remove(normalizedChallengeId);
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "anti abuse verification is invalid");
        }
        LOGIN_CHALLENGES.remove(normalizedChallengeId);
    }

    private String normalizeChallengePurpose(String purpose) {
        String normalized = blankToNull(purpose);
        if (normalized == null || CHALLENGE_PURPOSE_API_LOGIN.equals(normalized)) {
            return CHALLENGE_PURPOSE_API_LOGIN;
        }
        if (CHALLENGE_PURPOSE_API_REGISTER.equals(normalized)) {
            return CHALLENGE_PURPOSE_API_REGISTER;
        }
        throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "anti abuse verification purpose is invalid");
    }

    private String contextFingerprint(LoginRequestContext context) {
        String clientIp = context == null ? null : blankToNull(context.clientIp());
        String userAgent = context == null ? null : blankToNull(context.userAgent());
        return (clientIp == null ? "" : clientIp) + "|" + (userAgent == null ? "" : userAgent);
    }

    private boolean matchesLoginChallengeAnswer(String expectedAnswer, String answer, boolean ordered) {
        List<String> expected = Arrays.stream(expectedAnswer.split(",")).filter(value -> !value.isBlank()).toList();
        List<String> actual = Arrays.stream(answer.split(",")).filter(value -> !value.isBlank()).toList();
        if (!ordered) {
            expected = expected.stream().sorted().toList();
            actual = actual.stream().sorted().toList();
        }
        return expected.equals(actual);
    }

    private String randomChallengeOptionValue() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String randomFillMode() {
        return ThreadLocalRandom.current().nextBoolean() ? "solid" : "outline";
    }

    private ChallengeScheme randomChallengeScheme() {
        return CHALLENGE_SCHEMES.get(ThreadLocalRandom.current().nextInt(CHALLENGE_SCHEMES.size()));
    }

    private void pruneExpiredLoginChallenges() {
        Instant now = Instant.now();
        LOGIN_CHALLENGES.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateOptionalEmail(String email) {
        if (email != null && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "email format is invalid");
        }
    }

    private void validateOptionalPhone(String phoneCountryCode, String phone) {
        if (phone == null) {
            return;
        }
        if (phoneCountryCode == null || !phoneCountryCode.matches("^\\+[1-9]\\d{0,3}$") || !phone.matches("^\\d{4,14}$")
                || !(phoneCountryCode.substring(1) + phone).matches("^[1-9]\\d{7,14}$")) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "phone format is invalid");
        }
    }

    private String normalizePhone(String phone) {
        String normalized = phone == null ? null : phone.trim().replaceAll("[\\s\\-()]", "");
        if (normalized != null && normalized.startsWith("+")) {
            normalized = normalized.substring(1);
        }
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private String normalizePhoneCountryCode(String phoneCountryCode) {
        String normalized = blankToNull(phoneCountryCode);
        if (normalized == null) {
            return "+86";
        }
        normalized = normalized.replaceAll("[\\s\\-()]", "");
        return normalized.startsWith("+") ? normalized : "+" + normalized;
    }

    private void insertLoginLog(String userType,
                                String username,
                                String clientId,
                                String grantType,
                                LoginRequestContext context,
                                LocalDateTime loginAt,
                                String loginResult,
                                String failureReason) {
        if ("MANAGER".equalsIgnoreCase(userType)) {
            managerLoginLogDao.insertLoginLog(managerLoginLog(
                    username, userType, clientId, grantType, context, loginAt, loginResult, failureReason));
            return;
        }
        partnerLoginLogDao.insertLoginLog(partnerLoginLog(
                username, userType, clientId, grantType, context, loginAt, loginResult, failureReason));
    }

    private PartnerLoginLog partnerLoginLog(String username,
                                            String userType,
                                            String clientId,
                                            String grantType,
                                            LoginRequestContext context,
                                            LocalDateTime loginAt,
                                            String loginResult,
                                            String failureReason) {
        PartnerLoginLog loginLog = new PartnerLoginLog();
        fillLoginLog(loginLog, username, userType, clientId, grantType, context, loginAt, loginResult, failureReason);
        return loginLog;
    }

    private ManagerLoginLog managerLoginLog(String username,
                                            String userType,
                                            String clientId,
                                            String grantType,
                                            LoginRequestContext context,
                                            LocalDateTime loginAt,
                                            String loginResult,
                                            String failureReason) {
        ManagerLoginLog loginLog = new ManagerLoginLog();
        fillLoginLog(loginLog, username, userType, clientId, grantType, context, loginAt, loginResult, failureReason);
        return loginLog;
    }

    private void fillLoginLog(PartnerLoginLog loginLog,
                              String username,
                              String userType,
                              String clientId,
                              String grantType,
                              LoginRequestContext context,
                              LocalDateTime loginAt,
                              String loginResult,
                              String failureReason) {
        loginLog.setUsername(username);
        loginLog.setUserType(userType);
        loginLog.setClientId(clientId);
        loginLog.setGrantType(grantType);
        loginLog.setClientIp(context.clientIp());
        loginLog.setUserAgent(context.userAgent());
        loginLog.setDeviceType(context.deviceType());
        loginLog.setBrowser(context.browser());
        loginLog.setOperatingSystem(context.operatingSystem());
        loginLog.setLoginAt(loginAt);
        loginLog.setLoginResult(loginResult);
        loginLog.setFailureReason(failureReason);
    }

    private void fillLoginLog(ManagerLoginLog loginLog,
                              String username,
                              String userType,
                              String clientId,
                              String grantType,
                              LoginRequestContext context,
                              LocalDateTime loginAt,
                              String loginResult,
                              String failureReason) {
        loginLog.setUsername(username);
        loginLog.setUserType(userType);
        loginLog.setClientId(clientId);
        loginLog.setGrantType(grantType);
        loginLog.setClientIp(context.clientIp());
        loginLog.setUserAgent(context.userAgent());
        loginLog.setDeviceType(context.deviceType());
        loginLog.setBrowser(context.browser());
        loginLog.setOperatingSystem(context.operatingSystem());
        loginLog.setLoginAt(loginAt);
        loginLog.setLoginResult(loginResult);
        loginLog.setFailureReason(failureReason);
    }

    private List<String> findAuthorizationIdsByPrincipalName(String username) {
        return oAuth2AuthorizationDao.findIdsByPrincipalName(username);
    }

    private List<String> findAuthorizationIdsByPrincipalNameAndRegisteredClientId(String username, String registeredClientId) {
        return oAuth2AuthorizationDao.findIdsByPrincipalNameAndRegisteredClientId(username, registeredClientId);
    }

    private record LoginAttempt(int failedCount, LocalDateTime lockedUntil) {
    }

    private record LoginChallenge(String answer, String purpose, String contextFingerprint, boolean ordered, Instant expiresAt) {
    }

    private record ChallengeScheme(String value, int requiredSelectionCount, boolean ordered, String prompt) {
    }

    private List<LoginChallengeOption> buildChallengeOptions() {
        List<LoginChallengeOption> options = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<String> shapeTemplates = new ArrayList<>(GraphicChallengeUtil.readableShapeTemplates());
        Collections.shuffle(shapeTemplates);
        for (int i = 0; i < 9; i++) {
            PatternColor color = LOGIN_PATTERN_COLORS.get(random.nextInt(LOGIN_PATTERN_COLORS.size()));
            options.add(new LoginChallengeOption(
                    randomChallengeOptionValue(),
                    "随机图形",
                    randomChallengeOptionValue(),
                    GraphicChallengeUtil.jitterShapeClipPath(shapeTemplates.get(i)),
                    color.hex(),
                    randomFillMode()
            ));
        }
        return options;
    }

    private record LoginChallengeOption(String value,
                                        String label,
                                        String shapeValue,
                                        String shapeClipPath,
                                        String color,
                                        String fillMode) implements GraphicChallengeUtil.SvgShape {
    }

    private record PatternColor(String value, String label, String hex) {
    }
}
